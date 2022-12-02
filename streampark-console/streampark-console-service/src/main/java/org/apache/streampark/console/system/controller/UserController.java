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
import org.apache.streampark.console.base.util.ShaHashUtils;
import org.apache.streampark.console.core.enums.UserType;
import org.apache.streampark.console.core.service.CommonService;
import org.apache.streampark.console.system.entity.Team;
import org.apache.streampark.console.system.entity.User;
import org.apache.streampark.console.system.service.TeamService;
import org.apache.streampark.console.system.service.UserService;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @Autowired
    private CommonService commonService;

    @PostMapping("detail")
    public User detail(@NotBlank(message = "{required}") @PathVariable String username) {
        return this.userService.findByName(username);
    }

    @PostMapping("list")
    @RequiresPermissions(value = {"user:view", "app:view"}, logical = Logical.OR)
    public RestResponse userList(RestRequest restRequest, User user) {
        IPage<User> userList = userService.findUserDetail(user, restRequest);
        return RestResponse.success(userList);
    }

    @PostMapping("post")
    @RequiresPermissions("user:add")
    public RestResponse addUser(@Valid User user) throws Exception {
        this.userService.createUser(user);
        return RestResponse.success();
    }

    @PutMapping("update")
    @RequiresPermissions("user:update")
    public RestResponse updateUser(@Valid User user) throws Exception {
        this.userService.updateUser(user);
        return RestResponse.success();
    }

    @DeleteMapping("delete")
    @RequiresPermissions("user:delete")
    public RestResponse deleteUser(Long userId) throws Exception {
        this.userService.deleteUser(userId);
        return RestResponse.success();
    }

    @PutMapping("profile")
    public RestResponse updateProfile(@Valid User user) throws Exception {
        this.userService.updateProfile(user);
        return RestResponse.success();
    }

    @PutMapping("avatar")
    public RestResponse updateAvatar(
        @NotBlank(message = "{required}") String username,
        @NotBlank(message = "{required}") String avatar)
        throws Exception {
        this.userService.updateAvatar(username, avatar);
        return RestResponse.success();
    }

    @PostMapping("getNoTokenUser")
    public RestResponse getNoTokenUser() {
        List<User> userList = this.userService.getNoTokenUser();
        return RestResponse.success(userList);
    }

    @PostMapping("check/name")
    public RestResponse checkUserName(@NotBlank(message = "{required}") String username) {
        boolean result = this.userService.findByName(username) == null;
        return RestResponse.success(result);
    }

    @PostMapping("check/password")
    public RestResponse checkPassword(
        @NotBlank(message = "{required}") String username,
        @NotBlank(message = "{required}") String password) {

        User user = userService.findByName(username);
        String salt = user.getSalt();
        String encryptPassword = ShaHashUtils.encrypt(salt, password);
        boolean result = StringUtils.equals(user.getPassword(), encryptPassword);
        return RestResponse.success(result);
    }

    @PutMapping("password")
    public RestResponse updatePassword(User user)
        throws Exception {
        userService.updatePassword(user);
        return RestResponse.success();
    }

    @PutMapping("password/reset")
    @RequiresPermissions("user:reset")
    public RestResponse resetPassword(@NotBlank(message = "{required}") String usernames)
        throws Exception {
        String[] usernameArr = usernames.split(StringPool.COMMA);
        this.userService.resetPassword(usernameArr);
        return RestResponse.success();
    }

    @PostMapping("types")
    @RequiresPermissions("user:types")
    public RestResponse userTypes() {
        return RestResponse.success(UserType.values());
    }

    @PostMapping("initTeam")
    public RestResponse initTeam(Long teamId, Long userId) {
        Team team = teamService.getById(teamId);
        if (team == null) {
            return RestResponse.fail("teamId is invalid", ResponseCode.CODE_FAIL_ALERT);
        }
        userService.setLastTeam(teamId, userId);
        return RestResponse.success();
    }

    @PostMapping("setTeam")
    public RestResponse setTeam(Long teamId) {
        Team team = teamService.getById(teamId);
        if (team == null) {
            return RestResponse.fail("teamId is invalid", ResponseCode.CODE_FAIL_ALERT);
        }

        User user = commonService.getCurrentUser();

        //1) set the latest team
        userService.setLastTeam(teamId, user.getUserId());

        //2) get latest userInfo
        user.dataMasking();

        Map<String, Object> infoMap = userService.generateFrontendUserInfo(user, teamId, null);
        return new RestResponse().data(infoMap);
    }

    @PostMapping("appOwners")
    public RestResponse appOwners(Long teamId) {
        List<User> userList = userService.findByAppOwner(teamId);
        userList.forEach(User::dataMasking);
        return RestResponse.success(userList);
    }

}
