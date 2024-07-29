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
package org.apache.streampark.flink.kubernetes.enums

import org.apache.streampark.common.enums.FlinkExecutionMode

/** execution mode of flink on kubernetes */
object FlinkK8sExecuteMode extends Enumeration {

  val SESSION: FlinkK8sExecuteMode.Value = Value("kubernetes-session")
  val APPLICATION: FlinkK8sExecuteMode.Value = Value("kubernetes-application")

  def of(mode: FlinkExecutionMode): Value = {
    mode match {
      case FlinkExecutionMode.KUBERNETES_NATIVE_SESSION => SESSION
      case FlinkExecutionMode.KUBERNETES_NATIVE_APPLICATION => APPLICATION
      case _ =>
        throw new IllegalStateException(s"Illegal K8sExecuteMode, ${mode.name}")
    }
  }

  def toFlinkExecutionMode(mode: FlinkK8sExecuteMode.Value): FlinkExecutionMode = {
    mode match {
      case SESSION => FlinkExecutionMode.KUBERNETES_NATIVE_SESSION
      case APPLICATION => FlinkExecutionMode.KUBERNETES_NATIVE_APPLICATION
    }
  }

}
