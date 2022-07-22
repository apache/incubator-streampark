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

import com.streamxhub.streamx.console.base.domain.RestRequest;
import com.streamxhub.streamx.console.base.util.ShaHashUtils;
import com.streamxhub.streamx.console.core.service.CommonService;
import com.streamxhub.streamx.console.system.dao.UserMapper;
import com.streamxhub.streamx.console.system.entity.Menu;
import com.streamxhub.streamx.console.system.entity.Role;
import com.streamxhub.streamx.console.system.entity.Team;
import com.streamxhub.streamx.console.system.entity.TeamUser;
import com.streamxhub.streamx.console.system.entity.User;
import com.streamxhub.streamx.console.system.entity.UserRole;
import com.streamxhub.streamx.console.system.service.MenuService;
import com.streamxhub.streamx.console.system.service.RoleService;
import com.streamxhub.streamx.console.system.service.TeamService;
import com.streamxhub.streamx.console.system.service.TeamUserService;
import com.streamxhub.streamx.console.system.service.UserRoleService;
import com.streamxhub.streamx.console.system.service.UserService;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author benjobs
 */
@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private UserRoleService userRoleService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private MenuService menuService;

    @Autowired
    private CommonService commonService;

    @Autowired
    private TeamService teamService;

    @Autowired
    private TeamUserService teamUserService;

    @Override
    public User findByName(String username) {
        return baseMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    @Override
    public IPage<User> findUserDetail(User user, RestRequest request) {
        Page<User> page = new Page<>();
        page.setCurrent(request.getPageNum());
        page.setSize(request.getPageSize());

        String nowUser = commonService.getCurrentUser().getUsername();

        // 如果用户有选择某个组，则只查询该组下的员工
        if (StringUtils.isNotEmpty(user.getTeamId())) {
            List<Long> teamIdList = new ArrayList<>();
            teamIdList.add(Long.valueOf(user.getTeamId()));
            user.setTeamIdList(teamIdList);
        } else if (!userRoleService.isManageTeam(nowUser)) {
            // 如果用户没有选择组，则只查询用户拥有权限的组
            List<Long> teamIdList = teamUserService.getTeamIdList();
            user.setTeamIdList(teamIdList);
        }

        IPage<User> resPage = this.baseMapper.findUserDetail(page, user);

        if (resPage != null && !resPage.getRecords().isEmpty()) {
            List<User> users = resPage.getRecords();
            users.forEach(u -> {
                if (u.getUsername().equals(nowUser)) {
                    u.setIsNow(true);
                }

                List<Role> roleList = roleService.findUserRole(u.getUsername());
                String roleIds = roleList.stream().map((iter) -> iter.getRoleId().toString()).collect(Collectors.joining(","));
                String roleNames = roleList.stream().map(Role::getRoleName).collect(Collectors.joining(","));
                u.setRoleId(roleIds);
                u.setRoleName(roleNames);

                if (userRoleService.isManageTeam(u.getUsername())) {
                    u.setTeamId("0");
                    u.setTeamName("All Team");
                    return;
                }

                List<Team> teamUserList = teamService.findTeamByUser(u.getUsername());
                String teamIds = teamUserList.stream().map((iter) -> iter.getTeamId().toString()).collect(Collectors.joining(","));
                String teamNames = teamUserList.stream().map(Team::getTeamName).collect(Collectors.joining(","));
                u.setTeamId(teamIds);
                u.setTeamName(teamNames);

            });
            resPage.setRecords(users);
        }
        assert resPage != null;
        if (resPage.getTotal() == 0) {
            resPage.setRecords(Collections.emptyList());
        }
        return resPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLoginTime(String username) throws Exception {
        User user = new User();
        user.setLastLoginTime(new Date());
        this.baseMapper.update(user, new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createUser(User user) throws Exception {
        // 创建用户
        user.setCreateTime(new Date());
        user.setAvatar(User.DEFAULT_AVATAR);
        String salt = ShaHashUtils.getRandomSalt(26);
        String password = ShaHashUtils.encrypt(salt, user.getPassword());
        user.setSalt(salt);
        user.setPassword(password);
        save(user);
        // 保存用户角色
        String[] roles = user.getRoleId().split(StringPool.COMMA);
        setUserRoles(user, roles);

        if (null != user.getTeamId()) {
            // 保存团队用户
            String[] teams = user.getTeamId().split(StringPool.COMMA);
            setUserTeams(user, teams);
        }

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(User user) throws Exception {
        // 更新用户
        user.setPassword(null);
        user.setModifyTime(new Date());
        updateById(user);

        userRoleService.deleteUserRolesByUserId(new String[]{user.getUserId().toString()});
        String[] roles = user.getRoleId().split(StringPool.COMMA);
        setUserRoles(user, roles);

        teamUserService.deleteTeamUsersByUserId(new String[]{user.getUserId().toString()});
        if (null != user.getTeamId()) {
            String[] teams = user.getTeamId().split(StringPool.COMMA);
            setUserTeams(user, teams);
        }

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUsers(String[] userIds) throws Exception {
        List<String> list = Arrays.asList(userIds);
        removeByIds(list);
        // 删除用户角色
        this.userRoleService.deleteUserRolesByUserId(userIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(User user) throws Exception {
        updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAvatar(String username, String avatar) throws Exception {
        User user = new User();
        user.setAvatar(avatar);
        this.baseMapper.update(user, new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(String username, String password) throws Exception {
        User user = new User();
        String salt = ShaHashUtils.getRandomSalt(26);
        password = ShaHashUtils.encrypt(salt, password);
        user.setSalt(salt);
        user.setPassword(password);
        this.baseMapper.update(user, new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(String[] usernames) throws Exception {
        for (String username : usernames) {
            User user = new User();
            String salt = ShaHashUtils.getRandomSalt(26);
            String password = ShaHashUtils.encrypt(salt, User.DEFAULT_PASSWORD);
            user.setSalt(salt);
            user.setPassword(password);
            this.baseMapper.update(user, new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        }
    }

    /**
     * 通过用户名获取用户权限集合
     *
     * @param username 用户名
     * @return 权限集合
     */
    @Override
    public Set<String> getPermissions(String username) {
        List<Menu> permissionList = this.menuService.findUserPermissions(username);
        return permissionList.stream().map(Menu::getPerms).collect(Collectors.toSet());
    }

    @Override
    public List<User> getNoTokenUser() {
        List<User> users = this.baseMapper.getNoTokenUser();
        if (!users.isEmpty()) {
            users.forEach(u -> {
                u.setPassword(null);
                u.setSalt(null);
                u.setRoleId(null);
                u.setMobile(null);
            });
        }
        return users;
    }

    private void setUserRoles(User user, String[] roles) {
        Arrays.stream(roles).forEach(roleId -> {
            UserRole ur = new UserRole();
            ur.setUserId(user.getUserId());
            ur.setRoleId(Long.valueOf(roleId));
            this.userRoleService.save(ur);
        });
    }

    private void setUserTeams(User user, String[] teams) {
        Arrays.stream(teams).forEach(teamId -> {
            TeamUser teamUser = new TeamUser();
            teamUser.setUserId(user.getUserId());
            teamUser.setTeamId(Long.valueOf(teamId));
            teamUser.setCreateTime(new Date());
            this.teamUserService.save(teamUser);
        });
    }
}
