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
import com.streamxhub.streamx.flink.packer.pipeline.FlinkK8sApplicationBuildResponse
import com.streamxhub.streamx.flink.submit.`trait`.KubernetesNativeSubmitTrait
import com.streamxhub.streamx.flink.submit.domain._
import org.apache.flink.client.deployment.application.ApplicationConfiguration
import org.apache.flink.client.program.ClusterClient
import org.apache.flink.configuration.{DeploymentOptionsInternal, PipelineOptions}
import org.apache.flink.kubernetes.KubernetesClusterDescriptor
import org.apache.flink.kubernetes.configuration.KubernetesConfigOptions
import org.apache.flink.util.IOUtils

import scala.util.Try

/**
 * kubernetes native application mode submit
 */
object KubernetesNativeApplicationSubmit extends KubernetesNativeSubmitTrait {

  // noinspection DuplicatedCode
  @throws[Exception]
  override def doSubmit(submitRequest: SubmitRequest): SubmitResponse = {
    // require parameters
    assert(Try(submitRequest.k8sSubmitParam.clusterId.nonEmpty).getOrElse(false))

    // check the last building result
    checkBuildResult(submitRequest)
    val buildResult = submitRequest.buildResult.asInstanceOf[FlinkK8sApplicationBuildResponse]

    // extract flink config
    val flinkConfig = extractEffectiveFlinkConfig(submitRequest)

    // step-2: build fat-jar, output file name: _streamx-flinkjob_<jobamme>.jar, like "_streamx-flinkjob_myjob-test.jar"
    val fatJar = {
      val fatJarOutputPath = s"$buildWorkspace/_streamx-flinkjob_${flinkConfig.getString(PipelineOptions.NAME)}.jar"
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

    logInfo(s"[flink-submit] start building flink job docker image. ${flinkConfIdentifierInfo(flinkConfig)}")

    // step-3: build and push flink application image
    val dockerAuthConfig = submitRequest.k8sSubmitParam.dockerAuthConfig
    val flinkBaseImage = submitRequest.k8sSubmitParam.flinkBaseImage
    val dockerFileTemplate = new FlinkDockerfileTemplate(flinkBaseImage, fatJar.getAbsolutePath)
    val tagName = s"flinkjob-${submitRequest.k8sSubmitParam.clusterId}"
    // add flink pipeline.jars configuration
    flinkConfig.set(PipelineOptions.JARS, Lists.newArrayList(buildResult.dockerInnerMainJarPath))
    // add flink conf configuration, mainly to set the log4j configuration
    if (!flinkConfig.contains(DeploymentOptionsInternal.CONF_DIR)) {
      flinkConfig.set(DeploymentOptionsInternal.CONF_DIR, s"${submitRequest.flinkVersion.flinkHome}/conf")
    }
    // add flink container image tag to flink configuration
    flinkConfig.set(KubernetesConfigOptions.CONTAINER_IMAGE, buildResult.flinkImageTag)

    // retrieve k8s cluster and submit flink job on application mode
    var clusterDescriptor: KubernetesClusterDescriptor = null
    var clusterClient: ClusterClient[String] = null

    try {
      val (descriptor, clusterSpecification) = getK8sClusterDescriptorAndSpecification(flinkConfig)
      clusterDescriptor = descriptor
      val applicationConfig = ApplicationConfiguration.fromConfiguration(flinkConfig)
      clusterClient = clusterDescriptor
        .deployApplicationCluster(clusterSpecification, applicationConfig)
        .getClusterClient

      val clusterId = clusterClient.getClusterId
      val result = SubmitResponse(clusterId, flinkConfig.toMap)
      logInfo(s"[flink-submit] flink job has been submitted. ${flinkConfIdentifierInfo(flinkConfig)}")
      result

    } catch {
      case e: Exception =>
        logError(s"submit flink job fail in ${submitRequest.executionMode} mode")
        throw e
    } finally {
      IOUtils.closeAll(clusterClient, clusterDescriptor)
    }
  }

  override def doStop(stopInfo: StopRequest): StopResponse = {
    super.doStop(ExecutionMode.KUBERNETES_NATIVE_APPLICATION, stopInfo)
  }

}
