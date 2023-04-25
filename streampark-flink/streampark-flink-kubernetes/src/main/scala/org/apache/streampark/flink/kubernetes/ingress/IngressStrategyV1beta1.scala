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

import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder
import io.fabric8.kubernetes.client.DefaultKubernetesClient

import scala.collection.JavaConverters._
import scala.language.postfixOps
import scala.util.{Success, Try}

class IngressStrategyV1beta1 extends BaseIngressStrategy {
  override def configureIngress(domainName: String, clusterId: String, nameSpace: String): Unit = {
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
          "component" -> "ingress")

        val deployment = client
          .apps()
          .deployments()
          .inNamespace(nameSpace)
          .withName(clusterId)
          .get()

        val deploymentUid = if (deployment != null) {
          deployment.getMetadata.getUid
        } else {
          throw new RuntimeException(
            s"Deployment with name $clusterId not found in namespace $nameSpace")
        }

        // Create OwnerReference object
        val ownerReference = new OwnerReferenceBuilder()
          .withApiVersion("apps/v1")
          .withKind("Deployment")
          .withName(clusterId)
          .withUid(deploymentUid)
          .withController(true)
          .withBlockOwnerDeletion(true)
          .build()

        val ingress = new io.fabric8.kubernetes.api.model.networking.v1.IngressBuilder()
          .withNewMetadata()
          .withName(clusterId)
          .addToAnnotations(annotMap.asJava)
          .addToLabels(labelsMap.asJava)
          .addToOwnerReferences(ownerReference)
          .endMetadata()
          .withNewSpec()
          .addNewRule()
          .withHost(domainName)
          .withNewHttp()
          .addNewPath()
          .withPath(s"/$nameSpace/$clusterId/")
          .withPathType("ImplementationSpecific")
          .withNewBackend()
          .withNewService()
          .withName(s"$clusterId-rest")
          .withNewPort()
          .withName("rest")
          .endPort()
          .endService()
          .endBackend()
          .endPath()
          .addNewPath()
          .withPath(s"/$nameSpace/$clusterId" + "(/|$)(.*)")
          .withPathType("ImplementationSpecific")
          .withNewBackend()
          .withNewService()
          .withName(s"$clusterId-rest")
          .withNewPort()
          .withName("rest")
          .endPort()
          .endService()
          .endBackend()
          .endPath()
          .endHttp()
          .endRule()
          .endSpec()
          .build()
        client.network.v1.ingresses().inNamespace(nameSpace).create(ingress)

      case _ =>
    }
  }
}
