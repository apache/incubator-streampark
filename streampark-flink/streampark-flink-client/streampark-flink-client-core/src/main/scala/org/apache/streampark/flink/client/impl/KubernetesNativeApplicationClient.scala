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

import org.apache.streampark.common.enums.ExecutionMode
import org.apache.streampark.common.util.Utils
import org.apache.streampark.common.zio.ZIOExt.IOOps
import org.apache.streampark.flink.client.`trait`.KubernetesNativeClientTrait
import org.apache.streampark.flink.client.bean._
import org.apache.streampark.flink.kubernetes.v2.model.{FlinkDeploymentDef, JobDef, JobManagerDef, TaskManagerDef}
import org.apache.streampark.flink.kubernetes.v2.operator.FlinkK8sOperator
import org.apache.streampark.flink.packer.pipeline.DockerImageBuildResponse

import com.google.common.collect.Lists
import org.apache.commons.lang3.StringUtils
import org.apache.flink.client.program.ClusterClient
import org.apache.flink.configuration.{Configuration, DeploymentOptions, JobManagerOptions, PipelineOptions, TaskManagerOptions}
import org.apache.flink.kubernetes.KubernetesClusterDescriptor
import org.apache.flink.kubernetes.configuration.KubernetesConfigOptions
import org.apache.flink.v1beta1.FlinkDeploymentSpec.FlinkVersion

import scala.language.postfixOps

/** kubernetes native application mode submit */
object KubernetesNativeApplicationClient extends KubernetesNativeClientTrait {

  @throws[Exception]
  override def doSubmit(
      submitRequest: SubmitRequest,
      flinkConfig: Configuration): SubmitResponse = {

    // require parameters
    require(
      StringUtils.isNotBlank(submitRequest.k8sSubmitParam.clusterId),
      s"[flink-submit] submit flink job failed, clusterId is null, mode=${flinkConfig.get(DeploymentOptions.TARGET)}"
    )

    // check the last building result
    submitRequest.checkBuildResult()

    val buildResult = submitRequest.buildResult.asInstanceOf[DockerImageBuildResponse]

    // add flink pipeline.jars configuration
    flinkConfig.safeSet(
      PipelineOptions.JARS,
      Lists.newArrayList(buildResult.dockerInnerMainJarPath))

    // add flink container image tag to flink configuration
    flinkConfig.safeSet(KubernetesConfigOptions.CONTAINER_IMAGE, buildResult.flinkImageTag)

    // retrieve k8s cluster and submit flink job on application mode
    var clusterClient: ClusterClient[String] = null

    try {
      /**
       * This code needs to be deleted after the v2 module is successfully developed
       */
      //      val (descriptor, clusterSpecification) = getK8sClusterDescriptorAndSpecification(flinkConfig)
//      clusterDescriptor = descriptor
//
//      val applicationConfig = ApplicationConfiguration.fromConfiguration(flinkConfig)
//      clusterClient = clusterDescriptor
//        .deployApplicationCluster(clusterSpecification, applicationConfig)
//        .getClusterClient
      val spec: FlinkDeploymentDef = convertFlinkDeploymentDef(submitRequest, flinkConfig)
      FlinkK8sOperator.deployApplicationJob(submitRequest.id, spec).runIO
      val clusterId = clusterClient.getClusterId
      val result = SubmitResponse(
        clusterId,
        flinkConfig.toMap,
        submitRequest.jobId,
        clusterClient.getWebInterfaceURL)
      logInfo(
        s"[flink-submit] flink job has been submitted. ${flinkConfIdentifierInfo(flinkConfig)}")
      result
    } catch {
      case e: Exception =>
        logError(s"submit flink job fail in ${submitRequest.executionMode} mode")
        throw e
    } finally {
      Utils.close(clusterClient)
    }
  }

  override def doCancel(
      cancelRequest: CancelRequest,
      flinkConfig: Configuration): CancelResponse = {
    flinkConfig.safeSet(
      DeploymentOptions.TARGET,
      ExecutionMode.KUBERNETES_NATIVE_APPLICATION.getName)
    super.doCancel(cancelRequest, flinkConfig)
  }

  override def doTriggerSavepoint(
      request: TriggerSavepointRequest,
      flinkConf: Configuration): SavepointResponse = {
    flinkConf.safeSet(DeploymentOptions.TARGET, ExecutionMode.KUBERNETES_NATIVE_APPLICATION.getName)
    super.doTriggerSavepoint(request, flinkConf)
  }

  private[this] def convertFlinkDeploymentDef(
      submitRequest: SubmitRequest,
      flinkConfig: Configuration): FlinkDeploymentDef = {
    val spec = FlinkDeploymentDef(
      name = submitRequest.appName,
      namespace = submitRequest.k8sSubmitParam.kubernetesNamespace,
      image = KubernetesConfigOptions.CONTAINER_IMAGE.defaultValue(),
      flinkVersion = Option(submitRequest.flinkVersion.majorVersion)
        .map(_.replace(".", "_"))
        .map("V" + _)
        .flatMap(v => FlinkVersion.values().find(_.name() == v)) match {
        case Some(version) => version
        case None => throw new IllegalArgumentException("Flink version not found")
      },
      jobManager = JobManagerDef(
        cpu = 1,
        memory = flinkConfig.get(JobManagerOptions.TOTAL_PROCESS_MEMORY).toString),
      taskManager = TaskManagerDef(
        cpu = 1,
        memory = flinkConfig.get(TaskManagerOptions.TOTAL_PROCESS_MEMORY).toString),
      job = Some(
        JobDef(
          jarURI =
            submitRequest.buildResult.asInstanceOf[DockerImageBuildResponse].dockerInnerMainJarPath,
          parallelism = 1
        )),
      extJarPaths = submitRequest.userJarFile match {
        case null => Array.empty[String]
        case file => Array(file.getAbsolutePath)
      }
    )
    spec
  }

}
