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

package org.apache.streampark.console.core.controller;

import org.apache.streampark.common.util.CURLBuilder;
import org.apache.streampark.console.base.domain.RestResponse;
import org.apache.streampark.console.core.bean.OpenAPISchema;
import org.apache.streampark.console.core.component.OpenAPIComponent;
import org.apache.streampark.console.core.component.ServiceComponent;
import org.apache.streampark.console.system.service.AccessTokenService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotBlank;

@Slf4j
@Validated
@RestController
@RequestMapping("openapi")
public class OpenAPIController {

    @Autowired
    private AccessTokenService accessTokenService;

    @Autowired
    private ServiceComponent serviceComponent;

    @Autowired
    private OpenAPIComponent openAPIComponent;

    /**
     * copy cURL, hardcode now, there is no need for configuration here, because there are several
     * fixed interfaces
     */
    @PostMapping(value = "curl")
    public RestResponse copyRestApiCurl(
                                        @NotBlank(message = "{required}") String appId,
                                        @NotBlank(message = "{required}") String baseUrl,
                                        @NotBlank(message = "{required}") String path) {
        String resultCURL = null;
        CURLBuilder curlBuilder = new CURLBuilder(baseUrl + path);
        curlBuilder
            .addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            .addHeader(
                "Authorization",
                accessTokenService.getByUserId(serviceComponent.getUserId()).getToken());

        if ("/flink/app/start".equalsIgnoreCase(path)) {
            resultCURL = curlBuilder
                .addFormData("allowNonRestored", "false")
                .addFormData("savePoint", "")
                .addFormData("savePointed", "false")
                .addFormData("id", appId)
                .build();
        } else if ("/flink/app/cancel".equalsIgnoreCase(path)) {
            resultCURL = curlBuilder
                .addFormData("id", appId)
                .addFormData("savePointed", "false")
                .addFormData("drain", "false")
                .addFormData("nativeFormat", "false")
                .addFormData("savePoint", "")
                .build();
        }
        return RestResponse.success(resultCURL);
    }

    @PostMapping(value = "schema")
    public RestResponse schema(@NotBlank(message = "{required}") String url) {
        OpenAPISchema openAPISchema = openAPIComponent.getOpenAPISchema(url);
        return RestResponse.success(openAPISchema);
    }

}
