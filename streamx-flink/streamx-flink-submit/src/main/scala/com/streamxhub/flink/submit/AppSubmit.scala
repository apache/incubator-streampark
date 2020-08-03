package com.streamxhub.flink.submit


import java.net.{MalformedURLException, URL}
import java.util
import java.util.{ArrayList, Collections, List}

import com.streamxhub.common.util.{HdfsUtils, PropertiesUtils}
import org.apache.flink.client.deployment.application.ApplicationConfiguration
import org.apache.flink.configuration._
import org.apache.flink.yarn.configuration.YarnConfigOptions
import org.apache.flink.yarn.configuration.YarnDeploymentTarget
import org.apache.hadoop.fs.Path
import com.streamxhub.common.conf.ConfigConst._
import com.streamxhub.common.conf.FlinkRunOption
import org.apache.commons.cli._
import org.apache.flink.client.cli.CliFrontend.loadCustomCommandLines
import org.apache.flink.client.cli.CliFrontendParser.{SHUTDOWN_IF_ATTACHED_OPTION, YARN_DETACHED_OPTION}
import org.apache.flink.client.cli._
import org.apache.flink.client.deployment.application.cli.ApplicationClusterDeployer
import org.apache.flink.client.deployment.DefaultClusterClientServiceLoader
import org.apache.flink.client.program.PackagedProgramUtils
import org.apache.flink.runtime.jobgraph.SavepointRestoreSettings
import org.apache.flink.util.FlinkException
import org.apache.flink.util.Preconditions.checkNotNull

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.Try
import scala.collection.JavaConversions._


object AppSubmit {

  private[this] val optionPrefix = "flink.deployment.option."

  def main(args: Array[String]): Unit = {
    //配置文件必须在hdfs上
    val app_conf = "hdfs://nameservice1/streamx/workspace/streamx-flink-test-1.0.0/conf/application.yml"
    //val app_conf = "/Users/benjobs/Github/StreamX/streamx-flink/streamx-flink-test/assembly/conf/application.yml"

    val map = if (app_conf.startsWith("hdfs:")) PropertiesUtils.fromYamlText(HdfsUtils.readFile(app_conf)) else PropertiesUtils.fromYamlFile(app_conf)
    val appName = map(KEY_FLINK_APP_NAME)
    val appMain = map(KEY_FLINK_APP_MAIN)

    //存放flink集群相关的jar包目录
    val flinkLibs = new Path("hdfs://nameservice1/streamx/flink/flink-1.9.2/lib")
    val plugins = new Path("hdfs://nameservice1/streamx/flink/flink-1.9.2/plugins")
    //用户jar
    val flinkDistJar = "hdfs://nameservice1/streamx/flink/flink-1.9.2/lib/flink-dist_2.11-1.11.1.jar"

    val flinkUserJar = "hdfs://nameservice1/streamx/workspace/streamx-flink-test-1.0.0/lib/streamx-flink-test-1.0.0.jar"

    val flinkConfDir = System.getenv("FLINK_HOME").concat("/conf")
    //获取flink的配置
    val flinkConfiguration = GlobalConfiguration
      //从flink-conf.yaml中加载默认配置文件...
      .loadConfiguration(flinkConfDir)

      .set(CoreOptions.CLASSLOADER_RESOLVE_ORDER, "parent-first")
      //设置yarn.provided.lib.dirs
      .set(YarnConfigOptions.PROVIDED_LIB_DIRS, util.Arrays.asList(flinkLibs.toString, plugins.toString))
      //设置flinkDistJar
      .set(YarnConfigOptions.FLINK_DIST_JAR, flinkDistJar)
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
      .set(ApplicationConfiguration.APPLICATION_ARGS, util.Arrays.asList(KEY_FLINK_APP_CONF("--"), app_conf))

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
    val appConfiguration = ApplicationConfiguration.fromConfiguration(flinkConfiguration)
    val clusterClientServiceLoader = new DefaultClusterClientServiceLoader
    val deployer = new ApplicationClusterDeployer(clusterClientServiceLoader)
    deployer.run(effectiveConfiguration, appConfiguration)
  }

  /**
   * just create from flink v1.11.1 source
   * @param activeCustomCommandLine
   * @param commandLine
   * @param jobJars
   * @tparam T
   * @throws
   * @return
   */
  @throws[FlinkException] private def getEffectiveConfiguration[T](activeCustomCommandLine: CustomCommandLine, commandLine: CommandLine, jobJars: util.List[String]) = {
    val configuration = new Configuration
    val classpath = new util.ArrayList[URL]
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
    ConfigUtils.encodeCollectionToConfig(configuration, PipelineOptions.CLASSPATHS, classpath, new util.function.Function[URL, String] {
      override def apply(url: URL): String = url.toString
    })
    SavepointRestoreSettings.toConfiguration(savepointSettings, configuration)
    ConfigUtils.encodeCollectionToConfig(configuration, PipelineOptions.JARS, jobJars, new util.function.Function[String, String] {
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

