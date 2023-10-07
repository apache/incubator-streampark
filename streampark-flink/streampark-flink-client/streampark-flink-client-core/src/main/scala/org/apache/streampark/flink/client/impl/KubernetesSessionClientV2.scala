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
import org.apache.streampark.common.zio.ZIOExt.{IOOps, OptionZIOOps}
import org.apache.streampark.flink.client.`trait`.KubernetesClientV2Trait
import org.apache.streampark.flink.client.bean._
import org.apache.streampark.flink.kubernetes.v2.model.FlinkSessionJobDef
import org.apache.streampark.flink.kubernetes.v2.observer.FlinkK8sObserver
import org.apache.streampark.flink.kubernetes.v2.operator.FlinkK8sOperator
import org.apache.streampark.flink.packer.pipeline.ShadedBuildResponse

import org.apache.commons.lang3.StringUtils
import org.apache.flink.client.deployment.application.ApplicationConfiguration
import org.apache.flink.configuration._
import org.apache.flink.runtime.jobgraph.SavepointConfigOptions

import scala.jdk.CollectionConverters.mapAsScalaMapConverter
import scala.util.{Failure, Success}

/** Flink K8s session mode app operation client via Flink K8s Operator */
object KubernetesSessionClientV2 extends KubernetesClientV2Trait with Logger {
  private val observer = FlinkK8sObserver

  @throws[Throwable]
  override def doSubmit(
      submitRequest: SubmitRequest,
      flinkConfig: Configuration): SubmitResponse = {

    val richMsg: String => String = s"[flink-submit][appId=${submitRequest.id}] " + _

    submitRequest.checkBuildResult()
    val buildResult = submitRequest.buildResult.asInstanceOf[ShadedBuildResponse]

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
      buildResult: ShadedBuildResponse): Either[FailureMessage, FlinkSessionJobDef] = {

    val flinkConfObj = originFlinkConfig.clone()

    val namespace = Option(submitReq.k8sSubmitParam.kubernetesNamespace)
      .getOrElse("default")

    val name = submitReq.k8sSubmitParam.kubernetesName
      .filter(str => StringUtils.isNotBlank(str))
      .getOrElse(return Left("Kubernetes CR name should not be empty"))

    val deploymentName = Option(submitReq.k8sSubmitParam.clusterId)
      .filter(str => StringUtils.isNotBlank(str))
      .getOrElse(return Left("Target FlinkDeployment CR name should not be empty"))

    val jobDef = genJobDef(flinkConfObj, jarUriHint = Some(buildResult.shadedJarPath))
      .getOrElse(return Left("Invalid job definition"))

    // Remove conflicting configuration items
    val extraFlinkConfiguration = flinkConfObj
      .remove(DeploymentOptions.TARGET)
      .remove(PipelineOptions.JARS)
      .remove(CoreOptions.DEFAULT_PARALLELISM)
      .remove(ApplicationConfiguration.APPLICATION_ARGS)
      .remove(ApplicationConfiguration.APPLICATION_MAIN_CLASS)
      .remove(SavepointConfigOptions.SAVEPOINT_PATH)
      .remove(SavepointConfigOptions.SAVEPOINT_IGNORE_UNCLAIMED_STATE)
      .toMap
      .asScala
      .toMap

    Right(
      FlinkSessionJobDef(
        namespace = namespace,
        name = name,
        deploymentName = deploymentName,
        flinkConfiguration = extraFlinkConfiguration,
        job = jobDef
      ))
  }

  /** Shutdown Flink cluster. */
  @throws[Throwable]
  def shutdown(shutDownRequest: ShutDownRequest): ShutDownResponse = {
    val name = shutDownRequest.clusterId
    val namespace = shutDownRequest.kubernetesDeployParam.kubernetesNamespace
    def richMsg: String => String = s"[flink-shutdown][clusterId=$name][namespace=$namespace] " + _

    FlinkK8sOperator.k8sCrOpr.deleteSessionJob(namespace, name).runIOAsTry match {
      case Success(_) =>
        observer.trackedKeys
          .find(_.id == shutDownRequest.id)
          .someOrUnitZIO(key => observer.untrack(key))
        logInfo(richMsg("Shutdown Flink cluster successfully."))
        ShutDownResponse()
      case Failure(err) =>
        logError(richMsg(s"Fail to shutdown Flink cluster"), err)
        throw err
    }
  }

}
