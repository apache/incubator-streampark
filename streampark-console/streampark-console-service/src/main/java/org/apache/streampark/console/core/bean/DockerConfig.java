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
import org.apache.streampark.console.core.service.SettingService;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

/**
 * The DockerConfig class represents the configuration for docker system. It holds Registered
 * address, username, password, and namespace.
 *
 * <p>This class also provides a static factory method to create an DockerConfig object from a map
 * of settings.
 */
@Getter
@Setter
@Slf4j
public class DockerConfig {

    private String address;
    private String username;
    private String password;
    private String namespace;

    public static List<Setting> toSettings(DockerConfig dockerConfig) {
        Setting address = new Setting();
        address.setSettingKey(SettingService.KEY_DOCKER_REGISTER_ADDRESS);
        address.setSettingValue(dockerConfig.getAddress());

        Setting username = new Setting();
        username.setSettingKey(SettingService.KEY_DOCKER_REGISTER_USER);
        username.setSettingValue(dockerConfig.getUsername());

        Setting password = new Setting();
        password.setSettingKey(SettingService.KEY_DOCKER_REGISTER_PASSWORD);
        password.setSettingValue(dockerConfig.getPassword());

        Setting namespace = new Setting();
        namespace.setSettingKey(SettingService.KEY_DOCKER_REGISTER_NAMESPACE);
        namespace.setSettingValue(dockerConfig.getNamespace());
        return Arrays.asList(address, username, password, namespace);
    }
}
