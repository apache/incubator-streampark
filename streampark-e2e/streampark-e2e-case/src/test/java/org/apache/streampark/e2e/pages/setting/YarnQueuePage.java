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

package org.apache.streampark.e2e.pages.setting;

import org.apache.streampark.e2e.pages.common.Constants;
import org.apache.streampark.e2e.pages.common.NavBarPage;

import lombok.Getter;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;

@Getter
public class YarnQueuePage extends NavBarPage implements SettingPage.Tab {

    @FindBy(id = "e2e-yarnqueue-create-btn")
    public WebElement buttonCreateYarnQueue;

    @FindBy(xpath = "//tbody[contains(@class, 'ant-table-tbody')]")
    public List<WebElement> yarnQueueList;

    @FindBy(className = "ant-form-item-explain-error")
    public List<WebElement> errorMessageList;

    @FindBy(className = "e2e-yarnqueue-delete-confirm")
    public WebElement deleteConfirmButton;

    public final CreateYarnQueueForm createYarnQueueForm = new CreateYarnQueueForm();

    public YarnQueuePage(RemoteWebDriver driver) {
        super(driver);
    }

    public YarnQueuePage createYarnQueue(String queueLabel, String description) {
        waitForPageLoading();

        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.elementToBeClickable(buttonCreateYarnQueue));
        buttonCreateYarnQueue.click();

        createYarnQueueForm.inputQueueLabel.sendKeys(queueLabel);
        createYarnQueueForm.inputDescription.sendKeys(description);

        createYarnQueueForm.buttonOk.click();

        return this;
    }

    public YarnQueuePage editYarnQueue(String queueLabel, String editQueueLabel, String description) {
        waitForPageLoading();

        yarnQueueList.stream()
            .filter(it -> it.getText().contains(queueLabel))
            .flatMap(
                it -> it.findElements(By.className("e2e-yarnqueue-edit-btn")).stream())
            .filter(WebElement::isDisplayed)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No edit button in yarn queue list"))
            .click();

        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.elementToBeClickable(createYarnQueueForm.buttonOk));
        createYarnQueueForm.inputQueueLabel.clear();
        createYarnQueueForm.inputQueueLabel.sendKeys(editQueueLabel);
        createYarnQueueForm.inputDescription.clear();
        createYarnQueueForm.inputDescription.sendKeys(description);

        createYarnQueueForm.buttonOk.click();

        return this;
    }

    public YarnQueuePage deleteYarnQueue(String queueLabel) {
        waitForPageLoading();

        yarnQueueList.stream()
            .filter(it -> it.getText().contains(queueLabel))
            .flatMap(
                it -> it.findElements(By.xpath(".//button[contains(@tooltip, 'Delete')]")).stream())
            .filter(WebElement::isDisplayed)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No delete button in yarn queue list"))
            .click();

        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.elementToBeClickable(deleteConfirmButton));

        deleteConfirmButton.click();
        return this;
    }

    private void waitForPageLoading() {
        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.urlContains("/setting/yarn-queue"));
    }

    @Getter
    public class CreateYarnQueueForm {

        CreateYarnQueueForm() {
            PageFactory.initElements(driver, this);
        }

        @FindBy(id = "YarnQueueEditForm_queueLabel")
        public WebElement inputQueueLabel;

        @FindBy(id = "YarnQueueEditForm_description")
        public WebElement inputDescription;

        @FindBy(className = "e2e-yarnqueue-submit-btn")
        public WebElement buttonOk;

        @FindBy(className = "e2e-yarnqueue-cancel-btn")
        public WebElement buttonCancel;
    }
}
