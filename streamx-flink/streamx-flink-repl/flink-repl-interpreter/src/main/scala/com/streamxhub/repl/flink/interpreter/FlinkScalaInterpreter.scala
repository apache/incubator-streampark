/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.repl.flink.interpreter

import java.io.{BufferedReader, File}
import java.net.{URL, URLClassLoader}
import java.nio.file.Files
import java.util.Properties
import java.util.concurrent.TimeUnit
import java.util.jar.JarFile

import com.streamxhub.common.util.{ClassLoaderUtils, Logger}
import com.streamxhub.repl.flink.interpreter.FlinkShell.{Config, ExecutionMode, _}
import com.streamxhub.repl.flink.shims.FlinkShims
import com.streamxhub.repl.flink.util.{DependencyUtils, HadoopUtils}
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.flink.api.common.JobExecutionResult
import org.apache.flink.api.java.{ExecutionEnvironmentFactory, ExecutionEnvironment => JExecutionEnvironment}
import org.apache.flink.api.scala.{ExecutionEnvironment, FlinkILoop}
import org.apache.flink.client.program.ClusterClient
import org.apache.flink.configuration._
import org.apache.flink.core.execution.{JobClient, JobListener}
import org.apache.flink.runtime.jobgraph.SavepointConfigOptions
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.environment.{StreamExecutionEnvironmentFactory, StreamExecutionEnvironment => JStreamExecutionEnvironment}
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.table.api.config.ExecutionConfigOptions
import org.apache.flink.table.api.{EnvironmentSettings, TableConfig, TableEnvironment}
import org.apache.flink.table.catalog.hive.HiveCatalog
import org.apache.flink.table.catalog.{CatalogManager, FunctionCatalog}
import org.apache.flink.table.functions.{AggregateFunction, ScalarFunction, TableAggregateFunction, TableFunction}
import org.apache.flink.table.module.ModuleManager
import org.apache.flink.table.module.hive.HiveModule
import org.apache.flink.yarn.entrypoint.YarnJobClusterEntrypoint
import org.apache.zeppelin.interpreter.thrift.InterpreterCompletion
import org.apache.zeppelin.interpreter.util.InterpreterOutputStream
import org.apache.zeppelin.interpreter.{InterpreterContext, InterpreterException, InterpreterHookRegistry, InterpreterResult}

import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer
import scala.tools.nsc.Settings
import scala.tools.nsc.interpreter.Completion.ScalaCompleter
import scala.tools.nsc.interpreter.{JPrintWriter, SimpleReader}

/**
 * It instantiate flink scala shell and create env, senv, btenv, stenv.
 *
 * @param properties
 */
class FlinkScalaInterpreter(properties: Properties) extends Logger {

  private var flinkILoop: FlinkILoop = _
  private var cluster: Option[ClusterClient[_]] = _
  private var scalaCompleter: ScalaCompleter = _
  private val interpreterOutput = new InterpreterOutputStream(logger)
  private var configuration: Configuration = _
  private var mode: ExecutionMode.Value = _
  private var tableEnvFactory: TableEnvFactory = _
  private var benv: ExecutionEnvironment = _
  private var senv: StreamExecutionEnvironment = _

  // TableEnvironment of blink planner
  private var btenv: TableEnvironment = _
  private var stenv: TableEnvironment = _

  // TableEnvironment of flink planner
  private var btenv_2: TableEnvironment = _
  private var stenv_2: TableEnvironment = _

  // PyFlink depends on java version of TableEnvironment,
  // so need to create java version of TableEnvironment
  private var java_btenv: TableEnvironment = _
  private var java_stenv: TableEnvironment = _

  private var java_btenv_2: TableEnvironment = _
  private var java_stenv_2: TableEnvironment = _

  var flinkReplContext: FlinkReplContext = _
  var flinkShims: FlinkShims = _
  var jmWebUrl: String = _
  var replacedJMWebUrl: String = _
  var jobManager: JobManager = _
  var defaultParallelism = 1
  var defaultSqlParallelism = 1
  var userJars: Seq[String] = _
  var userUdfJars: Seq[String] = _
  var userCodeJarFile: File = _

