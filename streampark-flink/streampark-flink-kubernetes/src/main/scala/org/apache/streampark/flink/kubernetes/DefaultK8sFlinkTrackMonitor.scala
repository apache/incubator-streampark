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

package org.apache.streampark.flink.kubernetes

import com.google.common.eventbus.Subscribe
import org.apache.streampark.flink.kubernetes.enums.FlinkJobState
import org.apache.streampark.flink.kubernetes.enums.FlinkK8sExecuteMode.{APPLICATION, SESSION}
import org.apache.streampark.flink.kubernetes.event.{BuildInEvent, FlinkJobStateEvent, FlinkJobStatusChangeEvent}
import org.apache.streampark.flink.kubernetes.model._
import org.apache.streampark.flink.kubernetes.watcher.{FlinkCheckpointWatcher, FlinkJobStatusWatcher, FlinkK8sEventWatcher, FlinkMetricWatcher, FlinkWatcher}

import javax.annotation.Nullable

/**
 * Default K8sFlinkTrackMonitor implementation.
 *
 */
class DefaultK8sFlinkTrackMonitor(conf: FlinkTrackConfig = FlinkTrackConfig.defaultConf) extends K8sFlinkTrackMonitor {

  // cache pool for storage tracking result
  implicit val trackController: FlinkTrackController = new FlinkTrackController()

  // eventBus for change event
  implicit lazy val eventBus: ChangeEventBus = {
    val eventBus = new ChangeEventBus()
    eventBus.registerListener(new BuildInEventListener)
    eventBus
  }

  // remote server tracking watcher
  val k8sEventWatcher = new FlinkK8sEventWatcher()
  val jobStatusWatcher = new FlinkJobStatusWatcher(conf.jobStatusWatcherConf)
  val metricsWatcher = new FlinkMetricWatcher(conf.metricWatcherConf)
  val checkpointWatcher = new FlinkCheckpointWatcher(conf.metricWatcherConf)

  private[this] val allWatchers = Array[FlinkWatcher](k8sEventWatcher, jobStatusWatcher, metricsWatcher, checkpointWatcher)

  override def registerListener(listener: AnyRef): Unit = eventBus.registerListener(listener)

  override def start(): Unit = allWatchers.foreach(_.start())

  override def stop(): Unit = allWatchers.foreach(_.stop())

  override def restart(): Unit = allWatchers.foreach(_.restart())

  override def close(): Unit = {
    allWatchers.foreach(_.close)
    trackController.close()
  }

  def trackingJob(trackId: TrackId): Unit = {
    if (trackId.isLegal) {
      trackController.trackIds.set(trackId)
    }
  }

  def unTrackingJob(trackId: TrackId): Unit = {
    trackController.canceling.set(trackId)
  }

  override def isInTracking(trackId: TrackId): Boolean = trackController.isInTracking(trackId)

  override def getJobStatus(trackId: TrackId): Option[JobStatusCV] = Option(trackController.jobStatuses.get(trackId))

  override def getJobStatus(trackIds: Set[TrackId]): Map[CacheKey, JobStatusCV] = trackController.jobStatuses.getAsMap(trackIds)

  override def getAllJobStatus: Map[CacheKey, JobStatusCV] = trackController.jobStatuses.asMap()

  override def getAccClusterMetrics: FlinkMetricCV = trackController.collectAccMetric()

  override def getClusterMetrics(clusterKey: ClusterKey): Option[FlinkMetricCV] = Option(trackController.flinkMetrics.get(clusterKey))

  override def getAllTrackingIds: Set[TrackId] = trackController.collectAllTrackIds()

  override def checkIsInRemoteCluster(trackId: TrackId): Boolean = {
    if (!trackId.isLegal) false; else {
      val nonLost = (state: FlinkJobState.Value) => state != FlinkJobState.LOST || state != FlinkJobState.SILENT
      trackId.executeMode match {
        case SESSION =>
          jobStatusWatcher.touchSessionJob(trackId).exists(e => nonLost(e.jobState))
        case APPLICATION =>
          jobStatusWatcher.touchApplicationJob(trackId).exists(e => nonLost(e.jobState))
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

  @Nullable override def getRemoteRestUrl(trackId: TrackId): String = trackController.endpoints.get(trackId.toClusterKey)

  /**
   * Build-in Event Listener of K8sFlinkTrackMonitor.
   */
  class BuildInEventListener {

    /**
     * Watch the FlinkJobOperaEvent, then update relevant cache record and
     * trigger a new FlinkJobStatusChangeEvent.
     */
    // noinspection UnstableApiUsage
    @Subscribe def subscribeFlinkJobStateEvent(event: FlinkJobStateEvent): Unit = {
      if (event.trackId.isLegal) {
        val latest = trackController.jobStatuses.get(event.trackId)
        // determine if the current event should be ignored
        val shouldIgnore: Boolean = (latest, event) match {
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
            if (latest != null) {
              latest.copy(jobState = event.jobState)
            } else {
              JobStatusCV(
                jobState = event.jobState,
                jobId = event.trackId.jobId,
                pollEmitTime = event.pollTime,
                pollAckTime = System.currentTimeMillis)
            }
          }
          trackController.jobStatuses.put(event.trackId, newCache)
          // post new FlinkJobStatusChangeEvent
          eventBus.postAsync(FlinkJobStatusChangeEvent(event.trackId, newCache))
        }
      }
    }

  }
}

