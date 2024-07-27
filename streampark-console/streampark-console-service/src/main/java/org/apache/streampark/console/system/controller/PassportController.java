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
import org.apache.streampark.console.base.util.WebUtils;
import org.apache.streampark.console.core.enums.AuthenticationType;
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

  @Autowired private UserService userService;

  @Autowired private Authenticator authenticator;

  @PostMapping("signin")
  public RestResponse signin(
      @NotBlank(message = "{required}") String username,
      @NotBlank(message = "{required}") String password,
      @NotBlank(message = "{required}") String loginType)
      throws Exception {

    if (StringUtils.isEmpty(username)) {
      return RestResponse.success().put("code", 0);
    }

    User user = authenticator.authenticate(username, password, loginType);

    if (user == null) {
      return RestResponse.success().put("code", 0);
    }

    if (User.STATUS_LOCK.equals(user.getStatus())) {
      return RestResponse.success().put("code", 1);
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

    return new RestResponse().data(userInfo);
  }

  @PostMapping("signout")
  public RestResponse signout() {
    SecurityUtils.getSubject().logout();
    return new RestResponse();
  }
}
