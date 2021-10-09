/*
 * Copyright (c) 2019 The StreamX Project
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
import com.streamxhub.streamx.common.enums.ExecutionMode
import com.streamxhub.streamx.common.util.Logger
import com.streamxhub.streamx.flink.packer.maven.MavenTool
import com.streamxhub.streamx.flink.submit.`trait`.KubernetesNativeSubmitTrait
import com.streamxhub.streamx.flink.submit.domain._
import org.apache.commons.lang3.StringUtils
import org.apache.flink.api.common.JobID
import org.apache.flink.client.deployment.application.ApplicationConfiguration
import org.apache.flink.client.program.{ClusterClient, PackagedProgram, PackagedProgramUtils}
import org.apache.flink.configuration._
import org.apache.flink.kubernetes.KubernetesClusterDescriptor
import org.apache.flink.kubernetes.configuration.KubernetesConfigOptions
import org.apache.flink.util.IOUtils

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

    // build fat-jar
    val fatJar = {
      // sub workspace dir like: APP_WORKSPACE/k8s-clusterId@k8s-namespace/job-name/
      // is streamx, flink job-name under the specified clusterId/namespace must be unique.
      val fatJarOutputPath = s"${workspace.APP_WORKSPACE}" +
        s"/${flinkConfig.getString(KubernetesConfigOptions.CLUSTER_ID)}@${flinkConfig.getString(KubernetesConfigOptions.NAMESPACE)}" +
        s"/${flinkConfig.getString(PipelineOptions.NAME)}/flink-job.jar"
      val flinkLibs = extractProvidedLibs(submitRequest)
      val jarPackDeps = submitRequest.k8sSubmitParam.jarPackDeps
      MavenTool.buildFatJar(jarPackDeps.merge(flinkLibs), fatJarOutputPath)
      // cache file MD5 is used to compare whether it is consistent when it is generated next time.
      //  If it is consistent, it is used directly and returned directly instead of being regenerated
      // fatJarCached.getOrElseUpdate(flinkLibs._1, MavenTool.buildFatJar(flinkLibs._2, fatJarPath))
    }
    logInfo(s"[flink-submit] already built flink job fat-jar. " +
      s"${flinkConfIdentifierInfo(flinkConfig)}, jobId=${jobID.toString}, fatJarPath=${fatJar.getAbsolutePath}")

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
      val result = SubmitResponse(client.getClusterId, flinkConfig, jobId)
      logInfo(s"[flink-submit] flink job has been submitted. ${flinkConfIdentifierInfo(flinkConfig)}, jobId=${jobID.toString}")
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

  override def doStop(stopInfo: StopRequest): StopResponse = {
    super.doStop(ExecutionMode.KUBERNETES_NATIVE_SESSION, stopInfo)
  }

}
