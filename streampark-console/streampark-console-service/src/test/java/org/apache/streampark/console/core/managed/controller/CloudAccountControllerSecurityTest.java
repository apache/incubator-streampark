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

package org.apache.streampark.console.core.managed.controller;

import org.apache.streampark.console.core.annotation.Permission;

import org.apache.shiro.authz.annotation.RequiresPermissions;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CloudAccountControllerSecurityTest {

    @Test
    void shouldRequireExplicitPermissionForEveryEndpoint() {
        Map<String, String> permissions =
            Map.of(
                "page", "cloud-account:view",
                "get", "cloud-account:view",
                "create", "cloud-account:create",
                "update", "cloud-account:update",
                "test", "cloud-account:update",
                "disable", "cloud-account:update",
                "delete", "cloud-account:delete",
                "grant", "cloud-account:grant",
                "grants", "cloud-account:grant",
                "available", "cloud-account:view");

        assertThat(CloudAccountController.class.getDeclaredMethods())
            .filteredOn(method -> permissions.containsKey(method.getName()))
            .hasSize(permissions.size())
            .allSatisfy(
                method -> assertThat(permissionOf(method))
                    .containsExactly(permissions.get(method.getName())));
    }

    @Test
    void shouldRequireTeamMembershipForAvailableAccountLookup() throws Exception {
        Method method =
            CloudAccountController.class.getDeclaredMethod(
                "available",
                org.apache.streampark.console.core.managed.model.CloudAccountAvailableRequest.class);

        assertThat(method.getAnnotation(Permission.class))
            .isNotNull()
            .extracting(Permission::team)
            .isEqualTo("#request.teamId");
    }

    private static String[] permissionOf(Method method) {
        RequiresPermissions annotation = method.getAnnotation(RequiresPermissions.class);
        assertThat(annotation)
            .as("permission annotation for %s", method.getName())
            .isNotNull();
        return annotation.value();
    }
}
