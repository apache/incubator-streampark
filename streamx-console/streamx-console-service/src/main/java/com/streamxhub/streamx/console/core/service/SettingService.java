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

package com.streamxhub.streamx.console.core.service;

import com.streamxhub.streamx.console.core.entity.SenderEmail;
import com.streamxhub.streamx.console.core.entity.Setting;

import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @author benjobs
 */
public interface SettingService extends IService<Setting> {

    String KEY_MAVEN_REPOSITORY = "streamx.maven.central.repository";
    String KEY_MAVEN_AUTH_USER = "streamx.maven.auth.user";
    String KEY_MAVEN_AUTH_PASSWORD = "streamx.maven.auth.password";

    String KEY_STREAMX_ADDRESS = "streamx.console.webapp.address";

    String KEY_ALERT_EMAIL_HOST = "alert.email.host";
    String KEY_ALERT_EMAIL_PORT = "alert.email.port";
    String KEY_ALERT_EMAIL_FROM = "alert.email.from";
    String KEY_ALERT_EMAIL_USERNAME = "alert.email.userName";
    String KEY_ALERT_EMAIL_PASSWORD = "alert.email.password";
    String KEY_ALERT_EMAIL_SSL = "alert.email.ssl";

    String KEY_DOCKER_REGISTER_ADDRESS = "docker.register.address";
    String KEY_DOCKER_REGISTER_USER = "docker.register.user";
    String KEY_DOCKER_REGISTER_PASSWORD = "docker.register.password";

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

    String getStreamXAddress();

    String getMavenRepository();

    String getMavenAuthUser();

    String getMavenAuthPassword();

    SenderEmail getSenderEmail();

    String getDockerRegisterAddress();

    String getDockerRegisterUser();

    String getDockerRegisterPassword();

}
