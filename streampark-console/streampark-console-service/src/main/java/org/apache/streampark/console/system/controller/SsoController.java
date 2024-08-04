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
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.core.enums.LoginTypeEnum;
import org.apache.streampark.console.system.entity.User;
import org.apache.streampark.console.system.security.Authenticator;
import org.apache.streampark.console.system.service.UserService;

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

import java.util.List;

@Slf4j
@Controller
@RequestMapping("sso")
public class SsoController {

    @Autowired
    private UserService userService;

    @Autowired
    private Authenticator authenticator;

    @Value("${sso.properties.principalNameAttribute:#{null}}")
    private String principalNameAttribute;

    @Value("${sso.enable:#{false}}")
    private Boolean ssoEnable;

    @GetMapping("signin")
    public ModelAndView signin() throws Exception {
        // Redirect to home page with identity
        String url = "/#/?from=sso";
        return new ModelAndView("redirect:" + url);
    }

    @GetMapping("token")
    @ResponseBody
    public RestResponse token() throws Exception {
        // Check SSO enable status
        ApiAlertException.throwIfTrue(
            !ssoEnable,
            "Single Sign On (SSO) is not available, please contact the administrator to enable");

        Subject subject = SecurityUtils.getSubject();
        PrincipalCollection principals = subject.getPrincipals();
        Pac4jPrincipal principal = principals.oneByType(Pac4jPrincipal.class);

        List<CommonProfile> profiles = null;

        if (principal != null) {
            profiles = principal.getProfiles();
        }

        principal = new Pac4jPrincipal(profiles, principalNameAttribute);

        // Check Principal name
        ApiAlertException.throwIfNull(
            principal.getName(), "Please configure the correct Principal Name Attribute");

        User user = authenticator.authenticate(principal.getName(), null, LoginTypeEnum.SSO);

        return userService.getLoginUserInfo(user);
    }
}
