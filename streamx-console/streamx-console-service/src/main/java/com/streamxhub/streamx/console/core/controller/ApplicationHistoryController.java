/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.console.core.controller;

import com.streamxhub.streamx.common.enums.ExecutionMode;
import com.streamxhub.streamx.common.enums.StorageType;
import com.streamxhub.streamx.console.base.domain.RestResponse;
import com.streamxhub.streamx.console.core.dao.ApplicationMapper;
import com.streamxhub.streamx.console.core.service.ApplicationHistoryService;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Al-assad
 */
@Api(tags = "[flink history]相关操作", consumes = "Content-Type=application/x-www-form-urlencoded")
@Slf4j
@Validated
@RestController
@RequestMapping("flink/history")
public class ApplicationHistoryController {

    private static final int DEFAULT_HISTORY_RECORD_LIMIT = 25;

    private static final int DEFAULT_HISTORY_POD_TMPL_RECORD_LIMIT = 5;

    @Autowired
    private ApplicationHistoryService applicationHistoryService;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private ApplicationMapper applicationMapper;

    @PostMapping("uploadJars")
    @RequiresPermissions("app:create")
    public RestResponse listUploadJars() {
        List<String> jars = applicationHistoryService.listUploadJars(StorageType.LFS, DEFAULT_HISTORY_RECORD_LIMIT);
        return RestResponse.success(jars);
    }

    @PostMapping("k8sNamespaces")
    @RequiresPermissions("app:create")
    public RestResponse listK8sNamespace() {
        List<String> namespaces = applicationMapper.getRecentK8sNamespace(DEFAULT_HISTORY_RECORD_LIMIT);
        return RestResponse.success(namespaces);
    }

    @PostMapping("sessionClusterIds")
    @RequiresPermissions("app:create")
    public RestResponse listSessionClusterId(int executionMode) {
        List<String> clusterIds;
        switch (ExecutionMode.of(executionMode)) {
            case KUBERNETES_NATIVE_SESSION:
            case YARN_SESSION:
            case REMOTE:
                clusterIds = applicationMapper.getRecentK8sClusterId(executionMode, DEFAULT_HISTORY_RECORD_LIMIT);
                break;
            default:
                clusterIds = new ArrayList<>(0);
                break;
        }
        return RestResponse.success(clusterIds);
    }

    @PostMapping("flinkBaseImages")
    @RequiresPermissions("app:create")
    public RestResponse listFlinkBaseImage() {
        List<String> images = applicationMapper.getRecentFlinkBaseImage(DEFAULT_HISTORY_RECORD_LIMIT);
        return RestResponse.success(images);
    }

    @PostMapping("flinkPodTemplates")
    @RequiresPermissions("app:create")
    public RestResponse listPodTemplate() {
        List<String> templates = applicationMapper.getRecentK8sPodTemplate(DEFAULT_HISTORY_POD_TMPL_RECORD_LIMIT);
        return RestResponse.success(templates);
    }

    @PostMapping("flinkJmPodTemplates")
    @RequiresPermissions("app:create")
    public RestResponse listJmPodTemplate() {
        List<String> templates = applicationMapper.getRecentK8sJmPodTemplate(DEFAULT_HISTORY_POD_TMPL_RECORD_LIMIT);
        return RestResponse.success(templates);
    }

    @PostMapping("flinkTmPodTemplates")
    @RequiresPermissions("app:create")
    public RestResponse listTmPodTemplate() {
        List<String> templates = applicationMapper.getRecentK8sTmPodTemplate(DEFAULT_HISTORY_POD_TMPL_RECORD_LIMIT);
        return RestResponse.success(templates);
    }

}
