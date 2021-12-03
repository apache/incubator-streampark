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

import com.streamxhub.streamx.common.enums.ExecutionMode;
import com.streamxhub.streamx.common.enums.StorageType;
import com.streamxhub.streamx.console.base.domain.RestResponse;
import com.streamxhub.streamx.console.core.dao.ApplicationMapper;
import com.streamxhub.streamx.console.core.service.ApplicationHistoryService;
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
@Slf4j
@Validated
@RestController
@RequestMapping("flink/history")
public class ApplicationHistoryController {

    private final int defaultHistoryRecordLimit = 25;

    private final int defaultHistoryPodTmplRecordLimit = 5;

    @Autowired
    private ApplicationHistoryService applicationHistoryService;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private ApplicationMapper applicationMapper;

    @PostMapping("uploadJars")
    @RequiresPermissions("app:create")
    public RestResponse listUploadJars() {
        List<String> jars = applicationHistoryService.listUploadJars(StorageType.LFS, defaultHistoryRecordLimit);
        return RestResponse.create().data(jars);
    }

    @PostMapping("k8sNamespaces")
    @RequiresPermissions("app:create")
    public RestResponse listK8sNamespace() {
        List<String> namespaces = applicationMapper.getRecentK8sNamespace(defaultHistoryRecordLimit);
        return RestResponse.create().data(namespaces);
    }

    @PostMapping("sessionClusterIds")
    @RequiresPermissions("app:create")
    public RestResponse listSessionClusterId(int executionMode) {
        List<String> clusterIds;
        switch (ExecutionMode.of(executionMode)) {
            case KUBERNETES_NATIVE_SESSION:
            case YARN_SESSION:
            case REMOTE:
                clusterIds = applicationMapper.getRecentK8sClusterId(executionMode, defaultHistoryRecordLimit);
                break;
            default:
                clusterIds = new ArrayList<>(0);
                break;
        }
        return RestResponse.create().data(clusterIds);
    }

    @PostMapping("flinkBaseImages")
    @RequiresPermissions("app:create")
    public RestResponse listFlinkBaseImage() {
        List<String> images = applicationMapper.getRecentFlinkBaseImage(defaultHistoryRecordLimit);
        return RestResponse.create().data(images);
    }

    @PostMapping("flinkPodTemplates")
    @RequiresPermissions("app:create")
    public RestResponse listPodTemplate() {
        List<String> templates = applicationMapper.getRecentK8sPodTemplate(defaultHistoryPodTmplRecordLimit);
        return RestResponse.create().data(templates);
    }

    @PostMapping("flinkJmPodTemplates")
    @RequiresPermissions("app:create")
    public RestResponse listJmPodTemplate() {
        List<String> templates = applicationMapper.getRecentK8sJmPodTemplate(defaultHistoryPodTmplRecordLimit);
        return RestResponse.create().data(templates);
    }

    @PostMapping("flinkTmPodTemplates")
    @RequiresPermissions("app:create")
    public RestResponse listTmPodTemplate() {
        List<String> templates = applicationMapper.getRecentK8sTmPodTemplate(defaultHistoryPodTmplRecordLimit);
        return RestResponse.create().data(templates);
    }

}
