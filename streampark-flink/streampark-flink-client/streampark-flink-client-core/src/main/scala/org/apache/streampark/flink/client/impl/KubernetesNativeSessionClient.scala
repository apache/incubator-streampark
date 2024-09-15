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
import org.apache.streampark.common.util.{Logger, Utils}
import org.apache.streampark.flink.client.`trait`.KubernetesNativeClientTrait
import org.apache.streampark.flink.client.bean._
import org.apache.streampark.flink.client.tool.FlinkSessionSubmitHelper
import org.apache.streampark.flink.deployment.FlinkKubernetesClient
import org.apache.streampark.flink.kubernetes.KubernetesRetriever
import org.apache.streampark.flink.kubernetes.enums.FlinkK8sExecuteMode
import org.apache.streampark.flink.kubernetes.model.ClusterKey

import org.apache.commons.lang3.StringUtils
import org.apache.flink.client.program.{ClusterClient, PackagedProgram}
import org.apache.flink.configuration._
import org.apache.flink.kubernetes.KubernetesClusterDescriptor
import org.apache.flink.kubernetes.configuration.{KubernetesConfigOptions, KubernetesDeploymentTarget}
import org.apache.flink.kubernetes.configuration.KubernetesConfigOptions.ServiceExposedType
import org.apache.flink.kubernetes.kubeclient.FlinkKubeClientFactory

import java.io.File

import scala.collection.JavaConversions._
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

/** kubernetes native session mode submit */
object KubernetesNativeSessionClient extends KubernetesNativeClientTrait with Logger {

  @throws[Exception]
  override def doSubmit(
      submitRequest: SubmitRequest,
      flinkConfig: Configuration): SubmitResponse = {
    // require parameters
    require(
      StringUtils.isNotBlank(submitRequest.clusterId),
      s"[flink-submit] submit flink job failed, clusterId is null, mode=${flinkConfig.get(DeploymentOptions.TARGET)}"
    )
    super.trySubmit(submitRequest, flinkConfig, submitRequest.userJarFile)(
      jobGraphSubmit,
      restApiSubmit)
  }

  /** Submit flink session job via rest api. */
  @throws[Exception]
  def restApiSubmit(
      submitRequest: SubmitRequest,
      flinkConfig: Configuration,
      fatJar: File): SubmitResponse = {
    Try {
      // get jm rest url of flink session cluster
      val clusterKey = ClusterKey(
        FlinkK8sExecuteMode.SESSION,
        submitRequest.kubernetesNamespace,
        submitRequest.clusterId)
      val jmRestUrl = KubernetesRetriever
        .retrieveFlinkRestUrl(clusterKey)
        .getOrElse(throw new Exception(
          s"[flink-submit] retrieve flink session rest url failed, clusterKey=$clusterKey"))
      // submit job via rest api
      val jobId = FlinkSessionSubmitHelper.submitViaRestApi(jmRestUrl, fatJar, flinkConfig)
      SubmitResponse(clusterKey.clusterId, flinkConfig.toMap, jobId, jmRestUrl)
    } match {
      case Success(s) => s
      case Failure(e) =>
        logError(s"submit flink job fail in ${submitRequest.executionMode} mode")
        throw e
    }
  }

  /** Submit flink session job with building JobGraph via ClusterClient api. */
  @throws[Exception]
  def jobGraphSubmit(
      submitRequest: SubmitRequest,
      flinkConfig: Configuration,
      jarFile: File): SubmitResponse = {
    // retrieve k8s cluster and submit flink job on session mode
    var clusterDescriptor: KubernetesClusterDescriptor = null
    var packageProgram: PackagedProgram = null
    var client: ClusterClient[String] = null

    try {
      clusterDescriptor = getK8sClusterDescriptor(flinkConfig)
      // build JobGraph
      val packageProgramJobGraph = super.getJobGraph(flinkConfig, submitRequest, jarFile)
      packageProgram = packageProgramJobGraph._1
      val jobGraph = packageProgramJobGraph._2
      // retrieve client and submit JobGraph
      client = clusterDescriptor
        .retrieve(flinkConfig.getString(KubernetesConfigOptions.CLUSTER_ID))
        .getClusterClient
      val submitResult = client.submitJob(jobGraph)
      val jobId = submitResult.get().toString
      val result =
        SubmitResponse(client.getClusterId, flinkConfig.toMap, jobId, client.getWebInterfaceURL)
      logInfo(
        s"[flink-submit] flink job has been submitted. ${flinkConfIdentifierInfo(flinkConfig)}, jobId: $jobId")
      result
    } catch {
      case e: Exception =>
        logError(s"submit flink job fail in ${submitRequest.executionMode} mode")
        e.printStackTrace()
        throw e
    } finally {
      if (submitRequest.safePackageProgram) {
        Utils.close(packageProgram)
      }
      Utils.close(clusterDescriptor, client)
    }
  }

  override def doCancel(cancelRequest: CancelRequest, flinkConf: Configuration): CancelResponse = {
    flinkConf.safeSet(DeploymentOptions.TARGET, ExecutionMode.KUBERNETES_NATIVE_SESSION.getName)
    super.doCancel(cancelRequest, flinkConf)
  }

