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
package org.apache.streampark.console.core.service;

import org.apache.streampark.console.SpringUnitTestBase;
import org.apache.streampark.console.core.bean.DockerConfig;
import org.apache.streampark.console.core.bean.ResponseResult;
import org.apache.streampark.console.core.bean.SenderEmail;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Disabled("'ese test cases can't be runnable due to external service is not available.")
class SettingServiceTest extends SpringUnitTestBase {

    @Autowired
    SettingService settingService;

    @Test
    void testUpdateDockerConfigTest() {
        DockerConfig dockerConfig = new DockerConfig();
        dockerConfig.setUsername("test-username-setting-value");
        dockerConfig.setPassword("test-password-setting-value");
        dockerConfig.setNamespace("test-namespace-setting-value");
        dockerConfig.setAddress("test-address-setting-value");

        settingService.updateDocker(dockerConfig);

        Assertions.assertEquals(
            "test-address-setting-value",
            settingService.get(SettingService.KEY_DOCKER_REGISTER_ADDRESS).getSettingValue());
        Assertions.assertEquals(
            "test-username-setting-value",
            settingService.get(SettingService.KEY_DOCKER_REGISTER_USER).getSettingValue());
        Assertions.assertEquals(
            "test-password-setting-value",
            settingService.get(SettingService.KEY_DOCKER_REGISTER_PASSWORD).getSettingValue());
        Assertions.assertEquals(
            "test-namespace-setting-value",
            settingService.get(SettingService.KEY_DOCKER_REGISTER_NAMESPACE).getSettingValue());
    }

    @Test
    void testUpdateAlertEmailConfigTest() {
        SenderEmail senderEmail = new SenderEmail();
        senderEmail.setHost("test-host-setting-value");
        senderEmail.setUserName("test-username-setting-value");
        senderEmail.setPassword("test-password-setting-value");
        senderEmail.setFrom("test-from-setting-value");
        senderEmail.setSsl(true);
        senderEmail.setPort(456);

        settingService.updateEmail(senderEmail);

        Assertions.assertEquals(
            "test-host-setting-value",
            settingService.get(SettingService.KEY_ALERT_EMAIL_HOST).getSettingValue());
        Assertions.assertEquals(
            "test-from-setting-value",
            settingService.get(SettingService.KEY_ALERT_EMAIL_FROM).getSettingValue());
        Assertions.assertEquals(
            "test-username-setting-value",
            settingService.get(SettingService.KEY_ALERT_EMAIL_USERNAME).getSettingValue());
        Assertions.assertEquals(
            "test-password-setting-value",
            settingService.get(SettingService.KEY_ALERT_EMAIL_PASSWORD).getSettingValue());
        Assertions.assertEquals(
            "456", settingService.get(SettingService.KEY_ALERT_EMAIL_PORT).getSettingValue());
        Assertions.assertEquals(
            "true", settingService.get(SettingService.KEY_ALERT_EMAIL_SSL).getSettingValue());
    }

    @Test
    void checkEmailTest() {
        SenderEmail senderEmail = new SenderEmail();
        senderEmail.setHost("smtp.163.com");
        senderEmail.setUserName("XXXXXXXX@163.com");
        senderEmail.setPassword("XXXXXXXX");
        senderEmail.setFrom("XXXXXXXX@163.com");
        senderEmail.setSsl(false);
        senderEmail.setPort(25);
        ResponseResult result = settingService.checkEmail(senderEmail);
        Assertions.assertEquals(result.getStatus(), 200);
    }

    @Test
    void checkDockerTest() {
        String username = "XXXXXXXX ";
        String password = "XXXXXXXX";

        DockerConfig dockerConfig = new DockerConfig();
        dockerConfig.setAddress("registry.cn-hangzhou.aliyuncs.com");
        dockerConfig.setUsername(username);
        dockerConfig.setPassword(password);
        dockerConfig.setNamespace("streampark");

        ResponseResult result = settingService.checkDocker(dockerConfig);
        Assertions.assertEquals(result.getStatus(), 200);
    }

    @Test
    void testGetDockerConfig() {
        String username = "XXXXXXXX";
        String password = "XXXXXXXX";

        DockerConfig initDockerConfig = new DockerConfig();
        initDockerConfig.setAddress("registry.cn-hangzhou.aliyuncs.com");
        initDockerConfig.setUsername(username);
        initDockerConfig.setPassword(password);
        initDockerConfig.setNamespace("streampark");
        settingService.updateDocker(initDockerConfig);

        DockerConfig dockerConfig = settingService.getDockerConfig();
        Assertions.assertEquals(dockerConfig, initDockerConfig);
    }
}
