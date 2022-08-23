/*
 * Copyright 2019 The StreamX Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.flink.kubernetes.helper


import com.google.common.base.Charsets
import com.google.common.io.Files
import com.streamxhub.streamx.common.util.Utils
import io.fabric8.kubernetes.api.model.Pod
import io.fabric8.kubernetes.client.DefaultKubernetesClient

import java.io.File
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

  def watchDeploymentLog(nameSpace: String, jobName: String): String = try {
    Utils.tryWithResource(new DefaultKubernetesClient) { client =>
      val projectPath = new File("").getCanonicalPath
      val path = s"$projectPath/${nameSpace}_$jobName.log"
      val file = new File(path)
      val log = client.apps.deployments.inNamespace(nameSpace).withName(jobName).getLog
      Files.asCharSink(file, Charsets.UTF_8).write(log)
      path
    }
  }
}
