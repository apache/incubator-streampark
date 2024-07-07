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
package org.apache.streampark.e2e.pages.resource;

import org.apache.streampark.e2e.pages.common.NavBarPage;

import lombok.Getter;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

@Getter
public class VariableManagementPage extends NavBarPage implements ResourcePage.Tab {

    @FindBy(xpath = "//span[contains(., 'Variable List')]/..//button[contains(@class, 'ant-btn-primary')]/span[contains(text(), 'Add New')]")
    private WebElement buttonCreateVariable;

    @FindBy(xpath = "//tbody[contains(@class, 'ant-table-tbody')]")
    private List<WebElement> variableList;

    @FindBy(className = "swal2-html-container")
    private List<WebElement> errorMessageList;

    @FindBy(xpath = "//button[contains(text(), 'OK')]")
    private WebElement errorMessageConfirmButton;

    @FindBy(xpath = "//button[contains(@class, 'ant-btn')]/span[contains(., 'OK')]")
    private WebElement deleteConfirmButton;

    private final CreateVariableForm createVariableForm = new CreateVariableForm();

    public VariableManagementPage(RemoteWebDriver driver) {
        super(driver);
    }

    public VariableManagementPage createVariable(String variableCode, String variableValue, String description,
                                                 boolean notVisible) {
        waitForPageLoading();

        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.elementToBeClickable(buttonCreateVariable));
        buttonCreateVariable.click();
        createVariableForm.inputVariableCode().sendKeys(variableCode);
        createVariableForm.inputVariableValue().sendKeys(variableValue);
        createVariableForm.inputDescription().sendKeys(description);
        if (notVisible) {
            createVariableForm.buttonDesensitization.click();
        }

        createVariableForm.buttonSubmit().click();
        return this;
    }

    public VariableManagementPage editVariable(String variableCode, String variableValue, String description,
                                               boolean notVisible) {
        waitForPageLoading();

        variableList().stream()
                .filter(it -> it.getText().contains(variableCode))
                .flatMap(
                        it -> it.findElements(By.xpath("//button[contains(@tooltip,'Modify Variable')]")).stream())
                .filter(WebElement::isDisplayed)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No edit button in variable list"))
                .click();

        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.elementToBeClickable(createVariableForm.buttonSubmit));
        createVariableForm.inputVariableValue().sendKeys(variableValue);
        createVariableForm.inputDescription().clear();
        createVariableForm.inputDescription().sendKeys(description);
        if (notVisible) {
            createVariableForm.buttonDesensitization.click();
        }
        createVariableForm.buttonSubmit().click();

        return this;
    }

    public VariableManagementPage deleteVariable(String variableCode) {
        waitForPageLoading();

        variableList().stream()
                .filter(it -> it.getText().contains(variableCode))
                .flatMap(
                        it -> it.findElements(By.xpath("//button[contains(@tooltip,'Delete Variable')]")).stream())
                .filter(WebElement::isDisplayed)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No delete button in variable list"))
                .click();

        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.elementToBeClickable(deleteConfirmButton));

        deleteConfirmButton.click();
        return this;
    }

    private void waitForPageLoading() {
        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.urlContains("/resource/variable"));
    }

    @Getter
    public class CreateVariableForm {

        CreateVariableForm() {
            PageFactory.initElements(driver, this);
        }

        @FindBy(id = "VariableForm_variableCode")
        private WebElement inputVariableCode;

        @FindBy(id = "VariableForm_variableValue")
        private WebElement inputVariableValue;

        @FindBy(id = "VariableForm_description")
        private WebElement inputDescription;

        @FindBy(id = "VariableForm_desensitization")
        private WebElement buttonDesensitization;

        @FindBy(xpath = "//button[contains(@class, 'ant-btn')]//span[contains(., 'Submit')]")
        private WebElement buttonSubmit;

        @FindBy(xpath = "//button[contains(@class, 'ant-btn')]//span[contains(., 'Cancel')]")
        private WebElement buttonCancel;
    }
}
