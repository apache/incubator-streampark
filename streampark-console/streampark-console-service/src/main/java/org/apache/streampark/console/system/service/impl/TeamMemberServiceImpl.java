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
import org.apache.streampark.console.system.entity.Team;
import org.apache.streampark.console.system.entity.TeamMember;
import org.apache.streampark.console.system.entity.User;
import org.apache.streampark.console.system.mapper.TeamMemberMapper;
import org.apache.streampark.console.system.service.RoleService;
import org.apache.streampark.console.system.service.TeamMemberService;
import org.apache.streampark.console.system.service.TeamService;
import org.apache.streampark.console.system.service.UserService;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class TeamMemberServiceImpl extends ServiceImpl<TeamMemberMapper, TeamMember>
    implements TeamMemberService {

    @Autowired
    private UserService userService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private TeamService teamService;

    @Override
    @Transactional
    public void deleteUserRolesByRoleId(String[] roleIds) {
        Arrays.stream(roleIds).forEach(id -> baseMapper.deleteByRoleId(Long.valueOf(id)));
    }

    @Override
    @Transactional
    public void deleteUserRolesByUserId(String[] userIds) {
        Arrays.stream(userIds).forEach(id -> baseMapper.deleteByUserId(Long.valueOf(id)));
    }

    @Override
    public IPage<TeamMember> findUsers(TeamMember teamMember, RestRequest request) {
        AssertUtils.isTrue(teamMember.getTeamId() != null, "The team id is required.");
        Page<TeamMember> page = new Page<>();
        page.setCurrent(request.getPageNum());
        page.setSize(request.getPageSize());
        return baseMapper.findUsers(page, teamMember);
    }

    @Override
    public List<Team> findUserTeams(Long userId) {
        return teamService.findUserTeams(userId);
    }

    @Override
    public TeamMember findByTeamAndUserName(Long teamId, String userName) {
        User user = userService.findByName(userName);
        if (user == null) {
            return null;
        }
        return findByTeamAndUserId(teamId, user.getUserId());
    }

    private TeamMember findByTeamAndUserId(Long teamId, Long userId) {
        return baseMapper.selectOne(
            new LambdaQueryWrapper<TeamMember>().eq(TeamMember::getTeamId, teamId)
                .eq(TeamMember::getUserId, userId));
    }

    @Override
    public List<Long> findUserIdsByRoleId(Long roleId) {
        List<TeamMember> list =
            baseMapper.selectList(
                new LambdaQueryWrapper<TeamMember>().eq(TeamMember::getRoleId, roleId));
        return list.stream()
            .map(TeamMember::getUserId)
            .collect(Collectors.toList());
    }

    @Override
    public void createTeamMember(TeamMember teamMember) {
        User user = Optional.ofNullable(userService.findByName(teamMember.getUserName()))
            .orElseThrow(() -> new IllegalArgumentException(String.format("The username [%s] not found", teamMember.getUserName())));
        Optional.ofNullable(roleService.getById(teamMember.getRoleId()))
            .orElseThrow(() -> new IllegalArgumentException(String.format("The roleId [%s] not found", teamMember.getRoleId())));
        Team team = Optional.ofNullable(teamService.getById(teamMember.getTeamId()))
            .orElseThrow(() -> new IllegalArgumentException(String.format("The teamId [%s] not found", teamMember.getTeamId())));
        AssertUtils.isTrue(findByTeamAndUserId(teamMember.getTeamId(), user.getUserId()) == null,
            String.format("The user [%s] has been added the team [%s], please don't add it again.", teamMember.getUserName(), team.getTeamName()));

        teamMember.setId(null);
        teamMember.setUserId(user.getUserId());
        teamMember.setCreateTime(new Date());
        teamMember.setModifyTime(team.getCreateTime());
        this.save(teamMember);
    }

    @Override
    public void deleteTeamMember(TeamMember teamMember) {
        this.removeById(teamMember);
    }

    @Override
    public void updateTeamMember(TeamMember teamMember) {
        TeamMember oldTeamMember = Optional.ofNullable(this.getById(teamMember.getId()))
            .orElseThrow(() -> new IllegalArgumentException(String.format("The mapping [id=%s] not found", teamMember.getId())));
        AssertUtils.isTrue(oldTeamMember.getTeamId().equals(teamMember.getTeamId()), "Team id cannot be changed.");
        AssertUtils.isTrue(oldTeamMember.getUserId().equals(teamMember.getUserId()), "User id cannot be changed.");
        Optional.ofNullable(roleService.getById(teamMember.getRoleId()))
            .orElseThrow(() -> new IllegalArgumentException(String.format("The roleId [%s] not found", teamMember.getRoleId())));
        oldTeamMember.setRoleId(teamMember.getRoleId());
        updateById(oldTeamMember);
    }
}