  @throws[Exception]
  def deploy(deployReq: DeployRequest): DeployResponse = {
    val deployRequest = deployReq.asInstanceOf[KubernetesDeployRequest]
    logInfo(
      s"""
         |--------------------------------------- kubernetes session cluster start ---------------------------------------
         |    userFlinkHome    : ${deployRequest.flinkVersion.flinkHome}
         |    flinkVersion     : ${deployRequest.flinkVersion.version}
         |    execMode         : ${deployRequest.executionMode.name()}
         |    clusterId        : ${deployRequest.clusterId}
         |    namespace        : ${deployRequest.kubernetesNamespace}
         |    exposedType      : ${deployRequest.flinkRestExposedType}
         |    serviceAccount   : ${deployRequest.serviceAccount}
         |    flinkImage       : ${deployRequest.flinkImage}
         |    properties       : ${deployRequest.properties.mkString(" ")}
         |-------------------------------------------------------------------------------------------
         |""".stripMargin)

    val flinkConfig = getFlinkK8sConfig(deployRequest)
    val kubeClient = FlinkKubeClientFactory.getInstance.fromConfiguration(flinkConfig, "client")

    var clusterDescriptor: KubernetesClusterDescriptor = null
    var client: ClusterClient[String] = null

    try {
      val kubernetesClusterDescriptor = getK8sClusterDescriptorAndSpecification(flinkConfig)
      clusterDescriptor = kubernetesClusterDescriptor._1

      val kubeClientWrapper = new FlinkKubernetesClient(kubeClient)
      val kubeService = kubeClientWrapper.getService(deployRequest.clusterId)

      if (kubeService.isPresent) {
        client = clusterDescriptor.retrieve(deployRequest.clusterId).getClusterClient
      } else {
        client =
          clusterDescriptor.deploySessionCluster(kubernetesClusterDescriptor._2).getClusterClient
      }
      DeployResponse(address = client.getWebInterfaceURL, clusterId = client.getClusterId)
    } catch {
      case e: Exception => DeployResponse(error = e)
    } finally {
      Utils.close(client, clusterDescriptor, kubeClient)
    }
  }

  @throws[Exception]
  def shutdown(deployRequest: DeployRequest): ShutDownResponse = {
    val shutDownRequest = deployRequest.asInstanceOf[KubernetesDeployRequest]
    logInfo(
      s"""
         |--------------------------------------- kubernetes session cluster shutdown ---------------------------------------
         |    userFlinkHome     : ${shutDownRequest.flinkVersion.version}
         |    namespace         : ${shutDownRequest.kubernetesNamespace}
         |    clusterId         : ${shutDownRequest.clusterId}
         |    execMode          : ${shutDownRequest.executionMode.getName}
         |    flinkImage        : ${shutDownRequest.flinkImage}
         |    exposedType       : ${shutDownRequest.flinkRestExposedType.getName}
         |    kubeConf          : ${shutDownRequest.kubeConf}
         |    serviceAccount    : ${shutDownRequest.serviceAccount}
         |    properties        : ${shutDownRequest.properties.mkString(" ")}
         |-------------------------------------------------------------------------------------------
         |""".stripMargin)

    val flinkConfig = this.getFlinkK8sConfig(shutDownRequest)
    val clusterDescriptor = getK8sClusterDescriptor(flinkConfig)
    Try(
      clusterDescriptor
        .retrieve(flinkConfig.getString(KubernetesConfigOptions.CLUSTER_ID))
        .getClusterClient
    ) match {
      case Failure(e) => ShutDownResponse(e)
      case Success(c) =>
        Try(c.shutDownCluster()) match {
          case Success(_) => ShutDownResponse()
          case Failure(e) => ShutDownResponse(e)
        }
    }
  }

  private[this] def getFlinkK8sConfig(deployRequest: KubernetesDeployRequest): Configuration = {
    extractConfiguration(deployRequest.flinkVersion.flinkHome, deployRequest.properties)
      .safeSet(DeploymentOptions.TARGET, KubernetesDeploymentTarget.SESSION.getName)
      .safeSet(KubernetesConfigOptions.NAMESPACE, deployRequest.kubernetesNamespace)
      .safeSet(KubernetesConfigOptions.KUBERNETES_SERVICE_ACCOUNT, deployRequest.serviceAccount)
      .safeSet(KubernetesConfigOptions.CLUSTER_ID, deployRequest.clusterId)
      .safeSet(KubernetesConfigOptions.CONTAINER_IMAGE, deployRequest.flinkImage)
      .safeSet(
        KubernetesConfigOptions.REST_SERVICE_EXPOSED_TYPE,
        ServiceExposedType.valueOf(deployRequest.flinkRestExposedType.getName))
      .safeSet(
        KubernetesConfigOptions.KUBE_CONFIG_FILE,
        getDefaultKubernetesConf(deployRequest.kubeConf))
      .safeSet(DeploymentOptionsInternal.CONF_DIR, s"${deployRequest.flinkVersion.flinkHome}/conf")
  }

  override def doTriggerSavepoint(
      request: TriggerSavepointRequest,
      flinkConf: Configuration): SavepointResponse = {
    flinkConf.safeSet(DeploymentOptions.TARGET, ExecutionMode.KUBERNETES_NATIVE_SESSION.getName)
    super.doTriggerSavepoint(request, flinkConf)
  }

}
