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

import org.apache.streampark.common.util.Logger
import org.apache.streampark.common.zio.ZIOExt.IOOps
import org.apache.streampark.flink.client.`trait`.KubernetesClientV2Trait
import org.apache.streampark.flink.client.bean._
import org.apache.streampark.flink.kubernetes.v2.model.{FlinkSessionJobDef, JobManagerDef, TaskManagerDef}
import org.apache.streampark.flink.kubernetes.v2.operator.FlinkK8sOperator
import org.apache.streampark.flink.packer.pipeline.K8sAppModeBuildResponse

import org.apache.commons.lang3.StringUtils
import org.apache.flink.client.deployment.application.ApplicationConfiguration
import org.apache.flink.configuration._
import org.apache.flink.kubernetes.configuration.KubernetesConfigOptions
import org.apache.flink.runtime.jobgraph.SavepointConfigOptions

import scala.collection.mutable
import scala.jdk.CollectionConverters.mapAsScalaMapConverter
import scala.util.{Failure, Success, Try}

object KubernetesSessionClientV2 extends KubernetesClientV2Trait with Logger {
  @throws[Exception]
  override def doSubmit(
      submitRequest: SubmitRequest,
      flinkConfig: Configuration): SubmitResponse = {

    val richMsg: String => String = s"[flink-submit][appId=${submitRequest.id}] " + _

    submitRequest.checkBuildResult()
    val buildResult = submitRequest.buildResult.asInstanceOf[K8sAppModeBuildResponse]

    // Convert to FlinkSessionJobDef CR definition
    val flinkSessionJobDef = genFlinkSessionJobDef(submitRequest, flinkConfig, buildResult) match {
      case Right(result) => result
      case Left(errMsg) =>
        throw new IllegalArgumentException(
          richMsg(s"Error occurred while parsing parameters: $errMsg"))
    }

    // Submit FlinkSessionJobDef CR to Kubernetes
    FlinkK8sOperator.deploySessionJob(submitRequest.id, flinkSessionJobDef).runIOAsTry match {
      case Success(_) =>
        logInfo(richMsg("Flink job has been submitted successfully."))
      case Failure(err) =>
        logError(
          richMsg(s"Submit Flink job fail in ${submitRequest.executionMode.getName}_V2 mode!"),
          err)
        throw err
    }

    SubmitResponse(
      clusterId = submitRequest.k8sSubmitParam.clusterId,
      flinkConfig = flinkConfig.toMap,
      jobId = submitRequest.jobId,
      jobManagerUrl = null
    )
  }

  // Generate FlinkSessionJobDef CR definition, it is a pure effect function.
  private def genFlinkSessionJobDef(
      submitReq: SubmitRequest,
      originFlinkConfig: Configuration,
      buildResult: K8sAppModeBuildResponse): Either[FailureMessage, FlinkSessionJobDef] = {

    val flinkConfObj = originFlinkConfig.clone()

    val namespace = Option(submitReq.k8sSubmitParam.kubernetesNamespace)
      .getOrElse("default")

    val name = Option(submitReq.k8sSubmitParam.clusterId)
      .filter(str => StringUtils.isNotBlank(str))
      .getOrElse(return Left("cluster-id should not be empty"))

    val deploymentName = Option(submitReq.developmentMode.name())
      .filter(str => StringUtils.isNotBlank(str))
      .getOrElse(return Left("deploymentName should not be empty"))

    val jobDef = genJobDef(flinkConfObj, jarUriHint = Some(buildResult.mainJarPath))
      .getOrElse(return Left("Invalid job definition"))

    val extraFlinkConfiguration = {
      // Remove conflicting configuration items
      val result: mutable.Map[String, String] = flinkConfObj
        .remove(DeploymentOptions.TARGET)
        .remove(KubernetesConfigOptions.CONTAINER_IMAGE_PULL_POLICY)
        .remove(KubernetesConfigOptions.KUBERNETES_SERVICE_ACCOUNT)
        .remove(JobManagerOptions.TOTAL_PROCESS_MEMORY)
        .remove(TaskManagerOptions.TOTAL_PROCESS_MEMORY)
        .remove(PipelineOptions.JARS)
        .remove(CoreOptions.DEFAULT_PARALLELISM)
        .remove(ApplicationConfiguration.APPLICATION_ARGS)
        .remove(ApplicationConfiguration.APPLICATION_MAIN_CLASS)
        .remove(SavepointConfigOptions.SAVEPOINT_PATH)
        .remove(SavepointConfigOptions.SAVEPOINT_IGNORE_UNCLAIMED_STATE)
        .toMap
        .asScala
        .removeKey(KUBERNETES_JM_CPU_AMOUNT_KEY)
        .removeKey(KUBERNETES_TM_CPU_KEY)
        .removeKey(KUBERNETES_JM_CPU_AMOUNT_KEY)
        .removeKey(KUBERNETES_JM_CPU_KEY)
      // Set kubernetes.rest-service.exposed.type configuration for compatibility with native-k8s
      submitReq.k8sSubmitParam.flinkRestExposedType.foreach {
        exposedType => result += KUBERNETES_REST_SERVICE_EXPORTED_TYPE_KEY -> exposedType.getName
      }
      result.toMap
    }

    // TODO Migrate the construction logic of ingress to here and set it into FlinkDeploymentDef.ingress
    //  See: org.apache.streampark.flink.packer.pipeline.impl.FlinkK8sApplicationBuildPipeline Step-8
    Right(
      FlinkSessionJobDef(
        namespace = namespace,
        name = name,
        deploymentName = deploymentName,
        flinkConfiguration = extraFlinkConfiguration,
        job = jobDef,
        restartNonce = None
      ))
  }

  def shutdown(shutDownRequest: ShutDownRequest): ShutDownResponse = {
    def richMsg: String => String = s"[flink-delete-][appId=${shutDownRequest.clusterId}] " + _

    FlinkK8sOperator.k8sCrOpr
      .deleteSessionJob(
        shutDownRequest.kubernetesDeployParam.kubernetesNamespace,
        shutDownRequest.clusterId)
      .runIOAsTry match {
      case Success(rsp) =>
        logInfo(richMsg("Cancel flink job successfully."))
        rsp
      case Failure(err) =>
        logError(
          richMsg(s"delete flink job fail in ${shutDownRequest.executionMode.getName}_V2 mode!"),
          err)
        throw err
    }
    ShutDownResponse()
  }

}
