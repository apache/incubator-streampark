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

import org.apache.streampark.common.Constant
import org.apache.streampark.common.util.Logger
import org.apache.streampark.common.zio.ZIOExt.{IOOps, OptionZIOOps}
import org.apache.streampark.flink.client.`trait`.KubernetesClientV2Trait
import org.apache.streampark.flink.client.bean._
import org.apache.streampark.flink.kubernetes.v2.model.{FlinkDeploymentDef, FlinkSessionJobDef, JobManagerDef, TaskManagerDef}
import org.apache.streampark.flink.kubernetes.v2.model.TrackKey.ClusterKey
import org.apache.streampark.flink.kubernetes.v2.observer.FlinkK8sObserver
import org.apache.streampark.flink.kubernetes.v2.operator.FlinkK8sOperator
import org.apache.streampark.flink.kubernetes.v2.operator.OprError.{FlinkResourceNotFound, UnsupportedAction}
import org.apache.streampark.flink.packer.pipeline.ShadedBuildResponse

import org.apache.commons.lang3.StringUtils
import org.apache.flink.client.deployment.application.ApplicationConfiguration
import org.apache.flink.configuration._
import org.apache.flink.kubernetes.configuration.KubernetesConfigOptions
import org.apache.flink.runtime.jobgraph.SavepointConfigOptions
import org.apache.flink.v1beta1.FlinkDeploymentSpec.FlinkVersion
import zio.ZIO

import scala.collection.convert.ImplicitConversions.`map AsScala`
import scala.collection.mutable
import scala.jdk.CollectionConverters.mapAsScalaMapConverter
import scala.util.{Failure, Success, Try}

/** Flink K8s session mode app operation client via Flink K8s Operator */
object KubernetesSessionClientV2 extends KubernetesClientV2Trait with Logger {
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
      .getOrElse(Constant.DEFAULT)

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

  /** Deploy Flink cluster. */
  @throws[Throwable]
  def deploy(deployRequest: DeployRequest): DeployResponse = {
    logInfo(
      s"""
         |--------------------------------------- kubernetes cluster start ---------------------------------------
         |    userFlinkHome    : ${deployRequest.flinkVersion.flinkHome}
         |    flinkVersion     : ${deployRequest.flinkVersion.version}
         |    execMode         : ${deployRequest.executionMode.name()}
         |    clusterId        : ${deployRequest.clusterId}
         |    namespace        : ${deployRequest.k8sDeployParam.kubernetesNamespace}
         |    exposedType      : ${deployRequest.k8sDeployParam.flinkRestExposedType}
         |    serviceAccount   : ${deployRequest.k8sDeployParam.serviceAccount}
         |    flinkImage       : ${deployRequest.k8sDeployParam.flinkImage}
         |    properties       : ${deployRequest.properties.mkString(",")}
         |--------------------------------------------------------------------------------------------------------
         |""".stripMargin)
    val richMsg: String => String = s"[flink-submit][appId=${deployRequest.id}] " + _

    val flinkConfig =
      extractConfiguration(deployRequest.flinkVersion.flinkHome, deployRequest.properties)
    // Convert to FlinkDeployment CR definition
    val flinkDeployDef = genFlinkDeployDef(deployRequest, flinkConfig) match {
      case Right(result) => result
      case Left(errMsg) =>
        throw new IllegalArgumentException(
          richMsg(s"Error occurred while parsing parameters:$errMsg"))
    }

    // Submit FlinkDeployment CR to Kubernetes
    FlinkK8sOperator.deployCluster(deployRequest.id, flinkDeployDef).runIOAsTry match {
      case Success(_) =>
        logInfo(richMsg("Flink Cluster has been submitted successfully."))
      case Failure(err) =>
        logError(
          richMsg(s"Submit Flink Cluster fail in${deployRequest.executionMode.getName}_V2 mode!"),
          err)
        throw err
    }

    DeployResponse(null, deployRequest.clusterId)
  }

