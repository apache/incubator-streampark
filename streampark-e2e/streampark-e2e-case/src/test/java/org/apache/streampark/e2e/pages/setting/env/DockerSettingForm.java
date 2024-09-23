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

import static org.apache.streampark.e2e.pages.common.CommonFactory.WebElementClick;
import static org.apache.streampark.e2e.pages.common.CommonFactory.WebElementDeleteAndInput;

@Getter
public class DockerSettingForm extends CommonForm {

    public WebDriver driver;

    @FindBy(id = "SettingForm_address")
    public WebElement inputAddress;

    @FindBy(id = "SettingForm_namespace")
    public WebElement inputNamespace;

    @FindBy(id = "SettingForm_username")
    public WebElement inputUsername;

    @FindBy(xpath = "//input[@placeholder='Docker Password']")
    public WebElement inputPassword;

    @FindBy(xpath = "//div[contains(@class, 'ant-modal-title') and contains(., 'Docker Setting')]/../..//button[contains(@class, 'ant-btn')]//span[contains(text(), 'OK')]")
    public WebElement buttonOk;

    @FindBy(xpath = "//div[contains(@class, 'ant-modal-title') and contains(., 'Docker Setting')]/../..//button[contains(@class, 'ant-btn')]//span[contains(text(), 'Cancel')]")
    public WebElement buttonCancel;

    DockerSettingForm(EnvironmentDetailForm environmentDetailForm) {
        super(environmentDetailForm);
        this.driver = environmentDetailForm.driver;
    }

    public DockerSettingForm address(String address) {
        WebElementDeleteAndInput(inputAddress, address);
        return this;
    }

    public DockerSettingForm namespace(String namespace) {
        WebElementDeleteAndInput(inputNamespace, namespace);
        return this;
    }

    public DockerSettingForm user(String user) {
        WebElementDeleteAndInput(inputUsername, user);
        return this;
    }

    public DockerSettingForm password(String password) {
        WebElementDeleteAndInput(inputPassword, password);
        return this;
    }

    public DockerSettingForm ok() {
        WebElementClick(driver, buttonOk);
        return this;
    }

    public DockerSettingForm cancel() {
        WebElementClick(driver, buttonCancel);
        return this;
    }
}
