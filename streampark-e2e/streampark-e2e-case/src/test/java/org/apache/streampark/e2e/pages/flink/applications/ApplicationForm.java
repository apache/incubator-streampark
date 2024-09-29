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

package org.apache.streampark.e2e.pages.flink.applications;

import org.apache.streampark.e2e.pages.common.Constants;

import lombok.Getter;
import lombok.SneakyThrows;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.FindBys;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;

@Getter
public final class ApplicationForm {

    public WebDriver driver;

    @FindBy(xpath = "//div[contains(@codefield, 'jobType')]//div[contains(@class, 'ant-select-selector')]")
    public WebElement buttonJobTypeDropdown;

    @FindBys({
            @FindBy(css = "[codefield=jobType]"),
            @FindBy(className = "ant-select-item-option-content")
    })
    public List<WebElement> selectJobType;

    @FindBy(xpath = "//div[contains(@codefield, 'deployMode')]//div[contains(@class, 'ant-select-selector')]")
    public WebElement buttonDeployModeDropdown;

    @FindBys({
            @FindBy(css = "[codefield=deployMode]"),
            @FindBy(className = "ant-select-item-option-content")
    })
    public List<WebElement> selectDeployMode;

    @FindBy(id = "form_item_jobName")
    public WebElement inputApplicationName;

    @FindBy(xpath = "//div[contains(@codefield, 'yarnSessionClusterId')]//div[contains(@class, 'ant-select-selector')]")
    public WebElement buttonFlinkClusterDropdown;

    @FindBy(className = "ant-select-item-option-content")
    private List<WebElement> selectFlinkCluster;

    @FindBy(xpath = "//div[contains(@codefield, 'versionId')]//div[contains(@class, 'ant-select-selector')]")
    public WebElement buttonFlinkVersionDropdown;

    @FindBys({
            @FindBy(css = "[codefield=versionId]"),
            @FindBy(className = "ant-select-item-option-content")
    })
    public List<WebElement> selectFlinkVersion;

    @FindBy(xpath = "//button[contains(@class, 'ant-btn')]//span[contains(text(), 'Submit')]")
    public WebElement buttonSubmit;

    @FindBy(xpath = "//button[contains(@class, 'ant-btn')]//span[contains(text(), 'Cancel')]")
    public WebElement buttonCancel;

    public ApplicationForm(WebDriver driver) {
        this.driver = driver;

        PageFactory.initElements(driver, this);
    }

