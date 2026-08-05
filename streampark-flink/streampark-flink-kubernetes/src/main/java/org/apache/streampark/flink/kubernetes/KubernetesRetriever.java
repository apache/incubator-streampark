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
import org.apache.streampark.common.util.LoggerSupport;
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
import org.apache.flink.kubernetes.configuration.KubernetesConfigOptions;
import org.apache.flink.kubernetes.shaded.io.fabric8.kubernetes.client.KubernetesClient;
import org.apache.flink.kubernetes.shaded.io.fabric8.kubernetes.client.KubernetesClientException;
import org.apache.hc.core5.util.Timeout;

import javax.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class KubernetesRetriever extends LoggerSupport {

    private static final KubernetesRetriever INSTANCE = new KubernetesRetriever();

    /** See {@link org.apache.flink.client.cli.ClientOptions#CLIENT_TIMEOUT}. */
    public static final Timeout FLINK_CLIENT_TIMEOUT_SEC =
        Timeout.ofMilliseconds(ClientOptions.CLIENT_TIMEOUT.defaultValue().toMillis());

    /** See {@link org.apache.flink.configuration.RestOptions#AWAIT_LEADER_TIMEOUT}. */
    public static final Timeout FLINK_REST_AWAIT_TIMEOUT_SEC =
        Timeout.ofMilliseconds(RestOptions.AWAIT_LEADER_TIMEOUT.defaultValue().toMillis());

    private static final Map<String, Long> DEPLOYMENT_LOST_TIME = new HashMap<>();

    private static final DefaultClusterClientServiceLoader CLUSTER_CLIENT_SERVICE_LOADER =
        new DefaultClusterClientServiceLoader();

    private KubernetesRetriever() {
    }

    /** get new KubernetesClient */
    public static KubernetesClient newK8sClient() throws KubernetesClientException {
        return new org.apache.flink.kubernetes.shaded.io.fabric8.kubernetes.client.DefaultKubernetesClient();
    }

    /** check connection of kubernetes cluster */
    public static boolean checkK8sConnection() {
        try (KubernetesClient client = newK8sClient()) {
            return client.getVersion() != null;
        } catch (Exception e) {
            return false;
        }
    }

    /** get new flink cluster client of kubernetes mode */
    public static Optional<ClusterClient<String>> newFinkClusterClient(
                                                                       String clusterId,
                                                                       @Nullable String namespace,
                                                                       FlinkK8sDeployMode executeMode) {
        Configuration flinkConfig = new Configuration();
        flinkConfig.setString(DeploymentOptions.TARGET, executeMode.toString());
        flinkConfig.setString(KubernetesConfigOptions.CLUSTER_ID, clusterId);
        flinkConfig.set(ClientOptions.CLIENT_TIMEOUT, ClientOptions.CLIENT_TIMEOUT.defaultValue());
        flinkConfig.set(RestOptions.AWAIT_LEADER_TIMEOUT, RestOptions.AWAIT_LEADER_TIMEOUT.defaultValue());
        flinkConfig.set(RestOptions.RETRY_MAX_ATTEMPTS, RestOptions.RETRY_MAX_ATTEMPTS.defaultValue());
        if (namespace == null || namespace.isEmpty()) {
            flinkConfig.setString(
                KubernetesConfigOptions.NAMESPACE, KubernetesConfigOptions.NAMESPACE.defaultValue());
        } else {
            flinkConfig.setString(KubernetesConfigOptions.NAMESPACE, namespace);
        }

        try {
            ClusterDescriptor<String> clusterDescriptor =
                (ClusterDescriptor<String>) (ClusterDescriptor<?>) CLUSTER_CLIENT_SERVICE_LOADER
                    .getClusterClientFactory(flinkConfig)
                    .createClusterDescriptor(flinkConfig);
            return AutoCloseUtils.using(
                clusterDescriptor,
                descriptor -> {
                    try {
                        return Optional.of(
                            descriptor
                                .retrieve(flinkConfig.getString(KubernetesConfigOptions.CLUSTER_ID))
                                .getClusterClient());
                    } catch (Exception e) {
                        INSTANCE.logError("Get flinkClient error, the error is: " + e);
                        return Optional.empty();
                    }
                });
        } catch (Exception e) {
            INSTANCE.logError("Get flinkClient error, the error is: " + e);
            return Optional.empty();
        }
    }

    /**
     * check whether deployment exists on kubernetes cluster
     *
     * @param namespace deployment namespace
     * @param deploymentName deployment name
     */
    public static boolean isDeploymentExists(String namespace, String deploymentName) {
        return AutoCloseUtils.using(
            newK8sClient(),
            client -> client.apps()
                .deployments()
                .inNamespace(namespace)
                .withLabel("type", "flink-native-kubernetes")
                .list()
                .getItems()
                .stream()
                .anyMatch(item -> deploymentName.equals(item.getMetadata().getName())),
            e -> {
                INSTANCE.logWarn(
                    "[StreamPark] check deploymentExists WARN,\n"
                        + "namespace: "
                        + namespace
                        + ",\n"
                        + "deploymentName: "
                        + deploymentName
                        + ",\n"
                        + "error: "
                        + e);
                String key = namespace + "_" + deploymentName;
                Long time = DEPLOYMENT_LOST_TIME.get(key);
                if (time != null) {
                    long timeOut = 1000L * 60 * 3;
                    if (System.currentTimeMillis() - time >= timeOut) {
                        INSTANCE.logError(
                            "[StreamPark] check deploymentExists Failed,\n"
                                + "namespace: "
                                + namespace
                                + ",\n"
                                + "deploymentName: "
                                + deploymentName
                                + ",\n"
                                + "detail: deployment: "
                                + deploymentName
                                + " Not Found more than 3 minutes, "
                                + e);
                        DEPLOYMENT_LOST_TIME.remove(key);
                        return false;
                    }
                    return true;
                }
                DEPLOYMENT_LOST_TIME.put(key, System.currentTimeMillis());
                return true;
            });
    }

    /** retrieve flink jobManager rest url */
    public static Optional<String> retrieveFlinkRestUrl(ClusterKey clusterKey) {
        Optional<ClusterClient<String>> client =
            newFinkClusterClient(clusterKey.clusterId(), clusterKey.namespace(), clusterKey.executeMode());
        if (!client.isPresent()) {
            return Optional.empty();
        }
        try (ClusterClient<String> clusterClient = client.get()) {
            String url =
                IngressController.getIngressUrlAddress(
                    clusterKey.namespace(), clusterKey.clusterId(), clusterClient);
            INSTANCE.logger().info("retrieve flink jobManager rest url: " + url);
            return Optional.of(url);
        } catch (Exception e) {
            INSTANCE.logError("retrieve flink jobManager rest url error: " + e);
            return Optional.empty();
        }
    }
}
