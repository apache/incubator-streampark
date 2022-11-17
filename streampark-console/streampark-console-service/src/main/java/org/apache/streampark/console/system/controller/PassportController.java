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
import org.apache.streampark.console.base.domain.ResponseCode;
import org.apache.streampark.console.base.domain.RestResponse;
import org.apache.streampark.console.base.properties.ShiroProperties;
import org.apache.streampark.console.base.util.ShaHashUtils;
import org.apache.streampark.console.base.util.WebUtils;
import org.apache.streampark.console.system.authentication.JWTToken;
import org.apache.streampark.console.system.authentication.JWTUtil;
import org.apache.streampark.console.system.entity.User;
import org.apache.streampark.console.system.security.Authenticator;
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
import java.util.Map;

@Validated
@RestController
@RequestMapping("passport")
public class PassportController {

    @Autowired
    private UserService userService;

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
        return login(username, password, user);
    }

    @PostMapping("ldapSignin")
    public RestResponse ldapSignin(
        @NotBlank(message = "{required}") String username,
        @NotBlank(message = "{required}") String password) throws Exception {
        if (StringUtils.isEmpty(username)) {
            return RestResponse.success().put("code", 0);
        }
        User user = authenticator.ldapAuthenticate(username, password);
        return login(username, password, user);
    }

    @PostMapping("signout")
    public RestResponse signout() {
        SecurityUtils.getSecurityManager().logout(SecurityUtils.getSubject());
        return new RestResponse();
    }

    private RestResponse login(String username, String password, User user) throws Exception {
        if (user == null) {
            return RestResponse.success().put("code", 0);
        }

        if (User.STATUS_LOCK.equals(user.getStatus())) {
            return RestResponse.success().put("code", 1);
        }

        userService.fillInTeam(user);

        //no team.
        if (user.getLastTeamId() == null) {
            return RestResponse.success().data(user.getUserId()).put("code", ResponseCode.CODE_FORBIDDEN);
        }

        password = ShaHashUtils.encrypt(user.getSalt(), password);

        this.userService.updateLoginTime(username);
        String token = WebUtils.encryptToken(JWTUtil.sign(user.getUserId(), username, password));
        LocalDateTime expireTime = LocalDateTime.now().plusSeconds(properties.getJwtTimeOut());
        String expireTimeStr = DateUtils.formatFullTime(expireTime);
        JWTToken jwtToken = new JWTToken(token, expireTimeStr);
        String userId = RandomStringUtils.randomAlphanumeric(20);
        user.setId(userId);
        Map<String, Object> userInfo = userService.generateFrontendUserInfo(user, user.getLastTeamId(), jwtToken);
        return new RestResponse().data(userInfo);
    }
}
