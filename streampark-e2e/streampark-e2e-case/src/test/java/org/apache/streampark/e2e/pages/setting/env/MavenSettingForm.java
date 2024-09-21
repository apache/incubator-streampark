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

import lombok.Getter;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.FindBys;

import static org.apache.streampark.e2e.pages.common.CommonFactory.WebElementClick;
import static org.apache.streampark.e2e.pages.common.CommonFactory.WebElementDeleteAndInput;

@Getter
public class MavenSettingForm extends CommonForm {

    public WebDriver driver;
    @FindBys({
            @FindBy(xpath = "//h4[text()='Maven Settings File Path']"),
            @FindBy(xpath = "./ancestor::li[@class='ant-list-item']//button[contains(@class, 'ant-btn-primary')]")
    })
    public WebElement btnSettingsFilePath;

    @FindBys({
            @FindBy(xpath = "//h4[text()='Maven Central Repository']"),
            @FindBy(xpath = "./ancestor::li[@class='ant-list-item']//button[contains(@class, 'ant-btn-primary')]")
    })
    public WebElement btnCentralRepository;

    @FindBys({
            @FindBy(xpath = "//h4[text()='Maven Central Repository Auth User']"),
            @FindBy(xpath = "./ancestor::li[@class='ant-list-item']//button[contains(@class, 'ant-btn-primary')]")
    })
    // Maven central repository authentication username.
    public WebElement btnAuthUser;

    @FindBys({
            @FindBy(xpath = "//h4[text()='Maven Central Repository Auth Password']"),
            @FindBy(xpath = "./ancestor::li[@class='ant-list-item']//button[contains(@class, 'ant-btn-primary')]")
    })
    // Maven central repository authentication password.
    public WebElement btnAuthPassword;

    @FindBy(className = "streampark_maven_settings")
    public WebElement inputSettingsFilePath;

    @FindBy(className = "streampark_maven_central_repository")
    public WebElement inputCentralRepository;

    @FindBy(className = "streampark_maven_auth_user")
    public WebElement inputAuthUser;

    @FindBy(className = "streampark_maven_auth_password")
    public WebElement inputAuthPassword;

    MavenSettingForm(EnvironmentDetailForm environmentDetailForm) {
        super(environmentDetailForm);
        this.driver = environmentDetailForm.driver;
    }

    public MavenSettingForm filePath(String filePath) {
        WebElementClick(driver, btnSettingsFilePath);
        WebElementDeleteAndInput(inputSettingsFilePath, filePath);
        WebElementClick(driver, btnSettingsFilePath);
        return this;
    }

    public MavenSettingForm centralRepository(String repository) {
        WebElementClick(driver, btnCentralRepository);
        WebElementDeleteAndInput(inputCentralRepository, repository);
        WebElementClick(driver, btnCentralRepository);
        return this;
    }

    public MavenSettingForm authUser(String user) {
        WebElementClick(driver, btnAuthUser);
        WebElementDeleteAndInput(inputAuthUser, user);
        WebElementClick(driver, btnAuthUser);
        return this;
    }

    public MavenSettingForm authPassword(String password) {
        WebElementClick(driver, btnAuthPassword);
        WebElementDeleteAndInput(inputAuthPassword, password);
        WebElementClick(driver, btnAuthPassword);
        return this;
    }
}
