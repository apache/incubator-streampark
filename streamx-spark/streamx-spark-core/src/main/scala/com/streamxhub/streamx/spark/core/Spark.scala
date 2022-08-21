/*
 * Copyright 2019 The StreamX Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.spark.core

import com.streamxhub.streamx.common.conf.ConfigConst._
import com.streamxhub.streamx.common.util.{PropertiesUtils, SystemPropertyUtils}
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession

import scala.annotation.meta.getter
import scala.collection.mutable.ArrayBuffer

/**
 * <b><code>Spark</code></b>
 * <p/>
 * Spark基础特质
 * <p/>
 * <b>Creation Time:</b> 2022/8/8 20:44.
 *
 * @author guoning
 * @since streamx
 */
trait Spark {

  @(transient@getter)
  protected final lazy val sparkConf: SparkConf = new SparkConf()
  @(transient@getter)
  protected final val sparkListeners = new ArrayBuffer[String]()

  @(transient@getter)
  protected final var sparkSession: SparkSession = _

  // Directory of checkpoint
  protected final var checkpoint: String = ""

  // If recovery from checkpoint fails, recreate
  protected final var createOnError: Boolean = true


  /**
   * Entrance
   */
  def main(args: Array[String]): Unit = {
    init(args)
    config(sparkConf)
    sparkSession = sparkConf.get("spark.enable.hive.support", "false").toLowerCase match {
      case "true" => SparkSession.builder().enableHiveSupport().config(sparkConf).getOrCreate()
      case "false" => SparkSession.builder().config(sparkConf).getOrCreate()
    }
    ready()
    handle()
    start()
    destroy()
  }

  /**
   * Initialize sparkConf according to user parameters
   */

  final def init(args: Array[String]): Unit = {

    var argv = args.toList

    while (argv.nonEmpty) {
      argv match {
        case ("--checkpoint") :: value :: tail =>
          checkpoint = value
          argv = tail
        case ("--createOnError") :: value :: tail =>
          createOnError = value.toBoolean
          argv = tail
        case Nil =>
        case tail =>
          // scalastyle:off println
          System.err.println(s"Unrecognized options: ${tail.mkString(" ")}")
          printUsageAndExit()
      }
    }

    sparkConf.set(KEY_SPARK_USER_ARGS, args.mkString("|"))

    // The default configuration file passed in through vm -Dspark.debug.conf is used as local debugging mode
    val (isDebug, confPath) = SystemPropertyUtils.get(KEY_SPARK_CONF, "") match {
      case "" => (true, sparkConf.get(KEY_SPARK_DEBUG_CONF))
      case path => (false, path)
      case _ => throw new IllegalArgumentException("[StreamX] Usage:properties-file error")
    }

    val localConf = confPath.split("\\.").last match {
      case "properties" => PropertiesUtils.fromPropertiesFile(confPath)
      case "yaml" | "yml" => PropertiesUtils.fromYamlFile(confPath)
      case _ => throw new IllegalArgumentException("[StreamX] Usage:properties-file format error,must be properties or yml")
    }

    localConf.foreach(x => sparkConf.set(x._1, x._2))

    val (appMain, appName) = sparkConf.get(KEY_SPARK_MAIN_CLASS, null) match {
      case null | "" => (null, null)
      case other => sparkConf.get(KEY_SPARK_APP_NAME, null) match {
        case null | "" => (other, other)
        case name => (other, name)
      }
    }

    if (appMain == null) {
      // scalastyle:off println
      System.err.println(s"[StreamX] $KEY_SPARK_MAIN_CLASS must not be empty!")
      System.exit(1)
    }

    // debug mode
    if (isDebug) {
      sparkConf.setAppName(s"[LocalDebug] $appName").setMaster("local[*]")
      sparkConf.set("spark.streaming.kafka.maxRatePerPartition", "10")
    }
    // stop...
    sparkConf.set("spark.streaming.stopGracefullyOnShutdown", "true")

    val extraListeners = sparkListeners.mkString(",") + "," + sparkConf.get("spark.extraListeners", "")
    if (extraListeners != "") sparkConf.set("spark.extraListeners", extraListeners)
  }

  /**
   * config 阶段的目的是让开发者可以通过钩子的方式设置更多的参数(约定的配置文件以外的其他参数)
   * 用户设置sparkConf参数,如,spark序列化:
   * conf.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
   * // 注册要序列化的自定义类型。
   * conf.registerKryoClasses(Array(classOf[User], classOf[Order],...))
   *
   * @param sparkConf
   */
  def config(sparkConf: SparkConf): Unit

  /**
   * 阶段是在参数都设置完毕了,给开发者提供的一个用于做其他动作的入口, 该阶段是在初始化完成之后在程序启动之前进行
   */
  def ready(): Unit

  /**
   * handle 阶段是接入开发者编写的代码的阶段,是开发者编写代码的入口,也是最重要的一个阶段, 这个handle 方法会强制让开发者去实现
   */
  def handle(): Unit

  /**
   * start 阶段,顾名思义,这个阶段会启动任务,由框架自动执行
   */
  def start(): Unit

  /**
   * destroy 阶段,是程序运行完毕了,在jvm退出之前的最后一个阶段,一般用于收尾的工作
   */
  def destroy(): Unit

  /**
   * printUsageAndExit
   */
  def printUsageAndExit(): Unit = {
    // scalastyle:off println
    System.err.println(
      """
        |"Usage: Streaming [options]
        |
        | Options are:
        |   --checkpoint <checkpoint 目录设置>
        |   --createOnError <从 checkpoint 恢复失败,是否重新创建 true|false>
        |""".stripMargin)
    System.exit(1)
  }


}
