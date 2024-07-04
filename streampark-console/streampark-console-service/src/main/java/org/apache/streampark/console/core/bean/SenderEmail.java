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

package org.apache.streampark.console.core.bean;

import org.apache.streampark.console.core.entity.Setting;
import org.apache.streampark.console.core.service.SettingService;

import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.List;

@Getter
@Setter
public class SenderEmail {

    private String host;
    private Integer port;
    private String from;
    private String userName;
    private String password;
    private boolean ssl;

    public static List<Setting> toSettings(SenderEmail senderEmail) {
        Setting smtpHost = new Setting();
        smtpHost.setSettingKey(SettingService.KEY_ALERT_EMAIL_HOST);
        smtpHost.setSettingValue(senderEmail.getHost());

        Setting smtpPort = new Setting();
        smtpPort.setSettingKey(SettingService.KEY_ALERT_EMAIL_PORT);
        smtpPort.setSettingValue(senderEmail.getPort().toString());

        Setting smtpFrom = new Setting();
        smtpFrom.setSettingKey(SettingService.KEY_ALERT_EMAIL_FROM);
        smtpFrom.setSettingValue(senderEmail.getFrom());

        Setting smtpUserName = new Setting();
        smtpUserName.setSettingKey(SettingService.KEY_ALERT_EMAIL_USERNAME);
        smtpUserName.setSettingValue(senderEmail.getUserName());

        Setting smtpPassword = new Setting();
        smtpPassword.setSettingKey(SettingService.KEY_ALERT_EMAIL_PASSWORD);
        smtpPassword.setSettingValue(senderEmail.getPassword());

        Setting smtpSsl = new Setting();
        smtpSsl.setSettingKey(SettingService.KEY_ALERT_EMAIL_SSL);
        smtpSsl.setSettingValue(senderEmail.isSsl() + "");

        return Arrays.asList(smtpHost, smtpPort, smtpFrom, smtpUserName, smtpPassword, smtpSsl);
    }
}
