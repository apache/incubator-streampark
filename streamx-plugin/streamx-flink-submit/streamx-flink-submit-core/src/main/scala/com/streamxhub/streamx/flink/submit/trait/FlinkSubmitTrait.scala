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

import com.streamxhub.streamx.common.conf.ConfigConst._
import com.streamxhub.streamx.common.util.{Logger, SystemPropertyUtils, Utils}
import com.streamxhub.streamx.flink.core.conf.FlinkRunOption
import com.streamxhub.streamx.flink.submit.domain._
import org.apache.commons.cli.{CommandLine, Options}
import org.apache.flink.api.common.JobID
import org.apache.flink.client.cli.CliFrontend.loadCustomCommandLines
import org.apache.flink.client.cli.{CliArgsException, CliFrontend, CliFrontendParser, CustomCommandLine}
import org.apache.flink.configuration.{ConfigOption, Configuration, CoreOptions, GlobalConfiguration}
import org.apache.flink.util.Preconditions.checkNotNull

import java.io.File
import java.util.{List => JavaList}
import scala.collection.JavaConversions._
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
         |    applicationType  : ${submitRequest.applicationType}
         |    flameGraph       : ${submitRequest.flameGraph != null}
         |    savePoint        : ${submitRequest.savePoint}
         |    userJar          : ${submitRequest.flinkUserJar}
         |    option           : ${submitRequest.option}
         |    property         : ${submitRequest.property}
         |    dynamicOption    : ${submitRequest.dynamicOption.mkString(" ")}
         |    args             : ${submitRequest.args}
         |    appConf          : ${submitRequest.appConf}
         |    flinkBuildResult : ${submitRequest.buildResult}
         |-------------------------------------------------------------------------------------------
         |""".stripMargin)
    doSubmit(submitRequest)
  }

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
    doStop(stopRequest)
  }

  @throws[Exception]
  def doSubmit(submitRequest: SubmitRequest): SubmitResponse

  @throws[Exception]
  def doStop(stopRequest: StopRequest): StopResponse

  private[submit] def getJobID(jobId: String) = Try(JobID.fromHexString(jobId)) match {
    case Success(id) => id
    case Failure(e) => throw new CliArgsException(e.getMessage)
  }

  //----------Public Method end ------------------
  private[submit] def getEffectiveCommandLine(submitRequest: SubmitRequest,
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

      if (submitRequest.option != null && submitRequest.option.trim.nonEmpty) {
        submitRequest.option.split("\\s").filter(_.trim.nonEmpty).foreach(array +=)
      }

      //页面定义的参数优先级大于app配置文件,属性参数...
      if (submitRequest.property != null && submitRequest.property.nonEmpty) {
        submitRequest.property
          .filter(_._1 != KEY_FLINK_SQL())
          .filter(_._1 != KEY_JOB_ID)
          .foreach(x => array += s"-D${x._1.trim}=${x._2.toString.trim}")
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
    GlobalConfiguration.loadConfiguration(s"$flinkHome/conf")
  }

  private[submit] def getOptionFromDefaultFlinkConfig[T](flinkHome: String, option: ConfigOption[T]): T = {
    getFlinkDefaultConfiguration(flinkHome).get(option)
  }

  private[submit] def getCustomCommandLines(flinkHome: String) = {
    val flinkDefaultConfiguration: Configuration = getFlinkDefaultConfiguration(flinkHome)
    // 1. find the configuration directory
    val configurationDirectory = s"$flinkHome/conf"
    // 2. load the custom command lines
    val customCommandLines = loadCustomCommandLines(flinkDefaultConfiguration, configurationDirectory)
    new CliFrontend(flinkDefaultConfiguration, customCommandLines)
    customCommandLines
  }

}
