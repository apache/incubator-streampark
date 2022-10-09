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
import org.apache.streampark.console.base.domain.Constant;
import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.core.enums.UserType;
import org.apache.streampark.console.system.entity.Team;
import org.apache.streampark.console.system.entity.User;
import org.apache.streampark.console.system.mapper.TeamMapper;
import org.apache.streampark.console.system.service.TeamService;
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

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team> implements TeamService {

    @Autowired
    private UserService userService;

    @Override
    public IPage<Team> findTeams(Team team, RestRequest request) {
        Page<Team> page = new Page<>();
        page.setCurrent(request.getPageNum());
        page.setSize(request.getPageSize());
        return this.baseMapper.findTeam(page, team);
    }

    @Override
    public Team findByName(String teamName) {
        return baseMapper.selectOne(new LambdaQueryWrapper<Team>().eq(Team::getTeamName, teamName));
    }

    @Override
    public void createTeam(Team team) {
        Team existedTeam = findByName(team.getTeamName());
        if (existedTeam != null) {
            throw new IllegalArgumentException(String.format("Team name [%s] was found, please rename and try again.", team.getTeamName()));
        }
        team.setId(null);
        team.setCreateTime(new Date());
        team.setModifyTime(team.getCreateTime());
        this.save(team);
    }

    @Override
    public void deleteTeam(Team team) {
        // TODO 查询 app 和 project，如果还有 app 或 project 没有删除，则 team 不能被删除。
        this.removeById(team);
    }

    @Override
    public void updateTeam(Team team) {
        Team oldTeam = Optional.ofNullable(this.getById(team))
            .orElseThrow(() -> new IllegalArgumentException(String.format("Team id [id=%s] not found", team.getId())));
        AssertUtils.isTrue(oldTeam.getTeamName().equals(team.getTeamName()), "Team name cannot be changed.");
        oldTeam.setDescription(team.getDescription());
        oldTeam.setModifyTime(new Date());
        updateById(oldTeam);
    }

    @Override
    public List<Team> findUserTeams(Long userId) {
        User user = Optional.ofNullable(userService.getById(userId))
            .orElseThrow(() -> new IllegalArgumentException(String.format("The userId [%s] not found", userId)));
        // Admin has the permission for all teams.
        if (UserType.ADMIN.equals(user.getUserType())) {
            return this.list();
        }
        return baseMapper.findUserTeams(userId);
    }

    @Override
    public Team getDefaultTeam() {
        return getById(Constant.DEFAULT_TEAM_ID);
    }
}
