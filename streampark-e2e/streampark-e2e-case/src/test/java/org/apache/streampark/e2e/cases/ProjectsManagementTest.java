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

import lombok.SneakyThrows;
import org.apache.streampark.e2e.core.StreamPark;
import org.apache.streampark.e2e.pages.LoginPage;
import org.apache.streampark.e2e.pages.common.Constants;
import org.apache.streampark.e2e.pages.resource.ProjectsPage;
import org.apache.streampark.e2e.pages.resource.ResourcePage;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@StreamPark(composeFiles = "docker/basic/docker-compose.yaml")
public class ProjectsManagementTest {

    private static RemoteWebDriver browser;

    private static final String userName = "admin";

    private static final String password = "streampark";

    private static final String teamName = "default";

    private static final String projectName = "e2e_test_project";

    private static final String editedProjectName = "e2e_test_project_edited";

    private static final String cvs = "GitHub/GitLab";

    private static final String url = "https://github.com/apache/incubator-streampark-quickstart";

    private static final String branch = "dev";

    private static final String buildArgument =
        "-pl quickstart-flink/quickstart-apacheflink/apacheflinksql_1.16 -am -Dmaven.test.skip=true";

    private static final String description = "e2e test project description";

    private static final Awaitility awaitility = new Awaitility();

    @BeforeAll
    public static void setup() {
        ProjectsPage projectsPage = new LoginPage(browser)
            .login(userName, password, teamName)
            .goToNav(ResourcePage.class)
            .goToTab(ProjectsPage.class);
    }

    @Test
    @Order(10)
    void testCreateProject() {
        final ProjectsPage projectsPage = new ProjectsPage(browser);

        projectsPage.createProject(projectName, cvs, url, branch, buildArgument, description);

        Awaitility.await()
            .untilAsserted(
                () -> assertThat(projectsPage.projectList())
                    .as("Projects list should contain newly-created project")
                    .extracting(WebElement::getText)
                    .anyMatch(it -> it.contains(projectName)));
    }

    @Test
    @Order(20)
    void testEditProject() {
        final ProjectsPage projectsPage = new ProjectsPage(browser);

        projectsPage.editProject(projectName, editedProjectName, cvs, url, branch, buildArgument, description);

        Awaitility.await()
            .untilAsserted(
                () -> assertThat(projectsPage.projectList())
                    .as("Projects list should contain edited project")
                    .extracting(WebElement::getText)
                    .anyMatch(it -> it.contains(editedProjectName)));
    }

    @Test
    @Order(30)
    void testBuildProject() {
        final ProjectsPage projectsPage = new ProjectsPage(browser);

        projectsPage.buildProject(editedProjectName);

        Awaitility.await().timeout(Duration.ofMinutes(Constants.DEFAULT_PROJECT_BUILD_TIMEOUT_MINUTES))
            .untilAsserted(
                () -> assertThat(projectsPage.projectList())
                    .as("Projects list should contain build successful project")
                    .extracting(WebElement::getText)
                    .anyMatch(it -> it.contains("SUCCESSFUL")));
    }

    @Test
    @Order(40)
    void testDeleteProject() {
        final ProjectsPage projectsPage = new ProjectsPage(browser);

        projectsPage.deleteProject(editedProjectName);

        awaitility.await()
            .untilAsserted(
                () -> {
                    browser.navigate().refresh();

                    assertThat(projectsPage.projectList())
                        .noneMatch(it -> it.getText().contains(editedProjectName));
                });
    }
}
