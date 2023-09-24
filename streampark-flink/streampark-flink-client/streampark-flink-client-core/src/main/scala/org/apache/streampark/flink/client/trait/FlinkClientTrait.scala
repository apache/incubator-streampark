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

package org.apache.streampark.flink.client.`trait`

import org.apache.streampark.common.conf.{ConfigConst, Workspace}
import org.apache.streampark.common.conf.ConfigConst._
import org.apache.streampark.common.enums.{ApplicationTypeEnum, DevelopmentModeEnum, ExecutionModeEnum, RestoreModeEnum}
import org.apache.streampark.common.fs.FsOperator
import org.apache.streampark.common.util.{DeflaterUtils, FileUtils, Logger, SystemPropertyUtils}
import org.apache.streampark.flink.client.bean._
import org.apache.streampark.flink.core.FlinkClusterClient
import org.apache.streampark.flink.core.conf.FlinkRunOption

import com.google.common.collect.Lists
import org.apache.commons.cli.{CommandLine, Options}
import org.apache.commons.collections.MapUtils
import org.apache.commons.lang3.StringUtils
import org.apache.flink.api.common.JobID
import org.apache.flink.client.cli._
import org.apache.flink.client.cli.CliFrontend.loadCustomCommandLines
import org.apache.flink.client.deployment.application.ApplicationConfiguration
import org.apache.flink.client.program.{ClusterClient, PackagedProgram, PackagedProgramUtils}
import org.apache.flink.configuration._
import org.apache.flink.python.PythonOptions
import org.apache.flink.runtime.jobgraph.{JobGraph, SavepointConfigOptions}
import org.apache.flink.util.FlinkException
import org.apache.flink.util.Preconditions.checkNotNull

import java.util
import java.util.{Collections, List => JavaList, Map => JavaMap}

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.collection.convert.ImplicitConversions._
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

trait FlinkClientTrait extends Logger {

  private[client] lazy val PARAM_KEY_FLINK_CONF = KEY_FLINK_CONF(PARAM_PREFIX)
  private[client] lazy val PARAM_KEY_FLINK_SQL = KEY_FLINK_SQL(PARAM_PREFIX)
  private[client] lazy val PARAM_KEY_APP_CONF = KEY_APP_CONF(PARAM_PREFIX)
  private[client] lazy val PARAM_KEY_APP_NAME = KEY_APP_NAME(PARAM_PREFIX)
  private[client] lazy val PARAM_KEY_FLINK_PARALLELISM = KEY_FLINK_PARALLELISM(PARAM_PREFIX)

  private[this] lazy val javaEnvOpts = List(
    CoreOptions.FLINK_JVM_OPTIONS,
    CoreOptions.FLINK_JM_JVM_OPTIONS,
    CoreOptions.FLINK_HS_JVM_OPTIONS,
    CoreOptions.FLINK_TM_JVM_OPTIONS,
    CoreOptions.FLINK_CLI_JVM_OPTIONS
  )

