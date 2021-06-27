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
import org.apache.flink.configuration._
import org.apache.flink.runtime.security.{SecurityConfiguration, SecurityUtils}
import org.apache.flink.runtime.util.HadoopUtils
import org.apache.flink.util.Preconditions.checkNotNull
import org.apache.flink.yarn.configuration.{YarnConfigOptions, YarnDeploymentTarget}
import org.apache.hadoop.security.UserGroupInformation
import org.apache.hadoop.yarn.api.records.ApplicationId

import java.lang.{Boolean => JavaBool}
import java.util.concurrent.Callable
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
      "-t" -> YarnDeploymentTarget.APPLICATION.getName
    )

    val activeCommandLine = validateAndGetActiveCommandLine(submitRequest.customCommandLines, commandLine)

    val uri = PackagedProgramUtils.resolveURI(submitRequest.flinkUserJar)

    val flinkConfig = getEffectiveConfiguration(submitRequest, activeCommandLine, commandLine, Collections.singletonList(uri.toString))

    SecurityUtils.install(new SecurityConfiguration(flinkConfig))
    SecurityUtils.getInstalledContext.runSecured(new Callable[SubmitResponse] {
      override def call(): SubmitResponse = {
        val clusterClientServiceLoader = new DefaultClusterClientServiceLoader
        val clientFactory = clusterClientServiceLoader.getClusterClientFactory[ApplicationId](flinkConfig)
        val clusterDescriptor = clientFactory.createClusterDescriptor(flinkConfig)
        try {
          val clusterSpecification = clientFactory.getClusterSpecification(flinkConfig)
          logInfo(
            s"""
               |--------------------------<<specification>>---------------------------
               |$clusterSpecification
               |----------------------------------------------------------------------
               |""".stripMargin)

          val applicationConfiguration = ApplicationConfiguration.fromConfiguration(flinkConfig)
          var applicationId: ApplicationId = null
          val clusterClient = clusterDescriptor.deployApplicationCluster(clusterSpecification, applicationConfiguration).getClusterClient
          applicationId = clusterClient.getClusterId

          logInfo(
            s"""
               ||--------------------------<<applicationId>>--------------------------|
               || Flink Job Started: applicationId: $applicationId|
               ||_____________________________________________________________________|
               |""".stripMargin)

          SubmitResponse(applicationId, flinkConfig)
        } finally if (clusterDescriptor != null) {
          clusterDescriptor.close()
        }
      }
    })
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
    super.applyToConfiguration(submitRequest, effectiveConfiguration)

    val (providedLibs, programArgs) = {
      val programArgs = new ArrayBuffer[String]()
      Try(submitRequest.args.split("\\s+")).getOrElse(Array()).foreach(x => if (x.nonEmpty) programArgs += x)
      programArgs += PARAM_KEY_FLINK_CONF
      programArgs += DeflaterUtils.zipString(submitRequest.flinkYaml)
      programArgs += PARAM_KEY_APP_NAME
      programArgs += submitRequest.effectiveAppName
      val providedLibs = ListBuffer(
        submitRequest.workspaceEnv.flinkLib,
        submitRequest.workspaceEnv.appJars,
        submitRequest.workspaceEnv.appPlugins
      )
      submitRequest.developmentMode match {
        case DevelopmentMode.FLINKSQL =>
          programArgs += PARAM_KEY_FLINK_SQL
          programArgs += submitRequest.flinkSQL
          if (submitRequest.appConf != null) {
            programArgs += PARAM_KEY_APP_CONF
            programArgs += submitRequest.appConf
          }
          val version = submitRequest.flinkVersion.split("\\.").map(_.trim.toInt)
          version match {
            case Array(1, 13, _) =>
              providedLibs += s"${HdfsUtils.getDefaultFS}$APP_SHIMS/flink-1.13"
            case Array(1, 11 | 12, _) =>
              providedLibs += s"${HdfsUtils.getDefaultFS}$APP_SHIMS/flink-1.12"
            case _ =>
              throw new UnsupportedOperationException(s"Unsupported flink version: ${submitRequest.flinkVersion}")
          }
          val jobLib = s"${HdfsUtils.getDefaultFS}$APP_WORKSPACE/${submitRequest.jobID}/lib"
          if (HdfsUtils.exists(jobLib)) {
            providedLibs += jobLib
          }
        case _ =>
          // Custom Code 必传配置文件...
          programArgs += PARAM_KEY_APP_CONF
          programArgs += submitRequest.appConf
      }
      providedLibs -> programArgs
    }

    val currentUser = UserGroupInformation.getCurrentUser
    logDebug(s"UserGroupInformation currentUser: $currentUser")
    if (HadoopUtils.isKerberosSecurityEnabled(currentUser)) {
      logDebug(s"kerberos Security is Enabled...")
      val useTicketCache = getOptionFromDefaultFlinkConfig[JavaBool](submitRequest.flinkHome, SecurityOptions.KERBEROS_LOGIN_USETICKETCACHE)
      if (!HadoopUtils.areKerberosCredentialsValid(currentUser, useTicketCache)) {
        throw new RuntimeException(s"Hadoop security with Kerberos is enabled but the login user ${currentUser} does not have Kerberos credentials or delegation tokens!")
      }
    }

    //yarn.provided.lib.dirs
    effectiveConfiguration.set(YarnConfigOptions.PROVIDED_LIB_DIRS, providedLibs.asJava)
    //flinkDistJar
    effectiveConfiguration.set(YarnConfigOptions.FLINK_DIST_JAR, submitRequest.workspaceEnv.flinkDistJar)
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
    effectiveConfiguration.set(retainedOption, submitRequest.flinkDefaultConfiguration.get(retainedOption))

    logInfo(
      s"""
         |----------------------------------------------------------------------
         |Effective executor configuration: $effectiveConfiguration
         |----------------------------------------------------------------------
         |""".stripMargin)

    effectiveConfiguration
  }

}
