/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.flink.submit.`trait`

import com.google.common.collect.Lists
import com.streamxhub.streamx.common.conf.ConfigConst._
import com.streamxhub.streamx.common.conf.Workspace
import com.streamxhub.streamx.common.enums.{ApplicationType, DevelopmentMode, ExecutionMode, ResolveOrder}
import com.streamxhub.streamx.common.util.{Logger, SystemPropertyUtils, Utils}
import com.streamxhub.streamx.flink.core.conf.FlinkRunOption
import com.streamxhub.streamx.flink.core.{ClusterClient => ClusterClientWrapper}
import com.streamxhub.streamx.flink.submit.bean._
import org.apache.commons.cli.{CommandLine, Options}
import org.apache.commons.collections.MapUtils
import org.apache.commons.lang.StringUtils
import org.apache.flink.api.common.JobID
import org.apache.flink.client.cli.CliFrontend.loadCustomCommandLines
import org.apache.flink.client.cli._
import org.apache.flink.client.deployment.application.ApplicationConfiguration
import org.apache.flink.client.program.{ClusterClient, PackagedProgram, PackagedProgramUtils}
import org.apache.flink.configuration._
import org.apache.flink.runtime.jobgraph.{JobGraph, SavepointConfigOptions}
import org.apache.flink.util.FlinkException
import org.apache.flink.util.Preconditions.checkNotNull
import java.util.{Map => JavaMap}
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.{Collections, List => JavaList}
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}


trait FlinkSubmitTrait extends Logger {

  private[submit] lazy val PARAM_KEY_FLINK_CONF = KEY_FLINK_CONF("--")
  private[submit] lazy val PARAM_KEY_FLINK_SQL = KEY_FLINK_SQL("--")
  private[submit] lazy val PARAM_KEY_APP_CONF = KEY_APP_CONF("--")
  private[submit] lazy val PARAM_KEY_APP_NAME = KEY_APP_NAME("--")
  private[submit] lazy val PARAM_KEY_FLINK_PARALLELISM = KEY_FLINK_PARALLELISM("--")

  @throws[Exception] def submit(submitRequest: SubmitRequest): SubmitResponse = {
    logInfo(
      s"""
         |--------------------------------------- flink start ---------------------------------------
         |    userFlinkHome    : ${submitRequest.flinkVersion.flinkHome}
         |    flinkVersion     : ${submitRequest.flinkVersion.version}
         |    appName          : ${submitRequest.appName}
         |    devMode          : ${submitRequest.developmentMode.name()}
         |    execMode         : ${submitRequest.executionMode.name()}
         |    k8sNamespace     : ${submitRequest.k8sSubmitParam.kubernetesNamespace}
         |    flinkExposedType : ${submitRequest.k8sSubmitParam.flinkRestExposedType}
         |    clusterId        : ${submitRequest.k8sSubmitParam.clusterId}
         |    resolveOrder     : ${submitRequest.resolveOrder.getName}
         |    applicationType  : ${submitRequest.applicationType.getName}
         |    flameGraph       : ${submitRequest.flameGraph != null}
         |    savePoint        : ${submitRequest.savePoint}
         |    option           : ${submitRequest.option}
         |    property         : ${submitRequest.option}
         |    dynamicOption    : ${submitRequest.dynamicOption.mkString(" ")}
         |    args             : ${submitRequest.args}
         |    appConf          : ${submitRequest.appConf}
         |    flinkBuildResult : ${submitRequest.buildResult}
         |-------------------------------------------------------------------------------------------
         |""".stripMargin)

    val commandLine = getEffectiveCommandLine(
      submitRequest,
      "-t" -> submitRequest.executionMode.getName
    )

    val activeCommandLine = validateAndGetActiveCommandLine(getCustomCommandLines(submitRequest.flinkVersion.flinkHome), commandLine)
    val flinkConfig = applyConfiguration(submitRequest, activeCommandLine, commandLine)
    if (submitRequest.userJarFile != null) {
      val uri = PackagedProgramUtils.resolveURI(submitRequest.userJarFile.getAbsolutePath)
      val programOptions = ProgramOptions.create(commandLine)
      val executionParameters = ExecutionConfigAccessor.fromProgramOptions(programOptions, Collections.singletonList(uri.toString))
      executionParameters.applyToConfiguration(flinkConfig)
    }

    // set common parameter
    flinkConfig
      .safeSet(PipelineOptions.NAME, submitRequest.effectiveAppName)
      .safeSet(DeploymentOptions.TARGET, submitRequest.executionMode.getName)
      .safeSet(SavepointConfigOptions.SAVEPOINT_PATH, submitRequest.savePoint)
      .safeSet(CoreOptions.CLASSLOADER_RESOLVE_ORDER, submitRequest.resolveOrder.getName)
      .safeSet(ApplicationConfiguration.APPLICATION_MAIN_CLASS, submitRequest.appMain)
      .safeSet(ApplicationConfiguration.APPLICATION_ARGS, extractProgramArgs(submitRequest))
      .safeSet(PipelineOptionsInternal.PIPELINE_FIXED_JOB_ID, new JobID().toHexString)

    val flinkDefaultConfiguration = getFlinkDefaultConfiguration(submitRequest.flinkVersion.flinkHome)
    //state.checkpoints.num-retained
    val retainedOption = CheckpointingOptions.MAX_RETAINED_CHECKPOINTS
    flinkConfig.set(retainedOption, flinkDefaultConfiguration.get(retainedOption))

    setConfig(submitRequest, flinkConfig)

    doSubmit(submitRequest, flinkConfig)
  }

