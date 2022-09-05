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

import org.apache.streampark.console.base.domain.RestResponse;
import org.apache.streampark.console.core.entity.FlinkEnv;
import org.apache.streampark.console.core.service.FlinkEnvService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("flink/env")
public class FlinkEnvController {

    @Autowired
    private FlinkEnvService flinkEnvService;

    @PostMapping("list")
    public RestResponse list() {
        List<FlinkEnv> list = flinkEnvService.list();
        return RestResponse.success(list);
    }

    @PostMapping("exists")
    public RestResponse exists(FlinkEnv version) {
        boolean checked = flinkEnvService.exists(version);
        return RestResponse.success(checked);
    }

    @PostMapping("create")
    public RestResponse create(FlinkEnv version) {
        try {
            flinkEnvService.create(version);
        } catch (Exception e) {
            return RestResponse.success(false).message(e.getMessage());
        }
        return RestResponse.success(true);
    }

    @PostMapping("get")
    public RestResponse get(Long id) throws Exception {
        FlinkEnv flinkEnv = flinkEnvService.getById(id);
        flinkEnv.unzipFlinkConf();
        return RestResponse.success(flinkEnv);
    }

    @PostMapping("sync")
    public RestResponse sync(Long id) throws Exception {
        flinkEnvService.syncConf(id);
        return RestResponse.success();
    }

    @PostMapping("update")
    public RestResponse update(FlinkEnv version) throws Exception {
        try {
            flinkEnvService.update(version);
        } catch (Exception e) {
            return RestResponse.success(false).message(e.getMessage());
        }
        return RestResponse.success(true);
    }

    @PostMapping("default")
    public RestResponse setDefault(Long id) {
        flinkEnvService.setDefault(id);
        return RestResponse.success();
    }

}
