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
import org.apache.streampark.common.util.{PropertiesUtils, SystemPropertyUtils}
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession

import scala.annotation.meta.getter
import scala.collection.mutable.ArrayBuffer

/**
 * <b><code>Spark</code></b>
 * <p/>
 * Spark Basic Traits
 * <p/>
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
      case _ => throw new IllegalArgumentException("[StreamPark] Usage:properties-file error")
    }

    val localConf = confPath.split("\\.").last match {
      case "properties" => PropertiesUtils.fromPropertiesFile(confPath)
      case "yaml" | "yml" => PropertiesUtils.fromYamlFile(confPath)
      case _ => throw new IllegalArgumentException("[StreamPark] Usage:properties-file format error,must be properties or yml")
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
      System.err.println(s"[StreamPark] $KEY_SPARK_MAIN_CLASS must not be empty!")
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
   * The purpose of the config phase is to allow the developer to set more parameters (other than the agreed
   * configuration file) by means of hooks.
   * Such as,
   *  conf.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
   *  conf.registerKryoClasses(Array(classOf[User], classOf[Order],...))
   */
  def config(sparkConf: SparkConf): Unit

  /**
   * The ready phase is an entry point for the developer to do other actions after the parameters have been set,
   * and is done after initialization and before the program starts.
   */
  def ready(): Unit

  /**
   * The handle phase is the entry point to the code written by the developer and is the most important phase.
   */
  def handle(): Unit

  /**
   * The start phase starts the task, which is executed automatically by the framework.
   */
  def start(): Unit

  /**
   * The destroy phase, is the last phase before jvm exits after the program has finished running,
   * and is generally used to wrap up the work.
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
        |   --checkpoint <checkpoint dir>
        |   --createOnError <Failed to recover from checkpoint, whether to recreated, true or false>
        |""".stripMargin)
    System.exit(1)
  }

}
