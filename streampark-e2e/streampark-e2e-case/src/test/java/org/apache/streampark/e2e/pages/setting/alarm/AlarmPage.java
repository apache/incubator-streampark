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

package org.apache.streampark.e2e.pages.setting.alarm;

import org.apache.streampark.e2e.pages.common.Constants;
import org.apache.streampark.e2e.pages.common.NavBarPage;
import org.apache.streampark.e2e.pages.setting.SettingPage;

import lombok.Getter;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;

@Getter
public class AlarmPage extends NavBarPage implements SettingPage.Tab {

    @FindBy(xpath = "//span[contains(., 'Alarms Setting')]/..//button[contains(@class, 'ant-btn-dashed')]/span[contains(text(), 'Add New')]")
    public WebElement buttonCreateAlarm;

    @FindBy(xpath = "//div[@class='ant-row']")
    public List<WebElement> alarmList;

    @FindBy(className = "ant-form-item-explain-error")
    public List<WebElement> errorMessageList;

    @FindBy(xpath = "//button[contains(text(), 'Submit')]")
    public WebElement errorMessageConfirmButton;

    @FindBy(xpath = "//button[contains(@class, 'ant-btn')]/span[contains(., 'Yes')]")
    public WebElement deleteConfirmButton;

    public AlarmPage(RemoteWebDriver driver) {
        super(driver);
    }

    public AlertTypeDetailForm createAlarm() {
        waitForPageLoading();

        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.elementToBeClickable(buttonCreateAlarm));
        buttonCreateAlarm.click();

        return new AlertTypeDetailForm(driver);
    }

    public AlertTypeDetailForm editAlarm(String alarmName) {
        waitForPageLoading();

        alarmList.stream()
            // Filter out cards containing a specific alertName.
            .filter(card -> {
                WebElement titleElement = new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
                    .until(ExpectedConditions.visibilityOf(card
                        .findElement(By.xpath(".//div[@class='ant-card-head']//div[@class='ant-card-head-title']"))));
                return titleElement.getText().contains(alarmName);
            })
            // Find the eligible cards and click the edit button.
            .flatMap(card -> {
                List<WebElement> editButtons = card.findElements(By.xpath(
                    ".//button[.//span[contains(@class, 'anticon') and contains(@class, 'anticon-edit')]]"));

                // Make sure the button is loaded.
                new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
                    .until(ExpectedConditions.visibilityOfAllElements(editButtons));

                return editButtons.stream();
            })
            .filter(WebElement::isDisplayed)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No edit button found for alarm: " + alarmName))
            .click();

        return new AlertTypeDetailForm(driver);
    }

    public AlarmPage deleteAlarm(String alarmName) {
        waitForPageLoading();

        alarmList.stream()
            // Filter out cards containing a specific alertName.
            .filter(card -> {
                WebElement titleElement = new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
                    .until(ExpectedConditions.visibilityOf(card
                        .findElement(By.xpath(".//div[@class='ant-card-head']//div[@class='ant-card-head-title']"))));
                return titleElement.getText().contains(alarmName);
            })
            // Find the eligible cards and click the delete button.
            .flatMap(card -> {
                List<WebElement> deleteButtons = card.findElements(By.xpath(
                    ".//button[.//span[contains(@class, 'anticon') and contains(@class, 'anticon-delete')]]"));

                // Make sure the button is loaded.
                new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
                    .until(ExpectedConditions.visibilityOfAllElements(deleteButtons));

                return deleteButtons.stream();
            })
            .filter(WebElement::isDisplayed)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No delete button found for alarm: " + alarmName))
            .click();

        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.elementToBeClickable(deleteConfirmButton));

        deleteConfirmButton.click();
        return this;
    }

    private void waitForPageLoading() {
        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.urlContains("/setting/alarm"));
    }

}
