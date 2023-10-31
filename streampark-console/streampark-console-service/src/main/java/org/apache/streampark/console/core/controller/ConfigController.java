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
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.entity.ApplicationConfig;
import org.apache.streampark.console.core.service.ApplicationConfigService;

import org.apache.shiro.authz.annotation.RequiresPermissions;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.google.common.collect.ImmutableMap;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "CONFIG_TAG")
@Slf4j
@Validated
@RestController
@RequestMapping("flink/conf")
public class ConfigController {

  @Autowired private ApplicationConfigService applicationConfigService;

  @Operation(summary = "Get config")
  @PostMapping("get")
  public RestResponse get(Long id) {
    ApplicationConfig config = applicationConfigService.get(id);
    return RestResponse.success(config);
  }

  @Operation(summary = "Get the flink application conf-template")
  @PostMapping("template")
  public RestResponse template() {
    String config = applicationConfigService.readTemplate();
    return RestResponse.success(config);
  }

  @Operation(summary = "List the application configs")
  @PostMapping("list")
  public RestResponse list(ApplicationConfig config, RestRequest request) {
    IPage<ApplicationConfig> page = applicationConfigService.getPage(config, request);
    return RestResponse.success(page);
  }

  @Operation(summary = "List application config histories")
  @PostMapping("history")
  public RestResponse history(Application application) {
    List<ApplicationConfig> history = applicationConfigService.list(application);
    return RestResponse.success(history);
  }

  @Operation(summary = "Delete config")
  @PostMapping("delete")
  @RequiresPermissions("conf:delete")
  public RestResponse delete(Long id) {
    Boolean deleted = applicationConfigService.removeById(id);
    return RestResponse.success(deleted);
  }

  @Operation(summary = "Get system hadoop configs")
  @PostMapping("sysHadoopConf")
  @RequiresPermissions("app:create")
  public RestResponse getSystemHadoopConfig() {
    Map<String, Map<String, String>> result =
        ImmutableMap.of(
            "hadoop", HadoopConfigUtils.readSystemHadoopConf(),
            "hive", HadoopConfigUtils.readSystemHiveConf());
    return RestResponse.success(result);
  }
}
