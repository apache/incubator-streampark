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

import org.apache.streampark.console.core.bean.DockerConfig;
import org.apache.streampark.console.core.bean.ResponseResult;
import org.apache.streampark.console.core.bean.SenderEmail;
import org.apache.streampark.console.core.entity.Setting;
import org.apache.streampark.console.core.request.setting.SettingDockerRequest;
import org.apache.streampark.console.core.request.setting.SettingEmailRequest;
import org.apache.streampark.console.core.request.setting.SettingUpdateRequest;
import org.apache.streampark.console.core.response.setting.SettingCheckResponse;
import org.apache.streampark.console.core.response.setting.SettingDockerResponse;
import org.apache.streampark.console.core.response.setting.SettingEmailResponse;
import org.apache.streampark.console.core.response.setting.SettingResponse;

import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.List;

/** Converts between setting entities/beans and API request/response contracts. */
public final class SettingAssembler {

    private SettingAssembler() {
    }

    public static Setting toEntity(SettingUpdateRequest request) {
        if (request == null) {
            return null;
        }
        Setting setting = new Setting();
        BeanUtils.copyProperties(request, setting);
        return setting;
    }

    public static DockerConfig toDockerConfig(SettingDockerRequest request) {
        return DtoAssembler.toDto(request, DockerConfig.class);
    }

    public static SenderEmail toSenderEmail(SettingEmailRequest request) {
        return DtoAssembler.toDto(request, SenderEmail.class);
    }

    public static SettingResponse toResponse(Setting setting) {
        return DtoAssembler.toDto(setting, SettingResponse.class);
    }

    public static List<SettingResponse> toListResponse(List<Setting> settings) {
        return DtoAssembler.toList(settings, SettingAssembler::toResponse);
    }

    public static SettingDockerResponse toDockerResponse(DockerConfig dockerConfig) {
        return DtoAssembler.toDto(dockerConfig, SettingDockerResponse.class);
    }

    public static SettingEmailResponse toEmailResponse(SenderEmail senderEmail) {
        return DtoAssembler.toDto(senderEmail, SettingEmailResponse.class);
    }

    public static SettingCheckResponse toCheckResponse(ResponseResult<?> result) {
        if (result == null) {
            return null;
        }
        SettingCheckResponse response = new SettingCheckResponse();
        response.setStatus(result.getStatus());
        response.setMsg(result.getMsg());
        response.setResult((Serializable) result.getResult());
        return response;
    }
}
