/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.streampark.flink.submit.`trait`

import com.google.common.collect.Lists
import org.apache.commons.cli.{CommandLine, Options}
import org.apache.commons.collections.MapUtils
import org.apache.commons.lang3.StringUtils
import org.apache.flink.api.common.JobID
import org.apache.flink.client.cli.CliFrontend.loadCustomCommandLines
import org.apache.flink.client.cli._
import org.apache.flink.client.deployment.application.ApplicationConfiguration
import org.apache.flink.client.program.{ClusterClient, PackagedProgram, PackagedProgramUtils}
import org.apache.flink.configuration._
import org.apache.flink.runtime.jobgraph.{JobGraph, SavepointConfigOptions}
import org.apache.flink.util.FlinkException
import org.apache.flink.util.Preconditions.checkNotNull
import org.apache.streampark.common.conf.ConfigConst._
import org.apache.streampark.common.conf.{ConfigConst, Workspace}
import org.apache.streampark.common.enums.{ApplicationType, DevelopmentMode, ExecutionMode, ResolveOrder}
import org.apache.streampark.common.util.{DeflaterUtils, Logger, SystemPropertyUtils, Utils}
import org.apache.streampark.flink.core.conf.FlinkRunOption
import org.apache.streampark.flink.core.FlinkClusterClient
import org.apache.streampark.flink.submit.bean._

