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

import java.util.Objects

import com.streamxhub.streamx.flink.kubernetes.enums.FlinkK8sExecuteMode
import org.apache.flink.api.common.JobID

import scala.util.Try

/**
 * tracking identifier for flink on kubernetes
 * author:Al-assad
 */
case class TrackId(executeMode: FlinkK8sExecuteMode.Value,
                   namespace: String = "default",
                   clusterId: String,
                   appId: Long,
                   jobId: String) {

  /**
   * check whether fields of trackId are legal
   */
  def isLegal: Boolean = {
    executeMode match {
      case FlinkK8sExecuteMode.APPLICATION =>
        Try(namespace.nonEmpty).getOrElse(false) && Try(clusterId.nonEmpty).getOrElse(false)
      case FlinkK8sExecuteMode.SESSION =>
        Try(namespace.nonEmpty).getOrElse(false) && Try(clusterId.nonEmpty).getOrElse(false) && Try(jobId.nonEmpty).getOrElse(false)
      case _ => false
    }
  }

  /**
   * check whether fields of trackId are no legal
   */
  def nonLegal: Boolean = !isLegal

  /**
   * covert to ClusterKey
   */
  def toClusterKey: ClusterKey = ClusterKey(executeMode, namespace, clusterId)

  /**
   * belong to cluster
   */
  def belongTo(clusterKey: ClusterKey): Boolean =
    executeMode == clusterKey.executeMode && namespace == clusterKey.namespace && clusterId == clusterKey.clusterId

  override def hashCode(): Int = {
    Objects.hash(executeMode, clusterId, namespace, appId)
  }

  override def equals(obj: Any): Boolean = {
    if (this == null || obj == null) return false
    if (!obj.isInstanceOf[TrackId]) return false
    val that = obj.asInstanceOf[TrackId]
    this.executeMode == that.executeMode &&
      this.clusterId == that.clusterId &&
      this.namespace == that.namespace &&
      this.appId == that.appId
  }

}

object TrackId {
  def onSession(namespace: String, clusterId: String, appId: Long, jobId: String): TrackId = {
    this (FlinkK8sExecuteMode.SESSION, namespace, clusterId, appId, jobId)
  }

  def onApplication(namespace: String, clusterId: String, appId: Long, jobId: String = null): TrackId = {
    this (FlinkK8sExecuteMode.APPLICATION, namespace, clusterId, appId, jobId)
  }
}
