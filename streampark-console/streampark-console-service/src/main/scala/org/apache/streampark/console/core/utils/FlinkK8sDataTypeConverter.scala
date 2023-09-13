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

import org.apache.streampark.common.enums.{ClusterState, ExecutionMode}
import org.apache.streampark.console.core.entity.{Application, FlinkCluster}
import org.apache.streampark.console.core.enums.FlinkAppState
import org.apache.streampark.console.core.utils.FlinkK8sDataTypeConverter.genSessionJobCRName
import org.apache.streampark.flink.kubernetes.model.FlinkMetricCV
import org.apache.streampark.flink.kubernetes.v2.model._
import org.apache.streampark.flink.kubernetes.v2.model.EvalJobState.EvalJobState
import org.apache.streampark.flink.kubernetes.v2.model.TrackKey.ClusterKey

import org.apache.commons.lang3.StringUtils
import org.springframework.stereotype.Component

import java.util.UUID

import scala.util.Try

@Component
class FlinkK8sDataTypeConverter() extends FlinkK8sDataTypeConverterStub {
  override def genSessionJobK8sCRName(clusterId: String): String = genSessionJobCRName(clusterId)
}

object FlinkK8sDataTypeConverter {

  /** Create default name for Flink SessionJob CR for k8s-native compatibility. */
  def genSessionJobCRName(clusterId: String): String = {
    s"$clusterId-${UUID.randomUUID().toString.replace("-", "").take(8)}"
  }

  /** Convert [[EvalJobState]] to [[FlinkAppState]]. */
  def k8sEvalJobStateToFlinkAppState(jobState: EvalJobState): FlinkAppState = {
    Try(FlinkAppState.valueOf(jobState.toString)).getOrElse(FlinkAppState.OTHER)
  }

  /** Convert [[DeployCRStatus]] to [[ClusterState]]. */
  def k8sDeployStateToClusterState(crState: DeployCRStatus): ClusterState = {
    crState.evalState match {
      case EvalState.DEPLOYING => ClusterState.STARTING
      case EvalState.READY     => ClusterState.RUNNING
      case EvalState.SUSPENDED => ClusterState.CANCELED
      case EvalState.FAILED    => ClusterState.FAILED
      case EvalState.DELETED   => ClusterState.KILLED
      case _                   => ClusterState.UNKNOWN
    }
  }

  /** Convert [[ClusterMetrics]] to [[FlinkMetricCV]]. */
  def clusterMetricsToFlinkMetricCV(metrics: ClusterMetrics): FlinkMetricCV = {
    FlinkMetricCV(
      totalJmMemory = metrics.totalJmMemory,
      totalTmMemory = metrics.totalTmMemory,
      totalTm = metrics.totalTm,
      totalSlot = metrics.totalTm,
      availableSlot = metrics.availableSlot,
      runningJob = metrics.runningJob,
      finishedJob = metrics.finishedJob,
      cancelledJob = metrics.cancelledJob,
      failedJob = metrics.failedJob,
      pollAckTime = 0L
    )
  }

  /** Convert [[FlinkCluster]] to [[ClusterKey]]. */
  def flinkClusterToClusterKey(flinkCluster: FlinkCluster): Option[ClusterKey] = {
    val isLegal = {
      flinkCluster != null &&
      flinkCluster.getId != null &&
      ExecutionMode.isKubernetesSessionMode(flinkCluster.getExecutionMode) &&
      StringUtils.isNoneBlank(flinkCluster.getClusterId) &&
      StringUtils.isNoneBlank(flinkCluster.getK8sNamespace)
    }
    if (isLegal) Some(ClusterKey(flinkCluster.getId, flinkCluster.getK8sNamespace, flinkCluster.getClusterId))
    else None
  }

  /** Convert [[Application]] to [[TrackKey]]. */
  def applicationToTrackKey(app: Application): Option[TrackKey] = {
    import ExecutionMode._

    val isLegal = {
      app != null &&
      app.getId != null &&
      ExecutionMode.isKubernetesSessionMode(app.getExecutionMode) &&
      StringUtils.isNoneBlank(app.getClusterId) &&
      StringUtils.isNoneBlank(app.getK8sNamespace)
    }

    if (isLegal) None
    else
      app.getExecutionModeEnum match {
        case KUBERNETES_NATIVE_APPLICATION => Some(TrackKey.appJob(app.getId, app.getK8sNamespace, app.getClusterId))
        case KUBERNETES_NATIVE_SESSION     =>
          Option(app.getK8sName) match {
            case Some(name) => Some(TrackKey.sessionJob(app.getId, app.getK8sNamespace, name, app.getClusterId))
            case None       =>
              Option(app.getJobId) match {
                case Some(jid) =>
                  Some(TrackKey.unmanagedSessionJob(app.getId, app.getK8sNamespace, app.getClusterId, jid))
                case None      => None
              }
          }
      }
  }

}
