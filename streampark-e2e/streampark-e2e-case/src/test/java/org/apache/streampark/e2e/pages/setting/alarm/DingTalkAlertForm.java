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

import lombok.Getter;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static org.apache.streampark.e2e.pages.common.CommonFactory.WebElementDeleteAndInput;

@Getter
public class DingTalkAlertForm extends CommonForm {

    private WebDriver driver;

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

    public DingTalkAlertForm(AlertTypeDetailForm alertTypeDetailForm) {
        super(alertTypeDetailForm);

        this.driver = alertTypeDetailForm.driver();
    }

    public DingTalkAlertForm url(String url) {
        WebElementDeleteAndInput(inputDingTalkURL, url);
        return this;
    }

    public DingTalkAlertForm token(String token) {
        WebElementDeleteAndInput(inputDingTalkToken, token);
        return this;
    }

    public DingTalkAlertForm secretToken(String secretToken) {
        WebElementDeleteAndInput(inputDingTalkSecretToken, secretToken);
        return this;
    }

    public DingTalkAlertForm receiveUser(String receiveUser) {
        WebElementDeleteAndInput(inputDingTalkReceiveUser, receiveUser);
        return this;
    }

    public DingTalkAlertForm effectToAllUsers() {
        btnDingTalkEffectToAllUsers.click();
        return this;
    }

    public DingTalkAlertForm secretEnable() {
        btnDingTalkSecret.click();
        return this;
    }
}
