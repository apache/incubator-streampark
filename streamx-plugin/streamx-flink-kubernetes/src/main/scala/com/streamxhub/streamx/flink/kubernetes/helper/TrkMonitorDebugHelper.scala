/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.flink.kubernetes.helper

import com.streamxhub.streamx.flink.kubernetes.{DefaultK8sFlinkTrkMonitor, K8sFlinkTrkMonitor}

import java.util.{Timer, TimerTask}
import scala.collection.JavaConverters._
import scala.language.implicitConversions

/**
 * Debug helper for FlinkTrkMonitor, only for streamx development, debugging scenarios.
 * @author Al-assad
 */
object TrkMonitorDebugHelper {

  private implicit def funcToTimerTask(fun: () => Unit): TimerTask = new TimerTask() {
    def run(): Unit = fun()
  }

  // scalastyle:off println
  // print job status cache size info
  def watchJobStatusCacheSize(implicit trkMonitor: K8sFlinkTrkMonitor): Unit =
    new Timer().scheduleAtFixedRate(() => println(s"[flink-k8s][status-size]-${System.currentTimeMillis} => " +
      s"${trkMonitor.getAllJobStatus.size}"), 0, 1500)

  // print agg flink cluster metrics cache detail
  def watchAggClusterMetricsCache(implicit trkMonitor: K8sFlinkTrkMonitor): Unit =
    new Timer().scheduleAtFixedRate(() => println(s"[flink-k8s][agg-metric]-${System.currentTimeMillis} => " +
      s"${trkMonitor.getAccClusterMetrics}"), 0, 1500)

  // print all cluster metrics for each flink cluster
  def watchClusterMetricsCache(implicit trkMonitor: K8sFlinkTrkMonitor): Unit =
    new Timer().scheduleAtFixedRate(() => println(s"[flink-k8s][metric]-${System.currentTimeMillis} => " +
      s"count=${trkMonitor.asInstanceOf[DefaultK8sFlinkTrkMonitor].trkCache.flinkMetrics.asMap().size} | " +
      s"${trkMonitor.asInstanceOf[DefaultK8sFlinkTrkMonitor].trkCache.flinkMetrics.asMap().asScala.mkString(",")}"), 0, 1500)

  // print job cache detail
  def watchJobStatusCache(implicit trkMonitor: K8sFlinkTrkMonitor): Unit =
    new Timer().scheduleAtFixedRate(() => println(s"[flink-k8s][status]-${System.currentTimeMillis} =>" +
      s"count=${trkMonitor.getAllJobStatus.size} | " +
      s" ${trkMonitor.getAllJobStatus.mkString(", ")}"), 0, 1500)


  // print trkId cache detail
  def watchTrkIdsCache(implicit trkMonitor: K8sFlinkTrkMonitor): Unit = {
    new Timer().scheduleAtFixedRate(() => println(s"[flink-k8s][trkIds]-${System.currentTimeMillis} => " +
      s"${trkMonitor.getAllTrackingIds.mkString(",")}"), 0, 1500)
  }

  // print trkid cache size info
  def watchTrkIdsCacheSize(implicit trkMonitor: K8sFlinkTrkMonitor): Unit = {
    new Timer().scheduleAtFixedRate(() => println(s"[flink-k8s][trkIds-size]-${System.currentTimeMillis} => " +
      s"${trkMonitor.getAllTrackingIds.size}"), 0, 1500)
  }

  // print k8s event cache detail
  def watchK8sEventCache(implicit trkMonitor: K8sFlinkTrkMonitor): Unit = {
    new Timer().scheduleAtFixedRate(() => println(s"[flink-k8s][k8s-event]-${System.currentTimeMillis} => " +
      s"count=${trkMonitor.asInstanceOf[DefaultK8sFlinkTrkMonitor].trkCache.k8sDeploymentEvents.asMap().size} | " +
      s"${trkMonitor.asInstanceOf[DefaultK8sFlinkTrkMonitor].trkCache.k8sDeploymentEvents.asMap().asScala.mkString(",")}"), 0, 1500)
  }
  // scalastyle:on println

}
