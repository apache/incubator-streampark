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
import org.apache.streampark.e2e.pages.common.NavBarPage;
import org.apache.streampark.e2e.pages.flink.ApacheFlinkPage;

import lombok.Getter;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;

@Getter
public class ApplicationsPage extends NavBarPage implements ApacheFlinkPage.Tab {

    @FindBy(id = "e2e-flinkapp-create-btn")
    public WebElement buttonCreateApplication;

    @FindBy(className = "ant-table-tbody")
    public List<WebElement> applicationsList;

    @FindBy(className = "ant-form-item-explain-error")
    public List<WebElement> errorMessageList;

    @FindBy(className = "e2e-flinkapp-delete-btn")
    public WebElement deleteButton;

    @FindBy(className = "e2e-flinkapp-delete-confirm")
    public WebElement deleteConfirmButton;

    public ApplicationsPage(RemoteWebDriver driver) {
        super(driver);
    }

    public ApplicationForm createApplication() {
        waitForPageLoading();

        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.elementToBeClickable(buttonCreateApplication));

        buttonCreateApplication.click();
        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.urlContains("/flink/app/add"));
        return new ApplicationForm(driver);
    }

    public ApplicationsPage deleteApplication(String applicationName) {
        waitForPageLoading();

        WebElement extraButton = applicationsList.stream()
            .filter(it -> it.getText().contains(applicationName))
            .flatMap(
                it -> it.findElements(By.className("anticon-more"))
                    .stream())
            .filter(WebElement::isDisplayed)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No extra button in applications list"));
        Actions actions = new Actions(this.driver);
        actions.moveToElement(extraButton).perform();

        deleteButton.click();

        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.elementToBeClickable(deleteConfirmButton));

        deleteConfirmButton.click();

        return this;
    }

    public ApplicationsPage startApplication(String applicationName) {
        waitForPageLoading();

        applicationsList.stream()
            .filter(it -> it.getText().contains(applicationName))
            .flatMap(it -> it.findElements(By.className("e2e-flinkapp-startup-btn")).stream())
            .filter(WebElement::isDisplayed)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No start button in applications list"))
            .click();

        StartJobForm startJobForm = new StartJobForm();
        String startJobFormMessage = "Start Job";
        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(
                ExpectedConditions.visibilityOfElementLocated(
                    By.xpath(String.format("//*[contains(.,'%s')]",
                        startJobFormMessage))));

        startJobForm.buttonSubmit.click();
        return this;
    }

    public ApplicationsPage releaseApplication(String applicationName) {
        waitForPageLoading();

        applicationsList.stream()
            .filter(it -> it.getText().contains(applicationName))
            .flatMap(
                it -> it.findElements(By.className("e2e-flinkapp-release-btn"))
                    .stream())
            .filter(WebElement::isDisplayed)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No release button in applications list"))
            .click();

        return this;
    }

    public ApplicationsPage cancelApplication(String applicationName) {
        waitForPageLoading();

        applicationsList.stream()
            .filter(it -> it.getText().contains(applicationName))
            .flatMap(
                it -> it.findElements(By.className("e2e-flinkapp-cancel-btn"))
                    .stream())
            .filter(WebElement::isDisplayed)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No cancel button in applications list"))
            .click();

        CancelJobForm cancelJobForm = new CancelJobForm();
        String cancelJobFormMessage = "Stop Job";
        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(
                ExpectedConditions.visibilityOfElementLocated(
                    By.xpath(String.format("//*[contains(.,'%s')]",
                        cancelJobFormMessage))));

        cancelJobForm.buttonSubmit.click();

        return this;
    }

    private void waitForPageLoading() {
        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.urlContains("/flink/app"));
    }

    @Getter
    public class StartJobForm {

        StartJobForm() {
            PageFactory.initElements(driver, this);
        }

        @FindBy(id = "e2e-flinkapp-start-submit")
        public WebElement buttonSubmit;
    }

    @Getter
    public class CancelJobForm {

        CancelJobForm() {
            PageFactory.initElements(driver, this);
        }

        @FindBy(id = "e2e-flinkapp-stop-submit")
        public WebElement buttonSubmit;

        @FindBy(id = "e2e-flinkapp-stop-cancel")
        public WebElement buttonCancel;
    }
}
