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
import org.apache.streampark.e2e.pages.setting.ExternalLinkPage;
import org.apache.streampark.e2e.pages.setting.SettingPage;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import static org.assertj.core.api.Assertions.assertThat;

@StreamPark(composeFiles = "docker/basic/docker-compose.yaml")
public class ExternalLinkTest {

    public static RemoteWebDriver browser;

    private static final String newLabel = "new_label";

    private static final String editLabel = "edit_label";

    private static final String newName = "new_name";

    private static final String color = "#b54f4f";

    private static final String newLink = "https://grafana/flink-monitoring?var-JobId=var-JobId=1";

    @BeforeAll
    public static void setup() {
        new LoginPage(browser)
            .login()
            .goToNav(SettingPage.class)
            .goToTab(ExternalLinkPage.class);
    }

    @Test
    @Order(1)
    void testCreateExternalLink() {
        final ExternalLinkPage externalLinkPage = new ExternalLinkPage(browser);
        externalLinkPage.createExternalLink(newLabel, newName, color, newLink);

        Awaitility.await()
            .untilAsserted(
                () -> assertThat(externalLinkPage.externalLinkList)
                    .as("External link list should contain newly-created link")
                    .extracting(WebElement::getText)
                    .anyMatch(it -> it.contains(newLabel))
                    .anyMatch(it -> it.contains(newName))
                    .anyMatch(it -> it.contains(newLink)));
    }

    @Test
    @Order(2)
    void testCreateDuplicateExternalLink() {
        final ExternalLinkPage externalLinkPage = new ExternalLinkPage(browser);
        externalLinkPage.createExternalLink(newLabel, newName, color, newLink);

        Awaitility.await()
            .untilAsserted(
                () -> assertThat(externalLinkPage.errorMessageList)
                    .as("Name Duplicated Error message should be displayed")
                    .extracting(WebElement::getText)
                    .anyMatch(it -> it.contains(
                        String.format("The name: %s is already existing.", newName))));

        externalLinkPage.errorMessageConfirmButton.click();
        externalLinkPage.createExternalLinkForm.buttonCancel.click();
    }

    @Test
    @Order(3)
    void testEditExternalLink() {
        final ExternalLinkPage externalLinkPage = new ExternalLinkPage(browser);
        String editName = "edit_name";
        String editLink = "https://grafana/flink-monitoring?var-Job=var-Job=edit";
        externalLinkPage.editExternalLink(newLabel, editLabel, editName, color, editLink);

        Awaitility.await()
            .untilAsserted(
                () -> assertThat(externalLinkPage.externalLinkList)
                    .as("External link list should contain edited link")
                    .extracting(WebElement::getText)
                    .anyMatch(it -> it.contains(editLabel))
                    .anyMatch(it -> it.contains(editName))
                    .anyMatch(it -> it.contains(editLink)));
    }

    @Test
    @Order(4)
    void testDeleteExternalLink() {
        final ExternalLinkPage externalLinkPage = new ExternalLinkPage(browser);

        externalLinkPage.deleteExternalLink(editLabel);

        Awaitility.await()
            .untilAsserted(
                () -> {
                    assertThat(externalLinkPage.externalLinkList)
                        .noneMatch(it -> it.getText().contains(editLabel));
                });
    }
}
