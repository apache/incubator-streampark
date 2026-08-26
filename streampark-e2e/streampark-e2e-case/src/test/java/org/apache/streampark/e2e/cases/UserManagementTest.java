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
public class UserManagementTest {

    public static ApiClient api;

    private static final String password = "streampark";
    private static final String newUserName = "test_new";
    private static final String newUserEmail = "test@email.com";

    @BeforeAll
    public static void setup() {
        api.login();
    }

    @Test
    @Order(1)
    void testCreateUser() {
        ApiResponse response = api.postForm("/user/post", createUserParams(newUserName, newUserEmail));
        assertThat(response.isSuccess()).isTrue();

        ApiResponse list = api.postForm("/user/list", api.params("pageNum", "1", "pageSize", "100"));
        assertThat(api.pageRecordsContain(list, "username", newUserName)).isTrue();
    }

    @Test
    @Order(2)
    void testCreateDuplicateUser() {
        ApiResponse response = api.postForm("/user/post", createUserParams(newUserName, newUserEmail));
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).contains("Sorry the username already exists");
    }

    @Test
    @Order(3)
    void testEditUser() {
        String editEmail = "edit_" + newUserEmail;
        ApiResponse list = api.postForm("/user/list", api.params("pageNum", "1", "pageSize", "100"));
        Long userId =
            api.findInPageRecords(list, "username", newUserName)
                .map(node -> node.path("userId").asLong())
                .orElseThrow();

        Map<String, String> params = new LinkedHashMap<>();
        params.put("userId", String.valueOf(userId));
        params.put("username", newUserName);
        params.put("email", editEmail);
        params.put("userType", "ADMIN");
        params.put("status", "LOCKED");
        params.put("sex", "1");
        params.put("nickName", "test");

        ApiResponse response = api.putForm("/user/update", params);
        assertThat(response.isSuccess()).isTrue();

        list = api.postForm("/user/list", api.params("pageNum", "1", "pageSize", "100"));
        assertThat(
            api.findInPageRecords(list, "username", newUserName)
                .map(node -> node.path("status").asText())
                .orElse(""))
                    .isEqualTo("LOCKED");
    }

    private static Map<String, String> createUserParams(String username, String email) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("username", username);
        params.put("nickName", "test");
        params.put("password", password);
        params.put("email", email);
        params.put("userType", "ADMIN");
        params.put("status", "VALID");
        params.put("sex", "1");
        return params;
    }
}
