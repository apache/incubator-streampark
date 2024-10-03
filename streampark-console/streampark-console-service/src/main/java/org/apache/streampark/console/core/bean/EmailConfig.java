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

import org.apache.streampark.console.core.service.SettingService;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * The EmailConfig class represents the configuration for an email system. It holds the SMTP host,
 * port, from address, username, password, and whether SSL is enabled.
 *
 * <p>This class also provides a static factory method to create an EmailConfig object from a map of
 * settings.
 */
@Getter
@Setter
@Slf4j
public class EmailConfig {

    private String smtpHost;
    private Integer smtpPort;
    private String from;
    private String userName;
    private String password;
    private boolean ssl;

    /**
     * Constructs the EmailConfig object from the given settings map.
     *
     * @return a new EmailConfig object that has its fields set according to the provided settings
     */
    public static EmailConfig fromSetting() {
        try {
            EmailConfig emailConfig = new EmailConfig();

            emailConfig.setSmtpHost(
                SettingService.SETTINGS.get(SettingService.KEY_ALERT_EMAIL_HOST).getSettingValue());

            emailConfig.setSmtpPort(
                Integer.valueOf(
                    SettingService.SETTINGS.get(SettingService.KEY_ALERT_EMAIL_PORT)
                        .getSettingValue()));

            emailConfig.setFrom(
                SettingService.SETTINGS.get(SettingService.KEY_ALERT_EMAIL_FROM).getSettingValue());

            emailConfig.setUserName(
                SettingService.SETTINGS.get(SettingService.KEY_ALERT_EMAIL_USERNAME).getSettingValue());

            emailConfig.setPassword(
                SettingService.SETTINGS.get(SettingService.KEY_ALERT_EMAIL_PASSWORD).getSettingValue());

            emailConfig.setSsl(
                Boolean.parseBoolean(
                    SettingService.SETTINGS.get(SettingService.KEY_ALERT_EMAIL_SSL)
                        .getSettingValue()));

            return emailConfig;
        } catch (Exception e) {
            log.warn("Failed to create EmailConfig from settings", e);
        }
        return null;
    }
}
