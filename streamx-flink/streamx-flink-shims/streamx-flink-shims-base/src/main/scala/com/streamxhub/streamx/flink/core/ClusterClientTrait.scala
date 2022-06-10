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

import org.apache.flink.api.common.{JobID, JobStatus}
import org.apache.flink.client.program.ClusterClient
import org.apache.flink.configuration.Configuration
import org.apache.flink.runtime.client.JobStatusMessage
import org.apache.flink.runtime.jobgraph.{JobGraph, OperatorID}
import org.apache.flink.runtime.jobmaster.JobResult
import org.apache.flink.runtime.messages.Acknowledge
import org.apache.flink.runtime.operators.coordination.{CoordinationRequest, CoordinationResponse}

import java.util
import java.util.concurrent.CompletableFuture

abstract class ClusterClientTrait[T](clusterClient: ClusterClient[T]) extends ClusterClient[T] {

  override def close(): Unit = clusterClient.close()

  override def getClusterId = clusterClient.getClusterId

  override def getFlinkConfiguration: Configuration = clusterClient.getFlinkConfiguration

  override def shutDownCluster(): Unit = clusterClient.shutDownCluster()

  override def getWebInterfaceURL: String = clusterClient.getWebInterfaceURL

  override def listJobs(): CompletableFuture[util.Collection[JobStatusMessage]] = clusterClient.listJobs()

  override def disposeSavepoint(s: String): CompletableFuture[Acknowledge] = clusterClient.disposeSavepoint(s)

  override def submitJob(jobGraph: JobGraph): CompletableFuture[JobID] = clusterClient.submitJob(jobGraph)

  override def getJobStatus(jobID: JobID): CompletableFuture[JobStatus] = clusterClient.getJobStatus(jobID)

  override def requestJobResult(jobID: JobID): CompletableFuture[JobResult] = clusterClient.requestJobResult(jobID)

  override def getAccumulators(jobID: JobID, classLoader: ClassLoader): CompletableFuture[util.Map[String, AnyRef]] = clusterClient.getAccumulators(jobID, classLoader)

  override def cancel(jobID: JobID): CompletableFuture[Acknowledge] = clusterClient.cancel(jobID)

  override def sendCoordinationRequest(jobID: JobID, operatorID: OperatorID, coordinationRequest: CoordinationRequest): CompletableFuture[CoordinationResponse] = clusterClient.sendCoordinationRequest(jobID, operatorID, coordinationRequest)
}

