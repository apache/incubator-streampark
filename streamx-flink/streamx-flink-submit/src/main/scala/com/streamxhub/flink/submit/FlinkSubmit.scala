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
import java.util.concurrent.{CompletableFuture, TimeUnit}

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
import org.apache.flink.configuration.ConfigOptions
import java.lang.{Boolean => JBool}

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.flink.util.{ExceptionUtils, FlinkException}

object FlinkSubmit extends Logger {

  private[this] val optionPrefix = "flink.deployment.option."

  private[this] var flinkDefaultConfiguration: Configuration = null

  private[this] def getClusterClientByApplicationId(appId: String): ClusterClient[ApplicationId] = {
    val flinkConfiguration = new Configuration
    flinkConfiguration.set(YarnConfigOptions.APPLICATION_ID, appId)
    val clusterClientFactory = new YarnClusterClientFactory
    val applicationId = clusterClientFactory.getClusterId(flinkConfiguration)
    if (applicationId == null) {
      throw new FlinkException("[StreamX] getClusterClient error. No cluster id was specified. Please specify a cluster to which you would like to connect.")
    }
    val clusterDescriptor: YarnClusterDescriptor = clusterClientFactory.createClusterDescriptor(flinkConfiguration)
    val clusterClient: ClusterClient[ApplicationId] = clusterDescriptor.retrieve(applicationId).getClusterClient
    clusterClient
  }

  private[this] def getJobID(jobId: String): JobID = {
    Try(JobID.fromHexString(jobId)) match {
      case Success(id) => id
      case Failure(e) => throw new CliArgsException(e.getMessage)
    }
  }

  private[this] def getOptionFromDefaultFlinkConfig[T](option: ConfigOption[T]): T = {
    if (flinkDefaultConfiguration == null) {
      val flinkLocalHome = System.getenv("FLINK_HOME")
      require(flinkLocalHome != null)
      val flinkLocalConfDir = flinkLocalHome.concat("/conf")
      //获取flink的配置
      this.flinkDefaultConfiguration = GlobalConfiguration.loadConfiguration(flinkLocalConfDir)
    }
    flinkDefaultConfiguration.get(option)
  }

  private[this] def getSavePointDir(): String = getOptionFromDefaultFlinkConfig(
    ConfigOptions.key(CheckpointingOptions.SAVEPOINT_DIRECTORY.key())
      .stringType()
      .defaultValue(s"${HdfsUtils.getDefaultFS}$APP_SAVEPOINTS")
  )

  def stop(appId: String, jobStringId: String, savePoint: JBool, drain: JBool): String = {
    val jobID = getJobID(jobStringId)
    val clusterClient: ClusterClient[ApplicationId] = getClusterClientByApplicationId(appId)
    val savePointDir = getSavePointDir()

    val savepointPathFuture: CompletableFuture[String] = (Try(savePoint.booleanValue()).getOrElse(false), Try(drain.booleanValue()).getOrElse(false)) match {
      case (false, false) =>
        clusterClient.cancel(jobID)
        null
      case (true, false) => clusterClient.cancelWithSavepoint(jobID, savePointDir)
      case (_, _) => clusterClient.stopWithSavepoint(jobID, drain, savePointDir)
    }

    if (savepointPathFuture == null) null else {
      try {
        val clientTimeout = getOptionFromDefaultFlinkConfig(ClientOptions.CLIENT_TIMEOUT)
        savepointPathFuture.get(clientTimeout.toMillis(), TimeUnit.MILLISECONDS)
      } catch {
        case e: Exception =>
          val cause = ExceptionUtils.stripExecutionException(e)
          throw new FlinkException(s"[StreamX] Triggering a savepoint for the job $jobStringId failed. $cause");
      }
    }
  }

  /**
   * 加载streamx的plugins到flink下的plugins下.
   *
   * @param pluginPath
   */
  private[this] def loadPlugins(pluginPath: String) = {
    logInfo("[StreamX] loadPlugins starting...")
    val appHome = System.getProperty("app.home")
    val streamXPlugins = new File(appHome, "plugins")
    streamXPlugins.listFiles().foreach(x => {
      if (!HdfsUtils.exists(s"$pluginPath/${x.getName}")) {
        logInfo(s"[StreamX] load plugin:${x.getName} to $pluginPath")
        HdfsUtils.upload(x.getAbsolutePath, pluginPath)
      }
    })
  }