  def setConfig(submitRequest: SubmitRequest, flinkConf: Configuration): Unit

  @throws[Exception] def stop(stopRequest: StopRequest): StopResponse = {
    logInfo(
      s"""
         |----------------------------------------- flink stop --------------------------------------
         |     userFlinkHome  : ${stopRequest.flinkVersion.flinkHome}
         |     flinkVersion   : ${stopRequest.flinkVersion.version}
         |     withSavePoint  : ${stopRequest.withSavePoint}
         |     withDrain      : ${stopRequest.withDrain}
         |     k8sNamespace   : ${stopRequest.kubernetesNamespace}
         |     appId          : ${stopRequest.clusterId}
         |     jobId          : ${stopRequest.jobId}
         |-------------------------------------------------------------------------------------------
         |""".stripMargin)
    val flinkConf = new Configuration()
    doStop(stopRequest, flinkConf)
  }

  @throws[Exception]
  def doSubmit(submitRequest: SubmitRequest, flinkConf: Configuration): SubmitResponse

  @throws[Exception]
  def doStop(stopRequest: StopRequest, flinkConf: Configuration): StopResponse

  def trySubmit(submitRequest: SubmitRequest,
                flinkConfig: Configuration,
                jarFile: File)(restApiFunc: (SubmitRequest, Configuration, File) => SubmitResponse)
               (jobGraphFunc: (SubmitRequest, Configuration, File) => SubmitResponse): SubmitResponse = {
    // Prioritize using Rest API submit while using JobGraph submit plan as backup
    Try {
      logInfo(s"[flink-submit] Attempting to submit in Rest API Submit Plan.")
      restApiFunc(submitRequest, flinkConfig, jarFile)
    }.getOrElse {
      logWarn(s"[flink-submit] RestAPI Submit Plan failed,try JobGraph Submit Plan now.")
      Try(jobGraphFunc(submitRequest, flinkConfig, jarFile)) match {
        case Success(r) => r
        case Failure(e) =>
          logError(s"[flink-submit] Both Rest API Submit Plan and JobGraph Submit Plan failed.")
          throw e
      }

    }
  }

  private[submit] def getJobGraph(flinkConfig: Configuration, submitRequest: SubmitRequest, jarFile: File): (PackagedProgram, JobGraph) = {
    val packageProgram = PackagedProgram
      .newBuilder
      .setSavepointRestoreSettings(submitRequest.savepointRestoreSettings)
      .setJarFile(jarFile)
      .setEntryPointClassName(flinkConfig.getOptional(ApplicationConfiguration.APPLICATION_MAIN_CLASS).get())
      .setArguments(
        flinkConfig
          .getOptional(ApplicationConfiguration.APPLICATION_ARGS)
          .orElse(Lists.newArrayList()): _*
      ).build()

    val jobGraph = PackagedProgramUtils.createJobGraph(
      packageProgram,
      flinkConfig,
      getParallelism(submitRequest),
      null,
      false
    )
    packageProgram -> jobGraph
  }

