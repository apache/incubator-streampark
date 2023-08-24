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

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * The EmailConfig class represents the configuration for an email system. It holds the SMTP host,
 * port, from address, username, password, and whether SSL is enabled.
 *
 * <p>This class also provides a static factory method to create an EmailConfig object from a map of
 * settings.
 */
@Data
@Slf4j
public class EmailConfig {

  public static final String KEY_ALERT_EMAIL_HOST = "alert.email.host";
  public static final String KEY_ALERT_EMAIL_PORT = "alert.email.port";
  public static final String KEY_ALERT_EMAIL_FROM = "alert.email.from";
  public static final String KEY_ALERT_EMAIL_USERNAME = "alert.email.userName";
  public static final String KEY_ALERT_EMAIL_PASSWORD = "alert.email.password";
  public static final String KEY_ALERT_EMAIL_SSL = "alert.email.ssl";

  private String smtpHost;
  private Integer smtpPort;
  private String from;
  private String userName;
  private String password;
  private boolean ssl;

  /**
   * Constructs the EmailConfig object from the given settings map.
   *
   * @param settingMap a map of settings
   * @return a new EmailConfig object that has its fields set according to the provided settings
   */
  public static EmailConfig fromSetting(Map<String, Setting> settingMap) {
    try {
      EmailConfig emailConfig = new EmailConfig();
      emailConfig.setSmtpHost(settingMap.get(KEY_ALERT_EMAIL_HOST).getSettingValue());
      emailConfig.setSmtpPort(
          Integer.valueOf(settingMap.get(KEY_ALERT_EMAIL_PORT).getSettingValue()));
      emailConfig.setFrom(settingMap.get(KEY_ALERT_EMAIL_FROM).getSettingValue());
      emailConfig.setUserName(settingMap.get(KEY_ALERT_EMAIL_USERNAME).getSettingValue());
      emailConfig.setPassword(settingMap.get(KEY_ALERT_EMAIL_PASSWORD).getSettingValue());
      emailConfig.setSsl(
          Boolean.parseBoolean(settingMap.get(KEY_ALERT_EMAIL_SSL).getSettingValue()));
      return emailConfig;
    } catch (Exception e) {
      log.warn("Failed to create EmailConfig from settings", e);
    }
    return null;
  }
}
