package com.streamxhub.spark.core

import com.streamxhub.spark.core.util.{Logger, ZookeeperUtil}
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

  private val appName = sparkConf.get("spark.app.name")

  private val appId = sparkConf.getAppId

  private val zookeeperURL = sparkConf.get("spark.monitor.zookeeper")

  private val path = s"/StreamX/spark/$appName"

  def start(): Unit = {
    //本地测试,不启动心跳检测
    if (!isDebug()) {
      //spark处理间隔..
      val duration = sparkConf.getOption("spark.batch.duration")
      //钉钉URL
      val dingURL = sparkConf.getOption("spark.monitor.dingTalk.url")
      //钉钉@人...
      val dingUser = sparkConf.getOption("spark.monitor.dingTalk.user")

      /**
        * action:
        * startup:启动中,
        * running:运行中
        * killed:被杀,
        * error:挂了.
        */
      val data =
        s"""
           |{
           |"appId":"$appId",
           |"action":"startup",
           |"duration":$duration,
           |"dingURL":"$dingURL",
           |"dingUser":"$dingUser"
           |}
        """.stripMargin
      ZookeeperUtil.create(path, data, zookeeperURL)
    }
  }

  def stop(): Unit = {
    if (!isDebug()) {
      ZookeeperUtil.delete(path, zookeeperURL)
      logInfo("shutdown heartbeatExecutor ...")
    }
  }

  private def isDebug() = {
    sparkConf.contains("spark.conf")
  }

}
