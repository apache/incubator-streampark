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

import com.streamxhub.streamx.common.util.DateUtils;
import com.streamxhub.streamx.console.base.domain.Constant;
import com.streamxhub.streamx.console.base.domain.ResponseCode;
import com.streamxhub.streamx.console.base.domain.RestRequest;
import com.streamxhub.streamx.console.base.domain.RestResponse;
import com.streamxhub.streamx.console.base.exception.ServiceException;
import com.streamxhub.streamx.console.base.util.SortUtils;
import com.streamxhub.streamx.console.base.util.WebUtils;
import com.streamxhub.streamx.console.system.authentication.JWTToken;
import com.streamxhub.streamx.console.system.authentication.JWTUtil;
import com.streamxhub.streamx.console.system.dao.AccessTokenMapper;
import com.streamxhub.streamx.console.system.entity.AccessToken;
import com.streamxhub.streamx.console.system.entity.User;
import com.streamxhub.streamx.console.system.service.AccessTokenService;
import com.streamxhub.streamx.console.system.service.UserService;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class AccessTokenServiceImpl extends ServiceImpl<AccessTokenMapper, AccessToken> implements AccessTokenService {

    @Autowired
    private UserService userService;
    @Autowired
    private AccessTokenMapper tokenMapper;

    @Override
    public RestResponse generateToken(String username, String expireTime, String description) throws ServiceException {
        User user = userService.lambdaQuery().eq(User::getUsername, username).one();
        if (Objects.isNull(user)) {
            return RestResponse.create().put("code", 0).message("user not available");
        }

        if (StringUtils.isEmpty(expireTime)) {
            expireTime = AccessToken.DEFAULT_EXPIRE_TIME;
        }
        String password = AccessToken.DEFAULT_PASSWORD;
        String token = WebUtils.encryptToken(JWTUtil.sign(username, password));
        JWTToken jwtToken = new JWTToken(token, expireTime);

        AccessToken accessToken = new AccessToken();
        accessToken.setToken(jwtToken.getToken());
        accessToken.setUsername(username);
        accessToken.setDescription(description);
        accessToken.setExpireTime(DateUtils.stringToDate(jwtToken.getExpireAt()));
        accessToken.setCreateTime(new Date());
        accessToken.setStatus(AccessToken.STATUS_ENABLE);

        this.save(accessToken);
        return RestResponse.create().data(accessToken);
    }

    @Override
    public boolean deleteToken(Long id) {
        boolean res = this.removeById(id);
        return res;
    }

    @Override
    public IPage<AccessToken> findAccessTokens(AccessToken tokenParam, RestRequest request) {
        Page<AccessToken> page = new Page<>();
        SortUtils.handlePageSort(request, page, "create_time", Constant.ORDER_DESC, false);
        this.baseMapper.page(page, tokenParam);
        List<AccessToken> records = page.getRecords();
        page.setRecords(records);
        return page;
    }

    @Override
    public boolean checkTokenEffective(String username, String token) {
        AccessToken res = tokenMapper.getTokenInfo(username, token);
        if (Objects.isNull(res) || AccessToken.STATUS_DISABLE.equals(res.getFinalTokenStatus())) {
            return false;
        }
        return true;
    }

    @Override
    public RestResponse updateTokenStatus(Integer status, Long tokenId) throws ServiceException {
        AccessToken tokenInfo = tokenMapper.getTokenInfoById(tokenId);
        if (Objects.isNull(tokenInfo)) {
            return RestResponse.fail("accessToken could not be found!", ResponseCode.CODE_ACCESS_TOKEN_LOCKED);
        }

        if (User.STATUS_LOCK.equals(tokenInfo.getUserStatus())) {
            return RestResponse.fail("user status is locked, could not operate this accessToken!", ResponseCode.CODE_ACCESS_TOKEN_LOCKED);
        }

        AccessToken updateObj = new AccessToken();
        updateObj.setStatus(status);
        updateObj.setId(tokenId);
        return RestResponse.success(this.updateById(updateObj));
    }
}
