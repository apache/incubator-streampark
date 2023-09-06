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

import org.apache.streampark.flink.kubernetes.v2.model.EvalJobState.EvalJobState

/**
 * Flink job status snapshot identified by StreamPark app-id.
 *
 * For the logical code to convert a JobSnapshot to a [[org.apache.streampark.console.core.enums.FlinkAppState]],
 *
 * @param appId     Ref to [[org.apache.streampark.console.core.entity.Application.id]]
 * @param clusterNs Flink cluster namespace on kubernetes.
 * @param clusterId Flink cluster name on kubernetes.
 * @param evalState Final evaluation job status
 * @param crStatus  Flink K8s CR status.
 * @param jobStatus Flink job status received from REST API.
 */
case class JobSnapshot(
    appId: Long,
    clusterNs: String,
    clusterId: String,
    evalState: EvalJobState,
    crStatus: Option[FlinkCRStatus],
    jobStatus: Option[JobStatus])

object JobSnapshot {

  def eval(
      appId: Long,
      clusterNs: String,
      clusterId: String,
      crStatus: Option[FlinkCRStatus],
      jobStatus: Option[JobStatus]): JobSnapshot = JobSnapshot(
    appId = appId,
    clusterNs = clusterNs,
    clusterId = clusterId,
    evalState = evalFinalJobState(crStatus, jobStatus),
    crStatus = crStatus,
    jobStatus = jobStatus
  )

  private def evalFinalJobState(crStatus: Option[FlinkCRStatus], jobStatus: Option[JobStatus]): EvalJobState =
    (crStatus, jobStatus) match {
      case (None, None)                      => EvalJobState.LOST
      case (None, Some(jobStatus))           => EvalJobState.of(jobStatus.state)
      case (Some(crStatus), None)            =>
        crStatus.evalState match {
          case EvalState.DEPLOYING | EvalState.READY => EvalJobState.INITIALIZING
          case EvalState.FAILED                      => EvalJobState.FAILED
          case EvalState.SUSPENDED                   => EvalJobState.SUSPENDED
          case EvalState.DELETED                     => EvalJobState.TERMINATED
        }
      case (Some(crStatus), Some(jobStatus)) =>
        if (jobStatus.updatedTs >= crStatus.updatedTs) EvalJobState.of(jobStatus.state)
        else {
          crStatus.evalState match {
            case EvalState.FAILED    => EvalJobState.FAILED
            case EvalState.SUSPENDED => EvalJobState.SUSPENDED
            case EvalState.DELETED   => EvalJobState.TERMINATED
            case EvalState.READY     => EvalJobState.of(jobStatus.state)
            case EvalState.DEPLOYING =>
              if (JobState.maybeDeploying.contains(jobStatus.state)) EvalJobState.of(jobStatus.state)
              else EvalJobState.INITIALIZING
          }
        }
    }

}
