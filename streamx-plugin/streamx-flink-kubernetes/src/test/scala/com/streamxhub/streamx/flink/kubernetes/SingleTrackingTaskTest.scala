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

import com.streamxhub.streamx.flink.kubernetes.enums.FlinkK8sExecuteMode.{APPLICATION, SESSION}
import com.streamxhub.streamx.flink.kubernetes.model.TrackId
import org.junit.jupiter.api.Test

/**
 * test single tracking task
 */
// scalastyle:off println
class SingleTrackingTaskTest {

  val trackMonitor: DefaultK8sFlinkTrackMonitor = K8sFlinkTrackMonitorFactory.createInstance().asInstanceOf[DefaultK8sFlinkTrackMonitor]

  // retrieve flink job status  info
  @Test def testSingleTrackingFlinkJobStatus(): Unit = {
    // test session job
    val jobStatus1 = trackMonitor.jobStatusWatcher.touchSessionAllJob("flink-session", "default", 0L)
    println("result1 = ")
    jobStatus1.foreach(println)

    // test application job
    val jobStatus2 = trackMonitor.jobStatusWatcher.touchApplicationJob(TrackId.onApplication("flink-app2", "default", 0L, null))
    println(s"result2 = ${jobStatus2.map(_.toString).getOrElse("empty result")}")
    val jobStatus3 = trackMonitor.jobStatusWatcher.touchApplicationJob(TrackId.onApplication("flink-app3", "default", 0L, null))
    println(s"result3 = ${jobStatus3.map(_.toString).getOrElse("empty result")}")
  }

  // retrieve flink metric
  @Test def testSingleTrackingFlinkMetrics(): Unit = {
    // session
    var metrics = trackMonitor.metricsWatcher.collectMetrics(TrackId(SESSION, "flink-session", "default", 0L, "60dd003f048a5b2f92f0874e6612146c"))
    println(s"result1 = ${metrics.map(_.toString).getOrElse("empty result")}")
    // application
    metrics = trackMonitor.metricsWatcher.collectMetrics(TrackId(APPLICATION, "flink-app2", "default", 0L, "60dd003f048a5b2f92f0874e6612146c"))
    println(s"result2 = ${metrics.map(_.toString).getOrElse("empty result")}")
    metrics = trackMonitor.metricsWatcher.collectMetrics(TrackId(APPLICATION, "flink-app3", "default", 0L, "60dd003f048a5b2f92f0874e6612146c"))
    println(s"result3 = ${metrics.map(_.toString).getOrElse("empty result")}")
  }


}
