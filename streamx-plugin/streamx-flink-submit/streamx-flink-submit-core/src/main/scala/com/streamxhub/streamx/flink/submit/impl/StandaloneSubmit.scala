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
import com.streamxhub.streamx.common.enums.{DevelopmentMode, ExecutionMode}
import com.streamxhub.streamx.common.util.StandaloneUtils
import com.streamxhub.streamx.flink.packer.pipeline.FlinkStandaloneBuildResponse
import com.streamxhub.streamx.flink.submit.FlinkSubmitter
import com.streamxhub.streamx.flink.submit.`trait`.FlinkSubmitTrait
import com.streamxhub.streamx.flink.submit.bean.{StopRequest, StopResponse, SubmitRequest, SubmitResponse}
import com.streamxhub.streamx.flink.submit.tool.FlinkSessionSubmitHelper
import org.apache.flink.api.common.JobID
import org.apache.flink.client.deployment.application.ApplicationConfiguration
import org.apache.flink.client.deployment.{DefaultClusterClientServiceLoader, StandaloneClusterDescriptor, StandaloneClusterId}
import org.apache.flink.client.program.{ClusterClient, PackagedProgram, PackagedProgramUtils}
import org.apache.flink.configuration._
import org.apache.flink.util.IOUtils

import java.io.File
import scala.collection.JavaConversions._
import scala.util.{Failure, Success, Try}


/**
 * Submit Job to Remote Session Cluster
 */
object StandaloneSubmit extends FlinkSubmitTrait {

  /**
   * @param submitRequest
   * @param flinkConfig
   */
  override def doConfig(submitRequest: SubmitRequest, flinkConfig: Configuration): Unit = {
    flinkConfig
      .safeSet(PipelineOptions.NAME, Try(submitRequest.effectiveAppName).getOrElse(null))
      .safeSet(
        RestOptions.ADDRESS,
        Try(flinkConfig.get(JobManagerOptions.ADDRESS)).getOrElse {
          logWarn(s"RestOptions Address is not set,use default value : ${StandaloneUtils.DEFAULT_REST_ADDRESS}")
          StandaloneUtils.DEFAULT_REST_ADDRESS
        })
      .safeSet(
        RestOptions.PORT,
        Try(flinkConfig.get(RestOptions.PORT)).getOrElse {
          logWarn(s"RestOptions port is not set,use default value : ${StandaloneUtils.DEFAULT_REST_PORT}")
          StandaloneUtils.DEFAULT_REST_PORT
        })

    logInfo(
      s"""
         |------------------------------------------------------------------
         |Effective submit configuration: $flinkConfig
         |------------------------------------------------------------------
         |""".stripMargin)
  }

  override def doSubmit(submitRequest: SubmitRequest, flinkConfig: Configuration): SubmitResponse = {
    // 1) get userJar
    val userJar = submitRequest.developmentMode match {
      case DevelopmentMode.FLINKSQL =>
        checkBuildResult(submitRequest)
        // 1) get build result
        val buildResult = submitRequest.buildResult.asInstanceOf[FlinkStandaloneBuildResponse]
        // 2) get fat-jar
        new File(buildResult.flinkShadedJarPath)
      case _ => new File(submitRequest.flinkUserJar)
    }

    // 2) submit job
    Try(restApiSubmitPlan(submitRequest, flinkConfig, userJar))
      .recover {
        case _ =>
          logInfo(s"[flink-submit] Rest API Submit Plan failed, try Submit Plan  now.")
          jobGraphSubmitPlan(submitRequest, flinkConfig, userJar)
      } match {
      case Success(submitResponse) => submitResponse
      case Failure(ex) => throw ex
    }
  }

  override def doStop(stopRequest: StopRequest): StopResponse = {

    val flinkConfig = new Configuration()

    this.doConfig(null, flinkConfig)
    //get standalone jm to dynamicOption
    FlinkSubmitter.extractDynamicOption(stopRequest.dynamicOption).foreach(e => flinkConfig.setString(e._1, e._2))

    flinkConfig.safeSet(DeploymentOptions.TARGET, ExecutionMode.STANDALONE.getName)

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
  @throws[Exception] private def restApiSubmitPlan(submitRequest: SubmitRequest, flinkConfig: Configuration, fatJar: File): SubmitResponse = {
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
  @throws[Exception] private def jobGraphSubmitPlan(submitRequest: SubmitRequest, flinkConfig: Configuration, fatJar: File): SubmitResponse = {
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
        .setSavepointRestoreSettings(submitRequest.savepointRestoreSettings)
        .setArguments(
          flinkConfig
            .getOptional(ApplicationConfiguration.APPLICATION_ARGS)
            .orElse(Lists.newArrayList()): _*
        ).build()

      val jobGraph = PackagedProgramUtils.createJobGraph(
        packageProgram,
        flinkConfig,
        flinkConfig.getInteger(CoreOptions.DEFAULT_PARALLELISM),
        null,
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
