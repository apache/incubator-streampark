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

package org.apache.streampark.e2e.pages.system;

import org.apache.streampark.e2e.pages.common.Constants;
import org.apache.streampark.e2e.pages.common.NavBarPage;
import org.apache.streampark.e2e.pages.system.entity.UserManagementStatus;
import org.apache.streampark.e2e.pages.system.entity.UserManagementUserType;

import lombok.Getter;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.FindBys;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;

@Getter
public class UserManagementPage extends NavBarPage implements SystemPage.Tab {

    @FindBy(id = "e2e-user-create-btn")
    public WebElement buttonCreateUser;

    @FindBy(className = "ant-table-tbody")
    public List<WebElement> userList;

    @FindBy(className = "ant-form-item-explain-error")
    public List<WebElement> errorMessageList;

    public final CreateUserForm createUserForm = new CreateUserForm();

    public UserManagementPage(RemoteWebDriver driver) {
        super(driver);
    }

    public UserManagementPage createUser(
                                         String userName,
                                         String nickName,
                                         String password,
                                         String email,
                                         UserManagementUserType userManagementUserType) {
        waitForPageLoading();

        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.elementToBeClickable(buttonCreateUser));
        buttonCreateUser.click();
        createUserForm.inputUserName.sendKeys(userName);
        createUserForm.inputNickName.sendKeys(nickName);
        createUserForm.inputPassword.sendKeys(password);
        createUserForm.inputEmail.sendKeys(email);

        createUserForm.btnSelectUserTypeDropdown.click();
        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.visibilityOfAllElements(createUserForm.selectUserType));
        createUserForm.selectUserType.stream()
            .filter(e -> e.getText().equalsIgnoreCase(String.valueOf(userManagementUserType)))
            .findFirst()
            .orElseThrow(
                () -> new RuntimeException(
                    String.format("No %s in userType dropdown list",
                        userManagementUserType)))
            .click();

        createUserForm.buttonSubmit.click();

        return this;
    }

    public UserManagementPage editUser(
                                       String userName,
                                       String email,
                                       UserManagementUserType userManagementUserType,
                                       UserManagementStatus userManagementStatus) {
        waitForPageLoading();

        userList
            .stream()
            .filter(it -> it.getText().contains(userName))
            .flatMap(
                it -> it.findElements(By.className("e2e-user-edit-btn"))
                    .stream())
            .filter(WebElement::isDisplayed)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No edit button in user list"))
            .click();

        createUserForm.inputEmail.sendKeys(Keys.CONTROL + "a");
        createUserForm.inputEmail.sendKeys(Keys.BACK_SPACE);
        createUserForm.inputEmail.sendKeys(email);

        createUserForm.btnSelectUserTypeDropdown.click();
        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.visibilityOfAllElements(createUserForm.selectUserType));
        createUserForm.selectUserType.stream()
            .filter(e -> e.getText().equalsIgnoreCase(String.valueOf(userManagementUserType)))
            .findFirst()
            .orElseThrow(
                () -> new RuntimeException(
                    String.format("No %s in userType dropdown list",
                        userManagementUserType)))
            .click();

        switch (userManagementStatus) {
            case LOCKED:
                createUserForm.radioLocked.click();
                break;
            case EFFECTIVE:
                createUserForm.radioEffective.click();
                break;
            default:
                throw new RuntimeException("Unknown user management status");
        }

        createUserForm.buttonSubmit.click();
        return this;
    }

    private void waitForPageLoading() {
        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.urlContains("/system/user"));
    }

    @Getter
    public class CreateUserForm {

        CreateUserForm() {
            PageFactory.initElements(driver, this);
        }

        @FindBy(id = "formUserName")
        public WebElement inputUserName;

        @FindBy(id = "form_item_nickName")
        public WebElement inputNickName;

        @FindBy(id = "form_item_password")
        public WebElement inputPassword;

        @FindBy(id = "form_item_email")
        public WebElement inputEmail;

        @FindBys({
                @FindBy(css = "[codefield=userType]"),
                @FindBy(className = "ant-select-item-option-content")
        })
        public List<WebElement> selectUserType;

        @FindBy(css = "[codefield=userType] > .ant-select-selector")
        public WebElement btnSelectUserTypeDropdown;

        @FindBy(xpath = "//label[contains(@class, 'ant-radio-wrapper')]/span[contains(., 'lock')]")
        public WebElement radioLocked;

        @FindBy(xpath = "//label[contains(@class, 'ant-radio-wrapper')]/span[contains(., 'effective')]")
        public WebElement radioEffective;

        @FindBy(className = "e2e-user-submit-btn")
        public WebElement buttonSubmit;

        @FindBy(className = "e2e-user-cancel-btn")
        public WebElement buttonCancel;
    }
}
