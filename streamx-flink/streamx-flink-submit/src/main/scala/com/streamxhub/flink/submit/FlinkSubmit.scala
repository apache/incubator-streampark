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
package com.streamxhub.flink.submit

import com.fasterxml.jackson.databind.ObjectMapper
import com.streamxhub.common.conf.ConfigConst._
import com.streamxhub.common.util._
import org.apache.commons.cli.{CommandLine, Options}
import org.apache.flink.api.common.JobID
import org.apache.flink.client.cli.CliFrontend.loadCustomCommandLines
import org.apache.flink.client.cli._
import org.apache.flink.client.deployment.DefaultClusterClientServiceLoader
import org.apache.flink.client.deployment.application.ApplicationConfiguration
import org.apache.flink.client.program.{ClusterClient, PackagedProgramUtils}
import org.apache.flink.configuration._
import org.apache.flink.util.FlinkException
import org.apache.flink.util.Preconditions.checkNotNull
import org.apache.flink.yarn.configuration.{YarnConfigOptions, YarnDeploymentTarget}
import org.apache.flink.yarn.{YarnClusterClientFactory, YarnClusterDescriptor}
import org.apache.hadoop.fs.Path
import org.apache.hadoop.yarn.api.records.ApplicationId

import java.io.{File, Serializable}
import java.lang.{Boolean => JavaBool}
import java.util.concurrent.{CompletableFuture, TimeUnit}
import java.util.{Collections, Arrays => JavaArrays, List => JavaList, Map => JavaMap}
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.{Failure, Success, Try}

object FlinkSubmit extends Logger {

  private[this] var flinkDefaultConfiguration: Configuration = _

  private[this] var jvmProfilerJar: String = _

  private[this] lazy val configurationMap = new mutable.HashMap[String, Configuration]()

  private[this] lazy val FLINK_HOME: String = {
    val flinkLocalHome = System.getenv("FLINK_HOME")
    logInfo(s"[StreamX] flinkHome: $flinkLocalHome")
    flinkLocalHome
  }

