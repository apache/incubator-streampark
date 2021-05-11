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
package com.streamxhub.streamx.console.core.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.streamxhub.streamx.console.core.entity.SenderEmail;
import com.streamxhub.streamx.console.core.entity.Setting;

import java.io.IOException;
import java.util.Map;

/**
 * @author benjobs
 */
public interface SettingService extends IService<Setting> {

    String KEY_MAVEN_REPOSITORY = "maven.central.repository";
    String KEY_STREAMX_ADDRESS = "streamx.console.webapp.address";
    String KEY_STREAMX_WORKSPACE = "streamx.console.workspace";

    String KEY_ALERT_EMAIL_HOST = "alert.email.host";
    String KEY_ALERT_EMAIL_PORT = "alert.email.port";
    String KEY_ALERT_EMAIL_ADDRESS = "alert.email.address";
    String KEY_ALERT_EMAIL_PASSWORD = "alert.email.password";
    String KEY_ALERT_EMAIL_SSL = "alert.email.ssl";

    String KEY_ENV_FLINK_HOME = "env.flink.home";

    /**
     * @param key
     * @return
     */
    Setting get(String key);

    /**
     * @param setting
     * @return
     */
    boolean update(Setting setting);

    String getStreamXWorkspace();

    String getStreamXAddress();

    String getMavenRepository();

    String getEffectiveFlinkHome();

    Map<String, String> getFlinkDefaultConfig();

    boolean checkWorkspace();

    SenderEmail getSenderEmail();

    Setting getFlink() throws IOException;

    String getFlinkYaml();

    void syncFlinkConf() throws IOException;
}