  def open(): Unit = {
    val config = initFlinkConfig()
    createFlinkILoop(config)
    createTableEnvs()
    setTableEnvConfig()
    // init
    this.flinkReplContext = new FlinkReplContext(
      this,
      new InterpreterHookRegistry(),
      properties.getProperty("zeppelin.flink.maxResult", "1000").toInt
    )
    val modifiers = new ArrayBuffer[String]
    modifiers + "@transient"
    this.bind("z", flinkReplContext.getClass.getCanonicalName, flinkReplContext, modifiers.toList)
    this.jobManager = new JobManager(this.flinkReplContext, jmWebUrl, replacedJMWebUrl)
    // register JobListener
    val jobListener = new FlinkJobListener()
    this.benv.registerJobListener(jobListener)
    this.senv.registerJobListener(jobListener)
    // register hive catalog
    if (properties.getProperty("zeppelin.flink.enableHive", "false").toBoolean) {
      logInfo("Hive is enabled, registering hive catalog.")
      registerHiveCatalog()
    } else {
      logInfo("Hive is disabled.")
    }
    // load udf jar
    this.userUdfJars.foreach(loadUDFJar)
  }

  private def initFlinkConfig(): Config = {
    val flinkHome = System.getenv("FLINK_HOME")
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

    this.flinkShims = FlinkShims.getInstance(properties)
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
    logInfo("UserJars: " + userJars.mkString(","))
    config = config.copy(externalJars = Some(userJars.toArray))
    logInfo("Config: " + config)
    configuration.setString("flink.yarn.jars", userJars.mkString(":"))

    // load other configuration from interpreter properties
    properties.asScala.foreach(entry => configuration.setString(entry._1, entry._2))
    this.defaultParallelism = configuration.getInteger(CoreOptions.DEFAULT_PARALLELISM)
    this.defaultSqlParallelism = configuration.getInteger(ExecutionConfigOptions.TABLE_EXEC_RESOURCE_DEFAULT_PARALLELISM)
    logInfo("Default Parallelism: " + this.defaultParallelism)
    logInfo("Default SQL Parallelism: " + this.defaultSqlParallelism)

    // set scala.color
    if (properties.getProperty("scala.color", "true").toBoolean) {
      System.setProperty("scala.color", "true")
    }
    // set host/port when it is remote mode
    if (config.executionMode == ExecutionMode.REMOTE) {
      val host = properties.getProperty("flink.execution.remote.host")
      val port = properties.getProperty("flink.execution.remote.port")
      if (host == null) {
        throw new InterpreterException("flink.execution.remote.host is not specified when using REMOTE mode")
      }
      if (port == null) {
        throw new InterpreterException("flink.execution.remote.port is not specified when using REMOTE mode")
      }
      config = config.copy(host = Some(host)).copy(port = Some(Integer.parseInt(port)))
    }

    config
  }

  private def createFlinkILoop(config: Config): Unit = {
    val printReplOutput = properties.getProperty("repl.out", "true").toBoolean
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
            case e: ClassNotFoundException => throw new InterpreterException("Unable to load FlinkYarnSessionCli for yarn mode", e)
            case e: NoClassDefFoundError => throw new InterpreterException("No hadoop jar found, make sure you have hadoop command in your PATH", e)
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
                this.jmWebUrl = HadoopUtils.getYarnAppTrackingUrl(clusterClient)
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
      if (InterpreterContext.get() != null) {
        //        InterpreterContext.get().getIntpEventClient.sendWebUrlInfo(this.jmWebUrl)
      }

      logInfo("externalJars: " + config.externalJars.getOrElse(Array.empty[String]).mkString(":"))
      try {
        // use FlinkClassLoader to initialize FlinkILoop, otherwise TableFactoryService could not find
        ClassLoaderUtils.runAsClassLoader(getFlinkClassLoader, () => {
          val iLoop = new FlinkILoop(configuration, config.externalJars, None, replOut)
          (iLoop, cluster)
        })
      } catch {
        case e: Exception =>
          logError(ExceptionUtils.getStackTrace(e))
          throw e
      }
    }

