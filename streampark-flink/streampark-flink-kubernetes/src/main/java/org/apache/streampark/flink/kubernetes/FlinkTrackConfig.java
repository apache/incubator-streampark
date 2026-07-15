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

import org.apache.streampark.common.conf.InternalConfigHolder;
import org.apache.streampark.common.conf.K8sFlinkConfig;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent = true)
@AllArgsConstructor
public class FlinkTrackConfig {

    private JobStatusWatcherConfig jobStatusWatcherConf;
    private MetricWatcherConfig metricWatcherConf;

    public static FlinkTrackConfig defaultConf() {
        return new FlinkTrackConfig(
            JobStatusWatcherConfig.defaultConf(), MetricWatcherConfig.defaultConf());
    }

    public static FlinkTrackConfig debugConf() {
        return new FlinkTrackConfig(
            JobStatusWatcherConfig.debugConf(), MetricWatcherConfig.debugConf());
    }

    public static FlinkTrackConfig fromConfigHub() {
        return new FlinkTrackConfig(
            new JobStatusWatcherConfig(
                InternalConfigHolder.get(K8sFlinkConfig.jobStatusTrackTaskTimeoutSec),
                InternalConfigHolder.get(K8sFlinkConfig.jobStatueTrackTaskIntervalSec),
                InternalConfigHolder.get(K8sFlinkConfig.silentStateJobKeepTrackingSec),
                InternalConfigHolder.get(K8sFlinkConfig.jobStatusTrackCacheTimeoutSec)),
            new MetricWatcherConfig(
                InternalConfigHolder.get(K8sFlinkConfig.metricTrackTaskTimeoutSec),
                InternalConfigHolder.get(K8sFlinkConfig.metricTrackTaskIntervalSec)));
    }
}
