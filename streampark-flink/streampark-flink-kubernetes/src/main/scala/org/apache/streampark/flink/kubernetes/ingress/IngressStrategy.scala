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

import io.fabric8.kubernetes.api.model.{OwnerReference, OwnerReferenceBuilder}
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import org.apache.commons.io.FileUtils
import org.apache.flink.client.program.ClusterClient

import java.io.File

trait IngressStrategy {

  // splits a version string into numeric and "extra" parts
  private val VERSION_MATCH_RE = "^\\s*v?([0-9]+(?:\\.[0-9]+)*)(.*)*$".r

  def ingressUrlAddress(
      nameSpace: String,
      clusterId: String,
      clusterClient: ClusterClient[_]): String

  def configureIngress(domainName: String, clusterId: String, nameSpace: String): Unit

  /**
   * The version information of kubernetes is based on the gitVersion field ParseSemantic parses a
   * version string that exactly obeys the syntax and semantics of the "Semantic Versioning"
   * specification (http://semver.org/) (although it ignores leading and trailing whitespace, and
   * allows the version to be preceded by "v").
   */
  def parseSemantic(getGitVersion: String): Double = {
    val numbers = VERSION_MATCH_RE.findFirstMatchIn(getGitVersion).get.group(1).split('.')
    s"${numbers(0)}.${numbers(1)}".toDouble
  }

  def prepareIngressTemplateFiles(buildWorkspace: String, ingressTemplates: String): String = {
    val workspaceDir = new File(buildWorkspace)
    if (!workspaceDir.exists) workspaceDir.mkdir
    if (ingressTemplates.isEmpty) null
    else {
      val outputPath = buildWorkspace + "/ingress.yaml"
      val outputFile = new File(outputPath)
      FileUtils.write(outputFile, ingressTemplates, "UTF-8")
      outputPath
    }
  }

  def buildIngressAnnotations(clusterId: String): Map[String, String] = {
    Map(
      "nginx.ingress.kubernetes.io/rewrite-target" -> "/$2",
      "nginx.ingress.kubernetes.io/proxy-body-size" -> "1024m",
      "nginx.ingress.kubernetes.io/configuration-snippet" -> ("rewrite ^(/" + clusterId + ")$ $1/ permanent;")
    )
  }

  def buildIngressLabels(clusterId: String): Map[String, String] = {
    Map(
      "app" -> clusterId,
      "type" -> "flink-native-kubernetes",
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
