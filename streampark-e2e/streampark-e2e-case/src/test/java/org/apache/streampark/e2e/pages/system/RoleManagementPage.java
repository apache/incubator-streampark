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
import java.util.Objects;

@Getter
public class RoleManagementPage extends NavBarPage implements SystemPage.Tab {

    @FindBy(id = "e2e-role-create-btn")
    public WebElement buttonCreateRole;

    @FindBy(className = "ant-table-tbody")
    public List<WebElement> roleList;

    @FindBy(className = "ant-form-item-explain-error")
    public List<WebElement> errorMessageList;

    @FindBy(className = "e2e-role-delete-confirm")
    public WebElement deleteConfirmButton;

    public final CreateRoleForm createRoleForm = new CreateRoleForm();

    public RoleManagementPage(RemoteWebDriver driver) {
        super(driver);
    }

    public RoleManagementPage createRole(String roleName, String description,
                                         String menuName) {
        waitForPageLoading();

        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.elementToBeClickable(buttonCreateRole));

        buttonCreateRole.click();

        createRoleForm.inputRoleName.sendKeys(roleName);
        createRoleForm.inputDescription.sendKeys(description);
        editRoleMenu(menuName);

        createRoleForm.buttonSubmit.click();
        return this;
    }

    public RoleManagementPage editRole(String roleName, String description,
                                       String menuName) {
        waitForPageLoading();

        roleList.stream()
            .filter(it -> it.getText().contains(roleName))
            .flatMap(
                it -> it.findElements(By.className("e2e-role-edit-btn"))
                    .stream())
            .filter(WebElement::isDisplayed)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No edit button in role list"))
            .click();

        createRoleForm.inputDescription.sendKeys(description);
        editRoleMenu(menuName);

        createRoleForm.buttonSubmit.click();
        return this;
    }

    public RoleManagementPage deleteRole(String roleName) {

        waitForPageLoading();

        roleList.stream()
            .filter(it -> it.getText().contains(roleName))
            .flatMap(
                it -> it.findElements(By.className("e2e-role-delete-btn"))
                    .stream())
            .filter(WebElement::isDisplayed)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No delete button in role list"))
            .click();

        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.elementToBeClickable(deleteConfirmButton));

        deleteConfirmButton.click();
        return this;
    }

    private void waitForPageLoading() {
        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.urlContains("/system/role"));
    }

    private RoleManagementPage editRoleMenu(String menuName) {
        createRoleForm.inputMenus.stream()
            .filter(e -> Objects.equals(
                e.findElement(By.xpath(
                    ".//span[contains(@class, 'streampark-tree__title') and contains(@class, 'pl-2')]"))
                    .getText(),
                menuName))
            .findFirst()
            .orElseThrow(
                () -> new RuntimeException(
                    String.format("No %s in menus checkbox tree", menuName)))
            .findElement(By.className("ant-tree-checkbox-inner"))
            .click();
        return this;
    }

    @Getter
    public class CreateRoleForm {

        CreateRoleForm() {
            PageFactory.initElements(driver, this);
        }

        @FindBy(id = "role_form_roleName")
        public WebElement inputRoleName;

        @FindBy(id = "role_form_description")
        public WebElement inputDescription;

        @FindBys({
                @FindBy(className = "ant-tree-list"),
                @FindBy(className = "ant-tree-treenode")
        })
        public List<WebElement> inputMenus;

        @FindBy(className = "e2e-role-pop-ok")
        public WebElement buttonSubmit;

        @FindBy(className = "e2e-role-pop-cancel")
        public WebElement buttonCancel;
    }
}
