package com.streamxhub.repl.flink.interpreter

import java.io.{BufferedReader, File}

import com.streamxhub.repl.flink.shims.FlinkShims
import org.apache.flink.annotation.Internal
import org.apache.flink.client.cli.{CliFrontend, CliFrontendParser, CustomCommandLine}
import org.apache.flink.client.deployment.DefaultClusterClientServiceLoader
import org.apache.flink.client.deployment.executors.RemoteExecutor
import org.apache.flink.client.program.{ClusterClient, MiniClusterClient}
import org.apache.flink.configuration._
import org.apache.flink.runtime.minicluster.{MiniCluster, MiniClusterConfiguration}
import org.apache.flink.yarn.configuration.{YarnConfigOptions, YarnDeploymentTarget}
import org.slf4j.LoggerFactory

import scala.collection.mutable.ArrayBuffer

/**
 * Copy from flink, because we need to customize it to make sure
 * it work with multiple versions of flink.
 */
object FlinkShell {

  private lazy val LOGGER = LoggerFactory.getLogger(getClass)

  object ExecutionMode extends Enumeration {
    val UNDEFINED, LOCAL, REMOTE, YARN = Value
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

  @Internal def getClusterClient(config: Config,
                                 flinkConfig: Configuration,
                                 flinkShims: FlinkShims): (Configuration, Option[ClusterClient[_]]) = {

    val (effectiveConfig, clusterClient) = config.executionMode match {
      case ExecutionMode.LOCAL => createLocalCluster(flinkConfig)
      case ExecutionMode.REMOTE => createRemoteConfig(config, flinkConfig)
      case ExecutionMode.YARN => createYarnSessionCluster(config, flinkConfig, flinkShims)
      case _ => throw new IllegalArgumentException("please specify execution mode:[local | remote <host> <port> | yarn]")
    }
    LOGGER.info(s"[StreamX] Notebook connectionInfo:ExecutionMode:${config.executionMode},config:$effectiveConfig")
    (effectiveConfig, clusterClient)
  }

  def getCliFrontend(effectiveConfig: Configuration, config: Config) = {
    val configDir = config.configDir.getOrElse(CliFrontend.getConfigurationDirectoryFromEnv)
    new CliFrontend(effectiveConfig, CliFrontend.loadCustomCommandLines(effectiveConfig, configDir))
  }

  private[this] def createYarnSessionCluster(config: Config, flinkConfig: Configuration, flinkShims: FlinkShims) = {
    flinkConfig.setBoolean(DeploymentOptions.ATTACHED, true)
    val (clusterConfig, clusterClient) = config.yarnConfig match {
      case Some(_) => {
        val executorConfig = {
          val effectiveConfig = new Configuration(flinkConfig)
          val args = parseArgList(config, YarnDeploymentTarget.SESSION)
          val frontend = getCliFrontend(effectiveConfig, config)
          val commandOptions = CliFrontendParser.getRunCommandOptions
          val commandLineOptions = CliFrontendParser.mergeOptions(commandOptions, frontend.getCustomCommandLineOptions)
          val commandLine = CliFrontendParser.parse(commandLineOptions, args, true)

          val customCLI = flinkShims.getCustomCli(frontend, commandLine).asInstanceOf[CustomCommandLine]
          val executorConfig = customCLI.applyCommandLineOptionsToConfiguration(commandLine)
          executorConfig.set(DeploymentOptions.TARGET, YarnDeploymentTarget.SESSION.getName)

          /**
           * 如部署的环境未设置环境变量"FLINK_LIB_DIR",则一定要指定本地的Flink dist jar位置,不然yarn启动报错(错误: 找不到或无法加载主类 org.apache.flink.yarn.entrypoint.YarnSessionClusterEntrypoint)
           */
          val flinkLocalHome = System.getenv("FLINK_HOME")
          val flinkDistJar = new File(s"$flinkLocalHome/lib").list().filter(_.matches("flink-dist_.*\\.jar")) match {
            case Array() => throw new IllegalArgumentException(s"[StreamX] can no found flink-dist jar in $flinkLocalHome/lib")
            case array if array.length == 1 => s"$flinkLocalHome/lib/${array.head}"
            case more => throw new IllegalArgumentException(s"[StreamX] found multiple flink-dist jar in $flinkLocalHome/lib,[${more.mkString(",")}]")
          }
          executorConfig.set(YarnConfigOptions.FLINK_DIST_JAR, flinkDistJar)
        }

        val serviceLoader = new DefaultClusterClientServiceLoader
        val clientFactory = serviceLoader.getClusterClientFactory(executorConfig)
        val clusterDescriptor = clientFactory.createClusterDescriptor(executorConfig)
        val clusterSpecification = clientFactory.getClusterSpecification(executorConfig)
        LOGGER.info(s"\n[StreamX] Notebook connectionInfo:ExecutionMode:${YarnDeploymentTarget.SESSION},clusterSpecification:$clusterSpecification\n")
        val clusterClient = try clusterDescriptor.deploySessionCluster(clusterSpecification).getClusterClient
        finally clusterDescriptor.close()

        (executorConfig, Some(clusterClient))
      }
      case None => (flinkConfig, None)
    }
    val effectiveConfig = clusterClient match {
      case Some(_) => getEffectiveConfiguration(config, clusterConfig, YarnDeploymentTarget.SESSION, flinkShims)
      case None => getEffectiveConfiguration(config, clusterConfig, null, flinkShims)
    }
    (effectiveConfig, clusterClient)
  }

  def parseArgList(config: Config, target: YarnDeploymentTarget): Array[String] = {
    val args = target match {
      case YarnDeploymentTarget.SESSION | YarnDeploymentTarget.PER_JOB => ArrayBuffer[String]("-m", "yarn-cluster")
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

  private[this] def createLocalCluster(flinkConfig: Configuration): (Configuration, Option[MiniClusterClient]) = {
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
    val frontend = getCliFrontend(effectiveConfig, config)
    val commandOptions = CliFrontendParser.getRunCommandOptions
    val commandLineOptions = CliFrontendParser.mergeOptions(commandOptions, frontend.getCustomCommandLineOptions)
    val commandLine = CliFrontendParser.parse(commandLineOptions, args, true)
    val customCLI = flinkShims.getCustomCli(frontend, commandLine).asInstanceOf[CustomCommandLine]
    val executorConfig = customCLI.applyCommandLineOptionsToConfiguration(commandLine);
    executorConfig
  }
}
