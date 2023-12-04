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
import org.apache.streampark.flink.kubernetes.v2.model.{FlinkDeploymentDef, JobManagerDef, TaskManagerDef}
import org.apache.streampark.flink.kubernetes.v2.model.TrackKey.ApplicationJobKey
import org.apache.streampark.flink.kubernetes.v2.observer.FlinkK8sObserver
import org.apache.streampark.flink.kubernetes.v2.operator.FlinkK8sOperator
import org.apache.streampark.flink.kubernetes.v2.operator.OprError.{FlinkResourceNotFound, UnsupportedAction}
import org.apache.streampark.flink.packer.pipeline.K8sAppModeBuildResponse

import org.apache.commons.lang3.StringUtils
import org.apache.flink.client.deployment.application.ApplicationConfiguration
import org.apache.flink.configuration._
import org.apache.flink.kubernetes.configuration.KubernetesConfigOptions
import org.apache.flink.runtime.jobgraph.SavepointConfigOptions
import org.apache.flink.v1beta1.FlinkDeploymentSpec.FlinkVersion
import zio.ZIO

import scala.collection.mutable
import scala.jdk.CollectionConverters.mapAsScalaMapConverter
import scala.util.{Failure, Success, Try}

/** Flink K8s application mode task operation client via Flink K8s Operator */
object KubernetesApplicationClientV2 extends KubernetesClientV2Trait with Logger {

