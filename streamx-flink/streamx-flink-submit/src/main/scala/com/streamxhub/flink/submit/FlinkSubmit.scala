/**
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

import java.io.File
import java.net.{MalformedURLException, URL}
import java.util.Base64.getEncoder
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
import com.streamxhub.common.util.DeflaterUtils

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.Try


object FlinkSubmit extends Logger {

  private[this] val optionPrefix = "flink.deployment.option."

  def submit(submitInfo: SubmitInfo): ApplicationId = {
    logInfo(
      s"""
         |"[StreamX] flink submit," +
         |       "deployMode: ${submitInfo.deployMode},"
         |      "nameService: ${submitInfo.nameService},"
         |      "yarnName: ${submitInfo.yarnName},"
         |      "appConf: ${submitInfo.appConf},"
         |      "userJar: ${submitInfo.flinkUserJar},"
         |      "overrideOption: ${submitInfo.overrideOption.mkString(" ")},"
         |      "args: ${submitInfo.args}"
         |""".stripMargin)

    val map = if (submitInfo.appConf.startsWith("hdfs:")) PropertiesUtils.fromYamlText(HdfsUtils.read(submitInfo.appConf)) else PropertiesUtils.fromYamlFile(submitInfo.appConf)
    val appName = if (submitInfo.yarnName == null) map(KEY_FLINK_APP_NAME) else submitInfo.yarnName
    val appMain = map(KEY_FLINK_APP_MAIN)

    /**
     * 获取当前机器上的flink
     */
    val flinkLocalHome = System.getenv("FLINK_HOME")
    require(flinkLocalHome != null)

    logInfo(s"[StreamX] flinkHome: $flinkLocalHome")

    val flinkName = new File(flinkLocalHome).getName

    val flinkHdfsHome = s"$APP_FLINK/$flinkName"

    val flinkHdfsHomeWithNameService = s"hdfs://${submitInfo.nameService}$flinkHdfsHome"

    logInfo(s"[StreamX] flinkHdfsDir: $flinkHdfsHome")

    if (!HdfsUtils.exists(flinkHdfsHome)) {
      logInfo(s"[StreamX] $flinkHdfsHome is not exists,upload beginning....")
      HdfsUtils.upload(flinkLocalHome, flinkHdfsHome)
    }

    //存放flink集群相关的jar包目录
    val flinkHdfsLibs = new Path(s"$flinkHdfsHomeWithNameService/lib")

    val flinkHdfsPlugins = new Path(s"$flinkHdfsHomeWithNameService/plugins")

    val flinkHdfsDistJar = new File(s"$flinkLocalHome/lib").list().filter(_.matches("flink-dist_.*\\.jar")) match {
      case Array() => throw new IllegalArgumentException(s"[StreamX] can no found flink-dist jar in $flinkLocalHome/lib")
      case array if array.length == 1 => s"$flinkHdfsHomeWithNameService/lib/${array.head}"
      case more => throw new IllegalArgumentException(s"[StreamX] found multiple flink-dist jar in $flinkLocalHome/lib,[${more.mkString(",")}]")
    }

    val flinkLocalConfDir = flinkLocalHome.concat("/conf")

    /**
     * 相关参数在这里从hdfs中读取出来,然后传入到FlinkStreaming中来启动,主要是避免@see{com.streamxhub.flink.core.FlinkInitializer}程序中解析参数而引入额外的hdfs相关的jar
     * 可能会和用户导入的jar冲突,这里将对外部的依赖降到最低,避免带来不必要的麻烦,给用户使用框架的时候造成困扰...
     */
    val encodeConf = DeflaterUtils.zipString(HdfsUtils.read(submitInfo.appConf))

    val userAppConf = submitInfo.appConf.split("\\.").last.toLowerCase match {
      case "yaml" | "yml" => s"yaml://$encodeConf"
      case "properties" => s"prop://$encodeConf"
      case _ => null
    }

    val flinkConf = {
      val yaml = s"$flinkHdfsHomeWithNameService/conf/flink-conf.yaml"
      DeflaterUtils.zipString(HdfsUtils.read(yaml))
    }

    val appArgs = {
      val array = new ArrayBuffer[String]
      Try(submitInfo.args.split("\\s+")).getOrElse(Array()).foreach(x => array += x)
      array += KEY_FLINK_HOME("--")
      array += flinkHdfsHomeWithNameService
      array += KEY_APP_CONF("--")
      array += userAppConf
      array += KEY_FLINK_CONF("--")
      array += flinkConf
      array.toList.asJava
    }

    //获取flink的配置
    val flinkConfiguration = GlobalConfiguration
      //从flink-conf.yaml中加载默认配置文件...
      .loadConfiguration(flinkLocalConfDir)

      .set(CoreOptions.CLASSLOADER_RESOLVE_ORDER, "parent-first")
      //设置yarn.provided.lib.dirs
      .set(YarnConfigOptions.PROVIDED_LIB_DIRS, Arrays.asList(flinkHdfsLibs.toString, flinkHdfsPlugins.toString))
      //设置flinkDistJar
      .set(YarnConfigOptions.FLINK_DIST_JAR, flinkHdfsDistJar)
      //设置用户的jar
      .set(PipelineOptions.JARS, Collections.singletonList(submitInfo.flinkUserJar))
      //设置为部署模式
      .set(DeploymentOptions.TARGET, YarnDeploymentTarget.APPLICATION.getName)
      //yarn application name
      .set(YarnConfigOptions.APPLICATION_NAME, appName)
      //yarn application Type
      .set(YarnConfigOptions.APPLICATION_TYPE, "StreamX Flink")
      //设置启动主类
      .set(ApplicationConfiguration.APPLICATION_MAIN_CLASS, appMain)
      //设置启动参数
      .set(ApplicationConfiguration.APPLICATION_ARGS, appArgs)

    val customCommandLines = loadCustomCommandLines(flinkConfiguration, flinkLocalConfDir)

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

        //页面定义的参数优先级大于app配置文件
        submitInfo.overrideOption.foreach(x => array += x)

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
    val uri = PackagedProgramUtils.resolveURI(submitInfo.flinkUserJar)
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
    val classpath = new ArrayBuffer[URL]
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
    ConfigUtils.encodeCollectionToConfig(configuration, PipelineOptions.CLASSPATHS, classpath.toList, new function.Function[URL, String] {
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

