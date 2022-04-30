/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.flink.submit.impl

import java.io.File
import java.util

import com.streamxhub.streamx.common.enums.ExecutionMode
import com.streamxhub.streamx.common.util.{Logger, Utils}
import com.streamxhub.streamx.flink.kubernetes.KubernetesRetriever
import com.streamxhub.streamx.flink.kubernetes.enums.FlinkK8sExecuteMode
import com.streamxhub.streamx.flink.kubernetes.model.ClusterKey
import com.streamxhub.streamx.flink.submit.`trait`.KubernetesNativeSubmitTrait
import com.streamxhub.streamx.flink.submit.bean.{DeployResponse, _}
import com.streamxhub.streamx.flink.submit.tool.FlinkSessionSubmitHelper
import io.fabric8.kubernetes.api.model.{Config => _}
import org.apache.commons.lang3.StringUtils
import org.apache.flink.client.program.{ClusterClient, PackagedProgram}
import org.apache.flink.configuration._
import org.apache.flink.kubernetes.KubernetesClusterDescriptor
import org.apache.flink.kubernetes.configuration.KubernetesConfigOptions.ServiceExposedType
import org.apache.flink.kubernetes.configuration.{KubernetesConfigOptions, KubernetesDeploymentTarget}
import org.apache.flink.kubernetes.kubeclient.{Endpoint, FlinkKubeClient, FlinkKubeClientFactory}

import scala.collection.JavaConversions._
import scala.language.postfixOps
import scala.util.Try

/**
  * kubernetes native session mode submit
  */
object KubernetesNativeSessionSubmit extends KubernetesNativeSubmitTrait with Logger {

  @throws[Exception]
  override def doSubmit(submitRequest: SubmitRequest, flinkConfig: Configuration): SubmitResponse = {
    // require parameters
    require(
      StringUtils.isNotBlank(submitRequest.k8sSubmitParam.clusterId),
      s"[flink-submit] stop flink job failed, clusterId is null, mode=${flinkConfig.get(DeploymentOptions.TARGET)}"
    )
    super.trySubmit(submitRequest, flinkConfig, submitRequest.userJarFile)(restApiSubmit)(jobGraphSubmit)
  }

  /**
    * Submit flink session job via rest api.
    */
  @throws[Exception] def restApiSubmit(submitRequest: SubmitRequest, flinkConfig: Configuration, fatJar: File): SubmitResponse = {
    Try {
      // get jm rest url of flink session cluster
      val clusterKey = ClusterKey(
        FlinkK8sExecuteMode.SESSION,
        submitRequest.k8sSubmitParam.kubernetesNamespace,
        submitRequest.k8sSubmitParam.clusterId
      )
      val jmRestUrl = KubernetesRetriever.retrieveFlinkRestUrl(clusterKey)
        .getOrElse(throw new Exception(s"[flink-submit] retrieve flink session rest url failed, clusterKey=$clusterKey"))
      // submit job via rest api
      val jobId = FlinkSessionSubmitHelper.submitViaRestApi(jmRestUrl, fatJar, flinkConfig)
      SubmitResponse(clusterKey.clusterId, flinkConfig.toMap, jobId)
    }.recover { case e =>
      logError(s"submit flink job fail in ${submitRequest.executionMode} mode")
      throw e
    }.get
  }

