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

import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.base.util.WebUtils;
import org.apache.streampark.console.core.entity.FlinkEnv;
import org.apache.streampark.console.core.entity.SparkEnv;
import org.apache.streampark.console.core.service.CommonService;
import org.apache.streampark.console.system.authentication.JWTUtil;
import org.apache.streampark.console.system.entity.User;
import org.apache.streampark.console.system.service.UserService;

import org.apache.shiro.SecurityUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CommonServiceImpl implements CommonService {

  private String sqlClientJar = null;

  private String sparkSqlClientJar = null;

  @Autowired private UserService userService;

  @Override
  public User getCurrentUser() {
    String token = (String) SecurityUtils.getSubject().getPrincipal();
    Long userId = JWTUtil.getUserId(token);
    return userService.getById(userId);
  }

  @Override
  public Long getUserId() {
    return Optional.ofNullable(getCurrentUser()).map(User::getUserId).orElse(null);
  }

  @Override
  public String getSqlClientJar(FlinkEnv flinkEnv) {
    if (sqlClientJar == null) {
      File localClient = WebUtils.getAppClientDir();
      ApiAlertException.throwIfFalse(
          localClient.exists(), "[StreamPark] " + localClient + " no exists. please check.");
      List<String> jars =
          Arrays.stream(Objects.requireNonNull(localClient.list()))
              .filter(
                  x ->
                      x.matches(
                          "streampark-flink-sqlclient_" + flinkEnv.getScalaVersion() + "-.*\\.jar"))
              .collect(Collectors.toList());

      ApiAlertException.throwIfTrue(
          jars.isEmpty(),
          "[StreamPark] can't found streampark-flink-sqlclient jar in " + localClient);

      ApiAlertException.throwIfTrue(
          jars.size() > 1,
          "[StreamPark] found multiple streampark-flink-sqlclient jar in " + localClient);

      sqlClientJar = jars.get(0);
    }
    return sqlClientJar;
  }

  @Override
  public String getSqlClientJar(SparkEnv sparkEnv) {
    if (sparkSqlClientJar == null) {
      File localClient = WebUtils.getAppClientDir();
      ApiAlertException.throwIfFalse(
          localClient.exists(), "[StreamPark] " + localClient + " no exists. please check.");
      List<String> jars =
          Arrays.stream(Objects.requireNonNull(localClient.list()))
              .filter(
                  x ->
                      x.matches(
                          "streampark-spark-sqlclient_" + sparkEnv.getScalaVersion() + "-.*\\.jar"))
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
}
