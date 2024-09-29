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
import org.apache.streampark.console.base.domain.RestResponse;
import org.apache.streampark.console.core.entity.FlinkApplication;
import org.apache.streampark.console.core.entity.FlinkApplicationConfig;
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
    public RestResponse get(Long id) {
        FlinkApplicationConfig config = applicationConfigService.get(id);
        return RestResponse.success(config);
    }

    @PostMapping("template")
    public RestResponse template() {
        String config = applicationConfigService.readTemplate();
        return RestResponse.success(config);
    }

    @PostMapping("list")
    public RestResponse list(FlinkApplicationConfig config, RestRequest request) {
        IPage<FlinkApplicationConfig> page = applicationConfigService.getPage(config, request);
        return RestResponse.success(page);
    }

    @PostMapping("history")
    public RestResponse history(FlinkApplication application) {
        List<FlinkApplicationConfig> history = applicationConfigService.list(application.getId());
        return RestResponse.success(history);
    }

    @PostMapping("delete")
    @RequiresPermissions("conf:delete")
    public RestResponse delete(Long id) {
        Boolean deleted = applicationConfigService.removeById(id);
        return RestResponse.success(deleted);
    }

    @PostMapping("sys_hadoop_conf")
    @RequiresPermissions("app:create")
    public RestResponse getSystemHadoopConfig() {
        Map<String, Map<String, String>> result = ImmutableMap.of(
            "hadoop", HadoopConfigUtils.readSystemHadoopConf(),
            "hive", HadoopConfigUtils.readSystemHiveConf());
        return RestResponse.success(result);
    }
}
