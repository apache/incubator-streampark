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

import org.apache.streampark.console.base.bean.PageRequest;
import org.apache.streampark.console.base.bean.Response;
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.core.annotation.AppUpdated;
import org.apache.streampark.console.core.annotation.Permission;
import org.apache.streampark.console.core.entity.Project;
import org.apache.streampark.console.core.enums.GitAuthorizedErrorEnum;
import org.apache.streampark.console.core.service.ProjectService;

import org.apache.shiro.authz.annotation.RequiresPermissions;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    @Permission(team = "#project.teamId")
    @RequiresPermissions("project:create")
    public Response<Boolean> create(Project project) {
        ApiAlertException.throwIfNull(
            project.getTeamId(), "The teamId can't be null. Create team failed.");
        boolean success = projectService.create(project);
        return Response.success(success);
    }

    @AppUpdated
    @PostMapping("update")
    @RequiresPermissions("project:update")
    @Permission(team = "#project.teamId")
    public Response<Boolean> update(Project project) {
        boolean update = projectService.update(project);
        return Response.success(update);
    }

    @PostMapping("get")
    @Permission(team = "#project.teamId")
    public Response<Project> get(Project project) {
        return Response.success(projectService.getById(project.getId()));
    }

    @PostMapping("build")
    @RequiresPermissions("project:build")
    @Permission(team = "#project.teamId")
    public Response<Void> build(Project project) throws Exception {
        projectService.build(project.getId());
        return Response.success();
    }

    @PostMapping("build_log")
    @RequiresPermissions("project:build")
    @Permission(team = "#teamId")
    public Response<Map<String, String>> buildLog(Long id, Long startOffset) {
        return projectService.getBuildLog(id, startOffset);
    }

    @PostMapping("list")
    @RequiresPermissions("project:view")
    @Permission(team = "#project.teamId")
    public Response<IPage<Project>> list(Project project, PageRequest pageRequest) {
        if (project.getTeamId() == null) {
            return Response.fail("teamId must be not null", null);
        }
        IPage<Project> page = projectService.getPage(project, pageRequest);
        return Response.success(page);
    }

    @PostMapping("branches")
    @Permission(team = "#project.teamId")
    public Response<List<String>> branches(Project project) {
        List<String> branches = project.getAllBranches();
        return Response.success(branches);
    }

    @PostMapping("delete")
    @RequiresPermissions("project:delete")
    @Permission(team = "#project.teamId")
    public Response<Boolean> delete(Project project) {
        Boolean deleted = projectService.removeById(project.getId());
        return Response.success(deleted);
    }

    @PostMapping("git_check")
    @Permission(team = "#project.teamId")
    public Response<Integer> gitCheck(Project project) {
        GitAuthorizedErrorEnum error = project.gitCheck();
        return Response.success(error.getType());
    }

    @PostMapping("exists")
    @Permission(team = "#project.teamId")
    public Response<Boolean> exists(Project project) {
        boolean exists = projectService.exists(project);
        return Response.success(exists);
    }

    @PostMapping("modules")
    @Permission(team = "#project.teamId")
    public Response<List<String>> modules(Project project) {
        List<String> result = projectService.listModules(project.getId());
        return Response.success(result);
    }

    @PostMapping("jars")
    @Permission(team = "#project.teamId")
    public Response<List<String>> jars(Project project) {
        List<String> result = projectService.listJars(project);
        return Response.success(result);
    }

    @PostMapping("list_conf")
    @Permission(team = "#project.teamId")
    public Response<?> listConf(Project project) {
        List<Map<String, Object>> list = projectService.listConf(project);
        return Response.success(list);
    }

    @PostMapping("select")
    @Permission(team = "#teamId")
    public Response<List<Project>> select(@RequestParam Long teamId) {
        List<Project> list = projectService.listByTeamId(teamId);
        return Response.success(list);
    }
}
