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

package com.streamxhub.streamx.flink.repl.interpreter

import com.streamxhub.streamx.common.util.{ClassLoaderUtils, DependencyUtils, HadoopUtils, Utils}
import com.streamxhub.streamx.flink.repl.shell.{FlinkILoop, FlinkShell}
import com.streamxhub.streamx.flink.repl.shell.FlinkShell._
import com.streamxhub.streamx.flink.repl.shims.FlinkShims
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.flink.api.common.JobExecutionResult
import org.apache.flink.api.java.{ExecutionEnvironmentFactory, ExecutionEnvironment => JExecutionEnvironment}
import org.apache.flink.api.scala.ExecutionEnvironment
import org.apache.flink.client.program.ClusterClient
import org.apache.flink.configuration._
import org.apache.flink.core.execution.{JobClient, JobListener}
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.environment.{StreamExecutionEnvironmentFactory, StreamExecutionEnvironment => JStreamExecutionEnvironment}
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.table.api.bridge.scala.StreamTableEnvironment
import org.apache.flink.yarn.entrypoint.YarnJobClusterEntrypoint
import org.apache.hadoop.conf.{Configuration => HdfsConfig}
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.hadoop.yarn.api.records.ApplicationId
import org.slf4j.LoggerFactory

import java.io.{BufferedReader, File, IOException}
import java.net.URLClassLoader
import java.nio.file.Files
import java.util.Properties
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer
import scala.tools.nsc.Settings
import scala.tools.nsc.interpreter.{IR, JPrintWriter, SimpleReader}

/**
 * It instantiate flink scala shell and create env, benv.
 *
 */

class FlinkScalaInterpreter(properties: Properties) {

  private lazy val LOGGER = LoggerFactory.getLogger(classOf[FlinkScalaInterpreter])

  private val interpreterOutput = new InterpreterOutputStream(LOGGER)

  private var flinkILoop: FlinkILoop = _
  private var cluster: Option[ClusterClient[_]] = _
  private var configuration: Configuration = _
  private var mode: ExecutionMode.Value = _
  private var streamEnv: StreamExecutionEnvironment = _
  var jobClient: JobClient = _
  var flinkHome: String = _
  var flinkShims: FlinkShims = _
  var jmWebUrl: String = _
  var replacedJMWebUrl: String = _
  var defaultParallelism = 1
  var userJars: Seq[String] = _
  var userUdfJars: Seq[String] = _
  var userCodeJarFile: File = _

  def open(flinkHome: String): Unit = {
    val config = initFlinkConfig(flinkHome)
    createFlinkILoop(config)
    val modifiers = new ArrayBuffer[String]
    modifiers + "@transient"
    // register JobListener
    val jobListener = new JobListener() {
      override def onJobSubmitted(jobClient: JobClient, e: Throwable): Unit = {
        if (e != null) {
          LOGGER.warn("Fail to submit job")
        } else {
          FlinkScalaInterpreter.this.jobClient = jobClient
        }
      }

      override def onJobExecuted(jobExecutionResult: JobExecutionResult, e: Throwable): Unit = {
        if (e != null) {
          LOGGER.warn("Fail to execute job")
        } else {
          logInfo(s"Job ${jobExecutionResult.getJobID} is executed with time ${jobExecutionResult.getNetRuntime(TimeUnit.SECONDS)} seconds")
        }
      }
    }
    this.streamEnv.registerJobListener(jobListener)
  }

