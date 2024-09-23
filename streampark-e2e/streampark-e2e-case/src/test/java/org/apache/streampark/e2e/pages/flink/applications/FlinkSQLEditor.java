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

package org.apache.streampark.e2e.pages.flink.applications;

import org.apache.streampark.e2e.pages.common.Constants;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.platform.commons.util.StringUtils;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.Arrays;
import java.util.List;

@Getter
@Slf4j
public final class FlinkSQLEditor {

    @FindBy(xpath = "//label[contains(@for, 'form_item_flinkSql')]/../..//div[contains(@class, 'monaco-editor')]//div[contains(@class, 'view-line') and not(contains(@class, 'view-lines'))]")
    public List<WebElement> flinkSqlEditor;

    public WebDriver driver;

    public static final Integer FLINK_SQL_EDITOR_SLEEP_MILLISECONDS = 1000;

    public FlinkSQLEditor(WebDriver driver) {
        PageFactory.initElements(driver, this);
        this.driver = driver;
    }

    @SneakyThrows
    public FlinkSQLEditor content(String content) {
        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION)
            .until(ExpectedConditions.elementToBeClickable(flinkSqlEditor.get(0)));

        Actions actions = new Actions(this.driver);

        List<String> contentList = Arrays.asList(content.split(Constants.LINE_SEPARATOR));

        for (int i = 0; i < contentList.size(); i++) {
            String editorLineText;
            String inputContent = contentList.get(i);
            int flinkSqlEditorIndex = Math.min(i, 21);

            if (i == 0) {
                actions.moveToElement(flinkSqlEditor.get(flinkSqlEditorIndex))
                    .click()
                    .sendKeys(inputContent)
                    .sendKeys(Constants.LINE_SEPARATOR)
                    .perform();
                continue;
            } else {
                editorLineText = flinkSqlEditor.get(flinkSqlEditorIndex).getText();
            }

            if (StringUtils.isNotBlank(inputContent)) {
                if (!editorLineText.isEmpty()) {
                    for (int p = 0; p < editorLineText.trim().length(); p++) {
                        clearLine(actions, flinkSqlEditor.get(flinkSqlEditorIndex));
                    }
                    clearLine(actions, flinkSqlEditor.get(flinkSqlEditorIndex));
                }
                actions.moveToElement(flinkSqlEditor.get(flinkSqlEditorIndex))
                    .click()
                    .sendKeys(inputContent)
                    .sendKeys(Constants.LINE_SEPARATOR)
                    .perform();
                Thread.sleep(FLINK_SQL_EDITOR_SLEEP_MILLISECONDS);
            } else {
                actions.moveToElement(flinkSqlEditor.get(flinkSqlEditorIndex))
                    .click()
                    .sendKeys(Constants.LINE_SEPARATOR)
                    .perform();
                Thread.sleep(FLINK_SQL_EDITOR_SLEEP_MILLISECONDS);
            }
        }

        return this;
    }

    private void clearLine(Actions actions, WebElement element) {
        actions.moveToElement(element)
            .click()
            .sendKeys(Keys.BACK_SPACE)
            .perform();
    }
}
