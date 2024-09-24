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

import org.apache.streampark.e2e.pages.common.Constants;

import lombok.Getter;
import lombok.SneakyThrows;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.FindBys;
import org.openqa.selenium.support.PageFactory;

import java.util.List;

import static org.apache.streampark.e2e.pages.common.CommonFactory.WebElementDeleteAndInput;

@Getter
public abstract class CommonForm {

    public WebDriver driver;

    @FindBy(id = "flink_cluster_clusterName")
    public WebElement inputFlinkClusterName;

    @FindBy(xpath = "//div[contains(@codefield, 'versionId')]//div[contains(@class, 'ant-select-selector')]")
    public WebElement buttonFlinkVersionDropdown;

    @FindBys({
            @FindBy(css = "[codefield=versionId]"),
            @FindBy(className = "ant-select-item-option-content")
    })
    public List<WebElement> selectFlinkVersion;

    @FindBy(id = "e2e-flinkcluster-submit-btn")
    public WebElement buttonSubmit;

    private final ClusterDetailForm parent;

    CommonForm(ClusterDetailForm clusterDetailForm) {
        final WebDriver driver = clusterDetailForm.driver;

        PageFactory.initElements(driver, this);

        this.parent = clusterDetailForm;
    }

    public CommonForm clusterName(String clusterName) {
        WebElementDeleteAndInput(inputFlinkClusterName, clusterName);
        return this;
    }

    @SneakyThrows
    public CommonForm flinkVersion(String flinkVersion) {
        buttonFlinkVersionDropdown.click();
        Thread.sleep(Constants.DEFAULT_SLEEP_MILLISECONDS);
        selectFlinkVersion.stream()
            .filter(e -> e.getText().equalsIgnoreCase(flinkVersion))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(String.format("Flink version not found: %s", flinkVersion)))
            .click();

        return this;
    }

    public ClusterDetailForm submit() {
        buttonSubmit.click();
        return parent;
    }
}