  @throws[Exception] def submit(submitInfo: SubmitInfo): ApplicationId = {
    logInfo(
      s"""
         |"[StreamX] flink submit," +
         |      "appName: ${submitInfo.appName},"
         |      "appConf: ${submitInfo.appConf},"
         |      "applicationType: ${submitInfo.applicationType},"
         |      "savePint: ${submitInfo.savePoint}, "
         |      "userJar: ${submitInfo.flinkUserJar},"
         |      "overrideOption: ${submitInfo.overrideOption.mkString(" ")},"
         |      "dynamicOption": s"${submitInfo.dynamicOption.mkString(" ")},"
         |      "args: ${submitInfo.args}"
         |""".stripMargin)

    val appConfigMap = submitInfo.appConf match {
      case x if x.trim.startsWith("yaml://") =>
        PropertiesUtils.fromYamlText(DeflaterUtils.unzipString(x.trim.drop(7)))
      case x if x.trim.startsWith("prop://") =>
        PropertiesUtils.fromPropertiesText(DeflaterUtils.unzipString(x.trim.drop(7)))
      case x if x.trim.startsWith("hdfs://") =>

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
      case x if x.trim.startsWith("json://") =>
        val json = x.trim.drop(7)
        val map = new ObjectMapper().readValue[Map[String, String]](json, classOf[Map[String, String]])
        map.toMap
      case _ => throw new IllegalArgumentException("[StreamX] appConf format error.")
    }

    val appName = if (submitInfo.appName == null) appConfigMap(KEY_FLINK_APP_NAME) else submitInfo.appName
    val appMain = appConfigMap(KEY_FLINK_APP_MAIN)

    /**
     * init config....
     */
    val flinkLocalHome = System.getenv("FLINK_HOME")
    require(flinkLocalHome != null)

    logInfo(s"[StreamX] flinkHome: $flinkLocalHome")

    val flinkName = new File(flinkLocalHome).getName
    val flinkHdfsHome = s"$APP_FLINK/$flinkName"

    if (!HdfsUtils.exists(flinkHdfsHome)) {
      logInfo(s"[StreamX] $flinkHdfsHome is not exists,upload beginning....")
      HdfsUtils.upload(flinkLocalHome, flinkHdfsHome)
    }

    val flinkHdfsHomeWithNameService = s"${HdfsUtils.getDefaultFS}$flinkHdfsHome"
    val flinkLocalConfDir = flinkLocalHome.concat("/conf")
    //存放flink集群相关的jar包目录
    val flinkHdfsLibs = new Path(s"$flinkHdfsHomeWithNameService/lib")
    val flinkHdfsPlugins = new Path(s"$flinkHdfsHomeWithNameService/plugins")
    //加载streamx下的plugins到$FLINK_HOME/plugins下
    loadPlugins(flinkHdfsPlugins.toString)

    val customCommandLines = {

      val flinkHdfsDistJar = new File(s"$flinkLocalHome/lib").list().filter(_.matches("flink-dist_.*\\.jar")) match {
        case Array() => throw new IllegalArgumentException(s"[StreamX] can no found flink-dist jar in $flinkLocalHome/lib")
        case array if array.length == 1 => s"$flinkHdfsHomeWithNameService/lib/${array.head}"
        case more => throw new IllegalArgumentException(s"[StreamX] found multiple flink-dist jar in $flinkLocalHome/lib,[${more.mkString(",")}]")
      }

      val appArgs: ArrayBuffer[String] = {
        val array = new ArrayBuffer[String]
        Try(submitInfo.args.split("\\s+")).getOrElse(Array()).foreach(x => array += x)
        array += KEY_FLINK_APP_CONF("--")
        array += submitInfo.appConf
        array += KEY_FLINK_HOME("--")
        array += flinkHdfsHomeWithNameService
        array += KEY_APP_NAME("--")
        array += appName
      }

      if (submitInfo.overrideOption.containsKey("parallelism")) {
        appArgs += s"--$KEY_FLINK_PARALLELISM"
        appArgs += submitInfo.overrideOption.get("parallelism").toString
      }

      //获取flink的配置
      val runConfiguration = GlobalConfiguration
        //从flink-conf.yaml中加载默认配置文件...
        .loadConfiguration(flinkLocalConfDir)
        //设置yarn.provided.lib.dirs
        .set(YarnConfigOptions.PROVIDED_LIB_DIRS, Arrays.asList(flinkHdfsLibs.toString, flinkHdfsPlugins.toString))
        //设置flinkDistJar
        .set(YarnConfigOptions.FLINK_DIST_JAR, flinkHdfsDistJar)
        //设置用户的jar
        .set(PipelineOptions.JARS, Collections.singletonList(submitInfo.flinkUserJar))
        //设置部署模式为"application"
        .set(DeploymentOptions.TARGET, YarnDeploymentTarget.APPLICATION.getName)
        //yarn application name
        .set(YarnConfigOptions.APPLICATION_NAME, appName)
        //yarn application Type
        .set(YarnConfigOptions.APPLICATION_TYPE, submitInfo.applicationType)
        //设置启动主类
        .set(ApplicationConfiguration.APPLICATION_MAIN_CLASS, appMain)
        //设置启动参数
        .set(ApplicationConfiguration.APPLICATION_ARGS, appArgs.toList.asJava)

      loadCustomCommandLines(runConfiguration, flinkLocalConfDir)
    }

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

        optionMap += "-ynm" -> submitInfo.appName

        //页面定义的参数优先级大于app配置文件
        submitInfo.overrideOption.filter(x => commandLineOptions.hasLongOption(x._1)).foreach(x => {
          val option = commandLineOptions.getOption(x._1)
          if (option.hasArg) {
            optionMap += s"-${option.getOpt}" -> x._2.toString
          } else {
            optionMap += s"-${option.getOpt}" -> true
          }
        })

        val array = new ArrayBuffer[String]()
        optionMap.foreach(x => {
          array += x._1
          if (x._2.isInstanceOf[String]) {
            array += x._2.toString
          }
        })

        //-D 动态参数配置....
        submitInfo.dynamicOption.foreach(x => array += x.replaceFirst("^-D|^", "-D"))

        //-jvm profile support
        array += "-Denv.java.opts=-javaagent:jvm-profiler-1.0.0.jar=sampleInterval=50"

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

    commandLine.getOptionValue(FlinkRunOption.PARALLELISM_OPTION.getOpt) match {
      case null =>
      case p =>
        Try(p.toInt) match {
          case Success(value) =>
            if (value <= 0) {
              throw new NumberFormatException("[StreamX] parallelism muse be > 0. ")
            }
            configuration.setInteger(CoreOptions.DEFAULT_PARALLELISM.key(), value)
          case Failure(e) => throw new CliArgsException(s"The parallelism must be a positive number: $p,err:$e ")
        }
    }

    commandLine.getOptionValue(FlinkRunOption.YARN_SLOTS_OPTION.getOpt) match {
      case null =>
      case s =>
        Try(s.toInt) match {
          case Success(value) =>
            if (value <= 0) {
              throw new NumberFormatException("[StreamX] slot muse be > 0. ")
            }
            configuration.setInteger(TaskManagerOptions.NUM_TASK_SLOTS.key(), value)
          case Failure(e) => throw new CliArgsException(s"The slot must be a positive number: $s,err:$e ")
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
      case jmm => effectiveConfiguration.setString(JobManagerOptions.TOTAL_PROCESS_MEMORY.key(), jmm.trim.replaceFirst("(M$|$)", "M"))
    }

    commandLine.getOptionValue(FlinkRunOption.YARN_TMMEMORY_OPTION.getOpt) match {
      case null =>
      case tmm => effectiveConfiguration.setString(TaskManagerOptions.TOTAL_PROCESS_MEMORY.key(), tmm.trim.replaceFirst("(M$|$)", "M"))
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
        effectiveConfiguration.setBoolean(key, true)
      }
    })

    println("-----------------------")
    println("Effective executor configuration: ", effectiveConfiguration)
    println("-----------------------")
    effectiveConfiguration
  }

}

