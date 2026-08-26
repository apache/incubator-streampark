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
import org.apache.streampark.e2e.core.api.ApiResponse;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@StreamParkApi(composeFiles = "docker/basic/docker-compose.yaml")
public class ProjectsManagementTest {

    private static final Duration PROJECT_BUILD_TIMEOUT = Duration.ofMinutes(15);

    public static ApiClient api;

    private static final String projectName = "e2e_test_project";
    private static final String editedProjectName = "e2e_test_project_edited";
    private static final String url = "https://github.com/apache/streampark-quickstart";
    private static final String branch = "dev";
    private static final String buildArgument =
        "-pl quickstart-flink/quickstart-apacheflink/apacheflinksql_1.17 -am -Dmaven.test.skip=true";
    private static final String description = "e2e test project description";

    private static Long projectId;

    @BeforeAll
    public static void setup() {
        api.login();
    }

    @Test
    @Order(1)
    void testCreateProject() {
        ApiResponse response = api.postForm("/project/create", projectParams(projectName));
        assertThat(response.isSuccess()).isTrue();

        ApiResponse list = projectList();
        assertThat(api.pageRecordsContain(list, "name", projectName)).isTrue();
    }

    @Test
    @Order(2)
    void testEditProject() {
        ApiResponse list = projectList();
        projectId =
            api.findInPageRecords(list, "name", projectName)
                .map(node -> node.path("id").asLong())
                .orElseThrow();

        Map<String, String> params = projectParams(editedProjectName);
        params.put("id", String.valueOf(projectId));

        ApiResponse response = api.postForm("/project/update", params);
        assertThat(response.isSuccess()).isTrue();

        list = projectList();
        assertThat(api.pageRecordsContain(list, "name", editedProjectName)).isTrue();
    }

    @Test
    @Order(3)
    void testBuildProject() {
        Map<String, String> params = new LinkedHashMap<>(api.teamParams());
        params.put("id", String.valueOf(projectId));

        ApiResponse response = api.postForm("/project/build", params);
        assertThat(response.isSuccess()).isTrue();

        Awaitility.await()
            .atMost(PROJECT_BUILD_TIMEOUT)
            .untilAsserted(
                () -> {
                    ApiResponse list = projectList();
                    JsonNode record =
                        api.findInPageRecords(list, "name", editedProjectName).orElseThrow();
                    assertThat(record.path("buildState").asInt()).isEqualTo(1);
                });
    }

    @Test
    @Order(4)
    void testDeleteProject() {
        Map<String, String> params = new LinkedHashMap<>(api.teamParams());
        params.put("id", String.valueOf(projectId));

        ApiResponse response = api.postForm("/project/delete", params);
        assertThat(response.isSuccess()).isTrue();

        ApiResponse list = projectList();
        assertThat(api.pageRecordsContain(list, "name", editedProjectName)).isFalse();
    }

    private static ApiResponse projectList() {
        Map<String, String> params = new LinkedHashMap<>(api.teamParams());
        params.put("pageNum", "1");
        params.put("pageSize", "50");
        return api.postForm("/project/list", params);
    }

    private static Map<String, String> projectParams(String name) {
        Map<String, String> params = new LinkedHashMap<>(api.teamParams());
        params.put("name", name);
        params.put("url", url);
        params.put("refs", branch);
        params.put("repository", "1");
        params.put("type", "1");
        params.put("buildArgs", buildArgument);
        params.put("description", description);
        return params;
    }
}
