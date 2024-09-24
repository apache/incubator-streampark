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
public class ExternalLinkPage extends NavBarPage implements SettingPage.Tab {

    @FindBy(xpath = "//span[contains(., 'External Link')]/..//button[contains(@class, 'ant-btn-dashed')]/span[contains(text(), 'Add New')]")
    public WebElement buttonCreateExternalLink;

    @FindBy(xpath = "//tbody[contains(@class, 'ant-table-tbody')]")
    public List<WebElement> externalLinkList;

    @FindBy(className = "swal2-html-container")
    public List<WebElement> errorMessageList;

    @FindBy(xpath = "//button[contains(text(), 'OK')]")
    public WebElement errorMessageConfirmButton;

    @FindBy(xpath = "//button[contains(@class, 'ant-btn')]/span[contains(., 'OK')]")
    public WebElement deleteConfirmButton;

    public final CreateExternalLinkForm createExternalLinkForm = new CreateExternalLinkForm();

    public ExternalLinkPage(RemoteWebDriver driver) {
        super(driver);
    }

    public ExternalLinkPage createExternalLink(String label, String name, String color, String link) {
        waitForPageLoading();

        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.elementToBeClickable(buttonCreateExternalLink));
        buttonCreateExternalLink.click();
        createExternalLinkForm.inputLabel.sendKeys(label);
        createExternalLinkForm.inputName.sendKeys(name);
        createExternalLinkForm.inputColor.sendKeys(color);
        createExternalLinkForm.inputLink.sendKeys(link);

        createExternalLinkForm.buttonSubmit.click();

        return this;
    }

    public ExternalLinkPage editExternalLink(String label, String editLabel, String editName, String color,
                                             String editLink) {
        waitForPageLoading();

        externalLinkList.stream()
            .filter(it -> it.getText().contains(label))
            .flatMap(
                it -> it.findElements(By.className("e2e-extlink-edit-btn"))
                    .stream())
            .filter(WebElement::isDisplayed)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No edit button in external link list"))
            .click();

        createExternalLinkForm.inputLabel.clear();
        createExternalLinkForm.inputLabel.sendKeys(editLabel);
        createExternalLinkForm.inputName.clear();
        createExternalLinkForm.inputName.sendKeys(editName);
        createExternalLinkForm.inputColor.clear();
        createExternalLinkForm.inputColor.sendKeys(color);
        createExternalLinkForm.inputLink.clear();
        createExternalLinkForm.inputLink.sendKeys(editLink);

        createExternalLinkForm.buttonSubmit.click();
        return this;
    }

    public ExternalLinkPage deleteExternalLink(String label) {
        waitForPageLoading();

        externalLinkList.stream()
            .filter(it -> it.getText().contains(label))
            .flatMap(
                it -> it
                    .findElements(By.className("e2e-extlink-delete-btn"))
                    .stream())
            .filter(WebElement::isDisplayed)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No delete button in External link list"))
            .click();

        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.elementToBeClickable(deleteConfirmButton));

        deleteConfirmButton.click();

        return this;
    }

    private void waitForPageLoading() {
        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.urlContains("/setting/extlink"));
    }

    @Getter
    public class CreateExternalLinkForm {

        CreateExternalLinkForm() {
            PageFactory.initElements(driver, this);
        }

        @FindBy(id = "form_item_badgeLabel")
        public WebElement inputLabel;

        @FindBy(id = "form_item_badgeName")
        public WebElement inputName;

        @FindBy(id = "form_item_badgeColor")
        public WebElement inputColor;

        @FindBy(id = "form_item_linkUrl")
        public WebElement inputLink;

        @FindBy(className = "e2e-extlink-submit-btn")
        public WebElement buttonSubmit;

        @FindBy(className = "e2e-extlink-cancel-btn")
        public WebElement buttonCancel;
    }
}
