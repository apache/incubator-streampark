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

package org.apache.streampark.flink.kubernetes.rest;

import org.apache.streampark.flink.kubernetes.enums.FlinkJobState;
import org.apache.streampark.flink.kubernetes.model.JobStatusCV;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobDetail {

    private String jid;
    private String name;
    private String state;

    @JsonProperty("start-time")
    private long startTime;

    @JsonProperty("end-time")
    private long endTime;

    private long duration;

    @JsonProperty("last-modification")
    private long lastModification;

    private JobTask tasks;

    public String jid() {
        return jid;
    }

    public String name() {
        return name;
    }

    public String state() {
        return state;
    }

    public long startTime() {
        return startTime;
    }

    public long endTime() {
        return endTime;
    }

    public long duration() {
        return duration;
    }

    public long lastModification() {
        return lastModification;
    }

    public JobTask tasks() {
        return tasks;
    }

    public JobStatusCV toJobStatusCV(long pollEmitTime, long pollAckTime) {
        JobTask task = tasks != null ? tasks : new JobTask();
        return JobStatusCV.builder()
            .jobState(FlinkJobState.of(state))
            .jobId(jid)
            .jobName(name != null ? name : "")
            .jobStartTime(startTime)
            .jobEndTime(endTime)
            .duration(duration)
            .taskTotal(task.total())
            .pollEmitTime(pollEmitTime)
            .pollAckTime(pollAckTime)
            .build();
    }
}
