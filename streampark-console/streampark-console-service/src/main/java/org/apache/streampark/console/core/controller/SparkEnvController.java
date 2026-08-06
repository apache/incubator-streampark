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
import org.apache.streampark.console.base.exception.ApiDetailException;
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

import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("spark/env")
public class SparkEnvController {

    @Autowired
    private SparkEnvService sparkEnvService;

    @PostMapping("list")
    public RestResponse list() {
        List<SparkEnv> sparkEnvList = sparkEnvService.list();
        return RestResponse.success(SparkEnvAssembler.toListResponse(sparkEnvList));
    }

    @PostMapping("check")
    public RestResponse check(SparkEnvCheckRequest request) {
        FlinkEnvCheckEnum checkResp = sparkEnvService.check(SparkEnvAssembler.toEntity(request));
        return RestResponse.success(checkResp.getCode());
    }

    @PostMapping("create")
    public RestResponse create(@Valid SparkEnvCreateRequest request) {
        try {
            sparkEnvService.create(SparkEnvAssembler.toEntity(request));
        } catch (Exception e) {
            throw new ApiDetailException(e);
        }
        return RestResponse.success(true);
    }

    @PostMapping("get")
    public RestResponse get(IdRequest request) throws Exception {
        SparkEnv sparkEnv = sparkEnvService.getById(request.getId());
        sparkEnv.unzipSparkConf();
        SparkEnvResponse response = SparkEnvAssembler.toResponse(sparkEnv);
        return RestResponse.success(response);
    }

    @PostMapping("sync")
    public RestResponse sync(IdRequest request) throws Exception {
        sparkEnvService.syncConf(request.getId());
        return RestResponse.success();
    }

    @PostMapping("update")
    public RestResponse update(@Valid SparkEnvUpdateRequest request) throws Exception {
        try {
            sparkEnvService.update(SparkEnvAssembler.toEntity(request));
        } catch (Exception e) {
            throw new ApiDetailException(e);
        }
        return RestResponse.success(true);
    }

    @PostMapping("delete")
    public RestResponse delete(IdRequest request) {
        sparkEnvService.removeById(request.getId());
        return RestResponse.success();
    }

    @PostMapping("validity")
    public RestResponse validity(SparkEnvValidityRequest request) {
        sparkEnvService.validity(request.getId());
        return RestResponse.success(true);
    }

    @PostMapping("default")
    public RestResponse setDefault(IdRequest request) {
        sparkEnvService.setDefault(request.getId());
        return RestResponse.success();
    }
}
