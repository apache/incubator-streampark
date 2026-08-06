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

package org.apache.streampark.console.system.assembler;

import org.apache.streampark.console.core.assembler.DtoAssembler;
import org.apache.streampark.console.system.entity.Team;
import org.apache.streampark.console.system.request.team.TeamCreateRequest;
import org.apache.streampark.console.system.request.team.TeamListQueryRequest;
import org.apache.streampark.console.system.request.team.TeamUpdateRequest;
import org.apache.streampark.console.system.response.team.TeamResponse;

import com.baomidou.mybatisplus.core.metadata.IPage;

/** Converts between team entities and API request/response contracts. */
public final class TeamAssembler {

    private TeamAssembler() {
    }

    public static Team toEntity(TeamListQueryRequest request) {
        return DtoAssembler.toDto(request, Team.class);
    }

    public static Team toEntity(TeamCreateRequest request) {
        return DtoAssembler.toDto(request, Team.class);
    }

    public static Team toEntity(TeamUpdateRequest request) {
        return DtoAssembler.toDto(request, Team.class);
    }

    public static TeamResponse toResponse(Team team) {
        return DtoAssembler.toDto(team, TeamResponse.class);
    }

    public static IPage<TeamResponse> toPageResponse(IPage<Team> page) {
        return DtoAssembler.toPage(page, TeamAssembler::toResponse);
    }
}
