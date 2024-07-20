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

package org.apache.streampark.e2e.cases;

import org.apache.streampark.e2e.core.StreamPark;
import org.apache.streampark.e2e.pages.LoginPage;
import org.apache.streampark.e2e.pages.setting.AlarmPage;
import org.apache.streampark.e2e.pages.setting.SettingPage;
import org.apache.streampark.e2e.pages.setting.entity.AlarmPageAlertType;
import org.apache.streampark.e2e.pages.setting.entity.AlarmPageEmailSetting;
import org.apache.streampark.e2e.pages.setting.entity.AlarmPageWeChatSetting;
import org.apache.streampark.e2e.pages.setting.entity.FaultAlertSetting;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import static org.assertj.core.api.Assertions.assertThat;

@StreamPark(composeFiles = "docker/basic/docker-compose.yaml")
public class AlarmTest {

    private static RemoteWebDriver browser;

    private static final String userName = "admin";

    private static final String password = "streampark";

    private static final String teamName = "default";

    private static final String newAlarmName = "new_alarm";

    private static FaultAlertSetting faultAlertSetting;

    @BeforeAll
    public static void setup() {
        new LoginPage(browser)
            .login(userName, password, teamName)
            .goToNav(SettingPage.class)
            .goToTab(AlarmPage.class);

        faultAlertSetting = new AlarmPageEmailSetting("test@test.com");
    }

    @Test
    @Order(10)
    void testCreateAlarm() {
        final AlarmPage alarmPage = new AlarmPage(browser);

        alarmPage.createAlarm(newAlarmName, AlarmPageAlertType.EMAIL.value(), faultAlertSetting);

        Awaitility.await()
            .untilAsserted(
                () -> assertThat(alarmPage.alarmList())
                    .as("Alarm list should contain newly-created alarm")
                    .extracting(WebElement::getText)
                    .anyMatch(it -> it.contains(newAlarmName))
                    .anyMatch(it -> it.contains(AlarmPageAlertType.EMAIL.value())));
    }

    @Test
    @Order(20)
    void testCreateDuplicateAlarm() {
        final AlarmPage alarmPage = new AlarmPage(browser);

        alarmPage.createAlarm(newAlarmName, AlarmPageAlertType.EMAIL.value(), faultAlertSetting);

        Awaitility.await()
            .untilAsserted(
                () -> assertThat(alarmPage.errorMessageList())
                    .as("Alert Name Duplicated Error message should be displayed")
                    .extracting(WebElement::getText)
                    .anyMatch(it -> it.contains(
                        "Alert Name must be unique. The alert name already exists")));

        alarmPage.createAlarmForm().buttonCancel().click();
    }

    @Test
    @Order(30)
    void testEditAlarm() {
        final AlarmPage alarmPage = new AlarmPage(browser);
        faultAlertSetting = new AlarmPageWeChatSetting().token("wechat_token");
        alarmPage.editAlarm(newAlarmName, AlarmPageAlertType.WECHAT.value(), faultAlertSetting);

        Awaitility.await()
            .untilAsserted(
                () -> assertThat(alarmPage.alarmList())
                    .as("Alarm list should contain newly-edited alarm")
                    .extracting(WebElement::getText)
                    .anyMatch(it -> it.contains(newAlarmName))
                    .anyMatch(it -> it.contains(AlarmPageAlertType.EMAIL.value()))
                    .anyMatch(it -> it.contains(AlarmPageAlertType.WECHAT.value())));
    }

    @Test
    @Order(40)
    void testDeleteAlarm() {
        final AlarmPage alarmPage = new AlarmPage(browser);
        alarmPage.deleteAlarm(newAlarmName);

        Awaitility.await()
            .untilAsserted(
                () -> assertThat(alarmPage.alarmList())
                    .as(String.format("Alarm list shouldn't contain alarm witch named %s", newAlarmName))
                    .extracting(WebElement::getText)
                    .noneMatch(it -> it.contains(newAlarmName)));
    }
}
