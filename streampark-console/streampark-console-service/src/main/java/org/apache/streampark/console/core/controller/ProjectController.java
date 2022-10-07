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

import org.apache.streampark.common.util.AssertUtils;
import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.base.domain.RestResponse;
import org.apache.streampark.console.core.entity.Project;
import org.apache.streampark.console.core.enums.GitAuthorizedError;
import org.apache.streampark.console.core.service.ProjectService;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;

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
        AssertUtils.checkArgument(project.getTeamId() != null, "The teamId cannot be null");
        return projectService.create(project);
    }

    @PostMapping("update")
    @RequiresPermissions("project:update")
    public RestResponse update(Project project) {
        boolean update = projectService.update(project);
        return RestResponse.success().data(update);
    }

    @PostMapping("get")
    public RestResponse get(Long id) {
        return RestResponse.success().data(projectService.getById(id));
    }

    @PostMapping("build")
    @RequiresPermissions("project:build")
    public RestResponse build(Long id) throws Exception {
        projectService.build(id);
        return RestResponse.success();
    }

    @PostMapping("buildlog")
    @RequiresPermissions("project:build")
    public RestResponse buildLog(
        Long id,
        @RequestParam(value = "startOffset", required = false) Long startOffset) {
        return projectService.getBuildLog(id, startOffset);
    }

    @PostMapping("list")
    @RequiresPermissions("project:view")
    public RestResponse list(Project project, RestRequest restRequest) {
        if (project.getTeamId() == null) {
            return RestResponse.success(Collections.emptyList());
        }
        IPage<Project> page = projectService.page(project, restRequest);
        return RestResponse.success().data(page);
    }

    @PostMapping("branches")
    public RestResponse branches(Project project) {
        List<String> branches = project.getAllBranches();
        return RestResponse.success().data(branches);
    }

    @PostMapping("delete")
    @RequiresPermissions("project:delete")
    public RestResponse delete(Long id) {
        Boolean deleted = projectService.delete(id);
        return RestResponse.success().data(deleted);
    }

    @PostMapping("gitcheck")
    public RestResponse gitCheck(Project project) {
        GitAuthorizedError error = project.gitCheck();
        return RestResponse.success().data(error.getType());
    }

    @PostMapping("exists")
    public RestResponse exists(Project project) {
        boolean exists = projectService.checkExists(project);
        return RestResponse.success().data(exists);
    }

    @PostMapping("modules")
    public RestResponse modules(Long id) {
        List<String> result = projectService.modules(id);
        return RestResponse.success().data(result);
    }

    @PostMapping("jars")
    public RestResponse jars(Project project) {
        List<String> result = projectService.jars(project);
        return RestResponse.success().data(result);
    }

    @PostMapping("listconf")
    public RestResponse listConf(Project project) {
        List<Map<String, Object>> list = projectService.listConf(project);
        return RestResponse.success().data(list);
    }

    @PostMapping("select")
    public RestResponse select() {
        return RestResponse.success().data(projectService.list());
    }
}
