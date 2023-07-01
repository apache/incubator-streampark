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

import org.apache.streampark.common.util.DateUtils;
import org.apache.streampark.common.util.Utils;
import org.apache.streampark.console.base.domain.ResponseCode;
import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.base.domain.RestResponse;
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.base.properties.ShiroProperties;
import org.apache.streampark.console.base.util.ShaHashUtils;
import org.apache.streampark.console.base.util.WebUtils;
import org.apache.streampark.console.core.service.ApplicationService;
import org.apache.streampark.console.core.service.ResourceService;
import org.apache.streampark.console.system.authentication.JWTToken;
import org.apache.streampark.console.system.authentication.JWTUtil;
import org.apache.streampark.console.system.entity.Team;
import org.apache.streampark.console.system.entity.User;
import org.apache.streampark.console.system.mapper.UserMapper;
import org.apache.streampark.console.system.service.MemberService;
import org.apache.streampark.console.system.service.MenuService;
import org.apache.streampark.console.system.service.UserService;

import org.apache.commons.collections.CollectionUtils;
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

  @Autowired private MemberService memberService;

  @Autowired private MenuService menuService;

  @Autowired private ApplicationService applicationService;

  @Autowired private ResourceService resourceService;

  @Autowired private ShiroProperties shiroProperties;

  @Override
  public User findByName(String username) {
    LambdaQueryWrapper<User> queryWrapper =
        new LambdaQueryWrapper<User>().eq(User::getUsername, username);
    return baseMapper.selectOne(queryWrapper);
  }

  @Override
  public IPage<User> findUserDetail(User user, RestRequest request) {
    Page<User> page = new Page<>();
    page.setCurrent(request.getPageNum());
    page.setSize(request.getPageSize());
    IPage<User> resPage = this.baseMapper.findUserDetail(page, user);

    Utils.notNull(resPage);
    if (resPage.getTotal() == 0) {
      resPage.setRecords(Collections.emptyList());
    }
    return resPage;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void updateLoginTime(String username) {
    User user = new User();
    user.setLastLoginTime(new Date());
    LambdaQueryWrapper<User> queryWrapper =
        new LambdaQueryWrapper<User>().eq(User::getUsername, username);
    this.baseMapper.update(user, queryWrapper);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void createUser(User user) {
    user.setCreateTime(new Date());
    if (StringUtils.isNoneBlank(user.getPassword())) {
      String salt = ShaHashUtils.getRandomSalt();
      String password = ShaHashUtils.encrypt(salt, user.getPassword());
      user.setSalt(salt);
      user.setPassword(password);
    }
    save(user);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public RestResponse updateUser(User user) {
    User existsUser = getById(user.getUserId());
    user.setPassword(null);
    user.setModifyTime(new Date());
    if (needTransferResource(existsUser, user)) {
      return RestResponse.success(Collections.singletonMap("needTransferResource", true));
    }
    updateById(user);
    return RestResponse.success();
  }

  private boolean needTransferResource(User existsUser, User user) {
    if (User.STATUS_LOCK.equals(existsUser.getStatus())
        || User.STATUS_VALID.equals(user.getStatus())) {
      return false;
    }
    return applicationService.existsByUserId(user.getUserId())
        || resourceService.existsByUserId(user.getUserId());
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void updatePassword(User userParam) {
    User user = getById(userParam.getUserId());
    ApiAlertException.throwIfNull(user, "User is null. Update password failed.");

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
  @Transactional(rollbackFor = Exception.class)
  public void updateSaltPassword(User userParam) {
    User user = getById(userParam.getUserId());
    ApiAlertException.throwIfNull(user, "User is null. Update password failed.");
    user.setSalt(userParam.getSalt());
    user.setPassword(userParam.getPassword());
    this.baseMapper.updateById(user);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public String resetPassword(String username) {
    User user = new User();
    String salt = ShaHashUtils.getRandomSalt();
    String newPassword = ShaHashUtils.getRandomSalt(User.DEFAULT_PASSWORD_LENGTH);
    String password = ShaHashUtils.encrypt(salt, newPassword);
    user.setSalt(salt);
    user.setPassword(password);
    LambdaQueryWrapper<User> queryWrapper =
        new LambdaQueryWrapper<User>().eq(User::getUsername, username);
    this.baseMapper.update(user, queryWrapper);
    return newPassword;
  }

  @Override
  public Set<String> getPermissions(Long userId, @Nullable Long teamId) {
    List<String> userPermissions = this.menuService.findUserPermissions(userId, teamId);
    return new HashSet<>(userPermissions);
  }

  @Override
  public List<User> getNoTokenUser() {
    List<User> users = this.baseMapper.getNoTokenUser();
    if (!users.isEmpty()) {
      users.forEach(User::dataMasking);
    }
    return users;
  }

  @Override
  public void setLastTeam(Long teamId, Long userId) {
    User user = getById(userId);
    Utils.notNull(user);
    user.setLastTeamId(teamId);
    this.baseMapper.updateById(user);
  }

  @Override
  public void clearLastTeam(Long userId, Long teamId) {
    User user = getById(userId);
    Utils.notNull(user);
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
  public void fillInTeam(User user) {
    if (user.getLastTeamId() == null) {
      List<Team> teams = memberService.findUserTeams(user.getUserId());
      if (CollectionUtils.isEmpty(teams)) {
        throw new ApiAlertException(
            "The current user not belong to any team, please contact the administrator!");
      } else if (teams.size() == 1) {
        Team team = teams.get(0);
        user.setLastTeamId(team.getId());
        this.baseMapper.updateById(user);
      }
    }
  }

  @Override
  public List<User> findByAppOwner(Long teamId) {
    return baseMapper.findByAppOwner(teamId);
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
    Set<String> permissions = this.getPermissions(user.getUserId(), teamId);
    userInfo.put("permissions", permissions);

    return userInfo;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void transferResource(Long userId, Long targetUserId) {
    applicationService.changeOwnership(userId, targetUserId);
    resourceService.changeOwnership(userId, targetUserId);
  }

  @Override
  public Map<String, Object> getLoginUserInfo(User user) {

    if (user == null) {
      return RestResponse.success().put("code", 0);
    }

    if (User.STATUS_LOCK.equals(user.getStatus())) {
      return RestResponse.success().put("code", 1);
    }

    // set team
    fillInTeam(user);

    // no team.
    if (user.getLastTeamId() == null) {
      return RestResponse.success().data(user.getUserId()).put("code", ResponseCode.CODE_FORBIDDEN);
    }

    updateLoginTime(user.getUsername());
    String token = WebUtils.encryptToken(JWTUtil.sign(user.getUserId(), user.getUsername()));
    LocalDateTime expireTime = LocalDateTime.now().plusSeconds(shiroProperties.getJwtTimeOut());
    String expireTimeStr = DateUtils.formatFullTime(expireTime);
    JWTToken jwtToken = new JWTToken(token, expireTimeStr);
    String userId = RandomStringUtils.randomAlphanumeric(20);
    user.setId(userId);
    Map<String, Object> userInfo = generateFrontendUserInfo(user, user.getLastTeamId(), jwtToken);
    return new RestResponse().data(userInfo);
  }
}
