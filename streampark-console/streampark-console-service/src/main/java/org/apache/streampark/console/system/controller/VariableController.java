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

package org.apache.streampark.console.system.controller;

import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.base.domain.RestResponse;
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.service.CommonService;
import org.apache.streampark.console.system.entity.Variable;
import org.apache.streampark.console.system.service.VariableService;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
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
    private CommonService commonService;

    @Autowired
    private VariableService variableService;

    @PostMapping("list")
    @RequiresPermissions("variable:view")
    public RestResponse variableList(RestRequest restRequest, Variable variable) {
        IPage<Variable> variableList = variableService.findVariables(variable, restRequest);
        return RestResponse.success(variableList);
    }

    @PostMapping("post")
    @RequiresPermissions("variable:add")
    public RestResponse addVariable(@Valid Variable variable) throws Exception {
        boolean isExists = this.variableService.findByVariableCode(variable.getTeamId(), variable.getVariableCode()) != null;
        if (isExists) {
            throw new ApiAlertException("Sorry, the variable code already exists.");
        }
        isExists = this.variableService.findByVariableName(variable.getTeamId(), variable.getVariableName()) != null;
        if (isExists) {
            throw new ApiAlertException("Sorry, the variable name already exists.");
        }
        variable.setCreator(commonService.getCurrentUser().getUserId());
        this.variableService.createVariable(variable);
        return RestResponse.success();
    }

    @PutMapping("update")
    @RequiresPermissions("variable:update")
    public RestResponse updateVariable(@Valid Variable variable) throws Exception {
        Variable findVariable = this.variableService.findByVariableCode(variable.getTeamId(), variable.getVariableCode());
        if (findVariable == null) {
            throw new ApiAlertException("Sorry, the variable does not exist.");
        }
        if (findVariable.getId().longValue() != variable.getId().longValue()) {
            throw new ApiAlertException("Sorry, the variable id is inconsistent.");
        }
        this.variableService.updateVariable(variable);
        return RestResponse.success();
    }

    @DeleteMapping("delete")
    @RequiresPermissions("variable:delete")
    public RestResponse deleteVariables(@Valid Variable variable) {
        Variable findVariable = this.variableService.findByVariableCode(variable.getTeamId(), variable.getVariableCode());
        if (findVariable == null) {
            throw new ApiAlertException("Sorry, the variable does not exist.");
        }
        if (findVariable.getId().longValue() != variable.getId().longValue()) {
            throw new ApiAlertException("Sorry, the variable id is inconsistent.");
        }
        List<Application> dependApps = this.variableService.findDependByCode(variable);
        if (!(dependApps == null || dependApps.isEmpty())) {
            throw new ApiAlertException(String.format("Sorry, this variable is being used by [%s] applications.", dependApps.size()));
        }
        this.variableService.removeById(findVariable.getId());
        return RestResponse.success();
    }

    @PostMapping("check/code")
    public RestResponse checkVariableCode(@RequestParam Long teamId, @NotBlank(message = "{required}") String variableCode) {
        boolean result = this.variableService.findByVariableCode(teamId, variableCode) == null;
        return RestResponse.success(result);
    }

    /**
     * Check variable name when adding variable
     * @param teamId
     * @param variableName
     * @return
     */
    @PostMapping("check/addName")
    public RestResponse checkVariableNameForAdd(@RequestParam Long teamId, @NotBlank(message = "{required}") String variableName) {
        boolean result = this.variableService.findByVariableName(teamId, variableName) == null;
        return RestResponse.success(result);
    }

    /**
     * Check variable names when updating
     * @param variable
     * @return
     */
    @PostMapping("check/updateName")
    public RestResponse checkVariableNameForUpdate(@Valid Variable variable) {
        Variable findVariable = this.variableService.findByVariableName(variable.getTeamId(), variable.getVariableName());
        if (findVariable == null || findVariable.getId().longValue() == variable.getId().longValue()) {
            return RestResponse.success(true);
        }
        return RestResponse.success(false);
    }

    @PostMapping("select")
    public RestResponse selectVariables(@RequestParam Long teamId) {
        return RestResponse.success().data(this.variableService.findByTeamId(teamId));
    }
}