import java.io.File
import java.util.concurrent.TimeUnit
import java.util.{Collections, List => JavaList, Map => JavaMap}
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
         |--------------------------------------- flink job start ---------------------------------------
         |    userFlinkHome    : ${submitRequest.flinkVersion.flinkHome}
         |    flinkVersion     : ${submitRequest.flinkVersion.version}
         |    appName          : ${submitRequest.appName}
         |    devMode          : ${submitRequest.developmentMode.name()}
         |    execMode         : ${submitRequest.executionMode.name()}
         |    k8sNamespace     : ${submitRequest.k8sSubmitParam.kubernetesNamespace}
         |    flinkExposedType : ${submitRequest.k8sSubmitParam.flinkRestExposedType}
         |    clusterId        : ${submitRequest.k8sSubmitParam.clusterId}
         |    applicationType  : ${submitRequest.applicationType.getName}
         |    flameGraph       : ${submitRequest.flameGraph != null}
         |    savePoint        : ${submitRequest.savePoint}
         |    properties       : ${submitRequest.properties.mkString(" ")}
         |    args             : ${submitRequest.args}
         |    appConf          : ${submitRequest.appConf}
         |    flinkBuildResult : ${submitRequest.buildResult}
         |-------------------------------------------------------------------------------------------
         |""".stripMargin)

    val (commandLine, flinkConfig) = getCommandLineAndFlinkConfig(submitRequest)
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
      .safeSet(ApplicationConfiguration.APPLICATION_MAIN_CLASS, submitRequest.appMain)
      .safeSet(ApplicationConfiguration.APPLICATION_ARGS, extractProgramArgs(submitRequest))
      .safeSet(PipelineOptionsInternal.PIPELINE_FIXED_JOB_ID, submitRequest.jobId)

    val flinkDefaultConfiguration = getFlinkDefaultConfiguration(submitRequest.flinkVersion.flinkHome)
    //state.checkpoints.num-retained
    val retainedOption = CheckpointingOptions.MAX_RETAINED_CHECKPOINTS
    flinkConfig.safeSet(retainedOption, flinkDefaultConfiguration.get(retainedOption))

    //set savepoint parameter
    if (submitRequest.savePoint != null) {
      flinkConfig.safeSet(SavepointConfigOptions.SAVEPOINT_PATH, submitRequest.savePoint)
      flinkConfig.setBoolean(SavepointConfigOptions.SAVEPOINT_IGNORE_UNCLAIMED_STATE, submitRequest.allowNonRestoredState)
    }

    setConfig(submitRequest, flinkConfig)

    doSubmit(submitRequest, flinkConfig)
  }

  def setConfig(submitRequest: SubmitRequest, flinkConf: Configuration): Unit

  @throws[Exception] def cancel(cancelRequest: CancelRequest): CancelResponse = {
    logInfo(
      s"""
         |----------------------------------------- flink job cancel --------------------------------------
         |     userFlinkHome  : ${cancelRequest.flinkVersion.flinkHome}
         |     flinkVersion   : ${cancelRequest.flinkVersion.version}
         |     clusterId      : ${cancelRequest.clusterId}
         |     withSavePoint  : ${cancelRequest.withSavePoint}
         |     savePointPath  : ${cancelRequest.customSavePointPath}
         |     withDrain      : ${cancelRequest.withDrain}
         |     k8sNamespace   : ${cancelRequest.kubernetesNamespace}
         |     appId          : ${cancelRequest.clusterId}
         |     jobId          : ${cancelRequest.jobId}
         |-------------------------------------------------------------------------------------------
         |""".stripMargin)
    val flinkConf = new Configuration()
    doCancel(cancelRequest, flinkConf)
  }

  @throws[Exception]
  def doSubmit(submitRequest: SubmitRequest, flinkConf: Configuration): SubmitResponse

  @throws[Exception]
  def doCancel(cancelRequest: CancelRequest, flinkConf: Configuration): CancelResponse

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
      .setJarFile(jarFile)
      .setEntryPointClassName(flinkConfig.getOptional(ApplicationConfiguration.APPLICATION_MAIN_CLASS).get())
      .setSavepointRestoreSettings(submitRequest.savepointRestoreSettings)
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

  private[submit] lazy val jvmProfilerJar: String = {
    val pluginsPath = SystemPropertyUtils.get(ConfigConst.KEY_APP_HOME).concat("/plugins")
    val pluginsDir = new File(pluginsPath)
    pluginsDir.list().filter(_.matches("streampark-jvm-profiler-.*\\.jar")) match {
      case Array() => throw new IllegalArgumentException(s"[StreamPark] can no found streampark-jvm-profiler jar in $pluginsPath")
      case array if array.length == 1 => array.head
      case more => throw new IllegalArgumentException(s"[StreamPark] found multiple streampark-jvm-profiler jar in $pluginsPath,[${more.mkString(",")}]")
    }
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
    if (submitRequest.properties.containsKey(KEY_FLINK_PARALLELISM())) {
      Integer.valueOf(submitRequest.properties.get(KEY_FLINK_PARALLELISM()).toString)
    } else {
      getFlinkDefaultConfiguration(submitRequest.flinkVersion.flinkHome).getInteger(
        CoreOptions.DEFAULT_PARALLELISM,
        CoreOptions.DEFAULT_PARALLELISM.defaultValue()
      )
    }
  }

  private[this] def getCommandLineAndFlinkConfig(submitRequest: SubmitRequest): (CommandLine, Configuration) = {

    val commandLineOptions = getCommandLineOptions(submitRequest.flinkVersion.flinkHome)

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

      // fromSavePoint
      if (submitRequest.savePoint != null) {
        optionMap += s"-${FlinkRunOption.SAVEPOINT_PATH_OPTION.getOpt}" -> submitRequest.savePoint
      }

      Seq("-e", "--executor", "-t", "--target").foreach(optionMap.remove)
      if (submitRequest.executionMode != null) {
        optionMap += "-t" -> submitRequest.executionMode.getName
      }

      val array = new ArrayBuffer[String]()
      optionMap.foreach(x => {
        array += x._1
        x._2 match {
          case v: String => array += v
          case _ =>
        }
      })

      //-jvm profile only on yarn support
      if (Utils.notEmpty(submitRequest.flameGraph) && ExecutionMode.isYarnMode(submitRequest.executionMode)) {
        val buffer = new StringBuffer()
        submitRequest.flameGraph.foreach(p => buffer.append(s"${p._1}=${p._2},"))
        val param = buffer.toString.dropRight(1)
        array += s"-D${CoreOptions.FLINK_TM_JVM_OPTIONS.key()}=-javaagent:$$PWD/plugins/$jvmProfilerJar=$param"
      }

      // app properties
      if (MapUtils.isNotEmpty(submitRequest.properties)) {
        submitRequest.properties.foreach(x => array += s"-D${x._1}=${x._2}")
      }
      array.toArray
    }

    logger.info(s"cliArgs: ${cliArgs.mkString(" ")}")

    FlinkRunOption.parse(commandLineOptions, cliArgs, true)

    val commandLine = FlinkRunOption.parse(commandLineOptions, cliArgs, true)

    val activeCommandLine = validateAndGetActiveCommandLine(getCustomCommandLines(submitRequest.flinkVersion.flinkHome), commandLine)

    val configuration = applyConfiguration(submitRequest.flinkVersion.flinkHome, activeCommandLine, commandLine)

    commandLine -> configuration

  }

  private[submit] def getCommandLineOptions(flinkHome: String) = {
    val customCommandLines = getCustomCommandLines(flinkHome)
    val customCommandLineOptions = new Options
    for (customCommandLine <- customCommandLines) {
      customCommandLine.addGeneralOptions(customCommandLineOptions)
      customCommandLine.addRunOptions(customCommandLineOptions)
    }
    FlinkRunOption.mergeOptions(CliFrontendParser.getRunCommandOptions, customCommandLineOptions)
  }

  private[submit] def extractConfiguration(flinkHome: String, properties: JavaMap[String, Any]): Configuration = {
    val commandLine = {
      val commandLineOptions = getCommandLineOptions(flinkHome)
      //read and verify user config...
      val cliArgs = {
        val array = new ArrayBuffer[String]()
        // The priority of the parameters defined on the page is greater than the app conf file, property parameters etc.
        if (MapUtils.isNotEmpty(properties)) {
          properties.foreach(x => array += s"-D${x._1}=${x._2.toString.trim}")
        }
        array.toArray
      }
      FlinkRunOption.parse(commandLineOptions, cliArgs, true)
    }
    val activeCommandLine = validateAndGetActiveCommandLine(getCustomCommandLines(flinkHome), commandLine)
    val flinkConfig = applyConfiguration(flinkHome, activeCommandLine, commandLine)
    flinkConfig
  }

  private[this] def extractProgramArgs(submitRequest: SubmitRequest): JavaList[String] = {

    val programArgs = new ArrayBuffer[String]()

    if (StringUtils.isNotEmpty(submitRequest.args)) {
      val multiLineChar = "\"\"\""
      val array = submitRequest.args.split("\\s+")
      if (array.filter(_.startsWith(multiLineChar)).isEmpty) {
        array.foreach(programArgs +=)
      } else {
        val argsArray = new ArrayBuffer[String]()
        val tempBuffer = new ArrayBuffer[String]()

        def processElement(index: Int, multiLine: Boolean): Unit = {
          if (index == array.length) {
            if (tempBuffer.nonEmpty) {
              argsArray += tempBuffer.mkString(" ")
            }
            return
          }
          val next = index + 1
          val elem = array(index)

          if (elem.trim.nonEmpty) {
            if (!multiLine) {
              if (elem.startsWith(multiLineChar)) {
                tempBuffer += elem.drop(3)
                processElement(next, true)
              } else {
                argsArray += elem
                processElement(next, false)
              }
            } else {
              if (elem.endsWith(multiLineChar)) {
                tempBuffer += elem.dropRight(3)
                argsArray += tempBuffer.mkString(" ")
                tempBuffer.clear()
                processElement(next, false)
              } else {
                tempBuffer += elem
                processElement(next, multiLine)
              }
            }
          } else {
            tempBuffer += elem
            processElement(next, false)
          }
        }
        processElement(0, false)
        argsArray.foreach(x => programArgs += x.trim)
      }
    }

    if (submitRequest.applicationType == ApplicationType.STREAMPARK_FLINK) {
      programArgs += PARAM_KEY_FLINK_CONF
      programArgs += submitRequest.flinkYaml
      programArgs += PARAM_KEY_APP_NAME
      programArgs += DeflaterUtils.zipString(submitRequest.effectiveAppName)
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


  private[this] def applyConfiguration(flinkHome: String,
                                       activeCustomCommandLine: CustomCommandLine,
                                       commandLine: CommandLine): Configuration = {

    require(activeCustomCommandLine != null, "activeCustomCommandLine must not be null.")
    val executorConfig = activeCustomCommandLine.toConfiguration(commandLine)
    val customConfiguration = new Configuration(executorConfig)
    val configuration = new Configuration()
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


  private[submit] implicit class EnhanceFlinkConfiguration(flinkConfig: Configuration) {
    def safeSet[T](option: ConfigOption[T], value: T): Configuration = {
      flinkConfig match {
        case x if value != null && value.toString.nonEmpty => x.set(option, value)
        case x => x
      }
    }
  }

  private[submit] def cancelJob(cancelRequest: CancelRequest, jobID: JobID, client: ClusterClient[_]): String = {
    val savePointDir = {
      if (!cancelRequest.withSavePoint) null; else {
        if (StringUtils.isNotEmpty(cancelRequest.customSavePointPath)) {
          cancelRequest.customSavePointPath
        } else {
          val configDir = getOptionFromDefaultFlinkConfig[String](
            cancelRequest.flinkVersion.flinkHome,
            ConfigOptions.key(CheckpointingOptions.SAVEPOINT_DIRECTORY.key())
              .stringType()
              .defaultValue {
                if (cancelRequest.executionMode == ExecutionMode.YARN_APPLICATION) {
                  Workspace.remote.APP_SAVEPOINTS
                } else null
              }
          )
          if (StringUtils.isEmpty(configDir)) {
            throw new FlinkException(s"[StreamPark] executionMode: ${cancelRequest.executionMode.getName}, savePoint path is null or invalid.")
          } else configDir
        }
      }
    }

    val clientTimeout = getOptionFromDefaultFlinkConfig(cancelRequest.flinkVersion.flinkHome, ClientOptions.CLIENT_TIMEOUT)

    val clientWrapper = new FlinkClusterClient(client)

    (Try(cancelRequest.withSavePoint).getOrElse(false), Try(cancelRequest.withDrain).getOrElse(false)) match {
      case (false, false) =>
        client.cancel(jobID).get()
        null
      case (true, false) => clientWrapper.cancelWithSavepoint(jobID, savePointDir).get(clientTimeout.toMillis, TimeUnit.MILLISECONDS)
      case (_, _) => clientWrapper.stopWithSavepoint(jobID, cancelRequest.withDrain, savePointDir).get(clientTimeout.toMillis, TimeUnit.MILLISECONDS)
    }
  }

}
