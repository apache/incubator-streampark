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

import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.base.domain.RestResponse;
import org.apache.streampark.console.core.enums.AccessTokenState;
import org.apache.streampark.console.core.service.ServiceHelper;
import org.apache.streampark.console.system.entity.AccessToken;
import org.apache.streampark.console.system.entity.User;
import org.apache.streampark.console.system.service.AccessTokenService;

import org.apache.shiro.authz.annotation.RequiresPermissions;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@RestController
@RequestMapping("token")
public class AccessTokenController {

  @Autowired private AccessTokenService accessTokenService;

  @Autowired private ServiceHelper serviceHelper;

  /** generate token string */
  @PostMapping(value = "create")
  @RequiresPermissions("token:add")
  public RestResponse createToken(
      @NotBlank(message = "{required}") Long userId,
      @RequestParam(required = false) String description)
      throws Exception {
    return accessTokenService.create(userId, description);
  }

  @PostMapping(value = "check")
  public RestResponse verifyToken() {
    Long userId = serviceHelper.getUserId();
    RestResponse restResponse = RestResponse.success();
    AccessToken accessToken = accessTokenService.getByUserId(userId);
    if (accessToken == null) {
      restResponse.data(AccessTokenState.NULL.get());
    } else if (AccessToken.STATUS_DISABLE.equals(accessToken.getStatus())) {
      restResponse.data(AccessTokenState.INVALID_TOKEN.get());
    } else if (User.STATUS_LOCK.equals(accessToken.getUserStatus())) {
      restResponse.data(AccessTokenState.LOCKED_USER.get());
    }
    return restResponse;
  }

  /** query token list */
  @PostMapping(value = "list")
  @RequiresPermissions("token:view")
  public RestResponse tokensList(RestRequest restRequest, AccessToken accessToken) {
    IPage<AccessToken> accessTokens = accessTokenService.page(accessToken, restRequest);
    return RestResponse.success(accessTokens);
  }

  /** update token status */
  @PostMapping("toggle")
  @RequiresPermissions("token:add")
  public RestResponse toggleToken(@NotNull(message = "{required}") Long tokenId) {
    return accessTokenService.toggle(tokenId);
  }

  /** delete token by id */
  @DeleteMapping(value = "delete")
  @RequiresPermissions("token:delete")
  public RestResponse deleteToken(@NotBlank(message = "{required}") Long tokenId) {
    boolean res = accessTokenService.delete(tokenId);
    return RestResponse.success(res);
  }
}
