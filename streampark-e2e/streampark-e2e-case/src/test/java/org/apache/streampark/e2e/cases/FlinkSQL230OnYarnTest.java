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

@StreamParkApi(composeFiles = "docker/flink-2.3-on-yarn/docker-compose.yaml")
public class FlinkSQL230OnYarnTest {

    public static ApiClient api;

    private static FlinkApiSupport flink;

    private static final String flinkName = "flink-2.3.0";

    private static final String flinkHome = "/flink-2.3.0";

    private static final String clusterName = "flink_2.3.0_cluster_e2e";

    private static final String yarnAppJobName = "flink-230-e2e-test";

    private static final String yarnSessionJobName = "flink-230-session-e2e";

    private static long versionId;

    private static long clusterId;

    private static long yarnAppId;

    private static long yarnSessionAppId;

    @BeforeAll
    public static void setup() {
        api.login();
        flink = new FlinkApiSupport(api);
        flink.createFlinkEnv(flinkName, flinkHome, "description test");
        versionId = flink.requireVersionId(flinkName);

        ObjectNode clusterBody =
            flink.baseClusterBody(clusterName, FlinkApiConstants.DEPLOY_YARN_SESSION, versionId);
        clusterBody.put("resolveOrder", FlinkApiConstants.RESOLVE_PARENT_FIRST);
        clusterId = flink.createCluster(clusterBody);
        flink.startCluster(clusterId);
        flink.waitClusterState(
            clusterId, state -> state == FlinkApiConstants.CLUSTER_RUNNING, Duration.ofMinutes(15));
    }

    @Test
    @Order(1)
    void testCreateFlinkApplicationOnYarnApplicationMode() {
        ObjectNode body =
            flink.baseSqlAppBody(yarnAppJobName, FlinkApiConstants.DEPLOY_YARN_APPLICATION, versionId);
        yarnAppId = flink.createApp(body);
        assertThat(yarnAppId).isPositive();
    }

    @Test
    @Order(2)
    void testReleaseFlinkApplicationOnYarnApplicationMode() {
        flink.releaseApp(yarnAppId);
    }

    @Test
    @Order(3)
    void testStartFlinkApplicationOnYarnApplicationMode() {
        flink.startApp(yarnAppId);
        flink.waitAppState(
            yarnAppId, state -> state == FlinkApiConstants.APP_FINISHED, Duration.ofMinutes(20));
    }

    @Test
    @Order(4)
    void testCancelFlinkApplicationOnYarnApplicationMode() {
        flink.startApp(yarnAppId);
        flink.waitAppState(
            yarnAppId, state -> state == FlinkApiConstants.APP_RUNNING, Duration.ofMinutes(20));
        flink.cancelApp(yarnAppId);
        flink.waitAppState(
            yarnAppId, state -> state == FlinkApiConstants.APP_CANCELED, Duration.ofMinutes(20));
    }

    @Test
    @Order(5)
    void testDeleteFlinkApplicationOnYarnApplicationMode() {
        flink.deleteApp(yarnAppId);
        assertThatThrownBy(() -> flink.requireAppId(yarnAppJobName))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @Order(6)
    void testCreateFlinkApplicationOnYarnSessionMode() {
        ObjectNode body =
            flink.baseSqlAppBody(yarnSessionJobName, FlinkApiConstants.DEPLOY_YARN_SESSION, versionId);
        body.put("flinkClusterId", clusterId);
        yarnSessionAppId = flink.createApp(body);
        assertThat(yarnSessionAppId).isPositive();
    }

    @Test
    @Order(7)
    void testReleaseFlinkApplicationOnYarnSessionMode() {
        flink.releaseApp(yarnSessionAppId);
    }

    @Test
    @Order(8)
    void testStartFlinkApplicationOnYarnSessionMode() {
        flink.startApp(yarnSessionAppId);
        flink.waitAppState(
            yarnSessionAppId, state -> state == FlinkApiConstants.APP_FINISHED, Duration.ofMinutes(20));
    }

    @Test
    @Order(9)
    void testCancelFlinkApplicationOnYarnSessionMode() {
        flink.startApp(yarnSessionAppId);
        flink.waitAppState(
            yarnSessionAppId, state -> state == FlinkApiConstants.APP_RUNNING, Duration.ofMinutes(20));
        flink.cancelApp(yarnSessionAppId);
        flink.waitAppState(
            yarnSessionAppId, state -> state == FlinkApiConstants.APP_CANCELED, Duration.ofMinutes(20));
    }

    @Test
    @Order(10)
    void testDeleteFlinkApplicationOnYarnSessionMode() {
        flink.deleteApp(yarnSessionAppId);
        assertThatThrownBy(() -> flink.requireAppId(yarnSessionJobName))
            .isInstanceOf(IllegalStateException.class);
    }
}
