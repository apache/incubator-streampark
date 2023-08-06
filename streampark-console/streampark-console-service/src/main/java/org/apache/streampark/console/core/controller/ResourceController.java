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
import org.apache.streampark.console.core.entity.Resource;
import org.apache.streampark.console.core.service.ResourceService;

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
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;

import java.util.List;

@Tag(name = "RESOURCE_TAG")
@Slf4j
@Validated
@RestController
@RequestMapping("resource")
public class ResourceController {

  @Autowired private ResourceService resourceService;

  @Operation(summary = "add resource")
  @PostMapping("add")
  @RequiresPermissions("resource:add")
  public RestResponse addResource(@Valid Resource resource) throws Exception {
    this.resourceService.addResource(resource);
    return RestResponse.success();
  }

  @Operation(summary = "check resource")
  @PostMapping("check")
  public RestResponse checkResource(@Valid Resource resource) throws Exception {
    return this.resourceService.checkResource(resource);
  }

  @Operation(summary = "List resources")
  @PostMapping("page")
  public RestResponse page(RestRequest restRequest, Resource resource) {
    IPage<Resource> page = resourceService.page(resource, restRequest);
    return RestResponse.success(page);
  }

  @Operation(summary = "Update resource")
  @PutMapping("update")
  @RequiresPermissions("resource:update")
  public RestResponse updateResource(@Valid Resource resource) {
    resourceService.updateResource(resource);
    return RestResponse.success();
  }

  @Operation(summary = "Delete resource")
  @DeleteMapping("delete")
  @RequiresPermissions("resource:delete")
  public RestResponse deleteResource(@Valid Resource resource) {
    this.resourceService.deleteResource(resource);
    return RestResponse.success();
  }

  @Operation(summary = "List resource")
  @PostMapping("list")
  public RestResponse listResource(@RequestParam Long teamId) {
    List<Resource> resourceList = resourceService.findByTeamId(teamId);
    return RestResponse.success(resourceList);
  }

  @Operation(summary = "Upload the resource jar")
  @PostMapping("upload")
  @RequiresPermissions("resource:add")
  public RestResponse upload(MultipartFile file) throws Exception {
    String uploadPath = resourceService.upload(file);
    return RestResponse.success(uploadPath);
  }
}