  private def initFlinkConfig(flink: String): Config = {
    this.flinkHome = {
      if (Utils.isEmpty(flink)) {
        val flinkLocalHome = System.getenv("FLINK_HOME")
        require(flinkLocalHome != null)
        logInfo(s"flinkHome: $flinkLocalHome")
        flinkLocalHome
      } else {
        flink
      }
    }
    require(flinkHome != null)
    val flinkConfDir = sys.env.getOrElse("FLINK_CONF_DIR", s"$flinkHome/conf")
    val hadoopConfDir = sys.env.getOrElse("HADOOP_CONF_DIR", "")
    val yarnConfDir = sys.env.getOrElse("YARN_CONF_DIR", "")
    val hiveConfDir = sys.env.getOrElse("HIVE_CONF_DIR", "")
    logInfo(s"FLINK_HOME: $flinkHome")
    logInfo(s"FLINK_CONF_DIR: $flinkHome")
    logInfo(s"HADOOP_CONF_DIR: $hadoopConfDir")
    logInfo(s"YARN_CONF_DIR: $yarnConfDir")
    logInfo(s"HIVE_CONF_DIR: $hiveConfDir")

    this.mode = ExecutionMode.withName(properties.getProperty("flink.execution.mode", "LOCAL").toUpperCase)
    var config = Config(executionMode = mode)

    this.flinkShims = new FlinkShims(properties)
    this.configuration = GlobalConfiguration.loadConfiguration(flinkConfDir)

    val jmMemory = properties.getProperty("flink.jm.memory", "1024")
    config = config.copy(yarnConfig = Some(ensureYarnConfig(config).copy(jobManagerMemory = Some(jmMemory))))

    val tmMemory = properties.getProperty("flink.tm.memory", "1024")
    config = config.copy(yarnConfig = Some(ensureYarnConfig(config).copy(taskManagerMemory = Some(tmMemory))))

    val appName = properties.getProperty("flink.yarn.appName", "Flink Yarn App Name")
    config = config.copy(yarnConfig = Some(ensureYarnConfig(config).copy(name = Some(appName))))

    val slotNum = properties.getProperty("flink.tm.slot", "1").toInt
    config = config.copy(yarnConfig = Some(ensureYarnConfig(config).copy(slots = Some(slotNum))))

    this.configuration.setInteger("taskmanager.numberOfTaskSlots", slotNum)

    val queue = properties.getProperty("flink.yarn.queue", "default")
    config = config.copy(yarnConfig = Some(ensureYarnConfig(config).copy(queue = Some(queue))))

    this.userUdfJars = getUserUdfJars()
    this.userJars = getUserJarsExceptUdfJars ++ this.userUdfJars
    if (this.userJars.nonEmpty) {
      logInfo("UserJars: " + userJars.mkString(","))
      config = config.copy(externalJars = Some(userJars.toArray))
      logInfo("Config: " + config)
      configuration.setString("flink.yarn.jars", userJars.mkString(":"))
    }

    // load other configuration from interpreter properties
    properties.foreach(entry => configuration.setString(entry._1, entry._2))
    this.defaultParallelism = configuration.getInteger(CoreOptions.DEFAULT_PARALLELISM)
    logInfo("Default Parallelism: " + this.defaultParallelism)

    // set scala.color
    if (properties.getProperty("flink.scala.color", "true").toBoolean) {
      System.setProperty("scala.color", "true")
    }
    // set host/port when it is remote mode
    if (config.executionMode == ExecutionMode.REMOTE) {
      val host = properties.getProperty("flink.execution.remote.host")
      val port = properties.getProperty("flink.execution.remote.port")
      if (host == null) {
        throw new RuntimeException("flink.execution.remote.host is not specified when using REMOTE mode")
      }
      if (port == null) {
        throw new RuntimeException("flink.execution.remote.port is not specified when using REMOTE mode")
      }
      config = config.copy(host = Some(host)).copy(port = Some(Integer.parseInt(port)))
    }
    config
  }

