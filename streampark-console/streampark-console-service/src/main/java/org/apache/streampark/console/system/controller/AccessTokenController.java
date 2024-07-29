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

import org.apache.streampark.console.base.bean.PageRequest;
import org.apache.streampark.console.base.bean.Response;
import org.apache.streampark.console.base.exception.InternalException;
import org.apache.streampark.console.core.enums.AccessTokenStateEnum;
import org.apache.streampark.console.core.util.ServiceHelper;
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

import javax.validation.constraints.NotNull;

@RestController
@RequestMapping("token")
public class AccessTokenController {

    @Autowired
    private AccessTokenService accessTokenService;

    @PostMapping(value = "create")
    @RequiresPermissions("token:add")
    public Response<AccessToken> createToken(
                                             @NotNull(message = "{required}") Long userId,
                                             @RequestParam(required = false) String description) throws InternalException {
        return accessTokenService.create(userId, description);
    }

    @PostMapping(value = "check")
    public Response<Integer> verifyToken() {
        Long userId = ServiceHelper.getUserId();
        AccessToken accessToken = accessTokenService.getByUserId(userId);
        if (accessToken == null) {
            return Response.success(AccessTokenStateEnum.NULL.get());
        } else if (AccessToken.STATUS_DISABLE.equals(accessToken.getStatus())) {
            return Response.success(AccessTokenStateEnum.INVALID_TOKEN.get());
        } else if (User.STATUS_LOCK.equals(accessToken.getUserStatus())) {
            return Response.success(AccessTokenStateEnum.LOCKED_USER.get());
        }
        return Response.success(AccessTokenStateEnum.OK.get());
    }

    @PostMapping(value = "list")
    @RequiresPermissions("token:view")
    public Response<IPage<AccessToken>> tokensList(PageRequest pageRequest, AccessToken accessToken) {
        IPage<AccessToken> accessTokens = accessTokenService.getPage(accessToken, pageRequest);
        return Response.success(accessTokens);
    }

    @PostMapping("toggle")
    @RequiresPermissions("token:add")
    public Response<Void> toggleToken(@NotNull(message = "{required}") Long tokenId) {
        return accessTokenService.toggleToken(tokenId);
    }

    @DeleteMapping(value = "delete")
    @RequiresPermissions("token:delete")
    public Response<Boolean> deleteToken(@NotNull(message = "{required}") Long tokenId) {
        boolean res = accessTokenService.removeById(tokenId);
        return Response.success(res);
    }
}
