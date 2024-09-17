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
import org.apache.streampark.e2e.pages.flink.ApacheFlinkPage;
import org.apache.streampark.e2e.pages.flink.FlinkHomePage;
import org.apache.streampark.e2e.pages.flink.applications.ApplicationForm;
import org.apache.streampark.e2e.pages.flink.applications.ApplicationsPage;
import org.apache.streampark.e2e.pages.flink.clusters.ClusterDetailForm;
import org.apache.streampark.e2e.pages.flink.clusters.FlinkClustersPage;
import org.apache.streampark.e2e.pages.flink.clusters.YarnSessionForm;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import static org.assertj.core.api.Assertions.assertThat;

@StreamPark(composeFiles = "docker/flink-1.18-on-yarn/docker-compose.yaml")
public class FlinkSQL118OnYarnTest {

    public static RemoteWebDriver browser;

    private static final String flinkName = "flink-1.18.1";

    private static final String flinkHome = "/flink-1.18.1";

    private static final String applicationName = "flink-118-e2e-test";

    private static final String flinkDescription = "description test";

    private static final String flinkClusterName = "flink_1.18.1_cluster_e2e";

    @BeforeAll
    public static void setup() {
        FlinkHomePage flinkHomePage = new LoginPage(browser)
            .login()
            .goToNav(ApacheFlinkPage.class)
            .goToTab(FlinkHomePage.class);

        flinkHomePage.createFlinkHome(flinkName, flinkHome, flinkDescription);

        FlinkClustersPage flinkClustersPage = flinkHomePage.goToNav(ApacheFlinkPage.class)
            .goToTab(FlinkClustersPage.class);

        flinkClustersPage.createFlinkCluster()
            .<YarnSessionForm>addCluster(ClusterDetailForm.ExecutionMode.YARN_SESSION)
            .resolveOrder(YarnSessionForm.ResolveOrder.PARENT_FIRST)
            .clusterName(flinkClusterName)
            .flinkVersion(flinkName)
            .submit();

        flinkClustersPage.startFlinkCluster(flinkClusterName);

        flinkClustersPage.goToNav(ApacheFlinkPage.class)
            .goToTab(ApplicationsPage.class);
    }

    @Test
    @Order(10)
    void testCreateFlinkApplicationOnYarnApplicationMode() {
        final ApplicationsPage applicationsPage = new ApplicationsPage(browser);

        applicationsPage
            .createApplication()
            .addApplication(
                ApplicationForm.DevelopmentMode.FLINK_SQL,
                ApplicationForm.ExecutionMode.YARN_APPLICATION,
                applicationName)
            .flinkVersion(flinkName)
            .flinkSql(Constants.TEST_FLINK_SQL)
            .submit();

        Awaitility.await()
            .untilAsserted(
                () -> assertThat(applicationsPage.applicationsList)
                    .as("Applications list should contain newly-created application")
                    .extracting(WebElement::getText)
                    .anyMatch(it -> it.contains(applicationName)));
    }

    @Test
    @Order(20)
    void testReleaseFlinkApplicationOnYarnApplicationMode() {
        final ApplicationsPage applicationsPage = new ApplicationsPage(browser);

        applicationsPage.releaseApplication(applicationName);

        Awaitility.await()
            .untilAsserted(
                () -> assertThat(applicationsPage.applicationsList)
                    .as("Applications list should contain released application")
                    .extracting(WebElement::getText)
                    .anyMatch(it -> it.contains("SUCCESS")));
    }

    @Test
    @Order(30)
    void testStartFlinkApplicationOnYarnApplicationMode() {
        final ApplicationsPage applicationsPage = new ApplicationsPage(browser);

        applicationsPage.startApplication(applicationName);

        Awaitility.await()
            .untilAsserted(
                () -> assertThat(applicationsPage.applicationsList)
                    .as("Applications list should contain started application")
                    .extracting(WebElement::getText)
                    .anyMatch(it -> it.contains("RUNNING")));

        Awaitility.await()
            .untilAsserted(
                () -> assertThat(applicationsPage.applicationsList)
                    .as("Applications list should contain finished application")
                    .extracting(WebElement::getText)
                    .anyMatch(it -> it.contains("FINISHED")));
    }

