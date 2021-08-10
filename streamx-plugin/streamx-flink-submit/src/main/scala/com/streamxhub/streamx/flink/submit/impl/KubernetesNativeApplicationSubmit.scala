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
import com.streamxhub.streamx.flink.packer.{DockerTool, FlinkDockerfileTemplate, MavenTool}
import com.streamxhub.streamx.flink.submit.`trait`.KubernetesNativeSubmitTrait
import com.streamxhub.streamx.flink.submit.{StopRequest, StopResponse, SubmitRequest, SubmitResponse}
import org.apache.flink.client.deployment.application.ApplicationConfiguration
import org.apache.flink.client.program.ClusterClient
import org.apache.flink.kubernetes.KubernetesClusterDescriptor
import org.apache.flink.kubernetes.configuration.KubernetesConfigOptions

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
      val fatJarPath = buildWorkspace + "flink-job.jar"
      val flinkLibs = extractProvidedLibs(submitRequest)
      // cache file MD5 is used to compare whether it is consistent when it is generated next time.
      //  If it is consistent, it is used directly and returned directly instead of being regenerated
      fatJarCached.getOrElseUpdate(flinkLibs._1, MavenTool.buildFatJar(flinkLibs._2, fatJarPath))
    }
    // build and push flink application image
    val flinkImageTag = {
      val tagName = s"flinkjob-${submitRequest.k8sSubmitParam.clusterId}"
      val dockerFileTemplate = new FlinkDockerfileTemplate(submitRequest.k8sSubmitParam.flinkDockerImage, fatJar.getAbsolutePath)
      DockerTool.buildFlinkImage(
        submitRequest.k8sSubmitParam.dockerRegisterAddress,
        submitRequest.k8sSubmitParam.dockerRegisterUser,
        submitRequest.k8sSubmitParam.dockerRegisterPassword,
        buildWorkspace,
        dockerFileTemplate,
        tagName,
        push = true
      )
    }
    // add flink image tag to flink configuration
    flinkConfig.set(KubernetesConfigOptions.CONTAINER_IMAGE, flinkImageTag)

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
      SubmitResponse(clusterId, flinkConfig)

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
    doStop(ExecutionMode.KUBERNETES_NATIVE_APPLICATION, stopInfo)
  }

}
