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

package org.apache.streampark.flink.client.impl

import org.apache.streampark.flink.client.`trait`.FlinkClientTrait
import org.apache.streampark.flink.client.bean._

import org.apache.flink.client.deployment.executors.RemoteExecutor
import org.apache.flink.client.program.MiniClusterClient
import org.apache.flink.configuration._
import org.apache.flink.runtime.minicluster.{MiniCluster, MiniClusterConfiguration}

import java.lang.{Integer => JavaInt}

object LocalClient extends FlinkClientTrait {

  override def setConfig(submitRequest: SubmitRequest, flinkConfig: Configuration): Unit = {
    flinkConfig.safeSet(PipelineOptions.NAME, submitRequest.effectiveAppName)
    logInfo(s"""
               |------------------------------------------------------------------
               |Effective submit configuration: $flinkConfig
               |------------------------------------------------------------------
               |""".stripMargin)
  }

  override def doSubmit(
      submitRequest: SubmitRequest,
      flinkConfig: Configuration): SubmitResponse = {

    // build JobGraph
    val programJobGraph = super.getJobGraph(flinkConfig, submitRequest, submitRequest.userJarFile)
    val packageProgram = programJobGraph._1
    val jobGraph = programJobGraph._2
    val client = createLocalCluster(flinkConfig)
    val jobId = client.submitJob(jobGraph).get().toString
    val resp = SubmitResponse(jobId, flinkConfig.toMap, jobId, client.getWebInterfaceURL)
    closeSubmit(submitRequest, packageProgram, client)
    resp
  }

  override def doTriggerSavepoint(
      savepointRequest: TriggerSavepointRequest,
      flinkConfig: Configuration): SavepointResponse = {
    RemoteClient.doTriggerSavepoint(savepointRequest, flinkConfig)
  }

  override def doCancel(
      cancelRequest: CancelRequest,
      flinkConfig: Configuration): CancelResponse = {
    RemoteClient.doCancel(cancelRequest, flinkConfig)
  }

  private[this] def createLocalCluster(flinkConfig: Configuration) = {

    flinkConfig.safeSet[JavaInt](JobManagerOptions.PORT, 0)

    val cluster = {
      val numTaskManagers = flinkConfig.getInteger(
        ConfigConstants.LOCAL_NUMBER_TASK_MANAGER,
        ConfigConstants.DEFAULT_LOCAL_NUMBER_TASK_MANAGER)

      val numSlotsPerTaskManager = flinkConfig.getInteger(TaskManagerOptions.NUM_TASK_SLOTS)

      val miniClusterConfig = new MiniClusterConfiguration.Builder()
        .setConfiguration(flinkConfig)
        .setNumSlotsPerTaskManager(numSlotsPerTaskManager)
        .setNumTaskManagers(numTaskManagers)
        .build()

      val cluster = new MiniCluster(miniClusterConfig)
      cluster.start()
      cluster
    }

    val host = "localhost"
    val port = cluster.getRestAddress.get.getPort

    flinkConfig
      .safeSet(JobManagerOptions.ADDRESS, host)
      .safeSet[JavaInt](JobManagerOptions.PORT, port)
      .safeSet(RestOptions.ADDRESS, host)
      .safeSet[JavaInt](RestOptions.PORT, port)
      .safeSet(DeploymentOptions.TARGET, RemoteExecutor.NAME)

    logInfo(s"\nStarting local Flink cluster (host: localhost, port: $port).\n")

    new MiniClusterClient(flinkConfig, cluster)
  }

}
