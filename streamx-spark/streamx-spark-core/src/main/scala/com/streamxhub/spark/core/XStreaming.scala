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

package com.streamxhub.spark.core

import java.io.StringReader
import java.util.Properties
import java.util.regex.Pattern

import com.streamxhub.spark.core.util.SystemPropertyUtil
import com.streamxhub.spark.monitor.api.{Const, HeartBeat}
import com.streamxhub.spark.monitor.api.util.{PropertiesUtil, ZooKeeperUtil}

import scala.collection.JavaConverters._
import scala.collection.JavaConversions._
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.apache.spark.streaming.{Seconds, StreamingContext}

import scala.annotation.meta.getter
import scala.collection.mutable.ArrayBuffer
import scala.util.Try
import com.streamxhub.spark.monitor.api.Const._
import org.apache.commons.codec.digest.DigestUtils

/**
  *
  * Spark Streaming 入口封装
  *
  */
trait XStreaming {

  protected final def args: Array[String] = params

  private final var params: Array[String] = _

  private final var sparkConf: SparkConf = _

  private val sparkListeners = new ArrayBuffer[String]()

  // checkpoint目录
  private var checkpointPath: String = ""

  // 从checkpoint 中恢复失败，则重新创建
  private var createOnError: Boolean = true

  private var localSparkConf: Map[String, String] = Map[String, String]()


  @(transient@getter)
  var sparkSession: SparkSession = _

  /**
    * 初始化，函数，可以设置 sparkConf
    *
    * @param sparkConf
    */
  def configure(sparkConf: SparkConf): Unit = {}

  /**
    * StreamingContext 运行之前执行
    *
    * @param ssc
    */
  def beforeStarted(ssc: StreamingContext): Unit = {}

  /**
    * StreamingContext 运行之后执行
    */
  def afterStarted(ssc: StreamingContext): Unit = {
  }

  /**
    * StreamingContext 停止后 程序停止前 执行
    */
  def beforeStop(ssc: StreamingContext): Unit = {

  }

  /**
    * 处理函数
    *
    * @param ssc
    */
  def handle(ssc: StreamingContext)

  /**
    * 创建 Context
    *
    * @return
    */

  private[this] def initialize(args: Array[String]): Unit = {
    this.params = args
    var argv = args.toList
    while (argv.nonEmpty) {
      argv match {
        case ("--checkpointPath") :: value :: tail =>
          checkpointPath = value
          argv = tail
        case ("--createOnError") :: value :: tail =>
          createOnError = value.toBoolean
          argv = tail
        case Nil =>
        case tail =>
          System.err.println(s"Unrecognized options: ${tail.mkString(" ")}")
          printUsageAndExit()
      }
    }
    sparkConf = new SparkConf()
    sparkConf.set(SPARK_PARAM_USER_ARGS, args.mkString("|"))
    //通过vm -Dspark.debug.conf传入配置文件的默认当作本地调试模式
    val (isDebug, conf) = SystemPropertyUtil.get(SPARK_PARAM_DEBUG_CONF, "") match {
      case "" => (false, sparkConf.get(SPARK_PARAM_DEPLOY_CONF))
      case path => (true, path)
      case _ => throw new IllegalArgumentException("[StreamX] Usage:properties-file error")
    }

    val localConf = conf.split("\\.").last match {
      case "properties" => PropertiesUtil.getPropertiesFromFile(conf)
      case "yml" => PropertiesUtil.getPropertiesFromYaml(conf)
      case _ => throw new IllegalArgumentException("[StreamX] Usage:properties-file format error,muse be properties or yml")
    }

    val appMain = localConf.getOrDefault(SPARK_PARAM_MAIN_CLASS, null)
    if (appMain == null || appMain == "") {
      System.err.println(s"[StreamX] $SPARK_PARAM_MAIN_CLASS must be not empty!")
      System.exit(1)
    }

    /**
      * 先获取配置文件里的spark.app.name,如果没有则以spark.app.main为appName
      */
    val appName = localConf.get(SPARK_PARAM_APP_NAME) match {
      case null | "" => appMain
      case name => name
    }
    val myId = DigestUtils.md5Hex(appName)
    sparkConf.set(SPARK_PARAM_APP_MYID, myId)

    //保存本地的配置文件版本
    val localVersion = localConf.getOrElse(SPARK_PARAM_APP_CONF_LOCAL_VERSION, SPARK_APP_CONF_DEFAULT_VERSION)
    sparkConf.set(SPARK_PARAM_APP_CONF_LOCAL_VERSION, localVersion)

    val cloudConf = Try {
      val zookeeperURL = localConf(SPARK_PARAM_MONITOR_ZOOKEEPER)
      val path = s"${Const.SPARK_CONF_PATH_PREFIX}/$myId"
      val cloudConf = ZooKeeperUtil.get(path, zookeeperURL)
      if (Pattern.compile(Const.SPARK_CONF_TYPE_REGEXP).matcher(cloudConf).find) {
        val properties = new Properties()
        properties.load(new StringReader(cloudConf))
        properties.stringPropertyNames().asScala.map(k => (k, properties.getProperty(k).trim)).toMap
      } else {
        PropertiesUtil.getPropertiesFromYamlText(cloudConf).toMap
      }
    }.getOrElse(Map.empty)

    /**
      * 直接读取本地的配置文件,注意规则:
      * 1)读取配置文件里的version,会和配置中心里的version对比,如果配置中心里的version和本地相同或比本地的version大,则会使用配置中心里的version
      * 如果比本地小则使用本地的配置.
      * ...
      */
    cloudConf match {
      /**
        * 配置中心无配置文件,或者获取失败,则读取本地配置文件
        */
      case null => sparkConf.setAll(localConf)
      case _ =>
        val cloudVersion = cloudConf.getOrElse(SPARK_PARAM_APP_CONF_LOCAL_VERSION, SPARK_APP_CONF_DEFAULT_VERSION)
        cloudVersion.toString.compare(localVersion) match {
          case 1 | 0 => sparkConf.setAll(cloudConf)
          case _ => sparkConf.setAll(localConf)
        }
        //保存线上的版本...
        sparkConf.set(SPARK_PARAM_APP_CONF_CLOUD_VERSION, cloudVersion)
    }
    //debug mode
    if (isDebug) {
      val appName = sparkConf.get(SPARK_PARAM_APP_NAME)
      sparkConf.setAppName(s"[LocalDebug] $appName").setMaster("local[*]")
      sparkConf.set("spark.streaming.kafka.maxRatePerPartition", "10")
    }
    sparkConf.set(SPARK_PARAM_APP_CONF_SOURCE, PropertiesUtil.getFileSource(conf))
    sparkConf.set(SPARK_PARAM_APP_DEBUG, isDebug.toString)
  }


