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
package com.streamxhub.streamx.flink.submit.test

import com.streamxhub.streamx.common.util.Logger
import com.streamxhub.streamx.flink.core.scala.conf.FlinkRunOption
import com.streamxhub.streamx.flink.submit.SubmitResponse
import org.apache.commons.cli.Options
import org.apache.flink.client.cli.CliFrontend.loadCustomCommandLines
import org.apache.flink.client.cli.{CliFrontendParser, CustomCommandLine}
import org.apache.flink.client.deployment.application.ApplicationConfiguration
import org.apache.flink.client.deployment.{ClusterSpecification, DefaultClusterClientServiceLoader}
import org.apache.flink.client.program.{ClusterClientProvider, PackagedProgram, PackagedProgramUtils}
import org.apache.flink.configuration.MemorySize.MemoryUnit
import org.apache.flink.configuration._
import org.apache.flink.runtime.jobgraph.JobGraph
import org.apache.flink.util.Preconditions.checkNotNull
import org.apache.flink.yarn.YarnClusterDescriptor
import org.apache.flink.yarn.configuration.YarnDeploymentTarget
import org.apache.flink.yarn.entrypoint.YarnJobClusterEntrypoint
import org.apache.hadoop.fs.{Path => HadoopPath}
import org.apache.hadoop.yarn.api.records.ApplicationId

import java.io.File
import java.lang.reflect.Method
import java.util
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

/**
 * perJob 编程方式提交任务,
 */
object YarnPreJobTestCase extends Logger {

  /**
   * 必须要在本机安装部署flink,并且配置FLINK_HOME
   */
  lazy val FLINK_HOME = {
    val flinkLocalHome = System.getenv("FLINK_HOME")
    logInfo(s"flinkHome: $flinkLocalHome")
    flinkLocalHome
  }

  /**
   * SocketWindowWordCount.jar 在flink/examples下载自带的示例程序
   */
  val userJar = s"$FLINK_HOME/examples/streaming/SocketWindowWordCount.jar"
  /**
   * 运行该程序需要传入的参数
   */
  val programArgs = "--hostname localhost --port 9999"

  /**
   * 运行指定的option参数
   */
  val option = "-e yarn-pre-job -p 2 -n"

  lazy val flinkDefaultConfiguration: Configuration = {
    require(FLINK_HOME != null)
    //获取flink的配置
    GlobalConfiguration.loadConfiguration(s"$FLINK_HOME/conf")
  }

  lazy val customCommandLines: util.List[CustomCommandLine] = {
    // 1. find the configuration directory
    val configurationDirectory = s"$FLINK_HOME/conf"
    // 2. load the custom command lines
    loadCustomCommandLines(flinkDefaultConfiguration, configurationDirectory)
  }

  /**
   * 瞒天过海,偷天换日,鱼目混珠.
   * 反射出YarnClusterDescriptor的私有方法deployInternal,
   * 主要是为了传入applicationName,原始该方法里applicationName是写死的.
   */
  lazy val deployInternalMethod: Method = {
    val paramClass = Array(
      classOf[ClusterSpecification],
      classOf[String],
      classOf[String],
      classOf[JobGraph],
      Boolean2boolean(true).getClass
    )
    val deployInternal = classOf[YarnClusterDescriptor].getDeclaredMethod("deployInternal", paramClass: _*)
    deployInternal.setAccessible(true)
    deployInternal
  }

  def deployInternal(clusterDescriptor: YarnClusterDescriptor,
                     clusterSpecification: ClusterSpecification,
                     applicationName: String,
                     yarnClusterEntrypoint: String,
                     jobGraph: JobGraph,
                     detached: java.lang.Boolean): ClusterClientProvider[ApplicationId] = {
    deployInternalMethod.invoke(
      clusterDescriptor,
      clusterSpecification,
      applicationName,
      yarnClusterEntrypoint,
      jobGraph,
      detached
    ).asInstanceOf[ClusterClientProvider[ApplicationId]]
  }

