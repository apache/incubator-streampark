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

import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.base.domain.RestResponse;
import org.apache.streampark.console.core.annotation.Permission;
import org.apache.streampark.console.system.assembler.MemberAssembler;
import org.apache.streampark.console.system.assembler.TeamAssembler;
import org.apache.streampark.console.system.assembler.UserAssembler;
import org.apache.streampark.console.system.entity.Member;
import org.apache.streampark.console.system.request.member.MemberCandidateUsersRequest;
import org.apache.streampark.console.system.request.member.MemberCheckUserRequest;
import org.apache.streampark.console.system.request.member.MemberCreateRequest;
import org.apache.streampark.console.system.request.member.MemberDeleteRequest;
import org.apache.streampark.console.system.request.member.MemberListQueryRequest;
import org.apache.streampark.console.system.request.member.MemberTeamsRequest;
import org.apache.streampark.console.system.request.member.MemberUpdateRequest;
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

import java.util.stream.Collectors;

@Slf4j
@Validated
@RestController
@RequestMapping("member")
public class MemberController {

    @Autowired
    private MemberService memberService;

    @PostMapping("list")
    public RestResponse memberList(RestRequest restRequest, MemberListQueryRequest query) {
        IPage<Member> userList = memberService.getPage(MemberAssembler.toEntity(query), restRequest);
        return RestResponse.success(MemberAssembler.toPageResponse(userList));
    }

    @PostMapping("candidateUsers")
    public RestResponse candidateUsers(MemberCandidateUsersRequest request) {
        return RestResponse.success(
            UserAssembler.toResponseList(memberService.listUsersNotInTeam(request.getTeamId())));
    }

    @PostMapping("teams")
    public RestResponse listTeams(MemberTeamsRequest request) {
        return RestResponse.success(
            memberService.listTeamsByUserId(request.getUserId()).stream()
                .map(TeamAssembler::toResponse)
                .collect(Collectors.toList()));
    }

    @PostMapping("check/user")
    public RestResponse check(@Valid MemberCheckUserRequest request) {
        Member result = this.memberService.getByTeamIdUserName(request.getTeamId(), request.getUserName());
        return RestResponse.success(result == null);
    }

    @PostMapping("post")
    @Permission(team = "#request.teamId")
    @RequiresPermissions("member:add")
    public RestResponse create(@Valid MemberCreateRequest request) {
        this.memberService.createMember(MemberAssembler.toEntity(request));
        return RestResponse.success();
    }

    @DeleteMapping("delete")
    @Permission(team = "#request.teamId")
    @RequiresPermissions("member:delete")
    public RestResponse delete(MemberDeleteRequest request) {
        this.memberService.remove(request.getId());
        return RestResponse.success();
    }

    @PutMapping("update")
    @Permission(team = "#request.teamId")
    @RequiresPermissions("member:update")
    public RestResponse update(MemberUpdateRequest request) {
        this.memberService.updateMember(MemberAssembler.toEntity(request));
        return RestResponse.success();
    }
}
