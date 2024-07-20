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

package org.apache.streampark.e2e.pages.setting;

import org.apache.streampark.e2e.pages.common.Constants;
import org.apache.streampark.e2e.pages.common.NavBarPage;
import org.apache.streampark.e2e.pages.setting.entity.AlarmPageDingTalk;
import org.apache.streampark.e2e.pages.setting.entity.AlarmPageEmail;
import org.apache.streampark.e2e.pages.setting.entity.AlarmPageLark;
import org.apache.streampark.e2e.pages.setting.entity.AlarmPageSMS;
import org.apache.streampark.e2e.pages.setting.entity.AlarmPageWeChat;
import org.apache.streampark.e2e.pages.setting.entity.FaultAlert;

import lombok.Getter;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.FindBys;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import javax.annotation.Nonnull;

import java.util.List;

@Getter
public class AlarmPage extends NavBarPage implements SettingPage.Tab {

    public AlarmPage(RemoteWebDriver driver) {
        super(driver);
    }

    private final CreateAlarmForm createAlarmForm = new CreateAlarmForm();

    @FindBy(xpath = "//span[contains(., 'Alarms Setting')]/..//button[contains(@class, 'ant-btn-dashed')]/span[contains(text(), 'Add New')]")
    private WebElement buttonCreateAlarm;

    @FindBy(xpath = "//div[@class='ant-row']")
    private List<WebElement> alarmList;

    @FindBy(className = "ant-form-item-explain-error")
    private List<WebElement> errorMessageList;

    @FindBy(xpath = "//button[contains(text(), 'Submit')]")
    private WebElement errorMessageConfirmButton;

    @FindBy(xpath = "//button[contains(@class, 'ant-btn')]/span[contains(., 'Yes')]")
    private WebElement deleteConfirmButton;

    public AlarmPage createAlarm(String alertName, String alertType, FaultAlert faultAlert) {
        waitForPageLoading();

        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.elementToBeClickable(buttonCreateAlarm));

        buttonCreateAlarm.click();
        createAlarmForm.inputAlertName().sendKeys(alertName);

        createFaultAlert(alertType, faultAlert);
        createAlarmForm.buttonSubmit().click();

