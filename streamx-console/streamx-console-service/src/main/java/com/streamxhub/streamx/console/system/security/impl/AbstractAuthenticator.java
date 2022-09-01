/*
 * Copyright 2019 The StreamX Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.console.system.security.impl;

import com.streamxhub.streamx.common.util.DateUtils;
import com.streamxhub.streamx.console.base.properties.ShiroProperties;
import com.streamxhub.streamx.console.base.util.ShaHashUtils;
import com.streamxhub.streamx.console.base.util.WebUtils;
import com.streamxhub.streamx.console.system.authentication.JWTToken;
import com.streamxhub.streamx.console.system.authentication.JWTUtil;
import com.streamxhub.streamx.console.system.entity.User;
import com.streamxhub.streamx.console.system.security.Authenticator;
import com.streamxhub.streamx.console.system.service.RoleService;
import com.streamxhub.streamx.console.system.service.UserService;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class AbstractAuthenticator implements Authenticator {

    @Autowired
    private UserService userService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private ShiroProperties properties;

    /**
     * user login and return user in db
     *
     * @param userId user identity field
     * @param password user login password
     * @return user object in databse
     */
    public abstract User login(String userId, String password) throws Exception;

    @Override
    public Map<String, Object> authenticate(String username, String password) throws Exception {
        User user = login(username, password);
        String salt = user.getSalt();
        password = ShaHashUtils.encrypt(salt, password);

        // 更新用户登录时间
        this.userService.updateLoginTime(username);
        String token = WebUtils.encryptToken(JWTUtil.sign(username, password));
        LocalDateTime expireTime = LocalDateTime.now().plusSeconds(properties.getJwtTimeOut());
        String expireTimeStr = DateUtils.formatFullTime(expireTime);
        JWTToken jwtToken = new JWTToken(token, expireTimeStr);
        String userId = RandomStringUtils.randomAlphanumeric(20);
        user.setId(userId);

        Map<String, Object> userInfo = new HashMap<>(8);
        userInfo.put("token", jwtToken.getToken());
        userInfo.put("expire", jwtToken.getExpireAt());

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
