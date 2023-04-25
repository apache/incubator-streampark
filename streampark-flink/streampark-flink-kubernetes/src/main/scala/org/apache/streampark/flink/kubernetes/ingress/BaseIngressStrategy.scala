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

import org.apache.streampark.common.util.Utils._

import io.fabric8.kubernetes.client.DefaultKubernetesClient
import org.apache.commons.io.FileUtils
import org.apache.flink.client.program.ClusterClient
import org.json4s.{DefaultFormats, JArray}
import org.json4s.jackson.JsonMethods.parse

import java.io.File
import java.nio.file.{Files, Paths}

import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

abstract class BaseIngressStrategy extends IngressStrategy {
  override def ingressUrlAddress(
      nameSpace: String,
      clusterId: String,
      clusterClient: ClusterClient[_]): String = {
    val client = new DefaultKubernetesClient
    // for kubernetes 1.19+
    lazy val fromV1 =
      Option(client.network.v1.ingresses.inNamespace(nameSpace).withName(clusterId).get)
        .map(ingress => ingress.getSpec.getRules.get(0))
        .map(rule => rule.getHost -> rule.getHttp.getPaths.get(0).getPath)
    // for kubernetes 1.19-
    lazy val fromV1beta1 =
      Option(client.network.v1beta1.ingresses.inNamespace(nameSpace).withName(clusterId).get)
        .map(ingress => ingress.getSpec.getRules.get(0))
        .map(rule => rule.getHost -> rule.getHttp.getPaths.get(0).getPath)
    Try(
      fromV1
        .orElse(fromV1beta1)
        .map { case (host, path) => s"https://$host$path" }
        .getOrElse(clusterClient.getWebInterfaceURL)
    ).getOrElse(throw new RuntimeException("[StreamPark] get ingressUrlAddress error."))
  }

  override def prepareIngressTemplateFiles(
      buildWorkspace: String,
      ingressTemplates: String): String = {
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

  def configureIngress(ingressOutput: String): Unit = {
    close {
      val client = new DefaultKubernetesClient
      client.network.ingress
        .load(Files.newInputStream(Paths.get(ingressOutput)))
        .get()
      client
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
              val list = arr.map(
                x => {
                  IngressMeta(
                    addresses =
                      (x \ "addresses").extractOpt[List[String]].getOrElse(List.empty[String]),
                    port = (x \ "port").extractOpt[Integer].getOrElse(0),
                    protocol = (x \ "protocol").extractOpt[String].orNull,
                    serviceName = (x \ "serviceName").extractOpt[String].orNull,
                    ingressName = (x \ "ingressName").extractOpt[String].orNull,
                    hostname = (x \ "hostname").extractOpt[String].orNull,
                    path = (x \ "path").extractOpt[String].orNull,
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
}
