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
public class VariableManagementTest {

    public static ApiClient api;

    private static final String variableCode = "10000";
    private static final String variableValue = "3306";
    private static final String description = "MySQL default port";

    @BeforeAll
    public static void setup() {
        api.login();
    }

    @Test
    @Order(1)
    void testCreateVariable() {
        ApiResponse response = api.postForm("/variable/post", variableParams(variableCode, variableValue, description));
        assertThat(response.isSuccess()).isTrue();

        ApiResponse list = variableList();
        assertThat(api.pageRecordsContain(list, "variableCode", variableCode)).isTrue();
    }

    @Test
    @Order(2)
    void testCreateDuplicateVariable() {
        ApiResponse response = api.postForm("/variable/post", variableParams(variableCode, variableValue, description));
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).contains("The variable code already exists.");
    }

    @Test
    @Order(3)
    void testEditVariable() {
        String editVariableValue = "6379";
        String editDescription = "Redis default port";

        ApiResponse list = variableList();
        Long id =
            api.findInPageRecords(list, "variableCode", variableCode)
                .map(node -> node.path("id").asLong())
                .orElseThrow();

        Map<String, String> params = variableParams(variableCode, editVariableValue, editDescription);
        params.put("id", String.valueOf(id));

        ApiResponse response = api.putForm("/variable/update", params);
        assertThat(response.isSuccess()).isTrue();

        list = variableList();
        assertThat(api.pageRecordsContain(list, "variableValue", editVariableValue)).isTrue();
        assertThat(api.pageRecordsContain(list, "description", editDescription)).isTrue();
    }

    @Test
    @Order(4)
    void testDeleteVariable() {
        ApiResponse list = variableList();
        Long id =
            api.findInPageRecords(list, "variableCode", variableCode)
                .map(node -> node.path("id").asLong())
                .orElseThrow();

        Map<String, String> params = new LinkedHashMap<>(api.teamParams());
        params.put("id", String.valueOf(id));

        ApiResponse response = api.deleteForm("/variable/delete", params);
        assertThat(response.isSuccess()).isTrue();

        list = variableList();
        assertThat(api.pageRecordsContain(list, "variableCode", variableCode)).isFalse();
    }

    private static ApiResponse variableList() {
        Map<String, String> params = new LinkedHashMap<>(api.teamParams());
        params.put("pageNum", "1");
        params.put("pageSize", "50");
        return api.postForm("/variable/page", params);
    }

    private static Map<String, String> variableParams(String code, String value, String variableDescription) {
        Map<String, String> params = new LinkedHashMap<>(api.teamParams());
        params.put("variableCode", code);
        params.put("variableValue", value);
        params.put("description", variableDescription);
        params.put("desensitization", "true");
        return params;
    }
}
