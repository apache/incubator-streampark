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

import org.apache.streampark.common.conf.CommonConfig;
import org.apache.streampark.common.conf.InternalConfigHolder;
import org.apache.streampark.console.core.bean.SenderEmail;
import org.apache.streampark.console.core.entity.Setting;
import org.apache.streampark.console.core.mapper.SettingMapper;
import org.apache.streampark.console.core.service.SettingService;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
        LambdaQueryWrapper<Setting> queryWrapper = new LambdaQueryWrapper<Setting>()
            .eq(Setting::getSettingKey, key);
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
            LambdaQueryWrapper<Setting> queryWrapper = new LambdaQueryWrapper<Setting>()
                .eq(Setting::getSettingKey, setting.getSettingKey());
            this.update(entity, queryWrapper);

            String settingKey = setting.getSettingKey();
            if (CommonConfig.MAVEN_REMOTE_URL().key().equals(settingKey)) {
                InternalConfigHolder.set(CommonConfig.MAVEN_REMOTE_URL(), value);
            }
            if (CommonConfig.MAVEN_AUTH_USER().key().equals(settingKey)) {
                InternalConfigHolder.set(CommonConfig.MAVEN_AUTH_USER(), value);
            }
            if (CommonConfig.MAVEN_AUTH_PASSWORD().key().equals(settingKey)) {
                InternalConfigHolder.set(CommonConfig.MAVEN_AUTH_PASSWORD(), value);
            }

            Optional<Setting> optional = Optional.ofNullable(SETTINGS.get(setting.getSettingKey()));
            if (optional.isPresent()) {
                optional.get().setSettingValue(value);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public SenderEmail getSenderEmail() {
        try {
            String host = SETTINGS.get(SettingService.KEY_ALERT_EMAIL_HOST).getSettingValue();
            String port = SETTINGS.get(SettingService.KEY_ALERT_EMAIL_PORT).getSettingValue();
            String from = SETTINGS.get(SettingService.KEY_ALERT_EMAIL_FROM).getSettingValue();
            String userName = SETTINGS.get(SettingService.KEY_ALERT_EMAIL_USERNAME).getSettingValue();
            String password = SETTINGS.get(SettingService.KEY_ALERT_EMAIL_PASSWORD).getSettingValue();
            String ssl = SETTINGS.get(SettingService.KEY_ALERT_EMAIL_SSL).getSettingValue();

            SenderEmail senderEmail = new SenderEmail();
            senderEmail.setSmtpHost(host);
            senderEmail.setSmtpPort(Integer.parseInt(port));
            senderEmail.setFrom(from);
            senderEmail.setUserName(userName);
            senderEmail.setPassword(password);
            senderEmail.setSsl(Boolean.parseBoolean(ssl));
            return senderEmail;
        } catch (Exception e) {
            log.warn("Fault Alert Email is not set.");
        }
        return null;
    }

    @Override
    public String getDockerRegisterAddress() {
        return SETTINGS.getOrDefault(SettingService.KEY_DOCKER_REGISTER_ADDRESS, emptySetting).getSettingValue();
    }

    @Override
    public String getDockerRegisterUser() {
        return SETTINGS.getOrDefault(SettingService.KEY_DOCKER_REGISTER_USER, emptySetting).getSettingValue();
    }

    @Override
    public String getDockerRegisterPassword() {
        return SETTINGS.getOrDefault(SettingService.KEY_DOCKER_REGISTER_PASSWORD, emptySetting).getSettingValue();
    }

    @Override
    public String getDockerRegisterNamespace() {
        return SETTINGS.getOrDefault(SettingService.KEY_DOCKER_REGISTER_NAMESPACE, emptySetting).getSettingValue();
    }

    @Override
    public String getStreamParkAddress() {
        return SETTINGS.getOrDefault(SettingService.KEY_STREAMPARK_ADDRESS, emptySetting).getSettingValue();
    }

    @Override
    public String getMavenSettings() {
        return SETTINGS.getOrDefault(SettingService.KEY_MAVEN_SETTINGS, emptySetting).getSettingValue();
    }

    @Override
    public String getMavenRepository() {
        return SETTINGS.getOrDefault(SettingService.KEY_MAVEN_REPOSITORY, emptySetting).getSettingValue();
    }

    @Override
    public String getMavenAuthUser() {
        return SETTINGS.getOrDefault(SettingService.KEY_MAVEN_AUTH_USER, emptySetting).getSettingValue();
    }

    @Override
    public String getMavenAuthPassword() {
        return SETTINGS.getOrDefault(SettingService.KEY_MAVEN_AUTH_PASSWORD, emptySetting).getSettingValue();
    }

    @Override
    public String getIngressModeDefault() {
        return SETTINGS.getOrDefault(SettingService.KEY_INGRESS_MODE_DEFAULT, emptySetting).getSettingValue();
    }
}
