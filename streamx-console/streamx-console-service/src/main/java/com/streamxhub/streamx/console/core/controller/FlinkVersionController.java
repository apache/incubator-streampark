/*
 *  Copyright (c) 2019 The StreamX Project
 *
 * <p>Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.streamxhub.streamx.console.core.controller;

import com.streamxhub.streamx.console.base.domain.RestResponse;
import com.streamxhub.streamx.console.base.exception.ServiceException;
import com.streamxhub.streamx.console.core.entity.FlinkVersion;
import com.streamxhub.streamx.console.core.service.FlinkVersionService;
import lombok.extern.slf4j.Slf4j;
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
@RequestMapping("flink/version")
public class FlinkVersionController {

    @Autowired
    private FlinkVersionService flinkVersionService;

    @PostMapping("list")
    public RestResponse list() {
        List<FlinkVersion> list = flinkVersionService.list();
        return RestResponse.create().data(list);
    }

    @PostMapping("exists")
    public RestResponse exists(FlinkVersion version) {
        boolean checked = flinkVersionService.exists(version);
        return RestResponse.create().data(checked);
    }

    @PostMapping("create")
    public RestResponse create(FlinkVersion version) {
        boolean success = flinkVersionService.create(version);
        return RestResponse.create().data(success);
    }

    @PostMapping("get")
    public RestResponse get(Long id) throws Exception {
        FlinkVersion flinkVersion = flinkVersionService.getById(id);
        flinkVersion.unzipFlinkConf();
        return RestResponse.create().data(flinkVersion);
    }

    @PostMapping("sync")
    public RestResponse sync(Long id) throws Exception {
        flinkVersionService.syncConf(id);
        return RestResponse.create();
    }

    @PostMapping("update")
    public RestResponse update(FlinkVersion version) throws Exception {
        flinkVersionService.update(version);
        return RestResponse.create();
    }

    @PostMapping("default")
    public RestResponse setDefault(Long id) throws ServiceException {
        flinkVersionService.setDefault(id);
        return RestResponse.create();
    }

}
