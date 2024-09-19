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
import org.apache.streampark.e2e.pages.system.SystemPage;
import org.apache.streampark.e2e.pages.system.TeamManagementPage;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import static org.assertj.core.api.Assertions.assertThat;

@StreamPark(composeFiles = "docker/basic/docker-compose.yaml")
public class TeamManagementTest {

    public static RemoteWebDriver browser;

    private static final String newTeamName = "test_new_team";

    private static final String newTeamDescription = "test_new_team_description";

    @BeforeAll
    public static void setup() {
        new LoginPage(browser)
            .login()
            .goToNav(SystemPage.class)
            .goToTab(TeamManagementPage.class);
    }

    @Test
    @Order(1)
    void testCreateTeam() {
        final TeamManagementPage teamManagementPage = new TeamManagementPage(browser);
        teamManagementPage.createTeam(newTeamName, newTeamDescription);

        Awaitility.await()
            .untilAsserted(
                () -> assertThat(teamManagementPage.teamList)
                    .as("Team list should contain newly-created team")
                    .extracting(WebElement::getText)
                    .anyMatch(it -> it.contains(newTeamName)));
    }

    @Test
    @Order(2)
    void testCreateDuplicateTeam() {
        final TeamManagementPage teamManagementPage = new TeamManagementPage(browser);
        teamManagementPage.createTeam(newTeamName, newTeamDescription);

        Awaitility.await()
            .untilAsserted(
                () -> assertThat(teamManagementPage.errorMessageList)
                    .as("Team Duplicated Error message should be displayed")
                    .extracting(WebElement::getText)
                    .anyMatch(it -> it.contains("Create team failed.")));

        teamManagementPage.errorMessageConfirmButton.click();
        teamManagementPage.createTeamForm.buttonCancel.click();
    }

    @Test
    @Order(3)
    void testEditTeam() {
        final TeamManagementPage teamManagementPage = new TeamManagementPage(browser);
        String editDescription = "edit_" + newTeamDescription;

        teamManagementPage.editTeam(newTeamName, editDescription);

        Awaitility.await()
            .untilAsserted(
                () -> assertThat(teamManagementPage.teamList)
                    .as("Team list should contain edited team")
                    .extracting(WebElement::getText)
                    .anyMatch(it -> it.contains(editDescription)));
    }

    @Test
    @Order(4)
    void testDeleteTeam() {
        final TeamManagementPage teamManagementPage = new TeamManagementPage(browser);

        teamManagementPage.deleteTeam(newTeamName);

        Awaitility.await()
            .untilAsserted(
                () -> {
                    browser.navigate().refresh();

                    assertThat(teamManagementPage.teamList)
                        .noneMatch(it -> it.getText().contains(newTeamName));
                });
    }
}
