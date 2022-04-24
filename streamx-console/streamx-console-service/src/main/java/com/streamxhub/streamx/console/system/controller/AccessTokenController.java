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
import com.streamxhub.streamx.console.base.exception.ServiceException;
import com.streamxhub.streamx.console.base.util.WebUtils;
import com.streamxhub.streamx.console.system.entity.AccessToken;
import com.streamxhub.streamx.console.system.service.AccessTokenService;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.shiro.SecurityUtils;
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


    /**
     * generate token string
     */
    @PostMapping(value = "/create")
    @RequiresPermissions("token:add")
    public RestResponse createToken(@NotBlank(message = "{required}") String username, String expireTime, String description)
        throws ServiceException {
        return accessTokenService.generateToken(username, expireTime, description);
    }

    /**
     * query token list
     */
    @PostMapping(value = "/list")
    @RequiresPermissions("token:view")
    public RestResponse tokenList(RestRequest restRequest, AccessToken accessToken) {
        IPage<AccessToken> accessTokens = accessTokenService.findAccessTokens(accessToken, restRequest);
        return RestResponse.create().data(accessTokens);
    }

    /**
     * update token status
     *
     * @return
     */
    @PostMapping("/update/status")
    @RequiresPermissions("token:add")
    public RestResponse updateTokenStatus(@NotNull(message = "{required}") Integer status, @NotNull(message = "{required}") Long tokenId)
        throws ServiceException {
        return accessTokenService.updateTokenStatus(status, tokenId);
    }

    /**
     * delete token by id
     */
    @DeleteMapping(value = "/delete")
    @RequiresPermissions("token:delete")
    public RestResponse deleteToken(@NotBlank(message = "{required}") Long tokenId) {
        boolean res = accessTokenService.deleteToken(tokenId);
        return RestResponse.create().data(res);
    }

    /**
     * copy cURL
     * 暂且硬编码吧，这里没有配置的必要，因为是固定的几个接口
     */
    @PostMapping(value = "/curl")
    public RestResponse copyApplicationApiCurl(@NotBlank(message = "{required}") String appId,
                                               @NotBlank(message = "{required}") String baseUrl,
                                               @NotBlank(message = "{required}") String path) {
        String resultCURL = null;
        CURLBuilder curlBuilder = new CURLBuilder(baseUrl + path);

        curlBuilder
            .addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            .addHeader("Authorization", WebUtils.encryptToken(SecurityUtils.getSubject().getPrincipal().toString()));

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
