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
import org.apache.streampark.console.base.domain.Result;
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
    @Permission(team = "#project.teamId")
    @RequiresPermissions("project:create")
    public Result<Boolean> create(Project project) {
        ApiAlertException.throwIfNull(
            project.getTeamId(), "The teamId can't be null. Create team failed.");
        boolean success = projectService.create(project);
        return Result.success(success);
    }

    @AppUpdated
    @PostMapping("update")
    @RequiresPermissions("project:update")
    @Permission(team = "#project.teamId")
    public Result<Boolean> update(Project project) {
        boolean update = projectService.update(project);
        return Result.success(update);
    }

    @PostMapping("get")
    @Permission(team = "#project.teamId")
    public Result<Project> get(Project project) {
        return Result.success(projectService.getById(project.getId()));
    }

    @PostMapping("build")
    @RequiresPermissions("project:build")
    @Permission(team = "#project.teamId")
    public Result<Void> build(Project project) throws Exception {
        projectService.build(project.getId());
        return Result.success();
    }

    @PostMapping("build_log")
    @RequiresPermissions("project:build")
    @Permission(team = "#teamId")
    public Result<?> buildLog(Long id, Long startOffset) {
        return projectService.getBuildLog(id, startOffset);
    }

    @PostMapping("list")
    @RequiresPermissions("project:view")
    @Permission(team = "#project.teamId")
    public Result<?> list(Project project, RestRequest restRequest) {
        if (project.getTeamId() == null) {
            return Result.success(Collections.emptyList());
        }
        IPage<Project> page = projectService.getPage(project, restRequest);
        return Result.success(page);
    }

    @PostMapping("branches")
    @Permission(team = "#project.teamId")
    public Result<List<String>> branches(Project project) {
        List<String> branches = project.getAllBranches();
        return Result.success(branches);
    }

    @PostMapping("delete")
    @RequiresPermissions("project:delete")
    @Permission(team = "#project.teamId")
    public Result<Boolean> delete(Project project) {
        Boolean deleted = projectService.removeById(project.getId());
        return Result.success(deleted);
    }

    @PostMapping("git_check")
    @Permission(team = "#project.teamId")
    public Result<Integer> gitCheck(Project project) {
        GitAuthorizedErrorEnum error = project.gitCheck();
        return Result.success(error.getType());
    }

    @PostMapping("exists")
    @Permission(team = "#project.teamId")
    public Result<Boolean> exists(Project project) {
        boolean exists = projectService.exists(project);
        return Result.success(exists);
    }

    @PostMapping("modules")
    @Permission(team = "#project.teamId")
    public Result<List<String>> modules(Project project) {
        List<String> result = projectService.listModules(project.getId());
        return Result.success(result);
    }

    @PostMapping("jars")
    @Permission(team = "#project.teamId")
    public Result<List<String>> jars(Project project) {
        List<String> result = projectService.listJars(project);
        return Result.success(result);
    }

    @PostMapping("list_conf")
    @Permission(team = "#project.teamId")
    public Result<List<Map<String, Object>>> listConf(Project project) {
        List<Map<String, Object>> list = projectService.listConf(project);
        return Result.success(list);
    }

    @PostMapping("select")
    @Permission(team = "#teamId")
    public Result<List<Project>> select(@RequestParam Long teamId) {
        List<Project> list = projectService.listByTeamId(teamId);
        return Result.success(list);
    }
}
