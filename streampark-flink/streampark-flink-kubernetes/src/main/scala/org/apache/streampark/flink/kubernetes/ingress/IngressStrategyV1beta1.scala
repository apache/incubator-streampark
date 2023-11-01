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

import org.apache.streampark.common.util.ImplicitsUtils._

import io.fabric8.kubernetes.api.model.IntOrString
import io.fabric8.kubernetes.api.model.networking.v1beta1.IngressBuilder
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import org.apache.flink.client.program.ClusterClient

import scala.collection.JavaConverters._
import scala.language.postfixOps
import scala.util.Try

class IngressStrategyV1beta1 extends IngressStrategy {

  override def ingressUrlAddress(
      nameSpace: String,
      clusterId: String,
      clusterClient: ClusterClient[_]): String = {

    new DefaultKubernetesClient().autoClose(
      client =>
        Try {
          Option(client.network.v1beta1.ingresses.inNamespace(nameSpace).withName(clusterId).get)
            .map(ingress => ingress.getSpec.getRules.get(0))
            .map(rule => rule.getHost -> rule.getHttp.getPaths.get(0).getPath)
            .map { case (host, path) => s"http://$host$path" }
            .getOrElse(clusterClient.getWebInterfaceURL)
        }.recover {
          case e =>
            throw new RuntimeException(s"[StreamPark] get ingressUrlAddress error: $e")
        }.get)
  }

  override def configureIngress(domainName: String, clusterId: String, nameSpace: String): Unit = {
    new DefaultKubernetesClient().autoClose(
      client => {
        val ownerReference = getOwnerReference(nameSpace, clusterId, client)
        val ingress = new IngressBuilder()
          .withNewMetadata()
          .withName(clusterId)
          .addToAnnotations(buildIngressAnnotations(clusterId, nameSpace).asJava)
          .addToLabels(buildIngressLabels(clusterId).asJava)
          .addToOwnerReferences(ownerReference)
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
          .build()
        client.network.ingress.inNamespace(nameSpace).create(ingress)
      })
  }
}
