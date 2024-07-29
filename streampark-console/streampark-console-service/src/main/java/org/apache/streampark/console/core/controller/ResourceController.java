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

import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("resource")
public class ResourceController {

    @Autowired
    private ResourceService resourceService;

    @PostMapping("add")
    @RequiresPermissions("resource:add")
    public Response<Void> addResource(@Valid Resource resource) throws Exception {
        this.resourceService.addResource(resource);
        return Response.success();
    }

    @PostMapping("check")
    public Response<?> checkResource(@Valid Resource resource) throws Exception {
        return this.resourceService.checkResource(resource);
    }

    @PostMapping("page")
    public Response<IPage<Resource>> page(PageRequest pageRequest, Resource resource) {
        IPage<Resource> page = resourceService.getPage(resource, pageRequest);
        return Response.success(page);
    }

    @PutMapping("update")
    @RequiresPermissions("resource:update")
    public Response<Void> updateResource(@Valid Resource resource) {
        resourceService.updateResource(resource);
        return Response.success();
    }

    @DeleteMapping("delete")
    @RequiresPermissions("resource:delete")
    public Response<Void> deleteResource(@Valid Resource resource) {
        this.resourceService.remove(resource.getId());
        return Response.success();
    }

    @PostMapping("list")
    public Response<List<Resource>> listResource(@RequestParam Long teamId) {
        List<Resource> resourceList = resourceService.listByTeamId(teamId);
        return Response.success(resourceList);
    }

    @PostMapping("upload")
    @RequiresPermissions("resource:add")
    public Response<String> upload(MultipartFile file) throws Exception {
        String uploadPath = resourceService.upload(file);
        return Response.success(uploadPath);
    }
}
