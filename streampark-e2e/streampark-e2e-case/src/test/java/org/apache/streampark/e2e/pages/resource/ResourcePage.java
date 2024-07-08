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

package org.apache.streampark.e2e.pages.resource;

import org.apache.streampark.e2e.pages.common.NavBarPage;
import org.apache.streampark.e2e.pages.common.NavBarPage.NavBarItem;

import lombok.Getter;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

@Getter
public final class ResourcePage extends NavBarPage implements NavBarItem {

    @FindBy(xpath = "//span[contains(@class, 'streampark-simple-menu-sub-title') and contains(text(), 'Variables')]//..")
    private WebElement resourceVariableManagement;

    @FindBy(xpath = "//span[contains(@class, 'streampark-simple-menu-sub-title') and contains(text(), 'Projects')]//..")
    private WebElement resourceProjectsManagement;

    @FindBy(xpath = "//span[contains(@class, 'streampark-simple-menu-sub-title') and contains(text(), 'Uploads')]//..")
    private WebElement resourceManagement;

    public ResourcePage(RemoteWebDriver driver) {
        super(driver);
    }

    public <T extends ResourcePage.Tab> T goToTab(Class<T> tab) {
        if (tab == VariableManagementPage.class) {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.elementToBeClickable(resourceVariableManagement));
            resourceVariableManagement.click();
            return tab.cast(new VariableManagementPage(driver));
        }

        if (tab == ProjectsPage.class) {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.elementToBeClickable(resourceProjectsManagement));
            resourceProjectsManagement.click();
            return tab.cast(new ProjectsPage(driver));
        }

        if (tab == ResourceManagementPage.class) {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.elementToBeClickable(resourceManagement));
            resourceManagement.click();
            return tab.cast(new ResourceManagementPage(driver));
        }

        throw new UnsupportedOperationException("Unknown tab: " + tab.getName());
    }

    public interface Tab {
    }
}
