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

package org.apache.streampark.console.system.security.impl;

import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.base.util.ShaHashUtils;
import org.apache.streampark.console.core.enums.LoginType;
import org.apache.streampark.console.core.enums.UserType;
import org.apache.streampark.console.system.entity.User;
import org.apache.streampark.console.system.security.Authenticator;
import org.apache.streampark.console.system.service.UserService;

import org.apache.commons.lang3.StringUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class AuthenticatorImpl implements Authenticator {
  @Autowired private UserService usersService;
  @Autowired private LdapService ldapService;

  @Override
  public User authenticate(String username, String password, String loginType) throws Exception {
    LoginType loginTypeEnum = LoginType.of(loginType);
    if (loginTypeEnum == null) {
      throw new ApiAlertException(
          String.format("the login type [%s] is not supported.", loginType));
    }
    switch (loginTypeEnum) {
      case PASSWORD:
        return passwordAuthenticate(username, password);
      case LDAP:
        return ldapAuthenticate(username, password);
      case SSO:
        return ssoAuthenticate(username);
      default:
        throw new ApiAlertException(
            String.format("the login type [%s] is not supported.", loginType));
    }
  }

  private User passwordAuthenticate(String username, String password) {
    User user = usersService.findByName(username);
    if (user == null || user.getLoginType() != LoginType.PASSWORD) {
      throw new ApiAlertException(
          String.format("user [%s] does not exist or can not login with PASSWORD", username));
    }
    String salt = user.getSalt();
    password = ShaHashUtils.encrypt(salt, password);
    if (!StringUtils.equals(user.getPassword(), password)) {
      return null;
    }
    return user;
  }

  private User ldapAuthenticate(String username, String password) throws Exception {
    String ldapEmail = ldapService.ldapLogin(username, password);
    if (ldapEmail == null) {
      return null;
    }
    // check if user exist
    User user = usersService.findByName(username);

    if (user != null) {
      if (user.getLoginType() != LoginType.LDAP) {
        throw new ApiAlertException(
            String.format("user [%s] can only sign in with %s", username, user.getLoginType()));
      }
      String saltPassword = ShaHashUtils.encrypt(user.getSalt(), password);

      // ldap password changed, we should update user password
      if (!StringUtils.equals(saltPassword, user.getPassword())) {

        // encrypt password again
        String salt = ShaHashUtils.getRandomSalt();
        saltPassword = ShaHashUtils.encrypt(salt, password);
        user.setSalt(salt);
        user.setPassword(saltPassword);
        usersService.updateSaltPassword(user);
      }
      return user;
    }
    return this.newUserCreate(LoginType.LDAP, username, password);
  }

  private User ssoAuthenticate(String username) throws Exception {
    // check if user exist
    User user = usersService.findByName(username);
    if (user != null) {
      if (user.getLoginType() != LoginType.SSO) {
        throw new ApiAlertException(
            String.format("user [%s] can only sign in with %s", username, user.getLoginType()));
      }
      return user;
    }
    return this.newUserCreate(LoginType.SSO, username, null);
  }

  private User newUserCreate(LoginType loginType, String username, String password)
      throws Exception {
    User newUser = new User();
    newUser.setCreateTime(new Date());
    newUser.setUsername(username);
    newUser.setNickName(username);
    newUser.setLoginType(loginType);
    newUser.setUserType(UserType.USER);
    newUser.setStatus(User.STATUS_VALID);
    newUser.setSex(User.SEX_UNKNOWN);
    if (password != null) {
      newUser.setPassword(password);
    }
    usersService.createUser(newUser);
    return newUser;
  }
}
