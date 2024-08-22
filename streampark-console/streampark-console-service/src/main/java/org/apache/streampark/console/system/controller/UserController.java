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
import org.apache.streampark.console.system.entity.Team;
import org.apache.streampark.console.system.entity.User;
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
import javax.validation.constraints.NotBlank;

import java.util.List;
import java.util.Map;

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
    public RestResponse userList(RestRequest restRequest, User user) {
        IPage<User> userList = userService.getPage(user, restRequest);
        return RestResponse.success(userList);
    }

    @PostMapping("post")
    @RequiresPermissions("user:add")
    public RestResponse addUser(@Valid User user) throws Exception {
        user.setLoginType(LoginTypeEnum.PASSWORD);
        this.userService.createUser(user);
        return RestResponse.success();
    }

    @PutMapping("update")
    @Permission(user = "#user.id")
    @RequiresPermissions("user:update")
    public RestResponse updateUser(@Valid User user) throws Exception {
        return this.userService.updateUser(user);
    }

    @PutMapping("transferResource")
    @RequiresPermissions("user:update")
    public RestResponse transferResource(Long userId, Long targetUserId) {
        this.userService.transferResource(userId, targetUserId);
        return RestResponse.success();
    }

    @DeleteMapping("delete")
    @Permission(user = "#userId")
    @RequiresPermissions("user:delete")
    public RestResponse deleteUser(Long userId) throws Exception {
        this.userService.deleteUser(userId);
        return RestResponse.success();
    }

    @PostMapping("getNoTokenUser")
    public RestResponse getNoTokenUser() {
        List<User> userList = this.userService.listNoTokenUser();
        return RestResponse.success(userList);
    }

    @PostMapping("check/name")
    public RestResponse checkUserName(@NotBlank(message = "{required}") String username) {
        boolean result = this.userService.getByUsername(username) == null;
        return RestResponse.success(result);
    }

    @PutMapping("password")
    @Permission(user = "#user.id")
    public RestResponse updatePassword(User user) throws Exception {
        userService.updatePassword(user);
        return RestResponse.success();
    }

    @PutMapping("password/reset")
    @RequiresPermissions("user:reset")
    public RestResponse resetPassword(@NotBlank(message = "{required}") String username) throws Exception {
        String newPass = this.userService.resetPassword(username);
        return RestResponse.success(newPass);
    }

    @PostMapping("set_team")
    public RestResponse setTeam(Long teamId) {
        Team team = teamService.getById(teamId);
        if (team == null) {
            return RestResponse.fail(ResponseCode.CODE_FAIL_ALERT, "TeamId is invalid, set team failed.");
        }
        User user = ServiceHelper.getLoginUser();
        ApiAlertException.throwIfNull(user, "Current login user is null, set team failed.");
        // 1) set the latest team
        userService.setLastTeam(teamId, user.getUserId());

        // 2) get latest userInfo
        user.dataMasking();
        user.setLastTeamId(teamId);

        Map<String, Object> infoMap = userService.generateFrontendUserInfo(user, null);
        return new RestResponse().data(infoMap);
    }

    @PostMapping("appOwners")
    public RestResponse appOwners(Long teamId) {
        List<User> userList = userService.listByTeamId(teamId);
        userList.forEach(User::dataMasking);
        return RestResponse.success(userList);
    }
}
