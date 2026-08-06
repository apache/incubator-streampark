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

import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.base.domain.RestResponse;
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.core.annotation.AppChangeEvent;
import org.apache.streampark.console.core.annotation.Permission;
import org.apache.streampark.console.core.assembler.ProjectAssembler;
import org.apache.streampark.console.core.entity.Project;
import org.apache.streampark.console.core.enums.GitAuthorizedErrorEnum;
import org.apache.streampark.console.core.request.common.TeamIdRequest;
import org.apache.streampark.console.core.request.common.TeamScopedIdRequest;
import org.apache.streampark.console.core.request.project.ProjectBuildLogRequest;
import org.apache.streampark.console.core.request.project.ProjectCreateRequest;
import org.apache.streampark.console.core.request.project.ProjectExistsRequest;
import org.apache.streampark.console.core.request.project.ProjectGitRequest;
import org.apache.streampark.console.core.request.project.ProjectListQueryRequest;
import org.apache.streampark.console.core.request.project.ProjectModuleRequest;
import org.apache.streampark.console.core.request.project.ProjectUpdateRequest;
import org.apache.streampark.console.core.service.ProjectService;

import org.apache.shiro.authz.annotation.RequiresPermissions;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Validated
@RestController
@RequestMapping("project")
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    @PostMapping("create")
    @Permission(team = "#request.teamId")
    @RequiresPermissions("project:create")
    public RestResponse create(ProjectCreateRequest request) {
        ApiAlertException.throwIfNull(
            request.getTeamId(), "The teamId can't be null. Create team failed.");
        return projectService.create(ProjectAssembler.toEntity(request));
    }

    @AppChangeEvent
    @PostMapping("update")
    @RequiresPermissions("project:update")
    @Permission(team = "#request.teamId")
    public RestResponse update(ProjectUpdateRequest request) {
        boolean update = projectService.update(ProjectAssembler.toEntity(request));
        return RestResponse.success().data(update);
    }

    @PostMapping("get")
    @Permission(team = "#request.teamId")
    public RestResponse get(TeamScopedIdRequest request) {
        return RestResponse.success()
            .data(ProjectAssembler.toResponse(projectService.getById(request.getId())));
    }

    @PostMapping("build")
    @RequiresPermissions("project:build")
    @Permission(team = "#request.teamId")
    public RestResponse build(TeamScopedIdRequest request) throws Exception {
        projectService.build(request.getId());
        return RestResponse.success();
    }

    @PostMapping("build_log")
    @RequiresPermissions("project:build")
    @Permission(team = "#request.teamId")
    public RestResponse buildLog(ProjectBuildLogRequest request) {
        return projectService.getBuildLog(request.getId(), request.getStartOffset());
    }

    @PostMapping("list")
    @RequiresPermissions("project:view")
    @Permission(team = "#query.teamId")
    public RestResponse list(ProjectListQueryRequest query, RestRequest restRequest) {
        if (query.getTeamId() == null) {
            return RestResponse.success(Collections.emptyList());
        }
        IPage<Project> page = projectService.getPage(ProjectAssembler.toEntity(query), restRequest);
        return RestResponse.success().data(ProjectAssembler.toPageResponse(page));
    }

    @PostMapping("branches")
    @Permission(team = "#request.teamId")
    public RestResponse branches(ProjectGitRequest request) {
        Project project = ProjectAssembler.toEntity(request);
        List<String> branches = projectService.getAllBranches(project);
        List<String> tags = projectService.getAllTags(project);
        return RestResponse.success().data(ProjectAssembler.toBranchesResponse(branches, tags));
    }

    @PostMapping("delete")
    @RequiresPermissions("project:delete")
    @Permission(team = "#request.teamId")
    public RestResponse delete(TeamScopedIdRequest request) {
        Boolean deleted = projectService.removeById(request.getId());
        return RestResponse.success().data(deleted);
    }

    @PostMapping("git_check")
    @Permission(team = "#request.teamId")
    public RestResponse gitCheck(ProjectGitRequest request) {
        GitAuthorizedErrorEnum error = projectService.gitCheck(ProjectAssembler.toEntity(request));
        return RestResponse.success().data(error.getType());
    }

    @PostMapping("exists")
    @Permission(team = "#request.teamId")
    public RestResponse exists(ProjectExistsRequest request) {
        boolean exists = projectService.exists(ProjectAssembler.toEntity(request));
        return RestResponse.success().data(exists);
    }

    @PostMapping("modules")
    @Permission(team = "#request.teamId")
    public RestResponse modules(TeamScopedIdRequest request) {
        List<String> result = projectService.listModules(request.getId());
        return RestResponse.success().data(result);
    }

    @PostMapping("jars")
    @Permission(team = "#request.teamId")
    public RestResponse jars(ProjectModuleRequest request) {
        List<String> result = projectService.listJars(ProjectAssembler.toEntity(request));
        return RestResponse.success().data(result);
    }

    @PostMapping("list_conf")
    @Permission(team = "#request.teamId")
    public RestResponse listConf(ProjectModuleRequest request) {
        List<Map<String, Object>> list =
            projectService.listConf(ProjectAssembler.toEntity(request));
        return RestResponse.success().data(list);
    }

    @PostMapping("select")
    @Permission(team = "#request.teamId")
    public RestResponse select(TeamIdRequest request) {
        List<Project> list = projectService.listByTeamId(request.getTeamId());
        return RestResponse.success().data(ProjectAssembler.toListResponse(list));
    }
}
