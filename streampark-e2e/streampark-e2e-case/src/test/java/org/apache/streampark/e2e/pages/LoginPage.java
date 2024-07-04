/*
 * Licensed to Apache Software Foundation (ASF) under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Apache Software Foundation (ASF) licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.streampark.e2e.pages;

import org.apache.streampark.e2e.pages.common.NavBarPage;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public final class LoginPage extends NavBarPage {
  @FindBy(id = "form_item_account")
  private WebElement inputUsername;

  @FindBy(id = "form_item_password")
  private WebElement inputPassword;

  @FindBy(xpath = "//button[contains(@classnames, 'login-button')]")
  private WebElement buttonLogin;

  private final TeamForm teamForm = new TeamForm();

  public LoginPage(RemoteWebDriver driver) {
    super(driver);
  }

  @SneakyThrows
  public NavBarPage login(String username, String password, String teamName) {
    new WebDriverWait(driver, Duration.ofSeconds(10))
        .until(ExpectedConditions.elementToBeClickable(buttonLogin));

    inputUsername().sendKeys(username);
    inputPassword().sendKeys(password);
    buttonLogin().click();

    try {
      new WebDriverWait(driver, Duration.ofSeconds(10))
          .until(ExpectedConditions.visibilityOfAllElements(teamForm.btnSelectTeamDropdown));

      teamForm.btnSelectTeamDropdown.click();
      teamForm.selectTeam.stream()
          .filter(it -> it.getText().contains(teamName))
          .findFirst()
          .orElseThrow(
              () -> new RuntimeException(String.format("No %s in team dropdown list", teamName)))
          .click();
      teamForm.buttonSubmit.click();
    } catch (Exception e) {
      log.warn("No team selection required:", e);
    }

    new WebDriverWait(driver, Duration.ofSeconds(30))
        .until(ExpectedConditions.urlContains("/flink/app"));
    return new NavBarPage(driver);
  }

  @Getter
  public class TeamForm {
    TeamForm() {
      PageFactory.initElements(driver, this);
    }

    @FindBys({
      @FindBy(css = "[popupClassName=team-select-popup]"),
      @FindBy(className = "ant-select-item-option-content")
    })
    private List<WebElement> selectTeam;

    @FindBy(css = "[popupClassName=team-select-popup] > .ant-select-selector")
    private WebElement btnSelectTeamDropdown;

    @FindBy(xpath = "//button[contains(@class, 'ant-btn')]//span[contains(text(), 'OK')]")
    private WebElement buttonSubmit;

    @FindBy(xpath = "//button[contains(@class, 'ant-btn')]//span[contains(text(), 'Cancel')]")
    private WebElement buttonCancel;
  }
}
