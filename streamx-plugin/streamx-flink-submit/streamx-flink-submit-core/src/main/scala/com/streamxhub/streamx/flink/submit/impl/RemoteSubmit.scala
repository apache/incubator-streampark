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
import com.streamxhub.streamx.flink.packer.pipeline.FlinkRemoteBuildResponse
import com.streamxhub.streamx.flink.submit.`trait`.FlinkSubmitTrait
import com.streamxhub.streamx.flink.submit.bean.{StopRequest, StopResponse, SubmitRequest, SubmitResponse}
import com.streamxhub.streamx.flink.submit.tool.FlinkSessionSubmitHelper
import org.apache.flink.api.common.JobID
import org.apache.flink.client.deployment.{DefaultClusterClientServiceLoader, StandaloneClusterDescriptor, StandaloneClusterId}
import org.apache.flink.client.program.{ClusterClient, PackagedProgram, PackagedProgramUtils}
import org.apache.flink.configuration._
import org.apache.flink.util.IOUtils

import java.io.File
import java.lang.{Integer => JavaInt}


/**
 * Submit Job to Remote Session Cluster
 */
object RemoteSubmit extends FlinkSubmitTrait {

  /**
   * @param submitRequest
   * @param flinkConfig
   */
  override def setConfig(submitRequest: SubmitRequest, flinkConfig: Configuration): Unit = {
    flinkConfig
      .safeSet(PipelineOptions.NAME, submitRequest.effectiveAppName)
      .safeSet(RestOptions.ADDRESS, submitRequest.extraParameter.get(RestOptions.ADDRESS.key()).toString)
      .safeSet(RestOptions.PORT, JavaInt.valueOf(submitRequest.extraParameter.get(RestOptions.PORT.key()).toString))
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
        checkBuildResult(submitRequest)
        // 1) get build result
        val buildResult = submitRequest.buildResult.asInstanceOf[FlinkRemoteBuildResponse]
        // 2) get fat-jar
        new File(buildResult.flinkShadedJarPath)
      case _ => new File(submitRequest.flinkUserJar)
    }

