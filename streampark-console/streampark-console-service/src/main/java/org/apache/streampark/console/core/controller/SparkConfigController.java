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

import org.apache.streampark.common.util.HadoopConfigUtils;
import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.base.domain.RestResponseBody;
import org.apache.streampark.console.base.web.FormOrJson;
import org.apache.streampark.console.core.assembler.FlinkConfAssembler;
import org.apache.streampark.console.core.assembler.SparkConfigAssembler;
import org.apache.streampark.console.core.entity.SparkApplicationConfig;
import org.apache.streampark.console.core.request.common.IdRequest;
import org.apache.streampark.console.core.request.spark.SparkConfHistoryRequest;
import org.apache.streampark.console.core.request.spark.SparkConfListQueryRequest;
import org.apache.streampark.console.core.response.flink.FlinkConfHadoopResponse;
import org.apache.streampark.console.core.response.spark.SparkConfResponse;
import org.apache.streampark.console.core.service.application.SparkApplicationConfigService;

import org.apache.shiro.authz.annotation.RequiresPermissions;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

import java.util.List;
import java.util.Map;

@Slf4j
@Validated
@RestController
@RequestMapping("spark/conf")
public class SparkConfigController {

    @Autowired
    private SparkApplicationConfigService applicationConfigService;

    @PostMapping("get")
    public RestResponseBody<SparkConfResponse> get(@Valid IdRequest request) {
        SparkApplicationConfig config = applicationConfigService.get(request.getId());
        return RestResponseBody.success(SparkConfigAssembler.toResponse(config));
    }

    @PostMapping("template")
    public RestResponseBody<String> template() {
        String config = applicationConfigService.readTemplate();
        return RestResponseBody.success(config);
    }

    @PostMapping("list")
    public RestResponseBody<IPage<SparkConfResponse>> list(SparkConfListQueryRequest query, RestRequest request) {
        SparkApplicationConfig configParam = SparkConfigAssembler.toEntity(query);
        IPage<SparkApplicationConfig> page = applicationConfigService.getPage(configParam, request);
        return RestResponseBody.success(SparkConfigAssembler.toPageResponse(page));
    }

    @PostMapping("history")
    public RestResponseBody<List<SparkConfResponse>> history(@Valid SparkConfHistoryRequest request) {
        List<SparkApplicationConfig> history = applicationConfigService.list(request.getId());
        return RestResponseBody.success(SparkConfigAssembler.toListResponse(history));
    }

    @PostMapping("delete")
    @RequiresPermissions("conf:delete")
    public RestResponseBody<Boolean> delete(@Valid @FormOrJson IdRequest request) {
        Boolean deleted = applicationConfigService.removeById(request.getId());
        return RestResponseBody.success(deleted);
    }

    @PostMapping("sysHadoopConf")
    @RequiresPermissions("app:create")
    public RestResponseBody<FlinkConfHadoopResponse> getSystemHadoopConfig() {
        Map<String, Map<String, String>> result = ImmutableMap.of(
            "hadoop", HadoopConfigUtils.readSystemHadoopConf(),
            "hive", HadoopConfigUtils.readSystemHiveConf());
        return RestResponseBody.success(FlinkConfAssembler.toHadoopResponse(result));
    }
}
