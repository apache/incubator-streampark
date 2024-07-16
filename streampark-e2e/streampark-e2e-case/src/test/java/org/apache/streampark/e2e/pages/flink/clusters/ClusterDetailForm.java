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

package org.apache.streampark.e2e.pages.flink.clusters;

import lombok.Getter;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.FindBys;
import org.openqa.selenium.support.PageFactory;

import java.util.List;

@Getter
public class ClusterDetailForm {

    private WebDriver driver;

    @FindBy(xpath = "//div[contains(@codefield, 'executionMode')]//div[contains(@class, 'ant-select-selector')]")
    private WebElement buttonExecutionModeDropdown;

    @FindBys({
            @FindBy(css = "[codefield=executionMode]"),
            @FindBy(className = "ant-select-item-option-content")
    })
    private List<WebElement> selectExecutionMode;

    public ClusterDetailForm(WebDriver driver) {
        PageFactory.initElements(driver, this);

        this.driver = driver;
    }

    @SuppressWarnings("unchecked")
    public <T> T addCluster(ExecutionMode executionMode) {
        buttonExecutionModeDropdown.click();
        switch (executionMode) {
            case YARN_SESSION:
                selectExecutionMode.stream()
                    .filter(e -> e.getText().equalsIgnoreCase(ExecutionMode.YARN_SESSION.desc()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                        String.format("Execution Mode not found: %s", executionMode.desc())))
                    .click();
                return (T) new YarnSessionForm(this);
            default:
                throw new UnsupportedOperationException(String.format("Unknown execution mode: %s", executionMode.desc()));
        }
    }

    @Getter
    public enum ExecutionMode {

        REMOTE("remote"),
        YARN_SESSION("yarn session"),
        KUBERNETES_SESSION(
            "kubernetes session");

        private final String desc;

        ExecutionMode(String desc) {
            this.desc = desc;
        }
    }
}