  private[submit] def getJobID(jobId: String) = Try(JobID.fromHexString(jobId)) match {
    case Success(id) => id
    case Failure(e) => throw new CliArgsException(e.getMessage)
  }

  //----------Public Method end ------------------
  private[this] def getEffectiveCommandLine(submitRequest: SubmitRequest,
                                            otherParam: (String, String)*): CommandLine = {

    val customCommandLines = getCustomCommandLines(submitRequest.flinkVersion.flinkHome)
    //merge options....
    val customCommandLineOptions = new Options
    for (customCommandLine <- customCommandLines) {
      customCommandLine.addGeneralOptions(customCommandLineOptions)
      customCommandLine.addRunOptions(customCommandLineOptions)
    }

    val commandLineOptions = FlinkRunOption.mergeOptions(CliFrontendParser.getRunCommandOptions, customCommandLineOptions)

    //read and verify user config...
    val cliArgs = {
      val optionMap = new mutable.HashMap[String, Any]()
      submitRequest.appOption.filter(x => {
        //验证参数是否合法...
        val verify = commandLineOptions.hasOption(x._1)
        if (!verify) logWarn(s"param:${x._1} is error,skip it.")
        verify
      }).foreach(x => {
        val opt = commandLineOptions.getOption(x._1.trim).getOpt
        Try(x._2.toBoolean).getOrElse(x._2) match {
          case b if b.isInstanceOf[Boolean] => if (b.asInstanceOf[Boolean]) optionMap += s"-$opt" -> true
          case v => optionMap += s"-$opt" -> v
        }
      })

      //fromSavePoint
      if (submitRequest.savePoint != null) {
        optionMap += s"-${FlinkRunOption.SAVEPOINT_PATH_OPTION.getOpt}" -> submitRequest.savePoint
      }

      Seq("-e", "--executor", "-t", "--target").foreach(optionMap.remove)
      otherParam.foreach(optionMap +=)

      val array = new ArrayBuffer[String]()
      optionMap.foreach(x => {
        array += x._1
        x._2 match {
          case v: String => array += v
          case _ =>
        }
      })

      //-jvm profile support
      if (Utils.notEmpty(submitRequest.flameGraph)) {
        val buffer = new StringBuffer()
        submitRequest.flameGraph.foreach(p => buffer.append(s"${p._1}=${p._2},"))
        val param = buffer.toString.dropRight(1)

        /**
         * 不要问我javaagent路径为什么这么写,魔鬼在细节中.
         */
        array += s"-D${CoreOptions.FLINK_TM_JVM_OPTIONS.key()}=-javaagent:$$PWD/plugins/$jvmProfilerJar=$param"
      }

      //页面定义的参数优先级大于app配置文件,属性参数...
      if (MapUtils.isNotEmpty(submitRequest.option)) {
        submitRequest.option.foreach(x => array += s"-D${x._1.trim}=${x._2.toString.trim}")
      }

      //-D 其他动态参数配置....
      if (submitRequest.dynamicOption != null && submitRequest.dynamicOption.nonEmpty) {
        submitRequest.dynamicOption
          .filter(!_.matches("(^-D|^)classloader.resolve-order.*"))
          .foreach(x => array += x.replaceFirst("^-D|^", "-D"))
      }

      array += s"-Dclassloader.resolve-order=${submitRequest.resolveOrder.getName}"

      array.toArray
    }

    logger.info(s"cliArgs: ${cliArgs.mkString(" ")}")

    FlinkRunOption.parse(commandLineOptions, cliArgs, true)

  }

  private[submit] def validateAndGetActiveCommandLine(customCommandLines: JavaList[CustomCommandLine], commandLine: CommandLine): CustomCommandLine = {
    val line = checkNotNull(commandLine)
    logInfo(s"Custom commandline: $customCommandLines")
    for (cli <- customCommandLines) {
      val isActive = cli.isActive(line)
      logInfo(s"Checking custom commandline $cli, isActive: $isActive")
      if (isActive) return cli
    }
    throw new IllegalStateException("No valid command-line found.")
  }

  private[submit] lazy val jvmProfilerJar: String = {
    val pluginsPath = SystemPropertyUtils.get("app.home").concat("/plugins")
    val pluginsDir = new File(pluginsPath)
    pluginsDir.list().filter(_.matches("streamx-jvm-profiler-.*\\.jar")) match {
      case Array() => throw new IllegalArgumentException(s"[StreamX] can no found streamx-jvm-profiler jar in $pluginsPath")
      case array if array.length == 1 => array.head
      case more => throw new IllegalArgumentException(s"[StreamX] found multiple streamx-jvm-profiler jar in $pluginsPath,[${more.mkString(",")}]")
    }
  }

