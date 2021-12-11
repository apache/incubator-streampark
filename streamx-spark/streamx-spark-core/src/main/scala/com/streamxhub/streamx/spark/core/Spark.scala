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

  private final var _args: Array[String] = _

  private val sparkListeners = new ArrayBuffer[String]()

  @(transient@getter)
  var sparkSession: SparkSession = _

  /**
    * 初始化，函数，可以设置 sparkConf
    *
    * @param sparkConf
    */
  def initialize(sparkConf: SparkConf): Unit = {}

  /**
    * StreamingContext 运行之后执行
    */
  def afterStarted(sc: SparkContext): Unit = {
  }

  /**
    * StreamingContext 停止后 程序停止前 执行
    */
  def beforeStop(sc: SparkContext): Unit = {
  }

  /**
    * 处理函数
    *
    * @param sc
    */
  def handle(sc: SparkContext)

  def creatingContext(): SparkContext = {
    val sparkConf = new SparkConf()

    sparkConf.set(KEY_SPARK_USER_ARGS, args.mkString("|"))

    //通过vm -Dspark.conf传入配置文件的默认当作本地调试模式
    val (isDebug, conf) = SystemPropertyUtils.get(KEY_SPARK_CONF, "") match {
      case "" => (false, sparkConf.get(KEY_SPARK_DEBUG_CONF))
      case path => (true, path)
      case _ => throw new IllegalArgumentException("[StreamX] Usage:properties-file error")
    }

    conf.split("\\.").last match {
      case "properties" =>
        sparkConf.setAll(PropertiesUtils.fromPropertiesFile(conf))
      case "yaml"|"yml" =>
        sparkConf.setAll(PropertiesUtils.fromYamlFile(conf))
      case _ => throw new IllegalArgumentException("[StreamX] Usage:properties-file format error,must be properties or yml")
    }

    //debug mode
    if (isDebug) {
      val appName = sparkConf.get(KEY_APP_HOME)
      sparkConf.setAppName(s"[LocalDebug] $appName").setMaster("local[*]")
      sparkConf.set("spark.streaming.kafka.maxRatePerPartition", "10")
    }

    initialize(sparkConf)

    val extraListeners = sparkListeners.mkString(",") + "," + sparkConf.get("spark.extraListeners", "")
    if (extraListeners != "") sparkConf.set("spark.extraListeners", extraListeners)

    sparkSession = SparkSession.builder().enableHiveSupport().config(sparkConf).getOrCreate()

    val sc = sparkSession.sparkContext
    handle(sc)
    sc
  }

  def main(args: Array[String]): Unit = {

    this._args = args

    val context = creatingContext()
    afterStarted(context)
    context.stop()
    beforeStop(context)
  }

}
