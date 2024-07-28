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
import org.apache.streampark.common.util.DateUtils;
import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.base.domain.Result;
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.base.mybatis.pager.MybatisPager;
import org.apache.streampark.console.base.util.ShaHashUtils;
import org.apache.streampark.console.base.util.WebUtils;
import org.apache.streampark.console.core.enums.AuthenticationType;
import org.apache.streampark.console.core.enums.LoginTypeEnum;
import org.apache.streampark.console.core.service.ResourceService;
import org.apache.streampark.console.core.service.application.ApplicationInfoService;
import org.apache.streampark.console.core.service.application.ApplicationManageService;
import org.apache.streampark.console.system.authentication.JWTToken;
import org.apache.streampark.console.system.authentication.JWTUtil;
import org.apache.streampark.console.system.entity.User;
import org.apache.streampark.console.system.mapper.UserMapper;
import org.apache.streampark.console.system.service.MemberService;
import org.apache.streampark.console.system.service.MenuService;
import org.apache.streampark.console.system.service.TeamService;
import org.apache.streampark.console.system.service.UserService;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private MemberService memberService;

    @Autowired
    private MenuService menuService;

    @Autowired
    private ApplicationManageService applicationManageService;

    @Autowired
    private ApplicationInfoService applicationInfoService;

    @Autowired
    private ResourceService resourceService;

    @Autowired
    private TeamService teamService;

    @Override
    public User getByUsername(String username) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<User>().eq(User::getUsername, username);
        return baseMapper.selectOne(queryWrapper);
    }

    @Override
    public IPage<User> getPage(User user, RestRequest request) {
        Page<User> page = MybatisPager.getPage(request);
        IPage<User> resPage = this.baseMapper.selectPage(page, user);
        AssertUtils.notNull(resPage);
        if (resPage.getTotal() == 0) {
            resPage.setRecords(Collections.emptyList());
        }
        return resPage;
    }

    @Override
    public void updateLoginTime(String username) {
        User user = new User();
        user.setLastLoginTime(new Date());
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<User>().eq(User::getUsername, username);
        this.baseMapper.update(user, queryWrapper);
    }

    @Override
    public void createUser(User user) {
        if (StringUtils.isNoneBlank(user.getPassword())) {
            String salt = ShaHashUtils.getRandomSalt();
            String password = ShaHashUtils.encrypt(salt, user.getPassword());
            user.setSalt(salt);
            // default team
            user.setLastTeamId(teamService.getSysDefaultTeam().getId());
            user.setPassword(password);
        }
        save(user);
    }

    @Override
    public Result<Map<String, Boolean>> updateUser(User user) {
        User existsUser = getById(user.getUserId());
        user.setLoginType(null);
        user.setPassword(null);
        if (needTransferResource(existsUser, user)) {
            return Result.success(Collections.singletonMap("needTransferResource", true));
        }
        updateById(user);
        return Result.success(Collections.emptyMap());
    }

    private boolean needTransferResource(User existsUser, User user) {
        if (User.STATUS_LOCK.equals(existsUser.getStatus())
            || User.STATUS_VALID.equals(user.getStatus())) {
            return false;
        }
        return applicationInfoService.existsByUserId(user.getUserId())
            || resourceService.existsByUserId(user.getUserId());
    }

    @Override
    public void updatePassword(User userParam) {
        User user = getById(userParam.getUserId());
        ApiAlertException.throwIfNull(user, "User is null. Update password failed.");
        ApiAlertException.throwIfFalse(
            user.getLoginType() == LoginTypeEnum.PASSWORD,
            "Can only update password for user who sign in with PASSWORD");

        String saltPassword = ShaHashUtils.encrypt(user.getSalt(), userParam.getOldPassword());
        ApiAlertException.throwIfFalse(
            StringUtils.equals(user.getPassword(), saltPassword),
            "Old password error. Update password failed.");

        String salt = ShaHashUtils.getRandomSalt();
        String password = ShaHashUtils.encrypt(salt, userParam.getPassword());
        user.setSalt(salt);
        user.setPassword(password);
        this.baseMapper.updateById(user);
    }

    @Override
    public String resetPassword(String username) {
        User user = new User();
        String salt = ShaHashUtils.getRandomSalt();
        String newPassword = ShaHashUtils.getRandomSalt(User.DEFAULT_PASSWORD_LENGTH);
        String password = ShaHashUtils.encrypt(salt, newPassword);
        user.setSalt(salt);
        user.setPassword(password);
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<User>().eq(User::getUsername, username);
        this.baseMapper.update(user, queryWrapper);
        return newPassword;
    }

    @Override
    public Set<String> listPermissions(Long userId, @Nullable Long teamId) {
        List<String> userPermissions = this.menuService.listPermissions(userId, teamId);
        return new HashSet<>(userPermissions);
    }

    @Override
    public List<User> listNoTokenUser() {
        List<User> users = this.baseMapper.selectNoTokenUsers();
        if (!users.isEmpty()) {
            users.forEach(User::dataMasking);
        }
        return users;
    }

    @Override
    public void setLastTeam(Long teamId, Long userId) {
        User user = getById(userId);
        AssertUtils.notNull(user);
        user.setLastTeamId(teamId);
        this.baseMapper.updateById(user);
    }

    @Override
    public void clearLastTeam(Long userId, Long teamId) {
        User user = getById(userId);
        AssertUtils.notNull(user);
        if (!teamId.equals(user.getLastTeamId())) {
            return;
        }
        this.baseMapper.clearLastTeamByUserId(userId);
    }

    @Override
    public void clearLastTeam(Long teamId) {
        this.baseMapper.clearLastTeamByTeamId(teamId);
    }

    @Override
    public List<User> listByTeamId(Long teamId) {
        return baseMapper.selectUsersByAppOwner(teamId);
    }

    @Override
    public void transferResource(Long userId, Long targetUserId) {
        applicationManageService.changeOwnership(userId, targetUserId);
        resourceService.changeOwnership(userId, targetUserId);
    }

    @Override
    public Result<Map<String, Object>> getLoginUserInfo(User user) {
        if (user == null) {
            return Result.fail(null, null, 0);
        }

        if (User.STATUS_LOCK.equals(user.getStatus())) {
            return Result.fail(null, null, 0);
        }

        updateLoginTime(user.getUsername());
        String token = WebUtils.encryptToken(
            JWTUtil.sign(
                user.getUserId(), user.getUsername(), user.getSalt(), AuthenticationType.SIGN));
        LocalDateTime expireTime = LocalDateTime.now().plusSeconds(JWTUtil.getTTLOfSecond());
        String expireTimeStr = DateUtils.formatFullTime(expireTime);
        JWTToken jwtToken = new JWTToken(token, expireTimeStr, AuthenticationType.SIGN.get());
        String userId = RandomStringUtils.randomAlphanumeric(20);
        user.setId(userId);
        Map<String, Object> userInfo = generateFrontendUserInfo(user, user.getLastTeamId(), jwtToken);
        return Result.success(userInfo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long userId) {
        removeById(userId);
        this.memberService.removeByUserId(userId);
    }

    /**
     * generate user info, contains: 1.token, 2.vue router, 3.role, 4.permission, 5.personalized
     * config info of frontend
     *
     * @param user user
     * @return UserInfo
     */
    @Override
    public Map<String, Object> generateFrontendUserInfo(User user, Long teamId, JWTToken token) {
        Map<String, Object> userInfo = new HashMap<>(8);

        // 1) token & expire
        if (token != null) {
            userInfo.put("token", token.getToken());
            userInfo.put("expire", token.getExpireAt());
        }

        // 2) user
        user.dataMasking();
        userInfo.put("user", user);

        // 3) permissions
        Set<String> permissions = this.listPermissions(user.getUserId(), teamId);
        userInfo.put("permissions", permissions);

        return userInfo;
    }
}
