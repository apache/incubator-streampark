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

import lombok.Getter;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.FindBys;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;

@Getter
public class MemberManagementPage extends NavBarPage implements SystemPage.Tab {

    @FindBy(id = "e2e-member-create-btn")
    public WebElement buttonCreateMember;

    @FindBy(className = "ant-table-tbody")
    public List<WebElement> memberList;

    @FindBy(id = "swal2-html-container")
    public WebElement errorMessage;

    @FindBy(className = "swal2-confirm")
    public WebElement errorMessageConfirmButton;

    @FindBy(className = "e2e-member-delete-confirm")
    public WebElement deleteConfirmButton;

    public final CreateMemberForm createMemberForm = new CreateMemberForm();

    public MemberManagementPage(RemoteWebDriver driver) {
        super(driver);
    }

    public MemberManagementPage createMember(String userName, String role) {

        waitForPageLoading();

        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.elementToBeClickable(buttonCreateMember));

        buttonCreateMember.click();

        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.elementToBeClickable(createMemberForm.btnSelectUserNameDropDown));

        createMemberForm.btnSelectUserNameDropDown.click();

        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.visibilityOfAllElements(createMemberForm.selectUserName));

        createMemberForm.selectUserName.stream()
            .filter(e -> e.getText().equals(userName))
            .findFirst()
            .orElseThrow(
                () -> new RuntimeException(
                    String.format("No %s in username dropdown list", userName)))
            .click();

        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.visibilityOfAllElements(createMemberForm.btnSelectRoleDropDown));

        createMemberForm.btnSelectRoleDropDown.click();

        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.visibilityOfAllElements(createMemberForm.selectRole));

        createMemberForm.selectRole.stream()
            .filter(e -> e.getText().equals(role))
            .findFirst()
            .orElseThrow(
                () -> new RuntimeException(
                    String.format("No %s in role dropdown list", role)))
            .click();

        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.visibilityOfAllElements(createMemberForm.buttonSubmit));

        createMemberForm.buttonSubmit.click();
        return this;
    }

    public MemberManagementPage editMember(String userName, String role) {
        waitForPageLoading();

        memberList.stream()
            .filter(it -> it.getText().contains(userName))
            .flatMap(
                it -> it.findElements(By.className("e2e-member-edit-btn"))
                    .stream())
            .filter(WebElement::isDisplayed)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No edit button in member list"))
            .click();

        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.visibilityOfAllElements(createMemberForm.btnSelectRoleDropDown));
        createMemberForm.btnSelectRoleDropDown.click();

        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.visibilityOfAllElements(createMemberForm.selectRole));

        createMemberForm.selectRole.stream()
            .filter(e -> e.getText().equals(role))
            .findFirst()
            .orElseThrow(
                () -> new RuntimeException(
                    String.format("No %s in role dropdown list", role)))
            .click();

        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.visibilityOfAllElements(createMemberForm.buttonSubmit));

        createMemberForm.buttonSubmit.click();
        return this;
    }

    public MemberManagementPage deleteMember(String userName) {
        waitForPageLoading();

        memberList.stream()
            .filter(it -> it.getText().contains(userName))
            .flatMap(
                it -> it.findElements(By.className("e2e-member-delete-btn"))
                    .stream())
            .filter(WebElement::isDisplayed)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No delete button in member list"))
            .click();

        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.elementToBeClickable(deleteConfirmButton));

        deleteConfirmButton.click();
        return this;
    }

    private void waitForPageLoading() {
        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.urlContains("/system/member"));
    }

    @Getter
    public class CreateMemberForm {

        CreateMemberForm() {
            PageFactory.initElements(driver, this);
        }

        @FindBy(css = "div[optionfiltergroup='username'][codefield='userName']")
        public WebElement btnSelectUserNameDropDown;

        @FindBys({
                @FindBy(css = "[codefield='userName']"),
                @FindBy(className = "ant-select-item-option-content")
        })
        public List<WebElement> selectUserName;

        @FindBy(css = "[codefield='roleId']")
        public WebElement btnSelectRoleDropDown;

        @FindBys({
                @FindBy(css = "[codefield='roleId']"),
                @FindBy(className = "ant-select-item-option-content")
        })
        public List<WebElement> selectRole;

        @FindBy(className = "e2e-member-submit-btn")
        public WebElement buttonSubmit;

        @FindBy(className = "e2e-member-cancel-btn")
        public WebElement buttonCancel;
    }
}
