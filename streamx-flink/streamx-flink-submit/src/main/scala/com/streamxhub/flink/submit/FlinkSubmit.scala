package com.streamxhub.flink.submit

import java.io.File
import java.net.{MalformedURLException, URL}
import java.util._

import com.streamxhub.common.conf.ConfigConst._
import com.streamxhub.common.conf.FlinkRunOption
import com.streamxhub.common.util.{HdfsUtils, Logger, PropertiesUtils}
import org.apache.commons.cli._
import org.apache.flink.client.cli.CliFrontend.loadCustomCommandLines
import org.apache.flink.client.cli.CliFrontendParser.SHUTDOWN_IF_ATTACHED_OPTION
import org.apache.flink.client.cli._
import org.apache.flink.client.deployment.DefaultClusterClientServiceLoader
import org.apache.flink.client.deployment.application.ApplicationConfiguration
import org.apache.flink.client.program.{ClusterClient, PackagedProgramUtils}
import org.apache.flink.configuration._
import org.apache.flink.runtime.jobgraph.SavepointRestoreSettings
import org.apache.flink.util.FlinkException
import org.apache.flink.util.Preconditions.checkNotNull
import org.apache.flink.yarn.configuration.{YarnConfigOptions, YarnDeploymentTarget}
import org.apache.hadoop.fs.Path
import org.apache.hadoop.yarn.api.records.ApplicationId

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.Try


object FlinkSubmit extends Logger {

  private[this] val optionPrefix = "flink.deployment.option."

