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

import org.apache.streampark.console.base.domain.RestResponseBody;
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.base.exception.ApiDetailException;
import org.apache.streampark.console.base.web.FormOrJson;
import org.apache.streampark.console.core.assembler.SparkEnvAssembler;
import org.apache.streampark.console.core.entity.SparkEnv;
import org.apache.streampark.console.core.enums.FlinkEnvCheckEnum;
import org.apache.streampark.console.core.request.common.IdRequest;
import org.apache.streampark.console.core.request.spark.SparkEnvCheckRequest;
import org.apache.streampark.console.core.request.spark.SparkEnvCreateRequest;
import org.apache.streampark.console.core.request.spark.SparkEnvUpdateRequest;
import org.apache.streampark.console.core.request.spark.SparkEnvValidityRequest;
import org.apache.streampark.console.core.response.spark.SparkEnvResponse;
import org.apache.streampark.console.core.service.SparkEnvService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

import java.io.IOException;
import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("spark/env")
public class SparkEnvController {

    @Autowired
    private SparkEnvService sparkEnvService;

    @PostMapping("list")
    public RestResponseBody<List<SparkEnvResponse>> list() {
        List<SparkEnv> sparkEnvList = sparkEnvService.list();
        return RestResponseBody.success(SparkEnvAssembler.toListResponse(sparkEnvList));
    }

    @PostMapping("check")
    public RestResponseBody<Integer> check(SparkEnvCheckRequest request) {
        FlinkEnvCheckEnum checkResp = sparkEnvService.check(SparkEnvAssembler.toEntity(request));
        return RestResponseBody.success(checkResp.getCode());
    }

    @PostMapping("create")
    public RestResponseBody<Boolean> create(@Valid @FormOrJson SparkEnvCreateRequest request) {
        try {
            sparkEnvService.create(SparkEnvAssembler.toEntity(request));
        } catch (Exception e) {
            throw new ApiDetailException(e);
        }
        return RestResponseBody.success(true);
    }

    @PostMapping("get")
    public RestResponseBody<SparkEnvResponse> get(@Valid IdRequest request) throws Exception {
        SparkEnv sparkEnv = sparkEnvService.getById(request.getId());
        ApiAlertException.throwIfNull(sparkEnv, "Spark environment not found.");
        sparkEnv.unzipSparkConf();
        SparkEnvResponse response = SparkEnvAssembler.toResponse(sparkEnv);
        return RestResponseBody.success(response);
    }

    @PostMapping("sync")
    public RestResponseBody<Void> sync(@Valid @FormOrJson IdRequest request) throws Exception {
        sparkEnvService.syncConf(request.getId());
        return RestResponseBody.success();
    }

    @PostMapping("update")
    public RestResponseBody<Boolean> update(@Valid @FormOrJson SparkEnvUpdateRequest request) throws IOException {
        sparkEnvService.update(SparkEnvAssembler.toEntity(request));
        return RestResponseBody.success(true);
    }

    @PostMapping("delete")
    public RestResponseBody<Void> delete(@Valid @FormOrJson IdRequest request) {
        sparkEnvService.removeById(request.getId());
        return RestResponseBody.success();
    }

    @PostMapping("validity")
    public RestResponseBody<Boolean> validity(@Valid SparkEnvValidityRequest request) {
        sparkEnvService.validity(request.getId());
        return RestResponseBody.success(true);
    }

    @PostMapping("default")
    public RestResponseBody<Void> setDefault(@Valid @FormOrJson IdRequest request) {
        sparkEnvService.setDefault(request.getId());
        return RestResponseBody.success();
    }
}
