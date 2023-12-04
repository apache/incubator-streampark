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

package org.apache.streampark.flink.kubernetes.ingress

import org.apache.streampark.common.conf.{ConfigKeys, InternalConfigHolder, K8sFlinkConfig}
import org.apache.streampark.common.util.FileUtils

import io.fabric8.kubernetes.api.model.{OwnerReference, OwnerReferenceBuilder}
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import org.apache.flink.client.program.ClusterClient

import java.io.File

trait IngressStrategy {

  def ingressUrlAddress(
      nameSpace: String,
      clusterId: String,
      clusterClient: ClusterClient[_]): String

  def configureIngress(domainName: String, clusterId: String, nameSpace: String): Unit

  def prepareIngressTemplateFiles(buildWorkspace: String, ingressTemplates: String): String = {
    if (ingressTemplates.isEmpty) null
    else {
      val workspaceDir = new File(buildWorkspace)
      FileUtils.mkdir(workspaceDir)
      val outputPath = buildWorkspace + "/ingress.yaml"
      val outputFile = new File(outputPath)
      FileUtils.writeFile(ingressTemplates, outputFile)
      outputPath
    }
  }

  def buildIngressAnnotations(clusterId: String, namespace: String): Map[String, String] = {
    var annotations = Map(
      "nginx.ingress.kubernetes.io/rewrite-target" -> "/$2",
      "nginx.ingress.kubernetes.io/proxy-body-size" -> "1024m",
      "nginx.ingress.kubernetes.io/configuration-snippet" -> s"""rewrite ^(/$clusterId)$$ $$1/ permanent; sub_filter '<base href="./">' '<base href="/$namespace/$clusterId/">'; sub_filter_once off;"""
    )
    val ingressClass = InternalConfigHolder.get[String](K8sFlinkConfig.ingressClass)
    if (ingressClass.nonEmpty) {
      annotations += ("kubernetes.io/ingress.class" -> ingressClass)
    }
    annotations
  }

  def buildIngressLabels(clusterId: String): Map[String, String] = {
    Map(
      "app" -> clusterId,
      "type" -> ConfigKeys.FLINK_NATIVE_KUBERNETES_LABEL,
      "component" -> "ingress"
    )
  }

  def getOwnerReference(
      nameSpace: String,
      clusterId: String,
      client: DefaultKubernetesClient): OwnerReference = {

    val deployment = client
      .apps()
      .deployments()
      .inNamespace(nameSpace)
      .withName(clusterId)
      .get()

    require(
      deployment != null,
      s"Deployment with name $clusterId not found in namespace $nameSpace")

    new OwnerReferenceBuilder()
      .withUid(deployment.getMetadata.getUid)
      .withApiVersion("apps/v1")
      .withKind("Deployment")
      .withName(clusterId)
      .withController(true)
      .withBlockOwnerDeletion(true)
      .build()
  }

}
