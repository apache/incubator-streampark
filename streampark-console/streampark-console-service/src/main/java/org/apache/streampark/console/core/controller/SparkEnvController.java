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

import org.apache.streampark.console.base.domain.Result;
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
    public Result<List<SparkEnv>> list() {
        List<SparkEnv> sparkEnvList = sparkEnvService.list();
        return Result.success(sparkEnvList);
    }

    @PostMapping("check")
    public Result<Integer> check(SparkEnv version) {
        FlinkEnvCheckEnum checkResp = sparkEnvService.check(version);
        return Result.success(checkResp.getCode());
    }

    @PostMapping("create")
    public Result<Boolean> create(SparkEnv version) {
        try {
            sparkEnvService.create(version);
        } catch (Exception e) {
            throw new ApiDetailException(e);
        }
        return Result.success(true);
    }

    @PostMapping("get")
    public Result<SparkEnv> get(Long id) throws Exception {
        SparkEnv sparkEnv = sparkEnvService.getById(id);
        sparkEnv.unzipSparkConf();
        return Result.success(sparkEnv);
    }

    @PostMapping("sync")
    public Result<Void> sync(Long id) throws Exception {
        sparkEnvService.syncConf(id);
        return Result.success();
    }

    @PostMapping("update")
    public Result<Boolean> update(SparkEnv version) throws Exception {
        try {
            sparkEnvService.update(version);
        } catch (Exception e) {
            throw new ApiDetailException(e);
        }
        return Result.success(true);
    }

    @PostMapping("delete")
    public Result<Void> delete(Long id) {
        sparkEnvService.removeById(id);
        return Result.success();
    }

    @PostMapping("validity")
    public Result<Boolean> validity(SparkEnv version) {
        sparkEnvService.validity(version.getId());
        return Result.success(true);
    }

    @PostMapping("default")
    public Result<Void> setDefault(Long id) {
        sparkEnvService.setDefault(id);
        return Result.success();
    }
}
