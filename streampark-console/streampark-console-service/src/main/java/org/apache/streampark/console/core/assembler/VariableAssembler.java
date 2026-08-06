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

import org.apache.streampark.console.core.entity.Variable;
import org.apache.streampark.console.core.request.common.TeamScopedIdRequest;
import org.apache.streampark.console.core.request.variable.VariableCreateRequest;
import org.apache.streampark.console.core.request.variable.VariablePageQueryRequest;
import org.apache.streampark.console.core.request.variable.VariableUpdateRequest;
import org.apache.streampark.console.core.response.variable.VariableResponse;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.BeanUtils;

import java.util.List;

/** Converts between variable entities and API request/response contracts. */
public final class VariableAssembler {

    private VariableAssembler() {
    }

    public static Variable toEntity(VariableCreateRequest request) {
        if (request == null) {
            return null;
        }
        Variable variable = new Variable();
        BeanUtils.copyProperties(request, variable);
        return variable;
    }

    public static Variable toEntity(VariableUpdateRequest request) {
        if (request == null) {
            return null;
        }
        Variable variable = toEntity((VariableCreateRequest) request);
        variable.setId(request.getId());
        return variable;
    }

    public static Variable toEntity(VariablePageQueryRequest request) {
        if (request == null) {
            return null;
        }
        Variable variable = new Variable();
        variable.setTeamId(request.getTeamId());
        variable.setVariableCode(request.getVariableCode());
        return variable;
    }

    public static Variable toEntity(TeamScopedIdRequest request) {
        if (request == null) {
            return null;
        }
        Variable variable = new Variable();
        variable.setId(request.getId());
        variable.setTeamId(request.getTeamId());
        return variable;
    }

    public static VariableResponse toResponse(Variable variable) {
        return DtoAssembler.toDto(variable, VariableResponse.class);
    }

    public static IPage<VariableResponse> toPageResponse(IPage<Variable> page) {
        return DtoAssembler.toPage(page, VariableAssembler::toResponse);
    }

    public static List<VariableResponse> toListResponse(List<Variable> variables) {
        return DtoAssembler.toList(variables, VariableAssembler::toResponse);
    }
}
