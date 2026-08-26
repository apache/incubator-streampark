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

package org.apache.streampark.e2e.cases;

import org.apache.streampark.e2e.core.StreamParkApi;
import org.apache.streampark.e2e.core.api.ApiClient;
import org.apache.streampark.e2e.core.api.flink.FlinkApiConstants;
import org.apache.streampark.e2e.core.api.flink.FlinkApiSupport;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@StreamParkApi(composeFiles = "docker/flink-2.0-on-k8s/docker-compose.yaml")
public class Flink200OnK8sClusterDeployTest {

    public static ApiClient api;

    private static FlinkApiSupport flink;

    private static final String flinkName = "flink-2.0.2";

    private static final String flinkHome = "/flink-2.0.2";

    private static final String clusterName = "flink_2.0.2_k8s_cluster_e2e";

    private static final String clusterNameEdited = "flink_2.0.2_k8s_cluster_e2e_edited";

    private static final String flinkImage = "flink:2.0.2-scala_2.12-java11";

    private static final String k8sNamespace = "default";

    private static long versionId;

    private static long clusterId;

    @BeforeAll
    public static void setup() {
        api.login();
        flink = new FlinkApiSupport(api);
        flink.createFlinkEnv(flinkName, flinkHome, "description test");
        versionId = flink.requireVersionId(flinkName);
    }

    @Test
    @Order(1)
    public void testCreateFlinkCluster() {
        ObjectNode body =
            flink.baseClusterBody(clusterName, FlinkApiConstants.DEPLOY_K8S_SESSION, versionId);
        body.put("resolveOrder", FlinkApiConstants.RESOLVE_CHILD_FIRST);
        body.put("flinkImage", flinkImage);
        body.put("k8sNamespace", k8sNamespace);
        clusterId = flink.createCluster(body);
        assertThat(clusterId).isPositive();
    }

    @Test
    @Order(2)
    public void testEditFlinkCluster() {
        ObjectNode body =
            flink.baseClusterBody(clusterNameEdited, FlinkApiConstants.DEPLOY_K8S_SESSION, versionId);
        body.put("id", clusterId);
        body.put("flinkImage", flinkImage);
        body.put("k8sNamespace", k8sNamespace);
        flink.updateCluster(body);
        assertThat(flink.requireClusterId(clusterNameEdited)).isEqualTo(clusterId);
    }

    @Test
    @Order(3)
    public void testStartFlinkCluster() {
        flink.startCluster(clusterId);
        flink.waitClusterState(
            clusterId, state -> state == FlinkApiConstants.CLUSTER_RUNNING, Duration.ofMinutes(20));
    }

    @Test
    @Order(4)
    public void testStopFlinkCluster() {
        flink.shutdownCluster(clusterId);
        flink.waitClusterState(
            clusterId, state -> state == FlinkApiConstants.CLUSTER_CANCELED, Duration.ofMinutes(20));
    }

    @Test
    @Order(5)
    public void testDeleteFlinkCluster() {
        flink.deleteCluster(clusterId);
        assertThatThrownBy(() -> flink.requireClusterId(clusterNameEdited))
            .isInstanceOf(IllegalStateException.class);
    }
}
