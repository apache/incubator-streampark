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
import org.apache.streampark.console.base.exception.ApiAlertException;
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
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class MemberServiceImpl extends ServiceImpl<MemberMapper, Member>
    implements MemberService {

    @Autowired
    private UserService userService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private TeamService teamService;

    @Override
    @Transactional
    public void deleteByRoleIds(String[] roleIds) {
        Arrays.stream(roleIds).forEach(id -> baseMapper.deleteByRoleId(Long.valueOf(id)));
    }

    @Override
    @Transactional
    public void deleteByUserId(Long userId) {
        baseMapper.deleteByUserId(userId);
    }

    @Override
    public void deleteByTeamId(Long teamId) {
        LambdaQueryWrapper<Member> queryWrapper = new LambdaQueryWrapper<Member>()
            .eq(Member::getTeamId, teamId);
        this.remove(queryWrapper);
    }

    @Override
    public IPage<Member> findUsers(Member member, RestRequest request) {
        AssertUtils.isTrue(member.getTeamId() != null, "The team id is required.");
        Page<Member> page = new Page<>();
        page.setCurrent(request.getPageNum());
        page.setSize(request.getPageSize());
        return baseMapper.findUsers(page, member);
    }

    @Override
    public List<Team> findUserTeams(Long userId) {
        return teamService.findUserTeams(userId);
    }

    @Override
    public Member findByUserName(Long teamId, String userName) {
        User user = userService.findByName(userName);
        if (user == null) {
            return null;
        }
        return findByUserId(teamId, user.getUserId());
    }

    private Member findByUserId(Long teamId, Long userId) {
        AssertUtils.isTrue(teamId != null, "The team id is required.");
        LambdaQueryWrapper<Member> queryWrapper = new LambdaQueryWrapper<Member>().eq(Member::getTeamId, teamId)
            .eq(Member::getUserId, userId);
        return baseMapper.selectOne(queryWrapper);
    }

    @Override
    public List<Long> findUserIdsByRoleId(Long roleId) {
        LambdaQueryWrapper<Member> queryWrapper = new LambdaQueryWrapper<Member>()
            .eq(Member::getRoleId, roleId);
        List<Member> list = baseMapper.selectList(queryWrapper);
        return list.stream()
            .map(Member::getUserId)
            .collect(Collectors.toList());
    }

    @Override
    public void createMember(Member member) {
        User user = Optional.ofNullable(userService.findByName(member.getUserName()))
            .orElseThrow(() -> new ApiAlertException(String.format("The username [%s] not found", member.getUserName())));
        Optional.ofNullable(roleService.getById(member.getRoleId()))
            .orElseThrow(() -> new ApiAlertException(String.format("The roleId [%s] not found", member.getRoleId())));
        Team team = Optional.ofNullable(teamService.getById(member.getTeamId()))
            .orElseThrow(() -> new ApiAlertException(String.format("The teamId [%s] not found", member.getTeamId())));
        AssertUtils.isTrue(findByUserId(member.getTeamId(), user.getUserId()) == null,
            String.format("The user [%s] has been added the team [%s], please don't add it again.", member.getUserName(),
                team.getTeamName()));

        member.setId(null);
        member.setUserId(user.getUserId());
        member.setCreateTime(new Date());
        member.setModifyTime(team.getCreateTime());
        this.save(member);
    }

    @Override
    public void deleteMember(Member memberArg) {
        Member member = Optional.ofNullable(this.getById(memberArg.getId()))
            .orElseThrow(() -> new ApiAlertException(String.format("The member [id=%s] not found", memberArg.getId())));
        this.removeById(member);
        userService.clearLastTeam(member.getUserId(), member.getTeamId());
    }

    @Override
    public void updateMember(Member member) {
        Member oldMember = Optional.ofNullable(this.getById(member.getId()))
            .orElseThrow(() -> new ApiAlertException(String.format("The member [id=%s] not found", member.getId())));
        AssertUtils.isTrue(oldMember.getTeamId().equals(member.getTeamId()), "Team id cannot be changed.");
        AssertUtils.isTrue(oldMember.getUserId().equals(member.getUserId()), "User id cannot be changed.");
        Optional.ofNullable(roleService.getById(member.getRoleId()))
            .orElseThrow(() -> new ApiAlertException(String.format("The roleId [%s] not found", member.getRoleId())));
        oldMember.setRoleId(member.getRoleId());
        updateById(oldMember);
    }
}
