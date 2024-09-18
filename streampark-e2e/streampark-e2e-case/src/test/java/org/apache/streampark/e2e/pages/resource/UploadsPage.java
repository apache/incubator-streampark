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

package org.apache.streampark.e2e.pages.resource;

import org.apache.streampark.e2e.pages.common.Constants;
import org.apache.streampark.e2e.pages.common.NavBarPage;

import lombok.Getter;
import lombok.SneakyThrows;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;

@Getter
public class UploadsPage extends NavBarPage implements ResourcePage.Tab {

    @FindBy(id = "e2e-upload-create-btn")
    public WebElement buttonCreateResource;

    public final CreateUploadForm createUploadForm = new CreateUploadForm();

    @FindBy(className = "ant-table-tbody")
    public List<WebElement> resourceList;

    @FindBy(className = "swal2-html-container")
    public List<WebElement> errorMessageList;

    @FindBy(xpath = "//button[contains(text(), 'OK')]")
    public WebElement errorMessageConfirmButton;

    @FindBy(className = "e2e-upload-delete-confirm")
    public WebElement deleteConfirmButton;

    public UploadsPage(RemoteWebDriver driver) {
        super(driver);
    }

    @SneakyThrows
    public UploadsPage createUpload(String engineType, String resourceType, String resourceName,
                                    String resource,
                                    String description) {
        waitForPageLoading();

        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.elementToBeClickable(buttonCreateResource));
        buttonCreateResource.click();

        // select engine type.
        createUploadForm.btnSelectEngineTypeDropDown.click();
        Thread.sleep(Constants.DEFAULT_SLEEP_MILLISECONDS);
        createUploadForm.selectEngineType.stream()
            .filter(e -> e.getText().equals(engineType))
            .findFirst()
            .orElseThrow(
                () -> new RuntimeException(
                    String.format("No %s in engineType dropdown list", engineType)))
            .click();

        // select resource type.
        createUploadForm.btnSelectResourceTypeDropDown.click();
        Thread.sleep(Constants.DEFAULT_SLEEP_MILLISECONDS);
        createUploadForm.selectResourceType.stream()
            .filter(e -> e.getText().equals(resourceType))
            .findFirst()
            .orElseThrow(
                () -> new RuntimeException(
                    String.format("No %s in resourceType dropdown list", resourceType)))
            .click();

        createUploadForm.inputResourceName.sendKeys(resourceName);
        createUploadForm.textPom.sendKeys(resource);
        createUploadForm.inputDescription.sendKeys(description);

        createUploadForm.buttonSubmit.click();

        return this;
    }

    @SneakyThrows
    public UploadsPage editUpload(String engineType, String resourceType, String resourceName,
                                  String resource, String description) {
        waitForPageLoading();

        resourceList.stream()
            .filter(e -> e.getText().contains(resourceName))
            .flatMap(
                it -> it.findElements(By.className("e2e-upload-edit-btn")).stream())
            .filter(WebElement::isDisplayed)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No edit button in resource list"))
            .click();

        // select engine type.
        createUploadForm.btnSelectEngineTypeDropDown.click();
        Thread.sleep(Constants.DEFAULT_SLEEP_MILLISECONDS);
        createUploadForm.selectEngineType.stream()
            .filter(e -> e.getText().equals(engineType))
            .findFirst()
            .orElseThrow(
                () -> new RuntimeException(
                    String.format("No %s in engineType dropdown list", resourceType)))
            .click();

        // select resource type.
        createUploadForm.btnSelectResourceTypeDropDown.click();
        Thread.sleep(Constants.DEFAULT_SLEEP_MILLISECONDS);
        createUploadForm.selectResourceType.stream()
            .filter(e -> e.getText().equals(resourceType))
            .findFirst()
            .orElseThrow(
                () -> new RuntimeException(
                    String.format("No %s in resourceType dropdown list", resourceType)))
            .click();

        createUploadForm.textPom.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
        createUploadForm.textPom.sendKeys(resource);
        createUploadForm.inputDescription.clear();
        createUploadForm.inputDescription.sendKeys(description);

        createUploadForm.buttonSubmit.click();

        return this;
    }

    public UploadsPage deleteUpload(String resourceName) {
        waitForPageLoading();

        resourceList.stream()
            .filter(e -> e.getText().contains(resourceName))
            .flatMap(
                it -> it.findElements(By.className("e2e-upload-delete-btn")).stream())
            .filter(WebElement::isDisplayed)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No delete button in resource list"))
            .click();

        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.elementToBeClickable(deleteConfirmButton));

        deleteConfirmButton.click();

        return this;
    }

    private void waitForPageLoading() {
        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.urlContains("/resource/upload"));
    }

    @Getter
    public class CreateUploadForm {

        CreateUploadForm() {
            PageFactory.initElements(driver, this);
        }

        @FindBy(xpath = "//*[@id='form_item_engineType']/ancestor::div[contains(@class, 'ant-select-selector')]")
        private WebElement btnSelectEngineTypeDropDown;

        @FindBy(xpath = "//*[@id='form_item_engineType']//following::div[@class='ant-select-item-option-content']")
        public List<WebElement> selectEngineType;

        @FindBy(xpath = "//*[@id='form_item_resourceType']/ancestor::div[contains(@class, 'ant-select-selector')]")
        public WebElement btnSelectResourceTypeDropDown;

        @FindBy(xpath = "//*[@id='form_item_resourceType']//following::div[@class='ant-select-item-option-content']")
        public List<WebElement> selectResourceType;

        @FindBy(id = "upload_form_resourceName")
        public WebElement inputResourceName;

        @FindBy(css = "textarea.inputarea.monaco-mouse-cursor-text")
        public WebElement textPom;

        @FindBy(id = "upload_form_description")
        public WebElement inputDescription;

        @FindBy(className = "e2e-upload-submit-btn")
        public WebElement buttonSubmit;

        @FindBy(className = "e2e-upload-cancel-btn")
        public WebElement buttonCancel;
    }
}