  //----------Public Method------------------
  @throws[Exception] def submit(submitInfo: SubmitInfo): ApplicationId = {
    logInfo(
      s"""
         |"[StreamX] flink submit," +
         |      "appName: ${submitInfo.appName},"
         |      "appConf: ${submitInfo.appConf},"
         |      "applicationType: ${submitInfo.applicationType},"
         |      "savePint: ${submitInfo.savePoint}, "
         |      "flameGraph": ${submitInfo.flameGraph != null}, "
         |      "userJar: ${submitInfo.flinkUserJar},"
         |      "option: ${submitInfo.option},"
         |      "dynamicOption": ${submitInfo.dynamicOption.mkString(" ")},"
         |      "args: ${submitInfo.args}"
         |""".stripMargin)


    val customCommandLines: JavaList[CustomCommandLine] = {
      val configurationDirectory = s"$FLINK_HOME/conf"
      val globalConfiguration = GlobalConfiguration.loadConfiguration(configurationDirectory)
      loadCustomCommandLines(globalConfiguration, configurationDirectory)
    }

    val commandLine = getEffectiveCommandLine(submitInfo, customCommandLines)

    val activeCommandLine = validateAndGetActiveCommandLine(customCommandLines, commandLine)

    val uri = PackagedProgramUtils.resolveURI(submitInfo.flinkUserJar)
    val effectiveConfiguration = getEffectiveConfiguration(submitInfo, activeCommandLine, commandLine, Collections.singletonList(uri.toString))
    val applicationConfiguration = ApplicationConfiguration.fromConfiguration(effectiveConfiguration)

    var applicationId: ApplicationId = null
    val clusterClientServiceLoader = new DefaultClusterClientServiceLoader
    val clientFactory = clusterClientServiceLoader.getClusterClientFactory[ApplicationId](effectiveConfiguration)
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
    } finally if (clusterDescriptor != null) {
      clusterDescriptor.close()
    }
    configurationMap.put(applicationId.toString, effectiveConfiguration)
    applicationId
  }

  def stop(appId: String, jobStringId: String, savePoint: JavaBool, drain: JavaBool): String = {
    val jobID = getJobID(jobStringId)
    val clusterClient: ClusterClient[ApplicationId] = getClusterClientByApplicationId(appId)
    val savePointDir = getOptionFromDefaultFlinkConfig(
      ConfigOptions.key(CheckpointingOptions.SAVEPOINT_DIRECTORY.key())
        .stringType()
        .defaultValue(s"${HdfsUtils.getDefaultFS}$APP_SAVEPOINTS")
    )

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
        savepointPathFuture.get(clientTimeout.toMillis, TimeUnit.MILLISECONDS)
      } catch {
        case e: Exception =>
          val cause = ExceptionUtils.stringifyException(e)
          throw new FlinkException(s"[StreamX] Triggering a savepoint for the job $jobStringId failed. $cause");
      }
    }
  }

  def getSubmitedConfiguration(appId: ApplicationId): Configuration = configurationMap.remove(appId.toString).orNull

  //----------Public Method end ------------------

  private[this] def getConfigMapFromSubmit(submitInfo: SubmitInfo, prefix: String = ""): Map[String, String] = {
    val map = submitInfo.appConf match {
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
        new ObjectMapper().readValue[JavaMap[String, String]](json, classOf[JavaMap[String, String]]).toMap
      case _ => throw new IllegalArgumentException("[StreamX] appConf format error.")
    }
    prefix match {
      case "" | null => map
      case other => map
        .filter(_._1.startsWith(other)).filter(_._2.nonEmpty)
        .map(x => x._1.drop(other.length) -> x._2)
    }
  }

  private[this] def validateAndGetActiveCommandLine(customCommandLines: JavaList[CustomCommandLine], commandLine: CommandLine): CustomCommandLine = {
    val line = checkNotNull(commandLine)
    println("Custom commandlines: {}", customCommandLines)
    for (cli <- customCommandLines) {
      val isActive = cli.isActive(line)
      println("Checking custom commandline {}, isActive: {}", cli, isActive)
      if (isActive) return cli
    }
    throw new IllegalStateException("No valid command-line found.")
  }

  private[this] def getEffectiveCommandLine(submitInfo: SubmitInfo, customCommandLines: JavaList[CustomCommandLine]): CommandLine = {
    val appConfigMap = getConfigMapFromSubmit(submitInfo, KEY_FLINK_DEPLOYMENT_OPTION_PREFIX)
    //merge options....
    val customCommandLineOptions = new Options
    for (customCommandLine <- customCommandLines) {
      customCommandLine.addGeneralOptions(customCommandLineOptions)
      customCommandLine.addRunOptions(customCommandLineOptions)
    }
    val commandOptions = CliFrontendParser.getRunCommandOptions
    val commandLineOptions = CliFrontendParser.mergeOptions(commandOptions, customCommandLineOptions)

    //read and verify user config...
    val cliArgs = {
      val optionMap = new mutable.HashMap[String, Any]()
      appConfigMap.filter(x => {
        //验证参数是否合法...
        val verify = commandLineOptions.hasOption(x._1)
        if (!verify) {
          println(s"[StreamX] param:${x._1} is error,skip it.")
        }
        verify
      }).foreach(x => {
        val opt = commandLineOptions.getOption(x._1.trim).getOpt
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
        x._2 match {
          case v: String => array += v
          case _ =>
        }
      })

      //-jvm profile support
      if (submitInfo.flameGraph != null) {
        //find jvm-profiler
        if (jvmProfilerJar == null) {
          val pluginsPath = System.getProperty("app.home").concat("/plugins")
          val jvmProfilerPlugin = new File(pluginsPath, "jvm-profiler")
          jvmProfilerJar = jvmProfilerPlugin.list().filter(_.matches("jvm-profiler-.*\\.jar")) match {
            case Array() => throw new IllegalArgumentException(s"[StreamX] can no found jvm-profiler jar in $pluginsPath")
            case array if array.length == 1 => array.head
            case more => throw new IllegalArgumentException(s"[StreamX] found multiple jvm-profiler jar in $pluginsPath,[${more.mkString(",")}]")
          }
        }

        val buffer = new StringBuffer()
        submitInfo.flameGraph.foreach(p => buffer.append(s"${p._1}=${p._2},"))
        val param = buffer.toString.dropRight(1)
        array += s"-Denv.java.opts.taskmanager=-javaagent:$$PWD/plugins/jvm-profiler/$jvmProfilerJar=$param"
      }

      //页面定义的参数优先级大于app配置文件
      submitInfo.option.split("\\s").foreach(x => array += x)
      //属性参数...
      submitInfo.property.foreach(x => array += s"-D${x._1}=${x._2}")
      //-D 其他动态参数配置....
      submitInfo.dynamicOption.foreach(x => array += x.replaceFirst("^-D|^", "-D"))

      array.toArray
    }

    CliFrontendParser.parse(commandLineOptions, cliArgs, true)

  }

  @throws[FlinkException] private def getEffectiveConfiguration[T](submitInfo: SubmitInfo, activeCustomCommandLine: CustomCommandLine, commandLine: CommandLine, jobJars: JavaList[String]): Configuration = {
    val executorConfig = checkNotNull(activeCustomCommandLine).toConfiguration(commandLine)
    val effectiveConfiguration = new Configuration(executorConfig)
    val programOptions = ProgramOptions.create(commandLine)
    val executionParameters = ExecutionConfigAccessor.fromProgramOptions(programOptions, jobJars)
    executionParameters.applyToConfiguration(effectiveConfiguration)

    val flinkLocalHome = FLINK_HOME
    val flinkName = new File(flinkLocalHome).getName
    val flinkHdfsHome = s"${HdfsUtils.getDefaultFS}$APP_FLINK/$flinkName"
    val flinkHdfsLibs = new Path(s"$flinkHdfsHome/lib")
    val flinkHdfsPlugins = new Path(s"$flinkHdfsHome/plugins")

    val flinkHdfsDistJar = new File(s"$flinkLocalHome/lib").list().filter(_.matches("flink-dist_.*\\.jar")) match {
      case Array() => throw new IllegalArgumentException(s"[StreamX] can no found flink-dist jar in $flinkLocalHome/lib")
      case array if array.length == 1 => s"$flinkHdfsHome/lib/${array.head}"
      case more => throw new IllegalArgumentException(s"[StreamX] found multiple flink-dist jar in $flinkLocalHome/lib,[${more.mkString(",")}]")
    }

    val appConfigMap = getConfigMapFromSubmit(submitInfo, KEY_FLINK_DEPLOYMENT_PROPERTY_PREFIX)
    val appName = if (submitInfo.appName == null) appConfigMap(KEY_FLINK_APP_NAME) else submitInfo.appName
    val appMain = appConfigMap(ApplicationConfiguration.APPLICATION_MAIN_CLASS.key())

    val programArgs = new ArrayBuffer[String]()
    Try(submitInfo.args.split("\\s+")).getOrElse(Array()).foreach(x => programArgs += x)
    programArgs += KEY_FLINK_CONF("--")
    programArgs += submitInfo.appConf
    programArgs += KEY_FLINK_HOME("--")
    programArgs += flinkHdfsHome
    programArgs += KEY_APP_NAME("--")
    programArgs += appName
    if (submitInfo.property.containsKey(KEY_FLINK_PARALLELISM)) {
      programArgs += s"--$KEY_FLINK_PARALLELISM"
      programArgs += submitInfo.property.get(KEY_FLINK_PARALLELISM).toString
    }

    //yarn.provided.lib.dirs
    effectiveConfiguration.set(YarnConfigOptions.PROVIDED_LIB_DIRS, JavaArrays.asList(flinkHdfsLibs.toString, flinkHdfsPlugins.toString))
    //flinkDistJar
    effectiveConfiguration.set(YarnConfigOptions.FLINK_DIST_JAR, flinkHdfsDistJar)
    //设置用户的jar
    effectiveConfiguration.set(PipelineOptions.JARS, Collections.singletonList(submitInfo.flinkUserJar))
    //设置部署模式为"application"
    effectiveConfiguration.set(DeploymentOptions.TARGET, YarnDeploymentTarget.APPLICATION.getName)
    //yarn application name
    effectiveConfiguration.set(YarnConfigOptions.APPLICATION_NAME, appName)
    //yarn application Type
    effectiveConfiguration.set(YarnConfigOptions.APPLICATION_TYPE, submitInfo.applicationType)
    //main class
    effectiveConfiguration.set(ApplicationConfiguration.APPLICATION_MAIN_CLASS, appMain)
    //arguments...
    effectiveConfiguration.set(ApplicationConfiguration.APPLICATION_ARGS, programArgs.toList.asJava)

    println("-----------------------")
    println("Effective executor configuration:", effectiveConfiguration)
    println("-----------------------")
    effectiveConfiguration
  }

  private[this] def getClusterClientByApplicationId(appId: String): ClusterClient[ApplicationId] = {
    val flinkConfiguration = new Configuration
    flinkConfiguration.set(YarnConfigOptions.APPLICATION_ID, appId)
    val clusterClientFactory = new YarnClusterClientFactory
    val applicationId = clusterClientFactory.getClusterId(flinkConfiguration)
    if (applicationId == null) {
      throw new FlinkException("[StreamX] getClusterClient error. No cluster id was specified. Please specify a cluster to which you would like to connect.")
    }
    val clusterDescriptor: YarnClusterDescriptor = clusterClientFactory.createClusterDescriptor(flinkConfiguration)
    clusterDescriptor.retrieve(applicationId).getClusterClient
  }

  private[this] def getJobID(jobId: String): JobID = {
    Try(JobID.fromHexString(jobId)) match {
      case Success(id) => id
      case Failure(e) => throw new CliArgsException(e.getMessage)
    }
  }

  private[this] def getOptionFromDefaultFlinkConfig[T](option: ConfigOption[T]): T = {
    if (flinkDefaultConfiguration == null) {
      val flinkLocalHome = FLINK_HOME
      require(flinkLocalHome != null)
      val flinkLocalConfDir = flinkLocalHome.concat("/conf")
      //获取flink的配置
      this.flinkDefaultConfiguration = GlobalConfiguration.loadConfiguration(flinkLocalConfDir)
    }
    flinkDefaultConfiguration.get(option)
  }

  case class SubmitInfo(flinkUserJar: String,
                        appName: String,
                        appConf: String,
                        applicationType: String,
                        savePoint: String,
                        flameGraph: JavaMap[String, Serializable],
                        option: String,
                        property: JavaMap[String, Any],
                        dynamicOption: Array[String],
                        args: String)


}

