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

package org.apache.streampark.console.core.task;

import org.apache.streampark.flink.kubernetes.model.FlinkMetricCV;
import org.apache.streampark.flink.kubernetes.v2.model.ClusterMetrics;

/**
 * Stub for exposing methods of FlinkK8sObserver used to adapt the Java code invocation on
 * streampark-console-service.
 */
public interface FlinkK8sObserverStub {

  /** Get aggregated metrics of all flink jobs on k8s cluster by team-id */
  ClusterMetrics getAggClusterMetric(Long teamId);

  /** Compatible with old code of flink-k8s-v1. */
  FlinkMetricCV getAggClusterMetricCV(Long teamId);
}
