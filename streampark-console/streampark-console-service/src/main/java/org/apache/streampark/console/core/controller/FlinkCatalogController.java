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
import org.apache.streampark.console.base.domain.RestResponse;
import org.apache.streampark.console.base.mybatis.pager.MybatisPager;
import org.apache.streampark.console.core.entity.FlinkCatalog;
import org.apache.streampark.console.core.service.FlinkCatalogService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotNull;

@Tag(name = "FLINK_CATALOG_TAG")
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("flink/catalog")
public class FlinkCatalogController {

  private final FlinkCatalogService flinkCatalogService;

  @Operation(summary = "List catalog")
  @GetMapping("list")
  public RestResponse list(RestRequest request) {
    return RestResponse.success(
        flinkCatalogService.page(new MybatisPager<FlinkCatalog>().getDefaultPage(request)));
  }

  @Operation(summary = "Create flink catalog")
  @PostMapping("create")
  public RestResponse create(@RequestBody FlinkCatalog flinkCatalog) {
    flinkCatalogService.create(flinkCatalog);
    return RestResponse.success();
  }

  @Operation(summary = "Check flink catalog name")
  @GetMapping("check/name")
  public RestResponse checkName(
      @NotNull(message = "The flink catalog name cannot be null") @RequestParam("name")
          String name) {
    return RestResponse.success(flinkCatalogService.existsByCatalogName(name));
  }

  @Operation(summary = "Update flink catalog")
  @PutMapping("update")
  public RestResponse update(@RequestBody FlinkCatalog flinkCatalog) {
    flinkCatalogService.update(flinkCatalog);
    return RestResponse.success();
  }

  @Operation(summary = "Get flink catalog by id")
  @GetMapping("get/{id}")
  public RestResponse get(@PathVariable Long id) {
    return RestResponse.success(flinkCatalogService.getById(id));
  }

  @Operation(summary = "Delete flink catalog by id")
  @DeleteMapping("delete/{id}")
  public RestResponse delete(@PathVariable Long id) {
    flinkCatalogService.removeById(id);
    return RestResponse.success();
  }
}
