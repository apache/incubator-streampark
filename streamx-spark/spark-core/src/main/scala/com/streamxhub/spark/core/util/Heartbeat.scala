package com.streamxhub.spark.core.util

import org.apache.spark.SparkContext
import org.apache.spark.streaming.StreamingContext

/**
  * 心跳上报程序
  */
object Heartbeat extends Logger {

  private var sc: SparkContext = _

  def apply(sc: StreamingContext): Heartbeat.type = {
    this.sc = sc.sparkContext
    this
  }

  def apply(sc: SparkContext): Heartbeat.type = {
    this.sc = sc
    this
  }

  private val sparkConf = sc.getConf

  private val myid = sparkConf.get("spark.app.myid")

  private val zookeeperURL = sparkConf.get("spark.monitor.zookeeper")

  private val path = s"/StreamX/spark/$myid"

  private val isDebug = sparkConf.contains("spark.conf")

  def start(): Unit = {
    //本地测试,不启动心跳检测
    if (!isDebug) {
      ZookeeperUtil.create(path, sparkConf.toDebugString, zookeeperURL)
      logInfo(s"[StreamX] registry heartbeat path: $path")
    }
  }

  def stop(): Unit = {
    if (!isDebug) {
      ZookeeperUtil.delete(path, zookeeperURL)
      logInfo(s"[StreamX] un registry heartbeat path: $path")
    }
  }

}
