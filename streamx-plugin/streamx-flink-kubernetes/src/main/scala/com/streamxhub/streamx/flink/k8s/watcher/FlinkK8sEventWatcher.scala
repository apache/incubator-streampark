/*
 * Copyright (c) 2021 The StreamX Project
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
package com.streamxhub.streamx.flink.k8s.watcher

import com.streamxhub.streamx.common.util.Logger
import com.streamxhub.streamx.flink.k8s.model.{K8sDeploymentEventCV, K8sEventKey, K8sServiceEventCV, TrkId}
import com.streamxhub.streamx.flink.k8s.{FlinkTRKCachePool, KubernetesRetriever}
import io.fabric8.kubernetes.api.model.Service
import io.fabric8.kubernetes.api.model.apps.Deployment
import io.fabric8.kubernetes.client.{KubernetesClient, KubernetesClientException, Watcher}

/**
 * K8s Event Watcher for Flink Native-K8s Mode.
 * Currently only flink-native-application mode events would be tracked.
 * The results of tracked events would written into cachePool.
 *
 * auther:Al-assad
 *
 * @param cachePool cache pool
 */
class FlinkK8sEventWatcher(cachePool: FlinkTRKCachePool) extends Logger {

  private var k8sClient: KubernetesClient = _
  // just for debug
  private val FILTER_MODE = true


  /**
   * start wtacher process
   */
  def start(): Unit = {
    if (k8sClient != null) {
      return
    }
    k8sClient = KubernetesRetriever.newK8sClient()

    // watch k8s service events
    k8sClient.services()
      .withLabel("type", "flink-native-kubernetes")
      .watch(new Watcher[Service]() {
        override def eventReceived(action: Watcher.Action, event: Service): Unit = {
          handleServiceEvent(action, event)
        }

        override def onClose(e: KubernetesClientException): Unit = {
          logInfo(s"K8sEventWatcher[Kind=Service] stop, message=${e.getMessage}")
        }
      })

    // watch k8s deployment events
    k8sClient.apps().deployments()
      .withLabel("type", "flink-native-kubernetes")
      .watch(new Watcher[Deployment]() {
        override def eventReceived(action: Watcher.Action, event: Deployment): Unit = {
          handleDeploymentEvent(action, event)
        }

        override def onClose(e: KubernetesClientException): Unit = {
          logInfo(s"K8sEventWatcher[Kind=Deployment] stop, message=${e.getMessage}")
        }
      })
  }


  /**
   * stop watcher process
   */
  def stop(): Unit = {
    if (k8sClient == null) return
    k8sClient.close()
    k8sClient = null
  }

  /**
   * restart watcher process
   */
  def restart(): Unit = {
    stop()
    start()
  }

  //noinspection DuplicatedCode
  private def handleServiceEvent(action: Watcher.Action, event: Service): Unit = {
    val clusterId = event.getMetadata.getName
    val namespace = event.getMetadata.getNamespace
    if (FILTER_MODE && !cachePool.isInTracking(TrkId.onApplication(namespace, clusterId))) {
      return
    }
    cachePool.k8sServiceEvents.put(
      K8sEventKey(namespace, clusterId), K8sServiceEventCV(action, event, System.currentTimeMillis()))
  }

  //noinspection DuplicatedCode
  private def handleDeploymentEvent(action: Watcher.Action, event: Deployment): Unit = {
    val clusterId = event.getMetadata.getName
    val namespace = event.getMetadata.getNamespace
    if (FILTER_MODE && !cachePool.isInTracking(TrkId.onApplication(namespace, clusterId))) {
      return
    }
    cachePool.k8sDeploymentEvents.put(
      K8sEventKey(namespace, clusterId), K8sDeploymentEventCV(action, event, System.currentTimeMillis()))
  }


}
