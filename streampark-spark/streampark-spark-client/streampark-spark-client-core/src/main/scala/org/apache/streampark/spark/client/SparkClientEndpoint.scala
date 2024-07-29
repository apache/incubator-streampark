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

import org.apache.streampark.common.enums.SparkExecutionMode
import org.apache.streampark.common.enums.SparkExecutionMode._
import org.apache.streampark.spark.client.`trait`.SparkClientTrait
import org.apache.streampark.spark.client.bean._
import org.apache.streampark.spark.client.impl._

object SparkClientEndpoint {

  private[this] val clients: Map[SparkExecutionMode, SparkClientTrait] = Map(
    YARN_CLUSTER -> YarnClient,
    YARN_CLIENT -> YarnClient)

  def submit(submitRequest: SubmitRequest): SubmitResponse = {
    clients.get(submitRequest.executionMode) match {
      case Some(client) => client.submit(submitRequest)
      case _ =>
        throw new UnsupportedOperationException(
          s"Unsupported ${submitRequest.executionMode} spark submit.")
    }
  }

  def stop(stopRequest: StopRequest): StopResponse = {
    clients.get(stopRequest.executionMode) match {
      case Some(client) => client.stop(stopRequest)
      case _ =>
        throw new UnsupportedOperationException(
          s"Unsupported ${stopRequest.executionMode} spark stop.")
    }
  }

}
