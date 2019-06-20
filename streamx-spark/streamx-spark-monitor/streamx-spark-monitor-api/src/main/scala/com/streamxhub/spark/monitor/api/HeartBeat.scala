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
    val appId = sparkConf.get("spark.app.myid")
    this.zookeeperURL = sparkConf.get("spark.monitor.zookeeper")
    this.confPath = s"/StreamX/spark/conf/$appId"
    this.monitorPath = s"/StreamX/spark/monitor/$appId"
    this.isDebug = false//sparkConf.getBoolean("spark.app.debug", false)
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
      //register shutdown hook
      Runtime.getRuntime.addShutdownHook(new Thread(new Runnable {
        override def run(): Unit = {
          HeartBeat.this.stop()
          logger.info(s"[StreamX] run shutdown hook,appName:${sparkConf.get("spark.app.name")},appId:${sparkConf.getAppId} ")
        }
      }))
      //register conf...
      ZooKeeperUtil.create(confPath, sparkConf.get("spark.app.conf"), zookeeperURL, persistent = true)
      //register monitor...
      ZooKeeperUtil.create(monitorPath, sparkConf.toDebugString, zookeeperURL)
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
