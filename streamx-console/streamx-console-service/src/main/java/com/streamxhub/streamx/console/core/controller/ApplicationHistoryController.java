/*
 *  Copyright (c) 2021 The StreamX Project
 *
 * <p>Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.streamxhub.streamx.console.core.controller;

import com.streamxhub.streamx.common.enums.StorageType;
import com.streamxhub.streamx.console.base.domain.RestResponse;
import com.streamxhub.streamx.console.core.service.ApplicationHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author Al-assad
 */
@Slf4j
@Validated
@RestController
@RequestMapping("flink/history")
public class ApplicationHistoryController {

    private final int DEFAULT_HISTORY_RECORD_LIMIT = 25;

    @Autowired
    private ApplicationHistoryService applicationHistoryService;

    @GetMapping("uploadJars")
    @RequiresPermissions("app:create")
    public RestResponse listUploadJars() {
        List<String> jars = applicationHistoryService.listUploadJars(StorageType.LFS, DEFAULT_HISTORY_RECORD_LIMIT);
        return RestResponse.create().data(jars);
    }






}