  @throws[Throwable]
  override def doSubmit(
      submitRequest: SubmitRequest,
      flinkConfig: Configuration): SubmitResponse = {

    val richMsg: String => String = s"[flink-submit][appId=${submitRequest.id}] " + _

    submitRequest.checkBuildResult()
    val buildResult = submitRequest.buildResult.asInstanceOf[K8sAppModeBuildResponse]

    // Convert to FlinkDeployment CR definition
    val flinkDeployDef = genFlinkDeployDef(submitRequest, flinkConfig, buildResult) match {
      case Right(result) => result
      case Left(errMsg) =>
        throw new IllegalArgumentException(
          richMsg(s"Error occurred while parsing parameters: $errMsg"))
    }

    // Submit FlinkDeployment CR to Kubernetes
    FlinkK8sOperator.deployApplicationJob(submitRequest.id, flinkDeployDef).runIOAsTry match {
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

  /** Generate FlinkDeployment CR definition, it is a pure effect function. */
  private def genFlinkDeployDef(
      submitReq: SubmitRequest,
      originFlinkConfig: Configuration,
      buildResult: K8sAppModeBuildResponse): Either[FailureMessage, FlinkDeploymentDef] = {

    val flinkConfObj = originFlinkConfig.clone()
    val flinkConfMap = originFlinkConfig.toMap.asScala.toMap

    val namespace = Option(submitReq.k8sSubmitParam.kubernetesNamespace)
      .getOrElse(Constant.DEFAULT)

    val name = submitReq.k8sSubmitParam.kubernetesName
      .orElse(Option(submitReq.k8sSubmitParam.clusterId))
      .filter(str => StringUtils.isNotBlank(str))
      .getOrElse(return Left("Kubernetes CR name should not be empty"))

    val image = submitReq.k8sSubmitParam.baseImage
      .orElse(Option(buildResult.flinkBaseImage))
      .filter(str => StringUtils.isNotBlank(str))
      .getOrElse(return Left("Flink base image should not be empty"))

    val ingress = submitReq.k8sSubmitParam.ingressDefinition

    val imagePullPolicy = flinkConfObj
      .getOption(KubernetesConfigOptions.CONTAINER_IMAGE_PULL_POLICY)
      .map(_.toString)
      .orElse(submitReq.k8sSubmitParam.imagePullPolicy)

    val serviceAccount = flinkConfObj
      .getOption(KubernetesConfigOptions.KUBERNETES_SERVICE_ACCOUNT)
      .orElse(submitReq.k8sSubmitParam.serviceAccount)
      .getOrElse(FlinkDeploymentDef.DEFAULT_SERVICE_ACCOUNT)

    val flinkVersion = Option(submitReq.flinkVersion.majorVersion)
      .map(majorVer => "V" + majorVer.replace(".", "_"))
      .flatMap(v => FlinkVersion.values().find(_.name() == v))
      .getOrElse(return Left(s"Unsupported Flink version: ${submitReq.flinkVersion.majorVersion}"))

    val jobDef = genJobDef(flinkConfObj, jarUriHint = Some(buildResult.mainJarPath))
      .getOrElse(return Left("Invalid job definition"))

    val podTemplate = submitReq.k8sSubmitParam.podTemplate.map(
      yaml =>
        unmarshalPodTemplate(yaml)
          .getOrElse(return Left(s"Invalid pod template: \n$yaml")))

    val jobManager = {
      val cpu = flinkConfMap
        .get(KUBERNETES_JM_CPU_AMOUNT_KEY)
        .orElse(flinkConfMap.get(KUBERNETES_JM_CPU_KEY))
        .flatMap(value => Try(value.toDouble).toOption)
        .orElse(submitReq.k8sSubmitParam.jobManagerCpu)
        .getOrElse(KUBERNETES_JM_CPU_DEFAULT)

      val mem = flinkConfObj
        .getOption(JobManagerOptions.TOTAL_PROCESS_MEMORY)
        .map(_.toString)
        .orElse(submitReq.k8sSubmitParam.jobManagerMemory)
        .getOrElse(KUBERNETES_JM_MEMORY_DEFAULT)

      val podTemplate = submitReq.k8sSubmitParam.jobManagerPodTemplate.map(
        yaml =>
          unmarshalPodTemplate(yaml)
            .getOrElse(return Left(s"Invalid job manager pod template: \n$yaml")))
      JobManagerDef(
        cpu = cpu,
        memory = mem,
        ephemeralStorage = submitReq.k8sSubmitParam.jobManagerEphemeralStorage,
        podTemplate = podTemplate)
    }

    val taskManager = {
      val cpu = flinkConfMap
        .get(KUBERNETES_TM_CPU_AMOUNT_KEY)
        .orElse(flinkConfMap.get(KUBERNETES_TM_CPU_KEY))
        .flatMap(value => Try(value.toDouble).toOption)
        .orElse(submitReq.k8sSubmitParam.taskManagerCpu)
        .getOrElse(KUBERNETES_TM_CPU_DEFAULT)

      val mem = flinkConfObj
        .getOption(TaskManagerOptions.TOTAL_PROCESS_MEMORY)
        .map(_.toString)
        .orElse(submitReq.k8sSubmitParam.taskManagerMemory)
        .getOrElse(KUBERNETES_TM_MEMORY_DEFAULT)

      val podTemplate = submitReq.k8sSubmitParam.taskManagerPodTemplate.map(
        yaml =>
          unmarshalPodTemplate(yaml)
            .getOrElse(return Left(s"Invalid task manager pod template: \n$yaml")))
      TaskManagerDef(
        cpu = cpu,
        memory = mem,
        ephemeralStorage = submitReq.k8sSubmitParam.taskManagerEphemeralStorage,
        podTemplate = podTemplate)
    }

    val logConfiguration = {
      val items = submitReq.k8sSubmitParam.logConfiguration.asScala
      if (items.isEmpty) {
        // Get default log config from local target flink home
        val logConfigs = Array(
          "log4j.properties" -> s"${submitReq.flinkVersion.flinkHome}/conf/log4j-console.properties",
          "logback.xml" -> s"${submitReq.flinkVersion.flinkHome}/conf/logback-console.xml"
        )
        logConfigs
          .map { case (name, path) => name -> os.Path(path) }
          .filter { case (_, path) => Try(os.exists(path) && os.isFile(path)).getOrElse(false) }
          .map {
            case (name, path) =>
              name -> Try(os.read(path)).toOption.filter(str => StringUtils.isNotBlank(str))
          }
          .filter { case (_, content) => content.isDefined }
          .map { case (name, content) => name -> content.get }
          .foreach { case (name, content) => items += name -> content }
      }
      items.toMap
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
      // Set kubernetes.rest-service.exposed.type configuration for compatibility with native-k8s
      submitReq.k8sSubmitParam.flinkRestExposedType.foreach {
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
        logConfiguration = logConfiguration,
        podTemplate = podTemplate,
        job = Some(jobDef),
        extJarPaths = Array.empty,
        ingress = ingress
      ))
  }

  /** Shutdown Flink Application deployment. */
  @throws[Throwable]
  def shutdown(shutDownRequest: ShutDownRequest): ShutDownResponse = {
    val name = shutDownRequest.clusterId
    val namespace = shutDownRequest.kubernetesDeployParam.kubernetesNamespace
    def richMsg: String => String = s"[flink-shutdown][clusterId=$name][namespace=$namespace] " + _

    FlinkK8sObserver.trackedKeys
      .find {
        case ApplicationJobKey(_, ns, n) => ns == namespace && n == name
        case _ => false
      }
      .someOrUnitZIO(key => FlinkK8sOperator.delete(key.id))
      .catchSome {
        case _: FlinkResourceNotFound => ZIO.unit
        case _: UnsupportedAction => ZIO.unit
      }
      .as(ShutDownResponse(name))
      .runIOAsTry match {
      case Success(result) =>
        logInfo(richMsg("Shutdown Flink Application deployment successfully.")); result
      case Failure(err) =>
        logError(richMsg(s"Fail to shutdown Flink Application deployment"), err); throw err
    }
  }

}
