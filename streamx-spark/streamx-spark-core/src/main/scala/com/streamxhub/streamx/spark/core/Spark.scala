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

package com.streamxhub.streamx.spark.core

import com.streamxhub.streamx.common.conf.ConfigConst._
import com.streamxhub.streamx.common.util.{PropertiesUtils, SystemPropertyUtils}
import org.apache.spark.sql.SparkSession
import org.apache.spark.{SparkConf, SparkContext}

import scala.annotation.meta.getter
import scala.collection.mutable.ArrayBuffer

/**
 *
 */
trait Spark {

  protected final def args: Array[String] = _args

  protected final var _args: Array[String] = _

  protected val sparkListeners = new ArrayBuffer[String]()

  @(transient@getter)
  protected var sparkSession: SparkSession = _

  protected final var sparkConf: SparkConf = _

  // checkpoint目录
  protected var checkpoint: String = ""

  // 从checkpoint 中恢复失败，则重新创建
  protected var createOnError: Boolean = true

  /**
   * 用户设置sparkConf参数,如,spark序列化:
   * conf.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
   * // 注册要序列化的自定义类型。
   * conf.registerKryoClasses(Array(classOf[User], classOf[Order],...))
   *
   * @param conf
   */
  def initialize(sparkConf: SparkConf): Unit = {}

  /**
   * 初始化参数
   *
   * @return
   */

  def initArgs(args: Array[String]): Unit = {

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

    sparkConf = new SparkConf()
    sparkConf.set(KEY_SPARK_USER_ARGS, args.mkString("|"))

    //通过vm -Dspark.debug.conf传入配置文件的默认当作本地调试模式
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

    //debug mode
    if (isDebug) {
      sparkConf.setAppName(s"[LocalDebug] $appName").setMaster("local[*]")
      sparkConf.set("spark.streaming.kafka.maxRatePerPartition", "10")
    }
    //优雅停止...
    sparkConf.set("spark.streaming.stopGracefullyOnShutdown", "true")

    initialize(sparkConf)

    val extraListeners = sparkListeners.mkString(",") + "," + sparkConf.get("spark.extraListeners", "")
    if (extraListeners != "") sparkConf.set("spark.extraListeners", extraListeners)

    sparkSession = SparkSession.builder().enableHiveSupport().config(sparkConf).getOrCreate()

  }

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
