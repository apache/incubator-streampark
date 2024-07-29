/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.streampark.flink.kubernetes.helper

import org.apache.streampark.common.util.Logger
import org.apache.streampark.flink.kubernetes.{DefaultFlinkK8sWatcher, FlinkK8sWatcher}

import java.util.{Timer, TimerTask}

import scala.language.implicitConversions

/** Debug helper for FlinkTrackMonitor, only for streampark development, debugging scenarios. */
object KubernetesWatcherHelper extends Logger {

  implicit private def funcToTimerTask(fun: () => Unit): TimerTask =
    new TimerTask() {
      def run(): Unit = fun()
    }

  // scalastyle:off println
  // print job status cache size info
  def watchJobStatusCacheSize(implicit k8sWatcher: FlinkK8sWatcher): Unit =
    new Timer().scheduleAtFixedRate(
      () =>
        logInfo(
          s"[flink-k8s][status-size]-${System.currentTimeMillis} => " +
            s"${k8sWatcher.getAllJobStatus.size}"),
      0,
      1500)

  // print agg flink cluster metrics cache detail
  def watchAggClusterMetricsCache(implicit k8sWatcher: FlinkK8sWatcher): Unit =
    new Timer().scheduleAtFixedRate(
      () =>
        logInfo(
          s"[flink-k8s][agg-metric]-${System.currentTimeMillis} => " +
            s"${k8sWatcher.getAccGroupMetrics()}"),
      0,
      1500)

  // print all cluster metrics for each flink cluster
  def watchClusterMetricsCache(implicit k8sWatcher: FlinkK8sWatcher): Unit =
    new Timer().scheduleAtFixedRate(
      () =>
        logInfo(s"[flink-k8s][metric]-${System.currentTimeMillis} => " +
          s"count=${k8sWatcher.asInstanceOf[DefaultFlinkK8sWatcher].watchController.flinkMetrics.asMap().size} | " +
          s"${k8sWatcher.asInstanceOf[DefaultFlinkK8sWatcher].watchController.flinkMetrics.asMap().mkString(",")}"),
      0,
      1500)

  // print job cache detail
  def watchJobStatusCache(implicit k8sWatcher: FlinkK8sWatcher): Unit =
    new Timer().scheduleAtFixedRate(
      () =>
        logInfo(
          s"[flink-k8s][status]-${System.currentTimeMillis} =>" +
            s"count=${k8sWatcher.getAllJobStatus.size} | " +
            s" ${k8sWatcher.getAllJobStatus.mkString(", ")}"),
      0,
      1500)

  // print trackId cache detail
  def watchTrackIdsCache(implicit k8sWatcher: FlinkK8sWatcher): Unit = {
    new Timer().scheduleAtFixedRate(
      () =>
        logInfo(
          s"[flink-k8s][trackIds]-${System.currentTimeMillis} => " +
            s"${k8sWatcher.getAllWatchingIds.mkString(",")}"),
      0,
      1500)
  }

  def watchTrackIdsCacheSize(implicit k8sWatcher: FlinkK8sWatcher): Unit = {
    new Timer().scheduleAtFixedRate(
      () =>
        logInfo(
          s"[flink-k8s][trackIds-size]-${System.currentTimeMillis} => " +
            s"${k8sWatcher.getAllWatchingIds.size}"),
      0,
      1500)
  }

  def watchK8sEventCache(implicit k8sWatcher: FlinkK8sWatcher): Unit = {
    new Timer().scheduleAtFixedRate(
      () =>
        logInfo(s"[flink-k8s][k8s-event]-${System.currentTimeMillis} => " +
          s"count=${k8sWatcher.asInstanceOf[
              DefaultFlinkK8sWatcher].watchController.k8sDeploymentEvents.asMap().size} | " +
          s"${k8sWatcher.asInstanceOf[
              DefaultFlinkK8sWatcher].watchController.k8sDeploymentEvents.asMap().mkString(",")}"),
      0,
      1500)
  }

}
