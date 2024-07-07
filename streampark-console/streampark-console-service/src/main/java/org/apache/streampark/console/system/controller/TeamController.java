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
import org.apache.streampark.console.system.service.TeamService;

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
import javax.validation.constraints.NotBlank;

@Slf4j
@Validated
@RestController
@RequestMapping("team")
public class TeamController {

    @Autowired
    private TeamService teamService;

    @PostMapping("list")
    public RestResponse teamList(RestRequest restRequest, Team team) {
        IPage<Team> teamList = teamService.getPage(team, restRequest);
        return RestResponse.success(teamList);
    }

    @PostMapping("check/name")
    public RestResponse checkTeamName(@NotBlank(message = "{required}") String teamName) {
        Team result = this.teamService.getByName(teamName);
        return RestResponse.success(result == null);
    }

    @PostMapping("post")
    @RequiresPermissions("team:add")
    public RestResponse addTeam(@Valid Team team) {
        this.teamService.createTeam(team);
        return RestResponse.success();
    }

    @DeleteMapping("delete")
    @RequiresPermissions("team:delete")
    public RestResponse deleteTeam(Team team) {
        this.teamService.removeById(team.getId());
        return RestResponse.success();
    }

    @PutMapping("update")
    @RequiresPermissions("team:update")
    public RestResponse updateTeam(Team team) {
        this.teamService.updateTeam(team);
        return RestResponse.success();
    }
}
