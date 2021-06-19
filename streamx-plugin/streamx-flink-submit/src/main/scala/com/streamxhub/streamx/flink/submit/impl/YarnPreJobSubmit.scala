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

import com.streamxhub.streamx.common.enums.DevelopmentMode
import com.streamxhub.streamx.common.util.DeflaterUtils
import com.streamxhub.streamx.flink.submit.`trait`.YarnSubmitTrait
import com.streamxhub.streamx.flink.submit.{SubmitRequest, SubmitResponse}
import org.apache.commons.cli.CommandLine
import org.apache.flink.client.cli.CustomCommandLine
import org.apache.flink.client.deployment.DefaultClusterClientServiceLoader
import org.apache.flink.client.deployment.application.ApplicationConfiguration
import org.apache.flink.client.program.{PackagedProgram, PackagedProgramUtils}
import org.apache.flink.configuration.{Configuration, DeploymentOptions}
import org.apache.flink.util.Preconditions.checkNotNull
import org.apache.flink.yarn.YarnClusterDescriptor
import org.apache.flink.yarn.configuration.{YarnConfigOptions, YarnDeploymentTarget}
import org.apache.flink.yarn.entrypoint.YarnJobClusterEntrypoint
import org.apache.hadoop.fs.{Path => HadoopPath}
import org.apache.hadoop.yarn.api.records.ApplicationId

import java.io.File
import java.lang.{Boolean => JavaBool}
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.util.Try

/**
 * yarn PreJob mode submit
 */
object YarnPreJobSubmit extends YarnSubmitTrait {

  override def doSubmit(submitRequest: SubmitRequest): SubmitResponse = {

    val commandLine = getEffectiveCommandLine(
      submitRequest,
      "-t" -> YarnDeploymentTarget.PER_JOB.getName,
      "-m" -> "yarn-cluster",
      "-c" -> submitRequest.appMain
    )

    val activeCommandLine = validateAndGetActiveCommandLine(submitRequest.customCommandLines, commandLine)
    val flinkConfig = getEffectiveConfiguration(submitRequest, activeCommandLine, commandLine)

    val clusterClientServiceLoader = new DefaultClusterClientServiceLoader
    val clientFactory = clusterClientServiceLoader.getClusterClientFactory[ApplicationId](flinkConfig)

    val clusterDescriptor = {
      val clusterDescriptor = clientFactory.createClusterDescriptor(flinkConfig).asInstanceOf[YarnClusterDescriptor]
      val flinkDistJar = new File(s"${submitRequest.flinkHome}/lib").list().filter(_.matches("flink-dist_.*\\.jar")) match {
        case Array() => throw new IllegalArgumentException(s"[StreamX] can no found flink-dist jar in ${submitRequest.flinkHome}/lib")
        case array if array.length == 1 => s"${submitRequest.flinkHome}/lib/${array.head}"
        case more => throw new IllegalArgumentException(s"[StreamX] found multiple flink-dist jar in ${submitRequest.flinkHome}/lib,[${more.mkString(",")}]")
      }
      clusterDescriptor.setLocalJarPath(new HadoopPath(flinkDistJar))
      clusterDescriptor.addShipFiles(List(new File(s"${submitRequest.flinkHome}/plugins")))
      clusterDescriptor
    }

    try {
      val clusterClient = {
        val clusterSpecification = clientFactory.getClusterSpecification(flinkConfig)
        logInfo(
          s"""
             |--------------------------<<specification>>---------------------------
             |$clusterSpecification
             |----------------------------------------------------------------------
             |""".stripMargin)

        val packagedProgram = PackagedProgram
          .newBuilder
          .setJarFile(new File(submitRequest.flinkUserJar))
          .setEntryPointClassName(flinkConfig.getOptional(ApplicationConfiguration.APPLICATION_MAIN_CLASS).get())
          .setArguments(
            flinkConfig
              .getOptional(ApplicationConfiguration.APPLICATION_ARGS)
              .get()
              : _*
          ).build

        val jobGraph = PackagedProgramUtils.createJobGraph(
          packagedProgram,
          flinkConfig,
          getParallelism(submitRequest),
          false
        )
        logInfo(
          s"""
             ||--------------------------<<applicationId>>--------------------------|
             || jobGraph getJobID: ${jobGraph.getJobID.toString}  |
             ||_____________________________________________________________________|
             |""".stripMargin)
        deployInternal(
          clusterDescriptor,
          clusterSpecification,
          submitRequest.effectiveAppName,
          classOf[YarnJobClusterEntrypoint].getName,
          jobGraph,
          false
        ).getClusterClient

      }
      val applicationId = clusterClient.getClusterId
      logInfo(
        s"""
           ||--------------------------<<applicationId>>--------------------------|
           || Flink Job Started: applicationId: $applicationId  |
           ||_____________________________________________________________________|
           |""".stripMargin)

      SubmitResponse(applicationId, flinkConfig)
    } finally if (clusterDescriptor != null) {
      clusterDescriptor.close()
    }
  }

  private def getEffectiveConfiguration[T](submitRequest: SubmitRequest, activeCustomCommandLine: CustomCommandLine, commandLine: CommandLine) = {
    val executorConfig = checkNotNull(activeCustomCommandLine).toConfiguration(commandLine)
    val effectiveConfiguration = new Configuration(executorConfig)
    super.applyToConfiguration(submitRequest, effectiveConfiguration)

    val (providedLibs, programArgs) = {
      val providedLibs = ListBuffer(submitRequest.workspaceEnv.appJars)

      val programArgs = new ArrayBuffer[String]()
      Try(submitRequest.args.split("\\s+")).getOrElse(Array()).foreach(x => if (x.nonEmpty) programArgs += x)
      programArgs += PARAM_KEY_APP_NAME
      programArgs += submitRequest.effectiveAppName
      programArgs += PARAM_KEY_FLINK_CONF
      programArgs += DeflaterUtils.zipString(submitRequest.flinkYaml)

      submitRequest.developmentMode match {
        case DevelopmentMode.FLINKSQL =>
          programArgs += PARAM_KEY_FLINK_SQL
          programArgs += submitRequest.flinkSQL
          if (submitRequest.appConf != null) {
            programArgs += PARAM_KEY_APP_CONF
            programArgs += submitRequest.appConf
          }
          val jobId = submitRequest.jobID
        case _ =>
          // Custom Code 必传配置文件...
          programArgs += PARAM_KEY_APP_CONF
          programArgs += submitRequest.appConf
      }
      val parallelism = getParallelism(submitRequest)
      if (parallelism != null) {
        programArgs += PARAM_KEY_FLINK_PARALLELISM
        programArgs += s"$parallelism"
      }
      providedLibs -> programArgs
    }

    effectiveConfiguration.set(YarnConfigOptions.PROVIDED_LIB_DIRS, providedLibs.asJava)
    //execution.target
    effectiveConfiguration.set(DeploymentOptions.TARGET, YarnDeploymentTarget.PER_JOB.getName)
    //yarn application Type
    effectiveConfiguration.set(YarnConfigOptions.APPLICATION_TYPE, submitRequest.applicationType)
    //arguments...
    effectiveConfiguration.set(ApplicationConfiguration.APPLICATION_ARGS, programArgs.toList.asJava)
    //shutdown-on-attached-exit
    effectiveConfiguration.set(DeploymentOptions.SHUTDOWN_IF_ATTACHED, JavaBool.TRUE)

    logInfo(
      s"""
         |----------------------------------------------------------------------
         |Effective executor configuration: $effectiveConfiguration
         |----------------------------------------------------------------------
         |""".stripMargin)

    effectiveConfiguration
  }

}
