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
import org.openqa.selenium.support.FindBys;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;

@Getter
public class ProjectsPage extends NavBarPage implements ResourcePage.Tab {

    @FindBy(id = "e2e-project-create-btn")
    private WebElement buttonCreateProject;

    @FindBy(xpath = "//div[contains(@class, 'ant-list')]")
    private List<WebElement> projectList;

    @FindBy(xpath = "//button[contains(@class, 'ant-btn')]/span[contains(., 'OK')]")
    private WebElement popupConfirmButton;

    private CreateProjectForm createProjectForm;

    public ProjectsPage(RemoteWebDriver driver) {
        super(driver);
        createProjectForm = new CreateProjectForm();
    }

    @SneakyThrows
    public ProjectsPage createProject(String projectName,
                                      String projectCvs,
                                      String projectUrl,
                                      String projectBranch,
                                      String projectBuildArgument,
                                      String projectDescription) {
        waitForPageLoading();

        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.elementToBeClickable(buttonCreateProject));

        buttonCreateProject.click();
        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.urlContains("/project/add"));

        createProjectForm.inputProjectName().sendKeys(projectName);
        createProjectForm.selectCveDropdown().click();
        createProjectForm.selectCveText().stream()
            .filter(e -> e.getText().equalsIgnoreCase(projectCvs))
            .findFirst()
            .orElseThrow(() -> new Exception(String.format("Cvs not found: %s", projectCvs)))
            .click();

        createProjectForm.inputProjectUrl().sendKeys(projectUrl);
        createProjectForm.selectBranchDropdown().click();
        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.visibilityOfAllElements(createProjectForm.selectBranchText()));
        createProjectForm.selectBranchText().stream()
            .filter(e -> e.getText().equalsIgnoreCase(projectBranch))
            .findFirst()
            .orElseThrow(() -> new Exception(String.format("Branch not found: %s", projectBranch)))
            .click();

        createProjectForm.inputBuildArgument().sendKeys(projectBuildArgument);
        createProjectForm.inputDescription().sendKeys(projectDescription);
        createProjectForm.buttonSubmit().click();

        return this;
    }

    @SneakyThrows
    public ProjectsPage editProject(String oldProjectName,
                                    String newProjectName,
                                    String projectCvs,
                                    String projectUrl,
                                    String projectBranch,
                                    String projectBuildArgument,
                                    String projectDescription) {
        waitForPageLoading();
        projectList().stream()
            .filter(project -> project.getText().contains(oldProjectName))
            .findFirst()
            .orElseThrow(() -> new Exception("Project edit button not found"))
            .findElement(
                By.xpath("//..//li[contains(@class, 'ant-list-item')]//button[contains(@class, 'ant-btn')][3]"))
            .click();
        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.urlContains("/project/edit"));

        createProjectForm.inputProjectName().sendKeys(Keys.CONTROL + "a");
        createProjectForm.inputProjectName().sendKeys(Keys.BACK_SPACE);
        createProjectForm.inputProjectName().sendKeys(newProjectName);
        createProjectForm.selectCveDropdown().click();
        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.visibilityOfAllElements(createProjectForm.selectCveText()));
        createProjectForm.selectCveText().stream()
            .filter(e -> e.getText().equalsIgnoreCase(projectCvs))
            .findFirst()
            .orElseThrow(() -> new Exception(String.format("Cvs not found: %s", projectCvs)))
            .click();

        createProjectForm.inputProjectUrl().sendKeys(Keys.CONTROL + "a");
        createProjectForm.inputProjectUrl().sendKeys(Keys.BACK_SPACE);
        createProjectForm.inputProjectUrl().sendKeys(projectUrl);
        createProjectForm.selectBranchDropdown().click();
        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.visibilityOfAllElements(createProjectForm.selectBranchText()));
        createProjectForm.selectBranchText().stream()
            .filter(e -> e.getText().equalsIgnoreCase(projectBranch))
            .findFirst()
            .orElseThrow(() -> new Exception(String.format("Branch not found: %s", projectBranch)))
            .click();

        createProjectForm.inputBuildArgument().sendKeys(Keys.CONTROL + "a");
        createProjectForm.inputBuildArgument().sendKeys(Keys.BACK_SPACE);
        createProjectForm.inputBuildArgument().sendKeys(projectBuildArgument);
        createProjectForm.inputDescription().sendKeys(Keys.CONTROL + "a");
        createProjectForm.inputDescription().sendKeys(Keys.BACK_SPACE);
        createProjectForm.inputDescription().sendKeys(projectDescription);
        createProjectForm.buttonSubmit().click();

        return this;
    }

    @SneakyThrows
    public ProjectsPage buildProject(String projectName) {
        waitForPageLoading();
        projectList().stream()
            .filter(project -> project.getText().contains(projectName))
            .findFirst()
            .orElseThrow(() -> new Exception("Project build button not found"))
            .findElement(
                By.xpath("//..//li[contains(@class, 'ant-list-item')]//button[contains(@class, 'ant-btn')][2]"))
            .click();
        popupConfirmButton.click();
        return this;
    }

    @SneakyThrows
    public ProjectsPage deleteProject(String projectName) {
        waitForPageLoading();
        projectList().stream()
            .filter(project -> project.getText().contains(projectName))
            .findFirst()
            .orElseThrow(() -> new Exception("Project delete button not found"))
            .findElement(
                By.xpath("//..//li[contains(@class, 'ant-list-item')]//button[contains(@class, 'ant-btn')][4]"))
            .click();
        popupConfirmButton.click();
        String deletePopUpMessage = "delete successful";
        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(
                ExpectedConditions.visibilityOfElementLocated(
                    By.xpath(String.format("//*[contains(text(),'%s')]",
                        deletePopUpMessage))));
        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(
                ExpectedConditions.invisibilityOfElementLocated(
                    By.xpath(String.format("//*[contains(text(),'%s')]",
                        deletePopUpMessage))));
        Thread.sleep(Constants.DEFAULT_SLEEP_MILLISECONDS);
        return this;
    }

    private void waitForPageLoading() {
        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.urlContains("/resource/project"));
    }

    @Getter
    private class CreateProjectForm {

        CreateProjectForm() {
            PageFactory.initElements(driver, this);
        }

        @FindBy(id = "form_item_name")
        private WebElement inputProjectName;

        @FindBy(xpath = "//div[contains(@codefield, 'repository')]//div[contains(@class, 'ant-select-selector')]")
        private WebElement selectCveDropdown;

        @FindBys({
                @FindBy(css = "[codefield=repository]"),
                @FindBy(className = "ant-select-item-option-content")
        })
        private List<WebElement> selectCveText;

        @FindBy(name = "url")
        private WebElement inputProjectUrl;

        @FindBy(xpath = "//div[contains(@codefield, 'branches')]//div[contains(@class, 'ant-select-selector')]")
        private WebElement selectBranchDropdown;

        @FindBys({
                @FindBy(css = "[codefield=branches]"),
                @FindBy(className = "ant-select-item-option-content")
        })
        private List<WebElement> selectBranchText;

        @FindBy(id = "form_item_buildArgs")
        private WebElement inputBuildArgument;

        @FindBy(id = "form_item_description")
        private WebElement inputDescription;

        @FindBy(xpath = "//button[contains(@class, 'ant-btn')]//span[contains(text(), 'Submit')]")
        private WebElement buttonSubmit;

        @FindBy(xpath = "//button[contains(@class, 'ant-btn')]//span[contains(text(), 'Cancel')]")
        private WebElement buttonCancel;
    }
}
