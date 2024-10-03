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

import org.apache.streampark.console.base.domain.RestResponse;
import org.apache.streampark.console.core.annotation.Permission;
import org.apache.streampark.console.core.entity.ApplicationBuildPipeline;
import org.apache.streampark.console.core.service.application.SparkAplicationBuildPipelineService;

import org.apache.shiro.authz.annotation.RequiresPermissions;

import lombok.extern.slf4j.Slf4j;
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
@RequestMapping("spark/pipe")
public class SparkPipelineController {

    @Autowired
    private SparkAplicationBuildPipelineService appBuildPipeService;

    /**
     * Release application building pipeline.
     *
     * @param appId application id
     * @param forceBuild forced start pipeline or not
     * @return Whether the pipeline was successfully started
     */
    @PostMapping("build")
    @RequiresPermissions("app:create")
    @Permission(app = "#appId")
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
    @PostMapping("/detail")
    @RequiresPermissions("app:view")
    @Permission(app = "#appId")
    public RestResponse getBuildProgressDetail(Long appId) {
        Map<String, Object> details = new HashMap<>(0);
        Optional<ApplicationBuildPipeline> pipeline = appBuildPipeService.getCurrentBuildPipeline(appId);
        details.put("pipeline", pipeline.map(ApplicationBuildPipeline::toView).orElse(null));
        return RestResponse.success(details);
    }
}
