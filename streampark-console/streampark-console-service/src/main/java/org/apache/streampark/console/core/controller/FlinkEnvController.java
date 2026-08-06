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

import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.base.domain.RestResponseBody;
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.base.web.FormOrJson;
import org.apache.streampark.console.core.assembler.FlinkEnvAssembler;
import org.apache.streampark.console.core.entity.FlinkEnv;
import org.apache.streampark.console.core.enums.FlinkEnvCheckEnum;
import org.apache.streampark.console.core.request.common.IdRequest;
import org.apache.streampark.console.core.request.flink.FlinkEnvCheckRequest;
import org.apache.streampark.console.core.request.flink.FlinkEnvCreateRequest;
import org.apache.streampark.console.core.request.flink.FlinkEnvPageQueryRequest;
import org.apache.streampark.console.core.request.flink.FlinkEnvUpdateRequest;
import org.apache.streampark.console.core.response.flink.FlinkEnvResponse;
import org.apache.streampark.console.core.service.FlinkEnvService;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("flink/env")
public class FlinkEnvController {

    @Autowired
    private FlinkEnvService flinkEnvService;

    @PostMapping("page")
    public RestResponseBody<IPage<FlinkEnvResponse>> findPage(FlinkEnvPageQueryRequest query, RestRequest restRequest) {
        FlinkEnv flinkEnv = FlinkEnvAssembler.toEntity(query);
        IPage<FlinkEnv> envs = flinkEnvService.findPage(flinkEnv, restRequest);
        return RestResponseBody.success(FlinkEnvAssembler.toPageResponse(envs));
    }

    @PostMapping("list")
    public RestResponseBody<List<FlinkEnvResponse>> list() {
        List<FlinkEnv> flinkEnvList = flinkEnvService.list();
        return RestResponseBody.success(FlinkEnvAssembler.toListResponse(flinkEnvList));
    }

    @PostMapping("check")
    public RestResponseBody<Integer> check(FlinkEnvCheckRequest request) {
        FlinkEnvCheckEnum checkResp = flinkEnvService.check(FlinkEnvAssembler.toEntity(request));
        return RestResponseBody.success(checkResp.getCode());
    }

    @PostMapping("create")
    public RestResponseBody<Boolean> create(@Valid @FormOrJson FlinkEnvCreateRequest request) {
        flinkEnvService.create(FlinkEnvAssembler.toEntity(request));
        return RestResponseBody.success(true);
    }

    @PostMapping("get")
    public RestResponseBody<FlinkEnvResponse> get(@Valid IdRequest request) throws Exception {
        FlinkEnv flinkEnv = flinkEnvService.getById(request.getId());
        ApiAlertException.throwIfNull(flinkEnv, "Flink environment not found.");
        flinkEnv.unzipFlinkConf();
        return RestResponseBody.success(FlinkEnvAssembler.toResponse(flinkEnv));
    }

    @PostMapping("sync")
    public RestResponseBody<Void> sync(@Valid @FormOrJson IdRequest request) throws Exception {
        flinkEnvService.syncConf(request.getId());
        return RestResponseBody.success();
    }

    @PostMapping("update")
    public RestResponseBody<Boolean> update(@Valid @FormOrJson FlinkEnvUpdateRequest request) {
        flinkEnvService.update(FlinkEnvAssembler.toEntity(request));
        return RestResponseBody.success(true);
    }

    @PostMapping("delete")
    public RestResponseBody<Void> delete(@Valid @FormOrJson IdRequest request) {
        flinkEnvService.removeById(request.getId());
        return RestResponseBody.success();
    }

    @PostMapping("validity")
    public RestResponseBody<Boolean> validity(FlinkEnvCheckRequest request) {
        flinkEnvService.validity(request.getId());
        return RestResponseBody.success(true);
    }

    @PostMapping("default")
    public RestResponseBody<Void> setDefault(@Valid @FormOrJson IdRequest request) {
        flinkEnvService.setDefault(request.getId());
        return RestResponseBody.success();
    }
}
