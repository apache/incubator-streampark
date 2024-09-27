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

package org.apache.streampark.flink.kubernetes.model

import org.apache.streampark.common.util.Utils
import org.apache.streampark.flink.kubernetes.enums.FlinkK8sDeployMode

import java.lang.{Long => JavaLong}
import java.util.Properties

import scala.util.Try

/** tracking identifier for flink on kubernetes */
case class TrackId(
    executeMode: FlinkK8sDeployMode.Value,
    namespace: String = "default",
    clusterId: String,
    appId: JavaLong = null,
    jobId: String,
    groupId: String,
    properties: Properties) {

  def isLegal: Boolean = {
    executeMode match {
      case FlinkK8sDeployMode.APPLICATION =>
        Try(namespace.nonEmpty).getOrElse(false) && Try(clusterId.nonEmpty)
          .getOrElse(false)
      case FlinkK8sDeployMode.SESSION =>
        Try(namespace.nonEmpty).getOrElse(false) && Try(clusterId.nonEmpty)
          .getOrElse(false) && Try(jobId.nonEmpty).getOrElse(false)
      case _ => false
    }
  }

  def isActive: Boolean = isLegal && Try(jobId.nonEmpty).getOrElse(false)

  /** covert to ClusterKey */
  def toClusterKey: ClusterKey = ClusterKey(executeMode, namespace, clusterId)

  override def hashCode(): Int = {
    Utils.hashCode(executeMode, clusterId, namespace, appId, jobId, groupId, properties)
  }

  override def equals(obj: Any): Boolean = {
    obj match {
      case that: TrackId =>
        this.executeMode == that.executeMode &&
        this.clusterId == that.clusterId &&
        this.namespace == that.namespace &&
        this.appId == that.appId &&
        this.jobId == that.jobId &&
        this.groupId == that.groupId &&
        this.properties == that.properties
      case _ => false
    }
  }

}

object TrackId {
  def onSession(
      namespace: String,
      clusterId: String,
      appId: Long,
      jobId: String,
      groupId: String,
      properties: Properties): TrackId = {
    this(FlinkK8sDeployMode.SESSION, namespace, clusterId, appId, jobId, groupId, properties)
  }

  def onApplication(
      namespace: String,
      clusterId: String,
      appId: Long,
      jobId: String = null,
      groupId: String,
      properties: Properties): TrackId = {
    this(FlinkK8sDeployMode.APPLICATION, namespace, clusterId, appId, jobId, groupId, properties)
  }
}