    @Test
    @Order(31)
    @SneakyThrows
    void testRestartAndCancelFlinkApplicationOnYarnApplicationMode() {
        Thread.sleep(Constants.DEFAULT_SLEEP_MILLISECONDS);
        final ApplicationsPage applicationsPage = new ApplicationsPage(browser);

        applicationsPage.startApplication(applicationName);

        Awaitility.await()
            .untilAsserted(
                () -> assertThat(applicationsPage.applicationsList)
                    .as("Applications list should contain restarted application")
                    .extracting(WebElement::getText)
                    .anyMatch(it -> it.contains("RUNNING")));

        applicationsPage.cancelApplication(applicationName);

        Awaitility.await()
            .untilAsserted(
                () -> assertThat(applicationsPage.applicationsList)
                    .as("Applications list should contain canceled application")
                    .extracting(WebElement::getText)
                    .anyMatch(it -> it.contains("CANCELED")));
    }

    @Test
    @Order(40)
    void testDeleteFlinkApplicationOnYarnApplicationMode() {
        final ApplicationsPage applicationsPage = new ApplicationsPage(browser);

        applicationsPage.deleteApplication(applicationName);

        Awaitility.await()
            .untilAsserted(
                () -> {
                    browser.navigate().refresh();

                    assertThat(applicationsPage.applicationsList)
                        .noneMatch(it -> it.getText().contains(applicationName));
                });
    }

    @Test
    @Order(50)
    void testCreateFlinkApplicationOnYarnPerJobMode() {
        final ApplicationsPage applicationsPage = new ApplicationsPage(browser);

        applicationsPage
            .createApplication()
            .addApplication(
                ApplicationForm.DevelopmentMode.FLINK_SQL,
                ApplicationForm.ExecutionMode.YARN_PER_JOB,
                applicationName)
            .flinkVersion(flinkName)
            .flinkSql(Constants.TEST_FLINK_SQL)
            .submit();

        Awaitility.await()
            .untilAsserted(
                () -> assertThat(applicationsPage.applicationsList)
                    .as("Applications list should contain newly-created application")
                    .extracting(WebElement::getText)
                    .anyMatch(it -> it.contains(applicationName)));
    }

    @Test
    @Order(60)
    void testReleaseFlinkApplicationOnYarnPerJobMode() {
        final ApplicationsPage applicationsPage = new ApplicationsPage(browser);

        applicationsPage.releaseApplication(applicationName);

        Awaitility.await()
            .untilAsserted(
                () -> assertThat(applicationsPage.applicationsList)
                    .as("Applications list should contain released application")
                    .extracting(WebElement::getText)
                    .anyMatch(it -> it.contains("SUCCESS")));
    }

    @Test
    @Order(70)
    void testStartFlinkApplicationOnYarnPerJobMode() {
        final ApplicationsPage applicationsPage = new ApplicationsPage(browser);

        applicationsPage.startApplication(applicationName);

        Awaitility.await()
            .untilAsserted(
                () -> assertThat(applicationsPage.applicationsList)
                    .as("Applications list should contain started application")
                    .extracting(WebElement::getText)
                    .anyMatch(it -> it.contains("RUNNING")));

        Awaitility.await()
            .untilAsserted(
                () -> assertThat(applicationsPage.applicationsList)
                    .as("Applications list should contain finished application")
                    .extracting(WebElement::getText)
                    .anyMatch(it -> it.contains("FINISHED")));
    }

    @Test
    @Order(71)
    @SneakyThrows
    void testRestartAndCancelFlinkApplicationOnYarnPerJobMode() {
        Thread.sleep(Constants.DEFAULT_SLEEP_MILLISECONDS);
        final ApplicationsPage applicationsPage = new ApplicationsPage(browser);

        applicationsPage.startApplication(applicationName);

        Awaitility.await()
            .untilAsserted(
                () -> assertThat(applicationsPage.applicationsList)
                    .as("Applications list should contain restarted application")
                    .extracting(WebElement::getText)
                    .anyMatch(it -> it.contains("RUNNING")));

        applicationsPage.cancelApplication(applicationName);

        Awaitility.await()
            .untilAsserted(
                () -> assertThat(applicationsPage.applicationsList)
                    .as("Applications list should contain canceled application")
                    .extracting(WebElement::getText)
                    .anyMatch(it -> it.contains("CANCELED")));
    }

