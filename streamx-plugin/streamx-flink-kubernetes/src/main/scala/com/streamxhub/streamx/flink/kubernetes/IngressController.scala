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

package com.streamxhub.streamx.flink.kubernetes

import com.streamxhub.streamx.common.util.Utils
import io.fabric8.kubernetes.api.model.IntOrString
import io.fabric8.kubernetes.api.model.networking.v1beta1.IngressBuilder
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import org.apache.commons.io.FileUtils
import org.apache.flink.client.program.ClusterClient
import org.apache.flink.kubernetes.shaded.com.fasterxml.jackson.core.JsonProcessingException
import org.apache.flink.kubernetes.shaded.com.fasterxml.jackson.core.`type`.TypeReference
import org.apache.flink.kubernetes.shaded.com.fasterxml.jackson.databind.ObjectMapper

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.util
import scala.util.Try
import scala.collection.JavaConverters._

object IngressController {

  @throws[FileNotFoundException]
  def configureIngress(domainName: String, clusterId: String, nameSpace: String): Unit = {
    Utils.tryWithResource(new DefaultKubernetesClient) { client =>
      val annotMap = Map[String, String](
        "nginx.ingress.kubernetes.io/rewrite-target" -> "/$2",
        "nginx.ingress.kubernetes.io/proxy-body-size" -> "1024m",
        "nginx.ingress.kubernetes.io/configuration-snippet" -> ("rewrite ^(/" + clusterId + ")$ $1/ permanent;")
      )
      val labelsMap = Map[String, String](
        "app" -> clusterId,
        "type" -> "flink-native-kubernetes",
        "component" -> "ingress"
      )
      val ingress =
        new IngressBuilder()
          .withNewMetadata()
          .withName(clusterId)
          .addToAnnotations(annotMap.asJava)
          .addToLabels(labelsMap.asJava)
          .endMetadata()
          .withNewSpec()
          .addNewRule()
          .withHost(domainName)
          .withNewHttp()
          .addNewPath()
          .withPath(s"/$nameSpace/$clusterId/")
          .withNewBackend()
          .withServiceName(s"$clusterId-rest")
          .withServicePort(new IntOrString("rest"))
          .endBackend()
          .endPath()
          .addNewPath()
          .withPath(s"/$nameSpace/$clusterId" + "(/|$)(.*)")
          .withNewBackend()
          .withServiceName("$clusterId-rest")
          .withServicePort(new IntOrString("rest"))
          .endBackend()
          .endPath()
          .endHttp()
          .endRule()
          .endSpec()
          .build();
      client.network.ingress.inNamespace(nameSpace).create(ingress)
    }
  }

  @throws[FileNotFoundException]
  def configureIngress(ingressOutput: String): Unit = {
    Utils.close {
      val client = new DefaultKubernetesClient
      client.network.ingress
        .load(Files.newInputStream(Paths.get(ingressOutput)))
        .get()
      client
    }
  }

  def deleteIngress(ingressName: String, nameSpace: String): Unit = {
    if (determineThePodSurvivalStatus(ingressName, nameSpace)) {
      Utils.close {
        val client = new DefaultKubernetesClient
        client.network.ingress.inNamespace(nameSpace).withName(ingressName).delete
        client
      }
    }
  }

  private[this] def determineThePodSurvivalStatus(name: String, nameSpace: String): Boolean = { // getpod by deploymentName
    Utils.tryWithResource(new DefaultKubernetesClient()) { client =>
      Try {
        client.apps()
          .deployments()
          .inNamespace(nameSpace)
          .withName(name)
          .get()
          .getSpec()
          .getSelector()
          .getMatchLabels()
        false
      }.getOrElse(true)
    }
  }

  @throws[JsonProcessingException]
  def ingressUrlAddress(nameSpace: String, clusterId: String, clusterClient: ClusterClient[_]): String = {
    if (determineIfIngressExists(nameSpace, clusterId)) {
      val client = new DefaultKubernetesClient
      val ingress = client.network.ingress.inNamespace(nameSpace).withName(clusterId).get
      val publicEndpoints = ingress.getMetadata.getAnnotations.get("field.cattle.io/publicEndpoints")
      val objectMapper = new ObjectMapper
      val ingressMetas = objectMapper.readValue(publicEndpoints, new TypeReference[util.List[IngressMeta]]() {})
      val hostname = ingressMetas.get(0).hostname
      val path = ingressMetas.get(0).path
      s"https://$hostname$path"
    } else {
      clusterClient.getWebInterfaceURL
    }
  }

  def determineIfIngressExists(nameSpace: String, clusterId: String): Boolean = {
    Utils.tryWithResource(new DefaultKubernetesClient) { client =>
      Try {
        client.extensions.ingresses
          .inNamespace(nameSpace)
          .withName(clusterId).get.getMetadata.getName
        true
      }.getOrElse(false)
    }
  }

  @throws[IOException]
  def prepareIngressTemplateFiles(buildWorkspace: String, ingressTemplates: String): String = {
    val workspaceDir = new File(buildWorkspace)
    if (!workspaceDir.exists) workspaceDir.mkdir
    if (ingressTemplates.isEmpty) null; else {
      val outputPath = buildWorkspace + "/ingress.yaml"
      val outputFile = new File(outputPath)
      FileUtils.write(outputFile, ingressTemplates, "UTF-8")
      outputPath
    }
  }

}


case class IngressMeta(
                    addresses: List[String],
                    port: Int,
                    protocol: String,
                    serviceName: String,
                    ingressName: String,
                    hostname: String,
                    path: String,
                    allNodes: Boolean
                  )
