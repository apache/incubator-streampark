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

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;
import java.util.regex.Pattern;

@Getter
@Setter
public class SettingAlertEmailConfigParams {
  private Setting host;
  private Setting port;
  private Setting from;
  private Setting username;
  private Setting password;
  private Setting ssl;
  /* Only letters, digits, underscores, periods, and hyphens are allowed */
  private static final String EMAIL_ADDRESS_REGEXP =
      "^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$";
  /* The port value must be between 0-65535 */
  private static final String PORT_REGEXP =
      "^([1-9]\\d{0,3}|[1-5]\\d{4}|6[0-4]\\d{3}|65[0-4]\\d{2}|655[0-2]\\d|6553[0-5])$";
  private static final String FROM_REGEXP = "";
  /* Letters start with 5-16 bytes, alphanumeric underscores are allowed */
  private static final String USER_NAME_REGEXP = "^[a-zA-Z][a-zA-Z0-9_]{4,15}$";
  /* Letters start with 5-16 bytes, alphanumeric underscores are allowed */
  private static final String PASS_WORD_REGEXP = "^[a-zA-Z][a-zA-Z0-9_]{4,15}$";
  /* Whether SSL is enabled or not can only be true false */
  private static final String SSL_REGEXP = "^(true|false)$";

  public static boolean verifyParams(SettingAlertEmailConfigParams params) {
    return params.verifyHost()
        && params.verifyPort()
        && params.verifyFrom()
        && params.verifyUserName()
        && params.verifyPassWord()
        && params.verifySSL();
  }

  private boolean verifyHost() {
    if (Objects.isNull(getHost()) || Objects.isNull(getHost().getSettingValue())) {
      return false;
    }
    return Pattern.matches(EMAIL_ADDRESS_REGEXP, getHost().getSettingValue());
  }

  private boolean verifyPort() {
    if (Objects.isNull(getPort()) || Objects.isNull(getPort().getSettingValue())) {
      return false;
    }
    return Pattern.matches(PORT_REGEXP, getPort().getSettingValue());
  }

  private boolean verifyFrom() {
    if (Objects.isNull(getFrom()) || Objects.isNull(getFrom().getSettingValue())) {
      return false;
    }
    return Pattern.matches(FROM_REGEXP, getFrom().getSettingValue());
  }

  private boolean verifyUserName() {
    if (Objects.isNull(getUsername()) || Objects.isNull(getUsername().getSettingValue())) {
      return false;
    }
    return Pattern.matches(USER_NAME_REGEXP, getUsername().getSettingValue());
  }

  private boolean verifyPassWord() {
    if (Objects.isNull(getPassword()) || Objects.isNull(getPassword().getSettingValue())) {
      return false;
    }
    return Pattern.matches(PASS_WORD_REGEXP, getPassword().getSettingValue());
  }

  private boolean verifySSL() {
    if (Objects.isNull(getSsl()) || Objects.isNull(getSsl().getSettingValue())) {
      return false;
    }
    return Pattern.matches(SSL_REGEXP, getSsl().getSettingValue());
  }
}
