package com.streamxhub.repl.flink.interpreter

import java.io.{BufferedReader, File}
import java.util.Arrays

import com.streamxhub.repl.flink.shims.FlinkShims
import org.apache.flink.annotation.Internal
import org.apache.flink.client.cli.{CliFrontend, CliFrontendParser, CustomCommandLine}
import org.apache.flink.client.deployment.DefaultClusterClientServiceLoader
import org.apache.flink.client.deployment.executors.RemoteExecutor
import org.apache.flink.client.program.{ClusterClient, MiniClusterClient}
import org.apache.flink.configuration._
import org.apache.flink.runtime.minicluster.{MiniCluster, MiniClusterConfiguration}
import org.apache.flink.yarn.configuration.{YarnConfigOptions, YarnDeploymentTarget}
import com.streamxhub.common.conf.ConfigConst._
import com.streamxhub.common.util.{HdfsUtils, Logger}
import org.apache.flink.client.deployment.application.ApplicationConfiguration
import org.apache.hadoop.yarn.api.records.ApplicationId

import scala.collection.mutable.ArrayBuffer

/**
 * Copy from flink, because we need to customize it to make sure
 * it work with multiple versions of flink.
 */
object FlinkShell extends Logger {

  object ExecutionMode extends Enumeration {
    val UNDEFINED, LOCAL, REMOTE, YARN, APPLICATION = Value
  }

  /** Configuration object */
  case class Config(
                     host: Option[String] = None,
                     port: Option[Int] = None,
                     externalJars: Option[Array[String]] = None,
                     executionMode: ExecutionMode.Value = ExecutionMode.UNDEFINED,
                     yarnConfig: Option[YarnConfig] = None,
                     configDir: Option[String] = None
                   )

  /** YARN configuration object */
  case class YarnConfig(
                         jobManagerMemory: Option[String] = None,
                         name: Option[String] = None,
                         queue: Option[String] = None,
                         slots: Option[Int] = None,
                         taskManagerMemory: Option[String] = None
                       )

  /** Buffered reader to substitute input in test */
  var bufferedReader: Option[BufferedReader] = None

  @Internal def ensureYarnConfig(config: Config): YarnConfig = config.yarnConfig match {
    case Some(yarnConfig) => yarnConfig
    case None => YarnConfig()
  }

  private def getConfigurationDirectory(config: Config): String = config.configDir.getOrElse(CliFrontend.getConfigurationDirectoryFromEnv)

  @Internal def getClusterClient(config: Config,
                              flinkConfig: Configuration,
                              flinkShims: FlinkShims): (Configuration, Option[ClusterClient[_]]) = {

    val (effectiveConfig, clusterClient) = config.executionMode match {
      case ExecutionMode.LOCAL => createLocalClusterAndConfig(flinkConfig)
      case ExecutionMode.REMOTE => createRemoteConfig(config, flinkConfig)
      case ExecutionMode.YARN => createYarnSessionClusterAndConfig(config, flinkConfig, flinkShims)
      case ExecutionMode.APPLICATION => createApplicationAndConfig(config, flinkConfig, flinkShims)
      case _ => throw new IllegalArgumentException("please specify execution mode:[local | remote <host> <port> | yarn]")
    }
    logInfo(s"[StreamX] Notebook connectionInfo:ExecutionMode:${config.executionMode},config:$effectiveConfig")
    (effectiveConfig, clusterClient)
  }

