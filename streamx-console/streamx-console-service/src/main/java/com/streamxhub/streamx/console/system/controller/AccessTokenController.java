/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.console.system.controller;

import com.streamxhub.streamx.common.util.CURLBuilder;
import com.streamxhub.streamx.console.base.domain.RestRequest;
import com.streamxhub.streamx.console.base.domain.RestResponse;
import com.streamxhub.streamx.console.base.exception.InternalException;
import com.streamxhub.streamx.console.core.enums.AccessTokenState;
import com.streamxhub.streamx.console.core.service.CommonService;
import com.streamxhub.streamx.console.system.entity.AccessToken;
import com.streamxhub.streamx.console.system.entity.User;
import com.streamxhub.streamx.console.system.service.AccessTokenService;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;


/**
 * @author xianwei.yang
 */
@RestController
@RequestMapping("token")
public class AccessTokenController {

    @Autowired
    private AccessTokenService accessTokenService;

    @Autowired
    private CommonService commonService;

    /**
     * generate token string
     */
    @PostMapping(value = "create")
    @RequiresPermissions("token:add")
    public RestResponse createToken(@NotBlank(message = "{required}") Long userId, String expireTime, String description)
        throws InternalException {
        return accessTokenService.generateToken(userId, expireTime, description);
    }

    @PostMapping(value = "check")
    public RestResponse checkToken() {
        User user = commonService.getCurrentUser();
        RestResponse restResponse = RestResponse.success();
        if (user != null) {
            AccessToken accessToken = accessTokenService.getByUserId(user.getUserId());
            if (accessToken == null) {
                restResponse.data(AccessTokenState.NULL.get());
            } else if (AccessToken.STATUS_DISABLE.equals(accessToken.getFinalStatus())) {
                restResponse.data(AccessTokenState.INVALID.get());
            } else {
                restResponse.data(AccessTokenState.OK.get());
            }
        } else {
            restResponse.data(AccessTokenState.INVALID.get());
        }
        return restResponse;
    }

    /**
     * query token list
     */
    @PostMapping(value = "list")
    @RequiresPermissions("token:view")
    public RestResponse tokenList(RestRequest restRequest, AccessToken accessToken) {
        IPage<AccessToken> accessTokens = accessTokenService.findAccessTokens(accessToken, restRequest);
        return RestResponse.success(accessTokens);
    }

    /**
     * update token status
     *
     * @return
     */
    @PostMapping("toggle")
    @RequiresPermissions("token:add")
    public RestResponse toggleToken(@NotNull(message = "{required}") Long tokenId) {
        return accessTokenService.toggleToken(tokenId);
    }

    /**
     * delete token by id
     */
    @DeleteMapping(value = "delete")
    @RequiresPermissions("token:delete")
    public RestResponse deleteToken(@NotBlank(message = "{required}") Long tokenId) {
        boolean res = accessTokenService.deleteToken(tokenId);
        return RestResponse.success(res);
    }

    /**
     * copy cURL
     * 暂且硬编码吧，这里没有配置的必要，因为是固定的几个接口
     */
    @PostMapping(value = "curl")
    public RestResponse copyRestApiCurl(@NotBlank(message = "{required}") String appId,
                                               @NotBlank(message = "{required}") String baseUrl,
                                               @NotBlank(message = "{required}") String path) {
        String resultCURL = null;
        CURLBuilder curlBuilder = new CURLBuilder(baseUrl + path);

        User user = commonService.getCurrentUser();

        curlBuilder
            .addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            .addHeader("Authorization", accessTokenService.getByUserId(user.getUserId()).getToken());

        if ("/flink/app/start".equalsIgnoreCase(path)) {
            resultCURL = curlBuilder
                .addFormData("allowNonRestored", "false")
                .addFormData("flameGraph", "false")
                .addFormData("savePoint", "")
                .addFormData("savePointed", "false")
                .addFormData("id", appId)
                .build();
        } else if ("/flink/app/cancel".equalsIgnoreCase(path)) {
            resultCURL = curlBuilder
                .addFormData("id", appId)
                .addFormData("savePointed", "false")
                .addFormData("drain", "false")
                .addFormData("savePoint", "")
                .build();
        }
        return RestResponse.success(resultCURL);
    }
}
