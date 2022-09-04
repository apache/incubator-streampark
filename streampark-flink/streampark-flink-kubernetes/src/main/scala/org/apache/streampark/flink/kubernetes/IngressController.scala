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

package org.apache.streampark.flink.kubernetes

import org.apache.streampark.common.util.Logger
import org.apache.streampark.common.util.Utils._
import io.fabric8.kubernetes.api.model.IntOrString
import io.fabric8.kubernetes.api.model.networking.v1beta1.IngressBuilder
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import org.apache.commons.io.FileUtils
import org.apache.flink.client.program.ClusterClient
import org.json4s.jackson.JsonMethods.parse
import org.json4s.{DefaultFormats, JArray}

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import scala.util.{Failure, Success, Try}
import scala.collection.JavaConverters._
import scala.language.postfixOps

object IngressController extends Logger {

  def configureIngress(domainName: String, clusterId: String, nameSpace: String): Unit = {
    Try(new DefaultKubernetesClient) match {
      case Success(client) =>
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
        val ingress = new IngressBuilder()
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
          .withServiceName(s"$clusterId-rest")
          .withServicePort(new IntOrString("rest"))
          .endBackend()
          .endPath()
          .endHttp()
          .endRule()
          .endSpec()
          .build();
        client.network.ingress.inNamespace(nameSpace).create(ingress)
      case _ =>
    }
  }

  def configureIngress(ingressOutput: String): Unit = {
    close {
      val client = new DefaultKubernetesClient
      client.network.ingress
        .load(Files.newInputStream(Paths.get(ingressOutput)))
        .get()
      client
    }
  }

  def deleteIngress(ingressName: String, nameSpace: String): Unit = {
    if (determineThePodSurvivalStatus(ingressName, nameSpace)) {
      close {
        val client = new DefaultKubernetesClient
        client.network.ingress.inNamespace(nameSpace).withName(ingressName).delete
        client
      }
    }
  }

  private[this] def determineThePodSurvivalStatus(name: String, nameSpace: String): Boolean = {
    tryWithResource(KubernetesRetriever.newK8sClient()) { client =>
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

  def ingressUrlAddress(nameSpace: String, clusterId: String, clusterClient: ClusterClient[_]): String = {
    if (determineIfIngressExists(nameSpace, clusterId)) {
      val client = new DefaultKubernetesClient
      val ingress = client.network.ingress.inNamespace(nameSpace).withName(clusterId).get
      val publicEndpoints = ingress.getMetadata.getAnnotations.get("field.cattle.io/publicEndpoints")
      IngressMeta.as(publicEndpoints) match {
        case Some(metas) =>
          val ingressMeta = metas.head
          val hostname = ingressMeta.hostname
          val path = ingressMeta.path
          logger.info(s"Retrieve flink cluster $clusterId successfully, JobManager Web Interface: https://$hostname$path")
          s"https://$hostname$path"
        case None => throw new RuntimeException("[StreamPark] get ingressUrlAddress error.")
      }
    } else {
      clusterClient.getWebInterfaceURL
    }
  }

  def determineIfIngressExists(nameSpace: String, clusterId: String): Boolean = {
    tryWithResource(KubernetesRetriever.newK8sClient()) { client =>
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
                        port: Integer,
                        protocol: String,
                        serviceName: String,
                        ingressName: String,
                        hostname: String,
                        path: String,
                        allNodes: Boolean)

object IngressMeta {

  @transient implicit lazy val formats: DefaultFormats.type = org.json4s.DefaultFormats

  def as(json: String): Option[List[IngressMeta]] = {
    Try(parse(json)) match {
      case Success(ok) =>
        ok match {
          case JArray(arr) =>
            val list = arr.map(x => {
              IngressMeta(
                addresses = (x \ "addresses").extractOpt[List[String]].getOrElse(List.empty[String]),
                port = (x \ "port").extractOpt[Integer].getOrElse(0),
                protocol = (x \ "protocol").extractOpt[String].getOrElse(null),
                serviceName = (x \ "serviceName").extractOpt[String].getOrElse(null),
                ingressName = (x \ "ingressName").extractOpt[String].getOrElse(null),
                hostname = (x \ "hostname").extractOpt[String].getOrElse(null),
                path = (x \ "path").extractOpt[String].getOrElse(null),
                allNodes = (x \ "allNodes").extractOpt[Boolean].getOrElse(false)
              )
            })
            Some(list)
          case _ => None
        }
      case Failure(_) => None
    }
  }

}
