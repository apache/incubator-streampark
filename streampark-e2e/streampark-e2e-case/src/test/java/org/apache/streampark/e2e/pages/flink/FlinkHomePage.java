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

package org.apache.streampark.e2e.pages.flink;

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
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Getter
public class FlinkHomePage extends NavBarPage implements ApacheFlinkPage.Tab {

    @FindBy(id = "e2e-env-add-btn")
    public WebElement buttonCreateFlinkHome;

    @FindBy(className = "ant-table-tbody")
    public List<WebElement> flinkHomeList;

    @FindBy(className = "e2e-flinkenv-delete-confirm")
    public WebElement deleteConfirmButton;

    public final CreateFlinkHomeForm createFlinkHomeForm = new CreateFlinkHomeForm();

    public FlinkHomePage(RemoteWebDriver driver) {
        super(driver);
    }

    public FlinkHomePage createFlinkHome(String flinkName, String flinkHome, String description) {
        waitForPageLoading();

        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.elementToBeClickable(buttonCreateFlinkHome));

        buttonCreateFlinkHome.click();

        createFlinkHomeForm.inputFlinkName.sendKeys(flinkName);
        createFlinkHomeForm.inputFlinkHome.sendKeys(flinkHome);
        createFlinkHomeForm.inputDescription.sendKeys(description);
        createFlinkHomeForm.buttonSubmit.click();

        Awaitility.await()
            .untilAsserted(
                () -> assertThat(flinkHomeList)
                    .as("FlinkEnv list should contain newly-created env")
                    .extracting(WebElement::getText)
                    .anyMatch(it -> it.contains(flinkName)));

        return this;
    }

    public FlinkHomePage editFlinkHome(String oldFlinkName, String newFlinkName) {
        waitForPageLoading();

        flinkHomeList.stream()
            .filter(it -> it.getText().contains(oldFlinkName))
            .flatMap(
                it -> it
                    .findElements(By.className("e2e-flinkenv-edit-btn"))
                    .stream())
            .filter(WebElement::isDisplayed)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No edit button in flink home list"))
            .click();

        createFlinkHomeForm.inputFlinkName.sendKeys(Keys.CONTROL + "a");
        createFlinkHomeForm.inputFlinkName.sendKeys(Keys.BACK_SPACE);
        createFlinkHomeForm.inputFlinkName.sendKeys(newFlinkName);
        createFlinkHomeForm.buttonSubmit.click();

        return this;
    }

    public FlinkHomePage deleteFlinkHome(String flinkName) {
        waitForPageLoading();

        flinkHomeList.stream()
            .filter(it -> it.getText().contains(flinkName))
            .flatMap(
                it -> it
                    .findElements(
                        By.className(
                            "e2e-flinkenv-delete-btn"))
                    .stream())
            .filter(WebElement::isDisplayed)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No delete button in flink home list"))
            .click();

        deleteConfirmButton.click();

        return this;
    }

    private void waitForPageLoading() {
        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.urlContains("/flink/home"));
    }

    @Getter
    public class CreateFlinkHomeForm {

        CreateFlinkHomeForm() {
            PageFactory.initElements(driver, this);
        }

        @FindBy(id = "flink_env_flinkName")
        public WebElement inputFlinkName;

        @FindBy(id = "flink_env_flinkHome")
        public WebElement inputFlinkHome;

        @FindBy(id = "flink_env_description")
        public WebElement inputDescription;

        @FindBy(id = "e2e-flinkenv-submit-btn")
        public WebElement buttonSubmit;
    }
}
