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
import org.apache.streampark.console.base.domain.RestResponseBody;
import org.apache.streampark.console.base.web.FormOrJson;
import org.apache.streampark.console.core.assembler.ResourceAssembler;
import org.apache.streampark.console.core.entity.Resource;
import org.apache.streampark.console.core.request.common.TeamIdRequest;
import org.apache.streampark.console.core.request.common.TeamScopedIdRequest;
import org.apache.streampark.console.core.request.resource.ResourceCreateRequest;
import org.apache.streampark.console.core.request.resource.ResourcePageQueryRequest;
import org.apache.streampark.console.core.request.resource.ResourceUpdateRequest;
import org.apache.streampark.console.core.response.resource.ResourceCheckResponse;
import org.apache.streampark.console.core.response.resource.ResourceResponse;
import org.apache.streampark.console.core.response.resource.ResourceUploadResponse;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;

import java.io.IOException;
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
    public RestResponseBody<Void> addResource(@Valid @FormOrJson ResourceCreateRequest request) throws Exception {
        this.resourceService.addResource(ResourceAssembler.toEntity(request));
        return RestResponseBody.success();
    }

    @PostMapping("check")
    public RestResponseBody<ResourceCheckResponse> checkResource(@Valid ResourceCreateRequest request) throws Exception {
        return RestResponseBody.success(
            ResourceAssembler.toCheckResponse(resourceService.checkResource(ResourceAssembler.toEntity(request))));
    }

    @PostMapping("page")
    public RestResponseBody<IPage<ResourceResponse>> page(RestRequest restRequest, ResourcePageQueryRequest query) {
        IPage<Resource> page =
            resourceService.getPage(ResourceAssembler.toEntity(query), restRequest);
        return RestResponseBody.success(ResourceAssembler.toPageResponse(page));
    }

    @PutMapping("update")
    @RequiresPermissions("resource:update")
    public RestResponseBody<Void> updateResource(@Valid @FormOrJson ResourceUpdateRequest request) {
        resourceService.updateResource(ResourceAssembler.toEntity(request));
        return RestResponseBody.success();
    }

    @DeleteMapping("delete")
    @RequiresPermissions("resource:delete")
    public RestResponseBody<Void> deleteResource(@Valid @FormOrJson TeamScopedIdRequest request) {
        this.resourceService.remove(request.getId());
        return RestResponseBody.success();
    }

    @PostMapping("list")
    public RestResponseBody<List<ResourceResponse>> listResource(TeamIdRequest request) {
        List<Resource> resourceList = resourceService.listByTeamId(request.getTeamId());
        return RestResponseBody.success(ResourceAssembler.toListResponse(resourceList));
    }

    @PostMapping("upload")
    @RequiresPermissions("resource:add")
    public RestResponseBody<ResourceUploadResponse> upload(MultipartFile file) throws IOException {
        return RestResponseBody.success(ResourceAssembler.toUploadResponse(resourceService.upload(file)));
    }

    @PostMapping("upload_jars")
    public RestResponseBody<List<String>> listUploadJars() {
        List<String> jars = resourceService.listHistoryUploadJars();
        return RestResponseBody.success(jars);
    }

}
