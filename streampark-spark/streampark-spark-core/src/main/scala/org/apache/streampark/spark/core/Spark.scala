/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.streampark.spark.core

import org.apache.streampark.common.conf.ConfigConst._
import org.apache.streampark.common.util.{Logger, PropertiesUtils}

import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession

import scala.annotation.meta.getter
import scala.collection.mutable.ArrayBuffer

/** <b><code>Spark</code></b> <p/> Spark Basic Traits <p/> */
trait Spark extends Logger {

  @(transient @getter)
  final protected lazy val sparkConf: SparkConf = new SparkConf()

  @(transient @getter)
  final private[this] val sparkListeners = new ArrayBuffer[String]()

  @(transient @getter)
  final protected var sparkSession: SparkSession = _

  // Directory of checkpoint
  final protected var checkpoint: String = ""

  // If recovery from checkpoint fails, recreate
  final protected var createOnError: Boolean = true

  /** Entrance */
  def main(args: Array[String]): Unit = {

    init(args)

    config(sparkConf)

    // 1) system.properties
    val sysProps = sparkConf.getAllWithPrefix("spark.config.system.properties")
    if (sysProps != null) {
      sysProps.foreach(
        x => {
          System.getProperties.setProperty(x._1.drop(1), x._2)
        })
    }

    val builder = SparkSession.builder().config(sparkConf)
    val enableHive = sparkConf.getBoolean("spark.config.enable.hive.support", defaultValue = false)
    if (enableHive) {
      builder.enableHiveSupport()
    }

    sparkSession = builder.getOrCreate()

    // 2) hive
    val sparkSql = sparkConf.getAllWithPrefix("spark.config.spark.sql")
    if (sparkSql != null) {
      sparkSql.foreach(
        x => {
          sparkSession.sparkContext.getConf.set(x._1.drop(1), x._2)
        })
    }

    ready()
    handle()
    start()
    destroy()
  }

  /** Initialize sparkConf according to user parameters */
  final private def init(args: Array[String]): Unit = {
    var argv = args.toList
    var conf: String = null
    val userArgs = ArrayBuffer[(String, String)]()

    while (argv.nonEmpty) {
      argv match {
        case "--conf" :: value :: tail =>
          conf = value
          argv = tail
        case "--checkpoint" :: value :: tail =>
          checkpoint = value
          argv = tail
        case "--createOnError" :: value :: tail =>
          createOnError = value.toBoolean
          argv = tail
        case Nil =>
        case other :: value :: tail if other.startsWith("--") =>
          userArgs += other.drop(2) -> value
          argv = tail
        case tail =>
          logError(s"Unrecognized options: ${tail.mkString(" ")}")
          printUsageAndExit()
      }
    }

    val localConf = conf.split("\\.").last match {
      case "conf" => PropertiesUtils.fromHoconFile(conf)
      case "properties" => PropertiesUtils.fromPropertiesFile(conf)
      case "yaml" | "yml" => PropertiesUtils.fromYamlFile(conf)
      case _ =>
        throw new IllegalArgumentException(
          "[StreamPark] Usage: config file error,must be [properties|yaml|conf]")
    }

    sparkConf.setAll(localConf).setAll(userArgs)

    val appMain = sparkConf.get(KEY_SPARK_MAIN_CLASS, null)
    if (appMain == null) {
      logError(s"[StreamPark] parameter: $KEY_SPARK_MAIN_CLASS must not be empty!")
      System.exit(1)
    }

    val appName = sparkConf.get(KEY_SPARK_APP_NAME, null) match {
      case null | "" => appMain
      case name => name
    }

    // debug mode
    val localMode = sparkConf.get("spark.master", null) == "local"
    if (localMode) {
      sparkConf.setAppName(s"[LocalDebug] $appName").setMaster("local[*]")
      sparkConf.set("spark.streaming.kafka.maxRatePerPartition", "10")
    }
    // stop...
    sparkConf.set("spark.streaming.stopGracefullyOnShutdown", "true")

    val extraListeners =
      sparkListeners.mkString(",") + "," + sparkConf.get("spark.extraListeners", "")
    if (extraListeners != "") {
      sparkConf.set("spark.extraListeners", extraListeners)
    }
  }

  /**
   * The purpose of the config phase is to allow the developer to set more parameters (other than
   * the agreed configuration file) by means of hooks. Such as, conf.set("spark.serializer",
   * "org.apache.spark.serializer.KryoSerializer") conf.registerKryoClasses(Array(classOf[User],
   * classOf[Order],...))
   */
  def config(sparkConf: SparkConf): Unit = {}

  /**
   * The ready phase is an entry point for the developer to do other actions after the parameters
   * have been set, and is done after initialization and before the program starts.
   */
  def ready(): Unit = {}

  /**
   * The handle phase is the entry point to the code written by the developer and is the most
   * important phase.
   */
  def handle(): Unit

  /** The start phase starts the task, which is executed automatically by the framework. */
  def start(): Unit = {}

  /**
   * The destroy phase, is the last phase before jvm exits after the program has finished running,
   * and is generally used to wrap up the work.
   */
  def destroy(): Unit

  /** printUsageAndExit */
  private[this] def printUsageAndExit(): Unit = {
    logError(
      """
        |"Usage: Streaming [options]
        |
        | Options are:
        |   --checkpoint <checkpoint dir>
        |   --createOnError <Failed to recover from checkpoint, whether to recreated, true or false>
        |""".stripMargin)
    System.exit(1)
  }

}
