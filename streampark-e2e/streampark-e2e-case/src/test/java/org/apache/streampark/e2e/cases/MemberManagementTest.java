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
import org.apache.streampark.e2e.pages.system.MemberManagementPage;
import org.apache.streampark.e2e.pages.system.SystemPage;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import static org.assertj.core.api.Assertions.assertThat;

@StreamPark(composeFiles = "docker/basic/docker-compose.yaml")
public class MemberManagementTest {

    public static RemoteWebDriver browser;

    private static final String existUserName = "test3";

    private static final String existRole = "developer";

    @BeforeAll
    public static void setup() {
        new LoginPage(browser)
            .login()
            .goToNav(SystemPage.class)
            .goToTab(MemberManagementPage.class);
    }

    @Test
    @Order(1)
    void testCreateMember() {
        final MemberManagementPage memberManagementPage = new MemberManagementPage(browser);

        memberManagementPage.createMember(existUserName, existRole);

        Awaitility.await()
            .untilAsserted(
                () -> assertThat(memberManagementPage.memberList)
                    .as("Member list should contain newly-created member")
                    .extracting(WebElement::getText)
                    .anyMatch(it -> it.contains(existUserName)));
    }

    @Test
    @Order(2)
    void testCreateDuplicateMember() {
        final MemberManagementPage memberManagementPage = new MemberManagementPage(browser);

        memberManagementPage.createMember(existUserName, existRole);

        Awaitility.await()
            .untilAsserted(
                () -> assertThat(memberManagementPage.errorMessage)
                    .as("Member Duplicated Error message should be displayed")
                    .extracting(WebElement::getText)
                    .matches(it -> it.contains("please don't add it again.")));

        memberManagementPage.errorMessageConfirmButton.click();
        memberManagementPage.createMemberForm.buttonCancel.click();
    }

    @Test
    @Order(3)
    void testEditMember() {
        final MemberManagementPage memberManagementPage = new MemberManagementPage(browser);
        String anotherRole = "team admin";

        memberManagementPage.editMember(existUserName, anotherRole);

        Awaitility.await()
            .untilAsserted(
                () -> assertThat(memberManagementPage.memberList)
                    .as("Team list should contain edited team")
                    .extracting(WebElement::getText)
                    .anyMatch(
                        it -> it.contains(existUserName)));
    }

    @Test
    @Order(4)
    void testDeleteMember() {
        final MemberManagementPage memberManagementPage = new MemberManagementPage(browser);

        memberManagementPage.deleteMember(existUserName);

        Awaitility.await()
            .untilAsserted(
                () -> {
                    browser.navigate().refresh();

                    assertThat(memberManagementPage.memberList)
                        .noneMatch(it -> it.getText().contains(existUserName));
                });
    }
}
