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
package com.streamxhub.streamx.flink.core
import org.apache.flink.api.common.JobID
import org.apache.flink.client.program.{ClusterClient => FlinkClusterClient}

import java.util.concurrent.CompletableFuture

class ClusterClient[T](clusterClient: FlinkClusterClient[T]) extends ClusterClientTrait[T](clusterClient) {

  override def cancelWithSavepoint(jobID: JobID, s: String): CompletableFuture[String] = clusterClient.cancelWithSavepoint(jobID, s)

  override def stopWithSavepoint(jobID: JobID, b: Boolean, s: String): CompletableFuture[String] = clusterClient.stopWithSavepoint(jobID, b, s)

  override def triggerSavepoint(jobID: JobID, s: String): CompletableFuture[String] = clusterClient.triggerSavepoint(jobID, s)

}
