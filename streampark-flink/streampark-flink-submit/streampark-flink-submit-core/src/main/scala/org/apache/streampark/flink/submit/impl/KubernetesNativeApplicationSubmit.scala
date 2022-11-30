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

package org.apache.streampark.flink.submit.impl

import com.google.common.collect.Lists
import org.apache.streampark.common.enums.ExecutionMode
import org.apache.streampark.common.util.Utils
import org.apache.streampark.flink.packer.pipeline.DockerImageBuildResponse
import org.apache.streampark.flink.submit.`trait`.KubernetesNativeSubmitTrait
import org.apache.streampark.flink.submit.bean._
import org.apache.commons.lang3.StringUtils
import org.apache.flink.client.deployment.application.ApplicationConfiguration
import org.apache.flink.client.program.ClusterClient
import org.apache.flink.configuration.{Configuration, DeploymentOptions, DeploymentOptionsInternal, PipelineOptions}
import org.apache.flink.kubernetes.KubernetesClusterDescriptor
import org.apache.flink.kubernetes.configuration.KubernetesConfigOptions

/**
 * kubernetes native application mode submit
 */
object KubernetesNativeApplicationSubmit extends KubernetesNativeSubmitTrait {

  @throws[Exception]
  override def doSubmit(submitRequest: SubmitRequest, flinkConfig: Configuration): SubmitResponse = {

    // require parameters
    require(
      StringUtils.isNotBlank(submitRequest.k8sSubmitParam.clusterId),
      s"[flink-submit] submit flink job failed, clusterId is null, mode=${flinkConfig.get(DeploymentOptions.TARGET)}"
    )

    // check the last building result
    submitRequest.checkBuildResult()

    val buildResult = submitRequest.buildResult.asInstanceOf[DockerImageBuildResponse]

    // add flink pipeline.jars configuration
    flinkConfig.safeSet(PipelineOptions.JARS, Lists.newArrayList(buildResult.dockerInnerMainJarPath))
    // add flink conf configuration, mainly to set the log4j configuration
    if (!flinkConfig.contains(DeploymentOptionsInternal.CONF_DIR)) {
      flinkConfig.safeSet(DeploymentOptionsInternal.CONF_DIR, s"${submitRequest.flinkVersion.flinkHome}/conf")
    }
    // add flink container image tag to flink configuration
    flinkConfig.safeSet(KubernetesConfigOptions.CONTAINER_IMAGE, buildResult.flinkImageTag)

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
      val result = SubmitResponse(clusterId, flinkConfig.toMap, submitRequest.jobId)
      logInfo(s"[flink-submit] flink job has been submitted. ${flinkConfIdentifierInfo(flinkConfig)}")
      result
    } catch {
      case e: Exception =>
        logError(s"submit flink job fail in ${submitRequest.executionMode} mode")
        throw e
    } finally {
      Utils.close(clusterDescriptor, clusterClient)
    }
  }

  override def doCancel(cancelRequest: CancelRequest, flinkConfig: Configuration): CancelResponse = {
    flinkConfig.safeSet(DeploymentOptions.TARGET, ExecutionMode.KUBERNETES_NATIVE_APPLICATION.getName)
    super.doCancel(cancelRequest, flinkConfig)
  }

}
