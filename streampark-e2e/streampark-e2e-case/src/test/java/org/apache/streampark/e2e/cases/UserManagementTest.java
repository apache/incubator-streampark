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
import org.apache.streampark.e2e.pages.system.UserManagementPage;
import org.apache.streampark.e2e.pages.system.entity.UserManagementStatus;
import org.apache.streampark.e2e.pages.system.entity.UserManagementUserType;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import static org.assertj.core.api.Assertions.assertThat;

@StreamPark(composeFiles = "docker/basic/docker-compose.yaml")
public class UserManagementTest {

    public static RemoteWebDriver browser;

    private static final String password = "streampark";

    private static final String newUserName = "test_new";

    private static final String newUserEmail = "test@email.com";

    @BeforeAll
    public static void setup() {
        new LoginPage(browser)
            .login()
            .goToNav(SystemPage.class)
            .goToTab(UserManagementPage.class);
    }

    @Test
    @Order(1)
    void testCreateUser() {
        final UserManagementPage userManagementPage = new UserManagementPage(browser);
        userManagementPage.createUser(
            newUserName, "test", password, newUserEmail, UserManagementUserType.ADMIN);

        Awaitility.await()
            .untilAsserted(
                () -> assertThat(userManagementPage.userList)
                    .as("User list should contain newly-created user")
                    .extracting(WebElement::getText)
                    .anyMatch(it -> it.contains(newUserName)));
    }

    @Test
    @Order(2)
    void testCreateDuplicateUser() {
        final UserManagementPage userManagementPage = new UserManagementPage(browser);
        userManagementPage.createUser(
            newUserName, "test", password, "test@email.com", UserManagementUserType.ADMIN);

        Awaitility.await()
            .untilAsserted(
                () -> assertThat(userManagementPage.errorMessageList)
                    .as("User Name Duplicated Error message should be displayed")
                    .extracting(WebElement::getText)
                    .anyMatch(it -> it.contains(
                        "Sorry the username already exists")));

        userManagementPage.createUserForm.buttonCancel.click();
    }

    @Test
    @Order(3)
    void testEditUser() {
        final UserManagementPage userManagementPage = new UserManagementPage(browser);
        String editEmail = "edit_" + newUserEmail;

        userManagementPage.editUser(
            newUserName, editEmail, UserManagementUserType.ADMIN, UserManagementStatus.LOCKED);

        Awaitility.await()
            .untilAsserted(
                () -> assertThat(userManagementPage.userList)
                    .as("User list should contain edited user")
                    .extracting(WebElement::getText)
                    .anyMatch(
                        it -> it.contains(UserManagementStatus.LOCKED
                            .toString().toLowerCase())));
    }
}
