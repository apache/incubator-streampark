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
import com.streamxhub.streamx.console.system.dao.TeamUserMapper;
import com.streamxhub.streamx.console.system.entity.TeamUser;
import com.streamxhub.streamx.console.system.service.TeamUserService;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * @author benjobs
 */
@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class TeamUserServiceImpl extends ServiceImpl<TeamUserMapper, TeamUser> implements TeamUserService {

    @Autowired
    private CommonService commonService;

    @Override
    public List<Long> getTeamIdList() {
        Long userId = commonService.getCurrentUser().getUserId();
        return getTeamIdList(userId);
    }

    @Override
    public List<Long> getTeamIdList(Long userId) {
        return baseMapper.selectTeamIdList(userId);
    }

    @Override
    @Transactional
    public void deleteTeamUsersByUserId(String[] userIds) {
        Arrays.stream(userIds).forEach(id -> baseMapper.deleteByUserId(Long.valueOf(id)));
    }

    @Override
    public Long getCountByTeam(Long teamId) {
        return baseMapper.getCountByTeam(teamId);
    }

}
