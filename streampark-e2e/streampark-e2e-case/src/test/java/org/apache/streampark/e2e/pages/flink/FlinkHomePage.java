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
package org.apache.streampark.e2e.pages.flink;

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
public class FlinkHomePage extends NavBarPage implements ApacheFlinkPage.Tab {
  @FindBy(
      xpath =
          "//span[contains(., 'Flink Home')]/..//button[contains(@class, 'ant-btn')]/span[contains(text(), 'Add New')]")
  private WebElement buttonCreateFlinkHome;

  @FindBy(xpath = "//div[contains(@class, 'ant-spin-container')]")
  private List<WebElement> flinkHomeList;

  @FindBy(xpath = "//button[contains(@class, 'ant-btn')]/span[contains(., 'Yes')]")
  private WebElement deleteConfirmButton;

  private final CreateFlinkHomeForm createFlinkHomeForm = new CreateFlinkHomeForm();

  public FlinkHomePage(RemoteWebDriver driver) {
    super(driver);
  }

  public FlinkHomePage createFlinkHome(String flinkName, String flinkHome, String description) {
    waitForPageLoading();

    new WebDriverWait(driver, Duration.ofSeconds(10))
        .until(ExpectedConditions.elementToBeClickable(buttonCreateFlinkHome));
    buttonCreateFlinkHome.click();
    createFlinkHomeForm.inputFlinkName().sendKeys(flinkName);
    createFlinkHomeForm.inputFlinkHome().sendKeys(flinkHome);
    createFlinkHomeForm.inputDescription().sendKeys(description);
    createFlinkHomeForm.buttonSubmit().click();

    waitForClickFinish("create successful");
    return this;
  }

  public FlinkHomePage editFlinkHome(String oldFlinkName, String newFlinkName) {
    waitForPageLoading();

    flinkHomeList().stream()
        .filter(it -> it.getText().contains(oldFlinkName))
        .flatMap(
            it ->
                it
                    .findElements(
                        By.xpath(
                            "//button[contains(@class,'ant-btn')]/span[contains(@aria-label,'edit')]"))
                    .stream())
        .filter(WebElement::isDisplayed)
        .findFirst()
        .orElseThrow(() -> new RuntimeException("No edit button in flink home list"))
        .click();

    createFlinkHomeForm.inputFlinkName().sendKeys(Keys.CONTROL + "a");
    createFlinkHomeForm.inputFlinkName().sendKeys(Keys.BACK_SPACE);
    createFlinkHomeForm.inputFlinkName().sendKeys(newFlinkName);
    createFlinkHomeForm.buttonSubmit().click();

    waitForClickFinish("update successful");
    return this;
  }

  public FlinkHomePage deleteFlinkHome(String flinkName) {
    waitForPageLoading();

    flinkHomeList().stream()
        .filter(it -> it.getText().contains(flinkName))
        .flatMap(
            it ->
                it
                    .findElements(
                        By.xpath(
                            "//button[contains(@class,'ant-btn')]/span[contains(@aria-label,'delete')]"))
                    .stream())
        .filter(WebElement::isDisplayed)
        .findFirst()
        .orElseThrow(() -> new RuntimeException("No delete button in flink home list"))
        .click();

    deleteConfirmButton.click();

    waitForClickFinish("flink home is removed");
    return this;
  }

  private void waitForPageLoading() {
    new WebDriverWait(driver, Duration.ofSeconds(10))
        .until(ExpectedConditions.urlContains("/flink/home"));
  }

  private void waitForClickFinish(String message) {
    new WebDriverWait(driver, Duration.ofSeconds(10))
        .until(
            ExpectedConditions.visibilityOfElementLocated(
                By.xpath(String.format("//*[contains(text(),'%s')]", message))));
    new WebDriverWait(driver, Duration.ofSeconds(10))
        .until(
            ExpectedConditions.invisibilityOfElementLocated(
                By.xpath(String.format("//*[contains(text(),'%s')]", message))));
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