  @throws[Exception]
  def submit(submitRequest: SubmitRequest): SubmitResponse = {
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
         |    savePoint        : ${submitRequest.savePoint}
         |    properties       : ${submitRequest.properties.mkString(" ")}
         |    args             : ${submitRequest.args}
         |    appConf          : ${submitRequest.appConf}
         |    flinkBuildResult : ${submitRequest.buildResult}
         |-------------------------------------------------------------------------------------------
         |""".stripMargin)

    val (commandLine, flinkConfig) = getCommandLineAndFlinkConfig(submitRequest)

    submitRequest.developmentMode match {
      case DevelopmentModeEnum.PYFLINK =>
        val flinkOptPath: String = System.getenv(ConfigConstants.ENV_FLINK_OPT_DIR)
        if (StringUtils.isBlank(flinkOptPath)) {
          logWarn(s"Get environment variable ${ConfigConstants.ENV_FLINK_OPT_DIR} fail")
          val flinkHome = submitRequest.flinkVersion.flinkHome
          SystemPropertyUtils.setEnv(ConfigConstants.ENV_FLINK_OPT_DIR, s"$flinkHome/opt")
          logInfo(
            s"Set temporary environment variables ${ConfigConstants.ENV_FLINK_OPT_DIR} = $flinkHome/opt")
        }
      case _ =>
        if (submitRequest.userJarFile != null) {
          val uri = PackagedProgramUtils.resolveURI(submitRequest.userJarFile.getAbsolutePath)
          val programOptions = ProgramOptions.create(commandLine)
          val executionParameters = ExecutionConfigAccessor.fromProgramOptions(
            programOptions,
            Collections.singletonList(uri.toString))
          executionParameters.applyToConfiguration(flinkConfig)
        }
    }

    // set common parameter
    flinkConfig
      .safeSet(PipelineOptions.NAME, submitRequest.effectiveAppName)
      .safeSet(DeploymentOptions.TARGET, submitRequest.executionMode.getName)
      .safeSet(SavepointConfigOptions.SAVEPOINT_PATH, submitRequest.savePoint)
      .safeSet(ApplicationConfiguration.APPLICATION_MAIN_CLASS, submitRequest.appMain)
      .safeSet(ApplicationConfiguration.APPLICATION_ARGS, extractProgramArgs(submitRequest))
      .safeSet(PipelineOptionsInternal.PIPELINE_FIXED_JOB_ID, submitRequest.jobId)

    if (
      !submitRequest.properties.containsKey(CheckpointingOptions.MAX_RETAINED_CHECKPOINTS.key())
    ) {
      val flinkDefaultConfiguration = getFlinkDefaultConfiguration(
        submitRequest.flinkVersion.flinkHome)
      // state.checkpoints.num-retained
      val retainedOption = CheckpointingOptions.MAX_RETAINED_CHECKPOINTS
      flinkConfig.safeSet(retainedOption, flinkDefaultConfiguration.get(retainedOption))
    }

    // set savepoint parameter
    if (submitRequest.savePoint != null) {
      flinkConfig.safeSet(SavepointConfigOptions.SAVEPOINT_PATH, submitRequest.savePoint)
      flinkConfig.setBoolean(
        SavepointConfigOptions.SAVEPOINT_IGNORE_UNCLAIMED_STATE,
        submitRequest.allowNonRestoredState)
      if (
        submitRequest.flinkVersion.checkVersion(
          RestoreModeEnum.SINCE_FLINK_VERSION) && submitRequest.restoreMode != null
      ) {
        flinkConfig.setString(RestoreModeEnum.RESTORE_MODE, submitRequest.restoreMode.getName);
      }
    }

    // set JVMOptions..
    if (MapUtils.isNotEmpty(submitRequest.properties)) {
      submitRequest.properties.foreach(
        x =>
          javaEnvOpts.find(_.key == x._1.trim) match {
            case Some(p) => flinkConfig.set(p, x._2.toString)
            case _ =>
          })
    }

    setConfig(submitRequest, flinkConfig)

    doSubmit(submitRequest, flinkConfig)

  }

  def setConfig(submitRequest: SubmitRequest, flinkConf: Configuration): Unit

  @throws[Exception]
  def triggerSavepoint(savepointRequest: TriggerSavepointRequest): SavepointResponse = {
    logInfo(
      s"""
         |----------------------------------------- flink job trigger savepoint ---------------------
         |     userFlinkHome  : ${savepointRequest.flinkVersion.flinkHome}
         |     flinkVersion   : ${savepointRequest.flinkVersion.version}
         |     clusterId      : ${savepointRequest.clusterId}
         |     savePointPath  : ${savepointRequest.savepointPath}
         |     nativeFormat   : ${savepointRequest.nativeFormat}
         |     k8sNamespace   : ${savepointRequest.kubernetesNamespace}
         |     appId          : ${savepointRequest.clusterId}
         |     jobId          : ${savepointRequest.jobId}
         |-------------------------------------------------------------------------------------------
         |""".stripMargin)
    val flinkConf = new Configuration()
    doTriggerSavepoint(savepointRequest, flinkConf)
  }

  @throws[Exception]
  def cancel(cancelRequest: CancelRequest): CancelResponse = {
    logInfo(
      s"""
         |----------------------------------------- flink job cancel --------------------------------
         |     userFlinkHome     : ${cancelRequest.flinkVersion.flinkHome}
         |     flinkVersion      : ${cancelRequest.flinkVersion.version}
         |     clusterId         : ${cancelRequest.clusterId}
         |     withSavePoint     : ${cancelRequest.withSavepoint}
         |     savePointPath     : ${cancelRequest.savepointPath}
         |     withDrain         : ${cancelRequest.withDrain}
         |     nativeFormat      : ${cancelRequest.nativeFormat}
         |     k8sNamespace      : ${cancelRequest.kubernetesNamespace}
         |     appId             : ${cancelRequest.clusterId}
         |     jobId             : ${cancelRequest.jobId}
         |-------------------------------------------------------------------------------------------
         |""".stripMargin)
    val flinkConf = new Configuration()
    doCancel(cancelRequest, flinkConf)
  }

  @throws[Exception]
  def doSubmit(submitRequest: SubmitRequest, flinkConf: Configuration): SubmitResponse

  @throws[Exception]
  def doTriggerSavepoint(
      request: TriggerSavepointRequest,
      flinkConf: Configuration): SavepointResponse

  @throws[Exception]
  def doCancel(cancelRequest: CancelRequest, flinkConf: Configuration): CancelResponse

  def trySubmit(submitRequest: SubmitRequest, flinkConfig: Configuration)(
      restApiFunc: (SubmitRequest, Configuration) => SubmitResponse)(
      jobGraphFunc: (SubmitRequest, Configuration) => SubmitResponse): SubmitResponse = {
    // Prioritize using Rest API submit while using JobGraph submit plan as backup
    Try {
      logInfo(s"[flink-submit] Attempting to submit in Rest API Submit Plan.")
      restApiFunc(submitRequest, flinkConfig)
    }.getOrElse {
      logWarn(s"[flink-submit] RestAPI Submit Plan failed,try JobGraph Submit Plan now.")
      Try(jobGraphFunc(submitRequest, flinkConfig)) match {
        case Success(r) => r
        case Failure(e) =>
          logError(s"[flink-submit] Both Rest API Submit Plan and JobGraph Submit Plan failed.")
          throw e
      }
    }
  }

  private[client] def getJobGraph(
      submitRequest: SubmitRequest,
      flinkConfig: Configuration): (PackagedProgram, JobGraph) = {
    if (submitRequest.developmentMode == DevelopmentModeEnum.PYFLINK) {
      val pythonVenv: String = Workspace.local.APP_PYTHON_VENV
      if (!FsOperator.lfs.exists(pythonVenv)) {
        throw new RuntimeException(s"$pythonVenv File does not exist")
      }

      val localLib: String = s"${Workspace.local.APP_WORKSPACE}/${submitRequest.id}/lib"
      if (FileUtils.exists(localLib) && FileUtils.directoryNotBlank(localLib)) {
        flinkConfig.safeSet(PipelineOptions.JARS, util.Arrays.asList(localLib))
      }

      flinkConfig
        // python.archives
        .safeSet(PythonOptions.PYTHON_ARCHIVES, pythonVenv)
        // python.client.executable
        .safeSet(PythonOptions.PYTHON_CLIENT_EXECUTABLE, ConfigConst.PYTHON_EXECUTABLE)
        // python.executable
        .safeSet(PythonOptions.PYTHON_EXECUTABLE, ConfigConst.PYTHON_EXECUTABLE)
    }

    val packageProgram = PackagedProgram.newBuilder
      .setArguments(
        flinkConfig
          .getOptional(ApplicationConfiguration.APPLICATION_ARGS)
          .orElse(Lists.newArrayList()): _*)
      .setEntryPointClassName(
        flinkConfig.getOptional(ApplicationConfiguration.APPLICATION_MAIN_CLASS).get()
      )
      .setSavepointRestoreSettings(submitRequest.savepointRestoreSettings)
      .build()

    val jobGraph = PackagedProgramUtils.createJobGraph(
      packageProgram,
      flinkConfig,
      getParallelism(submitRequest),
      null,
      false)
    packageProgram -> jobGraph
  }

  private[client] def getJobID(jobId: String) = Try(JobID.fromHexString(jobId)) match {
    case Success(id) => id
    case Failure(e) => throw new CliArgsException(e.getMessage)
  }

  // ----------Public Method end ------------------

  private[client] def validateAndGetActiveCommandLine(
      customCommandLines: JavaList[CustomCommandLine],
      commandLine: CommandLine): CustomCommandLine = {
    val line = checkNotNull(commandLine)
    logInfo(s"Custom commandline: $customCommandLines")
    for (cli <- customCommandLines) {
      val isActive = cli.isActive(line)
      logInfo(s"Checking custom commandline $cli, isActive: $isActive")
      if (isActive) return cli
    }
    throw new IllegalStateException("No valid command-line found.")
  }

  private[client] def getFlinkDefaultConfiguration(flinkHome: String): Configuration = {
    Try(GlobalConfiguration.loadConfiguration(s"$flinkHome/conf")).getOrElse(new Configuration())
  }

  private[client] def getOptionFromDefaultFlinkConfig[T](
      flinkHome: String,
      option: ConfigOption[T]): T = {
    getFlinkDefaultConfiguration(flinkHome).get(option)
  }

  private[this] def getCustomCommandLines(flinkHome: String): JavaList[CustomCommandLine] = {
    val flinkDefaultConfiguration: Configuration = getFlinkDefaultConfiguration(flinkHome)
    // 1. find the configuration directory
    val configurationDirectory = s"$flinkHome/conf"
    // 2. load the custom command lines
    val customCommandLines =
      loadCustomCommandLines(flinkDefaultConfiguration, configurationDirectory)
    new CliFrontend(flinkDefaultConfiguration, customCommandLines)
    customCommandLines
  }

  private[client] def getParallelism(submitRequest: SubmitRequest): Integer = {
    if (submitRequest.properties.containsKey(KEY_FLINK_PARALLELISM())) {
      Integer.valueOf(submitRequest.properties.get(KEY_FLINK_PARALLELISM()).toString)
    } else {
      getFlinkDefaultConfiguration(submitRequest.flinkVersion.flinkHome)
        .getInteger(CoreOptions.DEFAULT_PARALLELISM, CoreOptions.DEFAULT_PARALLELISM.defaultValue())
    }
  }

  private[this] def getCommandLineAndFlinkConfig(
      submitRequest: SubmitRequest): (CommandLine, Configuration) = {

    val commandLineOptions = getCommandLineOptions(submitRequest.flinkVersion.flinkHome)

    // read and verify user config...
    val cliArgs = {
      val optionMap = new mutable.HashMap[String, Any]()
      submitRequest.appOption
        .filter(
          x => {
            val verify = commandLineOptions.hasOption(x._1)
            if (!verify) logWarn(s"param:${x._1} is error,skip it.")
            verify
          })
        .foreach(
          x => {
            val opt = commandLineOptions.getOption(x._1.trim).getOpt
            Try(x._2.toBoolean).getOrElse(x._2) match {
              case b if b.isInstanceOf[Boolean] =>
                if (b.asInstanceOf[Boolean]) optionMap += s"-$opt" -> true
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
      optionMap.foreach(
        x => {
          array += x._1
          x._2 match {
            case v: String => array += v
            case _ =>
          }
        })

      // app properties
      if (MapUtils.isNotEmpty(submitRequest.properties)) {
        submitRequest.properties.foreach(
          x => {
            if (!x._1.startsWith(CoreOptions.FLINK_JVM_OPTIONS.key())) {
              array += s"-D${x._1}=${x._2}"
            }
          })
      }
      array.toArray
    }

    logger.info(s"cliArgs: ${cliArgs.mkString(" ")}")

    val commandLine = FlinkRunOption.parse(commandLineOptions, cliArgs, true)

    val activeCommandLine = validateAndGetActiveCommandLine(
      getCustomCommandLines(submitRequest.flinkVersion.flinkHome),
      commandLine)

    val configuration =
      applyConfiguration(submitRequest.flinkVersion.flinkHome, activeCommandLine, commandLine)

    commandLine -> configuration

  }

  private[client] def getCommandLineOptions(flinkHome: String) = {
    val customCommandLines = getCustomCommandLines(flinkHome)
    val customCommandLineOptions = new Options
    for (customCommandLine <- customCommandLines) {
      customCommandLine.addGeneralOptions(customCommandLineOptions)
      customCommandLine.addRunOptions(customCommandLineOptions)
    }
    FlinkRunOption.mergeOptions(CliFrontendParser.getRunCommandOptions, customCommandLineOptions)
  }

  private[client] def extractConfiguration(
      flinkHome: String,
      properties: JavaMap[String, Any]): Configuration = {
    val commandLine = {
      val commandLineOptions = getCommandLineOptions(flinkHome)
      // read and verify user config...
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
    val activeCommandLine =
      validateAndGetActiveCommandLine(getCustomCommandLines(flinkHome), commandLine)
    val flinkConfig = applyConfiguration(flinkHome, activeCommandLine, commandLine)
    flinkConfig
  }

  private[this] def extractProgramArgs(submitRequest: SubmitRequest): JavaList[String] = {
    val programArgs = new ArrayBuffer[String]()
    val args = submitRequest.args

    if (StringUtils.isNotBlank(args)) {
      val multiChar = "\""
      val array = args.split("\\s+")
      if (!array.exists(_.startsWith(multiChar))) {
        array.foreach(programArgs +=)
      } else {
        val argsArray = new ArrayBuffer[String]()
        val tempBuffer = new ArrayBuffer[String]()

        @tailrec
        def processElement(index: Int, multi: Boolean): Unit = {

          if (index == array.length) {
            if (tempBuffer.nonEmpty) {
              argsArray += tempBuffer.mkString(" ")
            }
            return
          }

          val next = index + 1
          val elem = array(index).trim

          if (elem.isEmpty) {
            processElement(next, multi = false)
          } else {
            if (multi) {
              if (elem.endsWith(multiChar)) {
                tempBuffer += elem.dropRight(1)
                argsArray += tempBuffer.mkString(" ")
                tempBuffer.clear()
                processElement(next, multi = false)
              } else {
                tempBuffer += elem
                processElement(next, multi)
              }
            } else {
              val until = if (elem.endsWith(multiChar)) 1 else 0
              if (elem.startsWith(multiChar)) {
                tempBuffer += elem.drop(1).dropRight(until)
                processElement(next, multi = true)
              } else {
                argsArray += elem.dropRight(until)
                processElement(next, multi = false)
              }
            }
          }
        }

        processElement(0, multi = false)
        argsArray.foreach(x => programArgs += x)
      }
    }

    if (submitRequest.applicationType == ApplicationTypeEnum.STREAMPARK_FLINK) {

      programArgs += PARAM_KEY_FLINK_CONF += submitRequest.flinkYaml
      programArgs += PARAM_KEY_APP_NAME += DeflaterUtils.zipString(submitRequest.effectiveAppName)
      programArgs += PARAM_KEY_FLINK_PARALLELISM += getParallelism(submitRequest).toString

      submitRequest.developmentMode match {
        case DevelopmentModeEnum.FLINK_SQL =>
          programArgs += PARAM_KEY_FLINK_SQL += submitRequest.flinkSQL
          if (submitRequest.appConf != null) {
            programArgs += PARAM_KEY_APP_CONF += submitRequest.appConf
          }
        case _ if Try(!submitRequest.appConf.startsWith("json:")).getOrElse(true) =>
          programArgs += PARAM_KEY_APP_CONF += submitRequest.appConf
      }

    }

    // execution.runtime-mode
    if (submitRequest.properties.nonEmpty) {
      if (submitRequest.properties.containsKey(ExecutionOptions.RUNTIME_MODE.key())) {
        programArgs += s"--${ExecutionOptions.RUNTIME_MODE.key()}"
        programArgs += submitRequest.properties.get(ExecutionOptions.RUNTIME_MODE.key()).toString
      }
    }

    if (
      submitRequest.developmentMode == DevelopmentModeEnum.PYFLINK
      && submitRequest.executionMode != ExecutionModeEnum.YARN_APPLICATION
    ) {
      // python file
      programArgs.add("-py")
      programArgs.add(submitRequest.userJarFile.getAbsolutePath)
    }
    programArgs.toList.asJava
  }

  private[this] def applyConfiguration(
      flinkHome: String,
      activeCustomCommandLine: CustomCommandLine,
      commandLine: CommandLine): Configuration = {

    require(activeCustomCommandLine != null, "activeCustomCommandLine must not be null.")
    val configuration = new Configuration()
    val flinkDefaultConfiguration = getFlinkDefaultConfiguration(flinkHome)
    flinkDefaultConfiguration.keySet.foreach(
      x => {
        flinkDefaultConfiguration.getString(x, null) match {
          case v if v != null => configuration.setString(x, v)
          case _ =>
        }
      })
    configuration.addAll(activeCustomCommandLine.toConfiguration(commandLine))
    configuration
  }

  implicit private[client] class EnhanceFlinkConfiguration(flinkConfig: Configuration) {
    def safeSet[T](option: ConfigOption[T], value: T): Configuration = {
      flinkConfig match {
        case x if value != null && value.toString.nonEmpty => x.set(option, value)
        case x => x
      }
    }
    def getOption[T](key: ConfigOption[T]): Option[T] = {
      Option(flinkConfig.get(key))
    }
    def remove[T](key: ConfigOption[T]): Configuration = {
      flinkConfig.removeConfig(key)
      flinkConfig
    }
  }

  private[client] def cancelJob(
      cancelRequest: CancelRequest,
      jobID: JobID,
      client: ClusterClient[_]): String = {

    val savePointDir: String = tryGetSavepointPathIfNeed(cancelRequest)

    val clientWrapper = new FlinkClusterClient(client)

    (
      Try(cancelRequest.withSavepoint).getOrElse(false),
      Try(cancelRequest.withDrain).getOrElse(false)) match {
      case (false, false) =>
        client.cancel(jobID).get()
        null
      case (true, false) =>
        clientWrapper
          .cancelWithSavepoint(jobID, savePointDir, cancelRequest.nativeFormat)
          .get()
      case (_, _) =>
        clientWrapper
          .stopWithSavepoint(
            jobID,
            cancelRequest.withDrain,
            savePointDir,
            cancelRequest.nativeFormat)
          .get()
    }
  }

  private def tryGetSavepointPathIfNeed(request: SavepointRequestTrait): String = {
    if (!request.withSavepoint) null
    else {
      if (StringUtils.isNotBlank(request.savepointPath)) {
        request.savepointPath
      } else {
        val configDir = getOptionFromDefaultFlinkConfig[String](
          request.flinkVersion.flinkHome,
          ConfigOptions
            .key(CheckpointingOptions.SAVEPOINT_DIRECTORY.key())
            .stringType()
            .defaultValue {
              if (request.executionMode == ExecutionModeEnum.YARN_APPLICATION) {
                Workspace.remote.APP_SAVEPOINTS
              } else null
            }
        )

        if (StringUtils.isBlank(configDir)) {
          throw new FlinkException(
            s"[StreamPark] executionMode: ${request.executionMode.getName}, savePoint path is null or invalid.")
        } else configDir

      }
    }
  }

  private[client] def triggerSavepoint(
      savepointRequest: TriggerSavepointRequest,
      jobID: JobID,
      client: ClusterClient[_]): String = {
    val savepointPath = tryGetSavepointPathIfNeed(savepointRequest)
    val clientWrapper = new FlinkClusterClient(client)
    clientWrapper.triggerSavepoint(jobID, savepointPath, savepointRequest.nativeFormat).get()
  }

}
