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

package com.streamxhub.streamx.flink.submit.impl

import com.streamxhub.streamx.common.enums.DevelopmentMode
import com.streamxhub.streamx.common.util.Utils
import com.streamxhub.streamx.flink.packer.pipeline.ShadedBuildResponse
import com.streamxhub.streamx.flink.submit.`trait`.FlinkSubmitTrait
import com.streamxhub.streamx.flink.submit.bean._
import org.apache.flink.client.deployment.executors.RemoteExecutor
import org.apache.flink.client.program.MiniClusterClient.MiniClusterId
import org.apache.flink.client.program.{ClusterClient, MiniClusterClient, PackagedProgram, PackagedProgramUtils}
import org.apache.flink.configuration._
import org.apache.flink.runtime.minicluster.{MiniCluster, MiniClusterConfiguration}

import java.io.File
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
    // 1) get userJar
    val jarFile = submitRequest.developmentMode match {
      case DevelopmentMode.FLINKSQL =>
        submitRequest.checkBuildResult()
        // 1) get build result
        val buildResult = submitRequest.buildResult.asInstanceOf[ShadedBuildResponse]
        // 2) get fat-jar
        new File(buildResult.shadedJarPath)
      case _ => new File(submitRequest.flinkUserJar)
    }

    var packageProgram: PackagedProgram = null
    var client: ClusterClient[MiniClusterId] = null
    try {
      // build JobGraph
      packageProgram = super.getPackageProgram(flinkConfig, submitRequest, jarFile)
      val jobGraph = PackagedProgramUtils.createJobGraph(
        packageProgram,
        flinkConfig,
        getParallelism(submitRequest),
        null,
        false
      )
      client = createLocalCluster(flinkConfig)
      val jobId = client.submitJob(jobGraph).get().toString
      val result = SubmitResponse(jobId, flinkConfig.toMap, jobId)
      result
    } catch {
      case e: Exception =>
        logError(s"submit flink job fail in ${submitRequest.executionMode} mode")
        e.printStackTrace()
        throw e
    } finally {
      Utils.close(packageProgram, client)
    }
  }

  override def doStop(stopRequest: StopRequest): StopResponse = {
    RemoteSubmit.stop(stopRequest)
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
