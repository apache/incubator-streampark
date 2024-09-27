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

    public WebDriver driver;

    @FindBy(xpath = "//div[contains(@codefield, 'deployMode')]//div[contains(@class, 'ant-select-selector')]")
    public WebElement buttonDeployModeDropdown;

    @FindBys({
            @FindBy(css = "[codefield=deployMode]"),
            @FindBy(className = "ant-select-item-option-content")
    })
    private List<WebElement> selectDeployMode;

    public ClusterDetailForm(WebDriver driver) {
        PageFactory.initElements(driver, this);
        this.driver = driver;
    }

    @SuppressWarnings("unchecked")
    public <T> T addCluster(DeployMode deployMode) {
        buttonDeployModeDropdown.click();
        switch (deployMode) {
            case STANDALONE:
                selectDeployMode.stream()
                    .filter(e -> e.getText().equalsIgnoreCase(DeployMode.STANDALONE.desc))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                        String.format("Execution Mode not found: %s", deployMode.desc)))
                    .click();
                return (T) new RemoteForm(this);
            case YARN_SESSION:
                selectDeployMode.stream()
                    .filter(e -> e.getText().equalsIgnoreCase(DeployMode.YARN_SESSION.desc))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                        String.format("Execution Mode not found: %s", deployMode.desc)))
                    .click();
                return (T) new YarnSessionForm(this);
            default:
                throw new UnsupportedOperationException(
                    String.format("Unknown execution mode: %s", deployMode.desc));
        }
    }

    @Getter
    public enum DeployMode {

        STANDALONE("standalone"),
        YARN_SESSION("yarn session"),
        KUBERNETES_SESSION(
            "kubernetes session");

        private final String desc;

        DeployMode(String desc) {
            this.desc = desc;
        }
    }
}
