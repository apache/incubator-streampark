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

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class CommonFactory {

    public static void WebElementDeleteAndInput(WebElement element, String value) {
        element.sendKeys(Keys.CONTROL + "a");
        element.sendKeys(Keys.BACK_SPACE);
        element.sendKeys(value);
    }

    public static void WebDriverWaitForElementVisibilityAndInvisibility(WebDriver driver, String msg) {
        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(
                ExpectedConditions.visibilityOfElementLocated(
                    By.xpath(String.format("//*[contains(text(),'%s')]",
                        msg))));
        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(
                ExpectedConditions.invisibilityOfElementLocated(
                    By.xpath(String.format("//*[contains(text(),'%s')]",
                        msg))));
    }

    public static void WebDriverWaitForElementVisibilityAndInvisibilityWithDuration(WebDriver driver, String msg,
                                                                                    Duration duration) {
        new WebDriverWait(driver, duration)
            .until(
                ExpectedConditions.visibilityOfElementLocated(
                    By.xpath(String.format("//*[contains(text(),'%s')]",
                        msg))));
        new WebDriverWait(driver, duration)
            .until(
                ExpectedConditions.invisibilityOfElementLocated(
                    By.xpath(String.format("//*[contains(text(),'%s')]",
                        msg))));
    }
}
