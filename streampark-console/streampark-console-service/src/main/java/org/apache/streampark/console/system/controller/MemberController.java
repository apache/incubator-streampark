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
import org.apache.streampark.console.system.entity.Team;
import org.apache.streampark.console.system.entity.TeamMember;
import org.apache.streampark.console.system.service.TeamMemberService;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
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

@Slf4j
@Validated
@RestController
@RequestMapping("member")
public class TeamMemberController {

    @Autowired
    private TeamMemberService teamMemberService;

    @PostMapping("list")
    public RestResponse memberList(RestRequest restRequest, TeamMember teamMember) {
        IPage<TeamMember> userList = teamMemberService.findUsers(teamMember, restRequest);
        return RestResponse.success(userList);
    }

    @PostMapping("teams")
    public RestResponse listTeams(Long userId) {
        List<Team> teamList = teamMemberService.findUserTeams(userId);
        return RestResponse.success(teamList);
    }

    @PostMapping("check/user")
    public RestResponse checkTeamUser(@NotBlank(message = "{required}") String userName) {
        TeamMember result = this.teamMemberService.findByUserName(userName);
        return RestResponse.success(result == null);
    }

    @PostMapping("post")
    @RequiresPermissions("member:add")
    public RestResponse addUserTeamRole(@Valid TeamMember teamMember) {
        this.teamMemberService.createTeamMember(teamMember);
        return RestResponse.success();
    }

    @DeleteMapping("delete")
    @RequiresPermissions("member:delete")
    public RestResponse deleteUserTeamRole(TeamMember teamMember) {
        this.teamMemberService.deleteTeamMember(teamMember);
        return RestResponse.success();
    }

    @PutMapping("update")
    @RequiresPermissions("member:update")
    public RestResponse updateUserTeamRole(TeamMember teamMember) {
        this.teamMemberService.updateTeamMember(teamMember);
        return RestResponse.success();
    }

}
