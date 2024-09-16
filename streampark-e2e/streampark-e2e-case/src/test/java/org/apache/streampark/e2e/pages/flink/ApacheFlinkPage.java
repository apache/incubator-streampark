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

package org.apache.streampark.e2e.pages.flink;

import org.apache.streampark.e2e.pages.common.Constants;
import org.apache.streampark.e2e.pages.common.NavBarPage;
import org.apache.streampark.e2e.pages.common.NavBarPage.NavBarItem;
import org.apache.streampark.e2e.pages.flink.applications.ApplicationsPage;
import org.apache.streampark.e2e.pages.flink.clusters.FlinkClustersPage;

import lombok.Getter;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

@Getter
public final class ApacheFlinkPage extends NavBarPage implements NavBarItem {

    @FindBy(className = "menu-item-flink_app")
    public WebElement menuApplications;

    @FindBy(className = "menu-item-flink_home")
    public WebElement menuFlinkHome;

    @FindBy(className = "menu-item-flink_cluster")
    public WebElement menuClusters;

    public ApacheFlinkPage(RemoteWebDriver driver) {
        super(driver);
    }

    public <T extends ApacheFlinkPage.Tab> T goToTab(Class<T> tab) {
        if (tab == ApplicationsPage.class) {
            new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
                .until(ExpectedConditions.elementToBeClickable(menuApplications));
            menuApplications.click();
            return tab.cast(new ApplicationsPage(driver));
        }

        if (tab == FlinkHomePage.class) {
            new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
                .until(ExpectedConditions.elementToBeClickable(menuFlinkHome));
            menuFlinkHome.click();
            return tab.cast(new FlinkHomePage(driver));
        }

        if (tab == FlinkClustersPage.class) {
            new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
                .until(ExpectedConditions.elementToBeClickable(menuClusters));
            menuClusters.click();
            return tab.cast(new FlinkClustersPage(driver));
        }

        throw new UnsupportedOperationException("Unknown tab: " + tab.getName());
    }

    public interface Tab {
    }
}
