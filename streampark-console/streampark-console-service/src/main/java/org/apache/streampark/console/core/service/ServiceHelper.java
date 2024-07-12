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

import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.base.exception.ApiDetailException;
import org.apache.streampark.console.base.util.WebUtils;
import org.apache.streampark.console.core.entity.FlinkEnv;
import org.apache.streampark.console.core.entity.SparkEnv;
import org.apache.streampark.console.system.authentication.JWTUtil;
import org.apache.streampark.console.system.entity.User;
import org.apache.streampark.console.system.service.UserService;
import org.apache.streampark.flink.kubernetes.ingress.IngressController;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ServiceHelper {

    @Autowired
    private SettingService settingService;

    @Autowired
    private UserService userService;

    private String flinkSqlClientJar = null;

    private String sparkSqlClientJar = null;

    public User getLoginUser() {
        String token = (String) SecurityUtils.getSubject().getPrincipal();
        Long userId = JWTUtil.getUserId(token);
        if (userId == null) {
            throw new AuthenticationException("Unauthorized");
        }
        return userService.getById(userId);
    }

    public Long getUserId() {
        User user = getLoginUser();
        if (user != null) {
            return user.getUserId();
        }
        return null;
    }

    public String getFlinkSqlClientJar(FlinkEnv flinkEnv) {
        if (flinkSqlClientJar == null) {
            File localClient = WebUtils.getAppClientDir();
            ApiAlertException.throwIfFalse(
                localClient.exists(), "[StreamPark] " + localClient + " no exists. please check.");

            String regex = String.format("streampark-flink-sqlclient_%s-.*\\.jar", flinkEnv.getScalaVersion());

            List<String> jars = Arrays.stream(Objects.requireNonNull(localClient.list()))
                .filter(x -> x.matches(regex))
                .collect(Collectors.toList());

            ApiAlertException.throwIfTrue(
                jars.isEmpty(),
                "[StreamPark] can't found streampark-flink-sqlclient jar in " + localClient);

            ApiAlertException.throwIfTrue(
                jars.size() > 1,
                "[StreamPark] found multiple streampark-flink-sqlclient jar in " + localClient);
            flinkSqlClientJar = jars.get(0);
        }
        return flinkSqlClientJar;
    }

    public String getSparkSqlClientJar(SparkEnv sparkEnv) {
        if (sparkSqlClientJar == null) {
            File localClient = WebUtils.getAppClientDir();
            ApiAlertException.throwIfFalse(
                localClient.exists(), "[StreamPark] " + localClient + " no exists. please check.");
            List<String> jars = Arrays.stream(Objects.requireNonNull(localClient.list()))
                .filter(
                    x -> x.matches(
                        "streampark-spark-sqlclient_" + sparkEnv.getScalaVersion()
                            + "-.*\\.jar"))
                .collect(Collectors.toList());

            ApiAlertException.throwIfTrue(
                jars.isEmpty(),
                "[StreamPark] can't found streampark-flink-sqlclient jar in " + localClient);

            ApiAlertException.throwIfTrue(
                jars.size() > 1,
                "[StreamPark] found multiple streampark-flink-sqlclient jar in " + localClient);

            sparkSqlClientJar = jars.get(0);
        }
        return sparkSqlClientJar;
    }

    public String rollViewLog(String path, int offset, int limit) {
        try {
            File file = new File(path);
            if (file.exists() && file.isFile()) {
                try (Stream<String> stream = Files.lines(Paths.get(path))) {
                    return stream.skip(offset).limit(limit).collect(Collectors.joining("\r\n"));
                }
            }
            return null;
        } catch (Exception e) {
            throw new ApiDetailException("roll view log error: " + e);
        }
    }

    public void configureIngress(String clusterId, String namespace) throws KubernetesClientException {
        String domainName = settingService.getIngressModeDefault();
        if (StringUtils.isNotBlank(domainName)) {
            IngressController.configureIngress(domainName, clusterId, namespace);
        }
    }
}
