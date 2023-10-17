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
import org.apache.streampark.console.base.exception.InternalException;
import org.apache.streampark.console.core.entity.Datasource;
import org.apache.streampark.console.core.service.datasource.DatasourceService;
import org.apache.streampark.console.core.service.datasource.config.DatasourceColumnInfo;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "DATASOURCE_TAG")
@Slf4j
@Validated
@RestController
@RequestMapping("flink/datasource")
public class DatasourceController {

  @Autowired private DatasourceService datasourceService;

  @Operation(summary = "List datasources")
  @PostMapping("list")
  public RestResponse list() {
    List<Datasource> datasources = datasourceService.list();
    return RestResponse.success(datasources);
  }

  @Operation(summary = "Get databases")
  @PostMapping("databases")
  public RestResponse getDatabases(Long id) {
    List<String> databases = datasourceService.getDatabases(id);
    return RestResponse.success(databases);
  }

  @Operation(summary = "Get tables")
  @PostMapping("tables")
  public RestResponse getTables(Long id) {
    List<String> tables = datasourceService.getTables(id);
    return RestResponse.success(tables);
  }

  @Operation(summary = "Get columns")
  @PostMapping("columns")
  public RestResponse getColumns(Long id, String tableName) {
    List<DatasourceColumnInfo> columns = datasourceService.getColumns(id, tableName);
    return RestResponse.success(columns);
  }

  @Operation(summary = "Get All Support Type")
  @PostMapping("getAllSupportType")
  public RestResponse getAllSupportType() {
    return RestResponse.success(datasourceService.getAllSupportType());
  }

  @Operation(summary = "Get flink ddl")
  @PostMapping("flinkDdl")
  public RestResponse getFlinkDdl(Long id, String tableName) {
    String flinkDdl = datasourceService.getFlinkDdl(id, tableName);
    return RestResponse.success(flinkDdl);
  }

  @Operation(summary = "Datasource test connection")
  @PostMapping("test")
  public RestResponse check(Datasource datasource) {
    Boolean result = datasourceService.testConnection(datasource);
    return RestResponse.success(result);
  }

  @Operation(summary = "Create datasource")
  @PostMapping("create")
  public RestResponse create(Datasource datasource) {
    return datasourceService.create(datasource);
  }

  @Operation(summary = "Update datasource")
  @PostMapping("update")
  public RestResponse update(Datasource datasource) {
    datasourceService.update(datasource);
    return RestResponse.success();
  }

  @Operation(summary = "Get datasource")
  @PostMapping("get")
  public RestResponse get(Long id) throws InternalException {
    Datasource datasource = datasourceService.getById(id);
    return RestResponse.success(datasource);
  }

  @Operation(summary = "Check datasource exist")
  @PostMapping(value = "/exists")
  public RestResponse verifyDatasource(@RequestBody Datasource datasource) {
    boolean exist = datasourceService.exist(datasource);
    return RestResponse.success(exist);
  }

  @Operation(summary = "Delete datasource")
  @PostMapping("delete")
  public RestResponse delete(Long id) {
    datasourceService.delete(id);
    return RestResponse.success();
  }
}
