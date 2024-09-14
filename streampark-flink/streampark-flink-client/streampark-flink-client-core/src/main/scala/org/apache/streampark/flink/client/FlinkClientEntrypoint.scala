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

package org.apache.streampark.flink.client

import org.apache.streampark.common.enums.ExecutionMode
import org.apache.streampark.common.enums.ExecutionMode._
import org.apache.streampark.flink.client.`trait`.FlinkClientTrait
import org.apache.streampark.flink.client.bean._
import org.apache.streampark.flink.client.impl._

object FlinkClientEntrypoint {

  private[this] val clients: Map[ExecutionMode, FlinkClientTrait] = Map(
    LOCAL -> LocalClient,
    REMOTE -> RemoteClient,
    YARN_APPLICATION -> YarnApplicationClient,
    YARN_SESSION -> YarnSessionClient,
    YARN_PER_JOB -> YarnPerJobClient,
    KUBERNETES_NATIVE_SESSION -> KubernetesNativeSessionClient,
    KUBERNETES_NATIVE_APPLICATION -> KubernetesNativeApplicationClient
  )

  def submit(request: SubmitRequest): SubmitResponse = {
    clients.get(request.executionMode) match {
      case Some(client) => client.submit(request)
      case _ =>
        throw new UnsupportedOperationException(s"Unsupported ${request.executionMode} submit ")
    }
  }

  def cancel(request: CancelRequest): CancelResponse = {
    clients.get(request.executionMode) match {
      case Some(client) => client.cancel(request)
      case _ =>
        throw new UnsupportedOperationException(s"Unsupported ${request.executionMode} cancel ")
    }
  }

  def triggerSavepoint(request: TriggerSavepointRequest): SavepointResponse = {
    clients.get(request.executionMode) match {
      case Some(client) => client.triggerSavepoint(request)
      case _ =>
        throw new UnsupportedOperationException(
          s"Unsupported ${request.executionMode} triggerSavepoint ")
    }
  }

  def deploy(request: DeployRequest): DeployResponse = {
    request.executionMode match {
      case YARN_SESSION => YarnSessionClient.deploy(request)
      case KUBERNETES_NATIVE_SESSION => KubernetesNativeSessionClient.deploy(request)
      case _ =>
        throw new UnsupportedOperationException(
          s"Unsupported ${request.executionMode} deploy cluster ")
    }
  }

  def shutdown(request: DeployRequest): ShutDownResponse = {
    request.executionMode match {
      case YARN_SESSION => YarnSessionClient.shutdown(request)
      case KUBERNETES_NATIVE_SESSION => KubernetesNativeSessionClient.shutdown(request)
      case _ =>
        throw new UnsupportedOperationException(
          s"Unsupported ${request.executionMode} shutdown cluster ")
    }
  }

}
