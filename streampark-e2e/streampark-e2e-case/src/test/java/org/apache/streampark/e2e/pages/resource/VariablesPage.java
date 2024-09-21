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
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;

@Getter
public class VariablesPage extends NavBarPage implements ResourcePage.Tab {

    @FindBy(id = "e2e-var-create-btn")
    public WebElement buttonCreateVariable;

    @FindBy(className = "ant-table-tbody")
    public List<WebElement> variableList;

    @FindBy(className = "swal2-html-container")
    public List<WebElement> errorMessageList;

    @FindBy(xpath = "//button[contains(@class, 'swal2-confirm')]")
    public WebElement errorMessageConfirmButton;

    @FindBy(className = "e2e-var-delete-confirm")
    public WebElement deleteConfirmButton;

    public final CreateVariableForm createVariableForm = new CreateVariableForm();

    public VariablesPage(RemoteWebDriver driver) {
        super(driver);
    }

    public VariablesPage createVariable(String variableCode, String variableValue, String description,
                                        boolean notVisible) {
        waitForPageLoading();

        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.elementToBeClickable(buttonCreateVariable));
        buttonCreateVariable.click();
        createVariableForm.inputVariableCode.sendKeys(variableCode);
        createVariableForm.inputVariableValue.sendKeys(variableValue);
        createVariableForm.inputDescription.sendKeys(description);
        if (notVisible) {
            createVariableForm.buttonDesensitization.click();
        }

        createVariableForm.buttonSubmit.click();

        return this;
    }

    public VariablesPage editVariable(String variableCode, String variableValue, String description,
                                      boolean notVisible) {
        waitForPageLoading();

        variableList.stream()
            .filter(it -> it.getText().contains(variableCode))
            .flatMap(
                it -> it.findElements(
                    By.className("e2e-var-edit-btn"))
                    .stream())
            .filter(WebElement::isDisplayed)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No edit button in variable list"))
            .click();

        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.elementToBeClickable(createVariableForm.buttonSubmit));
        createVariableForm.inputVariableValue.clear();
        createVariableForm.inputVariableValue.sendKeys(variableValue);
        createVariableForm.inputDescription.clear();
        createVariableForm.inputDescription.sendKeys(description);
        if (notVisible) {
            createVariableForm.buttonDesensitization.click();
        }
        createVariableForm.buttonSubmit.click();
        return this;
    }

    public VariablesPage deleteVariable(String variableCode) {
        waitForPageLoading();

        variableList.stream()
            .filter(it -> it.getText().contains(variableCode))
            .flatMap(
                it -> it.findElements(
                    By.className("e2e-var-delete-btn"))
                    .stream())
            .filter(WebElement::isDisplayed)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No delete button in variable list"))
            .click();

        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.elementToBeClickable(deleteConfirmButton));

        deleteConfirmButton.click();

        return this;
    }

    private void waitForPageLoading() {
        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.urlContains("/resource/variable"));
    }

    @Getter
    public class CreateVariableForm {

        CreateVariableForm() {
            PageFactory.initElements(driver, this);
        }

        @FindBy(id = "VariableForm_variableCode")
        public WebElement inputVariableCode;

        @FindBy(id = "VariableForm_variableValue")
        public WebElement inputVariableValue;

        @FindBy(id = "VariableForm_description")
        public WebElement inputDescription;

        @FindBy(id = "VariableForm_desensitization")
        public WebElement buttonDesensitization;

        @FindBy(className = "e2e-var-submit-btn")
        public WebElement buttonSubmit;

        @FindBy(className = "e2e-var-cancel-btn")
        public WebElement buttonCancel;
    }
}
