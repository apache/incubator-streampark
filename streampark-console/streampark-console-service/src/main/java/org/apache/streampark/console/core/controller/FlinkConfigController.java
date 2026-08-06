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
import org.apache.streampark.console.core.entity.FlinkApplicationConfig;
import org.apache.streampark.console.core.request.common.IdRequest;
import org.apache.streampark.console.core.request.flink.FlinkAppIdRequest;
import org.apache.streampark.console.core.request.flink.FlinkConfListQueryRequest;
import org.apache.streampark.console.core.response.flink.FlinkConfHadoopResponse;
import org.apache.streampark.console.core.response.flink.FlinkConfResponse;
import org.apache.streampark.console.core.service.application.FlinkApplicationConfigService;

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
@RequestMapping("flink/conf")
public class FlinkConfigController {

    @Autowired
    private FlinkApplicationConfigService applicationConfigService;

    @PostMapping("get")
    public RestResponseBody<FlinkConfResponse> get(@Valid IdRequest request) {
        FlinkApplicationConfig config = applicationConfigService.get(request.getId());
        return RestResponseBody.success(FlinkConfAssembler.toResponse(config));
    }

    @PostMapping("template")
    public RestResponseBody<String> template() {
        String config = applicationConfigService.readTemplate();
        return RestResponseBody.success(config);
    }

    @PostMapping("list")
    public RestResponseBody<IPage<FlinkConfResponse>> list(FlinkConfListQueryRequest query, RestRequest request) {
        FlinkApplicationConfig config = FlinkConfAssembler.toEntity(query);
        IPage<FlinkApplicationConfig> page = applicationConfigService.getPage(config, request);
        return RestResponseBody.success(FlinkConfAssembler.toPageResponse(page));
    }

    @PostMapping("history")
    public RestResponseBody<List<FlinkConfResponse>> history(@Valid FlinkAppIdRequest request) {
        List<FlinkApplicationConfig> history =
            applicationConfigService.list(FlinkConfAssembler.toAppId(request));
        return RestResponseBody.success(FlinkConfAssembler.toListResponse(history));
    }

    @PostMapping("delete")
    @RequiresPermissions("conf:delete")
    public RestResponseBody<Boolean> delete(@Valid @FormOrJson IdRequest request) {
        Boolean deleted = applicationConfigService.removeById(request.getId());
        return RestResponseBody.success(deleted);
    }

    @PostMapping("sys_hadoop_conf")
    @RequiresPermissions("app:create")
    public RestResponseBody<FlinkConfHadoopResponse> getSystemHadoopConfig() {
        Map<String, Map<String, String>> result = ImmutableMap.of(
            "hadoop", HadoopConfigUtils.readSystemHadoopConf(),
            "hive", HadoopConfigUtils.readSystemHiveConf());
        return RestResponseBody.success(FlinkConfAssembler.toHadoopResponse(result));
    }
}
