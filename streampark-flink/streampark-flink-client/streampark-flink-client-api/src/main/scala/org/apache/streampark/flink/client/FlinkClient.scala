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

import org.apache.streampark.common.conf.FlinkVersion
import org.apache.streampark.common.util.Logger
import org.apache.streampark.flink.client.bean._
import org.apache.streampark.flink.proxy.FlinkShimsProxy

import scala.language.{implicitConversions, reflectiveCalls}
import scala.reflect.ClassTag

object FlinkClient extends Logger {

  private[this] val FLINK_CLIENT_ENDPOINT_CLASS =
    "org.apache.streampark.flink.client.FlinkClientEndpoint"

  private[this] val SUBMIT_REQUEST =
    "org.apache.streampark.flink.client.bean.SubmitRequest" -> "submit"

  private[this] val DEPLOY_REQUEST =
    "org.apache.streampark.flink.client.bean.DeployRequest" -> "deploy"

  private[this] val CANCEL_REQUEST =
    "org.apache.streampark.flink.client.bean.CancelRequest" -> "cancel"

  private[this] val SHUTDOWN_REQUEST =
    "org.apache.streampark.flink.client.bean.ShutDownRequest" -> "shutdown"

  private[this] val SAVEPOINT_REQUEST =
    "org.apache.streampark.flink.client.bean.TriggerSavepointRequest" -> "triggerSavepoint"

  def submit(submitRequest: SubmitRequest): SubmitResponse = {
    proxy[SubmitResponse](submitRequest, submitRequest.flinkVersion, SUBMIT_REQUEST)
  }

  def cancel(stopRequest: CancelRequest): CancelResponse = {
    proxy[CancelResponse](stopRequest, stopRequest.flinkVersion, CANCEL_REQUEST)
  }

  def deploy(deployRequest: DeployRequest): DeployResponse = {
    proxy[DeployResponse](deployRequest, deployRequest.flinkVersion, DEPLOY_REQUEST)
  }

  def shutdown(shutDownRequest: ShutDownRequest): ShutDownResponse = {
    proxy[ShutDownResponse](shutDownRequest, shutDownRequest.flinkVersion, SHUTDOWN_REQUEST)
  }

  def triggerSavepoint(savepointRequest: TriggerSavepointRequest): SavepointResponse = {
    proxy[SavepointResponse](savepointRequest, savepointRequest.flinkVersion, SAVEPOINT_REQUEST)
  }

  private[this] def proxy[T: ClassTag](
      request: Object,
      flinkVersion: FlinkVersion,
      requestBody: (String, String)): T = {
    flinkVersion.checkVersion()
    FlinkShimsProxy.proxy(
      flinkVersion,
      (classLoader: ClassLoader) => {
        val submitClass = classLoader.loadClass(FLINK_CLIENT_ENDPOINT_CLASS)
        val requestClass = classLoader.loadClass(requestBody._1)
        val method = submitClass.getDeclaredMethod(requestBody._2, requestClass)
        method.setAccessible(true)
        val obj = method.invoke(null, FlinkShimsProxy.getObject(classLoader, request))
        if (obj == null) null.asInstanceOf[T]
        else {
          FlinkShimsProxy.getObject[T](this.getClass.getClassLoader, obj)
        }
      }
    )
  }

}
