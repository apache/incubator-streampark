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
import org.apache.streampark.console.system.entity.Member;
import org.apache.streampark.console.system.entity.Team;
import org.apache.streampark.console.system.entity.User;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface MemberService extends IService<Member> {

    /**
     * Remove Member by role ids
     *
     * @param roleIds List of role id
     */
    void removeByRoleIds(String[] roleIds);

    /**
     * Remove Member by user id
     *
     * @param userId user id
     */
    void removeByUserId(Long userId);

    /**
     * Remove Member by team Id
     *
     * @param teamId team Id
     */
    void removeByTeamId(Long teamId);

    /**
     * Retrieves a page of {@link Member} objects based on the provided parameters.
     *
     * @param member The {@link Member} object containing the search criteria.
     * @param request The {@link RestRequest} object used for pagination and sorting.
     * @return An {@link IPage} containing the retrieved {@link Member} objects.
     */
    IPage<Member> getPage(Member member, RestRequest request);

    /**
     * List all users who are not in the team with the passed team id
     *
     * @param teamId team id
     * @return List of User
     */
    List<User> listUsersNotInTeam(Long teamId);

    /**
     * List all Teams, those containing the passed user id
     *
     * @param userId User id
     * @return List of Team
     */
    List<Team> listTeamsByUserId(Long userId);

    /**
     * Get Member by team id and username
     *
     * @param teamId team id
     * @param userName username
     * @return Member
     */
    Member getByTeamIdUserName(Long teamId, String userName);

    /**
     * List all User ids based on Role id as a condition
     *
     * @param roleId
     * @return List of User ids
     */
    List<Long> listUserIdsByRoleId(Long roleId);

    /**
     * Create a new Member instance
     *
     * @param member Member
     */
    void createMember(Member member);

    /**
     * Remove a member
     *
     * @param id Member id to be removed
     */
    void remove(Long id);

    /**
     * Update Member
     *
     * @param member Member which one contains the information to be updated
     */
    void updateMember(Member member);
}
