package com.streamxhub.streamx.flink

import java.util.{Timer, TimerTask}
import scala.language.implicitConversions
import scala.collection.JavaConverters._

package object kubernetes {

  implicit def funcToTimerTask(fun: () => Unit): TimerTask = new TimerTask() {
    def run(): Unit = fun()
  }

  def watchJobStatusCacheSize(implicit trkMonitor: FlinkTrkMonitor): Unit =
    new Timer().scheduleAtFixedRate(() => println(s"[status-size]-${System.currentTimeMillis} => " +
      s"${trkMonitor.getAllJobStatus.size}"), 0, 1500)

  def watchMetricsCache(implicit trkMonitor: FlinkTrkMonitor): Unit =
    new Timer().scheduleAtFixedRate(() => println(s"[metric]-${System.currentTimeMillis} => " +
      s"${trkMonitor.getClusterMetrics.map(_.toString).getOrElse("empty")}"), 0, 1500)

  def watchJobStatusCache(implicit trkMonitor: FlinkTrkMonitor): Unit =
    new Timer().scheduleAtFixedRate(() => println(s"[status]-${System.currentTimeMillis} =>" +
      s"count=${trkMonitor.getAllJobStatus.size} | " +
      s" ${trkMonitor.getAllJobStatus.mkString(", ")}"), 0, 1500)

  def watchTrkIdsCache(implicit trkMonitor: FlinkTrkMonitor): Unit = {
    new Timer().scheduleAtFixedRate(() => println(s"[trkIds]-${System.currentTimeMillis} => " +
      s"${trkMonitor.getAllTrackingIds.mkString(",")}"), 0, 1500)
  }

  def watchTrkIdsCacheSize(implicit trkMonitor: FlinkTrkMonitor): Unit = {
    new Timer().scheduleAtFixedRate(() => println(s"[trkIds-size]-${System.currentTimeMillis} => " +
      s"${trkMonitor.getAllTrackingIds.size}"), 0, 1500)
  }

  def watchK8sEventCache(implicit trkMonitor: FlinkTrkMonitor): Unit = {

    new Timer().scheduleAtFixedRate(() => println(s"[k8s-event]-${System.currentTimeMillis} => " +
      s"count=${trkMonitor.asInstanceOf[DefaultFlinkTrkMonitor].trkCache.k8sDeploymentEvents.asMap().size} | " +
      s"${trkMonitor.asInstanceOf[DefaultFlinkTrkMonitor].trkCache.k8sDeploymentEvents.asMap().asScala.mkString(",")}"), 0, 1500)
  }


}
