/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.flink.kubernetes.model

/**
 * flink cluster metric info
 * @author Al-assad
 */
case class FlinkMetricCV(totalJmMemory: Integer = 0,
                         totalTmMemory: Integer = 0,
                         totalTm: Integer = 0,
                         totalSlot: Integer = 0,
                         availableSlot: Integer = 0,
                         runningJob: Integer = 0,
                         finishedJob: Integer = 0,
                         cancelledJob: Integer = 0,
                         failedJob: Integer = 0,
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
  def empty: FlinkMetricCV = FlinkMetricCV(pollAckTime = System.currentTimeMillis)
}
