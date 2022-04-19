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

import com.streamxhub.streamx.console.base.domain.RestRequest;
import com.streamxhub.streamx.console.base.domain.RestResponse;
import com.streamxhub.streamx.console.system.entity.AccessToken;
import com.streamxhub.streamx.console.system.service.AccessTokenService;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotBlank;

import java.util.HashMap;
import java.util.Map;


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
    public RestResponse createToken(@NotBlank(message = "{required}") String username, String expireTime, String description) {

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
    public RestResponse copyApplicationApiCurl(@NotBlank(message = "{required}") String appId, @NotBlank(message = "{required}") String baseUrl, @NotBlank(message = "{required}") String path) {
        String resultCURL = null;
        CurlBuilder curlBuilder = new CurlBuilder()
            .setUrl(baseUrl + path)
            .addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            .addHeader("Authorization", "{替换accessToken}");
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

    class CurlBuilder {
        private Map<String, String> headers = new HashMap<>();
        private Map<String, String> formDatas = new HashMap<>();
        private String url;

        private CurlBuilder setUrl(String url) {
            this.url = url;
            return this;
        }

        private CurlBuilder addHeader(String k, String v) {
            this.headers.put(k, v);
            return this;
        }

        private CurlBuilder addFormData(String k, String v) {
            this.formDatas.put(k, v);
            return this;
        }

        public String build() {
            StringBuilder cURL = new StringBuilder("curl -X POST ");
            cURL.append(String.format("'%s' \\\n", url));
            for (String headerKey : headers.keySet()) {
                cURL.append(String.format("-H \'%s: %s\' \\\n", headerKey, headers.get(headerKey)));
            }
            for (String field : formDatas.keySet()) {
                cURL.append(String.format("--data-urlencode \'%s=%s\' \\\n", field, formDatas.get(field)));
            }

            cURL.append("-i");

            return cURL.toString();
        }

    }

}
