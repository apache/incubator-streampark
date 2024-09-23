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
import org.apache.streampark.e2e.pages.system.TokenManagementPage;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import static org.assertj.core.api.Assertions.assertThat;

@StreamPark(composeFiles = "docker/basic/docker-compose.yaml")
public class TokenManagementTest {

    public static RemoteWebDriver browser;

    private static final String existUserName = "admin";

    private static final String newTokenDescription = "test_new_token_description";

    @BeforeAll
    public static void setup() {
        new LoginPage(browser)
            .login()
            .goToNav(SystemPage.class)
            .goToTab(TokenManagementPage.class);
    }

    @Test
    @Order(1)
    void testCreateToken() {
        final TokenManagementPage tokenManagementPage = new TokenManagementPage(browser);
        tokenManagementPage.createToken(existUserName, newTokenDescription);

        Awaitility.await()
            .untilAsserted(
                () -> assertThat(tokenManagementPage.tokenList)
                    .as("Token list should contain newly-created token")
                    .extracting(WebElement::getText)
                    .anyMatch(it -> it.contains(existUserName)));
    }

    @Test
    @Order(2)
    void testCopyToken() {
        final TokenManagementPage tokenManagementPage = new TokenManagementPage(browser);
        tokenManagementPage.copyToken(existUserName);

        // put clipboard value into createTokenForm.description
        tokenManagementPage.buttonCreateToken.click();
        tokenManagementPage.createTokenForm.inputDescription.sendKeys(Keys.CONTROL, "v");
        String token = tokenManagementPage.createTokenForm.inputDescription.getAttribute("value");
        tokenManagementPage.createTokenForm.buttonCancel.click();

        Awaitility.await()
            .untilAsserted(
                () -> assertThat(tokenManagementPage.tokenList)
                    .as("Clipboard should contain existing token.")
                    .extracting(WebElement::getText)
                    .anyMatch(it -> it.contains(token)));
    }

    @Test
    @Order(3)
    void testCreateDuplicateToken() {
        final TokenManagementPage tokenManagementPage = new TokenManagementPage(browser);

        tokenManagementPage.createToken(existUserName, newTokenDescription);

        Awaitility.await()
            .untilAsserted(
                () -> assertThat(browser.findElement(By.tagName("body")).getText())
                    .contains(String.format("user %s already has a token", existUserName)));

        tokenManagementPage.createTokenForm.buttonCancel.click();
    }

    @Test
    @Order(4)
    void testDeleteToken() {
        final TokenManagementPage teamManagementPage = new TokenManagementPage(browser);
        teamManagementPage.deleteToken(existUserName);

        Awaitility.await()
            .untilAsserted(
                () -> {
                    browser.navigate().refresh();
                    assertThat(teamManagementPage.tokenList)
                        .noneMatch(it -> it.getText().contains(existUserName));
                });
    }
}
