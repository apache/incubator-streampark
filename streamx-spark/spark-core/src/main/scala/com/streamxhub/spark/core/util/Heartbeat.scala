package com.streamxhub.spark.core.util

import java.util.concurrent.{Executors, TimeUnit}

import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.apache.spark.streaming.StreamingContext


/**
  *
  */
class Heartbeat(private val ssc: StreamingContext) extends Logger {
  private val threadFactory =
    new ThreadFactoryBuilder().setDaemon(true).setNameFormat("spark-monitor-heartbeat-thread").build()
  private val heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(threadFactory)

  def start(): Unit = {
    val sparkConf = ssc.sparkContext.getConf
    logInfo("start heartbeatExecutor ...")
    sparkConf.get("spark.monitor.heartbeat.api", "none") match {
      case "none" =>
      case heartbeat =>

        val initialDelay = sparkConf.get("spark.monitor.heartbeat.initialDelay", "60000").toLong
        val period = sparkConf.get("spark.monitor.heartbeat.period", "10000").toLong

        heartbeatExecutor.scheduleAtFixedRate(new Runnable {
          override def run(): Unit = {
            try {
              val url = s"$heartbeat/${sparkConf.getAppId}/${period * 3}"
              val result = Utils.httpGet(url)
              logInfo(s"send heartbeat to $url $result")
            } catch {
              case e: Throwable => e.printStackTrace()
            }
          }
        }, initialDelay, period, TimeUnit.MILLISECONDS)
    }
  }

  def stop(): Unit = {
    logInfo("shutdown heartbeatExecutor ...")
    heartbeatExecutor.shutdown()
  }

}
