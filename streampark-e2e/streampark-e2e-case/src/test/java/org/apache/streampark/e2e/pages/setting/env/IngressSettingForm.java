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
public class IngressSettingForm extends CommonForm {

    public WebDriver driver;

    @FindBys({
            @FindBy(xpath = "//h4[text()='Ingress domain address Setting']"),
            @FindBy(xpath = "./ancestor::li[@class='ant-list-item']//button[contains(@class, 'ant-btn') and contains(@class, 'ant-btn-primary') and contains(@class, 'ant-btn-circle')]")
    })
    public WebElement btnDomainAddressSetting;

    @FindBy(className = "ingress_mode_default")
    public WebElement inputDomainAddress;

    IngressSettingForm(EnvironmentDetailForm environmentDetailForm) {
        super(environmentDetailForm);
        this.driver = environmentDetailForm.driver;
    }

    public IngressSettingForm domainAddress(String domainAddress) {
        WebElementClick(driver, btnDomainAddressSetting);
        WebElementDeleteAndInput(inputDomainAddress, domainAddress);
        WebElementClick(driver, btnDomainAddressSetting);
        return this;
    }
}
