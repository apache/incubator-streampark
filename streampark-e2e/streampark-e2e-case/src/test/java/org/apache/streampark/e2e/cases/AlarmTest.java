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
import org.apache.streampark.e2e.pages.setting.SettingPage;
import org.apache.streampark.e2e.pages.setting.alarm.AlarmPage;
import org.apache.streampark.e2e.pages.setting.alarm.AlertTypeDetailForm;
import org.apache.streampark.e2e.pages.setting.alarm.DingTalkAlertForm;
import org.apache.streampark.e2e.pages.setting.alarm.EmailAlertForm;
import org.apache.streampark.e2e.pages.setting.alarm.LarkAlertForm;
import org.apache.streampark.e2e.pages.setting.alarm.WeChatAlertForm;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import static org.assertj.core.api.Assertions.assertThat;

@StreamPark(composeFiles = "docker/basic/docker-compose.yaml")
public class AlarmTest {

    public static RemoteWebDriver browser;

    private static final String newEmail = "new@streampark.com";

    private static final String newAlarmName = "new_alarm";

    private static final String editAlarmName = "edit_alarm";

    @BeforeAll
    public static void setup() {
        new LoginPage(browser)
            .login()
            .goToNav(SettingPage.class)
            .goToTab(AlarmPage.class);
    }

    @Test
    @Order(1)
    public void testCreateAlarm() {
        final AlarmPage alarmPage = new AlarmPage(browser);

        final String dingTalkURL = "";
        final String dingTalkToken = "dingTalkToken";
        final String dingTalkSecretToken = "dingTalkSecretToken";
        final String dingTalkReceiveUser = "dingTalkUser";

        final String wechatToken = "wechatToken";

        final String larkToken = "larkToken";
        final String larkSecretToken = "larkSecretToken";

        AlertTypeDetailForm alertTypeDetailForm = alarmPage.createAlarm();
        alertTypeDetailForm
            .<EmailAlertForm>addAlertType(AlertTypeDetailForm.AlertTypeEnum.EMAIL)
            .email(newEmail)
            .alertName(newAlarmName);
        alertTypeDetailForm
            .<DingTalkAlertForm>addAlertType(AlertTypeDetailForm.AlertTypeEnum.DINGTALK)
            .url(dingTalkURL)
            .token(dingTalkToken)
            .secretEnable()
            .secretToken(dingTalkSecretToken)
            .effectToAllUsers()
            .receiveUser(dingTalkReceiveUser);
        alertTypeDetailForm
            .<WeChatAlertForm>addAlertType(AlertTypeDetailForm.AlertTypeEnum.WECHAT)
            .token(wechatToken);
        alertTypeDetailForm
            .<LarkAlertForm>addAlertType(AlertTypeDetailForm.AlertTypeEnum.LARK)
            .token(larkToken)
            .secretEnable()
            .secretToken(larkSecretToken)
            .effectToAllUsers()
            .submit();

        Awaitility.await()
            .untilAsserted(
                () -> assertThat(alarmPage.alarmList)
                    .as("Alarm list should contain newly-created alarm")
                    .extracting(WebElement::getText)
                    .anyMatch(it -> it.contains(newAlarmName))
                    .anyMatch(it -> it.contains(AlertTypeDetailForm.AlertTypeEnum.EMAIL.desc))
                    .anyMatch(it -> it.contains(newEmail))
                    .anyMatch(it -> it.contains(AlertTypeDetailForm.AlertTypeEnum.DINGTALK.desc))
                    .anyMatch(it -> it.contains(AlertTypeDetailForm.AlertTypeEnum.WECHAT.desc))
                    .anyMatch(it -> it.contains(AlertTypeDetailForm.AlertTypeEnum.LARK.desc))
                    .anyMatch(it -> it.contains(dingTalkReceiveUser)));
    }

    @Test
    @Order(2)
    public void testEditAlarm() {
        final AlarmPage alarmPage = new AlarmPage(browser);

        alarmPage.editAlarm(newAlarmName)
            // this step will cancel E-mail type click status.
            .<EmailAlertForm>addAlertType(AlertTypeDetailForm.AlertTypeEnum.EMAIL)
            .alertName(editAlarmName)
            .submit();

        Awaitility.await()
            .untilAsserted(
                () -> assertThat(alarmPage.alarmList)
                    .as("Alarm list should contain edited alarm")
                    .extracting(WebElement::getText)
                    .anyMatch(it -> it.contains(editAlarmName))
                    .noneMatch(it -> it.contains(AlertTypeDetailForm.AlertTypeEnum.EMAIL.desc))
                    .anyMatch(it -> it.contains(AlertTypeDetailForm.AlertTypeEnum.DINGTALK.desc))
                    .anyMatch(it -> it.contains(AlertTypeDetailForm.AlertTypeEnum.WECHAT.desc))
                    .anyMatch(it -> it.contains(AlertTypeDetailForm.AlertTypeEnum.LARK.desc))
                    .noneMatch(it -> it.contains(newEmail)));
    }

    @Test
    @Order(3)
    public void testDeleteAlarm() {
        final AlarmPage alarmPage = new AlarmPage(browser);

        alarmPage.deleteAlarm(editAlarmName);

        Awaitility.await()
            .untilAsserted(
                () -> assertThat(alarmPage.alarmList)
                    .as(String.format("Alarm list shouldn't contain alarm witch named %s", editAlarmName))
                    .extracting(WebElement::getText)
                    .noneMatch(it -> it.contains(editAlarmName)));
    }
}
