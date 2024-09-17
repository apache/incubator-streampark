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
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;

@Getter
public class TeamManagementPage extends NavBarPage implements SystemPage.Tab {

    @FindBy(id = "e2e-team-create-btn")
    public WebElement buttonCreateTeam;

    @FindBy(className = "ant-table-tbody")
    public List<WebElement> teamList;

    @FindBy(className = "swal2-html-container")
    public List<WebElement> errorMessageList;

    @FindBy(xpath = "//button[contains(text(), 'OK')]")
    public WebElement errorMessageConfirmButton;

    @FindBy(xpath = "//button[contains(@class, 'ant-btn')]/span[contains(., 'OK')]")
    public WebElement deleteConfirmButton;

    public final CreateTeamForm createTeamForm = new CreateTeamForm();

    public TeamManagementPage(RemoteWebDriver driver) {
        super(driver);
    }

    public void createTeam(String teamName, String description) {
        waitForPageLoading();

        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.elementToBeClickable(buttonCreateTeam));
        buttonCreateTeam.click();
        createTeamForm.inputTeamName.sendKeys(teamName);
        createTeamForm.inputDescription.sendKeys(description);

        createTeamForm.buttonSubmit.click();
    }

    public TeamManagementPage editTeam(String teamName, String description) {
        waitForPageLoading();

        teamList.stream()
            .filter(it -> it.getText().contains(teamName))
            .flatMap(
                it -> it.findElements(By.className("e2e-team-edit-btn"))
                    .stream())
            .filter(WebElement::isDisplayed)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No edit button in team list"))
            .click();

        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.elementToBeClickable(createTeamForm.buttonSubmit));
        createTeamForm.inputDescription.sendKeys(Keys.CONTROL + "a");
        createTeamForm.inputDescription.sendKeys(Keys.BACK_SPACE);
        createTeamForm.inputDescription.sendKeys(description);

        createTeamForm.buttonSubmit.click();

        return this;
    }

    public TeamManagementPage deleteTeam(String teamName) {
        waitForPageLoading();

        teamList.stream()
            .filter(it -> it.getText().contains(teamName))
            .flatMap(
                it -> it.findElements(By.className("e2e-team-delete-btn"))
                    .stream())
            .filter(WebElement::isDisplayed)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No delete button in team list"))
            .click();

        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.elementToBeClickable(deleteConfirmButton));

        deleteConfirmButton.click();
        return this;
    }

    private void waitForPageLoading() {
        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.urlContains("/system/team"));
    }

    @Getter
    public class CreateTeamForm {

        CreateTeamForm() {
            PageFactory.initElements(driver, this);
        }

        @FindBy(id = "TeamEditForm_teamName")
        public WebElement inputTeamName;

        @FindBy(id = "TeamEditForm_description")
        public WebElement inputDescription;

        @FindBy(className = "e2e-team-submit-btn")
        public WebElement buttonSubmit;

        @FindBy(className = "e2e-team-cancel-btn")
        public WebElement buttonCancel;
    }
}
