/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.streampark.flink.kubernetes.v2.model

/**
 * Flink custer metrics.
 *
 * see: [[org.apache.streampark.flink.kubernetes.model.FlinkMetricCV]]
 */
case class ClusterMetrics(
    totalJmMemory: Integer = 0,
    totalTmMemory: Integer = 0,
    totalTm: Integer = 0,
    totalSlot: Integer = 0,
    availableSlot: Integer = 0,
    runningJob: Integer = 0,
    finishedJob: Integer = 0,
    cancelledJob: Integer = 0,
    failedJob: Integer = 0) {

  lazy val totalJob: Integer = runningJob + finishedJob + cancelledJob + failedJob

  def +(another: ClusterMetrics): ClusterMetrics =
    ClusterMetrics(
      totalJmMemory = another.totalJmMemory + totalJmMemory,
      totalTmMemory = another.totalTmMemory + totalTmMemory,
      totalTm = another.totalTm + totalTm,
      totalSlot = another.totalSlot + totalSlot,
      availableSlot = another.availableSlot + availableSlot,
      runningJob = another.runningJob + runningJob,
      finishedJob = another.finishedJob + finishedJob,
      cancelledJob = another.cancelledJob + cancelledJob,
      failedJob = another.failedJob + failedJob
    )
}

object ClusterMetrics {
  lazy val empty: ClusterMetrics = ClusterMetrics()
}
