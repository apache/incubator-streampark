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
import org.springframework.web.bind.annotation.RequestBody;

import java.lang.reflect.Method;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ManagedFlinkMetadataControllerSecurityTest {

    @Test
    void shouldRequirePermissionAndTeamMembershipForMutatingAndApplicationEndpoints() {
        Map<String, String> permissions =
            Map.ofEntries(
                Map.entry("createEnvironment", "cluster:create"),
                Map.entry("updateEnvironment", "cluster:update"),
                Map.entry("deleteEnvironment", "cluster:delete"),
                Map.entry("probeEnvironment", "cluster:update"),
                Map.entry("getApplication", "app:detail"),
                Map.entry("applicationStatistics", "app:view"),
                Map.entry("createApplication", "app:create"),
                Map.entry("updateApplication", "app:update"),
                Map.entry("releaseApplication", "app:release"),
                Map.entry("startApplication", "app:start"),
                Map.entry("stopApplication", "app:cancel"),
                Map.entry("restartApplication", "app:start"),
                Map.entry("listSnapshots", "app:detail"),
                Map.entry("createSnapshot", "savepoint:trigger"),
                Map.entry("getOperation", "app:detail"),
                Map.entry("listOperations", "app:detail"),
                Map.entry("reconcileOperation", "app:release"));

        assertThat(ManagedFlinkMetadataController.class.getDeclaredMethods())
            .filteredOn(method -> permissions.containsKey(method.getName()))
            .hasSize(permissions.size())
            .allSatisfy(
                method -> {
                    RequiresPermissions requiresPermissions =
                        method.getAnnotation(RequiresPermissions.class);
                    assertThat(requiresPermissions)
                        .as("permission annotation for %s", method.getName())
                        .isNotNull();
                    assertThat(requiresPermissions.value())
                        .containsExactly(permissions.get(method.getName()));
                    assertThat(method.getAnnotation(Permission.class))
                        .as("Team annotation for %s", method.getName())
                        .isNotNull()
                        .extracting(Permission::team)
                        .isEqualTo("#request.teamId");
                });
    }

    @Test
    void shouldAuthorizeReadOnlyEnvironmentMetadataByTeamMembership() {
        assertThat(
            java.util.List.of(
                method("capability"),
                method("projects"),
                method("resourcePools"),
                method("listEnvironments"),
                method("getEnvironment")))
                    .allSatisfy(
                        method -> {
                            assertThat(method.getAnnotation(RequiresPermissions.class))
                                .as("read-only metadata must not require a non-existent cluster:view permission")
                                .isNull();
                            assertThat(method.getAnnotation(Permission.class))
                                .as("Team annotation for %s", method.getName())
                                .isNotNull()
                                .extracting(Permission::team)
                                .isEqualTo("#request.teamId");
                        });
    }

    @Test
    void shouldAcceptNestedApplicationDefinitionsAsJson() {
        assertThat(method("createApplication").getParameters()[0].isAnnotationPresent(
            RequestBody.class)).isTrue();
        assertThat(method("updateApplication").getParameters()[0].isAnnotationPresent(
            RequestBody.class)).isTrue();
    }

    private static Method method(String name) {
        return java.util.Arrays.stream(ManagedFlinkMetadataController.class.getDeclaredMethods())
            .filter(candidate -> candidate.getName().equals(name))
            .findFirst()
            .orElseThrow();
    }
}
