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

import com.streamxhub.streamx.console.base.domain.RestRequest;
import com.streamxhub.streamx.console.base.domain.RestResponse;
import com.streamxhub.streamx.console.core.entity.Project;
import com.streamxhub.streamx.console.core.enums.GitAuthorizedError;
import com.streamxhub.streamx.console.core.service.ProjectService;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * @author benjobs
 */
@Slf4j
@Validated
@RestController
@RequestMapping("flink/project")
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    @PostMapping("create")
    @RequiresPermissions("project:create")
    public RestResponse create(Project project) {
        return projectService.create(project);
    }

    @PostMapping("update")
    @RequiresPermissions("project:update")
    public RestResponse update(Project project) {
        boolean update = projectService.update(project);
        return RestResponse.create().data(update);
    }

    @PostMapping("get")
    public RestResponse get(Long id) {
        return RestResponse.create().data(projectService.getById(id));
    }

    @PostMapping("build")
    @RequiresPermissions("project:build")
    public RestResponse build(Long id, String socketId) throws Exception {
        projectService.build(id, socketId);
        return RestResponse.create();
    }

    @PostMapping("buildlog")
    @RequiresPermissions("project:build")
    public RestResponse buildLog(Long id) throws Exception {
        projectService.tailBuildLog(id);
        return RestResponse.create();
    }

    @PostMapping("closebuild")
    @RequiresPermissions("project:build")
    public RestResponse closeBuild(Long id) {
        projectService.closeBuildLog(id);
        return RestResponse.create();
    }

    @PostMapping("list")
    @RequiresPermissions("project:view")
    public RestResponse list(Project project, RestRequest restRequest) {
        IPage<Project> page = projectService.page(project, restRequest);
        return RestResponse.create().data(page);
    }

    @PostMapping("branches")
    public RestResponse branches(Project project) {
        List<String> branches = project.getAllBranches();
        return RestResponse.create().data(branches);
    }

    @PostMapping("delete")
    @RequiresPermissions("project:delete")
    public RestResponse delete(Long id) {
        Boolean deleted = projectService.delete(id);
        return RestResponse.create().data(deleted);
    }

    @PostMapping("gitcheck")
    public RestResponse gitCheck(Project project) {
        GitAuthorizedError error = project.gitCheck();
        return RestResponse.create().data(error.getType());
    }

    @PostMapping("exists")
    public RestResponse exists(Project project) {
        boolean exists = projectService.checkExists(project);
        return RestResponse.create().data(exists);
    }

    @PostMapping("modules")
    public RestResponse modules(Long id) {
        List<String> result = projectService.modules(id);
        return RestResponse.create().data(result);
    }

    @PostMapping("jars")
    public RestResponse jars(Project project) {
        List<String> result = projectService.jars(project);
        return RestResponse.create().data(result);
    }

    @PostMapping("listconf")
    public RestResponse listConf(Project project) {
        List<Map<String, Object>> list = projectService.listConf(project);
        return RestResponse.create().data(list);
    }

    @PostMapping("select")
    public RestResponse select() {
        return RestResponse.create().data(projectService.list());
    }
}
