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
package com.streamxhub.streamx.flink.kubernetes.watcher

import com.streamxhub.streamx.common.util.Logger
import com.streamxhub.streamx.flink.kubernetes.model.{K8sDeploymentEventCV, K8sEventKey, TrackId}
import com.streamxhub.streamx.flink.kubernetes.{FlinkTrackCachePool, KubernetesRetriever}
import io.fabric8.kubernetes.api.model.apps.Deployment
import io.fabric8.kubernetes.client.{KubernetesClient, KubernetesClientException, Watcher}

import javax.annotation.concurrent.ThreadSafe

/**
 * K8s Event Watcher for Flink Native-K8s Mode.
 * Currently only flink-native-application mode events would be tracked.
 * The results of traced events would written into cachePool.
 *
 * author:Al-assad
 */
@ThreadSafe
class FlinkK8sEventWatcher(cachePool: FlinkTrackCachePool) extends Logger with FlinkWatcher {

  private var k8sClient: KubernetesClient = _

  // status of whether FlinkK8sEventWatcher has already started
  @volatile private var isStarted = false

  // whether only recording events that are in FlinkTrackCachePool.trackIds, just for debug
  private val FILTER_MODE = true

  /**
   * start watcher process
   */
  override def start(): Unit = this.synchronized {
    if (!isStarted) {
      k8sClient = KubernetesRetriever.newK8sClient()
      prepareEventWatcher(k8sClient)
      isStarted = true
      logInfo("[flink-k8s] FlinkK8sEventWatcher started.")
    }
  }

  /**
   * stop watcher process
   */
  override def stop(): Unit = this.synchronized {
    if (isStarted) {
      k8sClient.close()
      k8sClient = null
      isStarted = false
      logInfo("[flink-k8s] FlinkK8sEventWatcher stopped.")
    }
  }

  override def close(): Unit = this.synchronized {
    if (isStarted) {
      k8sClient.close()
      k8sClient = null
      isStarted = false
      logInfo("[flink-k8s] FlinkK8sEventWatcher closed.")
    }
  }

  private def prepareEventWatcher(k8sClient: KubernetesClient): Unit = {
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

  private def handleDeploymentEvent(action: Watcher.Action, event: Deployment): Unit = {
    val clusterId = event.getMetadata.getName
    val namespace = event.getMetadata.getNamespace
    if (FILTER_MODE && !cachePool.isInTracking(TrackId.onApplication(namespace, clusterId))) {
      return
    }
    cachePool.k8sDeploymentEvents.put(
      K8sEventKey(namespace, clusterId), K8sDeploymentEventCV(action, event, System.currentTimeMillis()))
  }


}