  def submit(workspace: String,
             flinkUserJar: String,
             yarnName: String,
             appConf: String,
             overrideOption: Array[String],
             args: String): ApplicationId = {

    logInfo(s"[StreamX] flink submit,workspace $workspace," +
      s"yarnName $yarnName," +
      s"appConf $appConf," +
      s"userJar $flinkUserJar," +
      s"overrideOption ${overrideOption.mkString(" ")}," +
      s"args $args")

    val map = if (appConf.startsWith("hdfs:")) PropertiesUtils.fromYamlText(HdfsUtils.read(appConf)) else PropertiesUtils.fromYamlFile(appConf)
    val appName = if (yarnName == null) map(KEY_FLINK_APP_NAME) else yarnName
    val appMain = map(KEY_FLINK_APP_MAIN)

    /**
     * 获取当前机器上的flink
     */
    val flinkHome = System.getenv("FLINK_HOME")
    require(flinkHome != null)

    logInfo(s"[StreamX] flinkHome: $flinkHome")

    val flinkVersion = new File(flinkHome).getName

    val flinkHdfsDir = s"${workspace.replaceFirst("[^/]*$","flink")}/$flinkVersion"

    logInfo(s"[StreamX] flinkHdfsDir: $flinkHdfsDir")

    if (!HdfsUtils.exists(flinkHdfsDir)) {
      logInfo(s"[StreamX] $flinkHdfsDir is not exists,upload beginning....")
      HdfsUtils.upload(flinkHome, flinkHdfsDir)
    }

    //存放flink集群相关的jar包目录
    val flinkHdfsLibs = new Path(s"$flinkHdfsDir/lib")

    val flinkHdfsPlugins = new Path(s"$flinkHdfsDir/plugins")

    val flinkHdfsDistJar = new File(s"$flinkHome/lib").list().filter(_.matches("flink-dist_.*\\.jar")) match {
      case Array() => throw new IllegalArgumentException(s"[StreamX] can no found flink-dist jar in $flinkHome/lib")
      case array if array.length == 1 => s"$flinkHdfsDir/lib/${array.head}"
      case more => throw new IllegalArgumentException(s"[StreamX] found multiple flink-dist jar in $flinkHome/lib,[${more.mkString(",")}]")
    }

    val flinkConfDir = flinkHome.concat("/conf")

    val appArgs = {
      val array = new ArrayBuffer[String]
      Try(args.split("\\s+")).getOrElse(Array()).foreach(x => array += x)
      array += KEY_FLINK_APP_CONF("--")
      array += appConf
      array += KEY_FLINK_HOME("--")
      array += flinkHdfsDir
      array.toList.asJava
    }

    //获取flink的配置
    val flinkConfiguration = GlobalConfiguration
      //从flink-conf.yaml中加载默认配置文件...
      .loadConfiguration(flinkConfDir)

      .set(CoreOptions.CLASSLOADER_RESOLVE_ORDER, "parent-first")
      //设置yarn.provided.lib.dirs
      .set(YarnConfigOptions.PROVIDED_LIB_DIRS, Arrays.asList(flinkHdfsLibs.toString, flinkHdfsPlugins.toString))
      //设置flinkDistJar
      .set(YarnConfigOptions.FLINK_DIST_JAR, flinkHdfsDistJar)
      //设置用户的jar
      .set(PipelineOptions.JARS, Collections.singletonList(flinkUserJar))
      //设置为application模式
      .set(DeploymentOptions.TARGET, YarnDeploymentTarget.APPLICATION.getName)
      //yarn application name
      .set(YarnConfigOptions.APPLICATION_NAME, appName)
      //yarn application Type
      .set(YarnConfigOptions.APPLICATION_TYPE, "StreamX Flink")
      //设置启动主类
      .set(ApplicationConfiguration.APPLICATION_MAIN_CLASS, appMain)
      //设置启动参数
      .set(ApplicationConfiguration.APPLICATION_ARGS,appArgs)

    val customCommandLines = loadCustomCommandLines(flinkConfiguration, flinkConfDir)

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
      val userArgs = {
        val optionMap = new mutable.HashMap[String, Any]()
        map.filter(_._1.startsWith(optionPrefix)).filter(_._2.nonEmpty).filter(x => {
          val key = x._1.drop(optionPrefix.length)
          //验证参数是否合法...
          val verify = commandLineOptions.hasOption(key)
          if (!verify) {
            println(s"[StreamX] param:$key is error,skip it.")
          }
          verify
        }).foreach(x => {
          val opt = commandLineOptions.getOption(s"--${x._1.drop(optionPrefix.length).trim}").getOpt
          Try(x._2.toBoolean).getOrElse(x._2) match {
            case b if b.isInstanceOf[Boolean] => if (b.asInstanceOf[Boolean]) optionMap += s"-$opt" -> true
            case v => optionMap += s"-$opt" -> v
          }
        })
        val array = new ArrayBuffer[String]()
        optionMap.foreach(x => {
          array += x._1
          if (x._2.isInstanceOf[String]) {
            array += x._2.toString
          }
        })

        //页面定义的参数优先级大于pe配合文件
        overrideOption.foreach(x => array += x)

        array.toArray
      }
      CliFrontendParser.parse(commandLineOptions, userArgs, true)
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
    val uri = PackagedProgramUtils.resolveURI(flinkUserJar)
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
   * just create from flink v1.11.1 source
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
    val classpath = new ArrayList[URL]
    if (commandLine.hasOption(FlinkRunOption.CLASSPATH_OPTION.getOpt)) for (path <- commandLine.getOptionValues(FlinkRunOption.CLASSPATH_OPTION.getOpt)) {
      try classpath.add(new URL(path)) catch {
        case e: MalformedURLException => throw new CliArgsException(s"[StreamX]Bad syntax for classpath:${path},err:$e")
      }
    }
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
    ConfigUtils.encodeCollectionToConfig(configuration, PipelineOptions.CLASSPATHS, classpath, new function.Function[URL, String] {
      override def apply(url: URL): String = url.toString
    })
    SavepointRestoreSettings.toConfiguration(savepointSettings, configuration)
    ConfigUtils.encodeCollectionToConfig(configuration, PipelineOptions.JARS, jobJars, new function.Function[String, String] {
      override def apply(t: String): String = t
    })
    val executorConfig = checkNotNull(activeCustomCommandLine).applyCommandLineOptionsToConfiguration(commandLine)
    val effectiveConfiguration = new Configuration(executorConfig)
    effectiveConfiguration.addAll(configuration)
    println("-----------------------")
    println("Effective executor configuration: {}", effectiveConfiguration)
    println("-----------------------")
    effectiveConfiguration
  }

}

