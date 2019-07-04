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

package com.streamxhub.spark.monitor.api

import com.streamxhub.spark.monitor.api.util.ZooKeeperUtil
import org.apache.spark.api.java.JavaSparkContext
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.api.java.JavaStreamingContext
import org.apache.spark.{SparkConf, SparkContext}
import org.slf4j.LoggerFactory
import com.streamxhub.spark.monitor.api.Const._

/**
  * 心跳上报程序
  */
object HeartBeat {

  private val logger = LoggerFactory.getLogger(this.getClass.getName.stripSuffix("$"))

  private var sparkConf: SparkConf = _

  private var zookeeperURL: String = _

  private var confPath: String = _

  private var monitorPath: String = _

  private var isDebug: Boolean = _

  private[this] def initialize(sc: SparkContext): Unit = {
    this.sparkConf = sc.getConf
    val myId = sparkConf.get(SPARK_PARAM_APP_MYID)
    this.zookeeperURL = sparkConf.get(SPARK_PARAM_MONITOR_ZOOKEEPER)
    this.confPath = s"$SPARK_CONF_PATH_PREFIX/$myId"
    this.monitorPath = s"$SPARK_MONITOR_PATH_PREFIX/$myId"
    this.isDebug = sparkConf.getBoolean(SPARK_PARAM_APP_DEBUG, defaultValue = false)
  }

  //for java
  def apply(javaStreamingContext: JavaStreamingContext): HeartBeat.type = {
    HeartBeat(javaStreamingContext.sparkContext)
  }

  //for java
  def apply(javaSparkContext: JavaSparkContext): HeartBeat.type = {
    this.initialize(javaSparkContext)
    this
  }

  def apply(sc: StreamingContext): HeartBeat.type = {
    HeartBeat(sc.sparkContext)
  }

  def apply(sc: SparkContext): HeartBeat.type = {
    this.initialize(sc)
    this
  }

  def start(): Unit = {
    if (!isDebug) {
      //register conf...
      val localVersion = sparkConf.get(SPARK_PARAM_APP_CONF_LOCAL_VERSION)
      val cloudVersion = sparkConf.get(SPARK_PARAM_APP_CONF_CLOUD_VERSION, null)
      (cloudVersion, localVersion) match {
        case (null, _) =>

          /**
            * 第一次加载,zk里还没有配置文件...
            */
          sparkConf.set(SPARK_PARAM_APP_CONF_VERSION, localVersion)
          ZooKeeperUtil.create(confPath, sparkConf.get(SPARK_PARAM_APP_CONF_SOURCE), zookeeperURL, persistent = true)
        case (cloud, local) =>
          local.compare(cloud) match {
            /**
              * 本地配置文件比线上大...
              */
            case compare if compare > 0 =>
              sparkConf.set(SPARK_PARAM_APP_CONF_VERSION, local)
              ZooKeeperUtil.update(confPath, sparkConf.get(SPARK_PARAM_APP_CONF_SOURCE), zookeeperURL, persistent = true)
            case _ =>
              sparkConf.set(SPARK_PARAM_APP_CONF_VERSION, cloudVersion)
          }
        case _ =>
          new ExceptionInInitializerError("[StreamX] init config error,please check spark.app.conf.version.")
          System.exit(1)
      }
      val ignoredParam = List(SPARK_PARAM_DEPLOY_CONF, SPARK_PARAM_DEBUG_CONF, SPARK_PARAM_DEPLOY_STARTUP, SPARK_PARAM_APP_CONF_SOURCE, SPARK_PARAM_APP_CONF_LOCAL_VERSION, SPARK_PARAM_APP_CONF_CLOUD_VERSION)
      val debugInfo = sparkConf.getAll.filter(x => !ignoredParam.contains(x._1)).sorted.map { case (k, v) => k + "=" + v }.mkString("\n")
      logger.info(s"[StreamX] sparkConf Debug Info:$debugInfo")
      ZooKeeperUtil.create(monitorPath, debugInfo, zookeeperURL)
      logger.info(s"[StreamX] registry heartbeat path: $monitorPath")
    }
  }

  def stop(): Unit = {
    if (!isDebug) {
      ZooKeeperUtil.delete(monitorPath, zookeeperURL)
      logger.info(s"[StreamX] un registry heartbeat path: $monitorPath")
    }
  }

}
