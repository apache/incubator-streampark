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

import org.apache.streampark.common.conf.CommonConfig;
import org.apache.streampark.common.conf.InternalConfigHolder;
import org.apache.streampark.console.core.entity.Setting;
import org.apache.streampark.console.core.service.SettingService;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * This class represents the Maven configuration for the application. It provides methods to
 * retrieve the various Maven configuration options.
 */
@Getter
@Setter
public class MavenConfig {

  /** File path for Maven settings. */
  private String mvnSettings;

  /** Repository URL for Maven. */
  private String mvnRepository;

  /** User for Maven authentication. */
  private String mvnAuthUser;

  /** Password for Maven authentication. */
  private String mvnAuthPassword;

  /** */
  public static MavenConfig fromSetting(Setting settings) {
    MavenConfig mavenConfig = new MavenConfig();
    String value = settings.getSettingValue();
    switch (settings.getSettingKey()) {
      case SettingService.KEY_MAVEN_SETTINGS:
        mavenConfig.setMvnSettings(value);
        break;
      case SettingService.KEY_MAVEN_REPOSITORY:
        mavenConfig.setMvnRepository(value);
        break;
      case SettingService.KEY_MAVEN_AUTH_USER:
        mavenConfig.setMvnAuthUser(value);
        break;
      case SettingService.KEY_MAVEN_AUTH_PASSWORD:
        mavenConfig.setMvnAuthPassword(value);
        break;
      default:
        break;
    }
    return mavenConfig;
  }

  public static MavenConfig fromSetting() {
    MavenConfig mavenConfig = new MavenConfig();

    Map<String, Setting> settings = SettingService.SETTINGS;
    Setting emptySetting = new Setting();

    mavenConfig.setMvnSettings(
        settings.getOrDefault(SettingService.KEY_MAVEN_SETTINGS, emptySetting).getSettingValue());

    mavenConfig.setMvnRepository(
        settings.getOrDefault(SettingService.KEY_MAVEN_REPOSITORY, emptySetting).getSettingValue());

    mavenConfig.setMvnAuthUser(
        settings.getOrDefault(SettingService.KEY_MAVEN_AUTH_USER, emptySetting).getSettingValue());

    mavenConfig.setMvnAuthPassword(
        settings
            .getOrDefault(SettingService.KEY_MAVEN_AUTH_PASSWORD, emptySetting)
            .getSettingValue());

    return mavenConfig;
  }

  /**
   * Updates the internal configuration of Maven based on the assigned instance variables. If the
   * instance variables mvnSettings, mvnRepository, mvnAuthUser, or mvnAuthPassword have non-empty
   * values, they will be updated in the internal configuration.
   */
  public void updateConfig() {
    if (StringUtils.isNotBlank(mvnSettings)) {
      InternalConfigHolder.set(CommonConfig.MAVEN_SETTINGS_PATH(), mvnSettings);
    }

    if (StringUtils.isNotBlank(mvnRepository)) {
      InternalConfigHolder.set(CommonConfig.MAVEN_REMOTE_URL(), mvnRepository);
    }

    if (StringUtils.isNotBlank(mvnAuthUser)) {
      InternalConfigHolder.set(CommonConfig.MAVEN_AUTH_USER(), mvnAuthUser);
    }

    if (StringUtils.isNotBlank(mvnAuthPassword)) {
      InternalConfigHolder.set(CommonConfig.MAVEN_AUTH_PASSWORD(), mvnAuthPassword);
    }
  }
}
