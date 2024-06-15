/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package org.apache.streampark.e2e.pages.apacheflink.applications;

import lombok.Getter;
import lombok.SneakyThrows;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.FindBys;
import org.openqa.selenium.support.PageFactory;

import java.util.List;


@Getter
public final class ApplicationForm {
    private WebDriver driver;

    @FindBy(xpath = "//div[contains(@codefield, 'jobType')]//div[contains(@class, 'ant-select-selector')]")
    private WebElement buttonDevelopmentModeDropdown;

    @FindBys({
        @FindBy(css = "[codefield=jobType]"),
        @FindBy(className = "ant-select-item-option-content")
    })
    private List<WebElement> selectDevelopmentMode;

    @FindBy(xpath = "//div[contains(@codefield, 'executionMode')]//div[contains(@class, 'ant-select-selector')]")
    private WebElement buttonExecutionModeDropdown;

    @FindBys({
        @FindBy(css = "[codefield=executionMode]"),
        @FindBy(className = "ant-select-item-option-content")
    })
    private List<WebElement> selectExecutionMode;

    @FindBy(id = "form_item_jobName")
    private WebElement inputApplicationName;

    @FindBy(xpath = "//button[contains(@class, 'ant-btn')]//span[contains(text(), 'Submit')]")
    private WebElement buttonSubmit;

    @FindBy(xpath = "//button[contains(@class, 'ant-btn')]//span[contains(text(), 'Cancel')]")
    private WebElement buttonCancel;

    public ApplicationForm(WebDriver driver) {
        this.driver = driver;

        PageFactory.initElements(driver, this);
    }

    @SneakyThrows
    public ApplicationForm addApplication(DevelopmentMode developmentMode, ExecutionMode executionMode, String applicationName) {
        return this;
    }

    public enum DevelopmentMode {
        CUSTOM_CODE,
        FLINK_SQL,
        PYTHON_FLINK
    }

    public enum ExecutionMode {
        REMOTE,
        YARN_APPLICATION,
        YARN_SESSION,
        KUBERNETES_SESSION,
        KUBERNETES_APPLICATION,
        YARN_PER_JOB
    }
}
