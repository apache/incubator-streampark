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
import org.apache.streampark.console.core.entity.SparkEnv;
import org.apache.streampark.console.core.enums.FlinkEnvCheckEnum;
import org.apache.streampark.console.core.service.SparkEnvService;

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
@RequestMapping("spark/env")
public class SparkEnvController {

    @Autowired
    private SparkEnvService sparkEnvService;

    @PostMapping("list")
    public RestResponse list() {
        List<SparkEnv> sparkEnvList = sparkEnvService.list();
        return RestResponse.success(sparkEnvList);
    }

    @PostMapping("check")
    public RestResponse check(SparkEnv version) {
        FlinkEnvCheckEnum checkResp = sparkEnvService.check(version);
        return RestResponse.success(checkResp.getCode());
    }

    @PostMapping("create")
    public RestResponse create(SparkEnv version) {
        try {
            sparkEnvService.create(version);
        } catch (Exception e) {
            throw new ApiDetailException(e);
        }
        return RestResponse.success(true);
    }

    @PostMapping("get")
    public RestResponse get(Long id) throws Exception {
        SparkEnv sparkEnv = sparkEnvService.getById(id);
        sparkEnv.unzipSparkConf();
        return RestResponse.success(sparkEnv);
    }

    @PostMapping("sync")
    public RestResponse sync(Long id) throws Exception {
        sparkEnvService.syncConf(id);
        return RestResponse.success();
    }

    @PostMapping("update")
    public RestResponse update(SparkEnv version) throws Exception {
        try {
            sparkEnvService.update(version);
        } catch (Exception e) {
            throw new ApiDetailException(e);
        }
        return RestResponse.success(true);
    }

    @PostMapping("delete")
    public RestResponse delete(Long id) {
        sparkEnvService.removeById(id);
        return RestResponse.success();
    }

    @PostMapping("validity")
    public RestResponse validity(SparkEnv version) {
        sparkEnvService.validity(version.getId());
        return RestResponse.success(true);
    }

    @PostMapping("default")
    public RestResponse setDefault(Long id) {
        sparkEnvService.setDefault(id);
        return RestResponse.success();
    }
}
