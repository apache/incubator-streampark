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

import org.apache.streampark.e2e.pages.common.NavBarPage;

import lombok.Getter;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

@Getter
public class SettingPage extends NavBarPage implements NavBarPage.NavBarItem {

    @FindBy(xpath = "//span[contains(@class, 'streampark-simple-menu-sub-title') and contains(text(), 'Yarn Queue')]//..")
    private WebElement menuYarnQueueManagement;

    public SettingPage(RemoteWebDriver driver) {
        super(driver);
    }

    public <T extends SettingPage.Tab> T goToTab(Class<T> tab) {
        if (tab == YarnQueuePage.class) {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.elementToBeClickable(menuYarnQueueManagement));
            menuYarnQueueManagement.click();
            return tab.cast(new YarnQueuePage(driver));
        }

        throw new UnsupportedOperationException("Unknown tab: " + tab.getName());
    }

    public interface Tab {
    }
}
