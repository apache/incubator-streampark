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
import org.apache.streampark.console.core.bean.DatabaseParam;
import org.apache.streampark.console.core.service.DatabaseService;

import org.apache.shiro.authz.annotation.RequiresPermissions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("flink/database")
public class DatabaseController {

    @Autowired
    private DatabaseService databaseService;

    @PostMapping("create")
    @RequiresPermissions("database:create")
    public RestResponse create(DatabaseParam databaseParam) throws IOException {
        boolean saved = databaseService.createDatabase(databaseParam);
        return RestResponse.success(saved);
    }

    @PostMapping("list")
    @RequiresPermissions("database:view")
    public RestResponse list(DatabaseParam databaseParam, RestRequest request) {
        List<DatabaseParam> databaseParamList =
            databaseService.listDatabases(databaseParam.getCatalogId());
        return RestResponse.success(databaseParamList);
    }

    @PostMapping("delete")
    @RequiresPermissions("database:delete")
    public RestResponse remove(DatabaseParam databaseParam) {
        boolean deleted = databaseService.dropDatabase(databaseParam);
        return RestResponse.success(deleted);
    }
}
