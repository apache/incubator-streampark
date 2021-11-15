/*
 * Copyright (c) 2021 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.streamxhub.streamx.flink.submit.impl

import com.google.common.collect.Lists
import com.streamxhub.streamx.common.enums.{DevelopmentMode, ExecutionMode}
import com.streamxhub.streamx.common.fs.FsOperator
import com.streamxhub.streamx.common.util.DateUtils.fullCompact
import com.streamxhub.streamx.common.util.{DateUtils, Logger}
import com.streamxhub.streamx.flink.kubernetes.KubernetesRetriever
import com.streamxhub.streamx.flink.kubernetes.enums.FlinkK8sExecuteMode
import com.streamxhub.streamx.flink.kubernetes.model.ClusterKey
import com.streamxhub.streamx.flink.packer.maven.MavenTool
import com.streamxhub.streamx.flink.submit.`trait`.KubernetesNativeSubmitTrait
import com.streamxhub.streamx.flink.submit.domain._
import com.streamxhub.streamx.flink.submit.tool.FlinkSessionSubmitHelper
import org.apache.commons.lang3.StringUtils
import org.apache.flink.api.common.JobID
import org.apache.flink.client.deployment.application.ApplicationConfiguration
import org.apache.flink.client.program.{ClusterClient, PackagedProgram, PackagedProgramUtils}
import org.apache.flink.configuration._
import org.apache.flink.kubernetes.KubernetesClusterDescriptor
import org.apache.flink.kubernetes.configuration.KubernetesConfigOptions
import org.apache.flink.util.IOUtils

import java.io.File
import scala.collection.JavaConversions._
import scala.util.Try

/**
 * kubernetes native session mode submit
 */
object KubernetesNativeSessionSubmit extends KubernetesNativeSubmitTrait with Logger {

  // noinspection DuplicatedCode
  override def doSubmit(submitRequest: SubmitRequest): SubmitResponse = {
    // require parameters
    assert(Try(submitRequest.k8sSubmitParam.clusterId.nonEmpty).getOrElse(false))

    val jobID = {
      if (StringUtils.isNotBlank(submitRequest.jobID)) new JobID()
      else JobID.fromHexString(submitRequest.jobID)
    }
    // extract flink configuration
    val flinkConfig = extractEffectiveFlinkConfig(submitRequest)

    // sub workspace dir like: APP_WORKSPACE/k8s-clusterId@k8s-namespace/job-name/
    // in streamx, flink job-name under the specified clusterId/namespace must be unique.
    val buildWorkspace = s"${workspace.APP_WORKSPACE}" +
      s"/${flinkConfig.getString(KubernetesConfigOptions.CLUSTER_ID)}@${flinkConfig.getString(KubernetesConfigOptions.NAMESPACE)}" +
      s"/${flinkConfig.getString(PipelineOptions.NAME)}"
    FsOperator.lfs.delete(buildWorkspace)
    FsOperator.lfs.mkdirs(buildWorkspace)

    // build fat-jar, output file name: streamx-flinkjob_<job-name>_<timespamp>, like: streamx-flinkjob_myjobtest_20211024134822
    val fatJar = {
      val fatJarOutputPath = s"$buildWorkspace/streamx-flinkjob_${flinkConfig.getString(PipelineOptions.NAME)}_${DateUtils.now(fullCompact)}.jar"
      submitRequest.developmentMode match {
        case DevelopmentMode.FLINKSQL =>
          val flinkLibs = extractProvidedLibs(submitRequest)
          val jarPackDeps = submitRequest.k8sSubmitParam.jarPackDeps
          MavenTool.buildFatJar(jarPackDeps.merge(flinkLibs), fatJarOutputPath)
        case DevelopmentMode.CUSTOMCODE =>
          val providedLibs = Set(
            workspace.APP_JARS,
            workspace.APP_PLUGINS,
            submitRequest.flinkUserJar
          )
          val jarPackDeps = submitRequest.k8sSubmitParam.jarPackDeps
          MavenTool.buildFatJar(jarPackDeps.merge(providedLibs), fatJarOutputPath)
      }
    }
    logInfo(s"[flink-submit] already built flink job fat-jar. " +
      s"${flinkConfIdentifierInfo(flinkConfig)}, fatJarPath=${fatJar.getAbsolutePath}")

    // rest api submit plan
    restApiSubmitPlan(submitRequest, flinkConfig, fatJar)

    // old submit plan
    // jobGraphSubmitPlan(submitRequest, flinkConfig, jobID, fatJar)
  }

