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

import java.util.Map;

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

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.streamxhub.streamx.console.base.controller.BaseController;
import com.streamxhub.streamx.console.base.domain.RestRequest;
import com.streamxhub.streamx.console.base.exception.ServiceException;
import com.streamxhub.streamx.console.base.utils.ShaHashUtil;
import com.streamxhub.streamx.console.system.entity.User;
import com.streamxhub.streamx.console.system.service.UserService;

/**
 * @author benjobs
 */
@Slf4j
@Validated
@RestController
@RequestMapping("user")
public class UserController extends BaseController {

    private String message;

    @Autowired
    private UserService userService;

    @PostMapping("detail")
    public User detail(@NotBlank(message = "{required}") @PathVariable String username) {
        return this.userService.findByName(username);
    }

    @PostMapping("list")
    @RequiresPermissions("user:view")
    public Map<String, Object> userList(RestRequest restRequest, User user) {
        return getDataTable(userService.findUserDetail(user, restRequest));
    }

    @PostMapping("post")
    @RequiresPermissions("user:add")
    public void addUser(@Valid User user) throws ServiceException {
        try {
            this.userService.createUser(user);
        } catch (Exception e) {
            message = "新增用户失败";
            log.info(message, e);
            throw new ServiceException(message);
        }
    }

    @PutMapping("update")
    @RequiresPermissions("user:update")
    public void updateUser(@Valid User user) throws ServiceException {
        try {
            this.userService.updateUser(user);
        } catch (Exception e) {
            message = "修改用户失败";
            log.info(message, e);
            throw new ServiceException(message);
        }
    }

    @DeleteMapping("delete")
    @RequiresPermissions("user:delete")
    public void deleteUsers(@NotBlank(message = "{required}") String userIds)
            throws ServiceException {
        try {
            String[] ids = userIds.split(StringPool.COMMA);
            this.userService.deleteUsers(ids);
        } catch (Exception e) {
            message = "删除用户失败";
            log.info(message, e);
            throw new ServiceException(message);
        }
    }

    @PutMapping("profile")
    public void updateProfile(@Valid User user) throws ServiceException {
        try {
            this.userService.updateProfile(user);
        } catch (Exception e) {
            message = "修改个人信息失败";
            log.info(message, e);
            throw new ServiceException(message);
        }
    }

    @PutMapping("avatar")
    public void updateAvatar(
            @NotBlank(message = "{required}") String username,
            @NotBlank(message = "{required}") String avatar)
            throws ServiceException {
        try {
            this.userService.updateAvatar(username, avatar);
        } catch (Exception e) {
            message = "修改头像失败";
            log.info(message, e);
            throw new ServiceException(message);
        }
    }

    @PostMapping("check/name")
    public boolean checkUserName(@NotBlank(message = "{required}") String username) {
        return this.userService.findByName(username) == null;
    }

    @PostMapping("check/password")
    public boolean checkPassword(
            @NotBlank(message = "{required}") String username,
            @NotBlank(message = "{required}") String password) {

        User user = userService.findByName(username);
        String salt = user.getSalt();
        String encryptPassword = ShaHashUtil.encrypt(salt, password);
        return StringUtils.equals(user.getPassword(), encryptPassword);
    }

    @PutMapping("password")
    public void updatePassword(
            @NotBlank(message = "{required}") String username,
            @NotBlank(message = "{required}") String password)
            throws ServiceException {
        try {
            userService.updatePassword(username, password);
        } catch (Exception e) {
            message = "修改密码失败";
            log.info(message, e);
            throw new ServiceException(message);
        }
    }

    @PutMapping("password/reset")
    @RequiresPermissions("user:reset")
    public void resetPassword(@NotBlank(message = "{required}") String usernames)
            throws ServiceException {
        try {
            String[] usernameArr = usernames.split(StringPool.COMMA);
            this.userService.resetPassword(usernameArr);
        } catch (Exception e) {
            message = "重置用户密码失败";
            log.info(message, e);
            throw new ServiceException(message);
        }
    }

}
