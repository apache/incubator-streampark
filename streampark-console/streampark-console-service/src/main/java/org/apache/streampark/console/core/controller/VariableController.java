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
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.entity.Variable;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("variable")
public class VariableController {

    @Autowired
    private VariableService variableService;

    /**
     * Get variable list by page.
     *
     * @param pageRequest
     * @param variable
     * @return
     */
    @PostMapping("page")
    @RequiresPermissions("variable:view")
    public Response<IPage<Variable>> page(PageRequest pageRequest, Variable variable) {
        IPage<Variable> page = variableService.getPage(variable, pageRequest);
        for (Variable v : page.getRecords()) {
            v.dataMasking();
        }
        return Response.success(page);
    }

    /**
     * Get variables through team and search keywords.
     *
     * @param teamId
     * @param keyword Fuzzy search keywords through variable code or description, Nullable.
     * @return
     */
    @PostMapping("list")
    public Response<List<Variable>> variableList(@RequestParam Long teamId, String keyword) {
        List<Variable> variableList = variableService.listByTeamId(teamId, keyword);
        for (Variable v : variableList) {
            v.dataMasking();
        }
        return Response.success(variableList);
    }

    @PostMapping("depend_apps")
    @RequiresPermissions("variable:depend_apps")
    public Response<IPage<Application>> dependApps(PageRequest pageRequest, Variable variable) {
        IPage<Application> dependApps = variableService.getDependAppsPage(variable, pageRequest);
        return Response.success(dependApps);
    }

    @PostMapping("post")
    @RequiresPermissions("variable:add")
    public Response<Void> addVariable(@Valid Variable variable) {
        this.variableService.createVariable(variable);
        return Response.success();
    }

    @PutMapping("update")
    @RequiresPermissions("variable:update")
    public Response<Void> updateVariable(@Valid Variable variable) {
        variableService.updateVariable(variable);
        return Response.success();
    }

    @PostMapping("show_original")
    @RequiresPermissions("variable:show_original")
    public Response<Variable> showOriginal(@RequestParam Long id) {
        Variable v = this.variableService.getById(id);
        return Response.success(v);
    }

    @DeleteMapping("delete")
    @RequiresPermissions("variable:delete")
    public Response<Void> deleteVariable(@Valid Variable variable) {
        this.variableService.remove(variable);
        return Response.success();
    }

    @PostMapping("check/code")
    public Response<Boolean> checkVariableCode(
                                               @RequestParam Long teamId,
                                               @NotBlank(message = "{required}") String variableCode) {
        boolean result = this.variableService.findByVariableCode(teamId, variableCode) == null;
        return Response.success(result);
    }
}
