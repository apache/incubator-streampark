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

package org.apache.streampark.flink.client.bean

import org.apache.streampark.common.enums.FlinkK8sRestExposedType
import org.apache.streampark.flink.kubernetes.v2.model.IngressDef

import javax.annotation.Nullable

import java.util
import java.util.{Map => JMap}

/**
 * TODO Need to display more K8s submission parameters in the front-end UI.
 *
 * It will eventually be converted to
 * [[org.apache.streampark.flink.kubernetes.v2.model.FlinkDeploymentDef]]
 *
 * The logic of conversion is located at：
 * [[org.apache.streampark.flink.client.impl.KubernetesApplicationClientV2#genFlinkDeployDef]]
 */
case class KubernetesSubmitParam(
    clusterId: String,
    kubernetesNamespace: String,
    kubernetesName: Option[String],
    baseImage: Option[String] = None,
    imagePullPolicy: Option[String] = None,
    serviceAccount: Option[String] = None,
    podTemplate: Option[String] = None,
    jobManagerCpu: Option[Double] = None,
    jobManagerMemory: Option[String] = None,
    jobManagerEphemeralStorage: Option[String] = None,
    jobManagerPodTemplate: Option[String] = None,
    taskManagerCpu: Option[Double] = None,
    taskManagerMemory: Option[String] = None,
    taskManagerEphemeralStorage: Option[String] = None,
    taskManagerPodTemplate: Option[String] = None,
    logConfiguration: JMap[String, String] = new util.HashMap[String, String](),
    flinkRestExposedType: Option[FlinkK8sRestExposedType] = None,
    ingressDefinition: Option[IngressDef] = None
)

object KubernetesSubmitParam {

  /**
   * Compatible with streampark old native k8s submission parameters.
   *
   * @param clusterId
   *   flink cluster id in k8s cluster.
   * @param kubernetesNamespace
   *   k8s namespace.
   * @param flinkRestExposedType
   *   flink rest-service exposed type on k8s cluster.
   */
  def apply(
      clusterId: String,
      kubernetesName: String,
      kubernetesNamespace: String,
      baseImage: String,
      @Nullable flinkRestExposedType: FlinkK8sRestExposedType,
      @Nullable ingressDefinition: IngressDef): KubernetesSubmitParam =
    KubernetesSubmitParam(
      clusterId = clusterId,
      kubernetesNamespace = kubernetesNamespace,
      kubernetesName = Option(kubernetesName),
      baseImage = Some(baseImage),
      flinkRestExposedType = Option(flinkRestExposedType),
      ingressDefinition = Option(ingressDefinition)
    )
}
