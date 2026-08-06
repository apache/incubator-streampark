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

import org.apache.streampark.common.enums.FlinkDeployMode;
import org.apache.streampark.console.base.domain.RestResponseBody;
import org.apache.streampark.console.core.request.flink.FlinkHistoryDeployModeRequest;
import org.apache.streampark.console.core.service.application.FlinkApplicationInfoService;

import org.apache.shiro.authz.annotation.RequiresPermissions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("flink/history")
public class FlinkApplicationHistoryController {

    @Autowired
    private FlinkApplicationInfoService applicationInfoService;

    @PostMapping("k8s_namespaces")
    @RequiresPermissions("app:create")
    public RestResponseBody<List<String>> listK8sNamespace() {
        List<String> namespaces = applicationInfoService.listRecentK8sNamespace();
        return RestResponseBody.success(namespaces);
    }

    @PostMapping("session_cluster_ids")
    @RequiresPermissions("app:create")
    public RestResponseBody<List<String>> listSessionClusterId(FlinkHistoryDeployModeRequest request) {
        List<String> clusterIds;
        switch (FlinkDeployMode.of(request.getDeployMode())) {
            case KUBERNETES_NATIVE_SESSION:
            case YARN_SESSION:
            case REMOTE:
                clusterIds = applicationInfoService.listRecentK8sClusterId(request.getDeployMode());
                break;
            default:
                clusterIds = new ArrayList<>(0);
                break;
        }
        return RestResponseBody.success(clusterIds);
    }

    @PostMapping("flink_base_images")
    @RequiresPermissions("app:create")
    public RestResponseBody<List<String>> listFlinkBaseImage() {
        List<String> images = applicationInfoService.listRecentFlinkBaseImage();
        return RestResponseBody.success(images);
    }

    @PostMapping("flink_pod_templates")
    @RequiresPermissions("app:create")
    public RestResponseBody<List<String>> listPodTemplate() {
        List<String> templates = applicationInfoService.listRecentK8sPodTemplate();
        return RestResponseBody.success(templates);
    }

    @PostMapping("flink_jm_pod_templates")
    @RequiresPermissions("app:create")
    public RestResponseBody<List<String>> listJmPodTemplate() {
        List<String> templates = applicationInfoService.listRecentK8sJmPodTemplate();
        return RestResponseBody.success(templates);
    }

    @PostMapping("flink_tm_pod_templates")
    @RequiresPermissions("app:create")
    public RestResponseBody<List<String>> listTmPodTemplate() {
        List<String> templates = applicationInfoService.listRecentK8sTmPodTemplate();
        return RestResponseBody.success(templates);
    }
}
