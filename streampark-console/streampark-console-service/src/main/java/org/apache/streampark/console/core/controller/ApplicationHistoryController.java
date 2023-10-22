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

import org.apache.streampark.common.enums.FlinkExecutionMode;
import org.apache.streampark.console.base.domain.RestResponse;
import org.apache.streampark.console.core.service.application.ApplicationInfoService;

import org.apache.shiro.authz.annotation.RequiresPermissions;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@Tag(name = "FLINK_APPLICATION_HISTORY_TAG")
@Slf4j
@Validated
@RestController
@RequestMapping("flink/history")
public class ApplicationHistoryController {

  @Autowired private ApplicationInfoService applicationInfoService;

  @Operation(summary = "List the upload jar history records")
  @PostMapping("uploadJars")
  @RequiresPermissions("app:create")
  public RestResponse listUploadJars() {
    List<String> jars = applicationInfoService.listHistoryUploadJars();
    return RestResponse.success(jars);
  }

  @Operation(summary = "List the k8s namespace history records")
  @PostMapping("k8sNamespaces")
  @RequiresPermissions("app:create")
  public RestResponse listK8sNamespace() {
    List<String> namespaces = applicationInfoService.listRecentK8sNamespace();
    return RestResponse.success(namespaces);
  }

  @Operation(summary = "List the session cluster history records")
  @PostMapping("sessionClusterIds")
  @RequiresPermissions("app:create")
  public RestResponse listSessionClusterId(int executionMode) {
    List<String> clusterIds;
    switch (FlinkExecutionMode.of(executionMode)) {
      case KUBERNETES_NATIVE_SESSION:
      case YARN_SESSION:
      case REMOTE:
        clusterIds = applicationInfoService.listRecentK8sClusterId(executionMode);
        break;
      default:
        clusterIds = new ArrayList<>(0);
        break;
    }
    return RestResponse.success(clusterIds);
  }

  @Operation(summary = "List the flink base image history records")
  @PostMapping("flinkBaseImages")
  @RequiresPermissions("app:create")
  public RestResponse listFlinkBaseImage() {
    List<String> images = applicationInfoService.listRecentFlinkBaseImage();
    return RestResponse.success(images);
  }

  @Operation(summary = "List the flink pod template history records")
  @PostMapping("flinkPodTemplates")
  @RequiresPermissions("app:create")
  public RestResponse listPodTemplate() {
    List<String> templates = applicationInfoService.listRecentK8sPodTemplate();
    return RestResponse.success(templates);
  }

  @Operation(summary = "List the flink JM pod template history records")
  @PostMapping("flinkJmPodTemplates")
  @RequiresPermissions("app:create")
  public RestResponse listJmPodTemplate() {
    List<String> templates = applicationInfoService.listRecentK8sJmPodTemplate();
    return RestResponse.success(templates);
  }

  @Operation(summary = "List the flink TM pod template history records")
  @PostMapping("flinkTmPodTemplates")
  @RequiresPermissions("app:create")
  public RestResponse listTmPodTemplate() {
    List<String> templates = applicationInfoService.listRecentK8sTmPodTemplate();
    return RestResponse.success(templates);
  }
}
