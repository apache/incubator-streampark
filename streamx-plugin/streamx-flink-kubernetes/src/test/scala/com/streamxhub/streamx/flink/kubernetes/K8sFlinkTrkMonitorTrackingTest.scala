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

import com.google.common.eventbus.Subscribe
import com.streamxhub.streamx.flink.kubernetes.event.FlinkJobStatusChangeEvent
import com.streamxhub.streamx.flink.kubernetes.helper.TrkMonitorDebugHelper.{watchAggClusterMetricsCache, watchJobStatusCache, watchJobStatusCacheSize, watchTrkIdsCache}
import com.streamxhub.streamx.flink.kubernetes.model.TrkId
import org.junit.jupiter.api.Assertions.{assertFalse, assertTrue}
import org.junit.jupiter.api.{BeforeEach, Test}
import org.scalatest.Assertions.assertThrows

import java.util.concurrent.RejectedExecutionException
import scala.language.implicitConversions
import scala.util.Try

/**
 * test FlinkTrkMonitor tracking feature
 */
class K8sFlinkTrkMonitorTrackingTest {

  implicit var trkMonitor: DefaultK8sFlinkTrkMonitor = _

  private val trkIds = Array(
    TrkId.onSession("default", "flink-session", "2333"),
    TrkId.onApplication("default", "flink-app2"),
    TrkId.onApplication("default", "flink-app3"),
    TrkId.onApplication("default", "flink-app4"))


  @BeforeEach
  private def init(): Unit = {
    if (trkMonitor != null) Try(trkMonitor.close())
    trkMonitor = K8sFlinkTrkMonitorFactory.createInstance(FlinkTrkConf.debugConf).asInstanceOf[DefaultK8sFlinkTrkMonitor]
  }


  @Test def testMonitor(): Unit = {
    watchJobStatusCache
    watchAggClusterMetricsCache
    watchTrkIdsCache
    trkMonitor.start()
    trkIds.foreach(trkMonitor.trackingJob)
    while (true) {}
  }

  // test start, stop, close action
  @Test def testMonitorStartStop(): Unit = {
    watchAggClusterMetricsCache
    watchJobStatusCacheSize

    trkMonitor.start()
    trkIds.foreach(trkMonitor.trackingJob)
    Thread.sleep(20 * 1000)
    trkMonitor.stop()
    Thread.sleep(10 * 1000)
    trkMonitor.start()
    Thread.sleep(20 * 1000)
    trkMonitor.close()
    assertThrows(classOf[RejectedExecutionException])
  }

  // test tracking, untracking action
  @Test def testTrackingAndUnTracking(): Unit = {
    watchJobStatusCacheSize
    watchAggClusterMetricsCache
    watchTrkIdsCache

    val trkId = TrkId.onApplication("default", "flink-app3")
    trkMonitor.start()
    trkMonitor.trackingJob(trkId)
    assertTrue(trkMonitor.isInTracking(trkId))
    Thread.sleep(10 * 1000)

    trkMonitor.unTrackingJob(trkId)
    assertFalse(trkMonitor.isInTracking(trkId))
    Thread.sleep(10 * 1000)
  }

  // test change event subscribe
  @Test def testStatusEventsSubscribe(): Unit = {
    //    watchK8sEventCache
    watchTrkIdsCache
    watchJobStatusCache
    trkMonitor.registerListener(new ChangeEventListener())
    trkMonitor.start()
    trkIds.foreach(trkMonitor.trackingJob)
    while (true) {}

  }

  class ChangeEventListener {
    @Subscribe def catchJobStatusEvent(event: FlinkJobStatusChangeEvent): Unit = {
      println(s"[catch-jobStatus]-${System.currentTimeMillis()}-${event}")
    }
  }

  // test checkIsInRemoteCluster
  @Test def testCheckIsInRemoteCluster(): Unit = {
    val check = (trkId: TrkId) => println(s"[check-is-in-remote-cluster] trkId=$trkId, isExists=${trkMonitor.checkIsInRemoteCluster(trkId)}")
    check(TrkId.onSession("default", "flink-session", "7ff03ff5d0b3c66d65a7b4f3ad6ca2a4"))
    check(TrkId.onSession("default", "flink-session", "7ff03ff5d0b3c66d65a7b4f3ad6ca2a2"))
    check(TrkId.onApplication("default", "flink-app3"))
    check(TrkId.onApplication("default", "flink-app4"))
  }


}
