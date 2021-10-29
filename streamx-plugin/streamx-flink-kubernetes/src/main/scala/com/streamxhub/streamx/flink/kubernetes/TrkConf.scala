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

/**
 * @author Al-assad
 *
 * @param jobStatusWatcherConf configuration for flink job status tracking process
 * @param metricWatcherConf    configuration for flink metric tracking process
 */
case class FlinkTrkConf(jobStatusWatcherConf: JobStatusWatcherConf, metricWatcherConf: MetricWatcherConf)

/**
 * configuration for FlinkMetricWatcher
 *
 * @param sglTrkTaskTimeoutSec  run timeout of single tracking task
 * @param sglTrkTaskIntervalSec interval seconds between two single tracking task
 */
case class MetricWatcherConf(sglTrkTaskTimeoutSec: Long, sglTrkTaskIntervalSec: Long)

/**
 * configuration for FlinkJobStatusWatcher
 *
 * @param sglTrkTaskTimeoutSec          run timeout of single tracking task
 * @param sglTrkTaskIntervalSec         interval seconds between two single tracking task
 * @param silentStateJobKeepTrackingSec SILENT flink state job keep tracking seconds
 */
case class JobStatusWatcherConf(sglTrkTaskTimeoutSec: Long,
                                sglTrkTaskIntervalSec: Long,
                                silentStateJobKeepTrackingSec: Int)

object FlinkTrkConf {
  def defaultConf: FlinkTrkConf = FlinkTrkConf(
    JobStatusWatcherConf.defaultConf,
    MetricWatcherConf.defaultConf)

  def debugConf: FlinkTrkConf = FlinkTrkConf(
    JobStatusWatcherConf.debugConf,
    MetricWatcherConf.debugConf)
}

object JobStatusWatcherConf {
  def defaultConf: JobStatusWatcherConf = JobStatusWatcherConf(
    sglTrkTaskTimeoutSec = 120,
    sglTrkTaskIntervalSec = 5,
    silentStateJobKeepTrackingSec = 60)

  def debugConf: JobStatusWatcherConf = JobStatusWatcherConf(
    sglTrkTaskTimeoutSec = 120,
    sglTrkTaskIntervalSec = 2,
    silentStateJobKeepTrackingSec = 5)
}

object MetricWatcherConf {
  def defaultConf: MetricWatcherConf = MetricWatcherConf(
    sglTrkTaskTimeoutSec = 120,
    sglTrkTaskIntervalSec = 10)

  def debugConf: MetricWatcherConf = MetricWatcherConf(
    sglTrkTaskTimeoutSec = 120,
    sglTrkTaskIntervalSec = 2)
}


