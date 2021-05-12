/*
 * Copyright (c) 2019 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.streamxhub.streamx.console.system.controller;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.streamxhub.streamx.console.base.domain.RestResponse;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import lombok.extern.slf4j.Slf4j;

import com.streamxhub.streamx.console.base.controller.BaseController;
import com.streamxhub.streamx.console.base.domain.RestRequest;
import com.streamxhub.streamx.console.base.exception.ServiceException;
import com.streamxhub.streamx.console.system.entity.Role;
import com.streamxhub.streamx.console.system.entity.RoleMenu;
import com.streamxhub.streamx.console.system.service.RoleMenuServie;
import com.streamxhub.streamx.console.system.service.RoleService;

/**
 * @author benjobs
 */
@Slf4j
@Validated
@RestController
@RequestMapping("role")
public class RoleController extends BaseController {

    @Autowired
    private RoleService roleService;
    @Autowired
    private RoleMenuServie roleMenuServie;

    private String message;

    @PostMapping("list")
    @RequiresPermissions("role:view")
    public Map<String, Object> roleList(RestRequest restRequest, Role role) {
        return getDataTable(roleService.findRoles(role, restRequest));
    }

    @PostMapping("check/name")
    public boolean checkRoleName(@NotBlank(message = "{required}") String roleName) {
        Role result = this.roleService.findByName(roleName);
        return result == null;
    }

    @PostMapping("menu")
    public List<String> getRoleMenus(@NotBlank(message = "{required}") String roleId) {
        List<RoleMenu> list = this.roleMenuServie.getRoleMenusByRoleId(roleId);
        return list.stream()
                .map(roleMenu -> String.valueOf(roleMenu.getMenuId()))
                .collect(Collectors.toList());
    }

    @PostMapping("post")
    @RequiresPermissions("role:add")
    public void addRole(@Valid Role role) throws ServiceException {
        try {
            this.roleService.createRole(role);
        } catch (Exception e) {
            message = "新增角色失败";
            log.info(message, e);
            throw new ServiceException(message);
        }
    }

    @DeleteMapping("delete")
    @RequiresPermissions("role:delete")
    public RestResponse deleteRole(Long roleId) throws ServiceException {
        try {
            this.roleService.removeById(roleId);
            return RestResponse.create().data(true);
        } catch (Exception e) {
            message = "delete user failed, error:" + e.getMessage();
            log.info(message, e);
            throw new ServiceException(message);
        }
    }

    @PutMapping("update")
    @RequiresPermissions("role:update")
    public void updateRole(Role role) throws ServiceException {
        try {
            this.roleService.updateRole(role);
        } catch (Exception e) {
            message = "修改角色失败";
            log.info(message, e);
            throw new ServiceException(message);
        }
    }

}
