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

package org.apache.streampark.console.core.service.alert.impl;

import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.base.exception.AlertException;
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.base.exception.PermissionDeniedException;
import org.apache.streampark.console.base.mybatis.pager.MybatisPager;
import org.apache.streampark.console.core.bean.AlertConfigWithParams;
import org.apache.streampark.console.core.entity.AlertConfig;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.enums.UserType;
import org.apache.streampark.console.core.mapper.AlertConfigMapper;
import org.apache.streampark.console.core.service.ApplicationService;
import org.apache.streampark.console.core.service.ServiceHelper;
import org.apache.streampark.console.core.service.alert.AlertConfigService;
import org.apache.streampark.console.system.entity.User;

import org.apache.commons.collections.CollectionUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class AlertConfigServiceImpl extends ServiceImpl<AlertConfigMapper, AlertConfig>
    implements AlertConfigService {

  @Autowired private ApplicationService applicationService;

  @Autowired private ServiceHelper serviceHelper;

  /**
   * Verify that the currently logged-in user is either the platform administrator or the same user
   * as {@code targetUserId}. AlertConfig records are bound to an individual user (t_alert_config
   * has no team_id column), so this is the closest equivalent of the team-ownership checks used
   * elsewhere in the codebase for team-scoped entities.
   *
   * @param targetUserId the real, persisted userId of the record being accessed (or the userId a
   *     query is being explicitly scoped to). {@code null} means "no explicit scope requested".
   * @return the currently logged-in user, for convenience.
   */
  private User checkOwnership(Long targetUserId) {
    User currentUser = serviceHelper.getLoginUser();
    ApiAlertException.throwIfNull(currentUser, "Permission denied, please login first.");
    if (currentUser.getUserType() == UserType.ADMIN) {
      return currentUser;
    }
    if (targetUserId != null && !currentUser.getUserId().equals(targetUserId)) {
      throw new PermissionDeniedException(
          "Permission denied, this alert config was not created by the current user.");
    }
    return currentUser;
  }

  @Override
  public IPage<AlertConfigWithParams> page(AlertConfigWithParams params, RestRequest request) {
    // Never allow an unscoped query to leak every user's alert configs: if the caller didn't
    // specify a userId, default to the current user's own id (unless the current user is the
    // platform administrator). If the caller DID specify a userId, verify it's their own (or
    // they're the administrator) before honoring it.
    Long userId = params.getUserId();
    if (userId == null) {
      User currentUser = checkOwnership(null);
      if (currentUser.getUserType() != UserType.ADMIN) {
        userId = currentUser.getUserId();
      }
    } else {
      checkOwnership(userId);
    }

    // build query conditions
    LambdaQueryWrapper<AlertConfig> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(userId != null, AlertConfig::getUserId, userId);

    Page<AlertConfig> page = MybatisPager.getPage(request);
    IPage<AlertConfig> resultPage = getBaseMapper().selectPage(page, wrapper);

    Page<AlertConfigWithParams> result = new Page<>();
    if (CollectionUtils.isNotEmpty(resultPage.getRecords())) {
      result.setRecords(
          resultPage.getRecords().stream()
              .map(AlertConfigWithParams::of)
              .collect(Collectors.toList()));
    }

    return result;
  }

  @Override
  public boolean exist(AlertConfig alertConfig) {
    AlertConfig confByName = this.baseMapper.getAlertConfByName(alertConfig);
    return confByName != null;
  }

  @Override
  public boolean deleteById(Long id) throws AlertException {
    AlertConfig persisted = getById(id);
    ApiAlertException.throwIfTrue(persisted == null, "Sorry, the alert config does not exist.");
    checkOwnership(persisted.getUserId());
    long count =
        applicationService.count(
            new LambdaQueryWrapper<Application>().eq(id != null, Application::getAlertId, id));
    if (count > 0) {
      throw new AlertException(
          String.format(
              "AlertId:%d, this is bound by application. Please clear the configuration first",
              id));
    }
    return removeById(id);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public boolean createAlertConfig(AlertConfig alertConfig) {
    User currentUser = serviceHelper.getLoginUser();
    ApiAlertException.throwIfNull(currentUser, "Permission denied, please login first.");
    // Always attribute the new alert config to the current user, ignoring whatever userId the
    // client submitted, to prevent creating alert configs under someone else's identity.
    alertConfig.setUserId(currentUser.getUserId());
    return save(alertConfig);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public boolean updateAlertConfig(AlertConfig alertConfig) {
    ApiAlertException.throwIfTrue(
        alertConfig.getId() == null, "Sorry, the alert config id cannot be null.");
    AlertConfig persisted = getById(alertConfig.getId());
    ApiAlertException.throwIfTrue(persisted == null, "Sorry, the alert config does not exist.");
    checkOwnership(persisted.getUserId());
    // Never allow the client to reassign ownership of an existing alert config.
    alertConfig.setUserId(persisted.getUserId());
    return updateById(alertConfig);
  }

  @Override
  public AlertConfig getAlertConfig(Long id) {
    AlertConfig alertConfig = getById(id);
    if (alertConfig == null) {
      return null;
    }
    checkOwnership(alertConfig.getUserId());
    return alertConfig;
  }

  @Override
  public List<AlertConfig> listByCurrentUser() {
    User currentUser = serviceHelper.getLoginUser();
    ApiAlertException.throwIfNull(currentUser, "Permission denied, please login first.");
    if (currentUser.getUserType() == UserType.ADMIN) {
      return list();
    }
    return list(
        new LambdaQueryWrapper<AlertConfig>().eq(AlertConfig::getUserId, currentUser.getUserId()));
  }
}
