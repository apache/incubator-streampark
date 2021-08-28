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

import com.streamxhub.streamx.common.util.Logger
import com.streamxhub.streamx.flink.kubernetes.model.TrkId

/**
 * Tracking monitor for flink-k8s-native mode, including
 * trace of flink jobs status information, flink metrics
 * information.
 *
 * author:Al-assad
 */
trait FlinkTrkMonitor extends Logger with AutoCloseable {

  /**
   * Create FlinkTRKMonitor instance.
   *
   * @param conf configuration
   */
  def createInstance(conf: FlinkTrackConf = FlinkTrackConf.default): FlinkTrkMonitor = new DefaultFlinkTrkMonitor(conf)

  /**
   * start monitor tracking activities.
   */
  def start()

  /**
   * stop monitor tracking activities.
   */
  def stop()

  /**
   * restart monitor tracking activities.
   */
  def restart()

  /**
   * add tracking for the specified flink job which on k8s cluster.
   *
   * @param trkId identifier of flink job
   */
  def trackingJob(trkId: TrkId)

  /**
   * add tracking for the specified flinks job which on k8s cluster.
   *
   * @param trackIds identifieies of flink job
   */
  def trackingJob(trkIds: Set[TrkId])

  /**
   * remove tracking for the specified flink job which on k8s cluster.
   *
   * @param trkId identifier of flink job
   */
  def unTrackingJob(trkId: TrkId)

  /**
   * remove tracking for the specified flinks job which on k8s cluster.
   *
   * @param trackIds identifieies of flink job
   */
  def unTrackingJob(trkIds: Set[TrkId])

  /**
   * check whether the specified flink job is in tracking.
   *
   * @param trkId identifier of flink job
   */
  def isInTracking(trkId: TrkId): Boolean


}

