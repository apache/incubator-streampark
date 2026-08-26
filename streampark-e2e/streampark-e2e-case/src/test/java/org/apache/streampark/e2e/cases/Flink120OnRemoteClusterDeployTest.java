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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@StreamParkApi(composeFiles = "docker/flink-1.20-on-remote/docker-compose.yaml")
public class Flink120OnRemoteClusterDeployTest {

    public static ApiClient api;

    private static FlinkApiSupport flink;

    private static final String flinkName = "flink-1.20.1";

    private static final String flinkHome = "/opt/flink/";

    private static final String clusterName = "flink_1.20.1_cluster_e2e";

    private static final String jobManagerUrl = "http://jobmanager:8081";

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
        ObjectNode body = flink.baseClusterBody(clusterName, FlinkApiConstants.DEPLOY_STANDALONE, versionId);
        body.put("address", jobManagerUrl);
        clusterId = flink.createCluster(body);
        assertThat(clusterId).isPositive();
    }

    @Test
    @Order(5)
    public void testDeleteFlinkCluster() {
        flink.deleteCluster(clusterId);
        assertThatThrownBy(() -> flink.requireClusterId(clusterName))
            .isInstanceOf(IllegalStateException.class);
    }
}