  private[submit] def getFlinkDefaultConfiguration(flinkHome: String): Configuration = {
    Try(GlobalConfiguration.loadConfiguration(s"$flinkHome/conf")).getOrElse(new Configuration())
  }

  private[submit] def getOptionFromDefaultFlinkConfig[T](flinkHome: String, option: ConfigOption[T]): T = {
    getFlinkDefaultConfiguration(flinkHome).get(option)
  }

  private[this] def getCustomCommandLines(flinkHome: String): JavaList[CustomCommandLine] = {
    val flinkDefaultConfiguration: Configuration = getFlinkDefaultConfiguration(flinkHome)
    // 1. find the configuration directory
    val configurationDirectory = s"$flinkHome/conf"
    // 2. load the custom command lines
    val customCommandLines = loadCustomCommandLines(flinkDefaultConfiguration, configurationDirectory)
    new CliFrontend(flinkDefaultConfiguration, customCommandLines)
    customCommandLines
  }

  private[submit] def getParallelism(submitRequest: SubmitRequest): Integer = {
    if (submitRequest.option.containsKey(KEY_FLINK_PARALLELISM())) {
      Integer.valueOf(submitRequest.option.get(KEY_FLINK_PARALLELISM()).toString)
    } else {
      getFlinkDefaultConfiguration(submitRequest.flinkVersion.flinkHome).getInteger(
        CoreOptions.DEFAULT_PARALLELISM,
        CoreOptions.DEFAULT_PARALLELISM.defaultValue()
      )
    }
  }

  private[submit] def extractConfiguration(flinkHome: String,
                                           savePoint: String,
                                           flameGraph: JavaMap[String, java.io.Serializable],
                                           dynamicOption: Array[String],
                                           extraParameter: JavaMap[String, Any],
                                           resolveOrder: ResolveOrder,
                                           otherParam: (String, String)*): Configuration = {
    val commandLine = {
        val customCommandLines = getCustomCommandLines(flinkHome)
        //merge options....
        val customCommandLineOptions = new Options
        for (customCommandLine <- customCommandLines) {
          customCommandLine.addGeneralOptions(customCommandLineOptions)
          customCommandLine.addRunOptions(customCommandLineOptions)
        }
        val commandLineOptions = FlinkRunOption.mergeOptions(CliFrontendParser.getRunCommandOptions, customCommandLineOptions)

        //read and verify user config...
        val cliArgs = {
          val optionMap = new mutable.HashMap[String, Any]()
          //fromSavePoint
          if (savePoint != null) {
            optionMap += s"-${FlinkRunOption.SAVEPOINT_PATH_OPTION.getOpt}" -> savePoint
          }

          Seq("-e", "--executor", "-t", "--target").foreach(optionMap.remove)
          otherParam.foreach(optionMap +=)

          val array = new ArrayBuffer[String]()
          optionMap.foreach(x => {
            array += x._1
            x._2 match {
              case v: String => array += v
              case _ =>
            }
          })

          //-jvm profile support
          if (Utils.notEmpty(flameGraph)) {
            val buffer = new StringBuffer()
            flameGraph.foreach(p => buffer.append(s"${p._1}=${p._2},"))
            val param = buffer.toString.dropRight(1)

            /**
              * 不要问我javaagent路径为什么这么写,魔鬼在细节中.
              */
            array += s"-D${CoreOptions.FLINK_TM_JVM_OPTIONS.key()}=-javaagent:$$PWD/plugins/$jvmProfilerJar=$param"
          }

          //页面定义的参数优先级大于app配置文件,属性参数...
          if (MapUtils.isNotEmpty(extraParameter)) {
            extraParameter.foreach(x => array += s"-D${x._1.trim}=${x._2.toString.trim}")
          }

          //-D 其他动态参数配置....
          if (dynamicOption != null && dynamicOption.nonEmpty) {
            dynamicOption
              .filter(!_.matches("(^-D|^)classloader.resolve-order.*"))
              .foreach(x => array += x.replaceFirst("^-D|^", "-D"))
          }

          array += s"-Dclassloader.resolve-order=${resolveOrder.getName}"

          array.toArray
        }

        logger.info(s"cliArgs: ${cliArgs.mkString(" ")}")

        FlinkRunOption.parse(commandLineOptions, cliArgs, true)
    }
    val activeCommandLine = validateAndGetActiveCommandLine(getCustomCommandLines(flinkHome), commandLine)
    val flinkConfig = applyConfiguration(flinkHome, activeCommandLine, commandLine)
    flinkConfig
  }

