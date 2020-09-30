/**
 * Copyright (c) 2019 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.streamxhub.console.core.controller;

import com.streamxhub.console.base.controller.BaseController;
import com.streamxhub.console.base.domain.RestRequest;
import com.streamxhub.console.base.domain.RestResponse;
import com.streamxhub.console.core.entity.Project;
import com.streamxhub.console.core.service.ProjectService;
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
public class ProjectController extends BaseController {

    @Autowired
    private ProjectService projectService;

    @PostMapping("create")
    @RequiresPermissions("project:create")
    public RestResponse create(Project project) {
        return projectService.create(project);
    }

    @PostMapping("build")
    @RequiresPermissions("project:build")
    public RestResponse build(Long id) throws Exception {
        return projectService.build(id);
    }

    @PostMapping("list")
    @RequiresPermissions("project:list")
    public RestResponse list(Project project, RestRequest restRequest) {
        IPage<Project> page = projectService.page(project, restRequest);
        return RestResponse.create().data(page);
    }

    @PostMapping("delete")
    @RequiresPermissions("project:delete")
    public RestResponse delete(String id) {
        boolean result = projectService.delete(id);
        return RestResponse.create().message(result ? "删除成功" : "删除失败");
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
