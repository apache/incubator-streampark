/*
 * Copyright (c) 2019 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.streamxhub.streamx.console.core.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.streamxhub.streamx.common.util.CommandUtils;
import com.streamxhub.streamx.common.util.PropertiesUtils;
import com.streamxhub.streamx.common.util.Utils;
import com.streamxhub.streamx.console.core.dao.SettingMapper;
import com.streamxhub.streamx.console.core.entity.SenderEmail;
import com.streamxhub.streamx.console.core.entity.Setting;
import com.streamxhub.streamx.console.core.service.SettingService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author benjobs
 */
@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
@DependsOn({"flyway", "flywayInitializer"})
public class SettingServiceImpl extends ServiceImpl<SettingMapper, Setting>
        implements SettingService {

    private Map<String, String> flinkYamlMap;

    private String flinkYamlString = null;

    private String flinkVersion = null;

    private final Pattern flinkVersionPattern = Pattern.compile("^Version: (.*), Commit ID: (.*)$");

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

    @SneakyThrows
    private void loadDefaultConfig(boolean reload) {
        if (reload || flinkYamlMap == null) {
            String flinkLocalHome = getEffectiveFlinkHome();
            assert flinkLocalHome != null;
            File yaml = new File(flinkLocalHome.concat("/conf/flink-conf.yaml"));
            assert yaml.exists();
            this.flinkYamlString = FileUtils.readFileToString(yaml);
            this.flinkYamlMap = PropertiesUtils.loadFlinkConfYaml(yaml);
        }
    }

    @Override
    public boolean update(Setting setting) {
        try {
            if (setting.getValue() != null) {
                setting.setValue(setting.getValue().trim());
            }
            this.baseMapper.updateByKey(setting);
            settings.get(setting.getKey()).setValue(setting.getValue());
            if (setting.getKey().equals(SettingService.KEY_ENV_FLINK_HOME)) {
                this.flinkVersion = null;
                this.syncFlinkConf();
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String getEnvFlinkHome() {
        String flinkHome = settings.get(SettingService.KEY_ENV_FLINK_HOME).getValue();
        return Utils.isEmpty(flinkHome) ? null : flinkHome;
    }


    @Override
    public boolean checkWorkspace() {
        String workspace = getStreamXWorkspace();
        if (Utils.isEmpty(workspace)) {
            return false;
        }
        File file = new File(workspace);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                return false;
            }
        }
        return file.canRead() && file.canWrite();
    }

    @Override
    public SenderEmail getSenderEmail() {
        try {
            String host = settings.get(SettingService.KEY_ALERT_EMAIL_HOST).getValue();
            String port = settings.get(SettingService.KEY_ALERT_EMAIL_PORT).getValue();
            String email = settings.get(SettingService.KEY_ALERT_EMAIL_ADDRESS).getValue();
            String password = settings.get(SettingService.KEY_ALERT_EMAIL_PASSWORD).getValue();
            String ssl = settings.get(SettingService.KEY_ALERT_EMAIL_SSL).getValue();

            SenderEmail senderEmail = new SenderEmail();
            senderEmail.setSmtpHost(host);
            senderEmail.setSmtpPort(Integer.parseInt(port));
            senderEmail.setEmail(email);
            senderEmail.setPassword(password);
            senderEmail.setSsl(Boolean.parseBoolean(ssl));
            return senderEmail;
        } catch (Exception e) {
            log.warn("Fault Alert Email is not set.");
        }
        return null;
    }

    @Override
    public Setting getFlinkSetting() throws IOException {
        Setting setting = new Setting();
        String flinkHome = getEffectiveFlinkHome();
        assert flinkHome != null;

        File yaml = new File(flinkHome.concat("/conf/flink-conf.yaml"));
        assert yaml.exists();

        String confYaml = FileUtils.readFileToString(yaml);
        setting.setFlinkHome(flinkHome);
        setting.setFlinkConf(confYaml);
        return setting;
    }

    @Override
    public String getFlinkVersion() {
        if (flinkVersion == null) {
            String flinkHome = getEffectiveFlinkHome();
            String libPath = flinkHome.concat("/lib");
            File[] distJar = new File(libPath).listFiles(x -> x.getName().matches("flink-dist_.*\\.jar"));
            if (distJar == null || distJar.length == 0) {
                throw new IllegalArgumentException("[StreamX] can no found flink-dist jar in " + libPath);
            }
            if (distJar.length > 1) {
                throw new IllegalArgumentException("[StreamX] found multiple flink-dist jar in " + libPath);
            }
            List<String> cmd = Arrays.asList(
                    "cd ".concat(flinkHome),
                    String.format(
                            "java -classpath %s org.apache.flink.client.cli.CliFrontend --version",
                            distJar[0].getAbsolutePath()
                    )
            );

            CommandUtils.execute(cmd, versionInfo -> {
                Matcher matcher = flinkVersionPattern.matcher(versionInfo);
                if (matcher.find()) {
                    log.info("Flink version: {}", versionInfo);
                    flinkVersion = matcher.group(1);
                }
            });
        }
        return flinkVersion;
    }

    @Override
    public Map<String, String> getFlinkDefaultConfig() {
        this.loadDefaultConfig(false);
        return this.flinkYamlMap;
    }

    @Override
    public String getFlinkYaml() {
        this.loadDefaultConfig(false);
        return this.flinkYamlString;
    }

    @Override
    public void syncFlinkConf() {
        this.loadDefaultConfig(true);
    }

    @Override
    public String getEffectiveFlinkHome() {
        return Utils.isEmpty(this.getEnvFlinkHome()) ? System.getenv("FLINK_HOME") : this.getEnvFlinkHome();
    }

    @Override
    public String getStreamXWorkspace() {
        return settings.get(SettingService.KEY_STREAMX_WORKSPACE).getValue();
    }

    @Override
    public String getStreamXAddress() {
        return settings.get(SettingService.KEY_STREAMX_ADDRESS).getValue();
    }

    @Override
    public String getMavenRepository() {
        return settings.get(SettingService.KEY_MAVEN_REPOSITORY).getValue();
    }


}
