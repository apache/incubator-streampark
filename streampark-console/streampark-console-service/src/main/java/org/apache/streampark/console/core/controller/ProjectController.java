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
import java.util.HashMap;
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
    public RestResponse create(Project project) {
        ApiAlertException.throwIfNull(
            project.getTeamId(), "The teamId can't be null. Create team failed.");
        return projectService.create(project);
    }

    @AppChangeEvent
    @PostMapping("update")
    @RequiresPermissions("project:update")
    @Permission(team = "#project.teamId")
    public RestResponse update(Project project) {
        boolean update = projectService.update(project);
        return RestResponse.success().data(update);
    }

    @PostMapping("get")
    @Permission(team = "#project.teamId")
    public RestResponse get(Project project) {
        return RestResponse.success().data(projectService.getById(project.getId()));
    }

    @PostMapping("build")
    @RequiresPermissions("project:build")
    @Permission(team = "#project.teamId")
    public RestResponse build(Project project) throws Exception {
        projectService.build(project.getId());
        return RestResponse.success();
    }

    @PostMapping("build_log")
    @RequiresPermissions("project:build")
    @Permission(team = "#teamId")
    public RestResponse buildLog(
                                 Long id,
                                 @RequestParam(value = "startOffset", required = false) Long startOffset) {
        return projectService.getBuildLog(id, startOffset);
    }

    @PostMapping("list")
    @RequiresPermissions("project:view")
    @Permission(team = "#project.teamId")
    public RestResponse list(Project project, RestRequest restRequest) {
        if (project.getTeamId() == null) {
            return RestResponse.success(Collections.emptyList());
        }
        IPage<Project> page = projectService.getPage(project, restRequest);
        return RestResponse.success().data(page);
    }

    @PostMapping("branches")
    @Permission(team = "#project.teamId")
    public RestResponse branches(Project project) {
        List<String> branches = projectService.getAllBranches(project);
        List<String> tags = projectService.getAllTags(project);
        Map<String, List<String>> refs = new HashMap<>();
        refs.put("tags", tags);
        refs.put("branches", branches);
        return RestResponse.success().data(refs);
    }

    @PostMapping("delete")
    @RequiresPermissions("project:delete")
    @Permission(team = "#project.teamId")
    public RestResponse delete(Project project) {
        Boolean deleted = projectService.removeById(project.getId());
        return RestResponse.success().data(deleted);
    }

    @PostMapping("git_check")
    @Permission(team = "#project.teamId")
    public RestResponse gitCheck(Project project) {
        GitAuthorizedErrorEnum error = projectService.gitCheck(project);
        return RestResponse.success().data(error.getType());
    }

    @PostMapping("exists")
    @Permission(team = "#project.teamId")
    public RestResponse exists(Project project) {
        boolean exists = projectService.exists(project);
        return RestResponse.success().data(exists);
    }

    @PostMapping("modules")
    @Permission(team = "#project.teamId")
    public RestResponse modules(Project project) {
        List<String> result = projectService.listModules(project.getId());
        return RestResponse.success().data(result);
    }

    @PostMapping("jars")
    @Permission(team = "#project.teamId")
    public RestResponse jars(Project project) {
        List<String> result = projectService.listJars(project);
        return RestResponse.success().data(result);
    }

    @PostMapping("list_conf")
    @Permission(team = "#project.teamId")
    public RestResponse listConf(Project project) {
        List<Map<String, Object>> list = projectService.listConf(project);
        return RestResponse.success().data(list);
    }

    @PostMapping("select")
    @Permission(team = "#teamId")
    public RestResponse select(@RequestParam Long teamId) {
        List<Project> list = projectService.listByTeamId(teamId);
        return RestResponse.success().data(list);
    }
}
