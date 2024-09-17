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
import org.openqa.selenium.support.PageFactory;

import static org.apache.streampark.e2e.pages.common.CommonFactory.WebElementDeleteAndInput;

@Getter
public abstract class CommonForm {

    public WebDriver driver;

    @FindBy(id = "form_item_alertName")
    public WebElement inputAlertName;

    @FindBy(className = "e2e-alert-submit-btn")
    public WebElement buttonSubmit;

    @FindBy(className = "e2e-alert-cancel-btn")
    public WebElement buttonCancel;

    private final AlertTypeDetailForm parent;

    CommonForm(AlertTypeDetailForm alertTypeDetailForm) {
        final WebDriver driver = alertTypeDetailForm.driver;
        PageFactory.initElements(driver, this);
        this.parent = alertTypeDetailForm;
    }

    public CommonForm alertName(String alertName) {
        WebElementDeleteAndInput(inputAlertName, alertName);
        return this;
    }

    public AlertTypeDetailForm submit() {
        buttonSubmit.click();
        return parent;
    }

    public AlertTypeDetailForm cancel() {
        buttonCancel.click();
        return parent;
    }
}
