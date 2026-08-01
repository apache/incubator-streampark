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

package org.apache.streampark.console.system.authentication;

import org.apache.streampark.console.core.enums.UserType;
import org.apache.streampark.console.system.entity.User;
import org.apache.streampark.console.system.service.UserService;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.UnauthorizedException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

/**
 * A JWT filter variant that, on top of requiring a valid platform JWT (inherited from {@link
 * JWTFilter}), additionally requires the authenticated user to be a platform administrator ({@link
 * UserType#ADMIN}).
 *
 * <p>This exists to guard highly sensitive, non-REST endpoints (such as the embedded H2 database
 * console) that cannot be protected with the usual method-level {@code @RequiresPermissions}
 * annotation because they aren't Controller methods.
 */
@Slf4j
@Component
public class AdminOnlyFilter extends JWTFilter {

  @Autowired private UserService userService;

  @Override
  protected boolean isAccessAllowed(
      ServletRequest request, ServletResponse response, Object mappedValue)
      throws UnauthorizedException {
    boolean authenticated = super.isAccessAllowed(request, response, mappedValue);
    if (!authenticated) {
      return false;
    }
    try {
      Long userId = JWTUtil.getUserId((String) SecurityUtils.getSubject().getPrincipal());
      User user = userId == null ? null : userService.getById(userId);
      if (user == null || user.getUserType() != UserType.ADMIN) {
        log.warn(
            "Denied non-admin access to an admin-only resource, userId={}, uri={}",
            userId,
            ((HttpServletRequest) request).getRequestURI());
        return false;
      }
      return true;
    } catch (Exception e) {
      log.warn("Failed to verify admin-only access.", e);
      return false;
    }
  }
}
