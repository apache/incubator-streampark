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

import java.util.Objects;

public class FlinkMetricCV {

  private String groupId;

  private int totalJmMemory;

  private int totalTmMemory;

  private int totalTm;

  private int totalSlot;

  private int availableSlot;

  private int runningJob;

  private int finishedJob;

  private int cancelledJob;

  private int failedJob;

  private final long pollAckTime;

  public FlinkMetricCV(long pollAckTime) {
    this.pollAckTime = pollAckTime;
  }

  public static FlinkMetricCV empty(String groupId) {
    FlinkMetricCV flinkMetricCV = new FlinkMetricCV(System.currentTimeMillis());
    flinkMetricCV.setGroupId(groupId);
    return flinkMetricCV;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    FlinkMetricCV that = (FlinkMetricCV) o;
    return totalJmMemory == that.totalJmMemory
        && totalTmMemory == that.totalTmMemory
        && totalTm == that.totalTm
        && totalSlot == that.totalSlot
        && availableSlot == that.availableSlot
        && runningJob == that.runningJob
        && finishedJob == that.finishedJob
        && cancelledJob == that.cancelledJob
        && failedJob == that.failedJob
        && pollAckTime == that.pollAckTime
        && Objects.equals(groupId, that.groupId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        groupId,
        totalJmMemory,
        totalTmMemory,
        totalTm,
        totalSlot,
        availableSlot,
        runningJob,
        finishedJob,
        cancelledJob,
        failedJob,
        pollAckTime);
  }

  public FlinkMetricCV add(FlinkMetricCV flinkMetricCV) {
    if (Objects.isNull(flinkMetricCV)) {
      return this;
    }
    if (Objects.isNull(groupId) || groupId.equals(flinkMetricCV.getGroupId())) {
      long pollAckTime = Math.max(this.pollAckTime, flinkMetricCV.getPollAckTime());
      FlinkMetricCV result = new FlinkMetricCV(pollAckTime);
      result.setGroupId(groupId);
      result.setTotalJmMemory(totalJmMemory + flinkMetricCV.getTotalJmMemory());
      result.setTotalTmMemory(totalTmMemory + flinkMetricCV.getTotalTmMemory());
      result.setTotalTm(totalTm + flinkMetricCV.getTotalTm());
      result.setTotalSlot(totalSlot + flinkMetricCV.getTotalSlot());
      result.setAvailableSlot(availableSlot + flinkMetricCV.getAvailableSlot());
      result.setRunningJob(runningJob + flinkMetricCV.getRunningJob());
      result.setFinishedJob(finishedJob + flinkMetricCV.getFinishedJob());
      result.setCancelledJob(cancelledJob + flinkMetricCV.getCancelledJob());
      result.setFinishedJob(failedJob + flinkMetricCV.getFailedJob());
      return result;
    }
    return this;
  }

  public int totalJob() {
    return runningJob + finishedJob + cancelledJob + failedJob;
  }

  public String getGroupId() {
    return groupId;
  }

  public int getTotalJmMemory() {
    return totalJmMemory;
  }

  public int getTotalTmMemory() {
    return totalTmMemory;
  }

  public int getTotalTm() {
    return totalTm;
  }

  public int getTotalSlot() {
    return totalSlot;
  }

  public int getAvailableSlot() {
    return availableSlot;
  }

  public int getRunningJob() {
    return runningJob;
  }

  public int getFinishedJob() {
    return finishedJob;
  }

  public int getCancelledJob() {
    return cancelledJob;
  }

  public int getFailedJob() {
    return failedJob;
  }

  public long getPollAckTime() {
    return pollAckTime;
  }

  public void setGroupId(String groupId) {
    this.groupId = groupId;
  }

  public void setTotalJmMemory(int totalJmMemory) {
    this.totalJmMemory = totalJmMemory;
  }

  public void setTotalTmMemory(int totalTmMemory) {
    this.totalTmMemory = totalTmMemory;
  }

  public void setTotalTm(int totalTm) {
    this.totalTm = totalTm;
  }

  public void setTotalSlot(int totalSlot) {
    this.totalSlot = totalSlot;
  }

  public void setAvailableSlot(int availableSlot) {
    this.availableSlot = availableSlot;
  }

  public void setRunningJob(int runningJob) {
    this.runningJob = runningJob;
  }

  public void setFinishedJob(int finishedJob) {
    this.finishedJob = finishedJob;
  }

  public void setCancelledJob(int cancelledJob) {
    this.cancelledJob = cancelledJob;
  }

  public void setFailedJob(int failedJob) {
    this.failedJob = failedJob;
  }
}
