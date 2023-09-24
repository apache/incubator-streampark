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

import org.apache.streampark.console.base.domain.ApiDocConstant;
import org.apache.streampark.console.base.domain.RestResponse;
import org.apache.streampark.console.core.annotation.ApiAccess;
import org.apache.streampark.console.core.annotation.PermissionAction;
import org.apache.streampark.console.core.bean.AppBuildDockerResolvedDetail;
import org.apache.streampark.console.core.entity.AppBuildPipeline;
import org.apache.streampark.console.core.enums.PermissionTypeEnum;
import org.apache.streampark.console.core.service.AppBuildPipeService;
import org.apache.streampark.flink.packer.pipeline.DockerResolvedSnapshot;
import org.apache.streampark.flink.packer.pipeline.PipelineTypeEnum;

import org.apache.shiro.authz.annotation.RequiresPermissions;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Tag(name = "FLINK_APPLICATION_BUILD_PIPELINE_TAG")
@Slf4j
@Validated
@RestController
@RequestMapping("flink/pipe")
public class ApplicationBuildPipelineController {

  @Autowired private AppBuildPipeService appBuildPipeService;

  /**
   * Release application building pipeline.
   *
   * @param appId application id
   * @param forceBuild forced start pipeline or not
   * @return Whether the pipeline was successfully started
   */
  @Operation(
      summary = "Release application",
      tags = {ApiDocConstant.FLINK_APP_OP_TAG})
  @Parameters({
    @Parameter(name = "appId", description = "app id", required = true, example = "100000"),
    @Parameter(
        name = "forceBuild",
        description = "force build",
        required = true,
        example = "false",
        schema = @Schema(defaultValue = "false", implementation = boolean.class))
  })
  @ApiAccess
  @PermissionAction(id = "#appId", type = PermissionTypeEnum.APP)
  @PostMapping(value = "build")
  @RequiresPermissions("app:create")
  public RestResponse buildApplication(Long appId, boolean forceBuild) {
    try {
      boolean actionResult = appBuildPipeService.buildApplication(appId, forceBuild);
      return RestResponse.success(actionResult);
    } catch (Exception e) {
      return RestResponse.success(false).message(e.getMessage());
    }
  }

  /**
   * Get application building pipeline progress detail.
   *
   * @param appId application id
   * @return "pipeline" -> pipeline details, "docker" -> docker resolved snapshot
   */
  @Operation(summary = "Get application release pipeline")
  @ApiAccess
  @PostMapping("/detail")
  @RequiresPermissions("app:view")
  public RestResponse getBuildProgressDetail(Long appId) {
    Map<String, Object> details = new HashMap<>(0);
    Optional<AppBuildPipeline> pipeline = appBuildPipeService.getCurrentBuildPipeline(appId);
    details.put("pipeline", pipeline.map(AppBuildPipeline::toView).orElse(null));

    if (pipeline.isPresent()
        && PipelineTypeEnum.FLINK_NATIVE_K8S_APPLICATION == pipeline.get().getPipeType()) {
      DockerResolvedSnapshot dockerProgress =
          appBuildPipeService.getDockerProgressDetailSnapshot(appId);
      details.put("docker", AppBuildDockerResolvedDetail.of(dockerProgress));
    }
    return RestResponse.success(details);
  }
}
