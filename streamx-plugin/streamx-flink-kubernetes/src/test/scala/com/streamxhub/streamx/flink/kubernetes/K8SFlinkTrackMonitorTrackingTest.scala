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
import com.streamxhub.streamx.flink.kubernetes.helper.TrackMonitorDebugHelper.{watchAggClusterMetricsCache, watchJobStatusCache, watchJobStatusCacheSize, watchTrackIdsCache}
import com.streamxhub.streamx.flink.kubernetes.model.TrackId
import org.junit.jupiter.api.Assertions.{assertFalse, assertTrue}
import org.junit.jupiter.api.{BeforeEach, Test}
import org.scalatest.Assertions.assertThrows

import java.util.concurrent.RejectedExecutionException
import scala.language.implicitConversions
import scala.util.Try

/**
 * test FlinkTrackMonitor tracking feature
 */

// scalastyle:off println
class K8SFlinkTrackMonitorTrackingTest {

  implicit var trackMonitor: DefaultK8sFlinkTrackMonitor = _

  private val trackIds = Array(
    TrackId.onSession("default", "flink-session", 0L, "7ff03ff5d0b3c66d65a7b4f3ad6ca2a2"),
    TrackId.onApplication("default", "flink-app2", 0L),
    TrackId.onApplication("default", "flink-app3", 0L),
    TrackId.onApplication("default", "flink-app4", 0L))


  @BeforeEach
  private def init(): Unit = {
    if (trackMonitor != null) Try(trackMonitor.close())
    trackMonitor = K8sFlinkTrackMonitorFactory.createInstance(FlinkTrackConfig.debugConf).asInstanceOf[DefaultK8sFlinkTrackMonitor]
  }


  @Test def testMonitor(): Unit = {
    watchJobStatusCache
    watchAggClusterMetricsCache
    watchTrackIdsCache
    trackMonitor.start()
    trackIds.foreach(trackMonitor.trackingJob)
    while (true) {}
  }

  // test start, stop, close action
  @Test def testMonitorStartStop(): Unit = {
    watchAggClusterMetricsCache
    watchJobStatusCacheSize

    trackMonitor.start()
    trackIds.foreach(trackMonitor.trackingJob)
    Thread.sleep(20 * 1000)
    trackMonitor.stop()
    Thread.sleep(10 * 1000)
    trackMonitor.start()
    Thread.sleep(20 * 1000)
    trackMonitor.close()
    assertThrows(classOf[RejectedExecutionException])
  }

  // test tracking, untracking action
  @Test def testTrackingAndUnTracking(): Unit = {
    watchJobStatusCacheSize
    watchAggClusterMetricsCache
    watchTrackIdsCache

    val trackId = TrackId.onApplication("default", "flink-app3", 0L)
    trackMonitor.start()
    trackMonitor.trackingJob(trackId)
    assertTrue(trackMonitor.isInTracking(trackId))
    Thread.sleep(10 * 1000)

    trackMonitor.unTrackingJob(trackId)
    assertFalse(trackMonitor.isInTracking(trackId))
    Thread.sleep(10 * 1000)
  }

  // test change event subscribe
  @Test def testStatusEventsSubscribe(): Unit = {
    //    watchK8sEventCache
    watchTrackIdsCache
    watchJobStatusCache
    trackMonitor.registerListener(new ChangeEventListener())
    trackMonitor.start()
    trackIds.foreach(trackMonitor.trackingJob)
    while (true) {}

  }

  class ChangeEventListener {
    @Subscribe def catchJobStatusEvent(event: FlinkJobStatusChangeEvent): Unit = {
      println(s"[catch-jobStatus]-${System.currentTimeMillis()}-${event}")
    }
  }

  // test checkIsInRemoteCluster
  @Test def testCheckIsInRemoteCluster(): Unit = {
    val check = (trackId: TrackId) => println(s"[check-is-in-remote-cluster] trackId=$trackId, isExists=${trackMonitor.checkIsInRemoteCluster(trackId)}")
    check(TrackId.onSession("default", "flink-session", 0L, "7ff03ff5d0b3c66d65a7b4f3ad6ca2a4"))
    check(TrackId.onSession("default", "flink-session", 0L, "7ff03ff5d0b3c66d65a7b4f3ad6ca2a2"))
    check(TrackId.onApplication("default", "flink-app3", 0L))
    check(TrackId.onApplication("default", "flink-app4", 0L))
  }


}
