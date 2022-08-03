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

package com.streamxhub.streamx.console.system.service.impl;

import com.streamxhub.streamx.console.core.service.CommonService;
import com.streamxhub.streamx.console.system.dao.UserRoleMapper;
import com.streamxhub.streamx.console.system.entity.UserRole;
import com.streamxhub.streamx.console.system.service.UserRoleService;
import com.streamxhub.streamx.console.system.service.UserService;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class UserRoleServiceImpl extends ServiceImpl<UserRoleMapper, UserRole>
    implements UserRoleService {

    @Autowired
    private CommonService commonService;

    @Autowired
    private UserService userService;

    @Override
    @Transactional
    public void deleteUserRolesByRoleId(String[] roleIds) {
        Arrays.stream(roleIds).forEach(id -> baseMapper.deleteByRoleId(Long.valueOf(id)));
    }

    @Override
    @Transactional
    public void deleteUserRolesByUserId(String[] userIds) {
        Arrays.stream(userIds).forEach(id -> baseMapper.deleteByUserId(Long.valueOf(id)));
    }

    @Override
    public List<String> findUserIdsByRoleId(String[] roleIds) {
        List<UserRole> list =
            baseMapper.selectList(
                new LambdaQueryWrapper<UserRole>().in(UserRole::getRoleId, (Object) roleIds));
        return list.stream()
            .map(userRole -> String.valueOf(userRole.getUserId()))
            .collect(Collectors.toList());
    }

    @Override
    public List<Long> listRoleIdListByUserId(Long userId) {
        QueryWrapper<UserRole> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(true, "user_id", userId);
        return list(queryWrapper).stream().map(userRole -> userRole.getRoleId()).collect(Collectors.toList());
    }

    @Override
    public Boolean isManageTeam(String username) {
        Set<String> permissions = userService.getPermissions(username);
        return permissions.contains("team:view");
    }

    @Override
    public Boolean isManageTeam(){
        return isManageTeam(commonService.getCurrentUser().getUsername());
    }

    @Override
    public List<Long> getRoleIdListByCurrentUser() {
        Long userId = commonService.getCurrentUser().getUserId();
        return getRoleIdListByUserId(userId);
    }

    @Override
    public List<Long> getRoleIdListByUserId(Long userId) {
        return baseMapper.selectRoleIdList(userId);
    }
}
