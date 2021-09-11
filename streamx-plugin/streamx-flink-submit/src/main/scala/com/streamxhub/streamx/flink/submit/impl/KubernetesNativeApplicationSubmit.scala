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

import com.streamxhub.streamx.common.conf.ConfigConst.APP_WORKSPACE
import com.streamxhub.streamx.common.enums.ExecutionMode
import com.streamxhub.streamx.flink.packer.docker.{DockerTool, FlinkDockerfileTemplate}
import com.streamxhub.streamx.flink.packer.maven.MavenTool
import com.streamxhub.streamx.flink.submit.`trait`.KubernetesNativeSubmitTrait
import com.streamxhub.streamx.flink.submit.domain._
import org.apache.flink.client.deployment.application.ApplicationConfiguration
import org.apache.flink.client.program.ClusterClient
import org.apache.flink.configuration.PipelineOptions
import org.apache.flink.kubernetes.KubernetesClusterDescriptor
import org.apache.flink.kubernetes.configuration.KubernetesConfigOptions
import scala.collection.JavaConverters._

import scala.collection.mutable
import scala.util.Try

/**
 * kubernetes native application mode submit
 */
object KubernetesNativeApplicationSubmit extends KubernetesNativeSubmitTrait {

  //noinspection DuplicatedCode
  override def doSubmit(submitRequest: SubmitRequest): SubmitResponse = {
    // require parameters
    assert(Try(submitRequest.k8sSubmitParam.clusterId.nonEmpty).getOrElse(false))
    assert(Try(submitRequest.k8sSubmitParam.flinkDockerImage.nonEmpty).getOrElse(false))

    val buildWorkspace = s"$APP_WORKSPACE/${submitRequest.k8sSubmitParam.clusterId}/"
    // extract flink config
    val flinkConfig = extractEffectiveFlinkConfig(submitRequest)

    // build fat-jar
    val fatJar = {
      val fatJarOutputPath = buildWorkspace + "flink-job.jar"
      val flinkLibs = extractProvidedLibs(submitRequest)
      val jarPackDeps =  submitRequest.k8sSubmitParam.jarPackDeps
      MavenTool.buildFatJar(jarPackDeps.merge(flinkLibs), fatJarOutputPath)
      // cache file MD5 is used to compare whether it is consistent when it is generated next time.
      //  If it is consistent, it is used directly and returned directly instead of being regenerated
      // fatJarCached.getOrElseUpdate(flinkLibs._1, MavenTool.buildFatJar(flinkLibs._2, fatJarPath))
    }
    logInfo(s"[flink-submit] already built flink job fat-jar. " +
      s"${flinkConfIdentifierInfo(flinkConfig)}, fatJarPath=${fatJar.getAbsolutePath}")

    // build and push flink application image
    val tagName = s"flinkjob-${submitRequest.k8sSubmitParam.clusterId}"
    val dockerFileTemplate = new FlinkDockerfileTemplate(submitRequest.k8sSubmitParam.flinkDockerImage, fatJar.getAbsolutePath)
    val flinkImageTag = DockerTool.buildFlinkImage(
      submitRequest.k8sSubmitParam.dockerAuthConfig,
      buildWorkspace,
      dockerFileTemplate,
      tagName,
      push = true)

    // add flink container image tag to flink configuration
    flinkConfig.set(KubernetesConfigOptions.CONTAINER_IMAGE, flinkImageTag)
    // add flink pipeline.jars configuration
    flinkConfig.set(PipelineOptions.JARS, mutable.Buffer(dockerFileTemplate.getJobJar).asJava)

    logInfo(s"[flink-submit] already built flink job docker image. ${flinkConfIdentifierInfo(flinkConfig)}, " +
      s"flinkImageTag=${flinkImageTag}, baseFlinkImage=${submitRequest.k8sSubmitParam.flinkDockerImage}")

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
      val result = SubmitResponse(clusterId, flinkConfig)
      logInfo(s"[flink-submit] flink job has been submitted. ${flinkConfIdentifierInfo(flinkConfig)}")
      result

    } catch {
      case e: Exception =>
        logError(s"submit flink job fail in ${submitRequest.executionMode} mode")
        e.printStackTrace()
        throw e
    } finally {
      if (clusterClient != null) clusterClient.close()
      if (clusterDescriptor != null) clusterDescriptor.close()
    }
  }

  override def doStop(stopInfo: StopRequest): StopResponse = {
    super.doStop(ExecutionMode.KUBERNETES_NATIVE_APPLICATION, stopInfo)
  }

}
