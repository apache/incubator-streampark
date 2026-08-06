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
import org.apache.streampark.console.base.domain.RestResponseBody;
import org.apache.streampark.console.base.web.FormOrJson;
import org.apache.streampark.console.core.enums.AccessTokenStateEnum;
import org.apache.streampark.console.core.util.ServiceHelper;
import org.apache.streampark.console.system.assembler.AccessTokenAssembler;
import org.apache.streampark.console.system.entity.AccessToken;
import org.apache.streampark.console.system.entity.User;
import org.apache.streampark.console.system.request.token.TokenCreateRequest;
import org.apache.streampark.console.system.request.token.TokenDeleteRequest;
import org.apache.streampark.console.system.request.token.TokenListQueryRequest;
import org.apache.streampark.console.system.request.token.TokenToggleRequest;
import org.apache.streampark.console.system.response.token.AccessTokenResponse;
import org.apache.streampark.console.system.service.AccessTokenService;
import org.apache.streampark.console.system.service.result.AccessTokenCreateResult;

import org.apache.shiro.authz.annotation.RequiresPermissions;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Validated
@RestController
@RequestMapping("token")
public class AccessTokenController {

    @Autowired
    private AccessTokenService accessTokenService;

    @PostMapping(value = "create")
    @RequiresPermissions("token:add")
    public RestResponseBody<AccessToken> createToken(@Valid @FormOrJson TokenCreateRequest request) throws Exception {
        AccessTokenCreateResult result =
            accessTokenService.create(request.getUserId(), request.getDescription());
        if (!result.isCreated()) {
            return RestResponseBody.<AccessToken>success(null)
                .extra("code", 0)
                .message(result.getMessage());
        }
        return RestResponseBody.success(result.getAccessToken());
    }

    @PostMapping(value = "check")
    public RestResponseBody<Integer> verifyToken() {
        Long userId = ServiceHelper.getUserId();
        AccessToken accessToken = accessTokenService.getByUserId(userId);
        if (accessToken == null) {
            return RestResponseBody.success(AccessTokenStateEnum.NULL.get());
        }
        if (AccessToken.STATUS_DISABLE.equals(accessToken.getStatus())) {
            return RestResponseBody.success(AccessTokenStateEnum.INVALID_TOKEN.get());
        }
        if (User.STATUS_LOCK.equals(accessToken.getUserStatus())) {
            return RestResponseBody.success(AccessTokenStateEnum.LOCKED_USER.get());
        }
        return RestResponseBody.success(null);
    }

    @PostMapping(value = "list")
    @RequiresPermissions("token:view")
    public RestResponseBody<IPage<AccessTokenResponse>> tokensList(
                                                                   RestRequest restRequest,
                                                                   TokenListQueryRequest query) {
        IPage<AccessToken> accessTokens =
            accessTokenService.getPage(AccessTokenAssembler.toEntity(query), restRequest);
        return RestResponseBody.success(AccessTokenAssembler.toPageResponse(accessTokens));
    }

    @PostMapping("toggle")
    @RequiresPermissions("token:add")
    public RestResponseBody<Boolean> toggleToken(@Valid @FormOrJson TokenToggleRequest request) {
        return RestResponseBody.success(accessTokenService.toggle(request.getTokenId()));
    }

    @DeleteMapping(value = "delete")
    @RequiresPermissions("token:delete")
    public RestResponseBody<Boolean> deleteToken(@Valid @FormOrJson TokenDeleteRequest request) {
        boolean res = accessTokenService.removeById(request.getTokenId());
        return RestResponseBody.success(res);
    }
}