    @Test
    @Order(80)
    void testDeleteFlinkApplicationOnYarnPerJobMode() {
        final ApplicationsPage applicationsPage = new ApplicationsPage(browser);

        applicationsPage.deleteApplication(applicationName);

        Awaitility.await()
            .untilAsserted(
                () -> {
                    browser.navigate().refresh();

                    assertThat(applicationsPage.applicationsList)
                        .noneMatch(it -> it.getText().contains(applicationName));
                });
    }

    @Test
    @Order(90)
    void testCreateFlinkApplicationOnYarnSessionMode() {
        final ApplicationsPage applicationsPage = new ApplicationsPage(browser);

        applicationsPage
            .createApplication()
            .addApplication(
                ApplicationForm.DevelopmentMode.FLINK_SQL,
                ApplicationForm.ExecutionMode.YARN_SESSION,
                applicationName)
            .flinkVersion(flinkName)
            .flinkSql(Constants.TEST_FLINK_SQL)
            .flinkCluster(flinkClusterName)
            .submit();

        Awaitility.await()
            .untilAsserted(
                () -> assertThat(applicationsPage.applicationsList)
                    .as("Applications list should contain newly-created application")
                    .extracting(WebElement::getText)
                    .anyMatch(it -> it.contains(applicationName)));
    }

    @Test
    @Order(100)
    void testReleaseFlinkApplicationOnYarnSessionMode() {
        final ApplicationsPage applicationsPage = new ApplicationsPage(browser);

        applicationsPage.releaseApplication(applicationName);

        Awaitility.await()
            .untilAsserted(
                () -> assertThat(applicationsPage.applicationsList)
                    .as("Applications list should contain released application")
                    .extracting(WebElement::getText)
                    .anyMatch(it -> it.contains("SUCCESS")));
    }

    @Test
    @Order(110)
    void testStartFlinkApplicationOnYarnSessionMode() {
        final ApplicationsPage applicationsPage = new ApplicationsPage(browser);

        applicationsPage.startApplication(applicationName);

        Awaitility.await()
            .untilAsserted(
                () -> assertThat(applicationsPage.applicationsList)
                    .as("Applications list should contain started application")
                    .extracting(WebElement::getText)
                    .anyMatch(it -> it.contains("RUNNING")));

        Awaitility.await()
            .untilAsserted(
                () -> assertThat(applicationsPage.applicationsList)
                    .as("Applications list should contain finished application")
                    .extracting(WebElement::getText)
                    .anyMatch(it -> it.contains("FINISHED")));
    }

    @Test
    @Order(120)
    @SneakyThrows
    void testRestartAndCancelFlinkApplicationOnYarnSessionMode() {
        Thread.sleep(Constants.DEFAULT_SLEEP_MILLISECONDS);
        final ApplicationsPage applicationsPage = new ApplicationsPage(browser);

        applicationsPage.startApplication(applicationName);

        Awaitility.await()
            .untilAsserted(
                () -> assertThat(applicationsPage.applicationsList)
                    .as("Applications list should contain restarted application")
                    .extracting(WebElement::getText)
                    .anyMatch(it -> it.contains("RUNNING")));

        applicationsPage.cancelApplication(applicationName);

        Awaitility.await()
            .untilAsserted(
                () -> assertThat(applicationsPage.applicationsList)
                    .as("Applications list should contain canceled application")
                    .extracting(WebElement::getText)
                    .anyMatch(it -> it.contains("CANCELED")));
    }

    @Test
    @Order(130)
    void testDeleteFlinkApplicationOnYarnSessionMode() {
        final ApplicationsPage applicationsPage = new ApplicationsPage(browser);

        applicationsPage.deleteApplication(applicationName);

        Awaitility.await()
            .untilAsserted(
                () -> {
                    browser.navigate().refresh();

                    assertThat(applicationsPage.applicationsList)
                        .noneMatch(it -> it.getText().contains(applicationName));
                });
    }
}
