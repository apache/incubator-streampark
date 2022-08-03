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

import com.streamxhub.streamx.flink.kubernetes.event.BuildInEvent
import com.streamxhub.streamx.flink.kubernetes.model.{ClusterKey, FlinkMetricCV, JobStatusCV, TrackId}

/**
 * AOP for FlinkTrackMonitor used to trigger the run behavior.
 * What more, this AOP has the ability to automatically recover
 * the FlinkTrackMonitor's internal FlinkWatcher.
 *
 * author:Al-assad
 */
trait K8sFlinkTrackMonitorLazyStartAop extends K8sFlinkTrackMonitor {

  abstract override def trackingJob(trackId: TrackId): Unit = {
    start()
    super.trackingJob(trackId)
  }

  abstract override def unTrackingJob(trackId: TrackId): Unit = {
    start()
    super.unTrackingJob(trackId)
  }

  abstract override def isInTracking(trackId: TrackId): Boolean = {
    start()
    super.isInTracking(trackId)
  }

  abstract override def getAllTrackingIds: Set[TrackId] = {
    start()
    super.getAllTrackingIds
  }

  abstract override def getJobStatus(trackId: TrackId): Option[JobStatusCV] = {
    start()
    super.getJobStatus(trackId)
  }

  abstract override def getJobStatus(trackIds: Set[TrackId]): Map[TrackId, JobStatusCV] = {
    start()
    super.getJobStatus(trackIds)
  }

  abstract override def getAllJobStatus: Map[TrackId, JobStatusCV] = {
    start()
    super.getAllJobStatus
  }

  abstract override def getAccClusterMetrics: FlinkMetricCV = {
    // hehavior of getting cluster metrics will not trgger a delayed start
    super.getAccClusterMetrics
  }

  abstract override def getClusterMetrics(clusterKey: ClusterKey): Option[FlinkMetricCV] = {
    // hehavior of getting cluster metrics will not trgger a delayed start
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