  /** Shutdown Flink cluster. */
  @throws[Throwable]
  def shutdown(shutDownRequest: ShutDownRequest): ShutDownResponse = {
    val name = shutDownRequest.clusterId
    val namespace = shutDownRequest.kubernetesDeployParam.kubernetesNamespace
    def richMsg: String => String = s"[flink-shutdown][clusterId=$name][namespace=$namespace] " + _

    FlinkK8sObserver.trackedKeys
      .find {
        case ClusterKey(_, ns, n) => ns == namespace && n == name
        case _ => false
      }
      .someOrUnitZIO(key => FlinkK8sOperator.delete(key.id))
      .catchSome {
        case _: FlinkResourceNotFound => ZIO.unit
        case _: UnsupportedAction => ZIO.unit
      }
      .as(ShutDownResponse(name))
      .runIOAsTry match {
      case Success(result) => logInfo(richMsg("Shutdown Flink cluster successfully.")); result
      case Failure(err) => logError(richMsg(s"Fail to shutdown Flink cluster"), err); throw err
    }
  }

  /** Generate FlinkDeployment CR definition, it is a pure effect function. */
  private def genFlinkDeployDef(
      deployReq: DeployRequest,
      originFlinkConfig: Configuration): Either[FailureMessage, FlinkDeploymentDef] = {
    val flinkConfObj = originFlinkConfig.clone()
    val flinkConfMap = originFlinkConfig.toMap.asScala.toMap

    val namespace = Option(deployReq.k8sDeployParam.kubernetesNamespace)
      .getOrElse(Constant.DEFAULT)

    val name = Option(deployReq.k8sDeployParam.clusterId)
      .filter(str => StringUtils.isNotBlank(str))
      .getOrElse(return Left("Kubernetes CR name should not be empty"))

    val imagePullPolicy = flinkConfObj
      .getOption(KubernetesConfigOptions.CONTAINER_IMAGE_PULL_POLICY)
      .map(_.toString)

    val image = Option(deployReq.k8sDeployParam.flinkImage)
      .filter(str => StringUtils.isNotBlank(str))
      .getOrElse(return Left("Flink base image should not be empty"))

    val serviceAccount = flinkConfObj
      .getOption(KubernetesConfigOptions.KUBERNETES_SERVICE_ACCOUNT)
      .orElse(Option(deployReq.k8sDeployParam.serviceAccount))
      .getOrElse(FlinkDeploymentDef.DEFAULT_SERVICE_ACCOUNT)

    val flinkVersion = Option(deployReq.flinkVersion.majorVersion)
      .map(majorVer => "V" + majorVer.replace(".", "_"))
      .flatMap(v => FlinkVersion.values().find(_.name() == v))
      .getOrElse(return Left(s"Unsupported Flink version:${deployReq.flinkVersion.majorVersion}"))

    val jobManager = {
      val cpu = flinkConfMap
        .get(KUBERNETES_JM_CPU_AMOUNT_KEY)
        .orElse(flinkConfMap.get(KUBERNETES_JM_CPU_KEY))
        .flatMap(value => Try(value.toDouble).toOption)
        .getOrElse(KUBERNETES_JM_CPU_DEFAULT)

      val mem = flinkConfObj
        .getOption(JobManagerOptions.TOTAL_PROCESS_MEMORY)
        .map(_.toString)
        .getOrElse(KUBERNETES_JM_MEMORY_DEFAULT)

      JobManagerDef(cpu = cpu, memory = mem, ephemeralStorage = None, podTemplate = None)
    }

    val taskManager = {
      val cpu = flinkConfMap
        .get(KUBERNETES_TM_CPU_AMOUNT_KEY)
        .orElse(flinkConfMap.get(KUBERNETES_TM_CPU_KEY))
        .flatMap(value => Try(value.toDouble).toOption)
        .getOrElse(KUBERNETES_TM_CPU_DEFAULT)

      val mem = flinkConfObj
        .getOption(TaskManagerOptions.TOTAL_PROCESS_MEMORY)
        .map(_.toString)
        .getOrElse(KUBERNETES_TM_MEMORY_DEFAULT)

      TaskManagerDef(cpu = cpu, memory = mem, ephemeralStorage = None, podTemplate = None)
    }

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
      Option(deployReq.k8sDeployParam.flinkRestExposedType).foreach {
        exposedType => result += KUBERNETES_REST_SERVICE_EXPORTED_TYPE_KEY -> exposedType.getName
      }
      result.toMap
    }

    Right(
      FlinkDeploymentDef(
        namespace = namespace,
        name = name,
        image = image,
        imagePullPolicy = imagePullPolicy,
        serviceAccount = serviceAccount,
        flinkVersion = flinkVersion,
        jobManager = jobManager,
        taskManager = taskManager,
        flinkConfiguration = extraFlinkConfiguration,
        extJarPaths = Array.empty
      ))
  }

}
