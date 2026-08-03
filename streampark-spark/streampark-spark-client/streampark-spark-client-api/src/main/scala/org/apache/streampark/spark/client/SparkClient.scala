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

package org.apache.streampark.spark.client

import org.apache.streampark.common.conf.SparkVersion
import org.apache.streampark.common.util.Logger
import org.apache.streampark.spark.client.bean._
import org.apache.streampark.spark.client.proxy.SparkShimsProxy

import scala.reflect.ClassTag

object SparkClient extends Logger {

  private[this] val SPARK_CLIENT_ENDPOINT_CLASS =
    "org.apache.streampark.spark.client.SparkClientEndpoint"

  private[this] val SUBMIT_REQUEST =
    "org.apache.streampark.spark.client.bean.SubmitRequest" -> "submit"

  private[this] val CANCEL_REQUEST =
    "org.apache.streampark.spark.client.bean.CancelRequest" -> "cancel"

  def submit(submitRequest: SubmitRequest): SubmitResponse = {
    proxy[SubmitResponse](submitRequest, submitRequest.sparkVersion, SUBMIT_REQUEST)
  }

  def cancel(stopRequest: CancelRequest): CancelResponse = {
    proxy[CancelResponse](stopRequest, stopRequest.sparkVersion, CANCEL_REQUEST)
  }

  private[this] def proxy[T: ClassTag](
      request: Object,
      sparkVersion: SparkVersion,
      requestBody: (String, String)): T = {
    sparkVersion.checkVersion()
    SparkShimsProxy.proxy(
      sparkVersion,
      (classLoader: ClassLoader) => {
        val submitClass = classLoader.loadClass(SPARK_CLIENT_ENDPOINT_CLASS)
        val requestClass = classLoader.loadClass(requestBody._1)
        val method = submitClass.getDeclaredMethod(requestBody._2, requestClass)
        method.setAccessible(true)
        val obj =
          method.invoke(null, SparkShimsProxy.getObject(classLoader, request))
        if (obj == null) null.asInstanceOf[T]
        else {
          SparkShimsProxy.getObject[T](this.getClass.getClassLoader, obj)
        }
      })
  }
}
