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
public class MemberManagementTest {

    public static ApiClient api;

    private static final String existUserName = "test3";
    private static final String existRole = "developer";

    @BeforeAll
    public static void setup() {
        api.login();
    }

    @Test
    @Order(1)
    void testCreateMember() {
        ApiResponse response = api.postForm("/member/post", memberParams(existUserName, existRole));
        assertThat(response.isSuccess()).isTrue();

        ApiResponse list = memberList();
        assertThat(api.pageRecordsContain(list, "userName", existUserName)).isTrue();
    }

    @Test
    @Order(2)
    void testCreateDuplicateMember() {
        ApiResponse response = api.postForm("/member/post", memberParams(existUserName, existRole));
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).contains("please don't add it again.");
    }

    @Test
    @Order(3)
    void testEditMember() {
        String anotherRole = "team admin";
        ApiResponse list = memberList();
        Long memberId =
            api.findInPageRecords(list, "userName", existUserName)
                .map(node -> node.path("id").asLong())
                .orElseThrow();

        Map<String, String> params = memberParams(existUserName, anotherRole);
        params.put("id", String.valueOf(memberId));

        ApiResponse response = api.putForm("/member/update", params);
        assertThat(response.isSuccess()).isTrue();

        list = memberList();
        assertThat(api.pageRecordsContain(list, "userName", existUserName)).isTrue();
    }

    @Test
    @Order(4)
    void testDeleteMember() {
        ApiResponse list = memberList();
        Long memberId =
            api.findInPageRecords(list, "userName", existUserName)
                .map(node -> node.path("id").asLong())
                .orElseThrow();

        Map<String, String> params = new LinkedHashMap<>(api.teamParams());
        params.put("id", String.valueOf(memberId));

        ApiResponse response = api.deleteForm("/member/delete", params);
        assertThat(response.isSuccess()).isTrue();

        list = memberList();
        assertThat(api.pageRecordsContain(list, "userName", existUserName)).isFalse();
    }

    private static ApiResponse memberList() {
        Map<String, String> params = new LinkedHashMap<>(api.teamParams());
        params.put("pageNum", "1");
        params.put("pageSize", "100");
        return api.postForm("/member/list", params);
    }

    private static Map<String, String> memberParams(String userName, String roleName) {
        Long roleId = api.findRoleIdByName(roleName).orElseThrow();
        Map<String, String> params = new LinkedHashMap<>(api.teamParams());
        params.put("userName", userName);
        params.put("roleId", String.valueOf(roleId));
        return params;
    }
}
