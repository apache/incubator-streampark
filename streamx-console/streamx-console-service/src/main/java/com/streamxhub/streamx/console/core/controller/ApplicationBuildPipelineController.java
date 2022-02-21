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

import com.streamxhub.streamx.console.base.domain.RestResponse;
import com.streamxhub.streamx.console.core.entity.AppBuildDockerResolvedDetail;
import com.streamxhub.streamx.console.core.entity.AppBuildPipeline;
import com.streamxhub.streamx.console.core.entity.Application;
import com.streamxhub.streamx.console.core.service.AppBuildPipeService;
import com.streamxhub.streamx.console.core.service.ApplicationService;
import com.streamxhub.streamx.flink.packer.pipeline.DockerResolvedSnapshot;
import com.streamxhub.streamx.flink.packer.pipeline.PipelineType;
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

/**
 * @author Al-assad
 */
@Slf4j
@Validated
@RestController
@RequestMapping("flink/pipe")
@SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection")
public class ApplicationBuildPipelineController {

    @Autowired
    private AppBuildPipeService appBuildPipeService;

    @Autowired
    private ApplicationService applicationService;

    /**
     * Launch application building pipeline.
     *
     * @param appId      application id
     * @param forceBuild forced start pipeline or not
     * @return Whether the pipeline was successfully started
     */
    @PostMapping("/build")
    @RequiresPermissions("app:create")
    public RestResponse buildApplication(Long appId, boolean forceBuild) {
        try {
            if (!forceBuild && !appBuildPipeService.allowToBuildNow(appId)) {
                return RestResponse.create().data(false);
            }
            Application app = applicationService.getById(appId);
            boolean actionResult = appBuildPipeService.buildApplication(app);
            return RestResponse.create().data(actionResult);
        } catch (Exception e) {
            return RestResponse.create().data(false).message(e.getMessage());
        }
    }

    /**
     * Get application building pipeline progress detail.
     *
     * @param appId application id
     * @return "pipeline" -> pipeline details, "docker" -> docker resolved snapshot
     */
    @PostMapping("/detail")
    @RequiresPermissions("app:view")
    public RestResponse getBuildProgressDetail(Long appId) {
        Map<String, Object> details = new HashMap<>();
        Optional<AppBuildPipeline> pipeline = appBuildPipeService.getCurrentBuildPipeline(appId);
        details.put("pipeline", pipeline.map(AppBuildPipeline::toView).orElse(null));

        if (pipeline.isPresent() && PipelineType.FLINK_NATIVE_K8S_APPLICATION == pipeline.get().getPipeType()) {
            DockerResolvedSnapshot dockerProgress = appBuildPipeService.getDockerProgressDetailSnapshot(appId);
            details.put("docker", AppBuildDockerResolvedDetail.of(dockerProgress));
        }
        return RestResponse.create().data(details);
    }

}