  private def createFlinkILoop(config: Config): Unit = {
    val printReplOutput = properties.getProperty("flink.repl.out", "true").toBoolean
    val replOut = if (printReplOutput) {
      new JPrintWriter(interpreterOutput, true)
    } else {
      new JPrintWriter(Console.out, true)
    }
    val (iLoop, cluster) = {
      // workaround of checking hadoop jars in yarn  mode
      mode match {
        case ExecutionMode.YARN =>
          try {
            Class.forName(classOf[YarnJobClusterEntrypoint].getName)
          } catch {
            case e: ClassNotFoundException => throw new RuntimeException("Unable to load FlinkYarnSessionCli for yarn mode", e)
            case e: NoClassDefFoundError => throw new RuntimeException("No hadoop jar found, make sure you have hadoop command in your PATH", e)
          }
        case _ =>
      }

      val (effectiveConfiguration, cluster) = FlinkShell.getClusterClient(config, configuration, flinkShims)
      this.configuration = effectiveConfiguration

      cluster match {
        case Some(clusterClient) =>
          // local mode or yarn
          mode match {
            case ExecutionMode.LOCAL =>
              logInfo("Starting FlinkCluster in local mode")
              this.jmWebUrl = clusterClient.getWebInterfaceURL
            case ExecutionMode.YARN =>
              logInfo("Starting FlinkCluster in yarn mode")
              if (properties.getProperty("flink.webui.yarn.useProxy", "false").toBoolean) {
                this.jmWebUrl = HadoopUtils.getYarnAppTrackingUrl(clusterClient.getClusterId.asInstanceOf[ApplicationId])
                // for some cloud vender, the yarn address may be mapped to some other address.
                val yarnAddress = properties.getProperty("flink.webui.yarn.address")
                if (!StringUtils.isBlank(yarnAddress)) {
                  this.replacedJMWebUrl = replaceYarnAddress(this.jmWebUrl, yarnAddress)
                }
              } else {
                this.jmWebUrl = clusterClient.getWebInterfaceURL
              }
            case _ =>
              throw new Exception(s"Starting FlinkCluster in invalid mode: $mode")
          }
        case None =>
          // remote mode
          logInfo("Use FlinkCluster in remote mode")
          this.jmWebUrl = s"http://${config.host.get}:${config.port.get}"
      }

      logInfo(s"\nConnecting to Flink cluster: ${this.jmWebUrl}")

      logInfo("externalJars: " + config.externalJars.getOrElse(Array.empty[String]).mkString(":"))
      try {
        // use FlinkClassLoader to initialize FlinkILoop, otherwise TableFactoryService could not find
        ClassLoaderUtils.runAsClassLoader(getFlinkClassLoader, () => {
          val iLoop = new FlinkILoop(configuration, config.externalJars, None, replOut)
          (iLoop, cluster)
        })
      } catch {
        case e: Exception =>
          LOGGER.error(ExceptionUtils.getStackTrace(e))
          throw e
      }
    }

    this.flinkILoop = iLoop
    this.cluster = cluster

    val settings = new Settings()
    settings.usejavacp.value = true
    settings.Yreplsync.value = true
    settings.classpath.value = userJars.mkString(File.pathSeparator)
    settings.classpath.append(System.getProperty("java.class.path"))
    val outputDir = Files.createTempDirectory("flink-repl");
    val interpArguments = List(
      "-Yrepl-class-based",
      "-Yrepl-outdir", s"${outputDir.toFile.getAbsolutePath}"
    )
    settings.processArguments(interpArguments, true)

    flinkILoop.settings = settings
    flinkILoop.intp = new FlinkILoopInterpreter(settings, replOut)
    flinkILoop.intp.beQuietDuring {
      // set execution environment
      flinkILoop.intp.bind("env", flinkILoop.scalaSenv)

      val packageImports = Seq[String](
        "org.apache.flink.core.fs._",
        "org.apache.flink.core.fs.local._",
        "org.apache.flink.api.common.io._",
        "org.apache.flink.api.common.aggregators._",
        "org.apache.flink.api.common.accumulators._",
        "org.apache.flink.api.common.distributions._",
        "org.apache.flink.api.common.operators._",
        "org.apache.flink.api.common.operators.base.JoinOperatorBase.JoinHint",
        "org.apache.flink.api.common.functions._",
        "org.apache.flink.api.java.io._",
        "org.apache.flink.util._",
        "org.apache.flink.api.java.aggregation._",
        "org.apache.flink.api.java.functions._",
        "org.apache.flink.api.java.operators._",
        "org.apache.flink.api.java.sampling._",
        "org.apache.flink.api.scala._",
        "org.apache.flink.api.scala.utils._",
        "org.apache.flink.streaming.api._",
        "org.apache.flink.streaming.api.scala._",
        "org.apache.flink.streaming.api.windowing.time._",
        "org.apache.flink.types.Row"
      )

      flinkILoop.intp.interpret("import " + packageImports.mkString(", "))
    }

    val in0 = getField(flinkILoop, "scala$tools$nsc$interpreter$ILoop$$in0").asInstanceOf[Option[BufferedReader]]
    val reader = in0.fold(flinkILoop.chooseReader(settings))(r => SimpleReader(r, replOut, interactive = true))

    flinkILoop.in = reader
    flinkILoop.initializeSynchronous()
    flinkILoop.intp.setContextClassLoader()
    reader.postInit()

    this.streamEnv = flinkILoop.scalaSenv
    val timeType = properties.getProperty("flink.senv.timecharacteristic", "EventTime")
    this.streamEnv.getJavaEnv.setStreamTimeCharacteristic(TimeCharacteristic.valueOf(timeType))
    this.streamEnv.setParallelism(configuration.getInteger(CoreOptions.DEFAULT_PARALLELISM))
    setAsContext()
  }

