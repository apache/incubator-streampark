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

package org.apache.streampark.flink.kubernetes.helper

import com.google.common.base.Charsets
import com.google.common.io.Files
import org.apache.streampark.common.util.{Logger, SystemPropertyUtils}
import org.apache.streampark.common.util.Utils.tryWithResource
import org.apache.streampark.flink.kubernetes.KubernetesRetriever
import io.fabric8.kubernetes.api.model.Pod
import io.fabric8.kubernetes.client.DefaultKubernetesClient

import java.io.File
import scala.collection.JavaConversions._
import scala.util.{Success, Try}

object KubernetesDeploymentHelper extends Logger {

  private[this] def getPods(nameSpace: String, deploymentName: String): List[Pod] = {
    tryWithResource(KubernetesRetriever.newK8sClient()) { client =>
      Try {
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
      }.getOrElse(List.empty[Pod])
    }
  }

  def getDeploymentStatusChanges(nameSpace: String, deploymentName: String): Boolean = {
    Try {
      val pods = getPods(nameSpace, deploymentName)
      pods.head.getStatus.getContainerStatuses.head.getLastState.getTerminated != null
    }.getOrElse(true)
  }

  def getTheNumberOfTaskDeploymentRetries(nameSpace: String, deploymentName: String): Integer = {
    val pods = getPods(nameSpace, deploymentName)
    pods.head.getStatus.getContainerStatuses.head.getRestartCount
  }

  def deleteTaskDeployment(nameSpace: String, deploymentName: String): Boolean = {
    tryWithResource(KubernetesRetriever.newK8sClient()) { client =>
      Try {
        val r = client.apps.deployments
          .inNamespace(nameSpace)
          .withName(deploymentName)
          .delete
        Boolean.unbox(r)
      }.getOrElse(false)
    }
  }

  def isTheK8sConnectionNormal(): Boolean = {
    Try(new DefaultKubernetesClient) match {
      case Success(client) =>
        client.close()
        true
      case _ => false
    }
  }

  def watchDeploymentLog(nameSpace: String, jobName: String, jobId: String): String = {
    tryWithResource(KubernetesRetriever.newK8sClient()) { client =>
      val path = KubernetesDeploymentHelper.getJobLog(jobId)
      val file = new File(path)
      val log = client.apps.deployments.inNamespace(nameSpace).withName(jobName).getLog
      Files.asCharSink(file, Charsets.UTF_8).write(log)
      path
    }
  }

  def watchPodTerminatedLog(nameSpace: String, jobName: String, jobId: String): String = {
    tryWithResource(KubernetesRetriever.newK8sClient()) { client =>
      Try {
        val podName = getPods(nameSpace, jobName).head.getMetadata.getName
        val path = KubernetesDeploymentHelper.getJobErrorLog(jobId)
        val file = new File(path)
        val log = client.pods.inNamespace(nameSpace).withName(podName).terminated().withPrettyOutput.getLog
        Files.asCharSink(file, Charsets.UTF_8).write(log)
        path
      }.getOrElse(null)
    }(error => throw error)
  }

  def deleteTaskConfigMap(nameSpace: String, deploymentName: String): Boolean = {
    tryWithResource(KubernetesRetriever.newK8sClient()) { client =>
      Try {
        val r = client.configMaps()
          .inNamespace(nameSpace)
          .withLabel("app", deploymentName)
          .delete
        Boolean.unbox(r)
      }.getOrElse(false)
    }
  }

  private[kubernetes] def getJobLog(jobId: String): String = {
    val tmpPath = SystemPropertyUtils.getTmpdir()
    s"$tmpPath/$jobId.log"
  }

  private[kubernetes] def getJobErrorLog(jobId: String): String = {
    val tmpPath = SystemPropertyUtils.getTmpdir()
    s"$tmpPath/${jobId}_err.log"
  }

}
