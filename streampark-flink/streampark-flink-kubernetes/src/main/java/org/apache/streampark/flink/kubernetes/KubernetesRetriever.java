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

import org.apache.flink.client.cli.ClientOptions;
import org.apache.flink.client.deployment.ClusterClientFactory;
import org.apache.flink.client.deployment.DefaultClusterClientServiceLoader;
import org.apache.flink.client.program.ClusterClient;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.DeploymentOptions;
import org.apache.flink.configuration.RestOptions;
import org.apache.flink.kubernetes.KubernetesClusterDescriptor;
import org.apache.flink.kubernetes.configuration.KubernetesConfigOptions;
import org.apache.streampark.flink.kubernetes.enums.FlinkK8sExecuteMode;
import org.apache.streampark.flink.kubernetes.model.ClusterKey;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class KubernetesRetriever {

  // see org.apache.flink.client.cli.ClientOptions.CLIENT_TIMEOUT
  public static final long FLINK_CLIENT_TIMEOUT_SEC = 30L;
  // see org.apache.flink.configuration.RestOptions.AWAIT_LEADER_TIMEOUT
  public static final long FLINK_REST_AWAIT_TIMEOUT_SEC = 10L;
  // see org.apache.flink.configuration.RestOptions.RETRY_MAX_ATTEMPTS
  public static final int FLINK_REST_RETRY_MAX_ATTEMPTS = 2;

  private static final DefaultClusterClientServiceLoader DEFAULT_CLUSTER_CLIENT_SERVICE_LOADER =
      new DefaultClusterClientServiceLoader();

  /** get new KubernetesClient */
  public static KubernetesClient newK8sClient() {
    return new DefaultKubernetesClient();
  }

  /** check connection of kubernetes cluster */
  public static boolean checkK8sConnection() {
    try (KubernetesClient kubernetesClient = newK8sClient()) {
      return kubernetesClient.getVersion() != null;
    }
  }

  public static Optional<ClusterClient<String>> newFlinkClusterClient(
      String clusterId, String namespace, FlinkK8sExecuteMode executeMode) {
    Configuration flinkConfig = new Configuration();
    flinkConfig.setString(DeploymentOptions.TARGET, executeMode.getValue());
    flinkConfig.setString(KubernetesConfigOptions.CLUSTER_ID, clusterId);
    flinkConfig.set(ClientOptions.CLIENT_TIMEOUT, Duration.ofSeconds(FLINK_CLIENT_TIMEOUT_SEC));
    flinkConfig.setLong(RestOptions.AWAIT_LEADER_TIMEOUT, FLINK_REST_AWAIT_TIMEOUT_SEC * 1000);
    flinkConfig.setInteger(RestOptions.RETRY_MAX_ATTEMPTS, FLINK_REST_RETRY_MAX_ATTEMPTS);
    if (namespace.isEmpty()) {
      flinkConfig.setString(
          KubernetesConfigOptions.NAMESPACE, KubernetesConfigOptions.NAMESPACE.defaultValue());
    } else {
      flinkConfig.setString(KubernetesConfigOptions.NAMESPACE, namespace);
    }
    ClusterClientFactory<String> clusterClientFactory =
        DEFAULT_CLUSTER_CLIENT_SERVICE_LOADER.getClusterClientFactory(flinkConfig);
    KubernetesClusterDescriptor clusterDescriptor =
        (KubernetesClusterDescriptor) clusterClientFactory.createClusterDescriptor(flinkConfig);
    ClusterClient<String> clusterClient =
        clusterDescriptor
            .retrieve(flinkConfig.get(KubernetesConfigOptions.CLUSTER_ID))
            .getClusterClient();
    return Optional.of(clusterClient);
  }

  public static boolean isDeploymentExists(String name, String namespace) {
    try (KubernetesClient kubernetesClient = newK8sClient()) {
      List<String> types =
          kubernetesClient.apps().deployments().inNamespace(namespace)
              .withLabel("type", "flink-native-kubernetes").list().getItems().stream()
              .map(deployment -> deployment.getMetadata().getName())
              .collect(Collectors.toList());
      return types.contains(name);
    }
  }

  public static Optional<String> retrieveFlinkRestUrl(ClusterKey clusterKey) {
    Optional<ClusterClient<String>> clusterClientOptional =
        newFlinkClusterClient(
            clusterKey.getClusterId(), clusterKey.getNamespace(), clusterKey.getExecuteMode());
    if (clusterClientOptional.isPresent()) {
      try (ClusterClient<String> clusterClient = clusterClientOptional.get()) {
        String url =
            IngressController.ingressUrlAddress(
                clusterKey.getNamespace(), clusterKey.getClusterId(), clusterClient);
        return Optional.of(url);
      }
    } else {
      return Optional.empty();
    }
  }
}
