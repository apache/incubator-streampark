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

import org.apache.streampark.common.enums.ExecutionMode;
import org.apache.streampark.console.base.domain.RestResponse;
import org.apache.streampark.console.core.service.ApplicationService;

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

@Api(tags = "[flink history] related operations", consumes = "Content-Type=application/x-www-form-urlencoded")
@Slf4j
@Validated
@RestController
@RequestMapping("flink/history")
public class ApplicationHistoryController {

    @Autowired
    private ApplicationService applicationService;

    @PostMapping("uploadJars")
    @RequiresPermissions("app:create")
    public RestResponse listUploadJars() {
        List<String> jars = applicationService.historyUploadJars();
        return RestResponse.success(jars);
    }

    @PostMapping("k8sNamespaces")
    @RequiresPermissions("app:create")
    public RestResponse listK8sNamespace() {
        List<String> namespaces = applicationService.getRecentK8sNamespace();
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
                clusterIds = applicationService.getRecentK8sClusterId(executionMode);
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
        List<String> images = applicationService.getRecentFlinkBaseImage();
        return RestResponse.success(images);
    }

    @PostMapping("flinkPodTemplates")
    @RequiresPermissions("app:create")
    public RestResponse listPodTemplate() {
        List<String> templates = applicationService.getRecentK8sPodTemplate();
        return RestResponse.success(templates);
    }

    @PostMapping("flinkJmPodTemplates")
    @RequiresPermissions("app:create")
    public RestResponse listJmPodTemplate() {
        List<String> templates = applicationService.getRecentK8sJmPodTemplate();
        return RestResponse.success(templates);
    }

    @PostMapping("flinkTmPodTemplates")
    @RequiresPermissions("app:create")
    public RestResponse listTmPodTemplate() {
        List<String> templates = applicationService.getRecentK8sTmPodTemplate();
        return RestResponse.success(templates);
    }

}
