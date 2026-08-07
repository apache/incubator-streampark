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
public class YarnQueueTest {

    public static ApiClient api;

    private static final String newQueueLabel = "new_label";
    private static final String editQueueLabel = "edit_label";
    private static final String description = "test_description";

    @BeforeAll
    public static void setup() {
        api.login();
    }

    @Test
    @Order(1)
    void testYarnQueue() {
        Map<String, String> params = queueParams(newQueueLabel, description);
        ApiResponse response = api.postForm("/yarn/queue/create", params);
        assertThat(response.isSuccess()).isTrue();

        ApiResponse list = api.postForm("/yarn/queue/list", api.params("pageNum", "1", "pageSize", "50"));
        assertThat(api.pageRecordsContain(list, "queueLabel", newQueueLabel)).isTrue();
        assertThat(api.pageRecordsContain(list, "description", description)).isTrue();
    }

    @Test
    @Order(2)
    void testCreateDuplicateYarnQueue() {
        Map<String, String> params = queueParams(newQueueLabel, description);
        ApiResponse response = api.postForm("/yarn/queue/create", params);
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).contains("The queue label existed in the current team");
    }

    @Test
    @Order(3)
    void testEditYarnQueue() {
        String editDescription = "edit_" + description;
        Map<String, String> params = queueParams(editQueueLabel, editDescription);
        params.put("id", api.findInPageRecords(
            api.postForm("/yarn/queue/list", api.params("pageNum", "1", "pageSize", "50")),
            "queueLabel",
            newQueueLabel)
            .map(node -> String.valueOf(node.path("id").asLong()))
            .orElseThrow());

        ApiResponse response = api.postForm("/yarn/queue/update", params);
        assertThat(response.isSuccess()).isTrue();

        ApiResponse list = api.postForm("/yarn/queue/list", api.params("pageNum", "1", "pageSize", "50"));
        assertThat(api.pageRecordsContain(list, "queueLabel", editQueueLabel)).isTrue();
        assertThat(api.pageRecordsContain(list, "description", editDescription)).isTrue();
    }

    @Test
    @Order(4)
    void testDeleteYarnQueue() {
        ApiResponse list = api.postForm("/yarn/queue/list", api.params("pageNum", "1", "pageSize", "50"));
        String id =
            api.findInPageRecords(list, "queueLabel", editQueueLabel)
                .map(node -> String.valueOf(node.path("id").asLong()))
                .orElseThrow();

        ApiResponse response = api.postForm("/yarn/queue/delete", api.params("id", id));
        assertThat(response.isSuccess()).isTrue();

        list = api.postForm("/yarn/queue/list", api.params("pageNum", "1", "pageSize", "50"));
        assertThat(api.pageRecordsContain(list, "queueLabel", editQueueLabel)).isFalse();
    }

    private static Map<String, String> queueParams(String queueLabel, String queueDescription) {
        Map<String, String> params = new LinkedHashMap<>(api.teamParams());
        params.put("queueLabel", queueLabel);
        params.put("description", queueDescription);
        return params;
    }
}
