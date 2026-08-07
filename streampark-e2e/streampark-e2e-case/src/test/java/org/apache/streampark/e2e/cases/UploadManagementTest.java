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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@StreamParkApi(composeFiles = "docker/basic/docker-compose.yaml")
public class UploadManagementTest {

    public static ApiClient api;

    private static final String resourceName = "test-resource";

    private static final String mavenPom =
        "<dependency>\n"
            + "    <groupId>junit</groupId>\n"
            + "    <artifactId>junit</artifactId>\n"
            + "    <version>4.13.2</version>\n"
            + "    <scope>test</scope>\n"
            + "</dependency>";

    private static final String description = "Junit-jar-lib";

    @BeforeAll
    public static void setup() {
        api.login();
    }

    @Test
    @Order(1)
    void testCreateUpload() {
        ApiResponse response = api.postForm("/resource/add", resourceParams(resourceName, mavenPom, description));
        assertThat(response.isSuccess()).isTrue();

        ApiResponse list = resourceList();
        assertThat(api.pageRecordsContain(list, "resourceName", resourceName)).isTrue();
        assertThat(api.pageRecordsContain(list, "description", description)).isTrue();
    }

    @Test
    @Order(2)
    void testCreateDuplicateUpload() {
        ApiResponse response = api.postForm("/resource/add", resourceParams(resourceName, mavenPom, description));
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage())
            .contains(String.format("the resource %s already exists, please check.", resourceName));
    }

    @Test
    @Order(3)
    void testEditUpload() {
        String editDescription = "Kafka-jar-lib";
        String editResource =
            "<dependency>\n"
                + "   <groupId>org.apache.kafka</groupId>\n"
                + "   <artifactId>kafka-clients</artifactId>\n"
                + "   <version>3.7.1</version>\n"
                + "</dependency>";

        ApiResponse list = resourceList();
        Long id =
            api.findInPageRecords(list, "resourceName", resourceName)
                .map(node -> node.path("id").asLong())
                .orElseThrow();

        Map<String, String> params = resourceParams(resourceName, editResource, editDescription);
        params.put("id", String.valueOf(id));

        ApiResponse response = api.putForm("/resource/update", params);
        assertThat(response.isSuccess()).isTrue();

        list = resourceList();
        assertThat(api.pageRecordsContain(list, "resourceName", resourceName)).isTrue();
        assertThat(api.pageRecordsContain(list, "description", editDescription)).isTrue();
    }

    @Test
    @Order(4)
    void testDeleteUpload() {
        ApiResponse list = resourceList();
        Long id =
            api.findInPageRecords(list, "resourceName", resourceName)
                .map(node -> node.path("id").asLong())
                .orElseThrow();

        Map<String, String> params = new LinkedHashMap<>(api.teamParams());
        params.put("id", String.valueOf(id));

        ApiResponse response = api.deleteForm("/resource/delete", params);
        assertThat(response.isSuccess()).isTrue();

        list = resourceList();
        assertThat(api.pageRecordsContain(list, "resourceName", resourceName)).isFalse();
    }

    private static ApiResponse resourceList() {
        Map<String, String> params = new LinkedHashMap<>(api.teamParams());
        params.put("pageNum", "1");
        params.put("pageSize", "50");
        return api.postForm("/resource/page", params);
    }

    private static Map<String, String> resourceParams(String name, String resource, String resourceDescription) {
        Map<String, String> params = new LinkedHashMap<>(api.teamParams());
        params.put("engineType", "FLINK");
        params.put("resourceType", "JAR_LIBRARY");
        params.put("resourceName", name);
        params.put("resource", resource);
        params.put("description", resourceDescription);
        return params;
    }
}
