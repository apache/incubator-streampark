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

package org.apache.streampark.console.system.controller;

import org.apache.streampark.common.util.DateUtils;
import org.apache.streampark.console.base.domain.RestResponse;
import org.apache.streampark.console.base.properties.ShiroProperties;
import org.apache.streampark.console.base.util.ShaHashUtils;
import org.apache.streampark.console.base.util.WebUtils;
import org.apache.streampark.console.system.authentication.JWTToken;
import org.apache.streampark.console.system.authentication.JWTUtil;
import org.apache.streampark.console.system.entity.User;
import org.apache.streampark.console.system.security.Authenticator;
import org.apache.streampark.console.system.service.RoleService;
import org.apache.streampark.console.system.service.UserService;

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
        User user = authenticator.authenticate(username, password);
        Map<String, Object> userInfo = login(username, password, user);
        return new RestResponse().data(userInfo);
    }

    @PostMapping("ldapSignin")
    public RestResponse ldapSignin(
        @NotBlank(message = "{required}") String username,
        @NotBlank(message = "{required}") String password) throws Exception {
        if (StringUtils.isEmpty(username)) {
            return RestResponse.success().put("code", 0);
        }
        User user = authenticator.ldapAuthenticate(username, password);
        Map<String, Object> userInfo = login(username, password, user);
        return new RestResponse().data(userInfo);
    }

    @PostMapping("signout")
    public RestResponse signout() {
        SecurityUtils.getSecurityManager().logout(SecurityUtils.getSubject());
        return new RestResponse();
    }

    /**
     * generate user info, contains: 1.token, 2.vue router, 3.role, 4.permission, 5.personalized config info of frontend
     *
     * @param token token
     * @param user  user
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

    private Map<String, Object> login(String username, String password, User user) throws Exception {
        if (user == null) {
            return RestResponse.success().put("code", 0);
        }

        if (User.STATUS_LOCK.equals(user.getStatus())) {
            return RestResponse.success().put("code", 1);
        }

        password = ShaHashUtils.encrypt(user.getSalt(), password);

        this.userService.updateLoginTime(username);
        String token = WebUtils.encryptToken(JWTUtil.sign(username, password));
        LocalDateTime expireTime = LocalDateTime.now().plusSeconds(properties.getJwtTimeOut());
        String expireTimeStr = DateUtils.formatFullTime(expireTime);
        JWTToken jwtToken = new JWTToken(token, expireTimeStr);
        String userId = RandomStringUtils.randomAlphanumeric(20);
        user.setId(userId);
        return this.generateUserInfo(jwtToken, user);
    }
}
