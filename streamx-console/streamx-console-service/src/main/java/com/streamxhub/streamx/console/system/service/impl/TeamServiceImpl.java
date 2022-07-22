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
import com.streamxhub.streamx.console.core.service.ApplicationService;
import com.streamxhub.streamx.console.core.service.CommonService;
import com.streamxhub.streamx.console.core.service.ProjectService;
import com.streamxhub.streamx.console.system.dao.TeamMapper;
import com.streamxhub.streamx.console.system.entity.Team;
import com.streamxhub.streamx.console.system.entity.User;
import com.streamxhub.streamx.console.system.service.TeamService;
import com.streamxhub.streamx.console.system.service.TeamUserService;
import com.streamxhub.streamx.console.system.service.UserRoleService;
import com.streamxhub.streamx.console.system.service.UserService;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

/**
 * @author benjobs
 */
@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team> implements TeamService {

    @Autowired
    private CommonService commonService;

    @Autowired
    private TeamUserService teamUserService;

    @Autowired
    private UserRoleService userRoleService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private UserService userService;

    @Override
    public List<Team> findTeamByUser(String username) {
        IPage<Team> teamsByUser = findTeamsByUser(username, new Team(), null);
        return teamsByUser.getRecords();
    }

    @Override
    public String deleteTeamBeforeCheck(Long teamId) {
        Long userCount = teamUserService.getCountByTeam(teamId);
        if (userCount > 0) {
            return "团队中存在用户，不能删除";
        }
        Long projectCount = projectService.getCountByTeam(teamId);
        if (projectCount > 0) {
            return "团队中存在项目，不能删除";
        }
        Long applicationCount = applicationService.getCountByTeam(teamId);
        if (applicationCount > 0) {
            return "团队中存在任务，不能删除";
        }
        removeById(teamId);
        return "success";
    }

    @Override
    public IPage<Team> findTeamsByNowUser(Team team, RestRequest request) {
        return findTeamsByUser(commonService.getCurrentUser().getUsername(), team, request);
    }

    @Override
    public IPage<Team> findTeamsByUser(String username, Team team, RestRequest request) {

        User user = userService.findByName(username);
        Long userId = user.getUserId();
        if (!userRoleService.isManageTeam(username)) {
            List<Long> teamIdList = teamUserService.getTeamIdList(userId);
            team.setTeamIdList(teamIdList);
        }
        Page<User> page = new Page<>();
        if (null != request) {
            page.setCurrent(request.getPageNum());
            page.setSize(request.getPageSize());
        } else {
            page.setSize(9999);
        }
        return this.baseMapper.findTeamList(page, team);
    }

    @Override
    public IPage<Team> findTeams(Team team, RestRequest request) {
        Page<User> page = new Page<>();
        page.setCurrent(request.getPageNum());
        page.setSize(request.getPageSize());
        return this.baseMapper.findTeamList(page, team);
    }

    @Override
    public void createTeam(Team team) {
        // 创建团队
        team.setCreateTime(new Date());
        save(team);
    }

    @Override
    public Team findByName(String teamName) {
        return baseMapper.selectOne(new LambdaQueryWrapper<Team>().eq(Team::getTeamName, teamName));
    }

    @Override
    public Team findByCode(String teamCode) {
        return baseMapper.selectOne(new LambdaQueryWrapper<Team>().eq(Team::getTeamCode, teamCode));
    }

}
