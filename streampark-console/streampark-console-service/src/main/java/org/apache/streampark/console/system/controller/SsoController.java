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
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.base.properties.ShiroProperties;
import org.apache.streampark.console.base.util.WebUtils;
import org.apache.streampark.console.core.enums.LoginType;
import org.apache.streampark.console.system.authentication.JWTToken;
import org.apache.streampark.console.system.authentication.JWTUtil;
import org.apache.streampark.console.system.entity.User;
import org.apache.streampark.console.system.security.Authenticator;
import org.apache.streampark.console.system.service.UserService;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;

import io.buji.pac4j.subject.Pac4jPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.pac4j.core.profile.CommonProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("sso")
public class SsoController {
  @Autowired private UserService userService;

  @Autowired private ShiroProperties properties;

  @Autowired private Authenticator authenticator;

  @Value("${pac4j.properties.principalNameAttribute:#{null}}")
  private String principalNameAttribute;

  @Value("${sso.enable:#{false}}")
  private Boolean ssoEnable;

  @GetMapping("signin")
  public ModelAndView signin(RedirectAttributes attributes) throws Exception {
    // Redirect to home page with identity
    String url = "/#/?from=sso";
    return new ModelAndView("redirect:" + url);
  }

  @GetMapping("token")
  @ResponseBody
  public RestResponse token(HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    if (!ssoEnable) {
      throw new ApiAlertException(
          "Single Sign On (SSO) is not available, please contact the administrator to enable");
    }
    // Based on User Profile from Shiro and build Pac4jPrincipal
    Subject subject = SecurityUtils.getSubject();
    PrincipalCollection principals = subject.getPrincipals();
    Pac4jPrincipal principal = principals.oneByType(Pac4jPrincipal.class);
    List<CommonProfile> profiles = null;
    if (principal != null) {
      profiles = principal.getProfiles();
    }
    principal = new Pac4jPrincipal(profiles, principalNameAttribute);
    if (principal.getName() == null) {
      log.error(
          "Please configure correct principalNameAttribute from UserProfile: "
              + principal.toString());
      throw new ApiAlertException("Please configure the correct Principal Name Attribute");
    }
    User user = authenticator.authenticate(principal.getName(), null, LoginType.SSO.toString());
    return this.login(user.getUsername(), user);
  }

  private RestResponse login(String username, User user) throws Exception {
    if (user == null) {
      return RestResponse.success().put("code", 0);
    }

    if (User.STATUS_LOCK.equals(user.getStatus())) {
      return RestResponse.success().put("code", 1);
    }

    userService.fillInTeam(user);

    // no team.
    if (user.getLastTeamId() == null) {
      return RestResponse.success().data(user.getUserId()).put("code", ResponseCode.CODE_FORBIDDEN);
    }

    this.userService.updateLoginTime(username);
    String token = WebUtils.encryptToken(JWTUtil.sign(user.getUserId(), username));
    LocalDateTime expireTime = LocalDateTime.now().plusSeconds(properties.getJwtTimeOut());
    String expireTimeStr = DateUtils.formatFullTime(expireTime);
    JWTToken jwtToken = new JWTToken(token, expireTimeStr);
    String userId = RandomStringUtils.randomAlphanumeric(20);
    user.setId(userId);
    Map<String, Object> userInfo =
        userService.generateFrontendUserInfo(user, user.getLastTeamId(), jwtToken);
    return new RestResponse().data(userInfo);
  }
}
