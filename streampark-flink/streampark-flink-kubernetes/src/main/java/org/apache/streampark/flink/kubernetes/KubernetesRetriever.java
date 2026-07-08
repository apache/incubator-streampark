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

package org.apache.streampark.flink.kubernetes;

import org.apache.streampark.common.util.AutoCloseUtils;
import org.apache.streampark.flink.kubernetes.enums.FlinkK8sDeployMode;
import org.apache.streampark.flink.kubernetes.ingress.IngressController;
import org.apache.streampark.flink.kubernetes.model.ClusterKey;

import org.apache.flink.client.cli.ClientOptions;
import org.apache.flink.client.deployment.ClusterDescriptor;
import org.apache.flink.client.deployment.DefaultClusterClientServiceLoader;
import org.apache.flink.client.program.ClusterClient;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.DeploymentOptions;
import org.apache.flink.configuration.RestOptions;
import org.apache.flink.kubernetes.KubernetesClusterDescriptor;
import org.apache.flink.kubernetes.configuration.KubernetesConfigOptions;
import org.apache.flink.kubernetes.shaded.io.fabric8.kubernetes.client.DefaultKubernetesClient;
import org.apache.flink.kubernetes.shaded.io.fabric8.kubernetes.client.KubernetesClient;
import org.apache.flink.kubernetes.shaded.io.fabric8.kubernetes.client.KubernetesClientException;
import org.apache.hc.core5.util.Timeout;

import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
public final class KubernetesRetriever {

    public static final Timeout FLINK_CLIENT_TIMEOUT_SEC =
        Timeout.ofMilliseconds(ClientOptions.CLIENT_TIMEOUT.defaultValue().toMillis());

    public static final Timeout FLINK_REST_AWAIT_TIMEOUT_SEC =
        Timeout.ofMilliseconds(RestOptions.AWAIT_LEADER_TIMEOUT.defaultValue().toMillis());

    private static final Map<String, Long> DEPLOYMENT_LOST_TIME = new HashMap<>();

    private static final DefaultClusterClientServiceLoader CLUSTER_CLIENT_SERVICE_LOADER =
        new DefaultClusterClientServiceLoader();

    private KubernetesRetriever() {
    }

    public static KubernetesClient newK8sClient() throws KubernetesClientException {
        return new DefaultKubernetesClient();
    }

    public static boolean checkK8sConnection() {
        try {
            return newK8sClient().getVersion() != null;
        } catch (Exception e) {
            return false;
        }
    }

    public static Optional<ClusterClient<String>> newFinkClusterClient(
                                                                       String clusterId, @Nullable String namespace,
                                                                       FlinkK8sDeployMode executeMode) {
        Configuration flinkConfig = new Configuration();
        flinkConfig.setString(DeploymentOptions.TARGET, executeMode.toString());
        flinkConfig.setString(KubernetesConfigOptions.CLUSTER_ID, clusterId);
        flinkConfig.set(ClientOptions.CLIENT_TIMEOUT, ClientOptions.CLIENT_TIMEOUT.defaultValue());
        flinkConfig.set(
            RestOptions.AWAIT_LEADER_TIMEOUT, RestOptions.AWAIT_LEADER_TIMEOUT.defaultValue());
        flinkConfig.set(
            RestOptions.RETRY_MAX_ATTEMPTS, RestOptions.RETRY_MAX_ATTEMPTS.defaultValue());
        if (namespace == null || namespace.isEmpty()) {
            flinkConfig.setString(
                KubernetesConfigOptions.NAMESPACE,
                KubernetesConfigOptions.NAMESPACE.defaultValue());
        } else {
            flinkConfig.setString(KubernetesConfigOptions.NAMESPACE, namespace);
        }
        try {
            ClusterDescriptor<?> clusterDescriptor =
                CLUSTER_CLIENT_SERVICE_LOADER
                    .getClusterClientFactory(flinkConfig)
                    .createClusterDescriptor(flinkConfig);
            KubernetesClusterDescriptor descriptor = (KubernetesClusterDescriptor) clusterDescriptor;
            ClusterClient<String> clusterClient =
                descriptor
                    .retrieve(flinkConfig.getString(KubernetesConfigOptions.CLUSTER_ID))
                    .getClusterClient();
            return Optional.of(clusterClient);
        } catch (Exception e) {
            log.error("Get flinkClient error, the error is: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    public static boolean isDeploymentExists(String namespace, String deploymentName) {
        try {
            return AutoCloseUtils.using(
                newK8sClient(),
                client -> client.apps()
                    .deployments()
                    .inNamespace(namespace)
                    .withLabel("type", "flink-native-kubernetes")
                    .list()
                    .getItems()
                    .stream()
                    .anyMatch(
                        deployment -> deploymentName.equals(
                            deployment.getMetadata().getName())),
                error -> handleDeploymentExistsError(namespace, deploymentName, error));
        } catch (Exception e) {
            return handleDeploymentExistsError(namespace, deploymentName, e);
        }
    }

    private static boolean handleDeploymentExistsError(
                                                       String namespace, String deploymentName, Throwable e) {
        log.warn(
            "[StreamPark] check deploymentExists WARN, namespace: {}, deploymentName: {}, error: {}",
            namespace,
            deploymentName,
            e.getMessage());
        String key = namespace + "_" + deploymentName;
        Long lostTime = DEPLOYMENT_LOST_TIME.get(key);
        if (lostTime != null) {
            long timeOut = 1000 * 60 * 3L;
            if (System.currentTimeMillis() - lostTime >= timeOut) {
                log.error(
                    "[StreamPark] check deploymentExists Failed, namespace: {}, deploymentName: {}, detail: deployment: {} Not Found more than 3 minutes, {}",
                    namespace,
                    deploymentName,
                    deploymentName,
                    e.getMessage());
                DEPLOYMENT_LOST_TIME.remove(key);
                return false;
            }
            return true;
        }
        DEPLOYMENT_LOST_TIME.put(key, System.currentTimeMillis());
        return true;
    }

    public static Optional<String> retrieveFlinkRestUrl(ClusterKey clusterKey) {
        Optional<ClusterClient<String>> client =
            newFinkClusterClient(
                clusterKey.clusterId(), clusterKey.namespace(), clusterKey.executeMode());
        if (!client.isPresent()) {
            return Optional.empty();
        }
        String url =
            IngressController.getIngressUrlAddress(
                clusterKey.namespace(), clusterKey.clusterId(), client.get());
        log.info("retrieve flink jobManager rest url: {}", url);
        return Optional.of(url);
    }
}
