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

package com.streamxhub.streamx.flink.kubernetes.helper


import com.streamxhub.streamx.common.util.Utils
import io.fabric8.kubernetes.api.model.Pod
import io.fabric8.kubernetes.client.DefaultKubernetesClient

import scala.collection.JavaConversions._
import scala.util.Try

object KubernetesDeploymentHelper {

  private[this] def getPods(nameSpace: String, deploymentName: String): List[Pod] = {
    Try {
      Utils.tryWithResource(new DefaultKubernetesClient) { client =>
        client.pods.inNamespace(nameSpace)
          .withLabels {
            client.apps.deployments
              .inNamespace(nameSpace)
              .withName(deploymentName)
              .get
              .getSpec
              .getSelector
              .getMatchLabels
          }.list.getItems.toList
      }
    }.getOrElse(List.empty[Pod])
  }

  def getDeploymentStatusChanges(nameSpace: String, deploymentName: String): Boolean = {
    val pods = getPods(nameSpace, deploymentName)
    pods.head.getStatus.getContainerStatuses.head.getLastState.getTerminated != null
  }

  def getTheNumberOfTaskDeploymentRetries(nameSpace: String, deploymentName: String): Integer = {
    val pods = getPods(nameSpace, deploymentName)
    pods.head.getStatus.getContainerStatuses.head.getRestartCount
  }

  def deleteTaskDeployment(nameSpace: String, deploymentName: String): Unit = {
    Utils.tryWithResource(new DefaultKubernetesClient) { client =>
      client.apps.deployments
        .inNamespace(nameSpace)
        .withName(deploymentName)
        .delete
    }
  }

  def isTheK8sConnectionNormal(): Boolean = try {
    Utils.tryWithResource(new DefaultKubernetesClient) { client =>
      client != null
    }
  }

}
