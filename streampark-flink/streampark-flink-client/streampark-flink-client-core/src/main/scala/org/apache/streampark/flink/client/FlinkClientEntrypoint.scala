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

import org.apache.streampark.common.enums.FlinkDeployMode
import org.apache.streampark.flink.client.`trait`.FlinkClientTrait
import org.apache.streampark.flink.client.bean._
import org.apache.streampark.flink.client.impl._

import FlinkDeployMode._

object FlinkClientEntrypoint {

  private[this] val clients: Map[FlinkDeployMode, FlinkClientTrait] = Map(
    LOCAL -> LocalClient,
    REMOTE -> RemoteClient,
    YARN_APPLICATION -> YarnApplicationClient,
    YARN_SESSION -> YarnSessionClient,
    YARN_PER_JOB -> YarnPerJobClient,
    KUBERNETES_NATIVE_SESSION -> KubernetesNativeSessionClient,
    KUBERNETES_NATIVE_APPLICATION -> KubernetesNativeApplicationClient)

  def submit(submitRequest: SubmitRequest): SubmitResponse = {
    clients.get(submitRequest.deployMode) match {
      case Some(client) => client.submit(submitRequest)
      case _ =>
        throw new UnsupportedOperationException(
          s"Unsupported ${submitRequest.deployMode} submit ")
    }
  }

  def cancel(cancelRequest: CancelRequest): CancelResponse = {
    clients.get(cancelRequest.deployMode) match {
      case Some(client) => client.cancel(cancelRequest)
      case _ =>
        throw new UnsupportedOperationException(
          s"Unsupported ${cancelRequest.deployMode} cancel ")
    }
  }

  def triggerSavepoint(savepointRequest: TriggerSavepointRequest): SavepointResponse = {
    clients.get(savepointRequest.deployMode) match {
      case Some(client) => client.triggerSavepoint(savepointRequest)
      case _ =>
        throw new UnsupportedOperationException(
          s"Unsupported ${savepointRequest.deployMode} triggerSavepoint ")
    }
  }

  def deploy(deployRequest: DeployRequest): DeployResponse = {
    deployRequest.deployMode match {
      case YARN_SESSION => YarnSessionClient.deploy(deployRequest)
      case KUBERNETES_NATIVE_SESSION =>
        KubernetesNativeSessionClient.deploy(deployRequest)
      case _ =>
        throw new UnsupportedOperationException(
          s"Unsupported ${deployRequest.deployMode} deploy cluster ")
    }
  }

  def shutdown(shutDownRequest: ShutDownRequest): ShutDownResponse = {
    shutDownRequest.deployMode match {
      case YARN_SESSION => YarnSessionClient.shutdown(shutDownRequest)
      case KUBERNETES_NATIVE_SESSION =>
        KubernetesNativeSessionClient.shutdown(shutDownRequest)
      case _ =>
        throw new UnsupportedOperationException(
          s"Unsupported ${shutDownRequest.deployMode} shutdown cluster ")
    }
  }

}