  /**
    * Submit flink session job with building JobGraph via ClusterClient api.
    */
  @throws[Exception] def jobGraphSubmit(submitRequest: SubmitRequest, flinkConfig: Configuration, jarFile: File): SubmitResponse = {
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
      client = clusterDescriptor.retrieve(flinkConfig.getString(KubernetesConfigOptions.CLUSTER_ID)).getClusterClient
      val submitResult = client.submitJob(jobGraph)
      val jobId = submitResult.get().toString
      val result = SubmitResponse(client.getClusterId, flinkConfig.toMap, jobId)
      logInfo(s"[flink-submit] flink job has been submitted. ${flinkConfIdentifierInfo(flinkConfig)}, jobId: $jobId")
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

  override def doStop(stopRequest: StopRequest, flinkConfig: Configuration): StopResponse = {
    flinkConfig.safeSet(DeploymentOptions.TARGET, ExecutionMode.KUBERNETES_NATIVE_APPLICATION.getName)
    super.doStop(stopRequest, flinkConfig)
  }

  def deploy(deployRequest: DeployRequest): DeployResponse = {
    logInfo(
      s"""
         |--------------------------------------- kubernetes sesion start ---------------------------------------
         |    userFlinkHome    : ${deployRequest.flinkVersion.flinkHome}
         |    flinkVersion     : ${deployRequest.flinkVersion.version}
         |    execMode         : ${deployRequest.executionMode.name()}
         |    clusterId        : ${deployRequest.clusterId}
         |    namespace        : ${deployRequest.k8sDeployParam.kubernetesNamespace}
         |    exposedType      : ${deployRequest.k8sDeployParam.flinkRestExposedType}
         |    serviceAccount   : ${deployRequest.k8sDeployParam.serviceAccount}
         |    flinkImage       : ${deployRequest.k8sDeployParam.flinkImage}
         |    resolveOrder     : ${deployRequest.resolveOrder.getName}
         |    flameGraph       : ${deployRequest.flameGraph != null}
         |    dynamicOption    : ${deployRequest.dynamicOption.mkString(" ")}
         |-------------------------------------------------------------------------------------------
         |""".stripMargin)
    var clusterDescriptor: KubernetesClusterDescriptor = null
    var client: ClusterClient[String] = null
    var kubeClient: FlinkKubeClient = null
    try {
      val flinkConfig = extractConfiguration(deployRequest.flinkVersion.flinkHome,
        null,
        deployRequest.flameGraph,
        deployRequest.dynamicOption,
        deployRequest.extraParameter,
        deployRequest.resolveOrder)
      flinkConfig
        .safeSet(DeploymentOptions.TARGET, KubernetesDeploymentTarget.SESSION.getName)
        .safeSet(KubernetesConfigOptions.NAMESPACE, deployRequest.k8sDeployParam.kubernetesNamespace)
        .safeSet(KubernetesConfigOptions.KUBERNETES_SERVICE_ACCOUNT, deployRequest.k8sDeployParam.serviceAccount)
        .safeSet(KubernetesConfigOptions.REST_SERVICE_EXPOSED_TYPE, ServiceExposedType.valueOf(deployRequest.k8sDeployParam.flinkRestExposedType.getName))
        .safeSet(KubernetesConfigOptions.CLUSTER_ID, deployRequest.clusterId)
        .safeSet(KubernetesConfigOptions.CONTAINER_IMAGE, deployRequest.k8sDeployParam.flinkImage)
        .safeSet(KubernetesConfigOptions.KUBE_CONFIG_FILE, getDefaultKubernetesConf(deployRequest.k8sDeployParam.kubeConf))

      val kubernetesClusterDescriptor = getK8sClusterDescriptorAndSpecification(flinkConfig)
      clusterDescriptor = kubernetesClusterDescriptor._1
      kubeClient = FlinkKubeClientFactory.getInstance.fromConfiguration(flinkConfig, "client")

      if (deployRequest.clusterId != null && kubeClient.getRestService(deployRequest.clusterId).isPresent) {
        client = clusterDescriptor.retrieve(deployRequest.clusterId).getClusterClient
      } else {
        client = clusterDescriptor.deploySessionCluster(kubernetesClusterDescriptor._2).getClusterClient
      }
      if (client.getWebInterfaceURL != null) {
        DeployResponse(client.getWebInterfaceURL, client.getClusterId.toString)
      } else {
        null
      }
    } catch {
      case e: Exception => logError(s"start flink session fail in ${deployRequest.executionMode} mode")
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
      shutDownRequest.extraParameter.foreach(m => m._2 match {
        case v if v != null => flinkConfig.setString(m._1, m._2.toString)
        case _ =>
      })
      flinkConfig
        .safeSet(DeploymentOptions.TARGET, KubernetesDeploymentTarget.SESSION.getName)
        .safeSet(KubernetesConfigOptions.NAMESPACE, shutDownRequest.kubernetesDeployParam.kubernetesNamespace)
        .safeSet(KubernetesConfigOptions.KUBERNETES_SERVICE_ACCOUNT, shutDownRequest.kubernetesDeployParam.serviceAccount)
        .safeSet(KubernetesConfigOptions.REST_SERVICE_EXPOSED_TYPE, ServiceExposedType.valueOf(shutDownRequest.kubernetesDeployParam.flinkRestExposedType.getName))
        .safeSet(KubernetesConfigOptions.CLUSTER_ID, shutDownRequest.clusterId)
        .safeSet(KubernetesConfigOptions.CONTAINER_IMAGE, shutDownRequest.kubernetesDeployParam.flinkImage)
        .safeSet(KubernetesConfigOptions.KUBE_CONFIG_FILE, getDefaultKubernetesConf(shutDownRequest.kubernetesDeployParam.kubeConf))
      kubeClient = FlinkKubeClientFactory.getInstance.fromConfiguration(flinkConfig, "client")
      if (shutDownRequest.clusterId != null && kubeClient.getRestService(shutDownRequest.clusterId).isPresent) {
        kubeClient.stopAndCleanupCluster(shutDownRequest.clusterId)
        ShutDownResponse()
      } else {
        null
      }
    } catch {
      case e: Exception => logError(s"shutdown flink session fail in ${shutDownRequest.executionMode} mode")
        e.printStackTrace()
        throw e
    } finally {
      Utils.close(kubeClient)
    }
  }
}
