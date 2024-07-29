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

import org.apache.streampark.console.base.domain.Constant;
import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.base.mybatis.pager.MybatisPager;
import org.apache.streampark.console.core.enums.UserTypeEnum;
import org.apache.streampark.console.core.service.ProjectService;
import org.apache.streampark.console.core.service.VariableService;
import org.apache.streampark.console.core.service.application.ApplicationInfoService;
import org.apache.streampark.console.core.util.ServiceHelper;
import org.apache.streampark.console.system.entity.Team;
import org.apache.streampark.console.system.entity.User;
import org.apache.streampark.console.system.mapper.TeamMapper;
import org.apache.streampark.console.system.service.MemberService;
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

import java.util.List;
import java.util.Optional;

import static org.apache.streampark.console.base.enums.MessageStatus.APPLICATION;
import static org.apache.streampark.console.base.enums.MessageStatus.PROJECT;
import static org.apache.streampark.console.base.enums.MessageStatus.SYSTEM_TEAM_EXIST_MODULE_USE_DELETE_ERROR;
import static org.apache.streampark.console.base.enums.MessageStatus.SYSTEM_TEAM_NOT_EXIST;
import static org.apache.streampark.console.base.enums.MessageStatus.VARIABLES;

@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team> implements TeamService {

    @Autowired
    private UserService userService;

    @Autowired
    private ApplicationInfoService applicationInfoService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private MemberService memberService;

    @Autowired
    private VariableService variableService;

    @Override
    public IPage<Team> getPage(Team team, RestRequest request) {
        Page<Team> page = MybatisPager.getPage(request);
        return this.baseMapper.selectPage(page, team);
    }

    @Override
    public Team getByName(String teamName) {
        LambdaQueryWrapper<Team> queryWrapper = new LambdaQueryWrapper<Team>().eq(Team::getTeamName, teamName);
        return baseMapper.selectOne(queryWrapper);
    }

    @Override
    public void createTeam(Team team) {
        Team existedTeam = getByName(team.getTeamName());
        ApiAlertException.throwIfFalse(
            existedTeam == null,
            String.format(
                "Team name [%s] exists already. Create team failed. Please rename and try again.",
                team.getTeamName()));
        team.setId(null);
        this.save(team);
    }

    @Override
    public void removeById(Long teamId) {
        log.info("{} Proceed delete team[Id={}]", ServiceHelper.getLoginUser().getUsername(), teamId);
        Team team = this.getById(teamId);

        ApiAlertException.throwIfNull(team, SYSTEM_TEAM_NOT_EXIST);

        ApiAlertException.throwIfTrue(
            applicationInfoService.existsByTeamId(teamId),
            SYSTEM_TEAM_EXIST_MODULE_USE_DELETE_ERROR,
            team.getTeamName(),
            APPLICATION.getMsg());

        ApiAlertException.throwIfTrue(
            projectService.existsByTeamId(teamId),
            SYSTEM_TEAM_EXIST_MODULE_USE_DELETE_ERROR,
            team.getTeamName(),
            PROJECT.getMsg());

        ApiAlertException.throwIfTrue(
            variableService.existsByTeamId(teamId),
            SYSTEM_TEAM_EXIST_MODULE_USE_DELETE_ERROR,
            team.getTeamName(),
            VARIABLES.getMsg());

        memberService.removeByTeamId(teamId);
        userService.clearLastTeam(teamId);
        super.removeById(teamId);
    }

    @Override
    public void updateTeam(Team team) {
        Team oldTeam = Optional.ofNullable(this.getById(team.getId()))
            .orElseThrow(
                () -> new IllegalArgumentException(
                    String.format("Team id [id=%s] not found", team.getId())));
        ApiAlertException.throwIfFalse(
            oldTeam.getTeamName().equals(team.getTeamName()),
            "Team name can't be changed. Update team failed.");
        oldTeam.setDescription(team.getDescription());
        updateById(oldTeam);
    }

    @Override
    public List<Team> listByUserId(Long userId) {
        User user = Optional.ofNullable(userService.getById(userId))
            .orElseThrow(
                () -> new ApiAlertException(
                    String.format("The userId [%s] not found.", userId)));
        // Admin has the permission for all teams.
        if (UserTypeEnum.ADMIN == user.getUserType()) {
            return this.list();
        }
        return baseMapper.selectTeamsByUserId(userId);
    }

    @Override
    public Team getSysDefaultTeam() {
        return getById(Constant.DEFAULT_TEAM_ID);
    }
}
