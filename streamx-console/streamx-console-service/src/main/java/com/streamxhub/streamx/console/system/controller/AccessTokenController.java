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

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.streamxhub.streamx.console.base.domain.RestRequest;
import com.streamxhub.streamx.console.base.domain.RestResponse;
import com.streamxhub.streamx.console.system.entity.AccessToken;
import com.streamxhub.streamx.console.system.service.AccessTokenService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.constraints.NotBlank;


/**
 * @author xianwei.yang
 */
@Api(tags = "ACCESS_TOKEN_TAG")
@RestController
@RequestMapping("token")
public class AccessTokenController {

    @Autowired
    private AccessTokenService accessTokenService;


    /**
     * generate token string
     */
    @ApiIgnore
    @PostMapping(value = "/create")
    public RestResponse createToken(@NotBlank(message = "{required}") String username, String expireTime) {

        return accessTokenService.generateToken(username, expireTime);
    }

    /**
     * generate token string
     */
    @ApiIgnore
    @PostMapping(value = "/list")
    public RestResponse tokenList(RestRequest restRequest, AccessToken accessToken) {
        IPage<AccessToken> accessTokens = accessTokenService.findAccessTokens(accessToken, restRequest);
        return RestResponse.create().data(accessTokens);
    }


    /**
     * generate token string
     */
    @ApiIgnore
    @DeleteMapping(value = "/delete")
    public RestResponse deleteToken(@NotBlank(message = "{required}") Long id) {
        boolean res = accessTokenService.deleteToken(id);
        return RestResponse.create().data(res);
    }


}
