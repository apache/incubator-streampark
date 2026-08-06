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

package org.apache.streampark.console.core.assembler;

import org.apache.streampark.console.core.bean.UploadResponse;
import org.apache.streampark.console.core.entity.Resource;
import org.apache.streampark.console.core.enums.EngineTypeEnum;
import org.apache.streampark.console.core.enums.ResourceTypeEnum;
import org.apache.streampark.console.core.request.common.TeamScopedIdRequest;
import org.apache.streampark.console.core.request.resource.ResourceCreateRequest;
import org.apache.streampark.console.core.request.resource.ResourcePageQueryRequest;
import org.apache.streampark.console.core.request.resource.ResourceUpdateRequest;
import org.apache.streampark.console.core.response.resource.ResourceCheckResponse;
import org.apache.streampark.console.core.response.resource.ResourceResponse;
import org.apache.streampark.console.core.response.resource.ResourceUploadResponse;
import org.apache.streampark.console.core.service.result.ResourceCheckResult;

import org.apache.commons.lang3.StringUtils;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.BeanUtils;

import java.util.List;

/** Converts between resource entities and API request/response contracts. */
public final class ResourceAssembler {

    private ResourceAssembler() {
    }

    public static Resource toEntity(ResourceCreateRequest request) {
        if (request == null) {
            return null;
        }
        Resource resource = new Resource();
        BeanUtils.copyProperties(request, resource, "resourceType", "engineType", "resourcePath");
        resource.setResourceType(parseResourceType(request.getResourceType()));
        resource.setEngineType(parseEngineType(request.getEngineType()));
        return resource;
    }

    public static Resource toEntity(ResourceUpdateRequest request) {
        if (request == null) {
            return null;
        }
        Resource resource = toEntity((ResourceCreateRequest) request);
        resource.setId(request.getId());
        return resource;
    }

    public static Resource toEntity(ResourcePageQueryRequest request) {
        if (request == null) {
            return null;
        }
        Resource resource = new Resource();
        resource.setTeamId(request.getTeamId());
        resource.setResourceName(request.getResourceName());
        resource.setResourceType(parseResourceType(request.getResourceType()));
        resource.setEngineType(parseEngineType(request.getEngineType()));
        return resource;
    }

    public static Resource toEntity(TeamScopedIdRequest request) {
        if (request == null) {
            return null;
        }
        Resource resource = new Resource();
        resource.setId(request.getId());
        resource.setTeamId(request.getTeamId());
        return resource;
    }

    public static ResourceResponse toResponse(Resource resource) {
        if (resource == null) {
            return null;
        }
        ResourceResponse response = DtoAssembler.toDto(resource, ResourceResponse.class);
        if (resource.getResourceType() != null) {
            response.setResourceType(resource.getResourceType().name());
        }
        if (resource.getEngineType() != null) {
            response.setEngineType(resource.getEngineType().name());
        }
        return response;
    }

    public static IPage<ResourceResponse> toPageResponse(IPage<Resource> page) {
        return DtoAssembler.toPage(page, ResourceAssembler::toResponse);
    }

    public static List<ResourceResponse> toListResponse(List<Resource> resources) {
        return DtoAssembler.toList(resources, ResourceAssembler::toResponse);
    }

    public static ResourceUploadResponse toUploadResponse(UploadResponse upload) {
        return DtoAssembler.toDto(upload, ResourceUploadResponse.class);
    }

    public static ResourceCheckResponse toCheckResponse(ResourceCheckResult result) {
        if (result == null) {
            return null;
        }
        ResourceCheckResponse response = new ResourceCheckResponse();
        response.setState(result.getState());
        response.setException(result.getException());
        response.setConnector(result.getConnector());
        return response;
    }

    private static ResourceTypeEnum parseResourceType(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return ResourceTypeEnum.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return ResourceTypeEnum.of(Integer.valueOf(value));
        }
    }

    private static EngineTypeEnum parseEngineType(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return EngineTypeEnum.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return EngineTypeEnum.of(Integer.valueOf(value));
        }
    }
}
