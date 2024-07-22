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
import org.apache.streampark.e2e.pages.setting.alarm.AlarmPage;
import org.apache.streampark.e2e.pages.setting.SettingPage;
import org.apache.streampark.e2e.pages.setting.alarm.AlertTypeDetailForm;
import org.apache.streampark.e2e.pages.setting.alarm.EmailAlertForm;

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

    private static final String editAlarmName = "edit_alarm";

    private static final String newEmail = "new@streampark.com";

    private static final String editEmail = "edit@streampark.com";

    private static final AlertTypeDetailForm.AlertTypeEnum alertType = AlertTypeDetailForm.AlertTypeEnum.EMAIL;

    @BeforeAll
    public static void setup() {
        new LoginPage(browser)
            .login(userName, password, teamName)
            .goToNav(SettingPage.class)
            .goToTab(AlarmPage.class);
    }

    @Test
    @Order(10)
    public void testCreateEmailAlarm() {
        final AlarmPage alarmPage = new AlarmPage(browser);

        alarmPage.createAlarm()
            .<EmailAlertForm>addAlertType(alertType)
            .email(newEmail)
            .alertName(newAlarmName)
            .submit();

        Awaitility.await()
            .untilAsserted(
                () -> assertThat(alarmPage.alarmList())
                    .as("Alarm list should contain newly-created alarm")
                    .extracting(WebElement::getText)
                    .anyMatch(it -> it.contains(newAlarmName))
                    .anyMatch(it -> it.contains(AlertTypeDetailForm.AlertTypeEnum.EMAIL.desc())));
    }

    @Test
    @Order(20)
    public void testCreateDuplicateEmailAlarm() {
        final AlarmPage alarmPage = new AlarmPage(browser);

        EmailAlertForm emailAlertForm = alarmPage.createAlarm()
            .addAlertType(alertType);

        emailAlertForm.email(newEmail)
            .alertName(newAlarmName)
            .submit();

        Awaitility.await()
            .untilAsserted(
                () -> assertThat(alarmPage.alarmList())
                    .as("Alarm list should contain newly-created alarm")
                    .extracting(WebElement::getText)
                    .anyMatch(it -> it.contains(newAlarmName))
                    .anyMatch(it -> it.contains(AlertTypeDetailForm.AlertTypeEnum.EMAIL.desc())));
        emailAlertForm.cancel();
    }

    @Test
    @Order(20)
    public void testEditEmailAlarm() {
        final AlarmPage alarmPage = new AlarmPage(browser);

        alarmPage.editAlarm(newAlarmName)
            // this step will cancel alertType click status.
            .<EmailAlertForm>addAlertType(alertType)
            .parent()
            // click again to recover.
            .<EmailAlertForm>addAlertType(alertType)
            .email(editEmail)
            .alertName(editAlarmName)
            .submit();

        Awaitility.await()
            .untilAsserted(
                () -> assertThat(alarmPage.alarmList())
                    .as("Alarm list should contain edited alarm")
                    .extracting(WebElement::getText)
                    .anyMatch(it -> it.contains(editAlarmName))
                    .anyMatch(it -> it.contains(editEmail)));
    }

    @Test
    @Order(40)
    public void testDeleteEmailAlarm() {
        final AlarmPage alarmPage = new AlarmPage(browser);

        alarmPage.deleteAlarm(editAlarmName);

        Awaitility.await()
            .untilAsserted(
                () -> assertThat(alarmPage.alarmList())
                    .as(String.format("Alarm list shouldn't contain alarm witch named %s", editAlarmName))
                    .extracting(WebElement::getText)
                    .noneMatch(it -> it.contains(editAlarmName)));
    }
}