        return this;
    }

    public AlarmPage editAlarm(String alertName, String alertType, FaultAlert faultAlert) {
        waitForPageLoading();

        alarmList().stream()
            // Filter out cards containing a specific alertName.
            .filter(card -> {
                WebElement titleElement = new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
                    .until(ExpectedConditions.visibilityOf(card
                        .findElement(By.xpath(".//div[@class='ant-card-head']//div[@class='ant-card-head-title']"))));
                return titleElement.getText().contains(alertName);
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
            .orElseThrow(() -> new RuntimeException("No edit button found for alarm: " + alertName))
            .click();

        // todo: edit alert type
        // createFaultAlert(alertType, faultAlert);
        createAlarmForm.buttonSubmit().click();
        return this;
    }

    public AlarmPage deleteAlarm(String alertName) {
        waitForPageLoading();

        alarmList().stream()
            // Filter out cards containing a specific alertName.
            .filter(card -> {
                WebElement titleElement = new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
                    .until(ExpectedConditions.visibilityOf(card
                        .findElement(By.xpath(".//div[@class='ant-card-head']//div[@class='ant-card-head-title']"))));
                return titleElement.getText().contains(alertName);
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
            .orElseThrow(() -> new RuntimeException("No delete button found for alarm: " + alertName))
            .click();

        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.elementToBeClickable(deleteConfirmButton));

        deleteConfirmButton.click();
        return this;
    }

    // create one fault alert resource in our system.
    private void createFaultAlert(String alertType, FaultAlert faultAlert) {
        createAlarmForm.btnAlertTypeDropdown().click();
        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.visibilityOfAllElements(createAlarmForm.selectAlertType()));
        createAlarmForm.selectAlertType().stream()
            .filter(e -> e.getText().equals(alertType))
            .findFirst()
            .orElseThrow(
                () -> new RuntimeException(
                    String.format("No %s in alertType dropdown list", alertType)))
            .click();

        AlertService alertService = getAlertService(faultAlert);
        alertService.createAlert(faultAlert);
    }

    private @Nonnull AlertService getAlertService(@Nonnull FaultAlert faultAlert) {
        AlertService alertService = null;
        if (faultAlert instanceof AlarmPageEmail) {
            alertService = new CreateEmailAlertForm();
        } else if (faultAlert instanceof AlarmPageDingTalk) {
            alertService = new CreateDingTalkAlertForm();
        } else if (faultAlert instanceof AlarmPageLark) {
            alertService = new CreateLarkAlertForm();
        } else if (faultAlert instanceof AlarmPageWeChat) {
            alertService = new CreateWeChatAlertForm();
        } else if (faultAlert instanceof AlarmPageSMS) {
            // ignore
            alertService = new CreateSMSAlertForm();
        }

        return alertService;
    }

    private void waitForPageLoading() {
        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.urlContains("/setting/alarm"));
    }

    @Getter
    public class CreateAlarmForm {

        CreateAlarmForm() {
            PageFactory.initElements(driver, this);
        }

        @FindBy(id = "form_item_alertName")
        private WebElement inputAlertName;

        @FindBys({
                @FindBy(xpath = "//span[text()='Fault Alert Type']"),
                @FindBy(xpath = "./ancestor::div[contains(@class, 'ant-select-selector')]")
        })
        private WebElement btnAlertTypeDropdown;

        @FindBy(xpath = "//*[@id='form_item_alertType']//following::div[@class='ant-select-item-option-content']")
        private List<WebElement> selectAlertType;

        @FindBy(xpath = "//button[contains(@class, 'ant-btn')]//span[contains(text(), 'Submit')]")
        private WebElement buttonSubmit;

        @FindBy(xpath = "//button[contains(@class, 'ant-btn')]//span[contains(text(), 'Cancel')]")
        private WebElement buttonCancel;
    }

    private class CreateEmailAlertForm implements AlertService {

        CreateEmailAlertForm() {
            PageFactory.initElements(driver, this);
        }

        @FindBy(id = "form_item_alertEmail")
        private WebElement inputEmail;

        @Override
        public void createAlert(FaultAlert faultAlert) {
            AlarmPageEmail alarmPageEmail = (AlarmPageEmail) faultAlert;
            inputEmail.sendKeys(alarmPageEmail.email());
        }
    }

    private class CreateDingTalkAlertForm implements AlertService {

        CreateDingTalkAlertForm() {
            PageFactory.initElements(driver, this);
        }

        @FindBy(id = "form_item_alertDingURL")
        private WebElement inputDingTalkURL;

        @FindBy(id = "form_item_dingtalkToken")
        private WebElement inputDingTalkToken;

        @FindBy(id = "form_item_dingtalkSecretEnable")
        private WebElement btnDingTalkSecret;

        @FindBy(id = "form_item_dingtalkSecretToken")
        private WebElement inputDingTalkSecretToken;

        @FindBy(id = "form_item_alertDingUser")
        private WebElement inputDingTalkReceiveUser;

        @FindBy(id = "form_item_dingtalkIsAtAll")
        private WebElement btnDingTalkEffectToAllUsers;

        @Override
        public void createAlert(FaultAlert faultAlert) {
            AlarmPageDingTalk alarmPageDingTalk = (AlarmPageDingTalk) faultAlert;
            inputDingTalkURL.sendKeys(alarmPageDingTalk.url());
            inputDingTalkToken.sendKeys(alarmPageDingTalk.token());
            inputDingTalkReceiveUser.sendKeys(alarmPageDingTalk.receiveUser());
            // if we open secret button, need to fill the token value.
            if (alarmPageDingTalk.isSecretEnable()) {
                btnDingTalkSecret.click();
                inputDingTalkSecretToken.sendKeys(alarmPageDingTalk.secretToken());
            }
            if (alarmPageDingTalk.isEffectToAllUsers()) {
                btnDingTalkEffectToAllUsers.click();
            }
        }
    }

    private class CreateWeChatAlertForm implements AlertService {

        CreateWeChatAlertForm() {
            PageFactory.initElements(driver, this);
        }

        @FindBy(id = "form_item_weToken")
        private WebElement inputWeChatToken;

        @Override
        public void createAlert(FaultAlert faultAlert) {
            AlarmPageWeChat alarmPageWeChat = (AlarmPageWeChat) faultAlert;
            inputWeChatToken.sendKeys(alarmPageWeChat.token());
        }
    }

    private class CreateLarkAlertForm implements AlertService {

        CreateLarkAlertForm() {
            PageFactory.initElements(driver, this);
        }

        @FindBy(id = "form_item_larkToken")
        private WebElement inputLarkToken;

        @FindBy(id = "form_item_larkIsAtAll")
        private WebElement btnLarkEffectToAllUsers;

        @FindBy(id = "form_item_larkSecretEnable")
        private WebElement btnLarkSecret;

        @FindBy(id = "form_item_larkSecretToken")
        private WebElement inputLarkSecretToken;

        @Override
        public void createAlert(FaultAlert faultAlert) {
            AlarmPageLark alarmPageLark = (AlarmPageLark) faultAlert;
            inputLarkToken.sendKeys(alarmPageLark.token());
            // if we open secret button, need to fill the token value.
            if (alarmPageLark.isSecretEnable()) {
                btnLarkSecret.click();
                inputLarkSecretToken.sendKeys(alarmPageLark.SecretToken());
            }
            if (alarmPageLark.isEffectToALlUsers()) {
                btnLarkEffectToAllUsers.click();
            }
        }
    }

    // not visible here.
    private class CreateSMSAlertForm implements AlertService {

        CreateSMSAlertForm() {
            PageFactory.initElements(driver, this);
        }

        @Override
        public void createAlert(FaultAlert faultAlert) {
        }
    }

    private interface AlertService {

        void createAlert(FaultAlert faultAlert);
    }
}
