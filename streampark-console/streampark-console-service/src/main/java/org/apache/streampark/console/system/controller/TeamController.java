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
import org.apache.streampark.console.base.domain.RestResponseBody;
import org.apache.streampark.console.base.web.FormOrJson;
import org.apache.streampark.console.system.assembler.TeamAssembler;
import org.apache.streampark.console.system.entity.Team;
import org.apache.streampark.console.system.request.team.TeamCheckNameRequest;
import org.apache.streampark.console.system.request.team.TeamCreateRequest;
import org.apache.streampark.console.system.request.team.TeamDeleteRequest;
import org.apache.streampark.console.system.request.team.TeamListQueryRequest;
import org.apache.streampark.console.system.request.team.TeamUpdateRequest;
import org.apache.streampark.console.system.response.team.TeamResponse;
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

@Slf4j
@Validated
@RestController
@RequestMapping("team")
public class TeamController {

    @Autowired
    private TeamService teamService;

    @PostMapping("list")
    public RestResponseBody<IPage<TeamResponse>> teamList(RestRequest restRequest, TeamListQueryRequest query) {
        IPage<Team> teamList = teamService.getPage(TeamAssembler.toEntity(query), restRequest);
        return RestResponseBody.success(TeamAssembler.toPageResponse(teamList));
    }

    @PostMapping("check/name")
    public RestResponseBody<Boolean> checkTeamName(@Valid TeamCheckNameRequest request) {
        Team result = this.teamService.getByName(request.getTeamName());
        return RestResponseBody.success(result == null);
    }

    @PostMapping("post")
    @RequiresPermissions("team:add")
    public RestResponseBody<Void> addTeam(@Valid @FormOrJson TeamCreateRequest request) {
        this.teamService.createTeam(TeamAssembler.toEntity(request));
        return RestResponseBody.success();
    }

    @DeleteMapping("delete")
    @RequiresPermissions("team:delete")
    public RestResponseBody<Void> deleteTeam(@Valid @FormOrJson TeamDeleteRequest request) {
        this.teamService.removeById(request.getId());
        return RestResponseBody.success();
    }

    @PutMapping("update")
    @RequiresPermissions("team:update")
    public RestResponseBody<Void> updateTeam(@Valid @FormOrJson TeamUpdateRequest request) {
        this.teamService.updateTeam(TeamAssembler.toEntity(request));
        return RestResponseBody.success();
    }
}
