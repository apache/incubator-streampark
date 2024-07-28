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
import org.apache.streampark.console.core.entity.Resource;
import org.apache.streampark.console.core.service.ResourceService;

import org.apache.shiro.authz.annotation.RequiresPermissions;

import com.baomidou.mybatisplus.core.metadata.IPage;
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

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Slf4j
@Validated
@RestController
@RequestMapping("resource")
public class ResourceController {

    @Autowired
    private ResourceService resourceService;

    @PostMapping("add")
    @RequiresPermissions("resource:add")
    public Result<Void> addResource(@Valid Resource resource) throws Exception {
        this.resourceService.addResource(resource);
        return Result.success();
    }

    @PostMapping("check")
    public Result<Map<String, Serializable>> checkResource(@Valid Resource resource) throws Exception {
        return this.resourceService.checkResource(resource);
    }

    @PostMapping("page")
    public Result<IPage<Resource>> page(RestRequest restRequest, Resource resource) {
        IPage<Resource> page = resourceService.getPage(resource, restRequest);
        return Result.success(page);
    }

    @PutMapping("update")
    @RequiresPermissions("resource:update")
    public Result<Void> updateResource(@Valid Resource resource) {
        resourceService.updateResource(resource);
        return Result.success();
    }

    @DeleteMapping("delete")
    @RequiresPermissions("resource:delete")
    public Result<Void> deleteResource(@Valid Resource resource) {
        this.resourceService.remove(resource.getId());
        return Result.success();
    }

    @PostMapping("list")
    public Result<List<Resource>> listResource(@RequestParam Long teamId) {
        List<Resource> resourceList = resourceService.listByTeamId(teamId);
        return Result.success(resourceList);
    }

    @PostMapping("upload")
    @RequiresPermissions("resource:add")
    public Result<String> upload(MultipartFile file) throws Exception {
        String uploadPath = resourceService.upload(file);
        return Result.success(uploadPath);
    }
}