  private def setAsContext(): Unit = {
    val streamFactory = new StreamExecutionEnvironmentFactory() {
      override def createExecutionEnvironment(configuration: Configuration): JStreamExecutionEnvironment = {
        streamEnv.configure(configuration, getFlinkClassLoader)
        streamEnv.getJavaEnv
      }
    }
    //StreamExecutionEnvironment
    var method = classOf[JStreamExecutionEnvironment].getDeclaredMethod("initializeContextEnvironment", classOf[StreamExecutionEnvironmentFactory])
    method.setAccessible(true)
    method.invoke(null, streamFactory)

    //StreamExecutionEnvironment
    method = classOf[JExecutionEnvironment].getDeclaredMethod("initializeContextEnvironment", classOf[ExecutionEnvironmentFactory])
    method.setAccessible(true)
  }

  // for use in java side
  protected def bind(name: String,
                     tpe: String,
                     value: Object,
                     modifier: List[String]): Unit = {
    flinkILoop.beQuietDuring {
      flinkILoop.bind(name, tpe, value, modifier)
    }
  }

  protected def callMethod(obj: Object, name: String): Object = {
    callMethod(obj, name, Array.empty[Class[_]], Array.empty[Object])
  }

  protected def callMethod(obj: Object, name: String,
                           parameterTypes: Array[Class[_]],
                           parameters: Array[Object]): Object = {
    val method = obj.getClass.getMethod(name, parameterTypes: _ *)
    method.setAccessible(true)
    method.invoke(obj, parameters: _ *)
  }


  protected def getField(obj: Object, name: String): Object = {
    val field = obj.getClass.getField(name)
    field.setAccessible(true)
    field.get(obj)
  }


  /**
   *
   * @param code
   * @param out
   * @return
   */
  def interpret(code: String, out: InterpreterOutput): InterpreterResult = {
    logInfo(s" interpret starting!code:\n$code\n")
    val originalStdOut = System.out
    val originalStdErr = System.err
    interpreterOutput.setInterpreterOutput(out)

    Console.withOut(out) {
      System.setOut(Console.out)
      System.setErr(Console.out)
      interpreterOutput.ignoreLeadingNewLinesFromScalaReporter()
      // add print("") at the end in case the last line is comment which lead to INCOMPLETE
      val lines = code.split("\\n") ++ List("print(\"\")")
      var incompleteCode = ""
      var lastStatus: InterpreterResult.Value = null
      for ((line, i) <- lines.zipWithIndex if line.trim.nonEmpty) {
        val nextLine = if (incompleteCode != "") {
          incompleteCode + "\n" + line
        } else {
          line
        }
        if (i < (lines.length - 1) && lines(i + 1).trim.startsWith(".")) {
          incompleteCode = nextLine
        } else {
          lastStatus = flinkILoop.interpret(nextLine) match {
            case IR.Error => InterpreterResult.ERROR
            case IR.Success =>
              // continue the next line
              incompleteCode = ""
              InterpreterResult.SUCCESS
            case IR.Incomplete =>
              // put this line into inCompleteCode for the next execution.
              incompleteCode = incompleteCode + "\n" + line
              InterpreterResult.INCOMPLETE
          }
        }
      }
      // flush all output before returning result to frontend
      Console.flush()
      interpreterOutput.setInterpreterOutput(null)
      // reset the java stdout
      System.setOut(originalStdOut)
      System.setErr(originalStdErr)
      new InterpreterResult(lastStatus)
    }
  }

