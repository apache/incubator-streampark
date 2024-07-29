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
package org.apache.streampark.e2e.pages.system;

import org.apache.streampark.e2e.pages.common.Constants;
import org.apache.streampark.e2e.pages.common.NavBarPage;
import org.apache.streampark.e2e.pages.common.NavBarPage.NavBarItem;

import lombok.Getter;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

@Getter
public final class SystemPage extends NavBarPage implements NavBarItem {

    @FindBy(xpath = "//span[contains(@class, 'streampark-simple-menu-sub-title') and contains(text(), 'User Management')]//..")
    private WebElement menuUserManagement;

    @FindBy(xpath = "//span[contains(@class, 'streampark-simple-menu-sub-title') and contains(text(), 'Token Management')]//..")
    private WebElement menuTokenManagement;

    @FindBy(xpath = "//span[contains(@class, 'streampark-simple-menu-sub-title') and contains(text(), 'Role Management')]//..")
    private WebElement menuRoleManagement;

    @FindBy(xpath = "//span[contains(@class, 'streampark-simple-menu-sub-title') and contains(text(), 'Team Management')]//..")
    private WebElement menuTeamManagement;

    @FindBy(xpath = "//span[contains(@class, 'streampark-simple-menu-sub-title') and contains(text(), 'Member Management')]//..")
    private WebElement menuMemberManagement;

    public SystemPage(RemoteWebDriver driver) {
        super(driver);
    }

    public <T extends SystemPage.Tab> T goToTab(Class<T> tab) {
        if (tab == UserManagementPage.class) {
            new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
                .until(ExpectedConditions.elementToBeClickable(menuUserManagement));
            menuUserManagement.click();
            return tab.cast(new UserManagementPage(driver));
        }

        if (tab == TeamManagementPage.class) {
            new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
                .until(ExpectedConditions.elementToBeClickable(menuTeamManagement));
            menuTeamManagement.click();
            return tab.cast(new TeamManagementPage(driver));
        }

        if (tab == RoleManagementPage.class) {
            new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
                .until(ExpectedConditions.elementToBeClickable(menuRoleManagement));
            menuRoleManagement.click();
            return tab.cast(new RoleManagementPage(driver));
        }
        if (tab == TokenManagementPage.class) {
            new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
                .until(ExpectedConditions.elementToBeClickable(menuTokenManagement));
            menuTokenManagement.click();
            return tab.cast(new TokenManagementPage(driver));
        }

        if (tab == MemberManagementPage.class) {
            new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
                .until(ExpectedConditions.elementToBeClickable(menuMemberManagement));
            menuMemberManagement.click();
            return tab.cast(new MemberManagementPage(driver));
        }

        throw new UnsupportedOperationException("Unknown tab: " + tab.getName());
    }

    public interface Tab {
    }
}
