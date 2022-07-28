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

package com.streamxhub.streamx.console.system.controller;

import com.streamxhub.streamx.console.base.domain.RestResponse;
import com.streamxhub.streamx.console.base.properties.ShiroProperties;
import com.streamxhub.streamx.console.system.authentication.JWTToken;
import com.streamxhub.streamx.console.system.entity.User;
import com.streamxhub.streamx.console.system.security.Authenticator;
import com.streamxhub.streamx.console.system.service.RoleService;
import com.streamxhub.streamx.console.system.service.UserService;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotBlank;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author benjobs
 */
@Validated
@RestController
@RequestMapping("passport")
public class PassportController {

    @Autowired
    private UserService userService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private ShiroProperties properties;

    @Autowired
    private Authenticator authenticator;

    @PostMapping("signin")
    public RestResponse signin(
        @NotBlank(message = "{required}") String username,
        @NotBlank(message = "{required}") String password) throws Exception {

        if (StringUtils.isEmpty(username)) {
            return RestResponse.success().put("code", 0);
        }

        // verify username and password
        Map<String, Object> userInfo = authenticator.authenticate(username, password);
        return new RestResponse().data(userInfo);
    }

    @PostMapping("signout")
    public RestResponse signout() {
        SecurityUtils.getSecurityManager().logout(SecurityUtils.getSubject());
        return new RestResponse();
    }

    /**
     * 生成前端需要的用户信息，包括： 1. token 2. Vue Router 3. 用户角色 4. 用户权限 5. 前端系统个性化配置信息
     *
     * @param token token
     * @param user  用户信息
     * @return UserInfo
     */
    private Map<String, Object> generateUserInfo(JWTToken token, User user) {
        String username = user.getUsername();
        Map<String, Object> userInfo = new HashMap<>(8);
        userInfo.put("token", token.getToken());
        userInfo.put("expire", token.getExpireAt());

        Set<String> roles = this.roleService.getUserRoleName(username);
        userInfo.put("roles", roles);

        Set<String> permissions = this.userService.getPermissions(username);
        userInfo.put("permissions", permissions);
        user.setPassword("******");
        user.setSalt("******");
        userInfo.put("user", user);
        return userInfo;
    }
}
