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
import org.apache.flink.configuration.{Configuration, DeploymentOptions, PipelineOptions}
import org.apache.flink.util.Preconditions.checkNotNull
import org.apache.flink.yarn.configuration.{YarnConfigOptions, YarnDeploymentTarget}
import org.apache.hadoop.fs.Path
import org.apache.hadoop.yarn.api.records.ApplicationId

import java.io.File
import java.util.{Collections, List => JavaList}
import scala.collection.JavaConverters._
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

    /**
     * 必须保持本机flink和hdfs里的flink版本和配置都完全一致.
     */
    val flinkName = new File(FLINK_HOME).getName
    val flinkHdfsHome = s"${HdfsUtils.getDefaultFS}$APP_FLINK/$flinkName"
    val flinkHdfsLibs = new Path(s"$flinkHdfsHome/lib")
    val flinkHdfsPlugins = new Path(s"$flinkHdfsHome/plugins")
    val streamxPlugin = new Path(s"${HdfsUtils.getDefaultFS}$APP_PLUGINS")

    val flinkHdfsDistJar = new File(s"$FLINK_HOME/lib").list().filter(_.matches("flink-dist_.*\\.jar")) match {
      case Array() => throw new IllegalArgumentException(s"[StreamX] can no found flink-dist jar in $FLINK_HOME/lib")
      case array if array.length == 1 => s"$flinkHdfsHome/lib/${array.head}"
      case more => throw new IllegalArgumentException(s"[StreamX] found multiple flink-dist jar in $FLINK_HOME/lib,[${more.mkString(",")}]")
    }

    val flinkYaml = HdfsUtils.read(s"$flinkHdfsHome/conf/flink-conf.yaml")

    val programArgs = new ArrayBuffer[String]()
    Try(submitRequest.args.split("\\s+")).getOrElse(Array()).foreach(x => if (x.nonEmpty) programArgs += x)
    programArgs += PARAM_KEY_FLINK_CONF
    programArgs += DeflaterUtils.zipString(flinkYaml)
    programArgs += PARAM_KEY_APP_NAME
    programArgs += submitRequest.effectiveAppName

    val providedLibs = ListBuffer(flinkHdfsLibs.toString, flinkHdfsPlugins.toString, streamxPlugin.toString)

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
    //main class
    if (submitRequest.developmentMode == DevelopmentMode.CUSTOMCODE) {
      effectiveConfiguration.set(ApplicationConfiguration.APPLICATION_MAIN_CLASS, submitRequest.appMain)
    }
    //yarn.provided.lib.dirs
    effectiveConfiguration.set(YarnConfigOptions.PROVIDED_LIB_DIRS, providedLibs.asJava)
    //flinkDistJar
    effectiveConfiguration.set(YarnConfigOptions.FLINK_DIST_JAR, flinkHdfsDistJar)
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

    logInfo("----------------------------------------------------------------------")
    logInfo(s"Effective executor configuration: $effectiveConfiguration ")
    logInfo("----------------------------------------------------------------------")

    effectiveConfiguration
  }

}
