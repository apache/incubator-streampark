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

import org.apache.streampark.e2e.pages.common.NavBarPage;

import lombok.Getter;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

@Getter
public class ResourceManagementPage extends NavBarPage implements ResourcePage.Tab {

    @FindBy(xpath = "//span[contains(., 'Resource List')]/..//button[contains(@class, 'ant-btn-primary')]/span[contains(text(), 'Add New')]")
    private WebElement buttonCreateResource;

    private final CreateResourceForm createResourceForm = new CreateResourceForm();

    @FindBy(xpath = "//tbody[contains(@class, 'ant-table-tbody')]")
    private List<WebElement> resourceList;

    @FindBy(className = "swal2-html-container")
    private List<WebElement> errorMessageList;

    @FindBy(xpath = "//button[contains(text(), 'OK')]")
    private WebElement errorMessageConfirmButton;

    @FindBy(xpath = "//button[contains(@class, 'ant-btn')]/span[contains(., 'OK')]")
    private WebElement deleteConfirmButton;

    public ResourceManagementPage(RemoteWebDriver driver) {
        super(driver);
    }

    public ResourceManagementPage createResource(String engineType, String resourceType, String resourceName,
                                                 String resource,
                                                 String description) {
        waitForPageLoading();

        new WebDriverWait(driver, Duration.ofSeconds(10))
            .until(ExpectedConditions.elementToBeClickable(buttonCreateResource));
        buttonCreateResource.click();

        // select engine type.
        createResourceForm.btnSelectEngineTypeDropDown().click();
        new WebDriverWait(driver, Duration.ofSeconds(10))
            .until(ExpectedConditions.visibilityOfAllElements(createResourceForm.selectEngineType()));
        createResourceForm.selectResourceType().stream()
            .filter(e -> e.getText().equals(engineType))
            .findFirst()
            .orElseThrow(
                () -> new RuntimeException(
                    String.format("No %s in engineType dropdown list", engineType)))
            .click();

        // select resource type.
        createResourceForm.btnSelectResourceTypeDropDown().click();
        new WebDriverWait(driver, Duration.ofSeconds(10))
            .until(ExpectedConditions.visibilityOfAllElements(createResourceForm.selectResourceType()));
        createResourceForm.selectResourceType().stream()
            .filter(e -> e.getText().equals(resourceType))
            .findFirst()
            .orElseThrow(
                () -> new RuntimeException(
                    String.format("No %s in resourceType dropdown list", resourceType)))
            .click();

        createResourceForm.inputResourceName().sendKeys(resourceName);
        createResourceForm.textPom().sendKeys(resource);
        createResourceForm.inputDescription().sendKeys(description);

        createResourceForm.buttonSubmit().click();
        return this;
    }

    public ResourceManagementPage editResource(String engineType, String resourceType, String resourceName,
                                               String resource, String description) {
        waitForPageLoading();

        resourceList.stream()
            .filter(e -> e.getText().contains(resourceName))
            .flatMap(
                it -> it.findElements(By.xpath("//button[contains(@tooltip,'Modify Resource')]")).stream())
            .filter(WebElement::isDisplayed)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No edit button in resource list"))
            .click();

        // select engine type.
        createResourceForm.btnSelectEngineTypeDropDown().click();
        new WebDriverWait(driver, Duration.ofSeconds(10))
            .until(ExpectedConditions.visibilityOfAllElements(createResourceForm.selectEngineType()));
        createResourceForm.selectResourceType.stream()
            .filter(e -> e.getText().equals(engineType))
            .findFirst()
            .orElseThrow(
                () -> new RuntimeException(
                    String.format("No %s in resourceType dropdown list", resourceType)))
            .click();

        // select resource type.
        createResourceForm.btnSelectResourceTypeDropDown().click();
        new WebDriverWait(driver, Duration.ofSeconds(10))
            .until(ExpectedConditions.visibilityOfAllElements(createResourceForm.selectResourceType()));
        createResourceForm.selectResourceType().stream()
            .filter(e -> e.getText().equals(resourceType))
            .findFirst()
            .orElseThrow(
                () -> new RuntimeException(
                    String.format("No %s in resourceType dropdown list", resourceType)))
            .click();

        createResourceForm.textPom().sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
        createResourceForm.textPom().sendKeys(resource);
        createResourceForm.inputDescription().clear();
        createResourceForm.inputDescription().sendKeys(description);

        createResourceForm.buttonSubmit().click();
        return this;
    }

    public ResourceManagementPage deleteResource(String resourceName) {
        waitForPageLoading();

        resourceList.stream()
            .filter(e -> e.getText().contains(resourceName))
            .flatMap(
                it -> it.findElements(By.xpath("//button[contains(@tooltip,'Delete Resource')]")).stream())
            .filter(WebElement::isDisplayed)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No delete button in resource list"))
            .click();

        new WebDriverWait(driver, Duration.ofSeconds(10))
            .until(ExpectedConditions.elementToBeClickable(deleteConfirmButton));

        deleteConfirmButton.click();

        return this;
    }

    private void waitForPageLoading() {
        new WebDriverWait(driver, Duration.ofSeconds(10))
            .until(ExpectedConditions.urlContains("/resource/upload"));
    }

    @Getter
    public class CreateResourceForm {

        CreateResourceForm() {
            PageFactory.initElements(driver, this);
        }

        @FindBy(xpath = "//*[@id='form_item_engineType']/ancestor::div[contains(@class, 'ant-select-selector')]")
        private WebElement btnSelectEngineTypeDropDown;

        @FindBy(xpath = "//*[@id='form_item_engineType']//following::div[@class='ant-select-item-option-content']")
        private List<WebElement> selectEngineType;

        @FindBy(xpath = "//*[@id='form_item_resourceType']/ancestor::div[contains(@class, 'ant-select-selector')]")
        private WebElement btnSelectResourceTypeDropDown;

        @FindBy(xpath = "//*[@id='form_item_resourceType']//following::div[@class='ant-select-item-option-content']")
        private List<WebElement> selectResourceType;

        @FindBy(id = "ResourceForm_resourceName")
        private WebElement inputResourceName;

        @FindBy(css = "textarea.inputarea.monaco-mouse-cursor-text")
        private WebElement textPom;

        @FindBy(id = "ResourceForm_description")
        private WebElement inputDescription;

        @FindBy(xpath = "//button[contains(@class, 'ant-btn')]//span[contains(text(), 'Submit')]")
        private WebElement buttonSubmit;

        @FindBy(xpath = "//button[contains(@class, 'ant-btn')]//span[contains(text(), 'Cancel')]")
        private WebElement buttonCancel;
    }
}
