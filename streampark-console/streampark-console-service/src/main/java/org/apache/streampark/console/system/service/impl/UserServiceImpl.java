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

package org.apache.streampark.console.system.service.impl;

import org.apache.streampark.common.util.AssertUtils;
import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.base.util.ShaHashUtils;
import org.apache.streampark.console.system.entity.Menu;
import org.apache.streampark.console.system.entity.TeamMember;
import org.apache.streampark.console.system.entity.User;
import org.apache.streampark.console.system.mapper.UserMapper;
import org.apache.streampark.console.system.service.MenuService;
import org.apache.streampark.console.system.service.RoleService;
import org.apache.streampark.console.system.service.TeamMemberService;
import org.apache.streampark.console.system.service.UserService;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private TeamMemberService teamMemberService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private MenuService menuService;

    @Override
    public User findByName(String username) {
        return baseMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    @Override
    public IPage<User> findUserDetail(User user, RestRequest request) {
        Page<User> page = new Page<>();
        page.setCurrent(request.getPageNum());
        page.setSize(request.getPageSize());
        IPage<User> resPage = this.baseMapper.findUserDetail(page, user);

        AssertUtils.state(resPage != null);
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
        user.setCreateTime(new Date());
        user.setAvatar(User.DEFAULT_AVATAR);
        String salt = ShaHashUtils.getRandomSalt();
        String password = ShaHashUtils.encrypt(salt, user.getPassword());
        user.setSalt(salt);
        user.setPassword(password);
        save(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(User user) throws Exception {
        user.setPassword(null);
        user.setModifyTime(new Date());
        updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUsers(String[] userIds) throws Exception {
        List<String> list = Arrays.asList(userIds);
        removeByIds(list);
        this.teamMemberService.deleteUserRolesByUserId(userIds);
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
        String salt = ShaHashUtils.getRandomSalt();
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
            String salt = ShaHashUtils.getRandomSalt();
            String password = ShaHashUtils.encrypt(salt, User.DEFAULT_PASSWORD);
            user.setSalt(salt);
            user.setPassword(password);
            this.baseMapper.update(user, new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        }
    }

    /**
     * get user permissions by name
     *
     * @param username name
     * @return permissions
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
            });
        }
        return users;
    }

    private void setUserRoles(User user, String[] roles) {
        Arrays.stream(roles).forEach(roleId -> {
            TeamMember ur = new TeamMember();
            ur.setUserId(user.getUserId());
            ur.setRoleId(Long.valueOf(roleId));
            this.teamMemberService.save(ur);
        });
    }
}
