/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.console.system.controller;

import com.streamxhub.streamx.console.base.domain.RestRequest;
import com.streamxhub.streamx.console.base.domain.RestResponse;
import com.streamxhub.streamx.console.system.entity.Team;
import com.streamxhub.streamx.console.system.service.TeamService;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

/**
 * @author daixinyu
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/team")
public class TeamController {

    @Autowired
    private TeamService teamService;

    @PostMapping("list")
    @RequiresPermissions("team:view")
    public RestResponse teamList(RestRequest restRequest, Team team) {
        IPage<Team> groupList = teamService.findTeams(team, restRequest);
        return RestResponse.success(groupList);
    }

    @PostMapping("/listByUser")
    public RestResponse listByUser(RestRequest restRequest, Team team) {
        IPage<Team> teamList = teamService.findTeamsByNowUser(team, restRequest);
        return RestResponse.success(teamList);
    }

    @PostMapping("post")
    @RequiresPermissions("team:add")
    public RestResponse addTeam(@Valid Team team) throws Exception {
        this.teamService.createTeam(team);
        return RestResponse.success();
    }

    @DeleteMapping("delete")
    @RequiresPermissions("team:delete")
    public RestResponse deleteTeam(Long teamId) {
        String result = this.teamService.deleteTeamBeforeCheck(teamId);
        return RestResponse.success().message(result);
    }

    @PostMapping("check/name")
    public RestResponse checkTeamName(@NotBlank(message = "{required}") String teamName) {
        boolean result = this.teamService.findByName(teamName) == null;
        return RestResponse.success(result);
    }

    @PostMapping("check/code")
    public RestResponse checkTeamCode(@NotBlank(message = "{required}") String teamCode) {
        boolean result = this.teamService.findByCode(teamCode) == null;
        return RestResponse.success(result);
    }
}
