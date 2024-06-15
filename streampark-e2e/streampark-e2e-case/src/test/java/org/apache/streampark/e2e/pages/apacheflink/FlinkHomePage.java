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
package org.apache.streampark.e2e.pages.apacheflink;

import lombok.Getter;
import org.apache.streampark.e2e.pages.common.NavBarPage;
import org.apache.streampark.e2e.pages.system.entity.UserManagementStatus;
import org.apache.streampark.e2e.pages.system.entity.UserManagementUserType;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.FindBys;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

@Getter
public class FlinkHomePage extends NavBarPage implements ApacheFlinkPage.Tab {
    @FindBy(xpath = "//span[contains(., 'Flink Home')]/..//button[contains(@class, 'ant-btn')]/span[contains(text(), 'Add New')]")
    private WebElement buttonCreateUser;

    @FindBy(xpath = "//tbody[contains(@class, 'ant-table-tbody')]")
    private List<WebElement> userList;

    @FindBy(className = "ant-form-item-explain-error")
    private List<WebElement> errorMessageList;

    private final CreateFlinkHomeForm createFlinkHomeForm = new CreateFlinkHomeForm();

    public FlinkHomePage(RemoteWebDriver driver) {
        super(driver);
    }

    public FlinkHomePage createUser(String userName, String nickName, String password, String email, UserManagementUserType userManagementUserType) {
        waitForPageLoading();

        new WebDriverWait(driver, Duration.ofSeconds(10)).until(ExpectedConditions.elementToBeClickable(buttonCreateUser));
        buttonCreateUser.click();
        createFlinkHomeForm.inputUserName().sendKeys(userName);
        createFlinkHomeForm.inputNickName().sendKeys(nickName);
        createFlinkHomeForm.inputPassword().sendKeys(password);
        createFlinkHomeForm.inputEmail().sendKeys(email);

        createFlinkHomeForm.btnSelectUserTypeDropdown().click();
        new WebDriverWait(driver, Duration.ofSeconds(10)).until(ExpectedConditions.visibilityOfAllElements(createFlinkHomeForm.selectUserType));
        createFlinkHomeForm.selectUserType
            .stream()
            .filter(e -> e.getText().equalsIgnoreCase(String.valueOf(userManagementUserType)))
            .findFirst()
            .orElseThrow(() -> new RuntimeException(String.format("No %s in userType dropdown list", userManagementUserType)))
            .click();

        createFlinkHomeForm.buttonSubmit().click();
        return this;
    }

    public FlinkHomePage editUser(String userName, String email, UserManagementUserType userManagementUserType, UserManagementStatus userManagementStatus) {
        waitForPageLoading();

        userList()
            .stream()
            .filter(it -> it.getText().contains(userName))
            .flatMap(it -> it.findElements(By.xpath("//button[contains(@tooltip,'modify user')]")).stream())
            .filter(WebElement::isDisplayed)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No edit button in user list"))
            .click();

        createFlinkHomeForm.inputEmail().sendKeys(Keys.CONTROL+"a");
        createFlinkHomeForm.inputEmail().sendKeys(Keys.BACK_SPACE);
        createFlinkHomeForm.inputEmail().sendKeys(email);

        createFlinkHomeForm.btnSelectUserTypeDropdown().click();
        new WebDriverWait(driver, Duration.ofSeconds(10)).until(ExpectedConditions.visibilityOfAllElements(createFlinkHomeForm.selectUserType));
        createFlinkHomeForm.selectUserType
            .stream()
            .filter(e -> e.getText().equalsIgnoreCase(String.valueOf(userManagementUserType)))
            .findFirst()
            .orElseThrow(() -> new RuntimeException(String.format("No %s in userType dropdown list", userManagementUserType)))
            .click();

        switch (userManagementStatus) {
            case LOCKED:
                createFlinkHomeForm.radioLocked.click();
                break;
            case EFFECTIVE:
                createFlinkHomeForm.radioEffective.click();
                break;
            default:
                throw new RuntimeException("Unknown user management status");
        }

        createFlinkHomeForm.buttonSubmit().click();

        return this;
    }

    private void waitForPageLoading() {
        new WebDriverWait(driver, Duration.ofSeconds(10)).until(ExpectedConditions.urlContains("/system/user"));
    }

    @Getter
    public class CreateFlinkHomeForm {
        CreateFlinkHomeForm() {
            PageFactory.initElements(driver, this);
        }

        @FindBy(id = "form_item_flinkName")
        private WebElement inputFlinkName;

        @FindBy(id = "form_item_flinkHome")
        private WebElement inputFlinkHome;

        @FindBy(id = "form_item_description")
        private WebElement inputDescription;

        @FindBy(xpath = "//button[contains(@class, 'ant-btn')]//span[contains(text(), 'OK')]")
        private WebElement buttonSubmit;

        @FindBy(xpath = "//button[contains(@class, 'ant-btn')]//span[contains(text(), 'Cancel')]")
        private WebElement buttonCancel;
    }
}
