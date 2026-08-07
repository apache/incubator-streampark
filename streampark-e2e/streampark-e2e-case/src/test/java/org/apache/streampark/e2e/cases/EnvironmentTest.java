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

package org.apache.streampark.e2e.cases;

import org.apache.streampark.e2e.core.StreamParkApi;
import org.apache.streampark.e2e.core.api.ApiClient;
import org.apache.streampark.e2e.core.api.ApiResponse;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@StreamParkApi(composeFiles = "docker/environment/docker-compose.yaml")
public class EnvironmentTest {

    public static ApiClient api;

    private static final String mavenFilePath = "/maven/file/path";
    private static final String mavenCentralRepository = "https://mvnrepository.com/";
    private static final String mavenAuthUser = "maven_user";
    private static final String mavenAuthPassword = "maven_password";
    private static final String ingressDomainAddress = "https://localhost";
    private static final String dockerAddress = "https://hub.docker.com/v2/";
    private static final String dockerNamespace = "hello";
    private static final String dockerUser = "docker_user";
    private static final String dockerPassword = "docker_password";
    private static final String emailHost = "smtp.163.com";
    private static final String editEmailHost = "postfix";
    private static final String emailPort = "25";
    private static final String emailAddress = "hello@163.com";
    private static final String editEmailAddress = "hello@postfix.com";
    private static final String emailUser = "email_password";
    private static final String emailPassword = "email_password";

    @BeforeAll
    public static void setup() {
        api.login();
    }

    @Test
    @Order(1)
    public void testCreateEnvironment() {
        updateSetting("streampark.maven.settings", mavenFilePath);
        updateSetting("streampark.maven.central.repository", mavenCentralRepository);
        updateSetting("streampark.maven.auth.user", mavenAuthUser);
        updateSetting("streampark.maven.auth.password", mavenAuthPassword);
        updateSetting("ingress.mode.default", ingressDomainAddress);

        ApiResponse list = api.postForm("/setting/all", new LinkedHashMap<>());
        assertThat(list.isSuccess()).isTrue();
        String settings = list.getData().toString();
        assertThat(settings).contains(mavenFilePath);
        assertThat(settings).contains(mavenCentralRepository);
        assertThat(settings).contains(mavenAuthUser);
        assertThat(settings).contains(ingressDomainAddress);
    }

    @Test
    @Order(2)
    public void testCreateEmailSettingFailedWithAuth() {
        ApiResponse response = api.postForm("/setting/check/email", emailParams(emailHost, emailAddress));
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().path("msg").asText())
            .contains("connect to target mail server failed: 535 Error: authentication failed");
    }

    @Test
    @Order(3)
    public void testCreateEmailSettingSuccessful() {
        ApiResponse check = api.postForm("/setting/check/email", emailParams(editEmailHost, editEmailAddress));
        assertThat(check.isSuccess()).isTrue();

        ApiResponse update = api.postForm("/setting/update/email", emailParams(editEmailHost, editEmailAddress));
        assertThat(update.isSuccess()).isTrue();

        ApiResponse list = api.postForm("/setting/all", new LinkedHashMap<>());
        assertThat(list.getData().toString()).contains(editEmailAddress);
    }

    @Test
    @Order(4)
    public void testCreateDockerSettingFailed() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("address", dockerAddress);
        params.put("namespace", dockerNamespace);
        params.put("username", dockerUser);
        params.put("password", dockerPassword);

        ApiResponse response = api.postForm("/setting/check/docker", params);
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().path("msg").asText())
            .contains(
                String.format(
                    "Failed to validate Docker registry, error: Status 500: {\"message\":\"login attempt to %s failed with status: 404 Not Found\"}",
                    dockerAddress));
    }

    private static void updateSetting(String key, String value) {
        ApiResponse response =
            api.postForm("/setting/update", api.params("settingKey", key, "settingValue", value));
        assertThat(response.isSuccess()).isTrue();
    }

    private static Map<String, String> emailParams(String host, String from) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("host", host);
        params.put("port", emailPort);
        params.put("from", from);
        params.put("userName", emailUser);
        params.put("password", emailPassword);
        params.put("ssl", "false");
        return params;
    }
}
