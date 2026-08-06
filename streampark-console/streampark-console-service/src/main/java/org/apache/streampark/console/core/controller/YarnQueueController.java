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
import org.apache.streampark.console.core.assembler.YarnQueueAssembler;
import org.apache.streampark.console.core.entity.YarnQueue;
import org.apache.streampark.console.core.request.yarn.YarnQueueCreateRequest;
import org.apache.streampark.console.core.request.yarn.YarnQueueDeleteRequest;
import org.apache.streampark.console.core.request.yarn.YarnQueueListQueryRequest;
import org.apache.streampark.console.core.request.yarn.YarnQueueUpdateRequest;
import org.apache.streampark.console.core.response.yarn.YarnQueueCheckResponse;
import org.apache.streampark.console.core.response.yarn.YarnQueueResponse;
import org.apache.streampark.console.core.service.YarnQueueService;

import org.apache.shiro.authz.annotation.RequiresPermissions;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Slf4j
@Validated
@RestController
@RequestMapping("yarn/queue")
public class YarnQueueController {

    @Autowired
    private YarnQueueService yarnQueueService;

    @PostMapping("list")
    public RestResponseBody<IPage<YarnQueueResponse>> list(RestRequest restRequest, YarnQueueListQueryRequest query) {
        IPage<YarnQueue> queuePage =
            yarnQueueService.getPage(YarnQueueAssembler.toEntity(query), restRequest);
        return RestResponseBody.success(YarnQueueAssembler.toPageResponse(queuePage));
    }

    @PostMapping("check")
    public RestResponseBody<YarnQueueCheckResponse> check(YarnQueueCreateRequest request) {
        return RestResponseBody.success(
            YarnQueueAssembler.toCheckResponse(yarnQueueService.checkYarnQueue(YarnQueueAssembler.toEntity(request))));
    }

    @PostMapping("create")
    @RequiresPermissions("yarnQueue:create")
    public RestResponseBody<Boolean> create(@Valid @FormOrJson YarnQueueCreateRequest request) {
        return RestResponseBody.success(yarnQueueService.createYarnQueue(YarnQueueAssembler.toEntity(request)));
    }

    @PostMapping("update")
    @RequiresPermissions("yarnQueue:update")
    public RestResponseBody<Void> update(@Valid @FormOrJson YarnQueueUpdateRequest request) {
        yarnQueueService.updateYarnQueue(YarnQueueAssembler.toEntity(request));
        return RestResponseBody.success();
    }

    @PostMapping("delete")
    @RequiresPermissions("yarnQueue:delete")
    public RestResponseBody<Void> delete(@Valid @FormOrJson YarnQueueDeleteRequest request) {
        yarnQueueService.remove(YarnQueueAssembler.toEntity(request));
        return RestResponseBody.success();
    }
}
