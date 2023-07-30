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

package org.apache.streampark.console.core.utils

import org.apache.streampark.console.core.enums.FlinkAppState
import org.apache.streampark.flink.kubernetes.v2.model.{EvalState, JobSnapshot, JobState}
import org.apache.streampark.flink.kubernetes.v2.model.JobState.JobState

import scala.util.Try

object FlinkAppStateConverter {

  /** Merge CR status and job status inside [[JobSnapshot]] to [[FlinkAppState]]. */
  def dryK8sJobSnapshotToFlinkAppState(snapshot: JobSnapshot): FlinkAppState =
    (snapshot.crStatus, snapshot.jobStatus) match {
      case (None, None)                      => FlinkAppState.LOST
      case (None, Some(jobStatus))           => jobStateToAppState(jobStatus.state)
      case (Some(crStatus), None)            =>
        crStatus.evalState match {
          case EvalState.DEPLOYING | EvalState.READY => FlinkAppState.INITIALIZING
          case EvalState.FAILED                      => FlinkAppState.FAILED
          case EvalState.SUSPENDED                   => FlinkAppState.SUSPENDED
          case EvalState.DELETED                     => FlinkAppState.TERMINATED
        }
      case (Some(crStatus), Some(jobStatus)) =>
        if (jobStatus.updatedTs >= crStatus.updatedTs) jobStateToAppState(jobStatus.state)
        else {
          crStatus.evalState match {
            case EvalState.FAILED    => FlinkAppState.FAILED
            case EvalState.SUSPENDED => FlinkAppState.SUSPENDED
            case EvalState.DELETED   => FlinkAppState.TERMINATED
            case EvalState.READY     => jobStateToAppState(jobStatus.state)
            case EvalState.DEPLOYING =>
              if (JobState.maybeDeploying.contains(jobStatus.state)) jobStateToAppState(jobStatus.state)
              else FlinkAppState.INITIALIZING
          }
        }
    }

  private def jobStateToAppState(state: JobState) =
    Try(FlinkAppState.of(state.toString)).getOrElse(FlinkAppState.OTHER)

}
