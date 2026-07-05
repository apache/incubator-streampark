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
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent = true)
@NoArgsConstructor
@AllArgsConstructor
public class FlinkMetricCV {
    private String groupId = null;
    private Integer totalJmMemory = 0;
    private Integer totalTmMemory = 0;
    private Integer totalTm = 0;
    private Integer totalSlot = 0;
    private Integer availableSlot = 0;
    private Integer runningJob = 0;
    private Integer finishedJob = 0;
    private Integer cancelledJob = 0;
    private Integer failedJob = 0;
    private long pollAckTime;

    public FlinkMetricCV add(FlinkMetricCV another) {
        if (another == null) {
            return this;
        }
        if (groupId == null || groupId.equals(another.groupId)) {
            return new FlinkMetricCV(
                groupId,
                totalJmMemory + another.totalJmMemory,
                totalTmMemory + another.totalTmMemory,
                totalTm + another.totalTm,
                totalSlot + another.totalSlot,
                availableSlot + another.availableSlot,
                runningJob + another.runningJob,
                finishedJob + another.finishedJob,
                cancelledJob + another.cancelledJob,
                failedJob + another.failedJob,
                Math.max(pollAckTime, another.pollAckTime));
        }
        return this;
    }

    public Integer totalJob() {
        return runningJob + finishedJob + cancelledJob + failedJob;
    }

    public boolean equalsPayload(FlinkMetricCV another) {
        return groupId.equals(another.groupId)
            && totalJmMemory.equals(another.totalJmMemory)
            && totalTmMemory.equals(another.totalTmMemory)
            && totalTm.equals(another.totalTm)
            && totalSlot.equals(another.totalSlot)
            && availableSlot.equals(another.availableSlot)
            && runningJob.equals(another.runningJob)
            && finishedJob.equals(another.finishedJob)
            && cancelledJob.equals(another.cancelledJob)
            && failedJob.equals(another.failedJob);
    }

    public static FlinkMetricCV empty(String groupId) {
        return new FlinkMetricCV(groupId, 0, 0, 0, 0, 0, 0, 0, 0, 0, System.currentTimeMillis());
    }
}
