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
import org.apache.streampark.console.core.bean.AppBuildDockerResolvedDetail;
import org.apache.streampark.console.core.entity.AppBuildPipeline;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.service.AppBuildPipeService;
import org.apache.streampark.console.core.service.ApplicationService;
import org.apache.streampark.console.core.service.FlinkSqlService;
import org.apache.streampark.flink.packer.pipeline.DockerResolvedSnapshot;
import org.apache.streampark.flink.packer.pipeline.PipelineType;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Validated
@RestController
@RequestMapping("flink/pipe")
public class ApplicationBuildPipelineController {

    @Autowired
    private AppBuildPipeService appBuildPipeService;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private FlinkSqlService flinkSqlService;

    /**
     * Launch application building pipeline.
     *
     * @param appId      application id
     * @param forceBuild forced start pipeline or not
     * @return Whether the pipeline was successfully started
     */
    @ApiAccess
    @ApiOperation(value = "Launch application", notes = "Launch application", tags = ApiDocConstant.FLINK_APP_OP_TAG, consumes = "x-www-form-urlencoded")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "appId", value = "APP_ID", required = true, paramType = "form", dataType = "Long"),
        @ApiImplicitParam(name = "forceBuild", value = "FORCE_BUILD", required = true, paramType = "form", dataType = "Boolean", defaultValue = "false"),
    })
    @PostMapping("/build")
    @RequiresPermissions("app:create")
    public RestResponse buildApplication(Long appId, boolean forceBuild) {
        try {
            if (!forceBuild && !appBuildPipeService.allowToBuildNow(appId)) {
                return RestResponse.success(false);
            }
            Application app = applicationService.getById(appId);
            // 检查是否需要走build这一步流程(jar和pom发生变化了则需要走build流程, 其他普通参数修改了,不需要走build流程)
            boolean needBuild = applicationService.checkBuildAndUpdate(app);
            if (!needBuild) {
                return RestResponse.success(true);
            }

            //回滚任务.
            if (app.isNeedRollback() && app.isFlinkSqlJob()) {
                flinkSqlService.rollback(app);
            }

            boolean actionResult = appBuildPipeService.buildApplication(app);
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
    @ApiAccess
    @PostMapping("/detail")
    @RequiresPermissions("app:view")
    public RestResponse getBuildProgressDetail(Long appId) {
        Map<String, Object> details = new HashMap<>(0);
        Optional<AppBuildPipeline> pipeline = appBuildPipeService.getCurrentBuildPipeline(appId);
        details.put("pipeline", pipeline.map(AppBuildPipeline::toView).orElse(null));

        if (pipeline.isPresent() && PipelineType.FLINK_NATIVE_K8S_APPLICATION == pipeline.get().getPipeType()) {
            DockerResolvedSnapshot dockerProgress = appBuildPipeService.getDockerProgressDetailSnapshot(appId);
            details.put("docker", AppBuildDockerResolvedDetail.of(dockerProgress));
        }
        return RestResponse.success(details);
    }

}
