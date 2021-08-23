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
package com.streamxhub.streamx.flink.k8s.model

import com.streamxhub.streamx.flink.k8s.enums.FlinkK8sExecuteMode

import javax.annotation.Nullable
import scala.util.Try

/**
 * tracking identifier for flink on kubernetes
 */
case class TrkId(executeMode: FlinkK8sExecuteMode.Value,
                 namespace: String,
                 clusterId: String,
                 @Nullable jobId: String) {

  /**
   * check whether fields of trkid are legal
   */
  def isLegal: Boolean = {
    if (executeMode == null) {
      return false
    }
    executeMode match {
      case FlinkK8sExecuteMode.SESSION =>
        Try(namespace.nonEmpty).getOrElse(false) && Try(clusterId.nonEmpty).getOrElse(false)
      case FlinkK8sExecuteMode.APPLICATION =>
        Try(namespace.nonEmpty).getOrElse(false) && Try(clusterId.nonEmpty).getOrElse(false) && Try(jobId.nonEmpty).getOrElse(false)
      case _ => false
    }
  }

}

object TrkId {
  def onSession(namespace: String, clusterId: String, jobId: String): TrkId = {
    this (FlinkK8sExecuteMode.SESSION, namespace, clusterId, jobId)
  }

  def onApplication(namespace: String, clusterId: String): TrkId = {
    this (FlinkK8sExecuteMode.APPLICATION, namespace, clusterId, "")
  }
}

