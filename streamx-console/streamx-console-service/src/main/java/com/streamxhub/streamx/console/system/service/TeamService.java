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

package com.streamxhub.streamx.console.system.service;

import com.streamxhub.streamx.console.base.domain.RestRequest;
import com.streamxhub.streamx.console.system.entity.Team;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * @author benjobs
 */
public interface TeamService extends IService<Team> {

    String deleteTeamBeforeCheck(Long teamId);

    IPage<Team> findTeamsByNowUser(Team team, RestRequest request);

    IPage<Team> findTeamsByUser(String username, Team team, RestRequest request);

    IPage<Team> findTeams(Team group, RestRequest restRequest);

    void createTeam(Team team);

    Team findByName(String teamName);

    Team findByCode(String teamCode);

    List<Team> findTeamByUser(String username);
}