  def creatingContext(): StreamingContext = {
    val extraListeners = sparkListeners.mkString(",") + "," + sparkConf.get("spark.extraListeners", "")
    if (extraListeners != "") sparkConf.set("spark.extraListeners", extraListeners)
    sparkSession = SparkSession.builder().config(sparkConf).getOrCreate()
    // 时间间隔
    val duration = sparkConf.get("spark.batch.duration").toInt
    val ssc = new StreamingContext(sparkSession.sparkContext, Seconds(duration))
    handle(ssc)
    ssc
  }

  private def printUsageAndExit(): Unit = {
    System.err.println(
      """
        |"Usage: Streaming [options]
        |
        | Options are:
        |   --checkpointPath <checkpoint 目录设置>
        |   --createOnError <从 checkpoint 恢复失败,是否重新创建 true|false>
        |""".stripMargin)
    System.exit(1)
  }

  def cleanSparkConf(): Unit = {
    localSparkConf += SPARK_PARAM_DEPLOY_CONF -> sparkConf.get(SPARK_PARAM_DEPLOY_CONF)
    localSparkConf += SPARK_PARAM_DEPLOY_STARTUP -> sparkConf.get(SPARK_PARAM_DEPLOY_STARTUP)
    localSparkConf += SPARK_PARAM_APP_CONF_SOURCE -> sparkConf.get(SPARK_PARAM_APP_CONF_SOURCE)
    sparkConf.remove(SPARK_PARAM_DEPLOY_CONF)
    sparkConf.remove(SPARK_PARAM_DEPLOY_STARTUP)
    sparkConf.remove(SPARK_PARAM_APP_CONF_SOURCE)
  }

  def addSparkConf(): Unit = {
    sparkConf.set(SPARK_PARAM_DEPLOY_CONF, localSparkConf(SPARK_PARAM_DEPLOY_CONF))
    sparkConf.set(SPARK_PARAM_DEPLOY_STARTUP, localSparkConf(SPARK_PARAM_DEPLOY_STARTUP))
    sparkConf.set(SPARK_PARAM_APP_CONF_SOURCE, localSparkConf(SPARK_PARAM_APP_CONF_SOURCE))
  }

  def main(args: Array[String]): Unit = {
    initialize(args)
    configure(sparkConf)
    val context = checkpointPath match {
      case "" => creatingContext()
      case ck =>
        val ssc = StreamingContext.getOrCreate(ck, creatingContext, createOnError = createOnError)
        ssc.checkpoint(ck)
        ssc
    }

    beforeStarted(context)
    //将多余的参数从sparkConf中移除
    cleanSparkConf()
    context.start()
    //将需要的参数添加到sparkConf
    addSparkConf()
    HeartBeat(context).start()
    afterStarted(context)
    context.awaitTermination()
    HeartBeat(context).stop()
    beforeStop(context)
  }

}
