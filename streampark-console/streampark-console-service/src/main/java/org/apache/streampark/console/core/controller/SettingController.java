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
import org.apache.streampark.console.base.domain.Result;
import org.apache.streampark.console.core.bean.DockerConfig;
import org.apache.streampark.console.core.bean.SenderEmail;
import org.apache.streampark.console.core.entity.Setting;
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
    public Result<List<Setting>> all() {
        LambdaQueryWrapper<Setting> query = new LambdaQueryWrapper<Setting>().orderByAsc(Setting::getOrderNum);
        List<Setting> setting = settingService.list(query);
        return Result.success(setting);
    }

    @PostMapping("get")
    public Result<Setting> get(String key) {
        Setting setting = settingService.get(key);
        return Result.success(setting);
    }

    @PostMapping("update")
    @RequiresPermissions("setting:update")
    public Result<Boolean> update(Setting setting) {
        boolean updated = settingService.update(setting);
        return Result.success(updated);
    }

    @PostMapping("docker")
    @RequiresPermissions("setting:view")
    public Result<DockerConfig> docker() {
        DockerConfig dockerConfig = settingService.getDockerConfig();
        return Result.success(dockerConfig);
    }

    @PostMapping("check/docker")
    @RequiresPermissions("setting:view")
    public Result<Void> checkDocker(DockerConfig dockerConfig) {
        return settingService.checkDocker(dockerConfig);
    }

    @PostMapping("update/docker")
    @RequiresPermissions("setting:update")
    public Result<Boolean> updateDocker(DockerConfig dockerConfig) {
        boolean updated = settingService.updateDocker(dockerConfig);
        return Result.success(updated);
    }

    @PostMapping("email")
    @RequiresPermissions("setting:view")
    public Result<SenderEmail> email() {
        SenderEmail senderEmail = settingService.getSenderEmail();
        return Result.success(senderEmail);
    }

    @PostMapping("check/email")
    @RequiresPermissions("setting:view")
    public Result<Void> checkEmail(SenderEmail senderEmail) {
        return settingService.checkEmail(senderEmail);
    }

    @PostMapping("update/email")
    @RequiresPermissions("setting:update")
    public Result<Boolean> updateEmail(SenderEmail senderEmail) {
        boolean updated = settingService.updateEmail(senderEmail);
        return Result.success(updated);
    }

    @PostMapping("check/hadoop")
    public Result<Boolean> checkHadoop() throws IOException {
        HadoopUtils.hdfs().getStatus();
        return Result.success(true);
    }
}
