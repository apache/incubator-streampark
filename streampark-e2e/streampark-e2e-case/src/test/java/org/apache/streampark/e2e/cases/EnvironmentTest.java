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
import org.apache.streampark.e2e.pages.common.Constants;
import org.apache.streampark.e2e.pages.setting.SettingPage;
import org.apache.streampark.e2e.pages.setting.env.DockerSettingForm;
import org.apache.streampark.e2e.pages.setting.env.EmailSettingForm;
import org.apache.streampark.e2e.pages.setting.env.EnvironmentDetailForm;
import org.apache.streampark.e2e.pages.setting.env.EnvironmentPage;
import org.apache.streampark.e2e.pages.setting.env.IngressSettingForm;
import org.apache.streampark.e2e.pages.setting.env.MavenSettingForm;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import static org.assertj.core.api.Assertions.assertThat;

@StreamPark(composeFiles = "docker/basic/docker-compose.yaml")
public class EnvironmentTest {

    private static RemoteWebDriver browser;

    private static final String userName = "admin";

    private static final String password = "streampark";

    private static final String teamName = "default";

    // maven
    final String mavenFilePath = "/maven/file/path";
    final String mavenCentralRepository = "https://mvnrepository.com/";
    final String mavenAuthUser = "maven_user";
    final String mavenAuthPassword = "maven_password";

    // ingress
    final String ingressDomainAddress = "https://localhost";

    // docker
    final String dockerAddress = "https://hub.docker.com/v2/";
    final String dockerNamespace = "hello";
    final String dockerUser = "docker_user";
    final String dockerPassword = "docker_password";

    // email
    final String emailHost = "smtp.163.com";
    final String emailPort = "25";
    final String emailAddress = "hello@163.com";
    final String emailUser = "email_password";
    final String emailPassword = "email_password";

    final @BeforeAll public static void setup() {
        new LoginPage(browser)
            .login(userName, password, teamName)
            .goToNav(SettingPage.class)
            .goToTab(EnvironmentPage.class);
    }

    @Test
    @Order(10)
    public void testCreateEnvironment() {
        final EnvironmentPage environmentPage = new EnvironmentPage(browser);

        environmentPage.createEnvironment(EnvironmentDetailForm.EnvSettingTypeEnum.Maven)
            .<MavenSettingForm>addSetting(EnvironmentDetailForm.EnvSettingTypeEnum.Maven)
            .filePath(mavenFilePath)
            .centralRepository(mavenCentralRepository)
            .authUser(mavenAuthUser)
            .authPassword(mavenAuthPassword);

        environmentPage.createEnvironment(EnvironmentDetailForm.EnvSettingTypeEnum.Ingress)
            .<IngressSettingForm>addSetting(EnvironmentDetailForm.EnvSettingTypeEnum.Ingress)
            .domainAddress(ingressDomainAddress);

        Awaitility.await()
            .untilAsserted(
                () -> assertThat(environmentPage.settingList())
                    .as("Setting list should contain newly-created setting")
                    .extracting(WebElement::getText)
                    .anyMatch(it -> it.contains(mavenFilePath))
                    .anyMatch(it -> it.contains(mavenCentralRepository))
                    .anyMatch(it -> it.contains(mavenAuthUser))
                    .anyMatch(it -> it.contains(ingressDomainAddress)));
    }

    @Test
    @Order(20)
    public void testCreateEmailSettingFailedWithAuth() {
        final EnvironmentPage environmentPage = new EnvironmentPage(browser);

        EmailSettingForm emailSettingForm =
            environmentPage.createEnvironment(EnvironmentDetailForm.EnvSettingTypeEnum.Email)
                .<EmailSettingForm>addSetting(EnvironmentDetailForm.EnvSettingTypeEnum.Email)
                .host(emailHost)
                .port(emailPort)
                .address(emailAddress)
                .user(emailUser)
                .password(emailPassword)
                .ok();

        String expectedErrorMessage =
            "connect to target mail server failed: 535 Error: authentication failed";
        Awaitility.await()

            .untilAsserted(
                () -> {
                    new WebDriverWait(browser, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION);
                    assertThat(environmentPage.errorMessageList())
                        .as("Connect failed error message should be displayed")
                        .extracting(WebElement::getText)
                        .anyMatch(it -> it.contains(expectedErrorMessage));
                });

        new WebDriverWait(browser, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.elementToBeClickable(environmentPage.errorMessageConfirmButton()));
        environmentPage.errorMessageConfirmButton().click();
        emailSettingForm.cancel();
    }

    @Test
    @Order(130)
    public void testCreateDockerSettingFailed() {
        final EnvironmentPage environmentPage = new EnvironmentPage(browser);

        DockerSettingForm dockerSettingForm =
            environmentPage.createEnvironment(EnvironmentDetailForm.EnvSettingTypeEnum.Docker)
                .<DockerSettingForm>addSetting(EnvironmentDetailForm.EnvSettingTypeEnum.Docker)
                .address(dockerAddress)
                .namespace(dockerNamespace)
                .user(dockerUser)
                .password(dockerPassword)
                .ok();

        String expectedErrorMessage = String.format(
            "Failed to validate Docker registry, error: Status 500: {\"message\":\"login attempt to %s failed with status: 404 Not Found\"}",
            dockerAddress);
        Awaitility.await()

            .untilAsserted(
                () -> {
                    new WebDriverWait(browser, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION);
                    assertThat(environmentPage.errorMessageList())
                        .as("Failed to validate docker registry error message should be displayed")
                        .extracting(WebElement::getText)
                        .anyMatch(it -> it.contains(expectedErrorMessage));
                });

        new WebDriverWait(browser, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.elementToBeClickable(environmentPage.errorMessageConfirmButton()));
        environmentPage.errorMessageConfirmButton().click();
        dockerSettingForm.cancel();
    }
}
