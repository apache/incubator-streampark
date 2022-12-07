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
import org.apache.streampark.console.core.bean.AppBuildDockerResolvedDetail;
import org.apache.streampark.console.core.entity.AppBuildPipeline;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.service.AppBuildPipeService;
import org.apache.streampark.console.core.service.ApplicationService;
import org.apache.streampark.console.core.service.FlinkSqlService;
import org.apache.streampark.flink.packer.pipeline.DockerResolvedSnapshot;
import org.apache.streampark.flink.packer.pipeline.PipelineType;

import io.swagger.annotations.Api;
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

@Api(tags = {"FLINK_APPLICATION_BUILD_PIPELINE_TAG"})
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
    @ApiOperation(value = "Launch application", notes = "Launch application", tags = ApiDocConstant.FLINK_APP_OP_TAG, consumes = "application/x-www-form-urlencoded")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "appId", value = "APP_ID", required = true, paramType = "query", dataTypeClass = Long.class),
        @ApiImplicitParam(name = "forceBuild", value = "FORCE_BUILD", required = true, paramType = "query", dataTypeClass = Boolean.class, defaultValue = "false"),
    })
    @PostMapping(value = "build", consumes = "application/x-www-form-urlencoded")
    @RequiresPermissions("app:create")
    public RestResponse buildApplication(Long appId, boolean forceBuild) {
        try {
            Application app = applicationService.getById(appId);
            Boolean envOk = applicationService.checkEnv(app);
            if (!envOk) {
                throw new ApiAlertException("Check flink env failed, please check the flink version of this job");
            }

            if (!forceBuild && !appBuildPipeService.allowToBuildNow(appId)) {
                throw new ApiAlertException("The job is invalid, or the job cannot be built while it is running");
            }
            // check if you need to go through the build process (if the jar and pom have changed,
            // you need to go through the build process, if other common parameters are modified,
            // you don't need to go through the build process)
            boolean needBuild = applicationService.checkBuildAndUpdate(app);
            if (!needBuild) {
                return RestResponse.success(true);
            }

            // rollback
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
