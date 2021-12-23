/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.console.core.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.streamxhub.streamx.console.core.dao.SettingMapper;
import com.streamxhub.streamx.console.core.entity.SenderEmail;
import com.streamxhub.streamx.console.core.entity.SenderSMS;
import com.streamxhub.streamx.console.core.entity.Setting;
import com.streamxhub.streamx.console.core.service.SettingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author benjobs
 */
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

    @PostConstruct
    public void initSetting() {
        List<Setting> settingList = super.list();
        settingList.forEach(x -> settings.put(x.getKey(), x));
    }

    @Override
    public boolean update(Setting setting) {
        try {
            if (setting.getValue() != null) {
                setting.setValue(setting.getValue().trim());
            }
            this.baseMapper.updateByKey(setting);
            settings.get(setting.getKey()).setValue(setting.getValue());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public SenderEmail getSenderEmail() {
        try {
            String host = settings.get(SettingService.KEY_ALERT_EMAIL_HOST).getValue();
            String port = settings.get(SettingService.KEY_ALERT_EMAIL_PORT).getValue();
            String from = settings.get(SettingService.KEY_ALERT_EMAIL_FROM).getValue();
            String userName = settings.get(SettingService.KEY_ALERT_EMAIL_USERNAME).getValue();
            String password = settings.get(SettingService.KEY_ALERT_EMAIL_PASSWORD).getValue();
            String ssl = settings.get(SettingService.KEY_ALERT_EMAIL_SSL).getValue();

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
        return settings.get(SettingService.KEY_DOCKER_REGISTER_ADDRESS).getValue();
    }

    @Override
    public String getDockerRegisterUser() {
        return settings.get(SettingService.KEY_DOCKER_REGISTER_USER).getValue();
    }

    @Override
    public String getDockerRegisterPassword() {
        return settings.get(SettingService.KEY_DOCKER_REGISTER_PASSWORD).getValue();
    }

    @Override
    public String getStreamXAddress() {
        return settings.get(SettingService.KEY_STREAMX_ADDRESS).getValue();
    }

    @Override
    public String getMavenRepository() {
        return settings.get(SettingService.KEY_MAVEN_REPOSITORY).getValue();
    }

    @Override
    public SenderSMS getSenderSMS() {
        try {
            String number = settings.get(SettingService.KEY_ALERT_PHONE_NUMBER).getValue();
            String host = settings.get(SettingService.KEY_ALERT_PHONE_HOST).getValue();
            SenderSMS senderSMS = new SenderSMS();
            senderSMS.setPhoneNumber(number);
            senderSMS.setPhoneHost(host);
            return senderSMS;
        } catch (Exception e) {
            log.warn("Fault Alert SMS is not set.");
        }
        return null;
    }
}
