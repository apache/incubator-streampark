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
public class TeamManagementTest {

    public static ApiClient api;

    private static final String newTeamName = "test_new_team";
    private static final String newTeamDescription = "test_new_team_description";

    @BeforeAll
    public static void setup() {
        api.login();
    }

    @Test
    @Order(1)
    void testCreateTeam() {
        ApiResponse response = api.postForm("/team/post", teamParams(newTeamName, newTeamDescription));
        assertThat(response.isSuccess()).isTrue();

        ApiResponse list = api.postForm("/team/list", api.params("pageNum", "1", "pageSize", "100"));
        assertThat(api.pageRecordsContain(list, "teamName", newTeamName)).isTrue();
    }

    @Test
    @Order(2)
    void testCreateDuplicateTeam() {
        ApiResponse response = api.postForm("/team/post", teamParams(newTeamName, newTeamDescription));
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).contains("Create team failed.");
    }

    @Test
    @Order(3)
    void testEditTeam() {
        String editDescription = "edit_" + newTeamDescription;
        ApiResponse list = api.postForm("/team/list", api.params("pageNum", "1", "pageSize", "100"));
        Long teamId =
            api.findInPageRecords(list, "teamName", newTeamName)
                .map(node -> node.path("id").asLong())
                .orElseThrow();

        Map<String, String> params = teamParams(newTeamName, editDescription);
        params.put("id", String.valueOf(teamId));

        ApiResponse response = api.putForm("/team/update", params);
        assertThat(response.isSuccess()).isTrue();

        list = api.postForm("/team/list", api.params("pageNum", "1", "pageSize", "100"));
        assertThat(api.pageRecordsContain(list, "description", editDescription)).isTrue();
    }

    @Test
    @Order(4)
    void testDeleteTeam() {
        ApiResponse list = api.postForm("/team/list", api.params("pageNum", "1", "pageSize", "100"));
        Long teamId =
            api.findInPageRecords(list, "teamName", newTeamName)
                .map(node -> node.path("id").asLong())
                .orElseThrow();

        ApiResponse response = api.deleteForm("/team/delete", api.params("id", String.valueOf(teamId)));
        assertThat(response.isSuccess()).isTrue();

        list = api.postForm("/team/list", api.params("pageNum", "1", "pageSize", "100"));
        assertThat(api.pageRecordsContain(list, "teamName", newTeamName)).isFalse();
    }

    private static Map<String, String> teamParams(String teamName, String description) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("teamName", teamName);
        params.put("description", description);
        return params;
    }
}
