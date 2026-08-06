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

import org.apache.streampark.common.util.HadoopUtils;
import org.apache.streampark.console.base.domain.RestResponse;
import org.apache.streampark.console.core.assembler.SettingAssembler;
import org.apache.streampark.console.core.entity.Setting;
import org.apache.streampark.console.core.request.setting.SettingDockerRequest;
import org.apache.streampark.console.core.request.setting.SettingEmailRequest;
import org.apache.streampark.console.core.request.setting.SettingGetRequest;
import org.apache.streampark.console.core.request.setting.SettingUpdateRequest;
import org.apache.streampark.console.core.service.SettingService;

import org.apache.shiro.authz.annotation.RequiresPermissions;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("setting")
public class SettingController {

    @Autowired
    private SettingService settingService;

    @PostMapping("all")
    @RequiresPermissions("setting:view")
    public RestResponse all() {
        LambdaQueryWrapper<Setting> query =
            new LambdaQueryWrapper<Setting>().orderByAsc(Setting::getOrderNum);
        List<Setting> setting = settingService.list(query);
        return RestResponse.success(SettingAssembler.toListResponse(setting));
    }

    @PostMapping("get")
    public RestResponse get(SettingGetRequest request) {
        Setting setting = settingService.get(request.getKey());
        return RestResponse.success(SettingAssembler.toResponse(setting));
    }

    @PostMapping("update")
    @RequiresPermissions("setting:update")
    public RestResponse update(SettingUpdateRequest request) {
        boolean updated = settingService.update(SettingAssembler.toEntity(request));
        return RestResponse.success(updated);
    }

    @PostMapping("docker")
    @RequiresPermissions("setting:view")
    public RestResponse docker() {
        return RestResponse.success(SettingAssembler.toDockerResponse(settingService.getDockerConfig()));
    }

    @PostMapping("check/docker")
    @RequiresPermissions("setting:view")
    public RestResponse checkDocker(SettingDockerRequest request) {
        return RestResponse.success(
            SettingAssembler.toCheckResponse(
                settingService.checkDocker(SettingAssembler.toDockerConfig(request))));
    }

    @PostMapping("update/docker")
    @RequiresPermissions("setting:update")
    public RestResponse updateDocker(SettingDockerRequest request) {
        boolean updated =
            settingService.updateDocker(SettingAssembler.toDockerConfig(request));
        return RestResponse.success(updated);
    }

    @PostMapping("email")
    @RequiresPermissions("setting:view")
    public RestResponse email() {
        return RestResponse.success(SettingAssembler.toEmailResponse(settingService.getSenderEmail()));
    }

    @PostMapping("check/email")
    @RequiresPermissions("setting:view")
    public RestResponse checkEmail(SettingEmailRequest request) {
        return RestResponse.success(
            SettingAssembler.toCheckResponse(
                settingService.checkEmail(SettingAssembler.toSenderEmail(request))));
    }

    @PostMapping("update/email")
    @RequiresPermissions("setting:update")
    public RestResponse updateEmail(SettingEmailRequest request) {
        boolean updated = settingService.updateEmail(SettingAssembler.toSenderEmail(request));
        return RestResponse.success(updated);
    }

    @PostMapping("check/hadoop")
    public RestResponse checkHadoop() throws IOException {
        HadoopUtils.hdfs().getStatus();
        return RestResponse.success(true);
    }
}
