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

package org.apache.streampark.console.base.interceptor;

import org.apache.streampark.console.base.exception.UserLockedException;
import org.apache.streampark.console.core.service.CommonService;
import org.apache.streampark.console.system.entity.User;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Check user status interceptor. */
@Component
public class UserStatusCheckInterceptor implements HandlerInterceptor {

  @Autowired private CommonService commonService;

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    User currentUser = commonService.getCurrentUser();
    if (User.STATUS_LOCK.equals(currentUser.getStatus())) {
      throw new UserLockedException("User has been locked");
    }
    return true;
  }
}
