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

import lombok.AllArgsConstructor;
import lombok.Builder;

/** flink cluster metric info */
@Builder
@AllArgsConstructor
public class FlinkMetricCV {

    @Builder.Default
    private final String groupId = null;
    @Builder.Default
    private final Integer totalJmMemory = 0;
    @Builder.Default
    private final Integer totalTmMemory = 0;
    @Builder.Default
    private final Integer totalTm = 0;
    @Builder.Default
    private final Integer totalSlot = 0;
    @Builder.Default
    private final Integer availableSlot = 0;
    @Builder.Default
    private final Integer runningJob = 0;
    @Builder.Default
    private final Integer finishedJob = 0;
    @Builder.Default
    private final Integer cancelledJob = 0;
    @Builder.Default
    private final Integer failedJob = 0;
    private final Long pollAckTime;

    public String groupId() {
        return groupId;
    }

    public Integer totalJmMemory() {
        return totalJmMemory;
    }

    public Integer totalTmMemory() {
        return totalTmMemory;
    }

    public Integer totalTm() {
        return totalTm;
    }

    public Integer totalSlot() {
        return totalSlot;
    }

    public Integer availableSlot() {
        return availableSlot;
    }

    public Integer runningJob() {
        return runningJob;
    }

    public Integer finishedJob() {
        return finishedJob;
    }

    public Integer cancelledJob() {
        return cancelledJob;
    }

    public Integer failedJob() {
        return failedJob;
    }

    public Long pollAckTime() {
        return pollAckTime;
    }

    public FlinkMetricCV add(FlinkMetricCV another) {
        if (another == null) {
            return this;
        }
        if (groupId == null || groupId.equals(another.groupId())) {
            return FlinkMetricCV.builder()
                .groupId(groupId)
                .totalJmMemory(totalJmMemory + another.totalJmMemory())
                .totalTmMemory(totalTmMemory + another.totalTmMemory())
                .totalTm(totalTm + another.totalTm())
                .totalSlot(totalSlot + another.totalSlot())
                .availableSlot(availableSlot + another.availableSlot())
                .runningJob(runningJob + another.runningJob())
                .finishedJob(finishedJob + another.finishedJob())
                .cancelledJob(cancelledJob + another.cancelledJob())
                .failedJob(failedJob + another.failedJob())
                .pollAckTime(Math.max(pollAckTime, another.pollAckTime()))
                .build();
        }
        return this;
    }

    public Integer totalJob() {
        return runningJob + finishedJob + cancelledJob + failedJob;
    }

    public boolean equalsPayload(FlinkMetricCV another) {
        return java.util.Objects.equals(groupId, another.groupId())
            && totalJmMemory.equals(another.totalTmMemory())
            && totalTmMemory.equals(another.totalTmMemory())
            && totalTm.equals(another.totalTm())
            && totalSlot.equals(another.totalSlot())
            && availableSlot.equals(another.availableSlot())
            && runningJob.equals(another.runningJob())
            && finishedJob.equals(another.finishedJob())
            && cancelledJob.equals(another.cancelledJob())
            && failedJob.equals(another.failedJob());
    }

    public static FlinkMetricCV empty() {
        return empty(null);
    }

    public static FlinkMetricCV empty(String groupId) {
        return FlinkMetricCV.builder()
            .groupId(groupId)
            .pollAckTime(System.currentTimeMillis())
            .build();
    }
}
