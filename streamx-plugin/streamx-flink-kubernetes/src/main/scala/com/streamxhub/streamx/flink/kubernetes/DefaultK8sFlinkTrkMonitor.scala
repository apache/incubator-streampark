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
import com.streamxhub.streamx.flink.kubernetes.event.{BuildInEvent, FlinkJobOperaEvent, FlinkJobStatusChangeEvent}
import com.streamxhub.streamx.flink.kubernetes.model._
import com.streamxhub.streamx.flink.kubernetes.watcher.{FlinkJobStatusWatcher, FlinkK8sEventWatcher, FlinkMetricWatcher, FlinkWatcher}

import javax.annotation.Nullable
import scala.collection.JavaConverters._
import scala.util.Try

/**
 * Default K8sFlinkTrkMonitor implementation.
 *
 * author:Al-assad
 */
class DefaultK8sFlinkTrkMonitor(conf: FlinkTrkConf = FlinkTrkConf.defaultConf) extends K8sFlinkTrkMonitor {

  // cache pool for storage tracking result
  implicit val trkCache: FlinkTrkCachePool = new FlinkTrkCachePool()

  // eventBus for change event
  implicit val eventBus: ChangeEventBus = new ChangeEventBus()

  // remote server tracking watcher
  val k8sEventWatcher = new FlinkK8sEventWatcher()
  val jobStatusWatcher = new FlinkJobStatusWatcher(conf.jobStatusWatcherConf)
  val metricsWatcher = new FlinkMetricWatcher(conf.metricWatcherConf)

  private[this] val allWatchers = Array[FlinkWatcher](k8sEventWatcher, jobStatusWatcher, metricsWatcher)

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
    trkCache.close()
  }

  def trackingJob(trkId: TrkId): Unit = {
    if (!Try(trkId.nonLegal).getOrElse(true)) {
      trkCache.trackIds.put(trkId, TrkIdCV(System.currentTimeMillis()))
    }
  }

  def unTrackingJob(trkId: TrkId): Unit = {
    if (!Try(trkId.nonLegal).getOrElse(true)) {
      trkCache.trackIds.invalidate(trkId)
      trkCache.jobStatuses.invalidate(trkId)
      trkCache.flinkMetrics.invalidate(ClusterKey.of(trkId))
    }
  }

  override def isInTracking(trkId: TrkId): Boolean = trkCache.isInTracking(trkId)

  override def getJobStatus(trkId: TrkId): Option[JobStatusCV] = Option(trkCache.jobStatuses.getIfPresent(trkId))

  override def getJobStatus(trkIds: Set[TrkId]): Map[TrkId, JobStatusCV] = trkCache.jobStatuses.getAllPresent(trkIds.asJava).asScala.toMap

  override def getAllJobStatus: Map[TrkId, JobStatusCV] = Map(trkCache.jobStatuses.asMap().asScala.toSeq: _*)

  override def getAccClusterMetrics: FlinkMetricCV = trkCache.collectAccMetric()

  override def getClusterMetrics(clusterKey: ClusterKey): Option[FlinkMetricCV] = Option(trkCache.flinkMetrics.getIfPresent(clusterKey))

  override def getAllTrackingIds: Set[TrkId] = trkCache.collectAllTrackIds()

  override def checkIsInRemoteCluster(trkId: TrkId): Boolean = {
    if (Try(trkId.nonLegal).getOrElse(true)) false; else {
      val nonLost = (state: FlinkJobState.Value) => state != FlinkJobState.LOST || state != FlinkJobState.SILENT
      trkId.executeMode match {
        case SESSION => jobStatusWatcher.touchSessionJob(trkId.clusterId, trkId.namespace, Set(trkId.jobId))
          .exists(e => nonLost(e._2.jobState))
        case APPLICATION => jobStatusWatcher.touchApplicationJob(trkId.clusterId, trkId.namespace)
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

  @Nullable override def getRemoteRestUrl(trkId: TrkId): String = trkCache.clusterRestUrls.getIfPresent(ClusterKey.of(trkId))


  /**
   * Build-in Event Listener of K8sFlinkTrkMonitor.
   */
  class BuildInEventListener {

    /**
     * Watch the FlinkJobOperaEvent, then update relevant cache record and
     * trigger a new FlinkJobStatusChangeEvent.
     */
    // noinspection UnstableApiUsage
    @Subscribe def catchFlinkJobOperaEvent(event: FlinkJobOperaEvent): Unit = {
      if (!Try(event.trkId.nonLegal).getOrElse(true)) {

        val preCache = trkCache.jobStatuses.getIfPresent(event.trkId)

        // determine if the current event should be ignored
        val shouldIgnore: Boolean = (preCache, event.expectJobState) match {
          case (preCache, _) if preCache == null => false
          // discard current event when the job state is consistent
          case (preCache, expectJobState) if preCache.jobState == expectJobState.expect => true
          // discard current event when current event is too late
          case (preCache, expectJobState) if expectJobState.pollTime <= preCache.pollAckTime => true
          case _ => false
        }
        if (!shouldIgnore) {
          // update relevant cache
          val newCache = {
            if (preCache != null) {
              preCache.copy(jobState = event.expectJobState.expect)
            } else {
              JobStatusCV(
                jobState = event.expectJobState.expect,
                jobId = event.trkId.jobId,
                pollEmitTime = event.expectJobState.pollTime,
                pollAckTime = System.currentTimeMillis)
            }
          }
          trkCache.jobStatuses.put(event.trkId, newCache)
          // post new FlinkJobStatusChangeEvent
          eventBus.postAsync(FlinkJobStatusChangeEvent(event.trkId, newCache))
        }
      }
    }

  }
}