    // 2) submit job
    super.trySubmit(submitRequest, flinkConfig, jarFile)(restApiSubmit)(jobGraphSubmit)

  }

  override def doStop(stopRequest: StopRequest): StopResponse = {
    val flinkConfig = new Configuration()
    flinkConfig
      .safeSet(DeploymentOptions.TARGET, stopRequest.executionMode.getName)
      .safeSet(RestOptions.ADDRESS, stopRequest.extraParameter.get(RestOptions.ADDRESS.key()).toString)
      .safeSet(RestOptions.PORT, JavaInt.valueOf(stopRequest.extraParameter.get(RestOptions.PORT.key()).toString))
    logInfo(
      s"""
         |------------------------------------------------------------------
         |Effective submit configuration: $flinkConfig
         |------------------------------------------------------------------
         |""".stripMargin)

    val standAloneDescriptor = getStandAloneClusterDescriptor(flinkConfig)
    var client: ClusterClient[StandaloneClusterId] = null
    try {
      client = standAloneDescriptor._2.retrieve(standAloneDescriptor._1).getClusterClient
      val jobID = JobID.fromHexString(stopRequest.jobId)
      val savePointDir = stopRequest.customSavePointPath
      val actionResult = (stopRequest.withSavePoint, stopRequest.withDrain) match {
        case (true, true) if savePointDir.nonEmpty => client.stopWithSavepoint(jobID, true, savePointDir).get()
        case (true, false) if savePointDir.nonEmpty => client.cancelWithSavepoint(jobID, savePointDir).get()
        case _ => client.cancel(jobID).get()
          ""
      }
      StopResponse(actionResult)
    } catch {
      case e: Exception =>
        logError(s"stop flink standalone job fail")
        e.printStackTrace()
        throw e
    } finally {
      if (client != null) client.close()
      if (standAloneDescriptor != null) standAloneDescriptor._2.close()
    }
  }

  /**
   * Submit flink session job via rest api.
   */
  // noinspection DuplicatedCode
  @throws[Exception] def restApiSubmit(submitRequest: SubmitRequest, flinkConfig: Configuration, fatJar: File): SubmitResponse = {
    // retrieve standalone session cluster and submit flink job on session mode
    var clusterDescriptor: StandaloneClusterDescriptor = null;
    var client: ClusterClient[StandaloneClusterId] = null
    try {
      val standAloneDescriptor = getStandAloneClusterDescriptor(flinkConfig)
      clusterDescriptor = standAloneDescriptor._2
      client = clusterDescriptor.retrieve(standAloneDescriptor._1).getClusterClient
      logInfo(s"standalone submit WebInterfaceURL ${client.getWebInterfaceURL}")
      val jobId = FlinkSessionSubmitHelper.submitViaRestApi(client.getWebInterfaceURL, fatJar, flinkConfig)
      SubmitResponse(jobId, flinkConfig.toMap, jobId)
    } catch {
      case e: Exception =>
        logError(s"submit flink job fail in standalone mode")
        e.printStackTrace()
        throw e
    }
  }


  /**
   * Submit flink session job with building JobGraph via Standalone ClusterClient api.
   */
  // noinspection DuplicatedCode
  @throws[Exception] def jobGraphSubmit(submitRequest: SubmitRequest, flinkConfig: Configuration, jarFile: File): SubmitResponse = {
    // retrieve standalone session cluster and submit flink job on session mode
    var clusterDescriptor: StandaloneClusterDescriptor = null;
    var packageProgram: PackagedProgram = null
    var client: ClusterClient[StandaloneClusterId] = null
    try {
      val standAloneDescriptor = getStandAloneClusterDescriptor(flinkConfig)
      clusterDescriptor = standAloneDescriptor._2
      // build JobGraph
      packageProgram = super.getPackageProgram(flinkConfig, submitRequest, jarFile)

      val jobGraph = PackagedProgramUtils.createJobGraph(
        packageProgram,
        flinkConfig,
        getParallelism(submitRequest),
        null,
        false
      )

      client = clusterDescriptor.retrieve(standAloneDescriptor._1).getClusterClient
      val jobId = client.submitJob(jobGraph).get().toString
      val result = SubmitResponse(jobId, flinkConfig.toMap, jobId)
      result
    } catch {
      case e: Exception =>
        logError(s"submit flink job fail in ${submitRequest.executionMode} mode")
        e.printStackTrace()
        throw e
    } finally {
      IOUtils.closeAll(client, clusterDescriptor)
    }
  }

  /**
   * create StandAloneClusterDescriptor
   *
   * @param flinkConfig
   */
  def getStandAloneClusterDescriptor(flinkConfig: Configuration): (StandaloneClusterId, StandaloneClusterDescriptor) = {
    val serviceLoader = new DefaultClusterClientServiceLoader
    val clientFactory = serviceLoader.getClusterClientFactory(flinkConfig)
    val standaloneClusterId: StandaloneClusterId = clientFactory.getClusterId(flinkConfig)
    val standaloneClusterDescriptor = clientFactory.createClusterDescriptor(flinkConfig).asInstanceOf[StandaloneClusterDescriptor]
    (standaloneClusterId, standaloneClusterDescriptor)
  }

  @throws[Exception] private[this] def checkBuildResult(submitRequest: SubmitRequest): Unit = {
    val result = submitRequest.buildResult
    if (result == null) {
      throw new Exception(s"[flink-submit] current job: ${submitRequest.effectiveAppName} was not yet built, buildResult is empty")
    }
    if (!result.pass) {
      throw new Exception(s"[flink-submit] current job ${submitRequest.effectiveAppName} build failed, please check")
    }
  }

}
