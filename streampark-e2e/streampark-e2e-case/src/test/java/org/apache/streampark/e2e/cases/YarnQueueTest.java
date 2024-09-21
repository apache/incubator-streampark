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
import org.apache.streampark.e2e.pages.setting.SettingPage;
import org.apache.streampark.e2e.pages.setting.YarnQueuePage;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import static org.assertj.core.api.Assertions.assertThat;

@StreamPark(composeFiles = "docker/basic/docker-compose.yaml")
public class YarnQueueTest {

    public static RemoteWebDriver browser;

    private static final String newQueueLabel = "new_label";

    private static final String editQueueLabel = "edit_label";

    private static final String description = "test_description";

    @BeforeAll
    public static void setup() {
        new LoginPage(browser)
            .login()
            .goToNav(SettingPage.class)
            .goToTab(YarnQueuePage.class);
    }

    @Test
    @Order(1)
    void testYarnQueue() {
        final YarnQueuePage queuePage = new YarnQueuePage(browser);
        queuePage.createYarnQueue(newQueueLabel, description);

        Awaitility.await()
            .untilAsserted(
                () -> assertThat(queuePage.yarnQueueList)
                    .as("Yarn Queue list should contain newly-created item")
                    .extracting(WebElement::getText)
                    .anyMatch(it -> it.contains(newQueueLabel))
                    .anyMatch(it -> it.contains(description)));
    }

    @Test
    @Order(2)
    void testCreateDuplicateYarnQueue() {
        final YarnQueuePage queuePage = new YarnQueuePage(browser);
        queuePage.createYarnQueue(newQueueLabel, description);
        Awaitility.await()
            .untilAsserted(
                () -> assertThat(queuePage.errorMessageList)
                    .as("Yarn Queue Duplicated Error message should be displayed")
                    .extracting(WebElement::getText)
                    .anyMatch(it -> it.contains("The queue label existed in the current team")));

        queuePage.createYarnQueueForm.buttonCancel.click();
    }

    @Test
    @Order(3)
    void testEditYarnQueue() {
        final YarnQueuePage queuePage = new YarnQueuePage(browser);
        String editDescription = "edit_" + description;

        queuePage.editYarnQueue(newQueueLabel, editQueueLabel, editDescription);

        Awaitility.await()
            .untilAsserted(
                () -> assertThat(queuePage.yarnQueueList)
                    .as("Yarn queue list should contain edited yarn queue")
                    .extracting(WebElement::getText)
                    .anyMatch(it -> it.contains(editQueueLabel))
                    .anyMatch(it -> it.contains(editDescription)));
    }

    @Test
    @Order(4)
    void testDeleteYarnQueue() {
        final YarnQueuePage queuePage = new YarnQueuePage(browser);

        queuePage.deleteYarnQueue(editQueueLabel);
        Awaitility.await()
            .untilAsserted(
                () -> {
                    assertThat(queuePage.yarnQueueList)
                        .noneMatch(it -> it.getText().contains(editQueueLabel));
                });
    }
}
