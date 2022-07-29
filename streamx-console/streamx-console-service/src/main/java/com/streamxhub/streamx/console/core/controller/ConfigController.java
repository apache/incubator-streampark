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

package com.streamxhub.streamx.console.core.controller;

import com.streamxhub.streamx.common.util.HadoopConfigUtils;
import com.streamxhub.streamx.console.base.domain.RestRequest;
import com.streamxhub.streamx.console.base.domain.RestResponse;
import com.streamxhub.streamx.console.core.entity.Application;
import com.streamxhub.streamx.console.core.entity.ApplicationConfig;
import com.streamxhub.streamx.console.core.service.ApplicationConfigService;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * @author benjobs
 */
@Slf4j
@Validated
@RestController
@RequestMapping("flink/conf")
public class ConfigController {

    @Autowired
    private ApplicationConfigService applicationConfigService;

    @PostMapping("get")
    public RestResponse get(Long id) {
        ApplicationConfig config = applicationConfigService.get(id);
        return RestResponse.success(config);
    }

    @PostMapping("template")
    public RestResponse template() {
        String config = applicationConfigService.readTemplate();
        return RestResponse.success(config);
    }

    @PostMapping("list")
    public RestResponse list(ApplicationConfig config, RestRequest request) {
        IPage<ApplicationConfig> page = applicationConfigService.page(config, request);
        return RestResponse.success(page);
    }

    @PostMapping("history")
    public RestResponse history(Application application) {
        List<ApplicationConfig> history = applicationConfigService.history(application);
        return RestResponse.success(history);
    }

    @PostMapping("delete")
    public RestResponse delete(Long id) {
        Boolean deleted = applicationConfigService.removeById(id);
        return RestResponse.success(deleted);
    }

    @PostMapping("sysHadoopConf")
    @RequiresPermissions("app:create")
    public RestResponse getSystemHadoopConfig(){
        Map<String, Map<String, String>> result = ImmutableMap.of(
            "hadoop", HadoopConfigUtils.readSystemHadoopConf(),
            "hive", HadoopConfigUtils.readSystemHiveConf());
        return RestResponse.success(result);
    }

}
