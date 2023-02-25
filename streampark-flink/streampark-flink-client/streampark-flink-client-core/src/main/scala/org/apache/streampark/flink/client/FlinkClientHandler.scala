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
import org.apache.streampark.flink.client.bean._
import org.apache.streampark.flink.client.impl._

object FlinkClientHandler {

  def submit(submitInfo: SubmitRequest): SubmitResponse = {
    submitInfo.executionMode match {
      case ExecutionMode.LOCAL => LocalClient.submit(submitInfo)
      case ExecutionMode.REMOTE => RemoteClient.submit(submitInfo)
      case ExecutionMode.YARN_APPLICATION => YarnApplicationClient.submit(submitInfo)
      case ExecutionMode.YARN_SESSION => YarnSessionClient.submit(submitInfo)
      case ExecutionMode.YARN_PER_JOB => YarnPerJobClient.submit(submitInfo)
      case ExecutionMode.KUBERNETES_NATIVE_SESSION => KubernetesNativeSessionClient.submit(submitInfo)
      case ExecutionMode.KUBERNETES_NATIVE_APPLICATION => KubernetesNativeApplicationClient.submit(submitInfo)
      case _ => throw new UnsupportedOperationException(s"Unsupported ${submitInfo.executionMode} submit ")
    }
  }

  def triggerSavepoint(savepointRequest: TriggerSavepointRequest): SavepointResponse = {
    savepointRequest.executionMode match {
      case ExecutionMode.LOCAL => LocalClient.triggerSavepoint(savepointRequest)
      case ExecutionMode.REMOTE => RemoteClient.triggerSavepoint(savepointRequest)
      case ExecutionMode.YARN_APPLICATION => YarnApplicationClient.triggerSavepoint(savepointRequest)
      case ExecutionMode.YARN_SESSION => YarnSessionClient.triggerSavepoint(savepointRequest)
      case ExecutionMode.YARN_PER_JOB | ExecutionMode.YARN_SESSION => YarnPerJobClient.triggerSavepoint(savepointRequest)
      case ExecutionMode.KUBERNETES_NATIVE_SESSION => KubernetesNativeSessionClient.triggerSavepoint(savepointRequest)
      case ExecutionMode.KUBERNETES_NATIVE_APPLICATION => KubernetesNativeApplicationClient.triggerSavepoint(savepointRequest)
      case _ => throw new UnsupportedOperationException(s"Unsupported ${savepointRequest.executionMode} Submit ")
    }
  }

  def cancel(cancelRequest: CancelRequest): CancelResponse = {
    cancelRequest.executionMode match {
      case ExecutionMode.LOCAL => LocalClient.cancel(cancelRequest)
      case ExecutionMode.REMOTE => RemoteClient.cancel(cancelRequest)
      case ExecutionMode.YARN_APPLICATION => YarnApplicationClient.cancel(cancelRequest)
      case ExecutionMode.YARN_SESSION => YarnSessionClient.cancel(cancelRequest)
      case ExecutionMode.YARN_PER_JOB | ExecutionMode.YARN_SESSION => YarnPerJobClient.cancel(cancelRequest)
      case ExecutionMode.KUBERNETES_NATIVE_SESSION => KubernetesNativeSessionClient.cancel(cancelRequest)
      case ExecutionMode.KUBERNETES_NATIVE_APPLICATION => KubernetesNativeApplicationClient.cancel(cancelRequest)
      case _ => throw new UnsupportedOperationException(s"Unsupported ${cancelRequest.executionMode} cancel ")
    }
  }

  def deploy(deployRequest: DeployRequest): DeployResponse = {
    deployRequest.executionMode match {
      case ExecutionMode.YARN_SESSION => YarnSessionClient.deploy(deployRequest)
      case ExecutionMode.KUBERNETES_NATIVE_SESSION => KubernetesNativeSessionClient.deploy(deployRequest)
      case _ => throw new UnsupportedOperationException(s"Unsupported ${deployRequest.executionMode} deploy cluster ")
    }
  }

  def shutdown(shutDownRequest: ShutDownRequest): ShutDownResponse = {
    shutDownRequest.executionMode match {
      case ExecutionMode.YARN_SESSION => YarnSessionClient.shutdown(shutDownRequest)
      case ExecutionMode.KUBERNETES_NATIVE_SESSION => KubernetesNativeSessionClient.shutdown(shutDownRequest)
      case _ => throw new UnsupportedOperationException(s"Unsupported ${shutDownRequest.executionMode} shutdown cluster ")
    }
  }
}
