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

package org.apache.streampark.flink.kubernetes.configuration;

import org.apache.streampark.common.configuration.ConfigOption;
import org.apache.streampark.common.configuration.ConfigOptions;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/** Options owned by StreamPark's Flink Kubernetes integration. */
public final class KubernetesOptions {

    public static final ConfigOption<Duration> JOB_STATUS_REQUEST_TIMEOUT =
        durationOption(
            "streampark.flink-k8s.tracking.polling-task-timeout-sec.job-status",
            Duration.ofSeconds(120),
            "Timeout for one job-status polling request.");

    public static final ConfigOption<Duration> JOB_STATUS_CACHE_TIMEOUT =
        durationOption(
            "streampark.flink-k8s.tracking.cache-timeout-sec.job-status",
            Duration.ofSeconds(300),
            "Lifetime of one cached job-status entry.");

    public static final ConfigOption<Duration> METRIC_REQUEST_TIMEOUT =
        durationOption(
            "streampark.flink-k8s.tracking.polling-task-timeout-sec.cluster-metric",
            Duration.ofSeconds(120),
            "Timeout for one cluster-metric polling request.");

    public static final ConfigOption<Duration> JOB_STATUS_POLL_INTERVAL =
        durationOption(
            "streampark.flink-k8s.tracking.polling-interval-sec.job-status",
            Duration.ofSeconds(5),
            "Interval between job-status polling requests.");

    public static final ConfigOption<Duration> METRIC_POLL_INTERVAL =
        durationOption(
            "streampark.flink-k8s.tracking.polling-interval-sec.cluster-metric",
            Duration.ofSeconds(5),
            "Interval between cluster-metric polling requests.");

    public static final ConfigOption<Duration> SILENT_STATE_TRACKING_RETENTION =
        durationOption(
            "streampark.flink-k8s.tracking.silent-state-keep-sec",
            Duration.ofSeconds(60),
            "How long a job in SILENT state remains tracked.");

    public static final ConfigOption<String> INGRESS_CLASS =
        ConfigOptions.key("streampark.flink-k8s.ingress.class")
            .stringType()
            .defaultValue("nginx")
            .check(value -> !value.trim().isEmpty(), "ingress class must not be blank")
            .withDescription("Ingress controller class used by generated Kubernetes resources.")
            .build();

    private KubernetesOptions() {
    }

    private static ConfigOption<Duration> durationOption(
                                                         String key,
                                                         Duration defaultValue,
                                                         String description) {
        return ConfigOptions.key(key)
            .durationType(ChronoUnit.SECONDS)
            .defaultValue(defaultValue)
            .check(value -> value.getSeconds() > 0, "duration must be at least one second")
            .withDescription(description)
            .build();
    }
}
