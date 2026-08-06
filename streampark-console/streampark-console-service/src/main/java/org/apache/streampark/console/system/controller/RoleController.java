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
import org.apache.streampark.console.base.domain.RestResponseBody;
import org.apache.streampark.console.base.web.FormOrJson;
import org.apache.streampark.console.system.assembler.RoleAssembler;
import org.apache.streampark.console.system.entity.Role;
import org.apache.streampark.console.system.entity.RoleMenu;
import org.apache.streampark.console.system.request.role.RoleCheckNameRequest;
import org.apache.streampark.console.system.request.role.RoleCreateRequest;
import org.apache.streampark.console.system.request.role.RoleDeleteRequest;
import org.apache.streampark.console.system.request.role.RoleListQueryRequest;
import org.apache.streampark.console.system.request.role.RoleMenuQueryRequest;
import org.apache.streampark.console.system.request.role.RoleUpdateRequest;
import org.apache.streampark.console.system.response.role.RoleResponse;
import org.apache.streampark.console.system.service.RoleMenuService;
import org.apache.streampark.console.system.service.RoleService;

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
import java.util.stream.Collectors;

@Slf4j
@Validated
@RestController
@RequestMapping("role")
public class RoleController {

    @Autowired
    private RoleService roleService;
    @Autowired
    private RoleMenuService roleMenuService;

    @PostMapping("list")
    @RequiresPermissions("role:view")
    public RestResponseBody<IPage<RoleResponse>> roleList(RestRequest restRequest, RoleListQueryRequest query) {
        IPage<Role> roleList = roleService.getPage(RoleAssembler.toEntity(query), restRequest);
        return RestResponseBody.success(RoleAssembler.toPageResponse(roleList));
    }

    @PostMapping("check/name")
    public RestResponseBody<Boolean> checkRoleName(@Valid RoleCheckNameRequest request) {
        Role result = this.roleService.getByName(request.getRoleName());
        return RestResponseBody.success(result == null);
    }

    @PostMapping("menu")
    public RestResponseBody<List<String>> getRoleMenus(@Valid RoleMenuQueryRequest request) {
        List<RoleMenu> roleMenuList = this.roleMenuService.listByRoleId(request.getRoleId());
        List<String> menuIdList = roleMenuList.stream()
            .map(roleMenu -> String.valueOf(roleMenu.getMenuId()))
            .collect(Collectors.toList());
        return RestResponseBody.success(menuIdList);
    }

    @PostMapping("post")
    @RequiresPermissions("role:add")
    public RestResponseBody<Void> addRole(@Valid @FormOrJson RoleCreateRequest request) {
        this.roleService.createRole(RoleAssembler.toEntity(request));
        return RestResponseBody.success();
    }

    @DeleteMapping("delete")
    @RequiresPermissions("role:delete")
    public RestResponseBody<Void> deleteRole(@Valid @FormOrJson RoleDeleteRequest request) {
        this.roleService.removeById(request.getRoleId());
        return RestResponseBody.success();
    }

    @PutMapping("update")
    @RequiresPermissions("role:update")
    public RestResponseBody<Void> updateRole(@Valid @FormOrJson RoleUpdateRequest request) {
        this.roleService.updateRole(RoleAssembler.toEntity(request));
        return RestResponseBody.success();
    }
}
