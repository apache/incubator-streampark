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

package org.apache.streampark.flink.kubernetes.model;

import org.apache.streampark.flink.kubernetes.enums.FlinkJobState;

import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * @param jobState state of flink job
 * @param jobId flink jobId hex string
 * @param jobName flink job name
 * @param jobStartTime flink job starting timestamp
 * @param pollEmitTime tracking polling emit timestamp
 * @param pollAckTime tracking polling result receive timestamp
 */
@Builder(toBuilder = true)
@AllArgsConstructor
public class JobStatusCV {

    private final FlinkJobState jobState;
    private final String jobId;
    @Builder.Default
    private final String jobName = "";
    @Builder.Default
    private final Long jobStartTime = -1L;
    @Builder.Default
    private final Long jobEndTime = -1L;
    @Builder.Default
    private final Long duration = 0L;
    @Builder.Default
    private final Integer taskTotal = 0;
    private final Long pollEmitTime;
    private final Long pollAckTime;

    public FlinkJobState jobState() {
        return jobState;
    }

    public String jobId() {
        return jobId;
    }

    public String jobName() {
        return jobName;
    }

    public Long jobStartTime() {
        return jobStartTime;
    }

    public Long jobEndTime() {
        return jobEndTime;
    }

    public Long duration() {
        return duration;
    }

    public Integer taskTotal() {
        return taskTotal;
    }

    public Long pollEmitTime() {
        return pollEmitTime;
    }

    public Long pollAckTime() {
        return pollAckTime;
    }

    public boolean diff(JobStatusCV that) {
        return that == null
            || that.jobState() != jobState
            || !jobId.equals(that.jobId());
    }
}