  def main(args: Array[String]): Unit = {

    val commandLine = {
      val customCommandLineOptions = new Options
      for (customCommandLine <- customCommandLines) {
        customCommandLine.addGeneralOptions(customCommandLineOptions)
        customCommandLine.addRunOptions(customCommandLineOptions)
      }
      val commandLineOptions = FlinkRunOption.mergeOptions(CliFrontendParser.getRunCommandOptions, customCommandLineOptions)
      val cliArgs = option.split("\\s+")
      FlinkRunOption.parse(commandLineOptions, cliArgs, true)
    }

    val activeCommandLine = {
      val line = checkNotNull(commandLine)
      println("Custom commandlines: {}", customCommandLines)
      customCommandLines.filter(_.isActive(line)).get(0)
    }

    val executorConfig = checkNotNull(activeCommandLine).toConfiguration(commandLine)
    val flinkConfig = new Configuration(executorConfig)
    flinkConfig.set(DeploymentOptions.TARGET, YarnDeploymentTarget.PER_JOB.getName)
    flinkConfig.set(ApplicationConfiguration.APPLICATION_ARGS, programArgs.split("\\s+").toList.asJava)
    flinkConfig.set(JobManagerOptions.TOTAL_FLINK_MEMORY, MemorySize.parse("1024", MemoryUnit.MEGA_BYTES))
    flinkConfig.set(TaskManagerOptions.TOTAL_FLINK_MEMORY, MemorySize.parse("1024", MemoryUnit.MEGA_BYTES))

    val clusterClientServiceLoader = new DefaultClusterClientServiceLoader
    val clientFactory = clusterClientServiceLoader.getClusterClientFactory[ApplicationId](flinkConfig)

    val clusterDescriptor = {
      val clusterDescriptor = clientFactory.createClusterDescriptor(flinkConfig).asInstanceOf[YarnClusterDescriptor]
      val flinkDistJar = new File(s"$FLINK_HOME/lib").list().filter(_.matches("flink-dist_.*\\.jar")) match {
        case Array() => throw new IllegalArgumentException(s"[StreamX] can no found flink-dist jar in $FLINK_HOME/lib")
        case array if array.length == 1 => s"$FLINK_HOME/lib/${array.head}"
        case more => throw new IllegalArgumentException(s"[StreamX] found multiple flink-dist jar in $FLINK_HOME/lib,[${more.mkString(",")}]")
      }
      clusterDescriptor.setLocalJarPath(new HadoopPath(flinkDistJar))
      clusterDescriptor
    }

    try {
      val clusterClient = {
        val clusterSpecification = clientFactory.getClusterSpecification(flinkConfig)
        logInfo("------------------<<specification>>------------------")
        logInfo(s"$clusterSpecification")
        logInfo("------------------------------------")
        val packagedProgram = PackagedProgram
          .newBuilder
          .setJarFile(new File(userJar))
          .setArguments(programArgs.split("\\s"): _*)
          .build
        val jobGraph = PackagedProgramUtils.createJobGraph(
          packagedProgram,
          flinkConfig,
          1,
          false
        )
        logInfo("------------------<<jobId>>------------------")
        logger.info(s"${jobGraph.getJobID.toString}")
        logInfo("------------------------------------")

        deployInternal(
          clusterDescriptor,
          clusterSpecification,
          "MyJob",
          classOf[YarnJobClusterEntrypoint].getName,
          jobGraph,
          false
        ).getClusterClient
      }
      val applicationId = clusterClient.getClusterId
      logInfo("------------------<<applicationId>>-------------------")
      logInfo(s"Flink Job Started: applicationId: $applicationId ")
      logInfo("-------------------------------------")
      SubmitResponse(applicationId.toString, flinkConfig)
    } finally if (clusterDescriptor != null) {
      clusterDescriptor.close()
    }
  }

}
