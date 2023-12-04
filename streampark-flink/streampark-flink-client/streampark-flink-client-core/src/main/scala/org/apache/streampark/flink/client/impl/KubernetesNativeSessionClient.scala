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

import org.apache.streampark.common.enums.FlinkExecutionMode
import org.apache.streampark.common.util.{Logger, Utils}
import org.apache.streampark.flink.client.`trait`.KubernetesNativeClientTrait
import org.apache.streampark.flink.client.bean._
import org.apache.streampark.flink.client.tool.FlinkSessionSubmitHelper
import org.apache.streampark.flink.core.FlinkKubernetesClient
import org.apache.streampark.flink.kubernetes.KubernetesRetriever
import org.apache.streampark.flink.kubernetes.enums.FlinkK8sExecuteModeEnum
import org.apache.streampark.flink.kubernetes.model.ClusterKey

import io.fabric8.kubernetes.api.model.{Config => _}
import org.apache.commons.lang3.StringUtils
import org.apache.flink.client.program.{ClusterClient, PackagedProgram}
import org.apache.flink.configuration._
import org.apache.flink.kubernetes.KubernetesClusterDescriptor
import org.apache.flink.kubernetes.configuration.{KubernetesConfigOptions, KubernetesDeploymentTarget}
import org.apache.flink.kubernetes.configuration.KubernetesConfigOptions.ServiceExposedType
import org.apache.flink.kubernetes.kubeclient.{FlinkKubeClient, FlinkKubeClientFactory}

import scala.collection.convert.ImplicitConversions._
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

/**
 * Kubernetes native session mode submit.
 * @deprecated
 *   Please use [[KubernetesSessionClientV2]] instead.
 */
@Deprecated
object KubernetesNativeSessionClient extends KubernetesNativeClientTrait with Logger {

  @throws[Exception]
  override def doSubmit(
      submitRequest: SubmitRequest,
      flinkConfig: Configuration): SubmitResponse = {
    // require parameters
    require(
      StringUtils.isNotBlank(submitRequest.k8sSubmitParam.clusterId),
      s"[flink-submit] submit flink job failed, clusterId is null, mode=${flinkConfig.get(DeploymentOptions.TARGET)}"
    )
    super.trySubmit(submitRequest, flinkConfig)(jobGraphSubmit, restApiSubmit)
  }

