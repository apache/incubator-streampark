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

import org.apache.streampark.common.util.Logger
import org.apache.streampark.flink.kubernetes.event.BuildInEvent
import org.apache.streampark.flink.kubernetes.model.{ClusterKey, FlinkMetricCV, JobStatusCV, TrackId}

import org.apache.flink.annotation.Public

import javax.annotation.Nullable

/**
 * Tracking monitor for flink-k8s-native mode, including trace of flink jobs status information,
 * flink metrics information. This is the entry point for external calls to the
 * streampark.flink.kubernetes package.
 */
@Public
trait FlinkK8sWatcher extends Logger with AutoCloseable {

  /**
   * Register listener to EventBus.
   *
   * At present, the implementation of listener is in the same form as Guava EvenBus Listener. The
   * events that can be subscribed are included in org.apache.streampark.flink.kubernetes.event
   */
  def registerListener(listener: AnyRef): Unit

  /** start monitor tracking activities immediately. */
  def start(): Unit

  /** stop monitor tracking activities immediately. */
  def stop(): Unit

  /** restart monitor tracking activities immediately. */
  def restart(): Unit

  /**
   * add tracking for the specified flink job which on k8s cluster.
   *
   * @param trackId
   *   identifier of flink job
   */
  def doWatching(trackId: TrackId): Unit

  /**
   * remove tracking for the specified flink job which on k8s cluster.
   *
   * @param trackId
   *   identifier of flink job
   */
  def unWatching(trackId: TrackId): Unit

  /**
   * check whether the specified flink job is in tracking.
   *
   * @param trackId
   *   identifier of flink job
   */
  def isInWatching(trackId: TrackId): Boolean

  /** collect all TrackId which in tracking */
  def getAllWatchingIds: Set[TrackId]

  /** get flink status */
  def getJobStatus(trackId: TrackId): Option[JobStatusCV]

  /** get flink status */
  def getJobStatus(trackIds: Set[TrackId]): Map[CacheKey, JobStatusCV]

  /** get all flink status in tracking result pool */
  def getAllJobStatus: Map[CacheKey, JobStatusCV]

  /** get flink cluster metrics aggregation */
  def getAccGroupMetrics(groupId: String = null): FlinkMetricCV

  /** get flink cluster metrics */
  def getClusterMetrics(clusterKey: ClusterKey): Option[FlinkMetricCV]

  /** check whether flink job is in remote kubernetes cluster */
  def checkIsInRemoteCluster(trackId: TrackId): Boolean

  /**
   * post event to build-in EventBus of K8sFlinkTrackMonitor
   *
   * @param sync
   *   should this event be consumed sync or async
   */
  def postEvent(event: BuildInEvent, sync: Boolean = true): Unit

  /** get flink web rest url of k8s cluster */
  @Nullable def getRemoteRestUrl(trackId: TrackId): String

}

@Public
object FlinkK8sWatcherFactory {

  /**
   * Create FlinkK8sWatcher instance.
   *
   * @param conf
   *   configuration
   * @param lazyStart
   *   Whether monitor will performs delayed auto-start when necessary. In this case, there is no
   *   need to display the call to FlinkK8sWatcher.start(), useless the monitor is expected to start
   *   immediately.
   */
  def createInstance(
      conf: FlinkTrackConfig = FlinkTrackConfig.defaultConf,
      lazyStart: Boolean = false): FlinkK8sWatcher =
    if (lazyStart) {
      new DefaultFlinkK8sWatcher(conf) with FlinkK8sWatcherLazyStartAop
    } else {
      new DefaultFlinkK8sWatcher(conf)
    }
}
