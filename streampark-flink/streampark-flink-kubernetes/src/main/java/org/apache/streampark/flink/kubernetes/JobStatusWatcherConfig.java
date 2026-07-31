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

import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * configuration for FlinkJobStatusWatcher
 *
 * @param requestTimeoutSec run timeout of single tracking task
 * @param requestIntervalSec interval seconds between two single tracking task
 * @param silentStateJobKeepTrackingSec retained tracking time for SILENT state flink tasks
 * @param jobStatusCacheTimeOutSec job status cache time out of single tracking task, must bigger
 *     than silentStateJobKeepTrackingSec
 */
@Builder
@AllArgsConstructor
public class JobStatusWatcherConfig {

    private final long requestTimeoutSec;
    private final long requestIntervalSec;
    private final int silentStateJobKeepTrackingSec;
    private final int jobStatusCacheTimeOutSec;

    public long requestTimeoutSec() {
        return requestTimeoutSec;
    }

    public long requestIntervalSec() {
        return requestIntervalSec;
    }

    public int silentStateJobKeepTrackingSec() {
        return silentStateJobKeepTrackingSec;
    }

    public int jobStatusCacheTimeOutSec() {
        return jobStatusCacheTimeOutSec;
    }

    public static JobStatusWatcherConfig defaultConf() {
        return JobStatusWatcherConfig.builder()
            .requestTimeoutSec(120L)
            .requestIntervalSec(5L)
            .silentStateJobKeepTrackingSec(60)
            .jobStatusCacheTimeOutSec(300)
            .build();
    }

    public static JobStatusWatcherConfig debugConf() {
        return JobStatusWatcherConfig.builder()
            .requestTimeoutSec(120L)
            .requestIntervalSec(2L)
            .silentStateJobKeepTrackingSec(5)
            .jobStatusCacheTimeOutSec(30)
            .build();
    }
}
