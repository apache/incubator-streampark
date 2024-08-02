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
import org.apache.streampark.console.base.domain.ResponseCode;
import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.base.domain.RestResponse;
import org.apache.streampark.console.base.mybatis.pager.MybatisPager;
import org.apache.streampark.console.core.enums.AuthenticationType;
import org.apache.streampark.console.system.authentication.JWTUtil;
import org.apache.streampark.console.system.entity.AccessToken;
import org.apache.streampark.console.system.entity.User;
import org.apache.streampark.console.system.mapper.AccessTokenMapper;
import org.apache.streampark.console.system.service.AccessTokenService;
import org.apache.streampark.console.system.service.UserService;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.TimeZone;

@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class AccessTokenServiceImpl extends ServiceImpl<AccessTokenMapper, AccessToken>
    implements AccessTokenService {

  @Autowired private UserService userService;

  private final Long OPENAPI_TTL =
      DateUtils.getTime("9999-01-01 00:00:00", DateUtils.fullFormat(), TimeZone.getDefault());

  @Override
  public RestResponse create(Long userId, String description) throws Exception {
    User user = userService.getById(userId);
    if (user == null) {
      return RestResponse.success().put("code", 0).message("user not available");
    }

    String token = JWTUtil.sign(user, AuthenticationType.OPENAPI, OPENAPI_TTL);
    AccessToken accessToken = new AccessToken();
    accessToken.setToken(token);
    accessToken.setUserId(user.getUserId());
    accessToken.setDescription(description);

    Date date = new Date();
    accessToken.setCreateTime(date);
    accessToken.setModifyTime(date);
    accessToken.setStatus(AccessToken.STATUS_ENABLE);

    this.save(accessToken);
    return RestResponse.success().data(accessToken);
  }

  @Override
  public boolean deleteToken(Long id) {
    return this.removeById(id);
  }

  @Override
  public IPage<AccessToken> page(AccessToken tokenParam, RestRequest request) {
    Page<AccessToken> page = MybatisPager.getPage(request);
    this.baseMapper.page(page, tokenParam);
    List<AccessToken> records = page.getRecords();
    page.setRecords(records);
    return page;
  }

  @Override
  public RestResponse toggleToken(Long tokenId) {
    AccessToken tokenInfo = baseMapper.getById(tokenId);
    if (tokenInfo == null) {
      return RestResponse.fail("accessToken could not be found!", ResponseCode.CODE_FAIL_ALERT);
    }

    if (User.STATUS_LOCK.equals(tokenInfo.getUserStatus())) {
      return RestResponse.fail(
          "user status is locked, could not operate this accessToken!",
          ResponseCode.CODE_FAIL_ALERT);
    }

    Integer status =
        tokenInfo.getStatus().equals(AccessToken.STATUS_ENABLE)
            ? AccessToken.STATUS_DISABLE
            : AccessToken.STATUS_ENABLE;

    AccessToken updateObj = new AccessToken();
    updateObj.setStatus(status);
    updateObj.setId(tokenId);
    updateObj.setModifyTime(new Date());
    return RestResponse.success(this.updateById(updateObj));
  }

  @Override
  public AccessToken getByUserId(Long userId) {
    return baseMapper.getByUserId(userId);
  }
}
