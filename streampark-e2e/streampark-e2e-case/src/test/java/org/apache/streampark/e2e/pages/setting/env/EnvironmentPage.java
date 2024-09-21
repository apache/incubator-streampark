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

package org.apache.streampark.e2e.pages.setting.env;

import org.apache.streampark.e2e.pages.common.Constants;
import org.apache.streampark.e2e.pages.common.NavBarPage;
import org.apache.streampark.e2e.pages.setting.SettingPage;

import lombok.Getter;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.FindBys;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.apache.streampark.e2e.pages.common.CommonFactory.WebElementClick;

@Getter
public class EnvironmentPage extends NavBarPage implements SettingPage.Tab {

    private static final Logger log = LoggerFactory.getLogger(EnvironmentPage.class);

    public EnvironmentPage(RemoteWebDriver driver) {
        super(driver);
    }

    @FindBys({
            @FindBy(xpath = "//h4[text()='Docker Setting']"),
            @FindBy(xpath = "./ancestor::li[@class='ant-list-item']//button[contains(@class, 'ant-btn') and contains(@class, 'ant-btn-primary') and contains(@class, 'ant-btn-circle')]")
    })
    public WebElement btnCreateDockerSetting;

    @FindBys({
            @FindBy(xpath = "//h4[text()='Alert Mailbox Setting']"),
            @FindBy(xpath = "./ancestor::li[@class='ant-list-item']//button[contains(@class, 'ant-btn') and contains(@class, 'ant-btn-primary') and contains(@class, 'ant-btn-circle')]")
    })
    public WebElement btnCreateEmailSetting;

    @FindBy(xpath = "//div[contains(@class, 'system-setting')]")
    public List<WebElement> settingList;

    @FindBy(className = "swal2-container")
    public List<WebElement> errorMessageList;

    @FindBy(xpath = "//button[contains(@class, 'swal2-confirm') and contains(@class, 'swal2-styled') and text()='OK']")
    public WebElement errorMessageConfirmButton;

    public EnvironmentDetailForm createEnvironment(EnvironmentDetailForm.EnvSettingTypeEnum envSettingTypeEnum) {
        waitForPageLoading();
        switch (envSettingTypeEnum) {
            case Docker:
                WebElementClick(driver, btnCreateDockerSetting);
                break;
            case Email:
                WebElementClick(driver, btnCreateEmailSetting);
                break;
            case Maven:
            case Ingress:
                break;
            default:
                throw new UnsupportedOperationException(
                    String.format("Unsupported environment type %s", envSettingTypeEnum.desc));
        }
        return new EnvironmentDetailForm(driver);
    }

    private void waitForPageLoading() {
        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.urlContains("/setting/system"));
    }
}
