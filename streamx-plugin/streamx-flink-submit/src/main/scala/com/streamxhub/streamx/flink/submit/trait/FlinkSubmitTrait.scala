/*
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
package com.streamxhub.streamx.flink.submit.`trait`

import com.streamxhub.streamx.common.conf.ConfigConst._
import com.streamxhub.streamx.common.util.{HdfsUtils, Logger, Utils}
import com.streamxhub.streamx.flink.common.conf.FlinkRunOption
import com.streamxhub.streamx.flink.submit.{SubmitRequest, SubmitResponse}
import org.apache.commons.cli.{CommandLine, Options}
import org.apache.commons.io.{FileUtils, IOUtils}
import org.apache.flink.api.common.JobID
import org.apache.flink.client.cli.CliFrontend.loadCustomCommandLines
import org.apache.flink.client.cli.{CliArgsException, CliFrontend, CliFrontendParser, CustomCommandLine}
import org.apache.flink.configuration.{ConfigOption, Configuration, CoreOptions, GlobalConfiguration}
import org.apache.flink.util.Preconditions.checkNotNull
import org.apache.hadoop.fs.Path

import java.io.File
import java.lang.{Boolean => JavaBool}
import java.nio.charset.Charset
import java.util.{List => JavaList}
import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.{Failure, Success, Try}

trait FlinkSubmitTrait extends Logger {

  private[submit] var USER_FLINK_HOME: String = _

  private[submit] lazy val FLINK_HOME = {
    if (Utils.isEmpty(USER_FLINK_HOME)) {
      val flinkLocalHome = System.getenv("FLINK_HOME")
      require(flinkLocalHome != null)
      logInfo(s"flinkHome: $flinkLocalHome")
      flinkLocalHome
    } else {
      USER_FLINK_HOME
    }
  }

  lazy val workspaceEnv: WorkspaceEnv = {
    /**
     * 必须保持本机flink和hdfs里的flink版本和配置都完全一致.
     */
    val flinkName = new File(FLINK_HOME).getName
    val flinkHdfsHome = s"${HdfsUtils.getDefaultFS}$APP_FLINK/$flinkName"

    WorkspaceEnv(
      flinkName,
      flinkHdfsHome,
      flinkHdfsLibs = new Path(s"$flinkHdfsHome/lib"),
      flinkHdfsPlugins = new Path(s"$flinkHdfsHome/plugins"),
      flinkHdfsJars = new Path( s"${HdfsUtils.getDefaultFS}$APP_JARS"),
      streamxPlugin = new Path(s"${HdfsUtils.getDefaultFS}$APP_PLUGINS"),
      flinkHdfsDistJar = new File(s"$FLINK_HOME/lib").list().filter(_.matches("flink-dist_.*\\.jar")) match {
        case Array() => throw new IllegalArgumentException(s"[StreamX] can no found flink-dist jar in $FLINK_HOME/lib")
        case array if array.length == 1 => s"$flinkHdfsHome/lib/${array.head}"
        case more => throw new IllegalArgumentException(s"[StreamX] found multiple flink-dist jar in $FLINK_HOME/lib,[${more.mkString(",")}]")
      }
    )
  }

  private[submit] lazy val flinkDefaultConfiguration: Configuration = {
    require(FLINK_HOME != null)
    //获取flink的配置
    GlobalConfiguration.loadConfiguration(s"$FLINK_HOME/conf")
  }

  private[submit] lazy val customCommandLines = {
    // 1. find the configuration directory
    val configurationDirectory = s"$FLINK_HOME/conf"
    // 2. load the custom command lines
    val customCommandLines = loadCustomCommandLines(flinkDefaultConfiguration, configurationDirectory)
    new CliFrontend(flinkDefaultConfiguration, customCommandLines)
    customCommandLines
  }

  private[submit] lazy val jvmProfilerJar: String = {
    val pluginsPath = System.getProperty("app.home").concat("/plugins")
    val pluginsDir = new File(pluginsPath)
    pluginsDir.list().filter(_.matches("streamx-jvm-profiler-.*\\.jar")) match {
      case Array() => throw new IllegalArgumentException(s"[StreamX] can no found streamx-jvm-profiler jar in $pluginsPath")
      case array if array.length == 1 => array.head
      case more => throw new IllegalArgumentException(s"[StreamX] found multiple streamx-jvm-profiler jar in $pluginsPath,[${more.mkString(",")}]")
    }
  }

  private[submit] lazy val PARAM_KEY_FLINK_CONF = KEY_FLINK_CONF("--")
  private[submit] lazy val PARAM_KEY_FLINK_SQL = KEY_FLINK_SQL("--")
  private[submit] lazy val PARAM_KEY_APP_CONF = KEY_APP_CONF("--")
  private[submit] lazy val PARAM_KEY_APP_NAME = KEY_APP_NAME("--")
  private[submit] lazy val PARAM_KEY_FLINK_PARALLELISM = KEY_FLINK_PARALLELISM("--")

  @throws[Exception] def submit(submitRequest: SubmitRequest): SubmitResponse = {
    logInfo(
      s"""
         |"flink submit {" +
         |      "userFlinkHome" : ${submitRequest.flinkHome},
         |      "appName": ${submitRequest.appName},
         |      "devMode": ${submitRequest.developmentMode.name()},
         |      "execMode": ${submitRequest.executionMode.name()},
         |      "resolveOrder": ${submitRequest.resolveOrder.getName},
         |      "appConf": ${submitRequest.appConf},
         |      "applicationType": ${submitRequest.applicationType},
         |      "savePoint": ${submitRequest.savePoint},
         |      "flameGraph": ${submitRequest.flameGraph != null},
         |      "userJar": ${submitRequest.flinkUserJar},
         |      "option": ${submitRequest.option},
         |      "property": ${submitRequest.property},
         |      "dynamicOption": ${submitRequest.dynamicOption.mkString(" ")},
         |      "args": ${submitRequest.args}
         |}
         |""".stripMargin)
    if (USER_FLINK_HOME == null) {
      USER_FLINK_HOME = submitRequest.flinkHome
    }
    doSubmit(submitRequest)
  }

  def stop(appId: String, jobStringId: String, savePoint: JavaBool, drain: JavaBool): String = {
    logInfo(
      s"""
         |"flink stop {" +
         |      "appId": $appId,
         |      "jobId": $jobStringId,
         |      "savePoint": $savePoint,
         |      "drain": $drain
         |}
         |""".stripMargin)
    doStop(appId, jobStringId, savePoint, drain)
  }

  def doSubmit(submitRequest: SubmitRequest): SubmitResponse

  def doStop(appId: String, jobStringId: String, savePoint: JavaBool, drain: JavaBool): String

  private[submit] def getJobID(jobId: String) = Try(JobID.fromHexString(jobId)) match {
    case Success(id) => id
    case Failure(e) => throw new CliArgsException(e.getMessage)
  }


  //----------Public Method end ------------------
  private[submit] def getEffectiveCommandLine(submitRequest: SubmitRequest,
                                              customCommandLines: JavaList[CustomCommandLine],
                                              otherParam: (String, String)*): CommandLine = {
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
        if (!verify) println(s"[StreamX] param:${x._1} is error,skip it.")
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
    println("Custom commandlines: {}", customCommandLines)
    for (cli <- customCommandLines) {
      val isActive = cli.isActive(line)
      println("Checking custom commandline {}, isActive: {}", cli, isActive)
      if (isActive) return cli
    }
    throw new IllegalStateException("No valid command-line found.")
  }

  private[submit] def getOptionFromDefaultFlinkConfig[T](option: ConfigOption[T]): T = flinkDefaultConfiguration.get(option)


}

case class WorkspaceEnv(
                         flinkName: String,
                         flinkHdfsHome: String,
                         flinkHdfsLibs: Path,
                         flinkHdfsPlugins: Path,
                         flinkHdfsJars: Path,
                         streamxPlugin: Path,
                         flinkHdfsDistJar: String
                       )
