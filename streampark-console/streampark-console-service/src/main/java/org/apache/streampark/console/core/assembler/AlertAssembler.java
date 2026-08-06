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

package org.apache.streampark.console.core.assembler;

import org.apache.streampark.console.base.util.JacksonUtils;
import org.apache.streampark.console.core.bean.AlertDingTalkParams;
import org.apache.streampark.console.core.bean.AlertEmailParams;
import org.apache.streampark.console.core.bean.AlertHttpCallbackParams;
import org.apache.streampark.console.core.bean.AlertLarkParams;
import org.apache.streampark.console.core.bean.AlertWeComParams;
import org.apache.streampark.console.core.entity.AlertConfig;
import org.apache.streampark.console.core.request.alert.AlertConfigRequest;
import org.apache.streampark.console.core.response.alert.AlertConfigResponse;

import org.apache.commons.lang3.StringUtils;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.BeanUtils;

import java.util.List;

/** Converts between alert config entities and API request/response contracts. */
public final class AlertAssembler {

    private AlertAssembler() {
    }

    public static AlertConfig toEntity(AlertConfigRequest request) {
        if (request == null) {
            return null;
        }
        AlertConfig alertConfig = new AlertConfig();
        BeanUtils.copyProperties(
            request,
            alertConfig,
            "emailParams",
            "dingTalkParams",
            "weComParams",
            "httpCallbackParams",
            "larkParams");
        try {
            if (request.getEmailParams() != null) {
                alertConfig.setEmailParams(JacksonUtils.write(request.getEmailParams()));
            }
            if (request.getDingTalkParams() != null) {
                alertConfig.setDingTalkParams(JacksonUtils.write(request.getDingTalkParams()));
            }
            if (request.getWeComParams() != null) {
                alertConfig.setWeComParams(JacksonUtils.write(request.getWeComParams()));
            }
            if (request.getHttpCallbackParams() != null) {
                alertConfig.setHttpCallbackParams(JacksonUtils.write(request.getHttpCallbackParams()));
            }
            if (request.getLarkParams() != null) {
                alertConfig.setLarkParams(JacksonUtils.write(request.getLarkParams()));
            }
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize alert config params", e);
        }
        return alertConfig;
    }

    public static AlertConfigResponse toResponse(AlertConfig config) {
        if (config == null) {
            return null;
        }
        AlertConfigResponse response = new AlertConfigResponse();
        BeanUtils.copyProperties(config, response);
        return response;
    }

    public static AlertConfigRequest toRequest(AlertConfig config) {
        if (config == null) {
            return null;
        }
        AlertConfigRequest request = new AlertConfigRequest();
        BeanUtils.copyProperties(
            config,
            request,
            "emailParams",
            "dingTalkParams",
            "weComParams",
            "httpCallbackParams",
            "larkParams");
        try {
            if (StringUtils.isNotBlank(config.getEmailParams())) {
                request.setEmailParams(JacksonUtils.read(config.getEmailParams(), AlertEmailParams.class));
            }
            if (StringUtils.isNotBlank(config.getDingTalkParams())) {
                request.setDingTalkParams(
                    JacksonUtils.read(config.getDingTalkParams(), AlertDingTalkParams.class));
            }
            if (StringUtils.isNotBlank(config.getWeComParams())) {
                request.setWeComParams(JacksonUtils.read(config.getWeComParams(), AlertWeComParams.class));
            }
            if (StringUtils.isNotBlank(config.getHttpCallbackParams())) {
                request.setHttpCallbackParams(
                    JacksonUtils.read(config.getHttpCallbackParams(), AlertHttpCallbackParams.class));
            }
            if (StringUtils.isNotBlank(config.getLarkParams())) {
                request.setLarkParams(JacksonUtils.read(config.getLarkParams(), AlertLarkParams.class));
            }
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize alert config params", e);
        }
        return request;
    }

    public static IPage<AlertConfigResponse> toPageResponse(IPage<AlertConfig> page) {
        return DtoAssembler.toPage(page, AlertAssembler::toResponse);
    }

    public static List<AlertConfigResponse> toListResponse(List<AlertConfig> configs) {
        return DtoAssembler.toList(configs, AlertAssembler::toResponse);
    }
}
