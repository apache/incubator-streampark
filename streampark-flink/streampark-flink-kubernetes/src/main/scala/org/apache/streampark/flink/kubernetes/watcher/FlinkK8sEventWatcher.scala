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

package org.apache.streampark.flink.kubernetes.watcher

import org.apache.streampark.common.util.Logger
import org.apache.streampark.flink.kubernetes.{FlinkK8sWatchController, KubernetesRetriever}
import org.apache.streampark.flink.kubernetes.model.{K8sDeploymentEventCV, K8sEventKey}

import org.apache.flink.kubernetes.kubeclient.resources.{CompatibleKubernetesWatcher, CompKubernetesDeployment}
import org.apache.flink.kubernetes.shaded.io.fabric8.kubernetes.api.model.apps.Deployment
import org.apache.flink.kubernetes.shaded.io.fabric8.kubernetes.client.{KubernetesClient, Watcher}

import javax.annotation.concurrent.ThreadSafe

import scala.util.{Failure, Try}

/**
 * K8s Event Watcher for Flink Native-K8s Mode. Currently only flink-native-application mode events
 * would be tracked. The results of traced events would written into cachePool.
 */
@ThreadSafe
class FlinkK8sEventWatcher(implicit watchController: FlinkK8sWatchController)
  extends Logger
  with FlinkWatcher {

  private var k8sClient: KubernetesClient = _

  /** start watcher process */
  override def doStart(): Unit = {
    k8sClient = Try(KubernetesRetriever.newK8sClient()).getOrElse {
      logError("[flink-k8s] FlinkK8sEventWatcher fails to start.")
      return
    }
    doWatch()
    logInfo("[flink-k8s] FlinkK8sEventWatcher started.")
  }

  /** stop watcher process */
  override def doStop(): Unit = {
    k8sClient.close()
    k8sClient = null
    logInfo("[flink-k8s] FlinkK8sEventWatcher stopped.")
  }

  override def doClose(): Unit = {
    logInfo("[flink-k8s] FlinkK8sEventWatcher closed.")
  }

  override def doWatch(): Unit = {
    // watch k8s deployment events
    Try {
      k8sClient
        .apps()
        .deployments()
        .withLabel("type", "flink-native-kubernetes")
        .watch(new CompatibleKubernetesWatcher[Deployment, CompKubernetesDeployment] {
          override def eventReceived(action: Watcher.Action, event: Deployment): Unit = {
            handleDeploymentEvent(action, event)
          }
        })
    } match {
      case Failure(e) =>
        logError(s"k8sClient error: $e")
      case _ =>
    }
  }

  private def handleDeploymentEvent(action: Watcher.Action, event: Deployment): Unit = {
    val clusterId = event.getMetadata.getName
    val namespace = event.getMetadata.getNamespace
    // if (!cachePool.isInTracking(TrackId.onApplication(namespace, clusterId)))
    //  return
    // just tracking every flink-k8s-native event :)
    watchController.k8sDeploymentEvents.put(
      K8sEventKey(namespace, clusterId),
      K8sDeploymentEventCV(action, event, System.currentTimeMillis()))
  }

}
