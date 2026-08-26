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
public class RoleManagementTest {

    public static ApiClient api;

    private static final String newRoleName = "new_role";
    private static final String newDescription = "new_description";
    private static final String existMenuName = "Apache Flink";

    @BeforeAll
    public static void setup() {
        api.login();
    }

    @Test
    @Order(1)
    void testCreateRole() {
        ApiResponse response = api.postForm("/role/post", roleParams(newRoleName, newDescription, existMenuName));
        assertThat(response.isSuccess()).isTrue();

        ApiResponse list = api.postForm("/role/list", api.params("pageNum", "1", "pageSize", "100"));
        assertThat(api.pageRecordsContain(list, "roleName", newRoleName)).isTrue();
    }

    @Test
    @Order(2)
    void testCreateDuplicateRole() {
        ApiResponse response = api.postForm("/role/post", roleParams(newRoleName, newDescription, existMenuName));
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).contains("Sorry, the role name already exists");
    }

    @Test
    @Order(3)
    void testEditRole() {
        String newEditDescription = newDescription + "_edit";
        Long roleId = api.findRoleIdByName(newRoleName).orElseThrow();

        Map<String, String> params = roleParams(newRoleName, newEditDescription, "System");
        params.put("roleId", String.valueOf(roleId));

        ApiResponse response = api.putForm("/role/update", params);
        assertThat(response.isSuccess()).isTrue();

        ApiResponse list = api.postForm("/role/list", api.params("pageNum", "1", "pageSize", "100"));
        assertThat(api.pageRecordsContain(list, "description", newEditDescription)).isTrue();
    }

    @Test
    @Order(4)
    void testDeleteRole() {
        Long roleId = api.findRoleIdByName(newRoleName).orElseThrow();
        ApiResponse response = api.deleteForm("/role/delete", api.params("roleId", String.valueOf(roleId)));
        assertThat(response.isSuccess()).isTrue();

        ApiResponse list = api.postForm("/role/list", api.params("pageNum", "1", "pageSize", "100"));
        assertThat(api.pageRecordsContain(list, "roleName", newRoleName)).isFalse();
    }

    private static Map<String, String> roleParams(String roleName, String description, String menuTitle) {
        String menuId = api.findMenuIdByTitle(menuTitle).orElseThrow();
        Map<String, String> params = new LinkedHashMap<>();
        params.put("roleName", roleName);
        params.put("description", description);
        params.put("menuId", menuId);
        return params;
    }
}
