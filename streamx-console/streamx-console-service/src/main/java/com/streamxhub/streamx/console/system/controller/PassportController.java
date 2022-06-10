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

import com.streamxhub.streamx.common.util.DateUtils;
import com.streamxhub.streamx.console.base.domain.RestResponse;
import com.streamxhub.streamx.console.base.properties.ShiroProperties;
import com.streamxhub.streamx.console.base.util.ShaHashUtils;
import com.streamxhub.streamx.console.base.util.WebUtils;
import com.streamxhub.streamx.console.system.authentication.JWTToken;
import com.streamxhub.streamx.console.system.authentication.JWTUtil;
import com.streamxhub.streamx.console.system.entity.User;
import com.streamxhub.streamx.console.system.service.RoleService;
import com.streamxhub.streamx.console.system.service.UserService;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotBlank;

import java.time.LocalDateTime;
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

    @PostMapping("signin")
    public RestResponse signin(
        @NotBlank(message = "{required}") String username,
        @NotBlank(message = "{required}") String password) throws Exception {

        username = StringUtils.lowerCase(username);
        User user = this.userService.findByName(username);

        if (user == null) {
            return RestResponse.create().put("code", 0);
        }

        String salt = user.getSalt();
        password = ShaHashUtils.encrypt(salt, password);

        if (!StringUtils.equals(user.getPassword(), password)) {
            return RestResponse.create().put("code", 0);
        }

        if (User.STATUS_LOCK.equals(user.getStatus())) {
            return RestResponse.create().put("code", 1);
        }

        // 更新用户登录时间
        this.userService.updateLoginTime(username);
        LocalDateTime expireTime = LocalDateTime.now().plusSeconds(properties.getJwtTimeOut());
        String token = WebUtils.encryptToken(JWTUtil.sign(username, password));
        String expireTimeStr = DateUtils.formatFullTime(expireTime);
        JWTToken jwtToken = new JWTToken(token, expireTimeStr);
        String userId = RandomStringUtils.randomAlphanumeric(20);
        user.setId(userId);
        Map<String, Object> userInfo = this.generateUserInfo(jwtToken, user);
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
