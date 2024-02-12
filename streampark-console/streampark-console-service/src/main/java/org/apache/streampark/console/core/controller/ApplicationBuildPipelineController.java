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
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.core.annotation.ApiAccess;
import org.apache.streampark.console.core.annotation.PermissionAction;
import org.apache.streampark.console.core.bean.AppBuildDockerResolvedDetail;
import org.apache.streampark.console.core.entity.AppBuildPipeline;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.entity.ApplicationLog;
import org.apache.streampark.console.core.entity.FlinkEnv;
import org.apache.streampark.console.core.enums.PermissionType;
import org.apache.streampark.console.core.service.AppBuildPipeService;
import org.apache.streampark.console.core.service.ApplicationLogService;
import org.apache.streampark.console.core.service.ApplicationService;
import org.apache.streampark.console.core.service.FlinkEnvService;
import org.apache.streampark.console.core.service.FlinkSqlService;
import org.apache.streampark.flink.packer.pipeline.DockerResolvedSnapshot;
import org.apache.streampark.flink.packer.pipeline.PipelineType;

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

import java.util.Date;
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

  @Autowired private ApplicationService applicationService;

  @Autowired private FlinkSqlService flinkSqlService;

  @Autowired private ApplicationLogService applicationLogService;

  @Autowired private FlinkEnvService flinkEnvService;

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
  @PermissionAction(id = "#appId", type = PermissionType.APP)
  @PostMapping(value = "build")
  @RequiresPermissions("app:create")
  public RestResponse buildApplication(Long appId, boolean forceBuild) throws Exception {
    Application app = applicationService.getById(appId);

    ApiAlertException.throwIfNull(
        app.getVersionId(), "Please bind a Flink version to the current flink job.");
    // 1) check flink version
    FlinkEnv env = flinkEnvService.getById(app.getVersionId());
    boolean checkVersion = env.getFlinkVersion().checkVersion(false);
    if (!checkVersion) {
      throw new ApiAlertException("Unsupported flink version: " + env.getFlinkVersion().version());
    }

    // 2) check env
    boolean envOk = applicationService.checkEnv(app);
    if (!envOk) {
      throw new ApiAlertException(
          "Check flink env failed, please check the flink version of this job");
    }

    if (!forceBuild && !appBuildPipeService.allowToBuildNow(appId)) {
      throw new ApiAlertException(
          "The job is invalid, or the job cannot be built while it is running");
    }
    // check if you need to go through the build process (if the jar and pom have changed,
    // you need to go through the build process, if other common parameters are modified,
    // you don't need to go through the build process)

    ApplicationLog applicationLog = new ApplicationLog();
    applicationLog.setOptionName(
        org.apache.streampark.console.core.enums.Operation.RELEASE.getValue());
    applicationLog.setAppId(app.getId());
    applicationLog.setOptionTime(new Date());

    boolean needBuild = applicationService.checkBuildAndUpdate(app);
    if (!needBuild) {
      applicationLog.setSuccess(true);
      applicationLogService.save(applicationLog);
      return RestResponse.success(true);
    }

    // rollback
    if (app.isNeedRollback() && app.isFlinkSqlJob()) {
      flinkSqlService.rollback(app);
    }

    boolean actionResult = appBuildPipeService.buildApplication(app, applicationLog);
    return RestResponse.success(actionResult);
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
        && PipelineType.FLINK_NATIVE_K8S_APPLICATION == pipeline.get().getPipeType()) {
      DockerResolvedSnapshot dockerProgress =
          appBuildPipeService.getDockerProgressDetailSnapshot(appId);
      details.put("docker", AppBuildDockerResolvedDetail.of(dockerProgress));
    }
    return RestResponse.success(details);
  }
}
