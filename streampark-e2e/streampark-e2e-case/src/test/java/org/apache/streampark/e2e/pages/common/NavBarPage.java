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

package org.apache.streampark.e2e.pages.common;

import org.apache.streampark.e2e.pages.flink.ApacheFlinkPage;
import org.apache.streampark.e2e.pages.resource.ResourcePage;
import org.apache.streampark.e2e.pages.setting.SettingPage;
import org.apache.streampark.e2e.pages.system.SystemPage;

import lombok.Getter;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

@Getter
public class NavBarPage {

    public final RemoteWebDriver driver;

    @FindBy(className = "menu-item-flink")
    public WebElement apacheFlinkTab;

    @FindBy(className = "menu-item-resource")
    public WebElement resourcesTab;

    @FindBy(className = "menu-item-setting")
    public WebElement settingsTab;

    @FindBy(className = "menu-item-system")
    public WebElement systemTab;

    public NavBarPage(RemoteWebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }

    public <T extends NavBarItem> T goToNav(Class<T> nav) {
        if (nav == ApacheFlinkPage.class) {
            new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
                .until(ExpectedConditions.elementToBeClickable(apacheFlinkTab));
            String tabOpenStateXpath =
                "//span[contains(@class, 'menu-item-flink')]/../parent::li[contains(@class, 'streampark-menu-opened')]";
            if (driver.findElements(By.xpath(tabOpenStateXpath)).isEmpty()) {
                apacheFlinkTab.click();
            }
            return nav.cast(new ApacheFlinkPage(driver));
        }

        if (nav == SystemPage.class) {
            new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
                .until(ExpectedConditions.elementToBeClickable(systemTab));
            String tabOpenStateXpath =
                "//span[contains(@class, 'menu-item-system')]/../parent::li[contains(@class, 'streampark-menu-opened')]";
            if (driver.findElements(By.xpath(tabOpenStateXpath)).isEmpty()) {
                systemTab.click();
            }
            return nav.cast(new SystemPage(driver));
        }

        if (nav == ResourcePage.class) {
            new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
                .until(ExpectedConditions.elementToBeClickable(resourcesTab));
            String tabOpenStateXpath =
                "//span[contains(@class, 'menu-item-resource')]/../parent::li[contains(@class, 'streampark-menu-opened')]";
            if (driver.findElements(By.xpath(tabOpenStateXpath)).isEmpty()) {
                resourcesTab.click();
            }
            return nav.cast(new ResourcePage(driver));
        }

        if (nav == SettingPage.class) {
            new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
                .until(ExpectedConditions.elementToBeClickable(settingsTab));
            String tabOpenStateXpath =
                "//span[contains(@class, 'menu-item-setting')]/../parent::li[contains(@class, 'streampark-menu-opened')]";
            if (driver.findElements(By.xpath(tabOpenStateXpath)).isEmpty()) {
                settingsTab.click();
            }
            return nav.cast(new SettingPage(driver));
        }

        throw new UnsupportedOperationException("Unknown nav bar");
    }

    public interface NavBarItem {
    }
}
