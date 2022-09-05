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

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class SettingServiceImpl extends ServiceImpl<SettingMapper, Setting>
    implements SettingService {

    @Override
    public Setting get(String key) {
        return baseMapper.get(key);
    }

    private final Map<String, Setting> settings = new ConcurrentHashMap<>();

    private final Setting defaultSetting = new Setting();

    @PostConstruct
    public void initSetting() {
        List<Setting> settingList = super.list();
        settingList.forEach(x -> settings.put(x.getSettingKey(), x));
    }

    @Override
    public boolean update(Setting setting) {
        try {
            String value = setting.getSettingValue();
            if (value != null) {
                if (StringUtils.isEmpty(value.trim())) {
                    value = null;
                } else {
                    value = setting.getSettingValue().trim();
                }
            }
            setting.setSettingValue(value);
            this.baseMapper.updateByKey(setting);

            if (setting.getSettingKey().equals(CommonConfig.MAVEN_REMOTE_URL().key())) {
                InternalConfigHolder.set(CommonConfig.MAVEN_REMOTE_URL(), value);
            }
            if (setting.getSettingKey().equals(CommonConfig.MAVEN_AUTH_USER().key())) {
                InternalConfigHolder.set(CommonConfig.MAVEN_AUTH_USER(), value);
            }
            if (setting.getSettingKey().equals(CommonConfig.MAVEN_AUTH_PASSWORD().key())) {
                InternalConfigHolder.set(CommonConfig.MAVEN_AUTH_PASSWORD(), value);
            }
            settings.get(setting.getSettingKey()).setSettingValue(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public SenderEmail getSenderEmail() {
        try {
            String host = settings.get(SettingService.KEY_ALERT_EMAIL_HOST).getSettingValue();
            String port = settings.get(SettingService.KEY_ALERT_EMAIL_PORT).getSettingValue();
            String from = settings.get(SettingService.KEY_ALERT_EMAIL_FROM).getSettingValue();
            String userName = settings.get(SettingService.KEY_ALERT_EMAIL_USERNAME).getSettingValue();
            String password = settings.get(SettingService.KEY_ALERT_EMAIL_PASSWORD).getSettingValue();
            String ssl = settings.get(SettingService.KEY_ALERT_EMAIL_SSL).getSettingValue();

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
        return settings.getOrDefault(SettingService.KEY_DOCKER_REGISTER_ADDRESS, defaultSetting).getSettingValue();
    }

    @Override
    public String getDockerRegisterUser() {
        return settings.getOrDefault(SettingService.KEY_DOCKER_REGISTER_USER, defaultSetting).getSettingValue();
    }

    @Override
    public String getDockerRegisterPassword() {
        return settings.getOrDefault(SettingService.KEY_DOCKER_REGISTER_PASSWORD, defaultSetting).getSettingValue();
    }

    @Override
    public String getDockerRegisterNamespace() {
        return settings.getOrDefault(SettingService.KEY_DOCKER_REGISTER_NAMESPACE, defaultSetting).getSettingValue();
    }

    @Override
    public String getStreamParkAddress() {
        return settings.getOrDefault(SettingService.KEY_STREAMPARK_ADDRESS, defaultSetting).getSettingValue();
    }

    @Override
    public String getMavenRepository() {
        return settings.getOrDefault(SettingService.KEY_MAVEN_REPOSITORY, defaultSetting).getSettingValue();
    }

    @Override
    public String getMavenAuthUser() {
        return settings.getOrDefault(SettingService.KEY_MAVEN_AUTH_USER, defaultSetting).getSettingValue();
    }

    @Override
    public String getMavenAuthPassword() {
        return settings.getOrDefault(SettingService.KEY_MAVEN_AUTH_PASSWORD, defaultSetting).getSettingValue();
    }

}
