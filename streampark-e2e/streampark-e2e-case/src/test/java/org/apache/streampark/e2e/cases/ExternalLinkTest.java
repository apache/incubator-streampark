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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@StreamParkApi(composeFiles = "docker/basic/docker-compose.yaml")
public class ExternalLinkTest {

    public static ApiClient api;

    private static final String newLabel = "new_label";
    private static final String editLabel = "edit_label";
    private static final String newName = "new_name";
    private static final String color = "#b54f4f";
    private static final String newLink = "https://grafana/flink-monitoring?var-JobId=var-JobId=1";

    @BeforeAll
    public static void setup() {
        api.login();
    }

    @Test
    @Order(1)
    void testCreateExternalLink() {
        ApiResponse response = api.postForm("/flink/externalLink/create", linkParams(newLabel, newName, newLink));
        assertThat(response.isSuccess()).isTrue();

        ApiResponse list = api.postForm("/flink/externalLink/list", new LinkedHashMap<>());
        assertThat(listContainsLink(list, newLabel, newName, newLink)).isTrue();
    }

    @Test
    @Order(2)
    void testCreateDuplicateExternalLink() {
        ApiResponse response = api.postForm("/flink/externalLink/create", linkParams(newLabel, newName, newLink));
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).contains(String.format("The name: %s is already existing.", newName));
    }

    @Test
    @Order(3)
    void testEditExternalLink() {
        String editName = "edit_name";
        String editLink = "https://grafana/flink-monitoring?var-Job=var-Job=edit";
        Long id = findLinkId(newLabel).orElseThrow();

        Map<String, String> params = linkParams(editLabel, editName, editLink);
        params.put("id", String.valueOf(id));

        ApiResponse response = api.postForm("/flink/externalLink/update", params);
        assertThat(response.isSuccess()).isTrue();

        ApiResponse list = api.postForm("/flink/externalLink/list", new LinkedHashMap<>());
        assertThat(listContainsLink(list, editLabel, editName, editLink)).isTrue();
    }

    @Test
    @Order(4)
    void testDeleteExternalLink() {
        Long id = findLinkId(editLabel).orElseThrow();
        ApiResponse response = api.deleteForm("/flink/externalLink/delete", api.params("id", String.valueOf(id)));
        assertThat(response.isSuccess()).isTrue();

        ApiResponse list = api.postForm("/flink/externalLink/list", new LinkedHashMap<>());
        assertThat(findLinkId(editLabel)).isEmpty();
        assertThat(list.getData().toString()).doesNotContain(editLabel);
    }

    private static Map<String, String> linkParams(String label, String name, String linkUrl) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("badgeLabel", label);
        params.put("badgeName", name);
        params.put("badgeColor", color);
        params.put("linkUrl", linkUrl);
        return params;
    }

    private static Optional<Long> findLinkId(String label) {
        ApiResponse list = api.postForm("/flink/externalLink/list", new LinkedHashMap<>());
        if (list.getData() == null || !list.getData().isArray()) {
            return Optional.empty();
        }
        for (JsonNode item : list.getData()) {
            if (label.equals(item.path("badgeLabel").asText())) {
                return Optional.of(item.path("id").asLong());
            }
        }
        return Optional.empty();
    }

    private static boolean listContainsLink(ApiResponse list, String label, String name, String linkUrl) {
        if (list.getData() == null || !list.getData().isArray()) {
            return false;
        }
        for (JsonNode item : list.getData()) {
            if (label.equals(item.path("badgeLabel").asText())
                && name.equals(item.path("badgeName").asText())
                && linkUrl.equals(item.path("linkUrl").asText())) {
                return true;
            }
        }
        return false;
    }
}
