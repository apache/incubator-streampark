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

import com.streamxhub.streamx.flink.kubernetes.helper.TrackMonitorDebugHelper._
import com.streamxhub.streamx.flink.kubernetes.model.TrackId
import org.junit.jupiter.api.{BeforeEach, Test}

import scala.language.implicitConversions
import scala.util.Try

class K8SFlinkTrackMonitorLazyStartTest {

  implicit var trackMonitor: K8sFlinkTrackMonitor = _

  private val trackIds = Array(
    TrackId.onSession("default", "flink-session", 0L, "7ff03ff5d0b3c66d65a7b4f3ad6ca2a2"),
    TrackId.onApplication("default", "flink-app2", 0L),
    TrackId.onApplication("default", "flink-app4", 0L))


  @BeforeEach
  private def init(): Unit = {
    if (trackMonitor != null) Try(trackMonitor.close())
    trackMonitor = K8sFlinkTrackMonitorFactory.createInstance(FlinkTrackConfig.debugConf, lazyStart = true)
  }

  // test lazy start
  @Test def testMonitorLazyStart(): Unit = {
    watchTrackIdsCacheSize
    watchJobStatusCacheSize
    watchAggClusterMetricsCache
    watchK8sEventCache
    trackIds.foreach(trackMonitor.trackingJob)
    Thread.sleep(30 * 1000)
    trackMonitor.trackingJob(TrackId.onApplication("default", "flink-app3", 0L))
    Thread.sleep(20 * 1000)
    trackMonitor.unTrackingJob(TrackId.onApplication("default", "flink-app3", 0L))
    while (true) {}
  }


}
