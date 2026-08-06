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

package org.apache.streampark.console.system.assembler;

import org.apache.streampark.console.core.assembler.DtoAssembler;
import org.apache.streampark.console.system.entity.Role;
import org.apache.streampark.console.system.request.role.RoleCreateRequest;
import org.apache.streampark.console.system.request.role.RoleListQueryRequest;
import org.apache.streampark.console.system.request.role.RoleUpdateRequest;
import org.apache.streampark.console.system.response.role.RoleResponse;

import com.baomidou.mybatisplus.core.metadata.IPage;

/** Converts between role entities and API request/response contracts. */
public final class RoleAssembler {

    private RoleAssembler() {
    }

    public static Role toEntity(RoleListQueryRequest request) {
        return DtoAssembler.toDto(request, Role.class);
    }

    public static Role toEntity(RoleCreateRequest request) {
        return DtoAssembler.toDto(request, Role.class);
    }

    public static Role toEntity(RoleUpdateRequest request) {
        return DtoAssembler.toDto(request, Role.class);
    }

    public static RoleResponse toResponse(Role role) {
        return DtoAssembler.toDto(role, RoleResponse.class);
    }

    public static IPage<RoleResponse> toPageResponse(IPage<Role> page) {
        return DtoAssembler.toPage(page, RoleAssembler::toResponse);
    }
}
