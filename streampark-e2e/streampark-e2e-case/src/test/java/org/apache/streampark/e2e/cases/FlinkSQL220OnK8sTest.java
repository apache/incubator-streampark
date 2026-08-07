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

@StreamParkApi(composeFiles = "docker/flink-2.2-on-k8s/docker-compose.yaml")
public class FlinkSQL220OnK8sTest {

    public static ApiClient api;

    private static FlinkApiSupport flink;

    private static final String flinkName = "flink-2.2.1";

    private static final String flinkHome = "/flink-2.2.1";

    private static final String clusterName = "flink_2.2.1_k8s_cluster_e2e";

    private static final String k8sAppJobName = "flink220e2etest";

    private static final String k8sSessionJobName = "flink220session";

    private static final String flinkImage = "flink:2.2.1-scala_2.12-java11";

    private static final String k8sNamespace = "default";

    private static long versionId;

    private static long clusterId;

    private static long k8sAppId;

    private static long k8sSessionAppId;

    @BeforeAll
    public static void setup() {
        api.login();
        flink = new FlinkApiSupport(api);
        flink.createFlinkEnv(flinkName, flinkHome, "description test");
        versionId = flink.requireVersionId(flinkName);

        ObjectNode clusterBody =
            flink.baseClusterBody(clusterName, FlinkApiConstants.DEPLOY_K8S_SESSION, versionId);
        clusterBody.put("resolveOrder", FlinkApiConstants.RESOLVE_PARENT_FIRST);
        clusterBody.put("flinkImage", flinkImage);
        clusterBody.put("k8sNamespace", k8sNamespace);
        clusterId = flink.createCluster(clusterBody);
        flink.startCluster(clusterId);
        flink.waitClusterState(
            clusterId, state -> state == FlinkApiConstants.CLUSTER_RUNNING, Duration.ofMinutes(20));
    }

    @Test
    @Order(1)
    void testCreateFlinkApplicationOnKubernetesApplicationMode() {
        ObjectNode body =
            flink.baseSqlAppBody(k8sAppJobName, FlinkApiConstants.DEPLOY_K8S_APPLICATION, versionId);
        body.put("k8sNamespace", k8sNamespace);
        body.put("flinkImage", flinkImage);
        k8sAppId = flink.createApp(body);
        assertThat(k8sAppId).isPositive();
    }

    @Test
    @Order(2)
    void testReleaseFlinkApplicationOnKubernetesApplicationMode() {
        flink.releaseApp(k8sAppId);
    }

    @Test
    @Order(3)
    void testStartFlinkApplicationOnKubernetesApplicationMode() {
        flink.startApp(k8sAppId);
        flink.waitAppState(
            k8sAppId, state -> state == FlinkApiConstants.APP_FINISHED, Duration.ofMinutes(25));
    }

    @Test
    @Order(4)
    void testCancelFlinkApplicationOnKubernetesApplicationMode() {
        flink.startApp(k8sAppId);
        flink.waitAppState(
            k8sAppId, state -> state == FlinkApiConstants.APP_RUNNING, Duration.ofMinutes(25));
        flink.cancelApp(k8sAppId);
        flink.waitAppState(
            k8sAppId, state -> state == FlinkApiConstants.APP_CANCELED, Duration.ofMinutes(25));
    }

    @Test
    @Order(5)
    void testDeleteFlinkApplicationOnKubernetesApplicationMode() {
        flink.deleteApp(k8sAppId);
        assertThatThrownBy(() -> flink.requireAppId(k8sAppJobName))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @Order(6)
    void testCreateFlinkApplicationOnKubernetesSessionMode() {
        ObjectNode body =
            flink.baseSqlAppBody(k8sSessionJobName, FlinkApiConstants.DEPLOY_K8S_SESSION, versionId);
        body.put("flinkClusterId", clusterId);
        k8sSessionAppId = flink.createApp(body);
        assertThat(k8sSessionAppId).isPositive();
    }

    @Test
    @Order(7)
    void testReleaseFlinkApplicationOnKubernetesSessionMode() {
        flink.releaseApp(k8sSessionAppId);
    }

    @Test
    @Order(8)
    void testStartFlinkApplicationOnKubernetesSessionMode() {
        flink.startApp(k8sSessionAppId);
        flink.waitAppState(
            k8sSessionAppId, state -> state == FlinkApiConstants.APP_FINISHED, Duration.ofMinutes(25));
    }

    @Test
    @Order(9)
    void testCancelFlinkApplicationOnKubernetesSessionMode() {
        flink.startApp(k8sSessionAppId);
        flink.waitAppState(
            k8sSessionAppId, state -> state == FlinkApiConstants.APP_RUNNING, Duration.ofMinutes(25));
        flink.cancelApp(k8sSessionAppId);
        flink.waitAppState(
            k8sSessionAppId, state -> state == FlinkApiConstants.APP_CANCELED, Duration.ofMinutes(25));
    }

    @Test
    @Order(10)
    void testDeleteFlinkApplicationOnKubernetesSessionMode() {
        flink.deleteApp(k8sSessionAppId);
        assertThatThrownBy(() -> flink.requireAppId(k8sSessionJobName))
            .isInstanceOf(IllegalStateException.class);
    }
}
