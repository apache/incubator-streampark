/*
 * Copyright 2019 The StreamX Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.console.core.controller;

import com.streamxhub.streamx.common.util.HadoopUtils;
import com.streamxhub.streamx.console.base.domain.RestResponse;
import com.streamxhub.streamx.console.core.entity.Setting;
import com.streamxhub.streamx.console.core.service.SettingService;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author benjobs
 */
@Slf4j
@Validated
@RestController
@RequestMapping("flink/setting")
public class SettingController {

    @Autowired
    private SettingService settingService;

    @PostMapping("all")
    @RequiresPermissions("setting:view")
    public RestResponse all() {
        LambdaQueryWrapper<Setting> query = new QueryWrapper<Setting>().lambda().orderByAsc(Setting::getOrderNum);
        List<Setting> setting = settingService.list(query);
        return RestResponse.success(setting);
    }

    @PostMapping("get")
    public RestResponse get(String key) {
        Setting setting = settingService.get(key);
        return RestResponse.success(setting);
    }

    @PostMapping("weburl")
    public RestResponse weburl() {
        String url = settingService.getStreamXAddress();
        return RestResponse.success(url == null ? null : url.trim());
    }

    @PostMapping("update")
    @RequiresPermissions("setting:update")
    public RestResponse update(Setting setting) {
        boolean updated = settingService.update(setting);
        return RestResponse.success(updated);
    }

    @PostMapping("checkHadoop")
    public RestResponse checkHadoop() {
        try {
            HadoopUtils.hdfs().getStatus();
            return RestResponse.success(true);
        } catch (Exception e) {
            return RestResponse.success(false).message(e.getMessage());
        }
    }

}
