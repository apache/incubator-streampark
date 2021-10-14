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

import com.streamxhub.streamx.flink.kubernetes.helper.TrkMonitorDebugHelper._
import com.streamxhub.streamx.flink.kubernetes.model.TrkId
import org.junit.jupiter.api.{BeforeEach, Test}

import scala.language.implicitConversions
import scala.util.Try

class K8sFlinkTrkMonitorLazyStartTest {

  implicit var trkMonitor: K8sFlinkTrkMonitor = _

  private val trkIds = Array(
    TrkId.onSession("default", "flink-session", "2333"),
    TrkId.onApplication("default", "flink-app2"),
    TrkId.onApplication("default", "flink-app4"))


  @BeforeEach
  private def init(): Unit = {
    if (trkMonitor != null) Try(trkMonitor.close())
    trkMonitor = K8sFlinkTrkMonitorFactory.createInstance(FlinkTrkConf.debugConf, lazyStart = true)
  }

  // test lazy start
  @Test def testMonitorLazyStart(): Unit = {
    watchTrkIdsCacheSize
    watchJobStatusCacheSize
    watchAggClusterMetricsCache
    watchK8sEventCache
    println("[trk-monitor] call trackingJob")
    trkIds.foreach(trkMonitor.trackingJob)
    Thread.sleep(30 * 1000)
    trkMonitor.trackingJob(TrkId.onApplication("default", "flink-app3"))
    Thread.sleep(20 * 1000)
    trkMonitor.unTrackingJob(TrkId.onApplication("default", "flink-app3"))
    while (true) {}
  }


}