    @SneakyThrows
    public ApplicationForm addApplication(FlinkJobType jobType,
                                          DeployMode deployMode,
                                          String applicationName) {
        Thread.sleep(Constants.DEFAULT_SLEEP_MILLISECONDS);
        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.elementToBeClickable(buttonJobTypeDropdown));
        buttonJobTypeDropdown.click();
        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.visibilityOfAllElements(selectJobType));
        switch (jobType) {
            case CUSTOM_CODE:
                selectJobType.stream()
                    .filter(e -> e.getText().equalsIgnoreCase(FlinkJobType.CUSTOM_CODE.desc))
                    .findFirst()
                    .orElseThrow(
                        () -> new IllegalArgumentException(
                            String.format("Development mode not found: %s",
                                jobType.desc)))
                    .click();
                break;
            case FLINK_SQL:
                selectJobType.stream()
                    .filter(e -> e.getText().equalsIgnoreCase(FlinkJobType.FLINK_SQL.desc))
                    .findFirst()
                    .orElseThrow(
                        () -> new IllegalArgumentException(
                            String.format("Development mode not found: %s",
                                jobType.desc)))
                    .click();
                buttonDeployModeDropdown.click();
                switch (deployMode) {
                    case REMOTE:
                        selectDeployMode.stream()
                            .filter(e -> e.getText().equalsIgnoreCase(DeployMode.REMOTE.desc))
                            .findFirst()
                            .orElseThrow(
                                () -> new IllegalArgumentException(
                                    String.format("Execution mode not found: %s",
                                        deployMode.desc)))
                            .click();
                        break;
                    case YARN_APPLICATION:
                        selectDeployMode.stream()
                            .filter(e -> e.getText()
                                .equalsIgnoreCase(DeployMode.YARN_APPLICATION.desc))
                            .findFirst()
                            .orElseThrow(
                                () -> new IllegalArgumentException(
                                    String.format("Execution mode not found: %s",
                                        deployMode.desc)))
                            .click();

                        break;
                    case YARN_SESSION:
                        selectDeployMode.stream()
                            .filter(e -> e.getText().equalsIgnoreCase(DeployMode.YARN_SESSION.desc))
                            .findFirst()
                            .orElseThrow(
                                () -> new IllegalArgumentException(
                                    String.format("Execution mode not found: %s",
                                        deployMode.desc)))
                            .click();
                        break;
                    case KUBERNETES_SESSION:
                        selectDeployMode.stream()
                            .filter(e -> e.getText()
                                .equalsIgnoreCase(DeployMode.KUBERNETES_SESSION.desc))
                            .findFirst()
                            .orElseThrow(
                                () -> new IllegalArgumentException(
                                    String.format("Execution mode not found: %s",
                                        deployMode.desc)))
                            .click();
                        break;
                    case KUBERNETES_APPLICATION:
                        selectDeployMode.stream()
                            .filter(
                                e -> e.getText().equalsIgnoreCase(
                                    DeployMode.KUBERNETES_APPLICATION.desc))
                            .findFirst()
                            .orElseThrow(
                                () -> new IllegalArgumentException(
                                    String.format("Execution mode not found: %s",
                                        deployMode.desc)))
                            .click();
                        break;
                    case YARN_PER_JOB:
                        selectDeployMode.stream()
                            .filter(e -> e.getText().equalsIgnoreCase(DeployMode.YARN_PER_JOB.desc))
                            .findFirst()
                            .orElseThrow(
                                () -> new IllegalArgumentException(
                                    String.format("Execution mode not found: %s",
                                        deployMode.desc)))
                            .click();
                        break;
                    default:
                        throw new IllegalArgumentException(
                            String.format("Unknown execution mode: %s", deployMode.desc));
                }
                break;
            case PYTHON_FLINK:
                selectJobType.stream()
                    .filter(e -> e.getText().equalsIgnoreCase(FlinkJobType.PYTHON_FLINK.toString()))
                    .findFirst()
                    .orElseThrow(
                        () -> new IllegalArgumentException(
                            String.format("Development mode not found: %s",
                                jobType)))
                    .click();
                break;
            default:
                throw new IllegalArgumentException(
                    String.format("Unknown development mode: %s", jobType));
        }
        inputApplicationName.sendKeys(applicationName);

        return this;
    }

    public ApplicationForm flinkVersion(String flinkVersion) {
        new Actions(driver).moveToElement(buttonFlinkVersionDropdown).build().perform();
        buttonFlinkVersionDropdown.click();
        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.visibilityOfAllElements(selectFlinkVersion));
        selectFlinkVersion.stream()
            .filter(e -> e.getText().equalsIgnoreCase(flinkVersion))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Flink version not found"))
            .click();

        return this;
    }

    public ApplicationForm flinkSql(String flinkSql) {
        new FlinkSQLEditor(driver).content(flinkSql);
        return this;
    }

    @SneakyThrows
    public ApplicationForm flinkCluster(String flinkClusterName) {
        new Actions(driver).moveToElement(buttonFlinkClusterDropdown).build().perform();
        buttonFlinkClusterDropdown.click();
        Thread.sleep(Constants.DEFAULT_SLEEP_MILLISECONDS);
        selectFlinkCluster.stream()
            .filter(e -> e.getText().contains(flinkClusterName))
            .findFirst()
            .orElseThrow(
                () -> new IllegalArgumentException(
                    String.format("Flink cluster not found: %s", flinkClusterName)))
            .click();

        return this;
    }

    public ApplicationForm submit() {
        buttonSubmit.click();

        return this;
    }

    @Getter
    public enum FlinkJobType {

        CUSTOM_CODE("custom code"),
        FLINK_SQL("flink sql"),
        PYTHON_FLINK("python flink");

        private final String desc;

        FlinkJobType(String desc) {
            this.desc = desc;
        }
    }

    @Getter
    public enum DeployMode {

        REMOTE("remote"),
        YARN_APPLICATION("yarn application"),
        YARN_SESSION("yarn session"),
        KUBERNETES_SESSION("kubernetes session"),
        KUBERNETES_APPLICATION("kubernetes application"),
        YARN_PER_JOB("yarn per-job (deprecated, please use yarn-application mode)");

        private final String desc;

        DeployMode(String desc) {
            this.desc = desc;
        }
    }
}
