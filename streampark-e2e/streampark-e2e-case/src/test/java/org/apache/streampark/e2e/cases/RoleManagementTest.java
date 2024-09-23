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

import org.apache.streampark.e2e.core.StreamPark;
import org.apache.streampark.e2e.pages.LoginPage;
import org.apache.streampark.e2e.pages.system.RoleManagementPage;
import org.apache.streampark.e2e.pages.system.SystemPage;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import static org.assertj.core.api.Assertions.assertThat;

@StreamPark(composeFiles = "docker/basic/docker-compose.yaml")
public class RoleManagementTest {

    public static RemoteWebDriver browser;

    private static final String newRoleName = "new_role";

    private static final String newDescription = "new_description";

    private static final String existMenuName = "Apache Flink";

    @BeforeAll
    public static void setup() {
        new LoginPage(browser)
            .login()
            .goToNav(SystemPage.class)
            .goToTab(RoleManagementPage.class);
    }

    @Test
    @Order(1)
    void testCreateRole() {
        final RoleManagementPage roleManagementPage = new RoleManagementPage(browser);
        roleManagementPage.createRole(newRoleName, newDescription, existMenuName);

        Awaitility.await()
            .untilAsserted(
                () -> assertThat(roleManagementPage.roleList)
                    .as("Role list should contain newly-created role")
                    .extracting(WebElement::getText)
                    .anyMatch(it -> it.contains(newRoleName)));
    }

    @Test
    @Order(2)
    void testCreateDuplicateRole() {
        final RoleManagementPage roleManagementPage = new RoleManagementPage(browser);
        roleManagementPage.createRole(newRoleName, newDescription, existMenuName);

        Awaitility.await()
            .untilAsserted(
                () -> assertThat(roleManagementPage.errorMessageList)
                    .as("Role Name Duplicated Error message should be displayed")
                    .extracting(WebElement::getText)
                    .anyMatch(it -> it.contains(
                        "Sorry, the role name already exists")));

        roleManagementPage.createRoleForm.buttonCancel.click();
    }
    @Test
    @Order(3)
    void testEditRole() {
        final RoleManagementPage roleManagementPage = new RoleManagementPage(browser);

        String newEditDescription = newDescription + "_edit";
        String newEditMenuName = "System";
        roleManagementPage.editRole(newRoleName, newEditDescription, newEditMenuName);

        Awaitility.await()
            .untilAsserted(
                () -> assertThat(roleManagementPage.roleList)
                    .as("Role list should contain edited role")
                    .extracting(WebElement::getText)
                    .anyMatch(it -> it.contains(newEditDescription)));
    }

    @Test
    @Order(4)
    void testDeleteRole() {
        final RoleManagementPage roleManagementPage = new RoleManagementPage(browser);

        roleManagementPage.deleteRole(newRoleName);

        Awaitility.await()
            .untilAsserted(
                () -> assertThat(roleManagementPage.roleList)
                    .extracting(WebElement::getText)
                    .noneMatch(it -> it.contains(newRoleName)));
    }
}