  private[this] def createApplicationAndConfig(config: Config, flinkConfig: Configuration, flinkShims: FlinkShims) = {
    val flinkLocalHome = System.getenv("FLINK_HOME")
    val flinkHdfsLibs = s"$flinkLocalHome/lib"
    val flinkHdfsPlugins = s"$flinkLocalHome/plugins"
    val flinkName = new File(flinkLocalHome).getName
    val flinkHdfsHome = s"$APP_FLINK/$flinkName"
    val flinkHdfsHomeWithNameService = s"${HdfsUtils.getDefaultFS}$flinkHdfsHome"
    val flinkHdfsDistJar = new File(s"$flinkLocalHome/lib").list().filter(_.matches("flink-dist_.*\\.jar")) match {
      case Array() => throw new IllegalArgumentException(s"[StreamX] can no found flink-dist jar in $flinkLocalHome/lib")
      case array if array.length == 1 => s"$flinkHdfsHomeWithNameService/lib/${array.head}"
      case more => throw new IllegalArgumentException(s"[StreamX] found multiple flink-dist jar in $flinkLocalHome/lib,[${more.mkString(",")}]")
    }

    flinkConfig.set(YarnConfigOptions.PROVIDED_LIB_DIRS, Arrays.asList(flinkHdfsLibs, flinkHdfsPlugins))
      .set(YarnConfigOptions.FLINK_DIST_JAR, flinkHdfsDistJar)
      .set(DeploymentOptions.TARGET, YarnDeploymentTarget.APPLICATION.getName)
      .set(YarnConfigOptions.APPLICATION_TYPE, "StreamX NoteBook")

    val (clusterConfig, clusterClient) = config.yarnConfig match {
      case Some(_) => {
        val effectiveConfig = new Configuration(flinkConfig)
        val args = parseArgList(config, YarnDeploymentTarget.APPLICATION)
        val frontend = new CliFrontend(effectiveConfig, CliFrontend.loadCustomCommandLines(effectiveConfig, getConfigurationDirectory(config)))
        val commandOptions = CliFrontendParser.getRunCommandOptions
        val commandLineOptions = CliFrontendParser.mergeOptions(commandOptions, frontend.getCustomCommandLineOptions)
        val commandLine = CliFrontendParser.parse(commandLineOptions, args, true)
        val customCLI = flinkShims.getCustomCli(frontend, commandLine).asInstanceOf[CustomCommandLine]
        val effectiveConfiguration = customCLI.applyCommandLineOptionsToConfiguration(commandLine)

        val clusterClient = try {
          val clusterClientServiceLoader = new DefaultClusterClientServiceLoader
          val clientFactory = clusterClientServiceLoader.getClusterClientFactory[ApplicationId](effectiveConfiguration)
          val applicationConfiguration = ApplicationConfiguration.fromConfiguration(effectiveConfiguration)
          val clusterDescriptor = clientFactory.createClusterDescriptor(effectiveConfiguration)
          try {
            val clusterSpecification = clientFactory.getClusterSpecification(effectiveConfiguration)
            clusterDescriptor.deployApplicationCluster(clusterSpecification, applicationConfiguration).getClusterClient
          } finally if (clusterDescriptor != null) {
            clusterDescriptor.close()
          }
        }
        (effectiveConfig, Some(clusterClient))
      }
      case None => (flinkConfig, None)
    }
    val effectiveConfiguration = clusterClient match {
      case Some(_) => getEffectiveConfiguration(config, clusterConfig, YarnDeploymentTarget.APPLICATION, flinkShims)
      case None => getEffectiveConfiguration(config, clusterConfig, null, flinkShims)
    }
    (effectiveConfiguration, clusterClient)
  }


  private[this] def createYarnSessionClusterAndConfig(config: Config, flinkConfig: Configuration, flinkShims: FlinkShims) = {
    flinkConfig.setBoolean(DeploymentOptions.ATTACHED, true)
    val (clusterConfig, clusterClient) = config.yarnConfig match {
      case Some(_) => {
        val effectiveConfig = new Configuration(flinkConfig)
        val args = parseArgList(config, YarnDeploymentTarget.SESSION)
        val frontend = new CliFrontend(effectiveConfig, CliFrontend.loadCustomCommandLines(effectiveConfig, getConfigurationDirectory(config)))

        val commandOptions = CliFrontendParser.getRunCommandOptions
        val commandLineOptions = CliFrontendParser.mergeOptions(commandOptions, frontend.getCustomCommandLineOptions)
        val commandLine = CliFrontendParser.parse(commandLineOptions, args, true)

        val customCLI = flinkShims.getCustomCli(frontend, commandLine).asInstanceOf[CustomCommandLine]
        val executorConfig = customCLI.applyCommandLineOptionsToConfiguration(commandLine)

        val serviceLoader = new DefaultClusterClientServiceLoader
        val clientFactory = serviceLoader.getClusterClientFactory(executorConfig)
        val clusterDescriptor = clientFactory.createClusterDescriptor(executorConfig)
        val clusterSpecification = clientFactory.getClusterSpecification(executorConfig)

        println(s"[StreamX] Notebook connectionInfo:ExecutionMode:${YarnDeploymentTarget.SESSION},clusterSpecification:$clusterSpecification")

        val clusterClient = try {
          clusterDescriptor.deploySessionCluster(clusterSpecification).getClusterClient
        } finally {
          executorConfig.set(DeploymentOptions.TARGET, YarnDeploymentTarget.SESSION.getName)
          clusterDescriptor.close()
        }
        (executorConfig, Some(clusterClient))
      }
      case None => (flinkConfig, None)
    }
    flinkConfig.set(DeploymentOptions.TARGET, YarnDeploymentTarget.SESSION.getName)
    val effectiveConfig = clusterClient match {
      case Some(_) => getEffectiveConfiguration(config, clusterConfig, YarnDeploymentTarget.SESSION, flinkShims)
      case None => getEffectiveConfiguration(config, clusterConfig, null, flinkShims)
    }
    (effectiveConfig, clusterClient)
  }

