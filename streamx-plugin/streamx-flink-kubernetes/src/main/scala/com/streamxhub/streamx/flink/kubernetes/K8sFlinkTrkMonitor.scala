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

import com.streamxhub.streamx.common.util.Logger
import com.streamxhub.streamx.flink.kubernetes.event.BuildInEvent
import com.streamxhub.streamx.flink.kubernetes.model.{ClusterKey, FlinkMetricCV, JobStatusCV, TrkId}
import org.apache.flink.annotation.Public

import javax.annotation.Nullable

/**
 * Tracking monitor for flink-k8s-native mode, including
 * trace of flink jobs status information, flink metrics
 * information.
 * This is the entry point for external calls to the
 * streamx.flink.kubernetes package.
 *
 * author:Al-assad
 */
@Public
trait K8sFlinkTrkMonitor extends Logger with AutoCloseable {


  /**
   * Register listener to EventBus.
   *
   * At present, the implementation of listener is in the
   * same form as Guava EvenBus Listener. The events that
   * can be subcribed are included in
   * com.streamxhub.streamx.flink.kubernetes.event
   */
  def registerListener(listener: AnyRef): Unit

  /**
   * start monitor tracking activities immediately.
   */
  def start(): Unit

  /**
   * stop monitor tracking activities immediately.
   */
  def stop(): Unit

  /**
   * restart monitor tracking activities immediately.
   */
  def restart(): Unit

  /**
   * add tracking for the specified flink job which on k8s cluster.
   *
   * @param trkId identifier of flink job
   */
  def trackingJob(trkId: TrkId): Unit

  /**
   * remove tracking for the specified flink job which on k8s cluster.
   *
   * @param trkId identifier of flink job
   */
  def unTrackingJob(trkId: TrkId): Unit

  /**
   * check whether the specified flink job is in tracking.
   *
   * @param trkId identifier of flink job
   */
  def isInTracking(trkId: TrkId): Boolean

  /**
   * collect all TrkId which in tracking
   */
  def getAllTrackingIds: Set[TrkId]

  /**
   * get flink status
   */
  def getJobStatus(trkId: TrkId): Option[JobStatusCV]

  /**
   * get flink status
   */
  def getJobStatus(trkIds: Set[TrkId]): Map[TrkId, JobStatusCV]

  /**
   * get all flink status in tracking result pool
   */
  def getAllJobStatus: Map[TrkId, JobStatusCV]

  /**
   * get flink cluster metrics aggregation
   */
  def getAccClusterMetrics: FlinkMetricCV

  /**
   * get flink cluster metrics
   */
  def getClusterMetrics(clusterKey: ClusterKey): Option[FlinkMetricCV]

  /**
   * check whether flink job is in remote kubernetes cluster
   */
  def checkIsInRemoteCluster(trkId: TrkId): Boolean

  /**
   * post event to build-in EventBus of K8sFlinkTrkMonitor
   *
   * @param sync should this event be consumed sync or async
   */
  def postEvent(event: BuildInEvent, sync: Boolean = true): Unit

  /**
   * get flink web rest url of k8s cluster
   */
  @Nullable def getRemoteRestUrl(trkId: TrkId): String

}

/**
 * Factory of FlinkTrkMonitor.
 * This is the entry point for external calls to the
 * streamx.flink.kubernetes package.
 */
@Public
object K8sFlinkTrkMonitorFactory {

  /**
   * Create FlinkTRKMonitor instance.
   *
   * @param conf      onfiguration
   * @param lazyStart Whether monitor will performs delayed auto-start when necessary.
   *                  In this case, there is no need to display the call to FlinkTrkMonitor.start(),
   *                  useless the monitor is expected to start immediately.
   */
  def createInstance(conf: FlinkTrkConf = FlinkTrkConf.defaultConf, lazyStart: Boolean = false): K8sFlinkTrkMonitor =
    if (lazyStart) {
      new DefaultK8sFlinkTrkMonitor(conf) with K8sFlinkTrkMonitorLazyStartAop
    } else {
      new DefaultK8sFlinkTrkMonitor(conf)
    }
}
