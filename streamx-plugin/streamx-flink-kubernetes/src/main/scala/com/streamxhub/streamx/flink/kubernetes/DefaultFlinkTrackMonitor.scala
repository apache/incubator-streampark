/*
 * Copyright (c) 2021 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.streamxhub.streamx.flink.kubernetes

import com.streamxhub.streamx.flink.kubernetes.model.{TrackId, TrackIdCV}
import com.streamxhub.streamx.flink.kubernetes.watcher.{FlinkJobStatusWatcher, FlinkK8sEventWatcher, FlinkMetricWatcher, FlinkWatcher}

import scala.collection.JavaConverters._
import scala.util.Try

/**
 * author:Al-assad
 */
class DefaultFlinkTrackMonitor(conf: FlinkTrackConf = FlinkTrackConf.default) extends FlinkTrackMonitor {

  // cache pool for storage tracking result
  val trkCache = new FlinkTrackCachePool()

  // remote server tracking watcher
  val k8sEventWatcher = new FlinkK8sEventWatcher(trkCache)
  val jobStatusWatcher = new FlinkJobStatusWatcher(trkCache, conf.jobStatusWatcherConf)
  val metricsWatcher = new FlinkMetricWatcher(trkCache, conf.metricWatcherConf)

  private[this] val allWatchers = Array[FlinkWatcher](k8sEventWatcher, jobStatusWatcher, metricsWatcher)


  override def start(): Unit = allWatchers.foreach(_.start())

  override def stop(): Unit = allWatchers.foreach(_.stop())

  override def restart(): Unit = allWatchers.foreach(_.restart())

  override def close(): Unit = {
    allWatchers.foreach(_.close)
    trkCache.close()
  }


  def trackingJob(trackId: TrackId): Unit = {
    if (trackId == null || !trackId.isLegal) {
      return
    }
    trkCache.trackIds.put(trackId, TrackIdCV(System.currentTimeMillis()))
  }

  def trackingJob(trackIds: Set[TrackId]): Unit = {
    if (Try(trackIds.isEmpty).getOrElse(true)) {
      return
    }
    val now = System.currentTimeMillis()
    val trackingMap = trackIds.map(e => (e, TrackIdCV(now))).toMap
    trkCache.trackIds.putAll(trackingMap.asJava)
  }

  def unTrackingJob(trkId: TrackId): Unit = {
    if (trkId == null || !trkId.isLegal) {
      return
    }
    trkCache.trackIds.invalidate(trkId)
  }

  def unTrackingJob(trackIds: Set[TrackId]): Unit = {
    if (Try(trackIds.isEmpty).getOrElse(true)) {
      return
    }
    trkCache.trackIds.invalidateAll(trackIds.asJava)
  }

  override def isInTracking(trkId: TrackId): Boolean = trkCache.isInTracking(trkId)


}

