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

import lombok.Getter;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.FindBys;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;

@Getter
public class AlertTypeDetailForm {

    public WebDriver driver;

    @FindBys({
            @FindBy(xpath = "//div[contains(@class, 'ant-select-selector')]"),
            @FindBy(xpath = ".//input[@id='form_item_alertType']")
    })
    public WebElement btnAlertTypeDropdown;

    @FindBy(xpath = "//*[@id='form_item_alertType']//following::div[@class='ant-select-item-option-content']")
    public List<WebElement> selectAlertType;

    public AlertTypeDetailForm(WebDriver driver) {
        PageFactory.initElements(driver, this);
        this.driver = driver;
    }

    @SuppressWarnings("unchecked")
    public <T> T addAlertType(AlertTypeEnum alertTypeEnum) {
        btnAlertTypeDropdown.click();
        switch (alertTypeEnum) {
            case EMAIL:
                selectByAlertType(alertTypeEnum.desc);
                return (T) new EmailAlertForm(this);
            case DINGTALK:
                selectByAlertType(alertTypeEnum.desc);
                return (T) new DingTalkAlertForm(this);
            case WECHAT:
                selectByAlertType(alertTypeEnum.desc);
                return (T) new WeChatAlertForm(this);
            case SMS:
                // ignore.
            case LARK:
                selectByAlertType(alertTypeEnum.desc);
                return (T) new LarkAlertForm(this);
            default:
                throw new UnsupportedOperationException(
                    String.format("Unsupported alert type %s", alertTypeEnum.desc));
        }
    }

    private void selectByAlertType(String alertType) {
        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.visibilityOfAllElements(selectAlertType));
        selectAlertType.stream()
            .filter(e -> e.getText().equals(alertType))
            .findFirst()
            .orElseThrow(
                () -> new RuntimeException(
                    String.format("No %s in alertType dropdown list", alertType)))
            .click();
    }

    @Getter
    public enum AlertTypeEnum {

        EMAIL("E-mail"),
        DINGTALK("Ding Talk"),
        WECHAT("WeChat"),
        LARK("Lark"),
        SMS("SMS");
        public final String desc;

        AlertTypeEnum(String desc) {
            this.desc = desc;
        }
    }
}