  def close(): Unit = {
    logInfo("Closing FlinkScalaInterpreter")
    if (properties.getProperty("flink.interpreter.close.shutdown_cluster", "true").toBoolean) {
      if (cluster != null) {
        cluster match {
          case Some(clusterClient) =>
            logInfo("Shutdown FlinkCluster")
            clusterClient.shutDownCluster()
            clusterClient.close()
            // delete staging dir
            if (mode == ExecutionMode.YARN) {
              try {
                val appId = clusterClient.getClusterId.asInstanceOf[ApplicationId]
                val fs = FileSystem.get(new HdfsConfig)
                val stagingDirPath = new Path(fs.getHomeDirectory, ".flink/" + appId.toString)
                if (fs.delete(stagingDirPath, true)) LOGGER.info("Deleted staging directory " + stagingDirPath)
              } catch {
                case e: IOException =>
                  LOGGER.warn("Failed to cleanup staging dir", e)
              }
            }
          case None =>
            logInfo("Don't close the Remote FlinkCluster")
        }
      }
    } else {
      logInfo("Keep cluster alive when closing interpreter")
    }

    if (flinkILoop != null) {
      flinkILoop.closeInterpreter()
      flinkILoop = null
    }

    if (jobClient != null) {
      jobClient.cancel()
    }

  }

  def getStreamExecutionEnvironment(): StreamExecutionEnvironment = this.streamEnv

  private def getUserJarsExceptUdfJars: Seq[String] = {
    val flinkJars =
      if (!StringUtils.isBlank(properties.getProperty("flink.execution.jars", ""))) {
        getOrDownloadJars(properties.getProperty("flink.execution.jars").split(",").toSeq)
      } else {
        Seq.empty[String]
      }

    val flinkPackageJars =
      if (!StringUtils.isBlank(properties.getProperty("flink.execution.packages", ""))) {
        val packages = properties.getProperty("flink.execution.packages")
        DependencyUtils.resolveMavenDependencies(null, packages, null, null, null, new Consumer[String]() {
          override def accept(t: String): Unit = logInfo(t)
        })
      } else {
        Seq.empty[String]
      }

    flinkJars ++ flinkPackageJars
  }

  private def getUserUdfJars(): Seq[String] = {
    if (!StringUtils.isBlank(properties.getProperty("flink.udf.jars", ""))) {
      getOrDownloadJars(properties.getProperty("flink.udf.jars").split(",").toSeq)
    } else {
      Seq.empty[String]
    }
  }

  private def getOrDownloadJars(jars: Seq[String]): Seq[String] = {
    jars.map(jar => {
      if (jar.contains("://")) {
        HadoopUtils.downloadJar(jar)
      } else {
        val jarFile = new File(jar)
        if (!jarFile.exists() || !jarFile.isFile) {
          throw new Exception(s"jar file: ${jar} doesn't exist")
        } else {
          jar
        }
      }
    })
  }

  def getFlinkScalaShellLoader: ClassLoader = new URLClassLoader(Array(getUserCodeJarFile().toURL) ++ userJars.map(e => new File(e).toURL))

  def getUserCodeJarFile(): File = {
    this.userCodeJarFile match {
      case null =>
        this.userCodeJarFile = this.flinkILoop.writeFilesToDisk()
        this.userCodeJarFile
      case other => other
    }
  }

  private def getFlinkClassLoader: ClassLoader = {
    new URLClassLoader(userJars.map(e => new File(e).toURI.toURL).toArray)
  }

  def getConfiguration: Configuration = this.configuration

  def getCluster: Option[ClusterClient[_]] = cluster

  def getFlinkILoop: FlinkILoop = flinkILoop

  def getFlinkShims: FlinkShims = flinkShims

  def replaceYarnAddress(webURL: String, yarnAddress: String): String = {
    val pattern = "(https?://.*:\\d+)(.*)".r
    val pattern(_, remaining) = webURL
    yarnAddress + remaining
  }
}