  /**
   * Submit flink session job via rest api.
   */
  // noinspection DuplicatedCode
  @throws[Exception]
  private def restApiSubmitPlan(submitRequest: SubmitRequest, flinkConfig: Configuration, fatJar: File): SubmitResponse = {
    try {
      // get jm rest url of flink session cluster
      val clusterKey = ClusterKey(FlinkK8sExecuteMode.SESSION,
        submitRequest.k8sSubmitParam.kubernetesNamespace, submitRequest.k8sSubmitParam.clusterId)
      val jmRestUrl = KubernetesRetriever.retrieveFlinkRestUrl(clusterKey)
        .getOrElse(throw new Exception(s"[flink-submit] retrieve flink session rest url failed, clusterKey=$clusterKey"))
      // submit job via rest api
      val jobId = FlinkSessionSubmitHelper.submitViaRestApi(jmRestUrl, fatJar, flinkConfig)
      SubmitResponse(clusterKey.clusterId, flinkConfig.toMap, jobId)
    } catch {
      case e: Exception =>
        logError(s"submit flink job fail in ${submitRequest.executionMode} mode")
        e.printStackTrace()
        throw e
    }
  }

  /**
   * Submit flink session job with building JobGraph via ClusterClient api.
   */
  // noinspection DuplicatedCode
  @throws[Exception]
  private def jobGraphSubmitPlan(submitRequest: SubmitRequest, flinkConfig: Configuration, jobID: JobID, fatJar: File): SubmitResponse = {
    // retrieve k8s cluster and submit flink job on session mode
    var clusterDescriptor: KubernetesClusterDescriptor = null
    var packageProgram: PackagedProgram = null
    var client: ClusterClient[String] = null
    try {
      clusterDescriptor = getK8sClusterDescriptor(flinkConfig)
      // build JobGraph
      packageProgram = PackagedProgram.newBuilder()
        .setJarFile(fatJar)
        .setConfiguration(flinkConfig)
        .setEntryPointClassName(flinkConfig.get(ApplicationConfiguration.APPLICATION_MAIN_CLASS))
        .setArguments(flinkConfig.getOptional(ApplicationConfiguration.APPLICATION_ARGS)
          .orElse(Lists.newArrayList())
          : _*
        ).build()
      val jobGraph = PackagedProgramUtils.createJobGraph(
        packageProgram,
        flinkConfig,
        flinkConfig.getInteger(CoreOptions.DEFAULT_PARALLELISM),
        jobID,
        false)
      // retrieve client and submit JobGraph
      client = clusterDescriptor.retrieve(flinkConfig.getString(KubernetesConfigOptions.CLUSTER_ID)).getClusterClient
      val submitResult = client.submitJob(jobGraph)
      val jobId = submitResult.get().toString
      val result = SubmitResponse(client.getClusterId, flinkConfig.toMap, jobId)
      logInfo(s"[flink-submit] flink job has been submitted. ${flinkConfIdentifierInfo(flinkConfig)}, jobId=${jobID.toString}")
      result
    } catch {
      case e: Exception =>
        logError(s"submit flink job fail in ${submitRequest.executionMode} mode")
        e.printStackTrace()
        throw e
    } finally {
      // ref FLINK-21164 FLINK-9844 packageProgram.close()
      // must be flink 1.12.2 and above
      IOUtils.closeAll(client, packageProgram, clusterDescriptor)
    }
  }

  override def doStop(stopInfo: StopRequest): StopResponse = {
    super.doStop(ExecutionMode.KUBERNETES_NATIVE_SESSION, stopInfo)
  }

}
