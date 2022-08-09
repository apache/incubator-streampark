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
import com.streamxhub.streamx.console.core.service.ProjectTeamService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Api(tags = "Project team related operations")
@Slf4j
@Validated
@RestController
@RequestMapping("flink/team")
public class ProjectTeamController {

    @Autowired
    private ProjectTeamService projectTeamService;

    @ApiOperation(value = "create")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "projectName", dataType = "String"),
        @ApiImplicitParam(name = "description", dataType = "String")
    })
    @PutMapping
    public RestResponse createProject(
                                      @RequestParam("projectName") String projectName,
                                      @RequestParam(value = "description", required = false) String description) {
        Map<String, Object> result = projectTeamService.createProject(projectName, description);
        return RestResponse.success(result);
    }

    @PutMapping(value = "/{code}")
    public RestResponse updateProject(
                                @PathVariable("code") Long code,
                                @RequestParam("projectName") String projectName,
                                @RequestParam(value = "description", required = false) String description,
                                @RequestParam(value = "userName") String userName) {
        Map<String, Object> result = projectTeamService.update(code, projectName, description, userName);
        return RestResponse.success(result);
    }

    @PutMapping
    public RestResponse queryProjectListPaging(
                                         @RequestParam(value = "searchVal", required = false) String searchVal,
                                         @RequestParam("pageSize") Integer pageSize,
                                         @RequestParam("pageNo") Integer pageNo
    ) {
        Map<String, Object> result = projectTeamService.queryProjectListPaging(pageSize, pageNo, searchVal);
        return RestResponse.success(result);
    }

    @PutMapping(value = "/delete")
    public RestResponse deleteProject(Long code) {
        Map<String, Object> result = projectTeamService.deleteProject(code);
        return RestResponse.success(result);
    }

    @ApiOperation(value = "queryProjectByCode")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "code", dataType = "Long")
    })
    @PutMapping(value = "/query")
    public RestResponse queryProjectByCode(Long code) {
        Map<String, Object> result = projectTeamService.queryByCode(code);
        return RestResponse.success(result);
    }

    @ApiOperation(value = "queryAllProjectList")
    @PutMapping(value = "/list")
    public RestResponse queryAllProjectList() {
        Map<String, Object> result = projectTeamService.queryAllProjectList();
        return RestResponse.success(result);
    }
}
