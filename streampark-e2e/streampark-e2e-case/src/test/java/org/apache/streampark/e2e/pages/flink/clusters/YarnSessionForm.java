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

import java.util.List;

@Getter
public class YarnSessionForm extends CommonForm {

    public WebDriver driver;

    @FindBy(xpath = "//div[contains(@codefield, 'resolveOrder')]//div[contains(@class, 'ant-select-selector')]")
    public WebElement buttonResolveOrderDropdown;

    @FindBys({
            @FindBy(css = "[codefield=resolveOrder]"),
            @FindBy(className = "ant-select-item-option-content")
    })
    private List<WebElement> selectResolveOrder;

    public YarnSessionForm(ClusterDetailForm clusterDetailForm) {
        super(clusterDetailForm);
        this.driver = clusterDetailForm.driver;
    }

    public YarnSessionForm resolveOrder(ResolveOrder resolveOrder) {
        buttonResolveOrderDropdown.click();
        selectResolveOrder.stream()
            .filter(e -> e.getText().equalsIgnoreCase(resolveOrder.desc))
            .findFirst()
            .orElseThrow(
                () -> new IllegalArgumentException(String.format("Resolve Order not found: %s", resolveOrder.desc)))
            .click();

        return this;
    }

    @Getter
    public enum ResolveOrder {

        PARENT_FIRST("parent-first"),
        CHILD_FIRST("child-first");

        private final String desc;

        ResolveOrder(String desc) {
            this.desc = desc;
        }
    }
}
