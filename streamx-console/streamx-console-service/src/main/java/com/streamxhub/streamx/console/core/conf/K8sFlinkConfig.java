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

package com.streamxhub.streamx.console.core.conf;

import com.streamxhub.streamx.flink.kubernetes.FlinkTrkConf;
import com.streamxhub.streamx.flink.kubernetes.JobStatusWatcherConf;
import com.streamxhub.streamx.flink.kubernetes.MetricWatcherConf;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * flink-k8s configuration from springboot properties, prefix = flink-k8s
 * @author Al-assad
 */
@Configuration
@Data
public class K8sFlinkConfig {

    final private FlinkTrkConf defaultTrkConf = FlinkTrkConf.defaultConf();

    @Value("${streamx.flink-k8s.tracking.polling-task-timeout-sec.job-status:}")
    private Long sglJobStatusTrkTaskTimeoutSec = defaultTrkConf.jobStatusWatcherConf().sglTrkTaskTimeoutSec();


    @Value("${streamx.flink-k8s.tracking.polling-task-timeout-sec.cluster-metric:}")
    private Long sglMetricTrkTaskTimeoutSec = defaultTrkConf.metricWatcherConf().sglTrkTaskTimeoutSec();

    @Value("${streamx.flink-k8s.tracking.polling-interval-sec.job-status:}")
    private Long sglJobStatueTrkTaskIntervalSec = defaultTrkConf.jobStatusWatcherConf().sglTrkTaskIntervalSec();

    @Value("${streamx.flink-k8s.tracking.polling-interval-sec.cluster-metric:}")
    private Long sglMetricTrkTaskIntervalSec = defaultTrkConf.metricWatcherConf().sglTrkTaskIntervalSec();

    @Value("${streamx.flink-k8s.tracking.silent-state-keep-sec:}")
    private Integer silentStateJobKeepTrackingSec = defaultTrkConf.jobStatusWatcherConf().silentStateJobKeepTrackingSec();

    /**
     * covert to com.streamxhub.streamx.flink.kubernetes.FlinkTrkConf
     */
    public FlinkTrkConf toFlinkTrkConf() {
        return new FlinkTrkConf(
            new JobStatusWatcherConf(
                sglJobStatusTrkTaskTimeoutSec,
                sglJobStatueTrkTaskIntervalSec,
                silentStateJobKeepTrackingSec),
            new MetricWatcherConf(
                sglMetricTrkTaskTimeoutSec,
                sglMetricTrkTaskIntervalSec)
        );
    }

}
