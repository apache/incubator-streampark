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
public class TokenManagementTest {

    public static ApiClient api;

    private static final String existUserName = "admin";
    private static final String newTokenDescription = "test_new_token_description";

    private static String createdToken;
    private static Long tokenId;

    @BeforeAll
    public static void setup() {
        api.login();
    }

    @Test
    @Order(1)
    void testCreateToken() {
        ApiResponse response = api.postForm("/token/create", tokenParams());
        assertThat(response.isSuccess()).isTrue();
        createdToken = response.getData().path("token").asText();
        tokenId = response.getData().path("id").asLong();

        ApiResponse list = api.postForm("/token/list", api.params("pageNum", "1", "pageSize", "50"));
        assertThat(api.pageRecordsContain(list, "username", existUserName)).isTrue();
    }

    @Test
    @Order(2)
    void testCopyToken() {
        ApiResponse list = api.postForm("/token/list", api.params("pageNum", "1", "pageSize", "50"));
        String token =
            api.findInPageRecords(list, "username", existUserName)
                .map(node -> node.path("token").asText())
                .orElseThrow();
        assertThat(token).isEqualTo(createdToken);
    }

    @Test
    @Order(3)
    void testCreateDuplicateToken() {
        ApiResponse response = api.postForm("/token/create", tokenParams());
        assertThat(response.getCode()).isEqualTo(0L);
        assertThat(response.getMessage()).contains(String.format("user %s already has a token", existUserName));
    }

    @Test
    @Order(4)
    void testDeleteToken() {
        ApiResponse response = api.deleteForm("/token/delete", api.params("tokenId", String.valueOf(tokenId)));
        assertThat(response.isSuccess()).isTrue();

        ApiResponse list = api.postForm("/token/list", api.params("pageNum", "1", "pageSize", "50"));
        assertThat(api.pageRecordsContain(list, "username", existUserName)).isFalse();
    }

    private static Map<String, String> tokenParams() {
        Map<String, String> params = new LinkedHashMap<>(api.teamParams());
        params.put("userId", String.valueOf(api.getUserId()));
        params.put("description", newTokenDescription);
        return params;
    }
}
