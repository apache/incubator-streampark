/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.console.system.controller;

import com.streamxhub.streamx.console.base.domain.RestRequest;
import com.streamxhub.streamx.console.base.domain.RestResponse;
import com.streamxhub.streamx.console.base.util.ShaHashUtils;
import com.streamxhub.streamx.console.system.entity.User;
import com.streamxhub.streamx.console.system.service.UserService;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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

/**
 * @author benjobs
 */
@Slf4j
@Validated
@RestController
@RequestMapping("user")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("detail")
    public User detail(@NotBlank(message = "{required}") @PathVariable String username) {
        return this.userService.findByName(username);
    }

    @PostMapping("list")
    @RequiresPermissions("user:view")
    public RestResponse userList(RestRequest restRequest, User user) {
        IPage<User> userList = userService.findUserDetail(user, restRequest);
        return RestResponse.create().data(userList);
    }

    @PostMapping("post")
    @RequiresPermissions("user:add")
    public RestResponse addUser(@Valid User user) throws Exception {
        this.userService.createUser(user);
        return RestResponse.create();
    }

    @PutMapping("update")
    @RequiresPermissions("user:update")
    public RestResponse updateUser(@Valid User user) throws Exception {
        this.userService.updateUser(user);
        return RestResponse.create();
    }

    @DeleteMapping("delete")
    @RequiresPermissions("user:delete")
    public RestResponse deleteUsers(Long userId) {
        this.userService.removeById(userId);
        return RestResponse.create();
    }

    @PutMapping("profile")
    public RestResponse updateProfile(@Valid User user) throws Exception {
        this.userService.updateProfile(user);
        return RestResponse.create();
    }

    @PutMapping("avatar")
    public RestResponse updateAvatar(
        @NotBlank(message = "{required}") String username,
        @NotBlank(message = "{required}") String avatar)
        throws Exception {
        this.userService.updateAvatar(username, avatar);
        return RestResponse.create();
    }

    @PostMapping("getNoTokenUser")
    public RestResponse getNoTokenUser() {
        List<User> userList = this.userService.getNoTokenUser();
        return RestResponse.create().data(userList);
    }

    @PostMapping("check/name")
    public RestResponse checkUserName(@NotBlank(message = "{required}") String username) {
        boolean result = this.userService.findByName(username) == null;
        return RestResponse.create().data(result);
    }

    @PostMapping("check/password")
    public RestResponse checkPassword(
        @NotBlank(message = "{required}") String username,
        @NotBlank(message = "{required}") String password) {

        User user = userService.findByName(username);
        String salt = user.getSalt();
        String encryptPassword = ShaHashUtils.encrypt(salt, password);
        boolean result = StringUtils.equals(user.getPassword(), encryptPassword);
        return RestResponse.create().data(result);
    }

    @PutMapping("password")
    public RestResponse updatePassword(
        @NotBlank(message = "{required}") String username,
        @NotBlank(message = "{required}") String password)
        throws Exception {
        userService.updatePassword(username, password);
        return RestResponse.create();
    }

    @PutMapping("password/reset")
    @RequiresPermissions("user:reset")
    public RestResponse resetPassword(@NotBlank(message = "{required}") String usernames)
        throws Exception {
        String[] usernameArr = usernames.split(StringPool.COMMA);
        this.userService.resetPassword(usernameArr);
        return RestResponse.create();
    }

}
