package com.streamxhub.spark.core.util

import org.apache.spark.SparkContext
import org.apache.spark.streaming.StreamingContext

/**
  * 心跳上报程序
  */
class Heartbeat(private val sc: SparkContext) extends Logger {

  def this(ssc: StreamingContext) {
    this(ssc.sparkContext)
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
    }
  }

  def stop(): Unit = {
    if (!isDebug) {
      ZookeeperUtil.delete(path, zookeeperURL)
      logInfo("shutdown heartbeatExecutor ...")
    }
  }

}
