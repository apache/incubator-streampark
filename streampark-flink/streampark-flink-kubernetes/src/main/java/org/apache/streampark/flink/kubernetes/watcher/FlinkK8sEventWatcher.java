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
import org.apache.flink.kubernetes.shaded.io.fabric8.kubernetes.api.model.apps.Deployment;
import org.apache.flink.kubernetes.shaded.io.fabric8.kubernetes.client.KubernetesClient;
import org.apache.flink.kubernetes.shaded.io.fabric8.kubernetes.client.Watcher;

import javax.annotation.concurrent.ThreadSafe;

/**
 * K8s Event Watcher for Flink Native-K8s Mode. Currently only flink-native-application mode events
 * would be tracked. The results of traced events would written into cachePool.
 */
@ThreadSafe
public class FlinkK8sEventWatcher extends FlinkWatcher {

    private final FlinkK8sWatchController watchController;

    private KubernetesClient k8sClient;

    public FlinkK8sEventWatcher(FlinkK8sWatchController watchController) {
        this.watchController = watchController;
    }

    @Override
    protected void doStart() {
        try {
            k8sClient = KubernetesRetriever.newK8sClient();
        } catch (Exception e) {
            logError("[flink-k8s] FlinkK8sEventWatcher fails to start.");
            return;
        }
        doWatch();
        logInfo("[flink-k8s] FlinkK8sEventWatcher started.");
    }

    @Override
    protected void doStop() {
        if (k8sClient != null) {
            k8sClient.close();
            k8sClient = null;
        }
        logInfo("[flink-k8s] FlinkK8sEventWatcher stopped.");
    }

    @Override
    protected void doClose() {
        logInfo("[flink-k8s] FlinkK8sEventWatcher closed.");
    }

    @Override
    public void doWatch() {
        try {
            k8sClient
                .apps()
                .deployments()
                .withLabel("type", "flink-native-kubernetes")
                .watch(
                    new CompatibleKubernetesWatcher<Deployment, CompKubernetesDeployment>() {

                        @Override
                        public void eventReceived(Watcher.Action action, Deployment event) {
                            handleDeploymentEvent(action, event);
                        }
                    });
        } catch (Exception e) {
            logError("k8sClient error: " + e);
        }
    }

    private void handleDeploymentEvent(Watcher.Action action, Deployment event) {
        String clusterId = event.getMetadata().getName();
        String namespace = event.getMetadata().getNamespace();
        watchController.k8sDeploymentEvents.put(
            K8sEventKey.builder().namespace(namespace).clusterId(clusterId).build(),
            K8sDeploymentEventCV.builder()
                .action(action)
                .event(event)
                .pollAckTime(System.currentTimeMillis())
                .build());
    }
}
