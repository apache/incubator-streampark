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
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent = true)
@NoArgsConstructor
@AllArgsConstructor
public class JobStatusCV {
    private FlinkJobState jobState;
    private String jobId;
    private String jobName = "";
    private long jobStartTime = -1;
    private long jobEndTime = -1;
    private long duration = 0;
    private int taskTotal = 0;
    private long pollEmitTime;
    private long pollAckTime;

    public boolean diff(JobStatusCV that) {
        return that == null || that.jobState != this.jobState || !that.jobId.equals(this.jobId);
    }
}
