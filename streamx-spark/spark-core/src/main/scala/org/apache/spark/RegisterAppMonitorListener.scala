package org.apache.spark

import org.apache.spark.internal.Logging
import org.apache.spark.scheduler.{SparkListener, SparkListenerApplicationEnd, SparkListenerApplicationStart}
import org.apache.spark.util.Utils

import scala.util.control.NonFatal

/**
  *
  *
  * 用于向YarnAppMonitorSer注册监控的sparkListener
  * 目前的问题在于无法保证onApplicationEnd事件一定被触发,即重启请求不一定会发送
  *
  */
class RegisterAppMonitorListener(val sparkConf: SparkConf) extends SparkListener with Logging {

  private val appName = sparkConf.get("spark.app.name", "None")
  private val runConf = sparkConf.get("spark.app.main.conf", "None")
  private val host = sparkConf.get("spark.application.monitor.host", Utils.localHostName)
  private val port = sparkConf.getInt("spark.application.monitor.port", 23456)

  private def sendStartReq(): Unit = {
    try {
      val yarnAppMonitorRef = YarnAppMonitorCli.createYarnAppMonitorRef(sparkConf, host, port)
      yarnAppMonitorRef.send(YarnAppStartRequest(appName, runConf))
      logInfo(s"send start app request to YarnAppMonitorServer $appName $runConf")
    } catch {
      case NonFatal(e) => logError("send start request failed. ", e)
    }
  }

  override def onApplicationStart(applicationStart: SparkListenerApplicationStart): Unit = {
    val appId = applicationStart.appId
    logInfo(appId.toString)
  }

  override def onApplicationEnd(applicationEnd: SparkListenerApplicationEnd): Unit = {
    logInfo("app end time " + applicationEnd.time)
    if (runConf != "None" && appName != "None") sendStartReq()
  }

}
