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

package org.apache.streampark.flink.submit.impl

import org.apache.streampark.common.util.Utils
import org.apache.streampark.flink.submit.`trait`.FlinkSubmitTrait
import org.apache.streampark.flink.submit.bean._
import org.apache.flink.client.deployment.executors.RemoteExecutor
import org.apache.flink.client.program.MiniClusterClient.MiniClusterId
import org.apache.flink.client.program.{ClusterClient, MiniClusterClient, PackagedProgram}
import org.apache.flink.configuration._
import org.apache.flink.runtime.minicluster.{MiniCluster, MiniClusterConfiguration}

import java.lang.{Integer => JavaInt}

object LocalSubmit extends FlinkSubmitTrait {

  override def setConfig(submitRequest: SubmitRequest, flinkConfig: Configuration): Unit = {
    flinkConfig.safeSet(PipelineOptions.NAME, submitRequest.effectiveAppName)
    logInfo(
      s"""
         |------------------------------------------------------------------
         |Effective submit configuration: $flinkConfig
         |------------------------------------------------------------------
         |""".stripMargin)
  }

  override def doSubmit(submitRequest: SubmitRequest, flinkConfig: Configuration): SubmitResponse = {
    var packageProgram: PackagedProgram = null
    var client: ClusterClient[MiniClusterId] = null
    try {
      // build JobGraph
      val packageProgramJobGraph = super.getJobGraph(flinkConfig, submitRequest, submitRequest.userJarFile)
      packageProgram = packageProgramJobGraph._1
      val jobGraph = packageProgramJobGraph._2
      client = createLocalCluster(flinkConfig)
      val jobId = client.submitJob(jobGraph).get().toString
      SubmitResponse(jobId, flinkConfig.toMap, jobId)
    } catch {
      case e: Exception =>
        logError(s"submit flink job fail in ${submitRequest.executionMode} mode")
        e.printStackTrace()
        throw e
    } finally {
      if (submitRequest.safePackageProgram) {
        Utils.close(packageProgram)
      }
      Utils.close(client)
    }
  }

  override def doCancel(cancelRequest: CancelRequest, flinkConfig: Configuration): CancelResponse = {
    RemoteSubmit.doCancel(cancelRequest, flinkConfig)
  }

  private[this] def createLocalCluster(flinkConfig: Configuration) = {

    flinkConfig.safeSet[JavaInt](JobManagerOptions.PORT, 0)

    val cluster = {
      val numTaskManagers = flinkConfig.getInteger(
        ConfigConstants.LOCAL_NUMBER_TASK_MANAGER,
        ConfigConstants.DEFAULT_LOCAL_NUMBER_TASK_MANAGER
      )

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
