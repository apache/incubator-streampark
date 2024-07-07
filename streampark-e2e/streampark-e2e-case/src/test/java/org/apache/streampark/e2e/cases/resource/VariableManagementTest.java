/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package org.apache.streampark.e2e.cases.resource;

import org.apache.streampark.e2e.core.StreamPark;
import org.apache.streampark.e2e.pages.LoginPage;
import org.apache.streampark.e2e.pages.resource.ResourcePage;
import org.apache.streampark.e2e.pages.resource.VariableManagementPage;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testcontainers.shaded.org.awaitility.Awaitility;
;import static org.assertj.core.api.Assertions.assertThat;

@StreamPark(composeFiles = "docker/basic/docker-compose.yaml")
public class VariableManagementTest {

    private static RemoteWebDriver browser;

    private static final String userName = "admin";

    private static final String password = "streampark";

    private static final String teamName = "default";

    private static final String variableCode = "10000";

    private static final String variableValue = "3306";

    private static final String description = "MySQL default port";

    private static final boolean isNotVisible = true;

    @BeforeAll
    public static void setup() {
        new LoginPage(browser)
            .login(userName, password, teamName)
            .goToNav(ResourcePage.class)
            .goToTab(VariableManagementPage.class);
    }

    @Test
    @Order(10)
    void testCreateVariable() {
        final VariableManagementPage variableManagementPage = new VariableManagementPage(browser);
        variableManagementPage.createVariable(variableCode, variableValue, description, isNotVisible);

        Awaitility.await()
            .untilAsserted(
                () -> assertThat(variableManagementPage.variableList())
                    .as("Variable list should contain newly-created variable")
                    .extracting(WebElement::getText)
                    .anyMatch(it -> it.contains(variableCode)));
    }

    @Test
    @Order(20)
    void testCreateDuplicateVariable() {
        final VariableManagementPage variableManagementPage = new VariableManagementPage(browser);
        variableManagementPage.createVariable(variableCode, variableValue, description, isNotVisible);

        Awaitility.await()
            .untilAsserted(
                () -> assertThat(variableManagementPage.errorMessageList())
                    .as("Variable Code Duplicated Error message should be displayed")
                    .extracting(WebElement::getText)
                    .anyMatch(it -> it.contains("The variable code already exists.")));

        variableManagementPage.errorMessageConfirmButton().click();
        variableManagementPage.createVariableForm().buttonCancel().click();
    }

    @Test
    @Order(30)
    void testEditVariable() {
        final VariableManagementPage variableManagementPage = new VariableManagementPage(browser);
        String editVariableValue = "6379";
        String editDescription = "Redis default port";

        variableManagementPage.editVariable(variableCode, editVariableValue, editDescription, isNotVisible);
        Awaitility.await()
            .untilAsserted(
                () -> assertThat(variableManagementPage.variableList())
                    .as("Variable list should contain edited variable")
                    .extracting(WebElement::getText)
                    .anyMatch(it -> it.contains(editVariableValue))
                    .anyMatch(it -> it.contains(editDescription)));
    }

    @Test
    @Order(40)
    void testDeleteTeam() {
        final VariableManagementPage variableManagementPage = new VariableManagementPage(browser);

        variableManagementPage.deleteVariable(variableCode);

        Awaitility.await()
            .untilAsserted(
                () -> assertThat(variableManagementPage.variableList())
                    .extracting(WebElement::getText)
                    .noneMatch(it -> it.contains(variableCode))
            );
    }
}