    this.flinkILoop = iLoop
    this.cluster = cluster

    val settings = new Settings()
    settings.usejavacp.value = true
    settings.Yreplsync.value = true
    settings.classpath.value = userJars.mkString(File.pathSeparator)

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
      flinkILoop.intp.bind("benv", flinkILoop.scalaBenv)
      flinkILoop.intp.bind("senv", flinkILoop.scalaSenv)

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
        "org.apache.flink.api.java.aggregation._",
        "org.apache.flink.api.java.functions._",
        "org.apache.flink.api.java.operators._",
        "org.apache.flink.api.java.sampling._",
        "org.apache.flink.api.scala._",
        "org.apache.flink.api.scala.utils._",
        "org.apache.flink.streaming.api.scala._",
        "org.apache.flink.streaming.api.windowing.time._",
        "org.apache.flink.types.Row"
      )

      flinkILoop.intp.interpret("import " + packageImports.mkString(", "))
      flinkILoop.intp.interpret("import org.apache.flink.table.api._")
      flinkILoop.intp.interpret("import org.apache.flink.table.api.bridge.scala._")
      flinkILoop.intp.interpret("import org.apache.flink.table.functions.ScalarFunction")
      flinkILoop.intp.interpret("import org.apache.flink.table.functions.AggregateFunction")
      flinkILoop.intp.interpret("import org.apache.flink.table.functions.TableFunction")
      flinkILoop.intp.interpret("import org.apache.flink.table.functions.TableAggregateFunction")
    }

    val in0 = getField(flinkILoop, "scala$tools$nsc$interpreter$ILoop$$in0").asInstanceOf[Option[BufferedReader]]
    val reader = in0.fold(flinkILoop.chooseReader(settings))(r => SimpleReader(r, replOut, interactive = true))

    flinkILoop.in = reader
    flinkILoop.initializeSynchronous()
    flinkILoop.intp.setContextClassLoader()
    reader.postInit()
    this.scalaCompleter = reader.completion.completer()

    this.benv = flinkILoop.scalaBenv
    this.senv = flinkILoop.scalaSenv
    val timeType = properties.getProperty("flink.senv.timecharacteristic", "EventTime")
    this.senv.setStreamTimeCharacteristic(TimeCharacteristic.valueOf(timeType))
    this.benv.setParallelism(configuration.getInteger(CoreOptions.DEFAULT_PARALLELISM))
    this.senv.setParallelism(configuration.getInteger(CoreOptions.DEFAULT_PARALLELISM))

    setAsContext()
  }

  private def createTableEnvs(): Unit = {
    ClassLoaderUtils.runAsClassLoader(getFlinkClassLoader, () => {
      val tblConfig = new TableConfig
      tblConfig.getConfiguration.addAll(configuration)
      // Step 1.1 Initialize the CatalogManager if required.
      val catalogManager = flinkShims.createCatalogManager(tblConfig.getConfiguration).asInstanceOf[CatalogManager]
      // Step 1.2 Initialize the ModuleManager if required.
      val moduleManager = new ModuleManager();
      // Step 1.3 Initialize the FunctionCatalog if required.
      val flinkFunctionCatalog = new FunctionCatalog(tblConfig, catalogManager, moduleManager);
      val blinkFunctionCatalog = new FunctionCatalog(tblConfig, catalogManager, moduleManager);

      this.tableEnvFactory = new TableEnvFactory(
        this.flinkShims,
        this.benv,
        this.senv,
        tblConfig,
        catalogManager,
        moduleManager,
        flinkFunctionCatalog,
        blinkFunctionCatalog)

      val modifiers = new java.util.ArrayList[String]()
      modifiers.add("@transient")

      // blink planner
      var btEnvSetting = EnvironmentSettings.newInstance().inBatchMode().useBlinkPlanner().build()
      this.btenv = tableEnvFactory.createJavaBlinkBatchTableEnvironment(btEnvSetting, getFlinkClassLoader);
      flinkILoop.bind("btenv", btenv.getClass().getCanonicalName(), btenv, List("@transient"))
      this.java_btenv = this.btenv

      var stEnvSetting = EnvironmentSettings.newInstance().inStreamingMode().useBlinkPlanner().build()
      this.stenv = tableEnvFactory.createScalaBlinkStreamTableEnvironment(stEnvSetting, getFlinkClassLoader)
      flinkILoop.bind("stenv", stenv.getClass().getCanonicalName(), stenv, List("@transient"))
      this.java_stenv = tableEnvFactory.createJavaBlinkStreamTableEnvironment(stEnvSetting, getFlinkClassLoader)

      // flink planner
      this.btenv_2 = tableEnvFactory.createScalaFlinkBatchTableEnvironment()
      flinkILoop.bind("btenv_2", btenv_2.getClass().getCanonicalName(), btenv_2, List("@transient"))
      stEnvSetting = EnvironmentSettings.newInstance().inStreamingMode().useOldPlanner().build()
      this.stenv_2 = tableEnvFactory.createScalaFlinkStreamTableEnvironment(stEnvSetting, getFlinkClassLoader)
      flinkILoop.bind("stenv_2", stenv_2.getClass().getCanonicalName(), stenv_2, List("@transient"))

      this.java_btenv_2 = tableEnvFactory.createJavaFlinkBatchTableEnvironment()
      btEnvSetting = EnvironmentSettings.newInstance.useOldPlanner.inStreamingMode.build
      this.java_stenv_2 = tableEnvFactory.createJavaFlinkStreamTableEnvironment(btEnvSetting, getFlinkClassLoader)
    })
  }

  private def setTableEnvConfig(): Unit = {
    this.properties.asScala.filter(_._1.startsWith("table.exec"))
      .foreach(e => {
        this.btenv.getConfig.getConfiguration.setString(e._1, e._2)
        this.java_btenv.getConfig.getConfiguration.setString(e._1, e._2)
        this.stenv.getConfig.getConfiguration.setString(e._1, e._2)
        this.java_stenv.getConfig.getConfiguration.setString(e._1, e._2)
      })

    // set python exec for PyFlink
    val pythonExec = properties.getProperty("zeppelin.pyflink.python", "")
    if (!StringUtils.isBlank(pythonExec)) {
      this.stenv.getConfig.getConfiguration.setString("python.exec", pythonExec)
      this.btenv.getConfig.getConfiguration.setString("python.exec", pythonExec)
      this.java_btenv.getConfig.getConfiguration.setString("python.exec", pythonExec)
      this.java_stenv.getConfig.getConfiguration.setString("python.exec", pythonExec)
    }

    if (java.lang.Boolean.parseBoolean(
      properties.getProperty("zeppelin.flink.disableSysoutLogging", "true"))) {
      this.benv.getConfig.disableSysoutLogging()
      this.senv.getConfig.disableSysoutLogging()
    }
  }

  private def registerHiveCatalog(): Unit = {
    val hiveConfDir = properties.getOrDefault("HIVE_CONF_DIR", sys.env.get("HIVE_CONF_DIR")).toString
    if (hiveConfDir == null) {
      throw new InterpreterException("HIVE_CONF_DIR is not specified");
    }
    val database = properties.getProperty("zeppelin.flink.hive.database", "default")
    val hiveVersion = properties.getProperty("zeppelin.flink.hive.version", "2.3.4")
    val hiveCatalog = new HiveCatalog("hive", database, hiveConfDir, hiveVersion)
    this.btenv.registerCatalog("hive", hiveCatalog)
    this.btenv.useCatalog("hive")
    this.btenv.useDatabase(database)
    if (properties.getProperty("zeppelin.flink.module.enableHive", "false").toBoolean) {
      this.btenv.loadModule("hive", new HiveModule(hiveVersion))
    }
  }

  private def loadUDFJar(jar: String): Unit = {
    logInfo("Loading UDF Jar: " + jar)
    val jarFile = new JarFile(jar)
    val entries = jarFile.entries

    val udfPackages = properties.getProperty("flink.udf.jars.packages", "").split(",").toSet
    val urls = Array(new URL(s"jar:file:$jar!/"))
    val cl = new URLClassLoader(urls, getFlinkScalaShellLoader)

    while (entries.hasMoreElements) {
      val je = entries.nextElement
      if (!je.isDirectory && je.getName.endsWith(".class") && !je.getName.contains("$")) {
        try {
          // -6 because of .class
          var className = je.getName.substring(0, je.getName.length - 6)
          className = className.replace('/', '.')
          if (udfPackages.isEmpty || udfPackages.exists(p => className.startsWith(p))) {
            val c = cl.loadClass(className)
            val udf = c.newInstance()
            udf match {
              case scalarUDF: ScalarFunction =>
                btenv.registerFunction(c.getSimpleName, scalarUDF)
              case tableUDF: TableFunction[_] =>
                flinkShims.registerTableFunction(btenv, c.getSimpleName, tableUDF)
              case aggregateUDF: AggregateFunction[_, _] =>
                flinkShims.registerAggregateFunction(btenv, c.getSimpleName, aggregateUDF)
              case tableAggregateUDF: TableAggregateFunction[_, _] =>
                flinkShims.registerTableAggregateFunction(btenv, c.getSimpleName, tableAggregateUDF)
              case _ => logWarn("No UDF definition found in class file: " + je.getName)
            }
          }
        } catch {
          case e: Throwable => logInfo("Fail to inspect udf class: " + je.getName, e)
        }
      }
    }
  }

  private def setAsContext(): Unit = {
    val streamFactory = new StreamExecutionEnvironmentFactory() {
      override def createExecutionEnvironment: JStreamExecutionEnvironment = senv.getJavaEnv
    }
    //StreamExecutionEnvironment
    var method = classOf[JStreamExecutionEnvironment].getDeclaredMethod("initializeContextEnvironment", classOf[StreamExecutionEnvironmentFactory])
    method.setAccessible(true)
    method.invoke(null, streamFactory)

    val batchFactory = new ExecutionEnvironmentFactory() {
      override def createExecutionEnvironment: JExecutionEnvironment = benv.getJavaEnv
    }
    //StreamExecutionEnvironment
    method = classOf[JExecutionEnvironment].getDeclaredMethod("initializeContextEnvironment", classOf[ExecutionEnvironmentFactory])
    method.setAccessible(true)
    method.invoke(null, batchFactory)
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

  def completion(buf: String,
                 cursor: Int,
                 context: InterpreterContext): java.util.List[InterpreterCompletion] = {
    scalaCompleter
      .complete(buf.substring(0, cursor), cursor)
      .candidates
      .map(e => new InterpreterCompletion(e, e, null)).asJava
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
   * This is just a workaround to make table api work in multiple threads.
   */
  def createPlannerAgain(): Unit = {
    ClassLoaderUtils.runAsClassLoader(getFlinkClassLoader, () => {
      val stEnvSetting = EnvironmentSettings.newInstance().inStreamingMode().useBlinkPlanner().build()
      this.tableEnvFactory.createPlanner(stEnvSetting)
    })
  }

  def interpret(code: String, context: InterpreterContext): InterpreterResult = {
    val originalStdOut = System.out
    val originalStdErr = System.err
    if (context != null) {
      require(context.out != null)
      interpreterOutput.setInterpreterOutput(context.out)
      context.out.clear()
    }

    Console.withOut(if (context != null) context.out else Console.out) {
      System.setOut(Console.out)
      System.setErr(Console.out)
      interpreterOutput.ignoreLeadingNewLinesFromScalaReporter()
      // add print("") at the end in case the last line is comment which lead to INCOMPLETE
      val lines = code.split("\\n") ++ List("print(\"\")")
      var incompleteCode = ""
      var lastStatus: InterpreterResult.Code = null

      for ((line, i) <- lines.zipWithIndex if !line.trim.isEmpty) {
        val nextLine = if (incompleteCode != "") {
          incompleteCode + "\n" + line
        } else {
          line
        }
        if (i < (lines.length - 1) && lines(i + 1).trim.startsWith(".")) {
          incompleteCode = nextLine
        } else {
          flinkILoop.interpret(nextLine) match {
            case scala.tools.nsc.interpreter.IR.Success =>
              // continue the next line
              incompleteCode = ""
              lastStatus = InterpreterResult.Code.SUCCESS
            case error@scala.tools.nsc.interpreter.IR.Error =>
              return new InterpreterResult(InterpreterResult.Code.ERROR)
            case scala.tools.nsc.interpreter.IR.Incomplete =>
              // put this line into inCompleteCode for the next execution.
              incompleteCode = incompleteCode + "\n" + line
              lastStatus = InterpreterResult.Code.INCOMPLETE
          }
        }
      }
      // flush all output before returning result to frontend
      Console.flush()
      interpreterOutput.setInterpreterOutput(null)
      // reset the java stdout
      System.setOut(originalStdOut)
      System.setErr(originalStdErr)
      return new InterpreterResult(lastStatus)
    }
  }

  /**
   * Set execution.savepoint.path in the following order:
   *
   * 1. Use savepoint path stored in paragraph config, this is recorded by zeppelin when paragraph is canceled,
   * 2. Use checkpoint path stored in pararaph config, this is recorded by zeppelin in flink job progress poller.
   * 3. Use local property 'execution.savepoint.path' if user set it.
   * 4. Otherwise remove 'execution.savepoint.path' when user didn't specify it in %flink.conf
   *
   * @param context
   */
  def setSavepointPathIfNecessary(context: InterpreterContext): Unit = {
    val savepointPath = context.getConfig.getOrDefault(JobManager.SAVEPOINT_PATH, "").toString
    val resumeFromSavepoint = context.getBooleanLocalProperty(JobManager.RESUME_FROM_SAVEPOINT, false)
    if (!StringUtils.isBlank(savepointPath) && resumeFromSavepoint) {
      logInfo(s"Resume job from savepoint , savepointPath = ${savepointPath}")
      configuration.setString(SavepointConfigOptions.SAVEPOINT_PATH.key(), savepointPath)
      return
    }

    val checkpointPath = context.getConfig.getOrDefault(JobManager.LATEST_CHECKPOINT_PATH, "").toString
    val resumeFromLatestCheckpoint = context.getBooleanLocalProperty(JobManager.RESUME_FROM_CHECKPOINT, false)
    if (!StringUtils.isBlank(checkpointPath) && resumeFromLatestCheckpoint) {
      logInfo(s"Resume job from checkpoint , checkpointPath = ${checkpointPath}")
      configuration.setString(SavepointConfigOptions.SAVEPOINT_PATH.key(), checkpointPath)
      return
    }

    val userSavepointPath = context.getLocalProperties.getOrDefault(
      SavepointConfigOptions.SAVEPOINT_PATH.key(), "")
    if (!StringUtils.isBlank(userSavepointPath)) {
      logInfo(s"Resume job from user set savepoint , savepointPath = $userSavepointPath")
      configuration.setString(SavepointConfigOptions.SAVEPOINT_PATH.key(), checkpointPath)
      return
    }

    val userSettingSavepointPath = properties.getProperty(SavepointConfigOptions.SAVEPOINT_PATH.key())
    if (StringUtils.isBlank(userSettingSavepointPath)) {
      // remove SAVEPOINT_PATH when user didn't set it via %flink.conf
      configuration.removeConfig(SavepointConfigOptions.SAVEPOINT_PATH)
    }
  }

  def setParallelismIfNecessary(context: InterpreterContext): Unit = {
    val parallelismStr = context.getLocalProperties.get("parallelism")
    if (!StringUtils.isBlank(parallelismStr)) {
      val parallelism = parallelismStr.toInt
      this.senv.setParallelism(parallelism)
      this.benv.setParallelism(parallelism)
      this.stenv.getConfig.getConfiguration
        .setString(ExecutionConfigOptions.TABLE_EXEC_RESOURCE_DEFAULT_PARALLELISM.key(), parallelism + "")
      this.btenv.getConfig.getConfiguration
        .setString(ExecutionConfigOptions.TABLE_EXEC_RESOURCE_DEFAULT_PARALLELISM.key(), parallelism + "")
    }
    val maxParallelismStr = context.getLocalProperties.get("maxParallelism")
    if (!StringUtils.isBlank(maxParallelismStr)) {
      val maxParallelism = maxParallelismStr.toInt
      senv.setParallelism(maxParallelism)
    }
  }

  def cancel(context: InterpreterContext): Unit = {
    jobManager.cancelJob(context)
  }

  def getProgress(context: InterpreterContext): Int = {
    jobManager.getJobProgress(context.getParagraphId)
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
              HadoopUtils.cleanupStagingDirInternal(clusterClient)
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
    if (jobManager != null) {
      jobManager.shutdown()
    }
  }

  def getExecutionEnvironment(): ExecutionEnvironment = this.benv

  def getStreamExecutionEnvironment(): StreamExecutionEnvironment = this.senv

  def getBatchTableEnvironment(planner: String = "blink"): TableEnvironment = planner match {
    case "blink" => this.btenv
    case _ => this.btenv_2
  }

  def getStreamTableEnvironment(planner: String = "blink"): TableEnvironment = planner match {
    case "blink" => this.stenv
    case _ => this.stenv_2
  }

  def getJavaBatchTableEnvironment(planner: String): TableEnvironment = planner match {
    case "blink" => this.java_btenv
    case _ => this.java_btenv_2
  }

  def getJavaStreamTableEnvironment(planner: String): TableEnvironment = planner match {
    case "blink" => this.java_stenv
    case _ => this.java_stenv_2
  }

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
        DependencyUtils.resolveMavenDependencies(null, packages, null, null, None).split(":").toSeq
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

  def getJobManager = this.jobManager

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

  def getReplContext = this.flinkReplContext

  def getConfiguration = this.configuration

  def getCluster: Option[ClusterClient[_]] = cluster

  def getFlinkILoop = flinkILoop

  def getFlinkShims = flinkShims

  class FlinkJobListener extends JobListener {
    override def onJobSubmitted(jobClient: JobClient, e: Throwable): Unit = {
      if (e != null) {
        logWarn("Fail to submit job")
      } else {
        InterpreterContext.get() match {
          case null =>
            logWarn(s"Job ${jobClient.getJobID} is submitted but unable to associate this job to paragraph,as InterpreterContext is null")
          case _ =>
            logInfo(s"Job ${jobClient.getJobID} is submitted for paragraph ${InterpreterContext.get.getParagraphId}")
            jobManager.addJob(InterpreterContext.get(), jobClient)
            if (jmWebUrl != null) {
              jobManager.sendFlinkJobUrl(InterpreterContext.get())
            } else {
              logError("Unable to link JobURL, because JobManager weburl is null")
            }
        }
      }
    }

    override def onJobExecuted(jobExecutionResult: JobExecutionResult, e: Throwable): Unit = {
      if (e != null) {
        logWarn("Fail to execute job")
      } else {
        logInfo(s"Job ${jobExecutionResult.getJobID} is executed with time ${jobExecutionResult.getNetRuntime(TimeUnit.SECONDS)} seconds")
      }
      if (InterpreterContext.get() != null) {
        jobManager.removeJob(InterpreterContext.get().getParagraphId)
      } else {
        if (e == null) {
          logWarn(s"Unable to remove this job ${jobExecutionResult.getJobID}, as InterpreterContext is null")
        }
      }
    }
  }

  def replaceYarnAddress(webURL: String, yarnAddress: String): String = {
    val pattern = "(https?://.*:\\d+)(.*)".r
    val pattern(_, remaining) = webURL
    yarnAddress + remaining
  }
}


