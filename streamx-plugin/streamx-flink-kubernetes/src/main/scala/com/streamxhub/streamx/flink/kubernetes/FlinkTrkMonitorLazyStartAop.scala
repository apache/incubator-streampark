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

import com.streamxhub.streamx.flink.kubernetes.model.{FlinkMetricCV, JobStatusCV, TrkId}

/**
 * AOP for FlinkTrkMonitor used to trigger the run behavior.
 * What more, this AOP has the ability to automatically recover
 * the FlinkTrkMonitor's internal FlinkWatcher.
 *
 * author:Al-assad
 */
trait FlinkTrkMonitorLazyStartAop extends FlinkTrkMonitor {

  abstract override def trackingJob(trkId: TrkId): Unit = {
    start()
    super.trackingJob(trkId)
  }

  abstract override def trackingJob(trkIds: Set[TrkId]): Unit = {
    start()
    super.trackingJob(trkIds)
  }

  abstract override def unTrackingJob(trkId: TrkId): Unit = {
    start()
    super.unTrackingJob(trkId)
  }

  abstract override def unTrackingJob(trkIds: Set[TrkId]): Unit = {
    start()
    super.unTrackingJob(trkIds)
  }

  abstract override def isInTracking(trkId: TrkId): Boolean = {
    start()
    super.isInTracking(trkId)
  }

  abstract override def getAllTrackingIds: Set[TrkId] = {
    start()
    super.getAllTrackingIds
  }

  abstract override def getJobStatus(trkId: TrkId): Option[JobStatusCV] = {
    start()
    super.getJobStatus(trkId)
  }

  abstract override def getJobStatus(trkIds: Set[TrkId]): Map[TrkId, JobStatusCV] = {
    start()
    super.getJobStatus(trkIds)
  }

  abstract override def getAllJobStatus: Map[TrkId, JobStatusCV] = {
    start()
    super.getAllJobStatus
  }

  abstract override def getClusterMetrics: Option[FlinkMetricCV] = {
    start()
    super.getClusterMetrics
  }

  abstract override def checkIsInRemoteCluster(trkId: TrkId): Boolean = {
    start()
    super.checkIsInRemoteCluster(trkId)
  }

}



