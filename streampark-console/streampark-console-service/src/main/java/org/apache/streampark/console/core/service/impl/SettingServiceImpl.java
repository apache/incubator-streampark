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

package org.apache.streampark.console.core.service.impl;

import org.apache.streampark.console.core.bean.EmailConfig;
import org.apache.streampark.console.core.bean.MavenConfig;
import org.apache.streampark.console.core.entity.Setting;
import org.apache.streampark.console.core.mapper.SettingMapper;
import org.apache.streampark.console.core.service.SettingService;

import org.apache.commons.lang3.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class SettingServiceImpl extends ServiceImpl<SettingMapper, Setting>
    implements SettingService, ApplicationListener<ContextRefreshedEvent> {

  @Override
  public Setting get(String key) {
    LambdaQueryWrapper<Setting> queryWrapper =
        new LambdaQueryWrapper<Setting>().eq(Setting::getSettingKey, key);
    return this.getOne(queryWrapper);
  }

  private final Setting emptySetting = new Setting();

  @Override
  public void onApplicationEvent(ContextRefreshedEvent event) {
    List<Setting> settingList = super.list();
    settingList.forEach(x -> SETTINGS.put(x.getSettingKey(), x));
  }

  @Override
  public boolean update(Setting setting) {
    try {
      String value = StringUtils.trimToNull(setting.getSettingValue());
      setting.setSettingValue(value);

      Setting entity = new Setting();
      entity.setSettingValue(setting.getSettingValue());
      LambdaQueryWrapper<Setting> queryWrapper =
          new LambdaQueryWrapper<Setting>().eq(Setting::getSettingKey, setting.getSettingKey());
      this.update(entity, queryWrapper);

      getMavenConfig().updateMavenInternalConfig();

      Optional<Setting> optional = Optional.ofNullable(SETTINGS.get(setting.getSettingKey()));
      optional.ifPresent(x -> x.setSettingValue(value));
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  @Override
  public EmailConfig getSenderEmail() {
    return EmailConfig.fromSetting(SETTINGS);
  }

  @Override
  public String getDockerRegisterAddress() {
    return SETTINGS
        .getOrDefault(SettingService.KEY_DOCKER_REGISTER_ADDRESS, emptySetting)
        .getSettingValue();
  }

  @Override
  public String getDockerRegisterUser() {
    return SETTINGS
        .getOrDefault(SettingService.KEY_DOCKER_REGISTER_USER, emptySetting)
        .getSettingValue();
  }

  @Override
  public String getDockerRegisterPassword() {
    return SETTINGS
        .getOrDefault(SettingService.KEY_DOCKER_REGISTER_PASSWORD, emptySetting)
        .getSettingValue();
  }

  @Override
  public String getDockerRegisterNamespace() {
    return SETTINGS
        .getOrDefault(SettingService.KEY_DOCKER_REGISTER_NAMESPACE, emptySetting)
        .getSettingValue();
  }

  @Override
  public String getStreamParkAddress() {
    return SETTINGS
        .getOrDefault(SettingService.KEY_STREAMPARK_ADDRESS, emptySetting)
        .getSettingValue();
  }

  @Override
  public MavenConfig getMavenConfig() {
    return MavenConfig.fromSetting(SETTINGS);
  }

  @Override
  public String getIngressModeDefault() {
    return SETTINGS
        .getOrDefault(SettingService.KEY_INGRESS_MODE_DEFAULT, emptySetting)
        .getSettingValue();
  }
}