  private[this] def applyConfiguration(flinkHome: String,
                                       activeCustomCommandLine: CustomCommandLine,
                                       commandLine: CommandLine): Configuration = {

    require(activeCustomCommandLine != null, "activeCustomCommandLine must not be null.")
    val executorConfig = activeCustomCommandLine.toConfiguration(commandLine)
    val customConfiguration = new Configuration(executorConfig)
    val configuration = new Configuration()
    //flink-conf.yaml配置
    val flinkDefaultConfiguration = getFlinkDefaultConfiguration(flinkHome)
    flinkDefaultConfiguration.keySet.foreach(x => {
      flinkDefaultConfiguration.getString(x, null) match {
        case v if v != null => configuration.setString(x, v)
        case _ =>
      }
    })
    configuration.addAll(customConfiguration)
    configuration
  }

  private[this] def getEffectiveCommandLine(flinkHome: String,
                                            savePoint: String,
                                            flameGraph: JavaMap[String, java.io.Serializable],
                                            dynamicOption: Array[String],
                                            extraParameter: JavaMap[String, Any],
                                            resolveOrder: ResolveOrder,
                                            otherParam: (String, String)
                                           ): CommandLine = {

    val customCommandLines = getCustomCommandLines(flinkHome)
    //merge options....
    val customCommandLineOptions = new Options
    for (customCommandLine <- customCommandLines) {
      customCommandLine.addGeneralOptions(customCommandLineOptions)
      customCommandLine.addRunOptions(customCommandLineOptions)
    }
    val commandLineOptions = FlinkRunOption.mergeOptions(CliFrontendParser.getRunCommandOptions, customCommandLineOptions)

    //read and verify user config...
    val cliArgs = {
      val optionMap = new mutable.HashMap[String, Any]()
      //fromSavePoint
      if (savePoint != null) {
        optionMap += s"-${FlinkRunOption.SAVEPOINT_PATH_OPTION.getOpt}" -> savePoint
      }

      Seq("-e", "--executor", "-t", "--target").foreach(optionMap.remove)
      optionMap +=otherParam

      val array = new ArrayBuffer[String]()
      optionMap.foreach(x => {
        array += x._1
        x._2 match {
          case v: String => array += v
          case _ =>
        }
      })

      //-jvm profile support
      if (Utils.notEmpty(flameGraph)) {
        val buffer = new StringBuffer()
        flameGraph.foreach(p => buffer.append(s"${p._1}=${p._2},"))
        val param = buffer.toString.dropRight(1)

        /**
          * 不要问我javaagent路径为什么这么写,魔鬼在细节中.
          */
        array += s"-D${CoreOptions.FLINK_TM_JVM_OPTIONS.key()}=-javaagent:$$PWD/plugins/$jvmProfilerJar=$param"
      }

      //页面定义的参数优先级大于app配置文件,属性参数...
      if (MapUtils.isNotEmpty(extraParameter)) {
        extraParameter.foreach(x => array += s"-D${x._1.trim}=${x._2.toString.trim}")
      }

      //-D 其他动态参数配置....
      if (dynamicOption != null && dynamicOption.nonEmpty) {
        dynamicOption
          .filter(!_.matches("(^-D|^)classloader.resolve-order.*"))
          .foreach(x => array += x.replaceFirst("^-D|^", "-D"))
      }

      array += s"-Dclassloader.resolve-order=${resolveOrder.getName}"

      array.toArray
    }

    logger.info(s"cliArgs: ${cliArgs.mkString(" ")}")

    FlinkRunOption.parse(commandLineOptions, cliArgs, true)
  }

