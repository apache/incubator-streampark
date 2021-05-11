/*
 * Copyright (c) 2019 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.streamxhub.streamx.flink.submit.impl

import com.streamxhub.streamx.common.conf.ConfigConst._
import com.streamxhub.streamx.common.enums.DevelopmentMode
import com.streamxhub.streamx.common.util.{DeflaterUtils, HdfsUtils}
import com.streamxhub.streamx.flink.submit.`trait`.YarnSubmitTrait
import com.streamxhub.streamx.flink.submit.{SubmitRequest, SubmitResponse}
import org.apache.commons.cli.CommandLine
import org.apache.flink.client.cli.{CustomCommandLine, ExecutionConfigAccessor, ProgramOptions}
import org.apache.flink.client.deployment.DefaultClusterClientServiceLoader
import org.apache.flink.client.deployment.application.ApplicationConfiguration
import org.apache.flink.client.program.PackagedProgramUtils
import org.apache.flink.configuration.{CheckpointingOptions, ConfigOption, Configuration, DeploymentOptions, PipelineOptions}
import org.apache.flink.util.Preconditions.checkNotNull
import org.apache.flink.yarn.configuration.{YarnConfigOptions, YarnDeploymentTarget}
import org.apache.hadoop.yarn.api.records.ApplicationId

import java.util.{Collections, List => JavaList}
import scala.collection.JavaConverters._
import scala.collection.JavaConversions._

import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.util.Try

/**
 * yarn application mode submit
 */
object ApplicationSubmit extends YarnSubmitTrait {

  override def doSubmit(submitRequest: SubmitRequest): SubmitResponse = {

    val commandLine = getEffectiveCommandLine(
      submitRequest,
      customCommandLines,
      "-t" -> YarnDeploymentTarget.APPLICATION.getName
    )

    val activeCommandLine = validateAndGetActiveCommandLine(customCommandLines, commandLine)

    val uri = PackagedProgramUtils.resolveURI(submitRequest.flinkUserJar)

    val flinkConfig = getEffectiveConfiguration(submitRequest, activeCommandLine, commandLine, Collections.singletonList(uri.toString))

    val clusterClientServiceLoader = new DefaultClusterClientServiceLoader
    val clientFactory = clusterClientServiceLoader.getClusterClientFactory[ApplicationId](flinkConfig)
    val clusterDescriptor = clientFactory.createClusterDescriptor(flinkConfig)
    try {
      val clusterSpecification = clientFactory.getClusterSpecification(flinkConfig)
      logInfo("--------------------------<<specification>>---------------------------")
      logInfo(s"$clusterSpecification")
      logInfo("----------------------------------------------------------------------")
      val applicationConfiguration = ApplicationConfiguration.fromConfiguration(flinkConfig)
      var applicationId: ApplicationId = null
      val clusterClient = clusterDescriptor.deployApplicationCluster(clusterSpecification, applicationConfiguration).getClusterClient
      applicationId = clusterClient.getClusterId
      logInfo("|--------------------------<<applicationId>>--------------------------|")
      logInfo(s"| Flink Job Started: applicationId: $applicationId  |")
      logInfo("|_____________________________________________________________________|")
      SubmitResponse(applicationId, flinkConfig)
    } finally if (clusterDescriptor != null) {
      clusterDescriptor.close()
    }
  }

  private def getEffectiveConfiguration[T](
                                            submitRequest: SubmitRequest,
                                            activeCustomCommandLine: CustomCommandLine,
                                            commandLine: CommandLine,
                                            jobJars: JavaList[String]) = {

    val executorConfig = checkNotNull(activeCustomCommandLine).toConfiguration(commandLine)
    val effectiveConfiguration = new Configuration(executorConfig)
    val programOptions = ProgramOptions.create(commandLine)
    val executionParameters = ExecutionConfigAccessor.fromProgramOptions(programOptions, jobJars)
    executionParameters.applyToConfiguration(effectiveConfiguration)

    val programArgs = new ArrayBuffer[String]()
    Try(submitRequest.args.split("\\s+")).getOrElse(Array()).foreach(x => if (x.nonEmpty) programArgs += x)

    programArgs += PARAM_KEY_FLINK_CONF
    programArgs += DeflaterUtils.zipString(submitRequest.flinkYaml)
    programArgs += PARAM_KEY_APP_NAME
    programArgs += submitRequest.effectiveAppName

    val providedLibs = ListBuffer(
      workspaceEnv.flinkHdfsLibs.toString,
      workspaceEnv.flinkHdfsPlugins.toString,
      workspaceEnv.flinkHdfsJars.toString,
      workspaceEnv.streamxPlugin.toString
    )

    submitRequest.developmentMode match {
      case DevelopmentMode.FLINKSQL =>
        programArgs += PARAM_KEY_FLINK_SQL
        programArgs += submitRequest.flinkSQL
        if (submitRequest.appConf != null) {
          programArgs += PARAM_KEY_APP_CONF
          programArgs += submitRequest.appConf
        }
        providedLibs += s"${HdfsUtils.getDefaultFS}$APP_WORKSPACE/${submitRequest.jobID}/lib"
      case _ =>
        // Custom Code 必传配置文件...
        programArgs += PARAM_KEY_APP_CONF
        programArgs += submitRequest.appConf
    }

    val defParallelism = getParallelism(submitRequest)
    if (defParallelism != null) {
      programArgs += PARAM_KEY_FLINK_PARALLELISM
      programArgs += s"$defParallelism"
    }

    //flink-conf.yaml配置
    flinkDefaultConfiguration.keySet().foreach(x=>{
      flinkDefaultConfiguration.getString(x,null) match {
        case v if v != null => effectiveConfiguration.setString(x, v)
        case _ =>
      }
    })

    //main class
    if (submitRequest.developmentMode == DevelopmentMode.CUSTOMCODE) {
      effectiveConfiguration.set(ApplicationConfiguration.APPLICATION_MAIN_CLASS, submitRequest.appMain)
    }
    //yarn.provided.lib.dirs
    effectiveConfiguration.set(YarnConfigOptions.PROVIDED_LIB_DIRS, providedLibs.asJava)
    //flinkDistJar
    effectiveConfiguration.set(YarnConfigOptions.FLINK_DIST_JAR, workspaceEnv.flinkHdfsDistJar)
    //pipeline.jars
    effectiveConfiguration.set(PipelineOptions.JARS, Collections.singletonList(submitRequest.flinkUserJar))
    //execution.target
    effectiveConfiguration.set(DeploymentOptions.TARGET, YarnDeploymentTarget.APPLICATION.getName)
    //yarn application name
    effectiveConfiguration.set(YarnConfigOptions.APPLICATION_NAME, submitRequest.effectiveAppName)
    //yarn application Type
    effectiveConfiguration.set(YarnConfigOptions.APPLICATION_TYPE, submitRequest.applicationType)
    //arguments...
    effectiveConfiguration.set(ApplicationConfiguration.APPLICATION_ARGS, programArgs.toList.asJava)
    //state.checkpoints.num-retained
    val retainedOption = CheckpointingOptions.MAX_RETAINED_CHECKPOINTS
    effectiveConfiguration.set(retainedOption, flinkDefaultConfiguration.get(retainedOption))

    logInfo("----------------------------------------------------------------------")
    logInfo(s"Effective executor configuration: $effectiveConfiguration ")
    logInfo("----------------------------------------------------------------------")

    effectiveConfiguration
  }

}
