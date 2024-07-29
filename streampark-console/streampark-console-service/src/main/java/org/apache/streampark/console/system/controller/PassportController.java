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
import org.apache.streampark.console.base.bean.Response;
import org.apache.streampark.console.base.util.WebUtils;
import org.apache.streampark.console.core.enums.AuthenticationType;
import org.apache.streampark.console.core.enums.LoginTypeEnum;
import org.apache.streampark.console.system.authentication.JWTToken;
import org.apache.streampark.console.system.authentication.JWTUtil;
import org.apache.streampark.console.system.entity.User;
import org.apache.streampark.console.system.security.Authenticator;
import org.apache.streampark.console.system.service.UserService;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Validated
@RestController
@RequestMapping("passport")
public class PassportController {

    @Autowired
    private UserService userService;

    @Autowired
    private Authenticator authenticator;

    @Value("${sso.enable:#{false}}")
    private Boolean ssoEnable;

    @Value("${ldap.enable:#{false}}")
    private Boolean ldapEnable;

    @PostMapping("signtype")
    public Response<List<String>> type() {
        List<String> types = new ArrayList<>();
        types.add(LoginTypeEnum.PASSWORD.name().toLowerCase());
        if (ssoEnable) {
            types.add(LoginTypeEnum.SSO.name().toLowerCase());
        }
        if (ldapEnable) {
            types.add(LoginTypeEnum.LDAP.name().toLowerCase());
        }
        return Response.success(types);
    }

    @PostMapping("signin")
    public Response<?> signin(
                              @NotBlank(message = "{required}") String username,
                              @NotBlank(message = "{required}") String password,
                              @NotBlank(message = "{required}") String loginType) throws Exception {

        if (StringUtils.isEmpty(username)) {
            return Response.fail(0);
        }

        User user = authenticator.authenticate(username, password, loginType);

        if (user == null) {
            return Response.fail(0);
        }

        if (User.STATUS_LOCK.equals(user.getStatus())) {
            return Response.fail(1);
        }

        this.userService.updateLoginTime(username);
        String sign = JWTUtil.sign(user.getUserId(), username, user.getSalt(), AuthenticationType.SIGN);

        LocalDateTime expireTime = LocalDateTime.now().plusSeconds(JWTUtil.getTTLOfSecond());
        String ttl = DateUtils.formatFullTime(expireTime);

        // shiro login
        JWTToken loginToken = new JWTToken(sign, ttl, AuthenticationType.SIGN.get());
        SecurityUtils.getSubject().login(loginToken);

        // generate UserInfo
        String token = WebUtils.encryptToken(sign);
        JWTToken jwtToken = new JWTToken(token, ttl, AuthenticationType.SIGN.get());
        String userId = RandomStringUtils.randomAlphanumeric(20);
        user.setId(userId);
        Map<String, Object> userInfo =
            userService.generateFrontendUserInfo(user, user.getLastTeamId(), jwtToken);

        return Response.success(userInfo);
    }

    @PostMapping("signout")
    public Response<Void> signout() {
        SecurityUtils.getSubject().logout();
        return Response.success();
    }
}
