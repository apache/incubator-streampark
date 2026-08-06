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
import org.apache.streampark.console.core.assembler.FlinkApplicationAssembler;
import org.apache.streampark.console.core.assembler.VariableAssembler;
import org.apache.streampark.console.core.entity.FlinkApplication;
import org.apache.streampark.console.core.entity.Variable;
import org.apache.streampark.console.core.request.common.TeamScopedIdRequest;
import org.apache.streampark.console.core.request.variable.VariableCheckCodeRequest;
import org.apache.streampark.console.core.request.variable.VariableCreateRequest;
import org.apache.streampark.console.core.request.variable.VariableListRequest;
import org.apache.streampark.console.core.request.variable.VariablePageQueryRequest;
import org.apache.streampark.console.core.request.variable.VariableUpdateRequest;
import org.apache.streampark.console.core.response.flink.FlinkAppResponse;
import org.apache.streampark.console.core.response.variable.VariableResponse;
import org.apache.streampark.console.core.service.VariableService;

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

import javax.validation.Valid;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("variable")
public class VariableController {

    @Autowired
    private VariableService variableService;

    @PostMapping("page")
    @RequiresPermissions("variable:view")
    public RestResponseBody<IPage<VariableResponse>> page(RestRequest restRequest, VariablePageQueryRequest query) {
        IPage<Variable> page =
            variableService.getPage(VariableAssembler.toEntity(query), restRequest);
        for (Variable v : page.getRecords()) {
            v.dataMasking();
        }
        return RestResponseBody.success(VariableAssembler.toPageResponse(page));
    }

    @PostMapping("list")
    public RestResponseBody<List<VariableResponse>> variableList(VariableListRequest request) {
        List<Variable> variableList =
            variableService.listByTeamId(request.getTeamId(), request.getKeyword());
        for (Variable v : variableList) {
            v.dataMasking();
        }
        return RestResponseBody.success(VariableAssembler.toListResponse(variableList));
    }

    @PostMapping("depend_apps")
    @RequiresPermissions("variable:depend_apps")
    public RestResponseBody<IPage<FlinkAppResponse>> dependApps(RestRequest restRequest, TeamScopedIdRequest request) {
        IPage<FlinkApplication> dependApps =
            variableService.getDependAppsPage(VariableAssembler.toEntity(request), restRequest);
        return RestResponseBody.success(FlinkApplicationAssembler.toPageResponse(dependApps));
    }

    @PostMapping("post")
    @RequiresPermissions("variable:add")
    public RestResponseBody<Void> addVariable(@Valid @FormOrJson VariableCreateRequest request) {
        this.variableService.createVariable(VariableAssembler.toEntity(request));
        return RestResponseBody.success();
    }

    @PutMapping("update")
    @RequiresPermissions("variable:update")
    public RestResponseBody<Void> updateVariable(@Valid @FormOrJson VariableUpdateRequest request) {
        variableService.updateVariable(VariableAssembler.toEntity(request));
        return RestResponseBody.success();
    }

    @PostMapping("show_original")
    @RequiresPermissions("variable:show_original")
    public RestResponseBody<VariableResponse> showOriginal(TeamScopedIdRequest request) {
        Variable v = this.variableService.getById(request.getId());
        return RestResponseBody.success(VariableAssembler.toResponse(v));
    }

    @DeleteMapping("delete")
    @RequiresPermissions("variable:delete")
    public RestResponseBody<Void> deleteVariable(@Valid @FormOrJson TeamScopedIdRequest request) {
        this.variableService.remove(VariableAssembler.toEntity(request));
        return RestResponseBody.success();
    }

    @PostMapping("check/code")
    public RestResponseBody<Boolean> checkVariableCode(@Valid VariableCheckCodeRequest request) {
        boolean result =
            this.variableService.findByVariableCode(request.getTeamId(), request.getVariableCode()) == null;
        return RestResponseBody.success(result);
    }
}
