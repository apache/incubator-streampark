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

import com.google.common.collect.Lists
import com.streamxhub.streamx.common.enums.ExecutionMode
import com.streamxhub.streamx.flink.packer.pipeline.FlinkStandaloneBuildResponse
import com.streamxhub.streamx.flink.submit.FlinkSubmitHelper.extractDynamicOption
import com.streamxhub.streamx.flink.submit.`trait`.StandaloneSubmitTrait
import com.streamxhub.streamx.flink.submit.domain.{StopRequest, StopResponse, SubmitRequest, SubmitResponse}
import com.streamxhub.streamx.flink.submit.tool.FlinkSessionSubmitHelper
import org.apache.flink.api.common.JobID
import org.apache.flink.client.deployment.application.ApplicationConfiguration
import org.apache.flink.client.deployment.{StandaloneClusterDescriptor, StandaloneClusterId}
import org.apache.flink.client.program.{ClusterClient, PackagedProgram, PackagedProgramUtils}
import org.apache.flink.configuration._
import org.apache.flink.util.IOUtils

import java.io.File
import scala.collection.JavaConversions._


/**
 * Submit Job to Remote Session Cluster
 */
object StandaloneSubmit extends StandaloneSubmitTrait {

  override def doSubmit(submitRequest: SubmitRequest): SubmitResponse = {
    // require parameters with standalone remote
    val flinkConfig = extractEffectiveFlinkConfig(submitRequest)
    // get build result
    val buildResult = submitRequest.buildResult.asInstanceOf[FlinkStandaloneBuildResponse]
    // build fat-jar
    val fatJar = new File(buildResult.flinkShadedJarPath)
    // new submit plan with rest api
    restApiSubmitPlan(submitRequest, flinkConfig, fatJar)

    // old submit plan
    // jobGraphSubmitPlan(submitRequest, flinkConfig, fatJar)
  }

  override def doStop(stopRequest: StopRequest): StopResponse = {
    val flinkConfig = new Configuration()
    //get standalone jm to dynamicOption
    extractDynamicOption(stopRequest.dynamicOption)
      .foreach(e => flinkConfig.setString(e._1, e._2))
    flinkConfig.set(DeploymentOptions.TARGET, ExecutionMode.STANDALONE.getName)
    checkAndReplaceRestOptions(flinkConfig)
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
  @throws[Exception]
  private def restApiSubmitPlan(submitRequest: SubmitRequest, flinkConfig: Configuration, fatJar: File): SubmitResponse = {
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
        logError(s"submit flink job fail in ${submitRequest.executionMode} with standalone mode")
        e.printStackTrace()
        throw e
    }
  }


  /**
   * Submit flink session job with building JobGraph via Standalone ClusterClient api.
   */
  // noinspection DuplicatedCode
  @throws[Exception]
  private def jobGraphSubmitPlan(submitRequest: SubmitRequest, flinkConfig: Configuration, fatJar: File): SubmitResponse = {
    // retrieve standalone session cluster and submit flink job on session mode
    var clusterDescriptor: StandaloneClusterDescriptor = null;
    var packageProgram: PackagedProgram = null
    var client: ClusterClient[StandaloneClusterId] = null
    try {
      val standAloneDescriptor = getStandAloneClusterDescriptor(flinkConfig)
      clusterDescriptor = standAloneDescriptor._2
      // build JobGraph
      packageProgram = PackagedProgram.newBuilder()
        .setJarFile(fatJar)
        .setConfiguration(flinkConfig)
        .setEntryPointClassName(flinkConfig.get(ApplicationConfiguration.APPLICATION_MAIN_CLASS))
        .setArguments(flinkConfig.getOptional(ApplicationConfiguration.APPLICATION_ARGS)
          .orElse(Lists.newArrayList())
          : _*)
        .build()

      val jobGraph = PackagedProgramUtils.createJobGraph(
        packageProgram,
        flinkConfig,
        flinkConfig.getInteger(CoreOptions.DEFAULT_PARALLELISM),
        false)

      client = clusterDescriptor.retrieve(standAloneDescriptor._1).getClusterClient
      val submitResult = client.submitJob(jobGraph)
      val jobId = submitResult.get().toString
      val result = SubmitResponse(jobId, flinkConfig.toMap, jobId)
      result
    } catch {
      case e: Exception =>
        logError(s"submit flink job fail in ${submitRequest.executionMode} mode")
        e.printStackTrace()
        throw e
    } finally {
      IOUtils.closeAll(client, packageProgram, clusterDescriptor)
    }
  }
}
