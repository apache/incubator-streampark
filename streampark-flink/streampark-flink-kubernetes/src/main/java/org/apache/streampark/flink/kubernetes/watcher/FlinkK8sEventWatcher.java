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

package org.apache.streampark.flink.kubernetes.watcher;

import org.apache.streampark.flink.kubernetes.FlinkK8sWatchController;
import org.apache.streampark.flink.kubernetes.KubernetesRetriever;
import org.apache.streampark.flink.kubernetes.model.K8sDeploymentEventCV;
import org.apache.streampark.flink.kubernetes.model.K8sEventKey;

import org.apache.flink.kubernetes.kubeclient.resources.CompKubernetesDeployment;
import org.apache.flink.kubernetes.kubeclient.resources.CompatibleKubernetesWatcher;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlinkK8sEventWatcher implements FlinkWatcher {

  private static final Logger LOG = LoggerFactory.getLogger(FlinkK8sEventWatcher.class);

  private final FlinkK8sWatchController flinkK8sWatchController;

  private KubernetesClient kubernetesClient;

  public FlinkK8sEventWatcher(FlinkK8sWatchController flinkK8sWatchController) {
    this.flinkK8sWatchController = flinkK8sWatchController;
  }

  public FlinkK8sWatchController getFlinkK8sWatchController() {
    return flinkK8sWatchController;
  }

  @Override
  public void doStart() {
    kubernetesClient = KubernetesRetriever.newK8sClient();
    doWatch();
    LOG.info("[flink-k8s] Flink k8s event watcher started");
  }

  @Override
  public void doStop() {
    kubernetesClient.close();
    kubernetesClient = null;
    LOG.info("[flink-k8s] Flink k8s event watcher closed");
  }

  @Override
  public void doClose() {
    LOG.info("[flink-k8s] Flink k8s event watcher stopped");
  }

  @Override
  public void doWatch() {
    kubernetesClient
        .apps()
        .deployments()
        .withLabel("type", "flink-native-kubernetes")
        // TODO: verify that if or not release the resources
        .watch(
            new CompatibleKubernetesWatcher<Deployment, CompKubernetesDeployment>() {
              @Override
              public void eventReceived(Action action, Deployment deployment) {
                handleDeploymentEvent(action, deployment);
              }
            });
  }

  private void handleDeploymentEvent(Watcher.Action action, Deployment event) {
    String clusterId = event.getMetadata().getName();
    String namespace = event.getMetadata().getNamespace();
    flinkK8sWatchController
        .getK8sDeploymentEventCache()
        .put(
            new K8sEventKey(namespace, clusterId),
            new K8sDeploymentEventCV(action, event, System.currentTimeMillis()));
  }
}
