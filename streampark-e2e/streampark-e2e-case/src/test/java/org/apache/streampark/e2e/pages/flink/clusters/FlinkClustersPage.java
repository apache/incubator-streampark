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
package org.apache.streampark.e2e.pages.flink.clusters;

import org.apache.streampark.e2e.pages.common.Constants;
import org.apache.streampark.e2e.pages.common.NavBarPage;
import org.apache.streampark.e2e.pages.flink.ApacheFlinkPage;

import lombok.Getter;
import lombok.SneakyThrows;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

import static org.apache.streampark.e2e.pages.common.CommonFactory.WebDriverWaitForElementVisibilityAndInvisibility;
import static org.apache.streampark.e2e.pages.common.CommonFactory.WebDriverWaitForElementVisibilityAndInvisibilityWithDuration;

@Getter
public class FlinkClustersPage extends NavBarPage implements ApacheFlinkPage.Tab {

    @FindBy(xpath = "//span[contains(., 'Flink Cluster')]/..//button[contains(@class, 'ant-btn')]/span[contains(text(), 'Add New')]")
    private WebElement buttonCreateFlinkCluster;

    @FindBy(xpath = "//div[contains(@class, 'ant-spin-container')]")
    private List<WebElement> flinkClusterList;

    @FindBy(xpath = "//button[contains(@class, 'ant-btn')]/span[contains(., 'Yes')]")
    private WebElement deleteConfirmButton;

    public FlinkClustersPage(RemoteWebDriver driver) {
        super(driver);
    }

    public ClusterDetailForm createFlinkCluster() {
        waitForPageLoading();

        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.elementToBeClickable(buttonCreateFlinkCluster));
        buttonCreateFlinkCluster.click();
        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.urlContains("/flink/add_cluster"));

        return new ClusterDetailForm(driver);
    }

    public ClusterDetailForm editFlinkCluster(String flinkClusterName) {
        waitForPageLoading();

        flinkClusterList().stream()
            .filter(it -> it.getText().contains(flinkClusterName))
            .flatMap(
                it -> it
                    .findElements(
                        By.className("anticon-edit"))
                    .stream())
            .filter(WebElement::isDisplayed)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No edit button in flink clusters list"))
            .click();

        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.urlContains("/flink/edit_cluster"));
        return new ClusterDetailForm(driver);
    }

    @SneakyThrows
    public FlinkClustersPage startFlinkCluster(String flinkClusterName) {
        waitForPageLoading();
        Thread.sleep(Constants.DEFAULT_SLEEP_MILLISECONDS);
        flinkClusterList().stream()
            .filter(it -> it.getText().contains(flinkClusterName))
            .flatMap(
                it -> it
                    .findElements(
                        By.className("anticon-play-circle"))
                    .stream())
            .filter(WebElement::isDisplayed)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No start button in flink clusters list"))
            .click();

        String startMessage = "The current cluster is started";
        WebDriverWaitForElementVisibilityAndInvisibilityWithDuration(driver, startMessage, Duration.ofMinutes(1));
        return this;
    }

    public FlinkClustersPage stopFlinkCluster(String flinkClusterName) {
        waitForPageLoading();

        flinkClusterList().stream()
            .filter(it -> it.getText().contains(flinkClusterName))
            .flatMap(
                it -> it
                    .findElements(
                        By.className("anticon-pause-circle"))
                    .stream())
            .filter(WebElement::isDisplayed)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No stop button in flink clusters list"))
            .click();

        String stopMessage = "The current cluster is shutdown";
        WebDriverWaitForElementVisibilityAndInvisibility(driver, stopMessage);
        return this;
    }

    public FlinkClustersPage deleteFlinkCluster(String flinkName) {
        waitForPageLoading();

        flinkClusterList().stream()
            .filter(it -> it.getText().contains(flinkName))
            .flatMap(
                it -> it
                    .findElements(
                        By.className("anticon-delete"))
                    .stream())
            .filter(WebElement::isDisplayed)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No delete button in flink clusters list"))
            .click();

        deleteConfirmButton.click();
        String deleteMessage = "The current cluster is remove";
        WebDriverWaitForElementVisibilityAndInvisibility(driver, deleteMessage);
        return this;
    }

    private void waitForPageLoading() {
        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.urlContains("/flink/cluster"));
    }
}
