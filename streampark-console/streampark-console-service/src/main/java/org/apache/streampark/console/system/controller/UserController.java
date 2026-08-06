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
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.core.annotation.Permission;
import org.apache.streampark.console.core.enums.LoginTypeEnum;
import org.apache.streampark.console.core.util.ServiceHelper;
import org.apache.streampark.console.system.assembler.UserAssembler;
import org.apache.streampark.console.system.entity.Team;
import org.apache.streampark.console.system.entity.User;
import org.apache.streampark.console.system.request.user.UserCheckNameRequest;
import org.apache.streampark.console.system.request.user.UserCreateRequest;
import org.apache.streampark.console.system.request.user.UserDeleteRequest;
import org.apache.streampark.console.system.request.user.UserListQueryRequest;
import org.apache.streampark.console.system.request.user.UserPasswordUpdateRequest;
import org.apache.streampark.console.system.request.user.UserResetPasswordRequest;
import org.apache.streampark.console.system.request.user.UserTeamIdRequest;
import org.apache.streampark.console.system.request.user.UserTransferResourceRequest;
import org.apache.streampark.console.system.request.user.UserUpdateRequest;
import org.apache.streampark.console.system.service.TeamService;
import org.apache.streampark.console.system.service.UserService;

import org.apache.shiro.authz.annotation.Logical;
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
@RequestMapping("user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private TeamService teamService;

    @PostMapping("list")
    @RequiresPermissions(value = {"user:view", "app:view"}, logical = Logical.OR)
    public RestResponse userList(RestRequest restRequest, UserListQueryRequest query) {
        IPage<User> userList = userService.getPage(UserAssembler.toEntity(query), restRequest);
        return RestResponse.success(UserAssembler.toPageResponse(userList));
    }

    @PostMapping("post")
    @RequiresPermissions("user:add")
    public RestResponse addUser(@Valid UserCreateRequest request) throws Exception {
        User user = UserAssembler.toEntity(request);
        user.setLoginType(LoginTypeEnum.PASSWORD);
        this.userService.createUser(user);
        return RestResponse.success();
    }

    @PutMapping("update")
    @Permission(user = "#request.userId")
    @RequiresPermissions("user:update")
    public RestResponse updateUser(@Valid UserUpdateRequest request) throws Exception {
        return this.userService.updateUser(UserAssembler.toEntity(request));
    }

    @PutMapping("transferResource")
    @RequiresPermissions("user:update")
    public RestResponse transferResource(UserTransferResourceRequest request) {
        this.userService.transferResource(request.getUserId(), request.getTargetUserId());
        return RestResponse.success();
    }

    @DeleteMapping("delete")
    @Permission(user = "#request.userId")
    @RequiresPermissions("user:delete")
    public RestResponse deleteUser(UserDeleteRequest request) throws Exception {
        this.userService.deleteUser(request.getUserId());
        return RestResponse.success();
    }

    @PostMapping("getNoTokenUser")
    public RestResponse getNoTokenUser() {
        return RestResponse.success(UserAssembler.toResponseList(this.userService.listNoTokenUser()));
    }

    @PostMapping("check/name")
    public RestResponse checkUserName(@Valid UserCheckNameRequest request) {
        boolean result = this.userService.getByUsername(request.getUsername()) == null;
        return RestResponse.success(result);
    }

    @PutMapping("password")
    @Permission(user = "#request.userId")
    public RestResponse updatePassword(UserPasswordUpdateRequest request) throws Exception {
        userService.updatePassword(UserAssembler.toEntity(request));
        return RestResponse.success();
    }

    @PutMapping("password/reset")
    @RequiresPermissions("user:reset")
    public RestResponse resetPassword(@Valid UserResetPasswordRequest request) throws Exception {
        String newPass = this.userService.resetPassword(request.getUsername());
        return RestResponse.success(newPass);
    }

    @PostMapping("set_team")
    public RestResponse setTeam(UserTeamIdRequest request) {
        Team team = teamService.getById(request.getTeamId());
        if (team == null) {
            return RestResponse.fail(ResponseCode.CODE_FAIL_ALERT, "TeamId is invalid, set team failed.");
        }
        User user = ServiceHelper.getLoginUser();
        ApiAlertException.throwIfNull(user, "Current login user is null, set team failed.");
        userService.setLastTeam(request.getTeamId(), user.getUserId());

        user.dataMasking();
        user.setLastTeamId(request.getTeamId());

        return new RestResponse().data(UserAssembler.toSessionResponse(
            userService.generateFrontendUserInfo(user, null)));
    }

    @PostMapping("appOwners")
    public RestResponse appOwners(UserTeamIdRequest request) {
        List<User> userList = userService.listByTeamId(request.getTeamId());
        userList.forEach(User::dataMasking);
        return RestResponse.success(UserAssembler.toResponseList(userList));
    }
}
