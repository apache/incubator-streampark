package com.streamxhub.spark.core.util

import org.apache.spark.SparkContext
import org.apache.spark.streaming.StreamingContext

import scala.io.Source

/**
  * 心跳上报程序
  */
class Heartbeat(private val sc: SparkContext) extends Logger {

  def this(ssc: StreamingContext) {
    this(ssc.sparkContext)
  }

  private val sparkConf = sc.getConf

  private val appName = sparkConf.get("spark.app.name")

  private val appId = sparkConf.getAppId

  private val zookeeperURL = sparkConf.get("spark.monitor.zookeeper")

  private val path = s"/StreamX/spark/$appName"

  def start(): Unit = {
    //本地测试,不启动心跳检测
    if (!isDebug()) {
      //spark处理间隔..
      val map = Map(
        "appId" -> appId,
        "config" -> sparkConf.toDebugString
      )
      val json = Json.generate(map)
      ZookeeperUtil.create(path, json, zookeeperURL)
    }
  }

  def stop(): Unit = {
    if (!isDebug()) {
      ZookeeperUtil.delete(path, zookeeperURL)
      logInfo("shutdown heartbeatExecutor ...")
    }
  }

  private def isDebug() = {
    sparkConf.contains("debug.conf")
  }

}
