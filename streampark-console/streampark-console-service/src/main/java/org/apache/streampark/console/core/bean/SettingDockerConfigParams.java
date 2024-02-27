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
public class SettingDockerConfigParams {
  private Setting username;
  private Setting password;
  private Setting address;
  private Setting namespace;

  /* Letters start with 5-16 bytes, alphanumeric underscores are allowed */
  private static final String USER_NAME_REGEXP = "^[a-zA-Z][a-zA-Z0-9_]{4,15}$";
  /* Letters start with 5-16 bytes, alphanumeric underscores are allowed */
  private static final String PASSWORD_REGEXP = "^[a-zA-Z][a-zA-Z0-9_]{4,15}$";
  /*ipv4 match rule  */
  private static final String IPV4_ADDRESS_REGEX =
      "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
  /*ipv6 match rule */
  private static final String IPV6_ADDRESS_REGEX = "^([0-9a-fA-F]{1,4}:){7}([0-9a-fA-F]{1,4}|:)$";
  /* domain match rule */
  private static final String DOMAIN_NAME_REGEX =
      "^((http://)|(https://))?([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,6}(\\/)";
  /* Docker namespace rules based on the format of the domain name */
  private static final String NAMESPACE_REGEX = "^(?!-)[a-zA-Z0-9-]{1,253}(?<!-)$";

  public static boolean verifyParams(SettingDockerConfigParams params) {
    return params.verifyUserName()
        && params.verifyPassWord()
        && params.verifyAddress()
        && params.verifyNameSpace();
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
    return Pattern.matches(PASSWORD_REGEXP, getPassword().getSettingValue());
  }

  private boolean verifyAddress() {
    if (Objects.isNull(getAddress()) || Objects.isNull(getAddress().getSettingValue())) {
      return false;
    }

    return Pattern.matches(IPV4_ADDRESS_REGEX, getAddress().getSettingValue())
        || Pattern.matches(IPV6_ADDRESS_REGEX, getAddress().getSettingValue())
        || Pattern.matches(DOMAIN_NAME_REGEX, getAddress().getSettingValue());
  }

  private boolean verifyNameSpace() {
    if (Objects.isNull(getNamespace()) || Objects.isNull(getNamespace().getSettingValue())) {
      return false;
    }
    return Pattern.matches(NAMESPACE_REGEX, getNamespace().getSettingValue());
  }
}
