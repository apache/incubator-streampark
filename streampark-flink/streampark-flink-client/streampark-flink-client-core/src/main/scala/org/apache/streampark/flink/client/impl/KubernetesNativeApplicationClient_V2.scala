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
import org.apache.flink.kubernetes.configuration.KubernetesConfigOptions
import org.apache.flink.v1beta1.FlinkDeploymentSpec.FlinkVersion

import scala.language.postfixOps

object KubernetesNativeApplicationClient_V2 extends KubernetesNativeClientTrait {
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

    try {
      val spec: FlinkDeploymentDef = convertFlinkDeploymentDef(submitRequest, flinkConfig)
      FlinkK8sOperator.deployApplicationJob(submitRequest.id, spec).runIO
      val result = SubmitResponse(null, flinkConfig.toMap, submitRequest.jobId, null)
      logInfo(
        s"[flink-submit] flink job has been submitted. ${flinkConfIdentifierInfo(flinkConfig)}")
      result
    } catch {
      case e: Exception =>
        logError(s"submit flink job fail in ${submitRequest.executionMode} mode")
        throw e
    } finally {}
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
