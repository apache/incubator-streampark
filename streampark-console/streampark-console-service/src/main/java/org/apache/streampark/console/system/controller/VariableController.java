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

import org.apache.streampark.console.base.domain.ResponseCode;
import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.base.domain.RestResponse;
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
    CommonService commonService;

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
            return RestResponse.fail("Sorry, the variable Code already exists", ResponseCode.CODE_FAIL_ALERT);
        }
        isExists = this.variableService.findByVariableName(variable.getTeamId(), variable.getVariableName()) != null;
        if (isExists) {
            return RestResponse.fail("Sorry, the variable Name already exists", ResponseCode.CODE_FAIL_ALERT);
        }
        variable.setUserId(commonService.getCurrentUser().getUserId());
        this.variableService.createVariable(variable);
        return RestResponse.success();
    }

    @PutMapping("update")
    @RequiresPermissions("variable:update")
    public RestResponse updateVariable(@Valid Variable variable) throws Exception {
        Variable findVariable = this.variableService.findByVariableCode(variable.getTeamId(), variable.getVariableCode());
        if (findVariable == null) {
            return RestResponse.fail("Sorry, the variable does not exist!", ResponseCode.CODE_FAIL_ALERT);
        }
        if (findVariable.getId() != variable.getId()) {
            return RestResponse.fail("Sorry, the variable id is inconsistent!", ResponseCode.CODE_FAIL_ALERT);
        }
        this.variableService.updateVariable(variable);
        return RestResponse.success();
    }

    @DeleteMapping("delete")
    @RequiresPermissions("variable:delete")
    public RestResponse deleteVariables(@Valid Variable variable) {
        Variable findVariable = this.variableService.findByVariableCode(variable.getTeamId(), variable.getVariableCode());
        if (findVariable == null) {
            return RestResponse.fail("Sorry, the variable does not exist!", ResponseCode.CODE_FAIL_ALERT);
        }
        if (findVariable.getId() != variable.getId()) {
            return RestResponse.fail("Sorry, the variable id is inconsistent!", ResponseCode.CODE_FAIL_ALERT);
        }
        List<Application> dependApps = this.variableService.findDependByCode(variable);
        if (!(dependApps == null || dependApps.isEmpty())) {
            return RestResponse.fail("Sorry, this variable is being used by " + dependApps.size() + " applications", ResponseCode.CODE_FAIL_ALERT);
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
        if (findVariable == null || findVariable.getId() == variable.getId()) {
            return RestResponse.success(true);
        }
        return RestResponse.success(false);
    }

    @PostMapping("select")
    public RestResponse selectVariables(@RequestParam Long teamId) {
        return RestResponse.success().data(this.variableService.findByTeamId(teamId));
    }
}
