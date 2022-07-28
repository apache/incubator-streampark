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

import com.streamxhub.streamx.console.base.exception.ApiException;
import com.streamxhub.streamx.console.base.util.WebUtils;
import com.streamxhub.streamx.console.core.entity.FlinkEnv;
import com.streamxhub.streamx.console.core.service.CommonService;
import com.streamxhub.streamx.console.system.authentication.JWTUtil;
import com.streamxhub.streamx.console.system.entity.User;
import com.streamxhub.streamx.console.system.service.UserService;

import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author benjobs
 */

@Service
public class CommonServiceImpl implements CommonService {

    private String sqlClientJar = null;

    @Autowired
    private UserService userService;

    @Override
    public User getCurrentUser() {
        String token = (String) SecurityUtils.getSubject().getPrincipal();
        String username = JWTUtil.getUsername(token);
        return userService.findByName(username);
    }

    @Override
    public String getSqlClientJar(FlinkEnv flinkEnv) {
        if (sqlClientJar == null) {
            File localClient = WebUtils.getAppClientDir();
            if (!localClient.exists()) {
                throw new ApiException("[StreamX] " + localClient + " no exists. please check.");
            }
            List<String> jars =
                Arrays.stream(Objects.requireNonNull(localClient.list())).filter(x -> x.matches("streamx-flink-sqlclient_" + flinkEnv.getScalaVersion() + "-.*\\.jar"))
                    .collect(Collectors.toList());
            if (jars.isEmpty()) {
                throw new ApiException("[StreamX] can no found streamx-flink-sqlclient jar in " + localClient);
            }
            if (jars.size() > 1) {
                throw new ApiException("[StreamX] found multiple streamx-flink-sqlclient jar in " + localClient);
            }
            sqlClientJar = jars.get(0);
        }
        return sqlClientJar;
    }
}
