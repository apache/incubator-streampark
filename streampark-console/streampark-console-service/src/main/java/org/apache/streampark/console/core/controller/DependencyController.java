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
import org.apache.streampark.console.core.entity.Dependency;
import org.apache.streampark.console.core.service.DependencyService;

import org.apache.shiro.authz.annotation.RequiresPermissions;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

import java.util.List;

@Tag(name = "DEPENDENCY_TAG")
@Slf4j
@Validated
@RestController
@RequestMapping("dependency")
public class DependencyController {

  @Autowired private DependencyService dependencyService;

  @Operation(summary = "add dependency")
  @PostMapping("add")
  @RequiresPermissions("dependency:add")
  public RestResponse addDependency(@Valid Dependency dependency) {
    this.dependencyService.addDependency(dependency);
    return RestResponse.success();
  }

  @Operation(summary = "List dependencies")
  @PostMapping("page")
  public RestResponse page(RestRequest restRequest, Dependency dependency) {
    IPage<Dependency> page = dependencyService.page(dependency, restRequest);
    return RestResponse.success(page);
  }

  @Operation(summary = "Update dependency")
  @PutMapping("update")
  @RequiresPermissions("dependency:update")
  public RestResponse updateDependency(@Valid Dependency dependency) {
    dependencyService.updateDependency(dependency);
    return RestResponse.success();
  }

  @Operation(summary = "Delete dependency")
  @DeleteMapping("delete")
  @RequiresPermissions("dependency:delete")
  public RestResponse deleteDependency(@Valid Dependency dependency) {
    this.dependencyService.deleteDependency(dependency);
    return RestResponse.success();
  }

  @Operation(summary = "List dependency")
  @PostMapping("list")
  public RestResponse listDependency(@RequestParam Long teamId) {
    List<Dependency> dependencyList = dependencyService.findByTeamId(teamId);
    return RestResponse.success(dependencyList);
  }
}
