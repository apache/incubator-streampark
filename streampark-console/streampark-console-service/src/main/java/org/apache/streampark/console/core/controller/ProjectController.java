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
import org.apache.streampark.console.core.annotation.AppUpdated;
import org.apache.streampark.console.core.entity.Project;
import org.apache.streampark.console.core.enums.GitAuthorizedErrorEnum;
import org.apache.streampark.console.core.service.ProjectService;

import org.apache.shiro.authz.annotation.RequiresPermissions;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "PROJECT_TAG")
@Slf4j
@Validated
@RestController
@RequestMapping("project")
public class ProjectController {

  @Autowired private ProjectService projectService;

  @Operation(summary = "Create project")
  @PostMapping("create")
  @RequiresPermissions("project:create")
  public RestResponse create(Project project) {
    ApiAlertException.throwIfNull(
        project.getTeamId(), "The teamId can't be null. Create team failed.");
    return projectService.create(project);
  }

  @Operation(summary = "Update project")
  @AppUpdated
  @PostMapping("update")
  @RequiresPermissions("project:update")
  public RestResponse update(Project project) {
    boolean update = projectService.update(project);
    return RestResponse.success().data(update);
  }

  @Operation(summary = "Get project")
  @PostMapping("get")
  public RestResponse get(Long id) {
    return RestResponse.success().data(projectService.getById(id));
  }

  @Operation(summary = "Build project")
  @PostMapping("build")
  @RequiresPermissions("project:build")
  public RestResponse build(Long id) throws Exception {
    projectService.build(id);
    return RestResponse.success();
  }

  @Operation(summary = "Get project build logs")
  @PostMapping("buildlog")
  @RequiresPermissions("project:build")
  public RestResponse buildLog(
      Long id, @RequestParam(value = "startOffset", required = false) Long startOffset) {
    return projectService.getBuildLog(id, startOffset);
  }

  @Operation(summary = "List projects")
  @PostMapping("list")
  @RequiresPermissions("project:view")
  public RestResponse list(Project project, RestRequest restRequest) {
    if (project.getTeamId() == null) {
      return RestResponse.success(Collections.emptyList());
    }
    IPage<Project> page = projectService.getPage(project, restRequest);
    return RestResponse.success().data(page);
  }

  @Operation(summary = "List git project branches")
  @PostMapping("branches")
  public RestResponse branches(Project project) {
    List<String> branches = project.getAllBranches();
    return RestResponse.success().data(branches);
  }

  @Operation(summary = "Delete project")
  @PostMapping("delete")
  @RequiresPermissions("project:delete")
  public RestResponse delete(Long id) {
    Boolean deleted = projectService.removeById(id);
    return RestResponse.success().data(deleted);
  }

  @Operation(summary = "Authenticate git project")
  @PostMapping("gitcheck")
  public RestResponse gitCheck(Project project) {
    GitAuthorizedErrorEnum error = project.gitCheck();
    return RestResponse.success().data(error.getType());
  }

  @Operation(summary = "Check the project")
  @PostMapping("exists")
  public RestResponse exists(Project project) {
    boolean exists = projectService.exists(project);
    return RestResponse.success().data(exists);
  }

  @Operation(summary = "List project modules")
  @PostMapping("modules")
  public RestResponse modules(Long id) {
    List<String> result = projectService.listModules(id);
    return RestResponse.success().data(result);
  }

  @Operation(summary = "List project jars")
  @PostMapping("jars")
  public RestResponse jars(Project project) {
    List<String> result = projectService.listJars(project);
    return RestResponse.success().data(result);
  }

  @Operation(summary = "List project configurations")
  @PostMapping("listconf")
  public RestResponse listConf(Project project) {
    List<Map<String, Object>> confList = projectService.listConf(project);
    return RestResponse.success().data(confList);
  }

  @Operation(summary = "List the team projects")
  @PostMapping("select")
  public RestResponse select(@RequestParam Long teamId) {
    List<Project> projectList = projectService.listByTeamId(teamId);
    return RestResponse.success().data(projectList);
  }
}
