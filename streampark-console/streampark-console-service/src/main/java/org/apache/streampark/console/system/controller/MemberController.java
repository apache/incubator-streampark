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

package org.apache.streampark.console.system.controller;

import org.apache.streampark.console.base.bean.PageRequest;
import org.apache.streampark.console.base.bean.Response;
import org.apache.streampark.console.core.annotation.Permission;
import org.apache.streampark.console.system.entity.Member;
import org.apache.streampark.console.system.entity.Team;
import org.apache.streampark.console.system.entity.User;
import org.apache.streampark.console.system.service.MemberService;

import org.apache.shiro.authz.annotation.RequiresPermissions;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("member")
public class MemberController {

    @Autowired
    private MemberService memberService;

    @PostMapping("list")
    public Response<IPage<Member>> memberList(PageRequest pageRequest, Member member) {
        IPage<Member> userList = memberService.getPage(member, pageRequest);
        return Response.success(userList);
    }

    @PostMapping("candidateUsers")
    public Response<List<User>> candidateUsers(Long teamId) {
        List<User> userList = memberService.listUsersNotInTeam(teamId);
        return Response.success(userList);
    }

    @PostMapping("teams")
    public Response<List<Team>> listTeams(Long userId) {
        List<Team> teamList = memberService.listTeamsByUserId(userId);
        return Response.success(teamList);
    }

    @PostMapping("check/user")
    public Response<Boolean> check(@NotNull(message = "{required}") Long teamId, String userName) {
        Member result = this.memberService.getByTeamIdUserName(teamId, userName);
        return Response.success(result == null);
    }

    @PostMapping("post")
    @Permission(team = "#member.teamId")
    @RequiresPermissions("member:add")
    public Response<Void> create(@Valid Member member) {
        this.memberService.createMember(member);
        return Response.success();
    }

    @DeleteMapping("delete")
    @Permission(team = "#member.teamId")
    @RequiresPermissions("member:delete")
    public Response<Void> delete(Member member) {
        this.memberService.remove(member.getId());
        return Response.success();
    }

    @PutMapping("update")
    @Permission(team = "#member.teamId")
    @RequiresPermissions("member:update")
    public Response<Void> update(Member member) {
        this.memberService.updateMember(member);
        return Response.success();
    }
}
