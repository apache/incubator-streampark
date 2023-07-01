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

import org.apache.streampark.console.base.domain.RestResponse;
import org.apache.streampark.console.system.entity.User;
import org.apache.streampark.console.system.security.Authenticator;
import org.apache.streampark.console.system.service.UserService;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotBlank;

import java.util.ArrayList;
import java.util.List;

@Tag(name = "PASSPORT_TAG")
@Slf4j
@Validated
@RestController
@RequestMapping("passport")
public class PassportController {

  @Autowired private UserService userService;

  @Autowired private Authenticator authenticator;

  @Value("${sso.enable:#{false}}")
  private Boolean ssoEnable;

  @Value("${ldap.enable:#{false}}")
  private Boolean ldapEnable;

  @Operation(summary = "SigninType")
  @PostMapping("signtype")
  public RestResponse type() {
    List<String> types = new ArrayList<>();
    types.add("password");
    if (ssoEnable) {
      types.add("sso");
    }
    if (ldapEnable) {
      types.add("ldap");
    }
    return RestResponse.success(types);
  }

  @Operation(summary = "Signin")
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
    return userService.getLoginUserInfo(user);
  }

  @Operation(summary = "Signout")
  @PostMapping("signout")
  public RestResponse signout() {
    SecurityUtils.getSecurityManager().logout(SecurityUtils.getSubject());
    return new RestResponse();
  }
}
