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
import org.apache.streampark.console.core.annotation.PermissionAction;
import org.apache.streampark.console.core.enums.PermissionType;
import org.apache.streampark.console.system.entity.Member;
import org.apache.streampark.console.system.entity.Team;
import org.apache.streampark.console.system.entity.User;
import org.apache.streampark.console.system.service.MemberService;

import org.apache.shiro.authz.annotation.RequiresPermissions;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

import java.util.List;

@Tag(name = "MEMBER_TAG")
@Slf4j
@Validated
@RestController
@RequestMapping("member")
public class MemberController {

  @Autowired private MemberService memberService;

  @Operation(summary = "List members")
  @PostMapping("list")
  public RestResponse memberList(RestRequest restRequest, Member member) {
    IPage<Member> userList = memberService.findUsers(member, restRequest);
    return RestResponse.success(userList);
  }

  @Operation(summary = "List candidate users")
  @PostMapping("candidateUsers")
  public RestResponse candidateUsers(Long teamId) {
    List<User> userList = memberService.findCandidateUsers(teamId);
    return RestResponse.success(userList);
  }

  @Operation(summary = "List teams")
  @PostMapping("teams")
  public RestResponse listTeams(Long userId) {
    List<Team> teamList = memberService.findUserTeams(userId);
    return RestResponse.success(teamList);
  }

  @Operation(summary = "Check the username")
  @PostMapping("check/user")
  public RestResponse check(@NotBlank(message = "{required}") Long teamId, String userName) {
    Member result = this.memberService.findByUserName(teamId, userName);
    return RestResponse.success(result == null);
  }

  @Operation(summary = "Create member")
  @PermissionAction(id = "#member.teamId", type = PermissionType.TEAM)
  @PostMapping("post")
  @RequiresPermissions("member:add")
  public RestResponse create(@Valid Member member) {
    this.memberService.createMember(member);
    return RestResponse.success();
  }

  @Operation(summary = "Delete member")
  @PermissionAction(id = "#member.teamId", type = PermissionType.TEAM)
  @DeleteMapping("delete")
  @RequiresPermissions("member:delete")
  public RestResponse delete(Member member) {
    this.memberService.deleteMember(member);
    return RestResponse.success();
  }

  @Operation(summary = "Update member")
  @PermissionAction(id = "#member.teamId", type = PermissionType.TEAM)
  @PutMapping("update")
  @RequiresPermissions("member:update")
  public RestResponse update(Member member) {
    this.memberService.updateMember(member);
    return RestResponse.success();
  }
}
