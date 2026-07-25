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

package org.apache.streampark.common.conf;

/** Flink kubernetes Configuration for v1 version */
public final class K8sFlinkConfig {

    private K8sFlinkConfig() {
    }

    @Deprecated(since = "3.0.0", forRemoval = true)
    public static final InternalOption jobStatusTrackTaskTimeoutSec =
        new InternalOption(
            "streampark.flink-k8s.tracking.polling-task-timeout-sec.job-status",
            120L,
            Long.class,
            "run timeout seconds of single flink-k8s metrics tracking task");

    @Deprecated(since = "3.0.0", forRemoval = true)
    public static final InternalOption jobStatusTrackCacheTimeoutSec =
        new InternalOption(
            "streampark.flink-k8s.tracking.cache-timeout-sec.job-status",
            300,
            Integer.class,
            "status cache timeout seconds of single flink-k8s job status tracking task");

    @Deprecated(since = "3.0.0", forRemoval = true)
    public static final InternalOption metricTrackTaskTimeoutSec =
        new InternalOption(
            "streampark.flink-k8s.tracking.polling-task-timeout-sec.cluster-metric",
            120L,
            Long.class,
            "run timeout seconds of single flink-k8s job status tracking task");

    @Deprecated(since = "3.0.0", forRemoval = true)
    public static final InternalOption jobStatueTrackTaskIntervalSec =
        new InternalOption(
            "streampark.flink-k8s.tracking.polling-interval-sec.job-status",
            5L,
            Long.class,
            "interval seconds between two single flink-k8s metrics tracking task");

    @Deprecated(since = "3.0.0", forRemoval = true)
    public static final InternalOption metricTrackTaskIntervalSec =
        new InternalOption(
            "streampark.flink-k8s.tracking.polling-interval-sec.cluster-metric",
            5L,
            Long.class,
            "interval seconds between two single flink-k8s metrics tracking task");

    @Deprecated(since = "3.0.0", forRemoval = true)
    public static final InternalOption silentStateJobKeepTrackingSec =
        new InternalOption(
            "streampark.flink-k8s.tracking.silent-state-keep-sec",
            60,
            Integer.class,
            "retained tracking time for SILENT state flink tasks");

    public static final InternalOption ingressClass =
        new InternalOption(
            "streampark.flink-k8s.ingress.class",
            "nginx",
            String.class,
            "Direct ingress to the ingress controller.");

    @Deprecated(since = "3.0.0", forRemoval = true)
    public static final String DEFAULT_KUBERNETES_NAMESPACE = "default";
}
