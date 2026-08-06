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

import org.apache.streampark.common.util.AssertUtils;
import org.apache.streampark.console.base.domain.RestResponseBody;
import org.apache.streampark.console.base.web.FormOrJson;
import org.apache.streampark.console.core.assembler.ExternalLinkAssembler;
import org.apache.streampark.console.core.request.common.IdRequest;
import org.apache.streampark.console.core.request.externallink.ExternalLinkCreateRequest;
import org.apache.streampark.console.core.request.externallink.ExternalLinkRenderRequest;
import org.apache.streampark.console.core.request.externallink.ExternalLinkUpdateRequest;
import org.apache.streampark.console.core.response.externallink.ExternalLinkResponse;
import org.apache.streampark.console.core.service.ExternalLinkService;

import org.apache.shiro.authz.annotation.RequiresPermissions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("/flink/externalLink")
public class ExternalLinkController {

    @Autowired
    private ExternalLinkService externalLinkService;

    @PostMapping("/list")
    @RequiresPermissions("externalLink:view")
    public RestResponseBody<List<ExternalLinkResponse>> list() {
        return RestResponseBody.success(ExternalLinkAssembler.toListResponse(externalLinkService.list()));
    }

    @PostMapping("/render")
    public RestResponseBody<List<ExternalLinkResponse>> render(@Valid ExternalLinkRenderRequest request) {
        return RestResponseBody.success(
            ExternalLinkAssembler.toListResponse(externalLinkService.render(request.getAppId())));
    }

    @PostMapping("/create")
    @RequiresPermissions("externalLink:create")
    public RestResponseBody<Void> create(@Valid @FormOrJson ExternalLinkCreateRequest request) {
        externalLinkService.create(ExternalLinkAssembler.toEntity(request));
        return RestResponseBody.success();
    }

    @PostMapping("/update")
    @RequiresPermissions("externalLink:update")
    public RestResponseBody<Void> update(@Valid @FormOrJson ExternalLinkUpdateRequest request) {
        AssertUtils.notNull(request.getId(), "The link id cannot be null");
        externalLinkService.update(ExternalLinkAssembler.toEntity(request));
        return RestResponseBody.success();
    }

    @DeleteMapping("/delete")
    @RequiresPermissions("externalLink:delete")
    public RestResponseBody<Void> delete(@Valid @FormOrJson IdRequest request) {
        externalLinkService.removeById(request.getId());
        return RestResponseBody.success();
    }
}
