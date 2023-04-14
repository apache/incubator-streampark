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

import org.apache.streampark.flink.kubernetes.event.BuildInEvent
import org.apache.streampark.flink.kubernetes.model.{ClusterKey, FlinkMetricCV, JobStatusCV, TrackId}

/**
 * AOP for FlinkK8sWatcher used to trigger the run behavior. What more, this AOP has the ability to
 * automatically recover the FlinkK8sWatcher's internal FlinkWatcher.
 */
trait FlinkK8sWatcherLazyStartAop extends FlinkK8sWatcher {

  abstract override def doWatching(trackId: TrackId): Unit = {
    start()
    super.doWatching(trackId)
  }

  abstract override def unWatching(trackId: TrackId): Unit = {
    start()
    super.unWatching(trackId)
  }

  abstract override def isInWatching(trackId: TrackId): Boolean = {
    start()
    super.isInWatching(trackId)
  }

  abstract override def getAllWatchingIds: Set[TrackId] = {
    start()
    super.getAllWatchingIds
  }

  abstract override def getJobStatus(trackId: TrackId): Option[JobStatusCV] = {
    start()
    super.getJobStatus(trackId)
  }

  abstract override def getJobStatus(trackIds: Set[TrackId]): Map[CacheKey, JobStatusCV] = {
    start()
    super.getJobStatus(trackIds)
  }

  abstract override def getAllJobStatus: Map[CacheKey, JobStatusCV] = {
    start()
    super.getAllJobStatus
  }

  abstract override def getAccGroupMetrics(groupId: String): FlinkMetricCV = {
    // behavior of getting cluster metrics will not trigger a delayed start
    super.getAccGroupMetrics(groupId)
  }

  abstract override def getClusterMetrics(clusterKey: ClusterKey): Option[FlinkMetricCV] = {
    // behavior of getting cluster metrics will not trigger a delayed start
    super.getClusterMetrics(clusterKey)
  }

  abstract override def checkIsInRemoteCluster(trackId: TrackId): Boolean = {
    start()
    super.checkIsInRemoteCluster(trackId)
  }

  abstract override def postEvent(event: BuildInEvent, sync: Boolean): Unit = {
    start()
    super.postEvent(event, sync)
  }

}
