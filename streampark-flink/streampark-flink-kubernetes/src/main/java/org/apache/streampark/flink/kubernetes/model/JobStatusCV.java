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

public class JobStatusCV {
  private final FlinkJobState jobState;

  private final String jobId;

  private final long pollEmitTime;

  private final long pollAckTime;

  private String jobName = "";

  private long jobStartTime = -1L;

  private long jobEndTime = -1L;

  private long duration = 0L;

  private int taskTotal = 0;

  public JobStatusCV(FlinkJobState jobState, String jobId, long pollEmitTime, long pollAckTime) {
    this.jobState = jobState;
    this.jobId = jobId;
    this.pollEmitTime = pollEmitTime;
    this.pollAckTime = pollAckTime;
  }

  public FlinkJobState getJobState() {
    return jobState;
  }

  public String getJobId() {
    return jobId;
  }

  public long getPollEmitTime() {
    return pollEmitTime;
  }

  public long getPollAckTime() {
    return pollAckTime;
  }

  public String getJobName() {
    return jobName;
  }

  public long getJobStartTime() {
    return jobStartTime;
  }

  public long getJobEndTime() {
    return jobEndTime;
  }

  public long getDuration() {
    return duration;
  }

  public int getTaskTotal() {
    return taskTotal;
  }

  public void setJobName(String jobName) {
    this.jobName = jobName;
  }

  public void setJobStartTime(long jobStartTime) {
    this.jobStartTime = jobStartTime;
  }

  public void setJobEndTime(long jobEndTime) {
    this.jobEndTime = jobEndTime;
  }

  public void setDuration(long duration) {
    this.duration = duration;
  }

  public void setTaskTotal(int taskTotal) {
    this.taskTotal = taskTotal;
  }
}
