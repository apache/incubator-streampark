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

package org.apache.streampark.flink.kubernetes.v2.model

import org.apache.streampark.flink.kubernetes.v2.FlinkRestRequest.{JobOverviewInfo, TaskStats}
import org.apache.streampark.flink.kubernetes.v2.model.JobState.JobState

import org.apache.flink.v1beta1.flinkdeploymentstatus
import org.apache.flink.v1beta1.flinksessionjobstatus

import scala.util.Try

/**
 * Flink job status snapshot.
 *
 * @param jobId     Job id
 * @param jobName   Job name
 * @param state     Job state from rest api
 * @param startTs   Job start timestamp
 * @param endTs     Job end timestamp
 * @param tasks     Tasks statistical information
 * @param updatedTs Last updated timestamp
 */
case class JobStatus(
    jobId: String,
    jobName: String,
    state: JobState,
    startTs: Long,
    endTs: Option[Long] = None,
    tasks: Option[TaskStats] = None,
    updatedTs: Long)

object JobStatus {

  // Convert from REST API object.
  def fromRest(ov: JobOverviewInfo): JobStatus = JobStatus(
    jobId = ov.jid,
    jobName = ov.name,
    state = JobState.valueOf(ov.state),
    startTs = ov.startTime,
    endTs = Some(ov.endTime),
    updatedTs = ov.lastModifyTime,
    tasks = Some(ov.tasks)
  )

  // Convert from Kubernetes CR object.
  def fromDeployCR(status: flinkdeploymentstatus.JobStatus): JobStatus = JobStatus(
    jobId = status.getJobId,
    jobName = status.getJobName,
    state = JobState.valueOf(status.getState),
    startTs = Try(status.getStartTime.toLong).getOrElse(0L),
    endTs = None,
    updatedTs = Try(status.getUpdateTime.toLong).getOrElse(0L),
    tasks = None
  )

  def fromSessionJobCR(status: flinksessionjobstatus.JobStatus): JobStatus = JobStatus(
    jobId = status.getJobId,
    jobName = status.getJobName,
    state = JobState.valueOf(status.getState),
    startTs = Try(status.getStartTime.toLong).getOrElse(0L),
    endTs = None,
    updatedTs = Try(status.getUpdateTime.toLong).getOrElse(0L),
    tasks = None
  )
}
