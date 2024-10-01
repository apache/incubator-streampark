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
import org.apache.streampark.console.core.bean.TableParams;
import org.apache.streampark.console.core.service.TableService;

import org.apache.shiro.authz.annotation.RequiresPermissions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Slf4j
@Validated
@RestController
@RequestMapping("flink/table")
public class TableController {

    @Autowired
    private TableService tableService;

    @PostMapping("create")
    @RequiresPermissions("table:create")
    public RestResponse createTable(TableParams table) {
        boolean saved = tableService.createTable(table);
        return RestResponse.success(saved);
    }

    @PostMapping("column/add")
    @RequiresPermissions("table:column:add")
    public RestResponse addColumn(TableParams table) {
        boolean saved = tableService.addColumn(table);
        return RestResponse.success(saved);
    }

    @GetMapping("column/list")
    @RequiresPermissions("table:column:list")
    public RestResponse listColumns(
                                    @RequestParam String catalogName,
                                    @RequestParam String databaseName,
                                    @RequestParam String tableName) {
        TableParams tableParams = tableService.listColumns(catalogName, databaseName, tableName);
        return RestResponse.success(tableParams);
    }

    @DeleteMapping("column/drop/{catalogName}/{databaseName}/{tableName}/{columnName}")
    @RequiresPermissions("table:column:drop")
    public RestResponse dropColumns(
                                    @PathVariable String catalogName,
                                    @PathVariable String databaseName,
                                    @PathVariable String tableName,
                                    @PathVariable String columnName) {
        boolean dropped = tableService.dropColumn(catalogName, databaseName, tableName, columnName);
        return RestResponse.success(dropped);
    }

    @PostMapping("option/add")
    @RequiresPermissions("option:add")
    public RestResponse addOption(TableParams table) {
        boolean addedOption = tableService.addOption(table);
        return RestResponse.success(addedOption);
    }

    @PostMapping("option/remove")
    @RequiresPermissions("option:remove")
    public RestResponse removeOption(
                                     @RequestParam String catalogName,
                                     @RequestParam String databaseName,
                                     @RequestParam String tableName,
                                     @RequestParam String key) {
        boolean removedOption = tableService.removeOption(catalogName, databaseName, tableName, key);
        return RestResponse.success(removedOption);
    }

    @PostMapping("rename")
    @RequiresPermissions("table:update")
    public RestResponse renameTable(
                                    @RequestParam String catalogName,
                                    @RequestParam String databaseName,
                                    @RequestParam String fromTableName,
                                    @RequestParam String toTableName) {
        boolean renamedOption =
            tableService.renameTable(catalogName, databaseName, fromTableName, toTableName);
        return RestResponse.success(renamedOption);
    }

    @PostMapping("list")
    @RequiresPermissions("table:view")
    public RestResponse listTable(TableParams table) {
        List<TableParams> tableParamsList = tableService.listTables(table);
        if (Objects.nonNull(table.getCatalogId()) && Objects.nonNull(table.getDatabaseName())) {
            return RestResponse.success(tableParamsList);
        } else {
            TreeMap<Long, Map<String, List<TableParams>>> collect =
                tableParamsList.stream()
                    .collect(
                        Collectors.groupingBy(
                            TableParams::getCatalogId,
                            TreeMap::new,
                            Collectors.groupingBy(TableParams::getDatabaseName)));
            return RestResponse.success(collect);
        }
    }
}
