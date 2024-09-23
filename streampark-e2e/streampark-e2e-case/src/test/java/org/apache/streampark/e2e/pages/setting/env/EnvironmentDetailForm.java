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

package org.apache.streampark.e2e.pages.setting.env;

import org.apache.streampark.e2e.pages.common.Constants;

import lombok.Getter;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.WebDriverWait;

@Getter
public class EnvironmentDetailForm {

    public WebDriver driver;

    public EnvironmentDetailForm(WebDriver driver) {
        PageFactory.initElements(driver, this);
        this.driver = driver;
    }

    @SuppressWarnings("unchecked")
    public <T> T addSetting(EnvSettingTypeEnum envSettingTypeEnum) {
        new WebDriverWait(driver, Constants.DEFAULT_WEBDRIVER_WAIT_DURATION);
        switch (envSettingTypeEnum) {
            case Maven:
                return (T) new MavenSettingForm(this);
            case Email:
                return (T) new EmailSettingForm(this);
            case Docker:
                return (T) new DockerSettingForm(this);
            case Ingress:
                return (T) new IngressSettingForm(this);
            default:
                throw new UnsupportedOperationException(
                    String.format("Unsupported Environment setting type %s", envSettingTypeEnum.desc));
        }
    }

    @Getter
    public enum EnvSettingTypeEnum {

        Maven("Maven"),
        Docker("Docker"),
        Email("Email"),
        Ingress("Ingress");
        public final String desc;

        EnvSettingTypeEnum(String desc) {
            this.desc = desc;
        }
    }
}
