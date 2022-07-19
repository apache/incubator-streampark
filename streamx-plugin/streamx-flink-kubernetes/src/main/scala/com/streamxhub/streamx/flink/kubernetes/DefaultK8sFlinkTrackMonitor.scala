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

package com.streamxhub.streamx.flink.kubernetes

import com.google.common.eventbus.Subscribe
import com.streamxhub.streamx.flink.kubernetes.enums.FlinkJobState
import com.streamxhub.streamx.flink.kubernetes.enums.FlinkK8sExecuteMode.{APPLICATION, SESSION}
import com.streamxhub.streamx.flink.kubernetes.event.{BuildInEvent, FlinkJobStateEvent, FlinkJobStatusChangeEvent}
import com.streamxhub.streamx.flink.kubernetes.model._
import com.streamxhub.streamx.flink.kubernetes.watcher.{FlinkCheckpointWatcher, FlinkJobStatusWatcher, FlinkK8sEventWatcher, FlinkMetricWatcher, FlinkWatcher}

import javax.annotation.Nullable
import scala.collection.JavaConverters._
import scala.util.Try

/**
 * Default K8sFlinkTrackMonitor implementation.
 *
 * author:Al-assad
 */
class DefaultK8sFlinkTrackMonitor(conf: FlinkTrackConfig = FlinkTrackConfig.defaultConf) extends K8sFlinkTrackMonitor {

  // cache pool for storage tracking result
  implicit val trackCache: FlinkTrackCachePool = new FlinkTrackCachePool()

  // eventBus for change event
  implicit val eventBus: ChangeEventBus = new ChangeEventBus()

  // remote server tracking watcher
  val k8sEventWatcher = new FlinkK8sEventWatcher()
  val jobStatusWatcher = new FlinkJobStatusWatcher(conf.jobStatusWatcherConf)
  val metricsWatcher = new FlinkMetricWatcher(conf.metricWatcherConf)
  val checkpointWatcher = new FlinkCheckpointWatcher(conf.metricWatcherConf)

  private[this] val allWatchers = Array[FlinkWatcher](k8sEventWatcher, jobStatusWatcher, metricsWatcher, checkpointWatcher)

  {
    // register build-in event listener
    eventBus.registerListener(new BuildInEventListener)
  }

  override def registerListener(listener: AnyRef): Unit = eventBus.registerListener(listener)

  override def start(): Unit = allWatchers.foreach(_.start())

  override def stop(): Unit = allWatchers.foreach(_.stop())

  override def restart(): Unit = allWatchers.foreach(_.restart())

  override def close(): Unit = {
    allWatchers.foreach(_.close)
    trackCache.close()
  }

  def trackingJob(trackId: TrackId): Unit = {
    if (!Try(trackId.nonLegal).getOrElse(true)) {
      trackCache.trackIds.set(trackId)
    }
  }

  def unTrackingJob(trackId: TrackId): Unit = {
    if (!Try(trackId.nonLegal).getOrElse(true)) {
      trackCache.trackIds.invalidate(trackId)
      trackCache.jobStatuses.invalidate(trackId)
      trackCache.flinkMetrics.invalidate(ClusterKey.of(trackId))
    }
  }

  override def isInTracking(trackId: TrackId): Boolean = trackCache.isInTracking(trackId)

  override def getJobStatus(trackId: TrackId): Option[JobStatusCV] = Option(trackCache.jobStatuses.getIfPresent(trackId))

  override def getJobStatus(trackIds: Set[TrackId]): Map[TrackId, JobStatusCV] = trackCache.jobStatuses.getAllPresent(trackIds.asJava).asScala.toMap

  override def getAllJobStatus: Map[TrackId, JobStatusCV] = Map(trackCache.jobStatuses.asMap().asScala.toSeq: _*)

  override def getAccClusterMetrics: FlinkMetricCV = trackCache.collectAccMetric()

  override def getClusterMetrics(clusterKey: ClusterKey): Option[FlinkMetricCV] = Option(trackCache.flinkMetrics.getIfPresent(clusterKey))

  override def getAllTrackingIds: Set[TrackId] = trackCache.collectAllTrackIds()

  override def checkIsInRemoteCluster(trackId: TrackId): Boolean = {
    if (Try(trackId.nonLegal).getOrElse(true)) false; else {
      val nonLost = (state: FlinkJobState.Value) => state != FlinkJobState.LOST || state != FlinkJobState.SILENT
      trackId.executeMode match {
        case SESSION =>
          jobStatusWatcher
            .touchSessionJob(trackId.clusterId, trackId.namespace, trackId.appId, trackId.jobId)
            .exists(e => nonLost(e._2.jobState))
        case APPLICATION =>
          jobStatusWatcher
            .touchApplicationJob(trackId.clusterId, trackId.namespace, trackId.appId)
            .exists(e => nonLost(e._2.jobState))
        case _ => false
      }
    }
  }

  override def postEvent(event: BuildInEvent, sync: Boolean): Unit = {
    if (sync) {
      eventBus.postSync(event)
    } else {
      eventBus.postAsync(event)
    }
  }

  @Nullable override def getRemoteRestUrl(trackId: TrackId): String = trackCache.clusterRestUrls.getIfPresent(ClusterKey.of(trackId))


  /**
   * Build-in Event Listener of K8sFlinkTrackMonitor.
   */
  class BuildInEventListener {

    /**
     * Watch the FlinkJobOperaEvent, then update relevant cache record and
     * trigger a new FlinkJobStatusChangeEvent.
     */
    // noinspection UnstableApiUsage
    @Subscribe def catchFlinkJobStateEvent(event: FlinkJobStateEvent): Unit = {
      if (!Try(event.trackId.nonLegal).getOrElse(true)) {
        val preCache = trackCache.jobStatuses.getIfPresent(event.trackId)
        // determine if the current event should be ignored
        val shouldIgnore: Boolean = (preCache, event) match {
          case (preCache, _) if preCache == null => false
          // discard current event when the job state is consistent
          case (preCache, event) if preCache.jobState == event.jobState => true
          // discard current event when current event is too late
          case (preCache, event) if event.pollTime <= preCache.pollAckTime => true
          case _ => false
        }
        if (!shouldIgnore) {
          // update relevant cache
          val newCache = {
            if (preCache != null) {
              preCache.copy(jobState = event.jobState)
            } else {
              JobStatusCV(
                jobState = event.jobState,
                jobId = event.trackId.jobId,
                pollEmitTime = event.pollTime,
                pollAckTime = System.currentTimeMillis)
            }
          }
          trackCache.jobStatuses.put(event.trackId, newCache)
          // post new FlinkJobStatusChangeEvent
          eventBus.postAsync(FlinkJobStatusChangeEvent(event.trackId, newCache))
        }
      }
    }

  }
}

