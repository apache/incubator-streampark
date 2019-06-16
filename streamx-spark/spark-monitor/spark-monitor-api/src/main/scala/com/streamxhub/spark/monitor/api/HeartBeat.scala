package com.streamxhub.spark.monitor.api

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

  private var path: String = _

  private var isDebug: Boolean = _

  private[this] def initialize(sc: SparkContext): Unit = {
    this.sparkConf = sc.getConf
    val appId = sparkConf.get("spark.app.myid")
    this.zookeeperURL = sparkConf.get("spark.monitor.zookeeper")
    this.path = s"/StreamX/spark/$appId"
    this.isDebug = false //sparkConf.contains("spark.conf")
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
      Runtime.getRuntime().addShutdownHook(new Thread(new Runnable {
        override def run(): Unit = {
          HeartBeat.this.stop()
          logger.info(s"[StreamX] run shutdown hook,appName:${sparkConf.get("spark.app.name")},appId:${sparkConf.getAppId} ")
        }
      }))
      ZooKeeperUtil.create(path, sparkConf.toDebugString, zookeeperURL)
      logger.info(s"[StreamX] registry heartbeat path: $path")
    }
  }

  def stop(): Unit = {
    if (!isDebug) {
      ZooKeeperUtil.delete(path, zookeeperURL)
      logger.info(s"[StreamX] un registry heartbeat path: $path")
    }
  }

}
