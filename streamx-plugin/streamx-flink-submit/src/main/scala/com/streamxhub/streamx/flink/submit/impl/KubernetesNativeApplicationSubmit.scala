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

import com.streamxhub.streamx.common.enums.ExecutionMode
import com.streamxhub.streamx.flink.submit.`trait`.KubernetesNativeSubmitTrait
import com.streamxhub.streamx.flink.submit.{SubmitRequest, SubmitResponse}
import org.apache.flink.client.deployment.application.ApplicationConfiguration
import org.apache.flink.client.program.ClusterClient
import org.apache.flink.kubernetes.KubernetesClusterDescriptor
import org.apache.flink.kubernetes.configuration.KubernetesConfigOptions

import java.lang

/**
 * kubernetes native application mode submit
 */
object KubernetesNativeApplicationSubmit extends KubernetesNativeSubmitTrait {

  // todo request refactoring of submitRequest
  override def doSubmit(submitRequest: SubmitRequest): SubmitResponse = {

    val flinkConfig = extractEffectiveFlinkConfig(submitRequest)
    assert(flinkConfig.getOptional(KubernetesConfigOptions.CLUSTER_ID).isPresent)
    assert(flinkConfig.getOptional(KubernetesConfigOptions.CONTAINER_IMAGE).isPresent)

    var clusterDescriptor: KubernetesClusterDescriptor = null
    var clusterClient: ClusterClient[String] = null

    try {
      val (descriptor, clusterSpecification) = getK8sClusterDescriptorAndSpecification(flinkConfig)
      clusterDescriptor = descriptor
      val applicationConfig = ApplicationConfiguration.fromConfiguration(flinkConfig)
      clusterClient = clusterDescriptor
        .deployApplicationCluster(clusterSpecification, applicationConfig)
        .getClusterClient

      val appId = clusterClient.getClusterId
      // todo request refactoring of SubmitResponse
      SubmitResponse(null, flinkConfig)

    } finally {
      if (clusterClient != null) clusterClient.close()
      if (clusterDescriptor != null) clusterDescriptor.close()
    }

  }


  override def doStop(flinkHome: String, appId: String, jobStringId: String, savePoint: lang.Boolean, drain: lang.Boolean): String = {
    doStop(ExecutionMode.KUBERNETES_NATIVE_APPLICATION, flinkHome, appId, jobStringId, savePoint, drain)
  }


}
