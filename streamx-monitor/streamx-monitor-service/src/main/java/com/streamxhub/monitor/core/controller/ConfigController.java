/**
 * Copyright (c) 2019 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.streamxhub.monitor.core.controller;

import com.streamxhub.monitor.base.controller.BaseController;
import com.streamxhub.monitor.base.domain.RestResponse;
import com.streamxhub.monitor.core.entity.Application;
import com.streamxhub.monitor.core.entity.ApplicationConfig;
import com.streamxhub.monitor.core.service.ApplicationConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("flink/conf")
public class ConfigController extends BaseController {

    @Autowired
    private ApplicationConfigService applicationConfigService;

    @RequestMapping("get")
    public RestResponse get(Long id) {
        ApplicationConfig config =  applicationConfigService.get(id);
        return RestResponse.create().data(config);
    }

    @RequestMapping("confVer")
    public RestResponse confVer(Application app) {
        List<ApplicationConfig> list =  applicationConfigService.listConf(app.getId());
        return RestResponse.create().data(list);
    }

}
