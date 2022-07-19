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

package com.streamxhub.streamx.common.conf

/**
 * Flink kubernetes Configuration
 *
 * @author Al-assad
 */
object K8sFlinkConfig {

  val jobStatusTrackTaskTimeoutSec: InternalOption = InternalOption(
    key = "streamx.flink-k8s.tracking.polling-task-timeout-sec.job-status",
    defaultValue = 120L,
    classType = classOf[java.lang.Long],
    description = "run timeout seconds of single flink-k8s metrics tracking task")

  val metricTrackTaskTimeoutSec: InternalOption = InternalOption(
    key = "streamx.flink-k8s.tracking.polling-task-timeout-sec.cluster-metric",
    defaultValue = 120L,
    classType = classOf[java.lang.Long],
    description = "run timeout seconds of single flink-k8s job status tracking task")

  val jobStatueTrackTaskIntervalSec: InternalOption = InternalOption(
    key = "streamx.flink-k8s.tracking.polling-interval-sec.job-status",
    defaultValue = 5L,
    classType = classOf[java.lang.Long],
    description = "interval seconds between two single flink-k8s metrics tracking task")

  val metricTrackTaskIntervalSec: InternalOption = InternalOption(
    key = "streamx.flink-k8s.tracking.polling-interval-sec.cluster-metric",
    defaultValue = 5L,
    classType = classOf[java.lang.Long],
    description = "interval seconds between two single flink-k8s metrics tracking task")

  val silentStateJobKeepTrackingSec: InternalOption = InternalOption(
    key = "streamx.flink-k8s.tracking.silent-state-keep-sec",
    defaultValue = 60,
    classType = classOf[java.lang.Integer],
    description = "retained tracking time for SILENT state flink tasks")

  /**
   * kubernetes default namespace
   */
  val DEFAULT_KUBERNETES_NAMESPACE = "default"

}