  private[this] def extractProgramArgs(submitRequest: SubmitRequest): JavaList[String] = {

    val programArgs = new ArrayBuffer[String]()

    Try(submitRequest.args.split("\\s+")).getOrElse(Array()).foreach(x => if (x.nonEmpty) programArgs += x)

    if (submitRequest.applicationType == ApplicationType.STREAMX_FLINK) {
      programArgs += PARAM_KEY_FLINK_CONF
      programArgs += submitRequest.flinkYaml
      programArgs += PARAM_KEY_APP_NAME
      programArgs += submitRequest.effectiveAppName
      programArgs += PARAM_KEY_FLINK_PARALLELISM
      programArgs += getParallelism(submitRequest).toString
      submitRequest.developmentMode match {
        case DevelopmentMode.FLINKSQL =>
          programArgs += PARAM_KEY_FLINK_SQL
          programArgs += submitRequest.flinkSQL
          if (submitRequest.appConf != null) {
            programArgs += PARAM_KEY_APP_CONF
            programArgs += submitRequest.appConf
          }
        case _ if Try(!submitRequest.appConf.startsWith("json:")).getOrElse(true) =>
          programArgs += PARAM_KEY_APP_CONF
          programArgs += submitRequest.appConf
      }
    }
    programArgs.toList.asJava
  }

  /**
   * 页面定义参数优先级 > flink-conf.yaml中配置优先级
   *
   * @param submitRequest
   * @param activeCustomCommandLine
   * @param commandLine
   * @return
   */
  private[this] def applyConfiguration(submitRequest: SubmitRequest,
                                       activeCustomCommandLine: CustomCommandLine,
                                       commandLine: CommandLine): Configuration = {

    require(activeCustomCommandLine != null, "activeCustomCommandLine must not be null.")
    val executorConfig = activeCustomCommandLine.toConfiguration(commandLine)
    val customConfiguration = new Configuration(executorConfig)
    val configuration = new Configuration()
    //flink-conf.yaml配置
    val flinkDefaultConfiguration = getFlinkDefaultConfiguration(submitRequest.flinkVersion.flinkHome)
    flinkDefaultConfiguration.keySet.foreach(x => {
      flinkDefaultConfiguration.getString(x, null) match {
        case v if v != null => configuration.setString(x, v)
        case _ =>
      }
    })
    configuration.addAll(customConfiguration)
    configuration
  }

  private[submit] implicit class EnhanceFlinkConfiguration(flinkConfig: Configuration) {
    def safeSet[T](option: ConfigOption[T], value: T): Configuration = {
      flinkConfig match {
        case x if value != null && value.toString.nonEmpty => x.set(option, value)
        case x => x
      }
    }
  }

  private[submit] def cancelJob(stopRequest: StopRequest, jobID: JobID, client: ClusterClient[_]): String = {
    val savePointDir = {
      if (!stopRequest.withSavePoint) null; else {
        if (StringUtils.isNotEmpty(stopRequest.customSavePointPath)) {
          stopRequest.customSavePointPath
        } else {
          val configDir = getOptionFromDefaultFlinkConfig[String](
            stopRequest.flinkVersion.flinkHome,
            ConfigOptions.key(CheckpointingOptions.SAVEPOINT_DIRECTORY.key())
              .stringType()
              .defaultValue {
                if (stopRequest.executionMode == ExecutionMode.YARN_APPLICATION) {
                  Workspace.remote.APP_SAVEPOINTS
                } else null
              }
          )
          if (StringUtils.isEmpty(configDir)) {
            throw new FlinkException(s"[StreamX] executionMode: ${stopRequest.executionMode.getName}, savePoint path is null or invalid.")
          } else configDir
        }
      }
    }

    val clientTimeout = getOptionFromDefaultFlinkConfig(stopRequest.flinkVersion.flinkHome, ClientOptions.CLIENT_TIMEOUT)

    val clientWrapper = new ClusterClientWrapper(client)

    (Try(stopRequest.withSavePoint).getOrElse(false), Try(stopRequest.withDrain).getOrElse(false)) match {
      case (false, false) =>
        clientWrapper.cancel(jobID).get()
        null
      case (true, false) => clientWrapper.cancelWithSavepoint(jobID, savePointDir).get(clientTimeout.toMillis, TimeUnit.MILLISECONDS)
      case (_, _) => clientWrapper.stopWithSavepoint(jobID, stopRequest.withDrain, savePointDir).get(clientTimeout.toMillis, TimeUnit.MILLISECONDS)
    }
  }

}
