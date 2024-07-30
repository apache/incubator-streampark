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

import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.base.enums.MessageStatus;
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.base.mybatis.pager.MybatisPager;
import org.apache.streampark.console.system.entity.Member;
import org.apache.streampark.console.system.entity.Team;
import org.apache.streampark.console.system.entity.User;
import org.apache.streampark.console.system.mapper.MemberMapper;
import org.apache.streampark.console.system.service.MemberService;
import org.apache.streampark.console.system.service.RoleService;
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
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.streampark.console.base.enums.MessageStatus.MEMBER_ID_NOT_EXIST;
import static org.apache.streampark.console.base.enums.MessageStatus.MEMBER_TEAM_ID_CHANGE_ERROR;
import static org.apache.streampark.console.base.enums.MessageStatus.MEMBER_USER_ID_CHANGE_ERROR;
import static org.apache.streampark.console.base.enums.MessageStatus.SYSTEM_ROLE_ID_NOT_EXIST;
import static org.apache.streampark.console.base.enums.MessageStatus.SYSTEM_TEAM_ID_CANNOT_NULL;
import static org.apache.streampark.console.base.enums.MessageStatus.SYSTEM_TEAM_ID_NOT_EXIST;
import static org.apache.streampark.console.base.enums.MessageStatus.SYSTEM_USER_NOT_EXIST;

@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class MemberServiceImpl extends ServiceImpl<MemberMapper, Member> implements MemberService {

    @Autowired
    private UserService userService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private TeamService teamService;

    @Override
    @Transactional
    public void removeByRoleIds(String[] roleIds) {
        Arrays.stream(roleIds).forEach(id -> baseMapper.deleteByRoleId(Long.valueOf(id)));
    }

    @Override
    @Transactional
    public void removeByUserId(Long userId) {
        baseMapper.deleteByUserId(userId);
    }

    @Override
    public void removeByTeamId(Long teamId) {
        LambdaQueryWrapper<Member> queryWrapper = new LambdaQueryWrapper<Member>().eq(Member::getTeamId, teamId);
        this.remove(queryWrapper);
    }

    @Override
    public IPage<Member> getPage(Member member, RestRequest request) {
        ApiAlertException.throwIfNull(member.getTeamId(), SYSTEM_TEAM_ID_CANNOT_NULL);
        Page<Member> page = MybatisPager.getPage(request);
        return baseMapper.selectPage(page, member);
    }

    @Override
    public List<User> listUsersNotInTeam(Long teamId) {
        return baseMapper.selectUsersNotInTeam(teamId);
    }

    @Override
    public List<Team> listTeamsByUserId(Long userId) {
        return teamService.listByUserId(userId);
    }

    @Override
    public Member getByTeamIdUserName(Long teamId, String userName) {
        User user = userService.getByUsername(userName);
        if (user == null) {
            return null;
        }
        return findByUserId(teamId, user.getUserId());
    }

    private Member findByUserId(Long teamId, Long userId) {
        ApiAlertException.throwIfNull(teamId, SYSTEM_TEAM_ID_CANNOT_NULL);
        LambdaQueryWrapper<Member> queryWrapper = new LambdaQueryWrapper<Member>()
            .eq(Member::getTeamId, teamId)
            .eq(Member::getUserId, userId);
        return baseMapper.selectOne(queryWrapper);
    }

    @Override
    public List<Long> listUserIdsByRoleId(Long roleId) {
        LambdaQueryWrapper<Member> queryWrapper = new LambdaQueryWrapper<Member>().eq(Member::getRoleId, roleId);
        List<Member> memberList = baseMapper.selectList(queryWrapper);
        return memberList.stream().map(Member::getUserId).collect(Collectors.toList());
    }

    @Override
    public void createMember(Member member) {
        User user = userService.getByUsername(member.getUserName());
        ApiAlertException.throwIfNull(user, SYSTEM_USER_NOT_EXIST, member.getUserName());

        ApiAlertException.throwIfNull(
            roleService.getById(member.getRoleId()), SYSTEM_ROLE_ID_NOT_EXIST, member.getRoleId());
        Team team = teamService.getById(member.getTeamId());
        ApiAlertException.throwIfNull(team, SYSTEM_TEAM_ID_NOT_EXIST, member.getTeamId());
        ApiAlertException.throwIfNotNull(
            findByUserId(member.getTeamId(), user.getUserId()),
            MessageStatus.MEMBER_USER_TEAM_ALREADY_ERROR,
            member.getUserName(),
            team.getTeamName());

        member.setId(null);
        member.setUserId(user.getUserId());

        this.save(member);
    }

    @Override
    public void remove(Long id) {
        Member member = this.getById(id);
        ApiAlertException.throwIfNull(member, MEMBER_ID_NOT_EXIST, id);
        this.removeById(member);
        userService.clearLastTeam(member.getUserId(), member.getTeamId());
    }

    @Override
    public void updateMember(Member member) {
        Member oldMember = this.getById(member.getId());
        ApiAlertException.throwIfNull(oldMember, MEMBER_ID_NOT_EXIST, member.getId());
        ApiAlertException.throwIfFalse(oldMember.getTeamId().equals(member.getTeamId()), MEMBER_TEAM_ID_CHANGE_ERROR);
        ApiAlertException.throwIfFalse(oldMember.getUserId().equals(member.getUserId()), MEMBER_USER_ID_CHANGE_ERROR);
        ApiAlertException.throwIfNull(
            roleService.getById(member.getRoleId()), SYSTEM_ROLE_ID_NOT_EXIST, member.getRoleId());
        oldMember.setRoleId(member.getRoleId());
        updateById(oldMember);
    }
}
