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

package org.apache.streampark.flink.kubernetes;

import org.apache.streampark.common.configuration.Configuration;
import org.apache.streampark.common.configuration.GlobalConfiguration;
import org.apache.streampark.flink.kubernetes.configuration.KubernetesOptions;

import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * @param jobStatusWatcherConf configuration for flink job status tracking process
 * @param metricWatcherConf configuration for flink metric tracking process
 */
@Builder
@AllArgsConstructor
public class FlinkTrackConfig {

    private final JobStatusWatcherConfig jobStatusWatcherConf;
    private final MetricWatcherConfig metricWatcherConf;

    public JobStatusWatcherConfig jobStatusWatcherConf() {
        return jobStatusWatcherConf;
    }

    public MetricWatcherConfig metricWatcherConf() {
        return metricWatcherConf;
    }

    public static FlinkTrackConfig defaultConf() {
        return FlinkTrackConfig.builder()
            .jobStatusWatcherConf(JobStatusWatcherConfig.defaultConf())
            .metricWatcherConf(MetricWatcherConfig.defaultConf())
            .build();
    }

    public static FlinkTrackConfig debugConf() {
        return FlinkTrackConfig.builder()
            .jobStatusWatcherConf(JobStatusWatcherConfig.debugConf())
            .metricWatcherConf(MetricWatcherConfig.debugConf())
            .build();
    }

    /** Creates tracking configuration from the current immutable server snapshot. */
    public static FlinkTrackConfig fromConfiguration() {
        Configuration configuration = GlobalConfiguration.current();
        return FlinkTrackConfig.builder()
            .jobStatusWatcherConf(
                JobStatusWatcherConfig.builder()
                    .requestTimeoutSec(
                        configuration.get(KubernetesOptions.JOB_STATUS_REQUEST_TIMEOUT).getSeconds())
                    .requestIntervalSec(
                        configuration.get(KubernetesOptions.JOB_STATUS_POLL_INTERVAL).getSeconds())
                    .silentStateJobKeepTrackingSec(
                        Math.toIntExact(
                            configuration
                                .get(KubernetesOptions.SILENT_STATE_TRACKING_RETENTION)
                                .getSeconds()))
                    .jobStatusCacheTimeOutSec(
                        Math.toIntExact(
                            configuration
                                .get(KubernetesOptions.JOB_STATUS_CACHE_TIMEOUT)
                                .getSeconds()))
                    .build())
            .metricWatcherConf(
                MetricWatcherConfig.builder()
                    .requestTimeoutSec(
                        configuration.get(KubernetesOptions.METRIC_REQUEST_TIMEOUT).getSeconds())
                    .requestIntervalSec(
                        configuration.get(KubernetesOptions.METRIC_POLL_INTERVAL).getSeconds())
                    .build())
            .build();
    }
}
