/**
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
package com.streamxhub.flink.submit

import java.io.File
import java.net.{MalformedURLException, URL}
import java.util._

import com.streamxhub.common.conf.ConfigConst._
import com.streamxhub.common.conf.FlinkRunOption
import com.streamxhub.common.util.{DeflaterUtils, HdfsUtils, Logger, PropertiesUtils}
import org.apache.commons.cli._
import org.apache.flink.client.cli.CliFrontend.loadCustomCommandLines
import org.apache.flink.client.cli.CliFrontendParser.SHUTDOWN_IF_ATTACHED_OPTION
import org.apache.flink.client.cli._
import org.apache.flink.client.deployment.DefaultClusterClientServiceLoader
import org.apache.flink.client.deployment.application.ApplicationConfiguration
import org.apache.flink.client.program.{ClusterClient, PackagedProgramUtils}
import org.apache.flink.configuration._
import org.apache.flink.runtime.jobgraph.SavepointRestoreSettings
import org.apache.flink.util.Preconditions.checkNotNull
import org.apache.flink.yarn.configuration.YarnDeploymentTarget
import org.apache.hadoop.fs.Path
import org.apache.flink.api.common.JobID

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.{Failure, Success, Try}
import org.apache.flink.yarn.{YarnClusterClientFactory, YarnClusterDescriptor}
import org.apache.flink.yarn.configuration.YarnConfigOptions
import org.apache.hadoop.yarn.api.records.ApplicationId
import org.apache.flink.client.cli.CliArgsException
import org.apache.flink.util.FlinkException

object FlinkSubmit extends Logger {

  private[this] val optionPrefix = "flink.deployment.option."

  def stop(appId: String, jobId: String, savePoint: String, drain: Boolean): Unit = {
    val flinkConfiguration = new Configuration
    flinkConfiguration.set(YarnConfigOptions.APPLICATION_ID, appId)
    val clusterClientFactory = new YarnClusterClientFactory

    val applicationId = clusterClientFactory.getClusterId(flinkConfiguration)

    if (applicationId == null) {
      throw new FlinkException("[StreamX] flink cancel error: No cluster id was specified. Please specify a cluster to which you would like to connect.")
    }
    val jobID = Try(JobID.fromHexString(jobId)) match {
      case Success(id) => id
      case Failure(e) => throw new CliArgsException(e.getMessage)
    }
    val clusterDescriptor: YarnClusterDescriptor = clusterClientFactory.createClusterDescriptor(flinkConfiguration)
    val clusterClient: ClusterClient[ApplicationId] = clusterDescriptor.retrieve(applicationId).getClusterClient
    clusterClient.stopWithSavepoint(jobID, drain, savePoint)
  }

  def submit(submitInfo: SubmitInfo): ApplicationId = {
    logInfo(
      s"""
         |"[StreamX] flink submit," +
         |      "deployMode: ${submitInfo.deployMode},"
         |      "nameService: ${submitInfo.nameService},"
         |      "appName: ${submitInfo.appName},"
         |      "appConf: ${submitInfo.appConf},"
         |      "savePint: ${submitInfo.savePoint}, "
         |      "userJar: ${submitInfo.flinkUserJar},"
         |      "overrideOption: ${submitInfo.overrideOption.mkString(" ")},"
         |      "dynamicOption": s"${submitInfo.dynamicOption.mkString(" ")},"
         |      "args: ${submitInfo.args}"
         |""".stripMargin)

    val appConfigMap = submitInfo.appConf match {
      case x if x.startsWith("yaml://") =>
        PropertiesUtils.fromYamlText(DeflaterUtils.unzipString(x.drop(7)))
      case x if x.startsWith("prop://") =>
        PropertiesUtils.fromPropertiesText(DeflaterUtils.unzipString(x.drop(7)))
      case x if x.startsWith("hdfs://") =>

        /**
         * 如果配置文件为hdfs方式,则需要用户将hdfs相关配置文件copy到resources下...
         */
        val text = HdfsUtils.read(submitInfo.appConf)
        val extension = submitInfo.appConf.split("\\.").last.toLowerCase
        extension match {
          case "properties" => PropertiesUtils.fromPropertiesText(text)
          case "yml" | "yaml" => PropertiesUtils.fromYamlText(text)
          case _ => throw new IllegalArgumentException("[StreamX] Usage:flink.conf file error,muse be properties or yml")
        }
      case _ => throw new IllegalArgumentException("[StreamX] appConf format error.")
    }

    val appName = if (submitInfo.appName == null) appConfigMap(KEY_FLINK_APP_NAME) else submitInfo.appName
    val appMain = appConfigMap(KEY_FLINK_APP_MAIN)

    /**
     * 获取当前机器上的flink
     */
    val flinkLocalHome = System.getenv("FLINK_HOME")
    require(flinkLocalHome != null)

    logInfo(s"[StreamX] flinkHome: $flinkLocalHome")

    val flinkName = new File(flinkLocalHome).getName

    val flinkHdfsHome = s"$APP_FLINK/$flinkName"

    val flinkHdfsHomeWithNameService = s"hdfs://${submitInfo.nameService}$flinkHdfsHome"

    logInfo(s"[StreamX] flinkHdfsDir: $flinkHdfsHome")

    if (!HdfsUtils.exists(flinkHdfsHome)) {
      logInfo(s"[StreamX] $flinkHdfsHome is not exists,upload beginning....")
      HdfsUtils.upload(flinkLocalHome, flinkHdfsHome)
    }

    //存放flink集群相关的jar包目录
    val flinkHdfsLibs = new Path(s"$flinkHdfsHomeWithNameService/lib")

    val flinkHdfsPlugins = new Path(s"$flinkHdfsHomeWithNameService/plugins")

    val flinkHdfsDistJar = new File(s"$flinkLocalHome/lib").list().filter(_.matches("flink-dist_.*\\.jar")) match {
      case Array() => throw new IllegalArgumentException(s"[StreamX] can no found flink-dist jar in $flinkLocalHome/lib")
      case array if array.length == 1 => s"$flinkHdfsHomeWithNameService/lib/${array.head}"
      case more => throw new IllegalArgumentException(s"[StreamX] found multiple flink-dist jar in $flinkLocalHome/lib,[${more.mkString(",")}]")
    }

    val flinkLocalConfDir = flinkLocalHome.concat("/conf")

    val appArgs = {
      val array = new ArrayBuffer[String]
      Try(submitInfo.args.split("\\s+")).getOrElse(Array()).foreach(x => array += x)
      array += KEY_FLINK_APP_CONF("--")
      array += submitInfo.appConf
      array += KEY_FLINK_HOME("--")
      array += flinkHdfsHomeWithNameService
      array += KEY_APP_NAME("--")
      array += appName
      array.toList.asJava
    }

    //获取flink的配置
    val flinkConfiguration = GlobalConfiguration
      //从flink-conf.yaml中加载默认配置文件...
      .loadConfiguration(flinkLocalConfDir)
      //设置yarn.provided.lib.dirs
      .set(YarnConfigOptions.PROVIDED_LIB_DIRS, Arrays.asList(flinkHdfsLibs.toString, flinkHdfsPlugins.toString))
      //设置flinkDistJar
      .set(YarnConfigOptions.FLINK_DIST_JAR, flinkHdfsDistJar)
      //设置用户的jar
      .set(PipelineOptions.JARS, Collections.singletonList(submitInfo.flinkUserJar))
      //设置为部署模式
      .set(DeploymentOptions.TARGET, YarnDeploymentTarget.APPLICATION.getName)
      //yarn application name
      .set(YarnConfigOptions.APPLICATION_NAME, appName)
      //yarn application Type
      .set(YarnConfigOptions.APPLICATION_TYPE, "StreamX Flink")
      //设置启动主类
      .set(ApplicationConfiguration.APPLICATION_MAIN_CLASS, appMain)
      //设置启动参数
      .set(ApplicationConfiguration.APPLICATION_ARGS, appArgs)

    val customCommandLines = loadCustomCommandLines(flinkConfiguration, flinkLocalConfDir)

    val commandLine = {
      //merge options....
      val customCommandLineOptions = new Options
      for (customCommandLine <- customCommandLines) {
        customCommandLine.addGeneralOptions(customCommandLineOptions)
        customCommandLine.addRunOptions(customCommandLineOptions)
      }
      val commandOptions = CliFrontendParser.getRunCommandOptions
      val commandLineOptions = CliFrontendParser.mergeOptions(commandOptions, customCommandLineOptions)

      //read and verify user config...
      val appArgs = {
        val optionMap = new mutable.HashMap[String, Any]()
        appConfigMap.filter(_._1.startsWith(optionPrefix)).filter(_._2.nonEmpty).filter(x => {
          val key = x._1.drop(optionPrefix.length)
          //验证参数是否合法...
          val verify = commandLineOptions.hasOption(key)
          if (!verify) {
            println(s"[StreamX] param:$key is error,skip it.")
          }
          verify
        }).foreach(x => {
          val opt = commandLineOptions.getOption(x._1.drop(optionPrefix.length).trim).getOpt
          Try(x._2.toBoolean).getOrElse(x._2) match {
            case b if b.isInstanceOf[Boolean] => if (b.asInstanceOf[Boolean]) optionMap += s"-$opt" -> true
            case v => optionMap += s"-$opt" -> v
          }
        })

        //fromSavePoint
        if (submitInfo.savePoint != null) {
          optionMap += s"-${CliFrontendParser.SAVEPOINT_PATH_OPTION.getOpt}" -> submitInfo.savePoint
        }

        val array = new ArrayBuffer[String]()
        optionMap.foreach(x => {
          array += x._1
          if (x._2.isInstanceOf[String]) {
            array += x._2.toString
          }
        })

        //页面定义的参数优先级大于app配置文件
        submitInfo.overrideOption.foreach(x => array += x)

        //-D
        submitInfo.dynamicOption.foreach(x => array += x.replaceFirst("^-D|^", "-D"))


        array.toArray

      }
      CliFrontendParser.parse(commandLineOptions, appArgs, true)
    }

    def validateAndGetActiveCommandLine(): CustomCommandLine = {
      val line = checkNotNull(commandLine)
      println("Custom commandlines: {}", customCommandLines)
      for (cli <- customCommandLines) {
        println("Checking custom commandline {}, isActive: {}", cli, cli.isActive(line))
        if (cli.isActive(line)) return cli
      }
      throw new IllegalStateException("No valid command-line found.")
    }

    val activeCommandLine = validateAndGetActiveCommandLine()

    val uri = PackagedProgramUtils.resolveURI(submitInfo.flinkUserJar)

    val effectiveConfiguration = getEffectiveConfiguration(activeCommandLine, commandLine, Collections.singletonList(uri.toString))

    val clusterClientServiceLoader = new DefaultClusterClientServiceLoader
    val clientFactory = clusterClientServiceLoader.getClusterClientFactory[ApplicationId](effectiveConfiguration)
    val applicationConfiguration = ApplicationConfiguration.fromConfiguration(effectiveConfiguration)
    var applicationId: ApplicationId = null
    try {
      val clusterDescriptor = clientFactory.createClusterDescriptor(effectiveConfiguration)
      try {
        val clusterSpecification = clientFactory.getClusterSpecification(effectiveConfiguration)
        println("------------------<<specification>>------------------")
        println(clusterSpecification)
        println("------------------------------------")
        val clusterClient: ClusterClient[ApplicationId] = clusterDescriptor.deployApplicationCluster(clusterSpecification, applicationConfiguration).getClusterClient
        applicationId = clusterClient.getClusterId
        println("------------------<<applicationId>>------------------")
        println()
        println("Flink Job Started: applicationId: " + applicationId)
        println()
        println("------------------------------------")
      } finally if (clusterDescriptor != null) clusterDescriptor.close()
      applicationId
    }

  }

  /**
   *
   * @param activeCustomCommandLine
   * @param commandLine
   * @param jobJars
   * @tparam T
   * @throws
   * @return
   */
  @throws[FlinkException] private def getEffectiveConfiguration[T](activeCustomCommandLine: CustomCommandLine, commandLine: CommandLine, jobJars: List[String]) = {
    val configuration = new Configuration

    val classpath = new ArrayBuffer[URL]
    if (commandLine.hasOption(FlinkRunOption.CLASSPATH_OPTION.getOpt)) {
      for (path <- commandLine.getOptionValues(FlinkRunOption.CLASSPATH_OPTION.getOpt)) {
        try classpath.add(new URL(path)) catch {
          case e: MalformedURLException => throw new CliArgsException(s"[StreamX]Bad syntax for classpath:$path,err:$e")
        }
      }
    }

    ConfigUtils.encodeCollectionToConfig(configuration, PipelineOptions.CLASSPATHS, classpath, new function.Function[URL, String] {
      override def apply(t: URL): String = t.toString
    })

    if (commandLine.hasOption(FlinkRunOption.PARALLELISM_OPTION.getOpt)) {
      val parString = commandLine.getOptionValue(FlinkRunOption.PARALLELISM_OPTION.getOpt)
      try {
        val parallelism = parString.toInt
        if (parallelism <= 0) {
          throw new NumberFormatException("[StreamX] parallelism muse be > 0. ")
        }
        configuration.setInteger(CoreOptions.DEFAULT_PARALLELISM, parallelism)
      } catch {
        case e: NumberFormatException => throw new CliArgsException(s"The parallelism must be a positive number: $parString,err:$e ")
      }
    }

    val detachedMode = commandLine.hasOption(FlinkRunOption.DETACHED_OPTION.getOpt) || commandLine.hasOption(FlinkRunOption.YARN_DETACHED_OPTION.getOpt)
    val shutdownOnAttachedExit = commandLine.hasOption(SHUTDOWN_IF_ATTACHED_OPTION.getOpt)
    val savepointSettings = CliFrontendParser.createSavepointRestoreSettings(commandLine)
    configuration.setBoolean(DeploymentOptions.ATTACHED, !detachedMode)
    configuration.setBoolean(DeploymentOptions.SHUTDOWN_IF_ATTACHED, shutdownOnAttachedExit)

    SavepointRestoreSettings.toConfiguration(savepointSettings, configuration)
    ConfigUtils.encodeCollectionToConfig(configuration, PipelineOptions.JARS, jobJars, new function.Function[String, String] {
      override def apply(t: String): String = t
    })

    val executorConfig = checkNotNull(activeCustomCommandLine).applyCommandLineOptionsToConfiguration(commandLine)
    val effectiveConfiguration = new Configuration(executorConfig)

    effectiveConfiguration.addAll(configuration)

    commandLine.getOptionValue(FlinkRunOption.YARN_JMMEMORY_OPTION.getOpt) match {
      case null =>
      case jmm => effectiveConfiguration.setString(JobManagerOptions.TOTAL_PROCESS_MEMORY.key(), jmm)
    }

    commandLine.getOptionValue(FlinkRunOption.YARN_TMMEMORY_OPTION.getOpt) match {
      case null =>
      case tmm => effectiveConfiguration.setString(TaskManagerOptions.TOTAL_PROCESS_MEMORY.key(), tmm)
    }

    commandLine.getOptionValue(FlinkRunOption.PARALLELISM_OPTION.getOpt) match {
      case null =>
      case parallelism => effectiveConfiguration.setString(CoreOptions.DEFAULT_PARALLELISM.key(), parallelism)
    }

    commandLine.getOptionValue(FlinkRunOption.YARN_SLOTS_OPTION.getOpt) match {
      case null =>
      case slot => effectiveConfiguration.setString(TaskManagerOptions.NUM_TASK_SLOTS.key(), slot)
    }

    /**
     * dynamicOption(Highest priority...)
     */
    val properties = commandLine.getOptionProperties(FlinkRunOption.YARN_DYNAMIC_OPTION.getOpt)
    properties.stringPropertyNames.foreach((key: String) => {
      val value = properties.getProperty(key)
      if (value != null) {
        effectiveConfiguration.setString(key, value)
      } else {
        effectiveConfiguration.setString(key, "true")
      }
    })

    println("-----------------------")
    println("Effective executor configuration: ", effectiveConfiguration)
    println("-----------------------")
    effectiveConfiguration
  }

}

