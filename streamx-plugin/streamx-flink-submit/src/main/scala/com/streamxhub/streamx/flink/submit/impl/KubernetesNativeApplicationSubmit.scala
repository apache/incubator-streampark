/*
 * Copyright (c) 2019 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.streamxhub.streamx.flink.submit.impl

import com.google.common.collect.Lists
import com.streamxhub.streamx.common.conf.ConfigConst.APP_WORKSPACE
import com.streamxhub.streamx.common.enums.ExecutionMode
import com.streamxhub.streamx.flink.kubernetes.PodTemplateTool
import com.streamxhub.streamx.flink.packer.docker.{DockerTool, FlinkDockerfileTemplate}
import com.streamxhub.streamx.flink.packer.maven.MavenTool
import com.streamxhub.streamx.flink.submit.`trait`.KubernetesNativeSubmitTrait
import com.streamxhub.streamx.flink.submit.domain._
import org.apache.flink.client.deployment.application.ApplicationConfiguration
import org.apache.flink.client.program.ClusterClient
import org.apache.flink.configuration.{DeploymentOptionsInternal, PipelineOptions}
import org.apache.flink.kubernetes.KubernetesClusterDescriptor
import org.apache.flink.kubernetes.configuration.KubernetesConfigOptions

import java.io.File
import scala.util.Try

/**
 * kubernetes native application mode submit
 */
object KubernetesNativeApplicationSubmit extends KubernetesNativeSubmitTrait {

  // noinspection DuplicatedCode
  override def doSubmit(submitRequest: SubmitRequest): SubmitResponse = {
    // require parameters
    assert(Try(submitRequest.k8sSubmitParam.clusterId.nonEmpty).getOrElse(false))
    assert(Try(submitRequest.k8sSubmitParam.flinkBaseImage.nonEmpty).getOrElse(false))

    // extract flink config
    val flinkConfig = extractEffectiveFlinkConfig(submitRequest)
    // init build workspace of flink job
    val buildWorkspace = {
      // sub workspace dir like: APP_WORKSPACE/k8s-clusterId@k8s-namespace/
      val dirPath = s"$APP_WORKSPACE" +
        s"/${flinkConfig.getString(KubernetesConfigOptions.CLUSTER_ID)}@${flinkConfig.getString(KubernetesConfigOptions.NAMESPACE)}"
      val dir = new File(dirPath)
      if (!dir.exists()) dir.mkdir()
      dirPath
    }

    // step-1: build k8s pod template file
    if (submitRequest.k8sSubmitParam.podTemplates != null) {
      val k8sPodTmplConf = PodTemplateTool.preparePodTemplateFiles(buildWorkspace, submitRequest.k8sSubmitParam.podTemplates)
      k8sPodTmplConf.mergeToFlinkConf(flinkConfig)
      logInfo(s"[flink-submit] already built kubernetes pod template file " +
        s"${flinkConfIdentifierInfo(flinkConfig)}, k8sPodTemplateFile=${k8sPodTmplConf.tmplFiles}")
    }

    // step-2: build fat-jar
    val fatJar = {
      val fatJarOutputPath = s"${buildWorkspace}/flink-job.jar"
      val flinkLibs = extractProvidedLibs(submitRequest)
      val jarPackDeps = submitRequest.k8sSubmitParam.jarPackDeps
      MavenTool.buildFatJar(jarPackDeps.merge(flinkLibs), fatJarOutputPath)
      // cache file MD5 is used to compare whether it is consistent when it is generated next time.
      //  If it is consistent, it is used directly and returned directly instead of being regenerated
      // fatJarCached.getOrElseUpdate(flinkLibs._1, MavenTool.buildFatJar(flinkLibs._2, fatJarPath))
    }
    logInfo(s"[flink-submit] already built flink job fat-jar. " +
      s"${flinkConfIdentifierInfo(flinkConfig)}, fatJarPath=${fatJar.getAbsolutePath}")

    logInfo(s"[flink-submit] start building flink job docker image. ${flinkConfIdentifierInfo(flinkConfig)}")
    // step-3: build and push flink application image
    val tagName = s"flinkjob-${submitRequest.k8sSubmitParam.clusterId}"
    val dockerFileTemplate = new FlinkDockerfileTemplate(submitRequest.k8sSubmitParam.flinkBaseImage, fatJar.getAbsolutePath)
    // add flink pipeline.jars configuration
    flinkConfig.set(PipelineOptions.JARS, Lists.newArrayList(dockerFileTemplate.getJobJar))
    // add flink conf conciguration, mainly to set the log4j configuration
    if (!flinkConfig.contains(DeploymentOptionsInternal.CONF_DIR)){
      flinkConfig.set(DeploymentOptionsInternal.CONF_DIR, s"${submitRequest.flinkHome}/conf")
    }
    // build docker image
    val flinkImageTag = DockerTool.buildFlinkImage(
      submitRequest.k8sSubmitParam.dockerAuthConfig,
      buildWorkspace,
      dockerFileTemplate,
      tagName,
      push = true)
    // add flink container image tag to flink configuration
    flinkConfig.set(KubernetesConfigOptions.CONTAINER_IMAGE, flinkImageTag)

    logInfo(s"[flink-submit] already built flink job docker image. ${flinkConfIdentifierInfo(flinkConfig)}, " +
      s"flinkImageTag=${flinkImageTag}, baseFlinkImage=${submitRequest.k8sSubmitParam.flinkBaseImage}")

    // step-4: retrieve k8s cluster and submit flink job on application mode
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
      val result = SubmitResponse(clusterId, flinkConfig)
      logInfo(s"[flink-submit] flink job has been submitted. ${flinkConfIdentifierInfo(flinkConfig)}")
      result

    } catch {
      case e: Exception =>
        logError(s"submit flink job fail in ${submitRequest.executionMode} mode")
        throw e
    } finally {
      if (clusterClient != null) clusterClient.close()
      if (clusterDescriptor != null) clusterDescriptor.close()
    }
  }

  override def doStop(stopInfo: StopRequest): StopResponse = {
    super.doStop(ExecutionMode.KUBERNETES_NATIVE_APPLICATION, stopInfo)
  }

}
