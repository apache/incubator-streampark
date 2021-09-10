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
package com.streamxhub.streamx.flink.kubernetes.model

/**
 * flink clutser mertic info
 * author: Al-assad
 */
case class FlinkMetricCV(totalJmMemory: Integer,
                         totalTmMemory: Integer,
                         totalTm: Integer,
                         totalSlot: Integer,
                         availableSlot: Integer,
                         runningJob: Integer,
                         finishedJob: Integer,
                         cancelledJob: Integer,
                         failedJob: Integer,
                         pollAckTime: Long) {

  def +(another: FlinkMetricCV): FlinkMetricCV = {
    this.copy(
      totalJmMemory + another.totalJmMemory,
      totalTmMemory + another.totalTmMemory,
      totalTm + another.totalTm,
      totalSlot + another.totalSlot,
      availableSlot + another.availableSlot,
      runningJob + another.runningJob,
      finishedJob + another.finishedJob,
      cancelledJob + another.cancelledJob,
      failedJob + another.failedJob,
      math.max(pollAckTime, another.pollAckTime)
    )
  }

  def totalJob(): Integer = runningJob + finishedJob + cancelledJob + failedJob

  def equalsPayload(another: FlinkMetricCV): Boolean = {
    totalJmMemory == another.totalTmMemory &&
      totalTmMemory == another.totalTmMemory &&
      totalTm == another.totalTm &&
      totalSlot == another.totalSlot &&
      availableSlot == another.availableSlot &&
      runningJob == another.runningJob &&
      finishedJob == another.finishedJob &&
      cancelledJob == another.cancelledJob &&
      failedJob == another.failedJob
  }

  def nonEqualsPayload(another: FlinkMetricCV): Boolean = !equalsPayload(another)

}

object FlinkMetricCV {
  def empty: FlinkMetricCV = FlinkMetricCV(0, 0, 0, 0, 0, 0, 0, 0, 0, System.currentTimeMillis)
}
