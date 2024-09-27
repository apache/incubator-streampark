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
import org.apache.streampark.flink.client.bean.{CancelRequest, CancelResponse, SavepointRequestTrait, SavepointResponse, SubmitRequest, SubmitResponse, TriggerSavepointRequest}
import org.apache.streampark.flink.client.tool.FlinkSessionSubmitHelper

import org.apache.flink.api.common.JobID
import org.apache.flink.client.deployment.{DefaultClusterClientServiceLoader, StandaloneClusterDescriptor, StandaloneClusterId}
import org.apache.flink.client.program.ClusterClient
import org.apache.flink.configuration._

import java.io.File
import java.lang.{Integer => JavaInt}

/** Submit Job to Remote Cluster */
object RemoteClient extends FlinkClientTrait {

  override def setConfig(submitRequest: SubmitRequest, flinkConfig: Configuration): Unit = {}

  override def doSubmit(
      submitRequest: SubmitRequest,
      flinkConfig: Configuration): SubmitResponse = {

    // 2) submit job
    super.trySubmit(submitRequest, flinkConfig, submitRequest.userJarFile)(
      jobGraphSubmit,
      restApiSubmit)
  }

  override def doCancel(
      cancelRequest: CancelRequest,
      flinkConfig: Configuration): CancelResponse = {
    executeClientAction(
      cancelRequest,
      flinkConfig,
      (jobID, clusterClient) => {
        CancelResponse(super.cancelJob(cancelRequest, jobID, clusterClient))
      })
  }

  private[this] def executeClientAction[O, R <: SavepointRequestTrait](
      request: R,
      flinkConfig: Configuration,
      actFunc: (JobID, ClusterClient[_]) => O): O = {
    var client: ClusterClient[StandaloneClusterId] = null
    var standAloneDescriptor: (StandaloneClusterId, StandaloneClusterDescriptor) = null
    try {
      flinkConfig
        .safeSet(DeploymentOptions.TARGET, request.deployMode.getName)
        .safeSet(RestOptions.ADDRESS, request.properties.get(RestOptions.ADDRESS.key()).toString)
        .safeSet[JavaInt](
          RestOptions.PORT,
          request.properties.get(RestOptions.PORT.key()).toString.toInt)
      logInfo(s"""
                 |------------------------------------------------------------------
                 |Effective submit configuration: $flinkConfig
                 |------------------------------------------------------------------
                 |""".stripMargin)
      standAloneDescriptor = getStandAloneClusterDescriptor(flinkConfig)
      client = standAloneDescriptor._2
        .retrieve(standAloneDescriptor._1)
        .getClusterClient
      actFunc(JobID.fromHexString(request.jobId), client)
    } catch {
      case e: Exception =>
        logError(s"Do ${request.getClass.getSimpleName} for flink standalone job fail")
        e.printStackTrace()
        throw e
    } finally {
      if (client != null) client.close()
      if (standAloneDescriptor != null) standAloneDescriptor._2.close()
    }
  }

  override def doTriggerSavepoint(
      savepointRequest: TriggerSavepointRequest,
      flinkConfig: Configuration): SavepointResponse = {
    executeClientAction(
      savepointRequest,
      flinkConfig,
      (jobID, clusterClient) => {
        SavepointResponse(super.triggerSavepoint(savepointRequest, jobID, clusterClient))
      })
  }

  /** Submit flink session job via rest api. */
  // noinspection DuplicatedCode
  @throws[Exception]
  def restApiSubmit(
      submitRequest: SubmitRequest,
      flinkConfig: Configuration,
      fatJar: File): SubmitResponse = {
    // retrieve standalone session cluster and submit flink job on session mode
    var clusterDescriptor: StandaloneClusterDescriptor = null;
    var client: ClusterClient[StandaloneClusterId] = null
    val standAloneDescriptor = getStandAloneClusterDescriptor(flinkConfig)
    val yarnClusterId: StandaloneClusterId = standAloneDescriptor._1
    clusterDescriptor = standAloneDescriptor._2

    client = clusterDescriptor.retrieve(yarnClusterId).getClusterClient
    val jobId =
      FlinkSessionSubmitHelper.submitViaRestApi(client.getWebInterfaceURL, fatJar, flinkConfig)
    logInfo(
      s"${submitRequest.deployMode} mode submit by restApi, WebInterfaceURL ${client.getWebInterfaceURL}, jobId: $jobId")
    val resp =
      SubmitResponse(null, flinkConfig.toMap, jobId, client.getWebInterfaceURL)
    closeSubmit(submitRequest, client, clusterDescriptor)
    resp
  }

  /** Submit flink session job with building JobGraph via Standalone ClusterClient api. */
  @throws[Exception]
  def jobGraphSubmit(
      submitRequest: SubmitRequest,
      flinkConfig: Configuration,
      jarFile: File): SubmitResponse = {

    val standAloneDescriptor = getStandAloneClusterDescriptor(flinkConfig)
    val clusterDescriptor = standAloneDescriptor._2
    // build JobGraph
    val packageProgramJobGraph =
      super.getJobGraph(flinkConfig, submitRequest, jarFile)
    val packageProgram = packageProgramJobGraph._1
    val jobGraph = packageProgramJobGraph._2
    val client =
      clusterDescriptor.retrieve(standAloneDescriptor._1).getClusterClient
    val jobId = client.submitJob(jobGraph).get().toString
    logInfo(
      s"${submitRequest.deployMode} mode submit by jobGraph, WebInterfaceURL ${client.getWebInterfaceURL}, jobId: $jobId")
    val result =
      SubmitResponse(null, flinkConfig.toMap, jobId, client.getWebInterfaceURL)
    closeSubmit(submitRequest, packageProgram, client, clusterDescriptor)
    result
  }

  /**
   * create StandAloneClusterDescriptor
   *
   * @param flinkConfig
   */
  private[this] def getStandAloneClusterDescriptor(
      flinkConfig: Configuration): (StandaloneClusterId, StandaloneClusterDescriptor) = {
    val serviceLoader = new DefaultClusterClientServiceLoader
    val clientFactory = serviceLoader.getClusterClientFactory(flinkConfig)
    val standaloneClusterId: StandaloneClusterId =
      clientFactory.getClusterId(flinkConfig)
    val standaloneClusterDescriptor =
      clientFactory
        .createClusterDescriptor(flinkConfig)
        .asInstanceOf[StandaloneClusterDescriptor]
    (standaloneClusterId, standaloneClusterDescriptor)
  }

}
