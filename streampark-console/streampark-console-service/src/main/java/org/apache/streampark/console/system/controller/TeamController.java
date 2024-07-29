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
    public Response<IPage<Team>> teamList(PageRequest pageRequest, Team team) {
        IPage<Team> teamList = teamService.getPage(team, pageRequest);
        return Response.success(teamList);
    }

    @PostMapping("check/name")
    public Response<Boolean> checkTeamName(@NotBlank(message = "{required}") String teamName) {
        Team result = this.teamService.getByName(teamName);
        return Response.success(result == null);
    }

    @PostMapping("post")
    @RequiresPermissions("team:add")
    public Response<Void> addTeam(@Valid Team team) {
        this.teamService.createTeam(team);
        return Response.success();
    }

    @DeleteMapping("delete")
    @RequiresPermissions("team:delete")
    public Response<Void> deleteTeam(Team team) {
        this.teamService.removeById(team.getId());
        return Response.success();
    }

    @PutMapping("update")
    @RequiresPermissions("team:update")
    public Response<Void> updateTeam(Team team) {
        this.teamService.updateTeam(team);
        return Response.success();
    }
}