  def parseArgList(config: Config, target: YarnDeploymentTarget): Array[String] = {
    val args = target match {
      case YarnDeploymentTarget.SESSION =>
        ArrayBuffer[String]("-m", "yarn-cluster")
      case _ => ArrayBuffer[String]()
    }
    config.yarnConfig match {
      case Some(yarnConfig) =>
        yarnConfig.jobManagerMemory.foreach(jmMem => args ++= Seq("-yjm", jmMem))
        yarnConfig.taskManagerMemory.foreach(tmMem => args ++= Seq("-ytm", tmMem))
        yarnConfig.name.foreach(name => args ++= Seq("-ynm", name))
        yarnConfig.queue.foreach(queue => args ++= Seq("-yqu", queue))
        yarnConfig.slots.foreach(slots => args ++= Seq("-ys", slots.toString))
        args.toArray
      case None => args.toArray
    }
  }

  private[this] def createRemoteConfig(config: Config, flinkConfig: Configuration): (Configuration, None.type) = {
    if (config.host.isEmpty || config.port.isEmpty) {
      throw new IllegalArgumentException("<host> or <port> is not specified!")
    }
    val effectiveConfig = new Configuration(flinkConfig)
    setJobManagerInfoToConfig(effectiveConfig, config.host.get, config.port.get)
    effectiveConfig.set(DeploymentOptions.TARGET, RemoteExecutor.NAME)
    effectiveConfig.setBoolean(DeploymentOptions.ATTACHED, true)
    (effectiveConfig, None)
  }

  private[this] def createLocalClusterAndConfig(flinkConfig: Configuration) = {
    val config = new Configuration(flinkConfig)
    config.setInteger(JobManagerOptions.PORT, 0)

    val cluster = {
      val numTaskManagers = flinkConfig.getInteger(ConfigConstants.LOCAL_NUMBER_TASK_MANAGER, ConfigConstants.DEFAULT_LOCAL_NUMBER_TASK_MANAGER)
      val numSlotsPerTaskManager = flinkConfig.getInteger(TaskManagerOptions.NUM_TASK_SLOTS)

      val miniClusterConfig = new MiniClusterConfiguration.Builder()
        .setConfiguration(flinkConfig)
        .setNumSlotsPerTaskManager(numSlotsPerTaskManager)
        .setNumTaskManagers(numTaskManagers)
        .build()

      val cluster = new MiniCluster(miniClusterConfig)
      cluster.start()
      cluster
    }
    val port = cluster.getRestAddress.get.getPort
    setJobManagerInfoToConfig(config, "localhost", port)
    config.set(DeploymentOptions.TARGET, RemoteExecutor.NAME)
    config.setBoolean(DeploymentOptions.ATTACHED, true)
    println(s"\nStarting local Flink cluster (host: localhost, port: ${port}).\n")
    val clusterClient = new MiniClusterClient(config, cluster)
    (config, Some(clusterClient))
  }

  private def setJobManagerInfoToConfig(config: Configuration, host: String, port: Integer): Unit = {
    config.setString(JobManagerOptions.ADDRESS, host)
    config.setInteger(JobManagerOptions.PORT, port)
    config.setString(RestOptions.ADDRESS, host)
    config.setInteger(RestOptions.PORT, port)
  }


  private[this] def getEffectiveConfiguration(config: Config,
                                              flinkConfig: Configuration,
                                              target: YarnDeploymentTarget,
                                              flinkShims: FlinkShims) = {
    val effectiveConfig = new Configuration(flinkConfig)
    val args = parseArgList(config, target)
    val frontend = new CliFrontend(effectiveConfig, CliFrontend.loadCustomCommandLines(effectiveConfig, getConfigurationDirectory(config)))
    val commandOptions = CliFrontendParser.getRunCommandOptions
    val commandLineOptions = CliFrontendParser.mergeOptions(commandOptions, frontend.getCustomCommandLineOptions)
    val commandLine = CliFrontendParser.parse(commandLineOptions, args, true)
    val customCLI = flinkShims.getCustomCli(frontend, commandLine).asInstanceOf[CustomCommandLine]
    val executorConfig = customCLI.applyCommandLineOptionsToConfiguration(commandLine);
    executorConfig
  }
}
