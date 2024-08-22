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

package org.apache.streampark.console.system.service;

import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.system.entity.Team;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface TeamService extends IService<Team> {

    /**
     * Retrieves a page of {@link Team} objects based on the provided parameters.
     *
     * @param team The {@link Team} object containing the search criteria.
     * @param request The {@link RestRequest} object used for pagination and sorting.
     * @return An {@link IPage} containing the retrieved {@link Team} objects.
     */
    IPage<Team> getPage(Team team, RestRequest request);

    /**
     * Get the Team by team name
     *
     * @param teamName team name
     * @return Team
     */
    Team getByName(String teamName);

    /**
     * Create a Team instance
     *
     * @param team Team
     */
    void createTeam(Team team);

    /**
     * Remove the Team by team id
     *
     * @param teamId team id
     */
    void removeById(Long teamId);

    /**
     * Update Team information carried by the incoming Team
     *
     * @param team Team
     */
    void updateTeam(Team team);

    /**
     * List All team by user id
     *
     * @param userId user id
     * @return List of Team
     */
    List<Team> listByUserId(Long userId);

    /**
     * get system default team
     * @return
     */

    Team getSysDefaultTeam();
}
