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

package org.apache.streampark.console.core.managed.controller;

import org.apache.streampark.console.base.domain.RestResponse;
import org.apache.streampark.console.core.annotation.Permission;
import org.apache.streampark.console.core.managed.model.CloudAccountAvailableRequest;
import org.apache.streampark.console.core.managed.model.CloudAccountCreateRequest;
import org.apache.streampark.console.core.managed.model.CloudAccountGrantRequest;
import org.apache.streampark.console.core.managed.model.CloudAccountIdRequest;
import org.apache.streampark.console.core.managed.model.CloudAccountPageRequest;
import org.apache.streampark.console.core.managed.model.CloudAccountTeamListRequest;
import org.apache.streampark.console.core.managed.model.CloudAccountUpdateRequest;
import org.apache.streampark.console.core.managed.model.CloudAccountVersionedIdRequest;
import org.apache.streampark.console.core.managed.service.CloudAccountGrantService;
import org.apache.streampark.console.core.managed.service.CloudAccountService;

import org.apache.shiro.authz.annotation.RequiresPermissions;

import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/** Administrative endpoints for encrypted managed Flink cloud accounts. */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("cloud/account")
public class CloudAccountController {

    private final CloudAccountService cloudAccountService;
    private final CloudAccountGrantService cloudAccountGrantService;

    @PostMapping("page")
    @RequiresPermissions("cloud-account:view")
    public RestResponse page(@Valid CloudAccountPageRequest request) {
        return RestResponse.success(cloudAccountService.page(request));
    }

    @PostMapping("get")
    @RequiresPermissions("cloud-account:view")
    public RestResponse get(@Valid CloudAccountIdRequest request) {
        return RestResponse.success(cloudAccountService.get(request.getId()));
    }

    @PostMapping("create")
    @RequiresPermissions("cloud-account:create")
    public RestResponse create(@Valid CloudAccountCreateRequest request) {
        return RestResponse.success(cloudAccountService.create(request));
    }

    @PostMapping("update")
    @RequiresPermissions("cloud-account:update")
    public RestResponse update(@Valid CloudAccountUpdateRequest request) {
        cloudAccountService.update(request);
        return RestResponse.success();
    }

    @PostMapping("test")
    @RequiresPermissions("cloud-account:update")
    public RestResponse test(@Valid CloudAccountVersionedIdRequest request) {
        return RestResponse.success(cloudAccountService.test(request));
    }

    @PostMapping("disable")
    @RequiresPermissions("cloud-account:update")
    public RestResponse disable(@Valid CloudAccountVersionedIdRequest request) {
        cloudAccountService.disable(request);
        return RestResponse.success();
    }

    @PostMapping("delete")
    @RequiresPermissions("cloud-account:delete")
    public RestResponse delete(@Valid CloudAccountVersionedIdRequest request) {
        cloudAccountService.delete(request);
        return RestResponse.success();
    }

    @PostMapping("grant")
    @RequiresPermissions("cloud-account:grant")
    public RestResponse grant(@Valid CloudAccountGrantRequest request) {
        cloudAccountGrantService.replaceGrants(request);
        return RestResponse.success();
    }

    @PostMapping("grants")
    @RequiresPermissions("cloud-account:grant")
    public RestResponse grants(@Valid CloudAccountTeamListRequest request) {
        return RestResponse.success(
            cloudAccountGrantService.listGrants(request.getAccountId()));
    }

    @PostMapping("available")
    @RequiresPermissions("cloud-account:view")
    @Permission(team = "#request.teamId")
    public RestResponse available(@Valid CloudAccountAvailableRequest request) {
        return RestResponse.success(
            cloudAccountGrantService.listAvailableAccounts(request.getTeamId()));
    }
}