  /** Submit flink session job via rest api. */
  @throws[Exception]
  def restApiSubmit(submitRequest: SubmitRequest, flinkConfig: Configuration): SubmitResponse = {
    Try {
      // get jm rest url of flink session cluster
      val clusterKey = ClusterKey(
        FlinkK8sExecuteModeEnum.SESSION,
        submitRequest.k8sSubmitParam.kubernetesNamespace,
        submitRequest.k8sSubmitParam.clusterId)
      val jmRestUrl = KubernetesRetriever
        .retrieveFlinkRestUrl(clusterKey)
        .getOrElse(throw new Exception(
          s"[flink-submit] retrieve flink session rest url failed, clusterKey=$clusterKey"))
      // submit job via rest api
      val jobId =
        FlinkSessionSubmitHelper.submitViaRestApi(jmRestUrl, submitRequest.userJarFile, flinkConfig)
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
  def jobGraphSubmit(submitRequest: SubmitRequest, flinkConfig: Configuration): SubmitResponse = {
    // retrieve k8s cluster and submit flink job on session mode
    var clusterDescriptor: KubernetesClusterDescriptor = null
    var packageProgram: PackagedProgram = null
    var client: ClusterClient[String] = null

    try {
      clusterDescriptor = getK8sClusterDescriptor(flinkConfig)
      // build JobGraph
      val programJobGraph = super.getJobGraph(submitRequest, flinkConfig)
      packageProgram = programJobGraph._1
      val jobGraph = programJobGraph._2
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

  override def doCancel(
      cancelRequest: CancelRequest,
      flinkConfig: Configuration): CancelResponse = {
    flinkConfig.safeSet(
      DeploymentOptions.TARGET,
      FlinkExecutionMode.KUBERNETES_NATIVE_SESSION.getName)
    super.doCancel(cancelRequest, flinkConfig)
  }

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
    var clusterDescriptor: KubernetesClusterDescriptor = null
    var client: ClusterClient[String] = null
    var kubeClient: FlinkKubeClient = null
    try {
      val flinkConfig =
        extractConfiguration(deployRequest.flinkVersion.flinkHome, deployRequest.properties)
      flinkConfig
        .safeSet(DeploymentOptions.TARGET, KubernetesDeploymentTarget.SESSION.getName)
        .safeSet(
          KubernetesConfigOptions.NAMESPACE,
          deployRequest.k8sDeployParam.kubernetesNamespace)
        .safeSet(
          KubernetesConfigOptions.KUBERNETES_SERVICE_ACCOUNT,
          deployRequest.k8sDeployParam.serviceAccount)
        .safeSet(
          KubernetesConfigOptions.REST_SERVICE_EXPOSED_TYPE,
          ServiceExposedType.valueOf(deployRequest.k8sDeployParam.flinkRestExposedType.getName))
        .safeSet(KubernetesConfigOptions.CLUSTER_ID, deployRequest.clusterId)
        .safeSet(KubernetesConfigOptions.CONTAINER_IMAGE, deployRequest.k8sDeployParam.flinkImage)
        .safeSet(
          KubernetesConfigOptions.KUBE_CONFIG_FILE,
          getDefaultKubernetesConf(deployRequest.k8sDeployParam.kubeConf))
        .safeSet(
          DeploymentOptionsInternal.CONF_DIR,
          s"${deployRequest.flinkVersion.flinkHome}/conf")

      val kubernetesClusterDescriptor = getK8sClusterDescriptorAndSpecification(flinkConfig)
      clusterDescriptor = kubernetesClusterDescriptor._1
      kubeClient = FlinkKubeClientFactory.getInstance.fromConfiguration(flinkConfig, "client")
      val kubeClientWrapper = new FlinkKubernetesClient(kubeClient)

      if (
        deployRequest.clusterId != null && kubeClientWrapper
          .getService(deployRequest.clusterId)
          .isPresent
      ) {
        client = clusterDescriptor.retrieve(deployRequest.clusterId).getClusterClient
      } else {
        client =
          clusterDescriptor.deploySessionCluster(kubernetesClusterDescriptor._2).getClusterClient
      }
      if (client.getWebInterfaceURL != null) {
        DeployResponse(client.getWebInterfaceURL, client.getClusterId)
      } else {
        null
      }
    } catch {
      case e: Exception =>
        logError(s"start flink session fail in ${deployRequest.executionMode} mode")
        e.printStackTrace()
        throw e
    } finally {
      Utils.close(client, clusterDescriptor, kubeClient)
    }
  }

  def shutdown(shutDownRequest: ShutDownRequest): ShutDownResponse = {
    var kubeClient: FlinkKubeClient = null
    try {
      val flinkConfig = getFlinkDefaultConfiguration(shutDownRequest.flinkVersion.flinkHome)
      shutDownRequest.properties.foreach(
        m =>
          m._2 match {
            case v if v != null => flinkConfig.setString(m._1, m._2.toString)
            case _ =>
          })
      flinkConfig
        .safeSet(DeploymentOptions.TARGET, KubernetesDeploymentTarget.SESSION.getName)
        .safeSet(
          KubernetesConfigOptions.NAMESPACE,
          shutDownRequest.kubernetesDeployParam.kubernetesNamespace)
        .safeSet(
          KubernetesConfigOptions.KUBERNETES_SERVICE_ACCOUNT,
          shutDownRequest.kubernetesDeployParam.serviceAccount)
        .safeSet(
          KubernetesConfigOptions.REST_SERVICE_EXPOSED_TYPE,
          ServiceExposedType.valueOf(
            shutDownRequest.kubernetesDeployParam.flinkRestExposedType.getName))
        .safeSet(KubernetesConfigOptions.CLUSTER_ID, shutDownRequest.clusterId)
        .safeSet(
          KubernetesConfigOptions.CONTAINER_IMAGE,
          shutDownRequest.kubernetesDeployParam.flinkImage)
        .safeSet(
          KubernetesConfigOptions.KUBE_CONFIG_FILE,
          getDefaultKubernetesConf(shutDownRequest.kubernetesDeployParam.kubeConf))
      kubeClient = FlinkKubeClientFactory.getInstance.fromConfiguration(flinkConfig, "client")
      val kubeClientWrapper = new FlinkKubernetesClient(kubeClient)

      if (
        shutDownRequest.clusterId != null && kubeClientWrapper
          .getService(shutDownRequest.clusterId)
          .isPresent
      ) {
        kubeClient.stopAndCleanupCluster(shutDownRequest.clusterId)
        ShutDownResponse(shutDownRequest.clusterId)
      } else {
        null
      }
    } catch {
      case e: Exception =>
        logError(s"shutdown flink session fail in ${shutDownRequest.executionMode} mode")
        e.printStackTrace()
        throw e
    } finally {
      Utils.close(kubeClient)
    }
  }

  override def doTriggerSavepoint(
      triggerSavepointRequest: TriggerSavepointRequest,
      flinkConfig: Configuration): SavepointResponse = {
    flinkConfig.safeSet(
      DeploymentOptions.TARGET,
      FlinkExecutionMode.KUBERNETES_NATIVE_SESSION.getName)
    super.doTriggerSavepoint(triggerSavepointRequest, flinkConfig)
  }
}
