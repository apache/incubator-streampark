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

import org.apache.streampark.console.base.bean.Response;
import org.apache.streampark.console.core.entity.FlinkEnv;
import org.apache.streampark.console.core.enums.FlinkEnvCheckEnum;
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
    public Response<List<FlinkEnv>> list() {
        List<FlinkEnv> flinkEnvList = flinkEnvService.list();
        return Response.success(flinkEnvList);
    }

    @PostMapping("check")
    public Response<Integer> check(FlinkEnv version) {
        FlinkEnvCheckEnum checkResp = flinkEnvService.check(version);
        return Response.success(checkResp.getCode());
    }

    @PostMapping("create")
    public Response<Boolean> create(FlinkEnv version) throws Exception {
        flinkEnvService.create(version);
        return Response.success(true);
    }

    @PostMapping("get")
    public Response<FlinkEnv> get(Long id) throws Exception {
        FlinkEnv flinkEnv = flinkEnvService.getById(id);
        flinkEnv.unzipFlinkConf();
        return Response.success(flinkEnv);
    }

    @PostMapping("sync")
    public Response<Void> sync(Long id) throws Exception {
        flinkEnvService.syncConf(id);
        return Response.success();
    }

    @PostMapping("update")
    public Response<Boolean> update(FlinkEnv version) {
        flinkEnvService.update(version);
        return Response.success(true);
    }

    @PostMapping("delete")
    public Response<Void> delete(Long id) {
        flinkEnvService.removeById(id);
        return Response.success();
    }

    @PostMapping("validity")
    public Response<Boolean> validity(FlinkEnv version) {
        flinkEnvService.validity(version.getId());
        return Response.success(true);
    }

    @PostMapping("default")
    public Response<Void> setDefault(Long id) {
        flinkEnvService.setDefault(id);
        return Response.success();
    }
}
