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
import org.apache.streampark.console.core.annotation.Permission;
import org.apache.streampark.console.core.bean.FlinkCatalogParams;
import org.apache.streampark.console.core.entity.FlinkCatalog;
import org.apache.streampark.console.core.service.FlinkCatalogService;
import org.apache.streampark.console.core.util.ServiceHelper;

import org.apache.shiro.authz.annotation.RequiresPermissions;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@Slf4j
@Validated
@RestController
@RequestMapping("flink/catalog")
public class FlinkCatalogController {

    @Autowired
    FlinkCatalogService catalogService;

    @Permission(team = "#catalog.teamId")
    @PostMapping("create")
    @RequiresPermissions("catalog:create")
    public RestResponse create(FlinkCatalogParams catalog) throws IOException {
        Long userId = ServiceHelper.getUserId();
        boolean saved = catalogService.create(catalog, userId);
        return RestResponse.success(saved);
    }

    @PostMapping("list")
    @Permission(team = "#app.teamId")
    @RequiresPermissions("catalog:view")
    public RestResponse list(FlinkCatalogParams catalog, RestRequest request) {
        IPage<FlinkCatalogParams> catalogList = catalogService.page(catalog, request);
        return RestResponse.success(catalogList);
    }

    @GetMapping("get/{catalogName}")
    @Permission(team = "#teamId")
    @RequiresPermissions("catalog:view")
    public RestResponse get(@PathVariable String catalogName, Long teamId) {
        FlinkCatalog catalog = catalogService.getCatalog(catalogName);
        return RestResponse.success(FlinkCatalogParams.of(catalog));
    }

    @PostMapping("delete")
    @Permission(team = "#app.teamId")
    @RequiresPermissions("catalog:delete")
    public RestResponse remove(FlinkCatalogParams catalog, RestRequest request) {
        boolean deleted = catalogService.remove(catalog.getId());
        return RestResponse.success(deleted);
    }

    @PostMapping("update")
    @Permission(team = "#app.teamId")
    @RequiresPermissions("catalog:update")
    public RestResponse update(FlinkCatalogParams catalog) {
        Long userId = ServiceHelper.getUserId();
        boolean updated = catalogService.update(catalog, userId);
        return RestResponse.success(updated);
    }
}
