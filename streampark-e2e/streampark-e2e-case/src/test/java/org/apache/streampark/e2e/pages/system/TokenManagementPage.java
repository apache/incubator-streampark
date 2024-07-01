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
package org.apache.streampark.e2e.pages.system;

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
public class TokenManagementPage extends NavBarPage implements SystemPage.Tab {

    @FindBy(xpath = "//span[contains(., 'Token List')]/..//button[contains(@class, 'ant-btn-primary')]/span[contains(text(), 'Add New')]")
    private WebElement buttonCreateToken;

    @FindBy(xpath = "//tbody[contains(@class, 'ant-table-tbody')]")
    private List<WebElement> tokenList;

    @FindBy(xpath = "//button[contains(@class, 'ant-btn')]/span[contains(., 'OK')]")
    private WebElement deleteConfirmButton;

    @FindBy(className = "ant-form-item-explain-error")
    private WebElement errorMessageSearchLayout;

    private final CreateTokenForm createTokenForm = new CreateTokenForm();

    public TokenManagementPage(RemoteWebDriver driver) {
        super(driver);
    }

    public TokenManagementPage createToken(String existUserName, String description) {
        waitForPageLoading();

        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.elementToBeClickable(buttonCreateToken));
        buttonCreateToken.click();

        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.elementToBeClickable(createTokenForm.inputUserName()));
        createTokenForm.inputUserName().sendKeys(existUserName);
        createTokenForm.inputUserName().sendKeys(Keys.RETURN);

        createTokenForm.inputDescription().sendKeys(description);
        createTokenForm.buttonSubmit().click();
        return this;
    }

    public TokenManagementPage copyToken(String existUserName) {
        waitForPageLoading();

        tokenList().stream()
                .filter(it -> it.getText().contains(existUserName))
                .flatMap(
                        it -> it.findElements(By.xpath("//button[contains(@tooltip,'Copy Token')]")).stream())
                .filter(WebElement::isDisplayed)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No Copy button in token list"))
                .click();

        return this;
    }

    public TokenManagementPage deleteToken(String existUserName) {
        waitForPageLoading();

        tokenList().stream()
                .filter(it -> it.getText().contains(existUserName))
                .flatMap(
                        it -> it.findElements(By.xpath("//button[contains(@tooltip,'Delete Token')]")).stream())
                .filter(WebElement::isDisplayed)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No delete button in token list"))
                .click();

        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.elementToBeClickable(deleteConfirmButton));
        deleteConfirmButton.click();

        return this;
    }

    private void waitForPageLoading() {
        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.urlContains("/system/token"));
    }

    @Getter
    public class CreateTokenForm {

        CreateTokenForm() {
            PageFactory.initElements(driver, this);
        }

        @FindBy(id = "form_item_userId")
        private WebElement inputUserName;

        @FindBy(id = "form_item_description")
        private WebElement inputDescription;

        @FindBy(xpath = "//button[contains(@class, 'ant-btn')]//span[contains(., 'Submit')]")
        private WebElement buttonSubmit;

        @FindBy(xpath = "//button[contains(@class, 'ant-btn')]//span[contains(., 'Cancel')]")
        private WebElement buttonCancel;
    }
}
