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

package org.apache.streampark.e2e.cases.resource;

import org.apache.streampark.e2e.core.StreamPark;
import org.apache.streampark.e2e.pages.LoginPage;
import org.apache.streampark.e2e.pages.resource.ResourceManagementPage;
import org.apache.streampark.e2e.pages.resource.ResourcePage;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import static org.assertj.core.api.Assertions.assertThat;

@StreamPark(composeFiles = "docker/basic/docker-compose.yaml")
public class ResourceManagementTest {

    private static RemoteWebDriver browser;

    private static final String userName = "admin";

    private static final String password = "streampark";

    private static final String teamName = "default";

    private static final String engineType = "Apache Flink";

    private static final String resourceType = "Jar Library";

    private static final String resourceName = "test-resource";

    private static final String mavenPom =
        "<dependency>\n" +
            "    <groupId>junit</groupId>\n" +
            "    <artifactId>junit</artifactId>\n" +
            "    <version>4.13.2</version>\n" +
            "    <scope>test</scope>\n" +
            "</dependency>";

    private static final String description = "Junit-jar-lib";

    @BeforeAll
    public static void setup() {
        new LoginPage(browser)
            .login(userName, password, teamName)
            .goToNav(ResourcePage.class)
            .goToTab(ResourceManagementPage.class);
    }

    @Test
    @Order(10)
    void testCreateResource() {
        final ResourceManagementPage resourceManagementPage = new ResourceManagementPage(browser);
        resourceManagementPage.createResource(engineType, resourceType, resourceName, mavenPom, description);

        Awaitility.await()
            .untilAsserted(
                () -> assertThat(resourceManagementPage.resourceList())
                    .as("Resource list should contain newly-created resource")
                    .extracting(WebElement::getText)
                    .anyMatch(it -> it.contains(resourceName))
                    .anyMatch(it -> it.contains(description))
                    /*
                     * todo: make Resource Type filed value same, hardcode here to pass test currently. In add resource
                     * page: Add Resource -> Resource Type: Jar Library In upload page: Resource List -> Resource Type:
                     * Jar library
                     */
                    .anyMatch(it -> it.contains("Jar library"))
                    .anyMatch(it -> it.contains(engineType)));
    }

    @Test
    @Order(20)
    void testCreateDuplicateResource() {
        final ResourceManagementPage resourceManagementPage = new ResourceManagementPage(browser);
        resourceManagementPage.createResource(engineType, resourceType, resourceName, mavenPom, description);

        Awaitility.await()
            .untilAsserted(
                () -> assertThat(resourceManagementPage.errorMessageList())
                    .as("Resource Name Duplicated Error message should be displayed")
                    .extracting(WebElement::getText)
                    .anyMatch(it -> it.contains(
                        String.format("the resource %s already exists, please check.", resourceName))));

        resourceManagementPage.errorMessageConfirmButton().click();
        resourceManagementPage.createResourceForm().buttonCancel().click();
    }

    @Test
    @Order(30)
    void testEditResource() {
        final ResourceManagementPage resourceManagementPage = new ResourceManagementPage(browser);
        String editDescription = "Kafka-jar-lib";
        String editResource =
            "<dependency>\n" +
                "    <groupId>org.apache.kafka</groupId>\n" +
                "    <artifactId>kafka-clients</artifactId>\n" +
                "    <version>3.7.1</version>\n" +
                "</dependency>";

        resourceManagementPage.editResource(engineType, resourceType, resourceName, editResource,
            editDescription);

        Awaitility.await()
            .untilAsserted(
                () -> assertThat(resourceManagementPage.resourceList())
                    .as("Resource list should contain edit resource")
                    .extracting(WebElement::getText)
                    .anyMatch(it -> it.contains(resourceName))
                    .anyMatch(it -> it.contains(editDescription))
                    /*
                     * todo: make Resource Type filed value same, hardcode here to pass test currently. In add resource
                     * page: Add Resource -> Resource Type: Jar Library In upload page: Resource List -> Resource Type:
                     * Jar library
                     */
                    .anyMatch(it -> it.contains("Jar library"))
                    .anyMatch(it -> it.contains(engineType)));
    }

    @Test
    @Order(40)
    void testDeleteResource() {
        final ResourceManagementPage resourceManagementPage = new ResourceManagementPage(browser);
        resourceManagementPage.deleteResource(resourceName);
        Awaitility.await()
            .untilAsserted(
                () -> {
                    browser.navigate().refresh();

                    assertThat(resourceManagementPage.resourceList())
                        .noneMatch(it -> it.getText().contains(resourceName));
                });
    }
}
