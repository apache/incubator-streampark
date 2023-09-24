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

import org.apache.streampark.common.enums.ExecutionModeEnum

/** execution mode of flink on kubernetes */
object FlinkK8sExecuteModeEnum extends Enumeration {

  val SESSION: FlinkK8sExecuteModeEnum.Value = Value("kubernetes-session")
  val APPLICATION: FlinkK8sExecuteModeEnum.Value = Value("kubernetes-application")

  def of(mode: ExecutionModeEnum): Value = {
    mode match {
      case ExecutionModeEnum.KUBERNETES_NATIVE_SESSION => SESSION
      case ExecutionModeEnum.KUBERNETES_NATIVE_APPLICATION => APPLICATION
      case _ => throw new IllegalStateException(s"Illegal K8sExecuteMode, ${mode.name}")
    }
  }

  def toExecutionMode(mode: FlinkK8sExecuteModeEnum.Value): ExecutionModeEnum = {
    mode match {
      case SESSION => ExecutionModeEnum.KUBERNETES_NATIVE_SESSION
      case APPLICATION => ExecutionModeEnum.KUBERNETES_NATIVE_APPLICATION
    }
  }

}
