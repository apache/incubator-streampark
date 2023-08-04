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

import org.apache.streampark.flink.kubernetes.v2.model.EvalState.EvalState

import io.fabric8.kubernetes.client.Watcher
import org.apache.flink.v1beta1.{FlinkDeployment, FlinkDeploymentStatus, FlinkSessionJob, FlinkSessionJobStatus}
import org.apache.flink.v1beta1.FlinkDeploymentStatus.JobManagerDeploymentStatus

/*
 * Flink K8s custom resource status.
 *
 * For the evaluation logic of the state, please refer to:
 * - [[org.apache.streampark.flink.kubernetes.v2.model.DeployCRStatus.eval]]
 * - [[org.apache.streampark.flink.kubernetes.v2.model.SessionJobCRStatus.eval]
 */
sealed trait FlinkCRStatus {
  val namespace: String
  val name: String
  val evalState: EvalState
  val error: Option[String]
  val updatedTs: Long
}

/*
 * Evaluated status for Flink K8s CR.
 *
 * - DEPLOYING: The CR is being deploying or rollback.
 * - READY: The JobManager is ready.
 * - SUSPENDED: The CR has been suspended.
 * - FAILED: The job terminally failed or JobManager occurs error.
 * - DELETED: The CR has been deleted.
 */
object EvalState extends Enumeration {
  type EvalState = Value
  val DEPLOYING, READY, SUSPENDED, FAILED, DELETED = Value
}

/**
 * Flink deployment CR status snapshot.
 * See: [[org.apache.flink.v1beta1.FlinkDeploymentStatus]]
 */
case class DeployCRStatus(
    namespace: String,
    name: String,
    evalState: EvalState,
    action: Watcher.Action,
    lifecycle: FlinkDeploymentStatus.LifecycleState,
    jmDeployStatus: JobManagerDeploymentStatus,
    error: Option[String] = None,
    updatedTs: Long)
  extends FlinkCRStatus

object DeployCRStatus {

  import FlinkDeploymentStatus.LifecycleState

  def eval(action: Watcher.Action, cr: FlinkDeployment): DeployCRStatus = {
    val metadata       = cr.getMetadata
    val status         = cr.getStatus
    val lifecycle      = status.getLifecycleState
    val jmDeployStatus = status.getJobManagerDeploymentStatus

    val evalState = (action, lifecycle, jmDeployStatus) match {
      case (Watcher.Action.DELETED, _, _)           => EvalState.DELETED
      case (_, LifecycleState.FAILED, _)            => EvalState.FAILED
      case (_, _, JobManagerDeploymentStatus.ERROR) => EvalState.FAILED
      case (_, LifecycleState.SUSPENDED, _)         => EvalState.SUSPENDED
      case (_, _, JobManagerDeploymentStatus.READY) => EvalState.READY
      case _                                        => EvalState.DEPLOYING
    }

    DeployCRStatus(
      namespace = metadata.getNamespace,
      name = metadata.getName,
      evalState = evalState,
      action = action,
      lifecycle = lifecycle,
      jmDeployStatus = jmDeployStatus,
      error = Option(cr.getStatus.getError),
      updatedTs = System.currentTimeMillis
    )
  }
}

/**
 * Flink Session Job CR status snapshot.
 * See: [[org.apache.flink.v1beta1.FlinkSessionJobStatus]]
 */
case class SessionJobCRStatus(
    namespace: String,
    name: String,
    refDeployName: String,
    evalState: EvalState,
    action: Watcher.Action,
    lifecycle: FlinkSessionJobStatus.LifecycleState,
    error: Option[String],
    updatedTs: Long)
  extends FlinkCRStatus

object SessionJobCRStatus {

  import FlinkSessionJobStatus.LifecycleState

  def eval(action: Watcher.Action, cr: FlinkSessionJob): SessionJobCRStatus = {
    val metadata  = cr.getMetadata
    val status    = cr.getStatus
    val lifecycle = status.getLifecycleState

    val evalState = (action, lifecycle) match {
      case (Watcher.Action.DELETED, _)     => EvalState.DELETED
      case (_, LifecycleState.STABLE)      => EvalState.READY
      case (_, LifecycleState.ROLLED_BACK) => EvalState.READY
      case (_, LifecycleState.SUSPENDED)   => EvalState.SUSPENDED
      case (_, LifecycleState.FAILED)      => EvalState.FAILED
      case _                               => EvalState.DEPLOYING
    }

    SessionJobCRStatus(
      namespace = metadata.getNamespace,
      name = metadata.getName,
      refDeployName = cr.getSpec.getDeploymentName,
      evalState = evalState,
      action = action,
      lifecycle = lifecycle,
      error = Option(cr.getStatus.getError),
      updatedTs = System.currentTimeMillis
    )
  }
}
