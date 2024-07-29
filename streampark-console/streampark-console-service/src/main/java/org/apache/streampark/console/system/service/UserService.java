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
import org.apache.streampark.console.base.domain.RestResponse;
import org.apache.streampark.console.system.authentication.JWTToken;
import org.apache.streampark.console.system.entity.User;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface UserService extends IService<User> {

    /**
     * find user by name
     *
     * @param username username
     * @return user
     */
    User getByUsername(String username);

    /**
     * find uer detail, contains basic info, role, department
     *
     * @param user user
     * @param restRequest queryRequest
     * @return IPage
     */
    IPage<User> getPage(User user, RestRequest restRequest);

    /**
     * update login time
     *
     * @param username username
     */
    void updateLoginTime(String username) throws Exception;

    /**
     * create user
     *
     * @param user user
     */
    void createUser(User user) throws Exception;

    /**
     * update user
     *
     * @param user user
     * @return
     */
    RestResponse updateUser(User user) throws Exception;

    /**
     * update password
     *
     * @param user
     * @throws Exception
     */
    void updatePassword(User user) throws Exception;

    /**
     * reset password
     *
     * @param username user name
     */
    String resetPassword(String username) throws Exception;

    /**
     * Get the permissions of current userId.
     *
     * @param userId the user Id
     * @param teamId team id. If it's null, will find permissions from all teams.
     * @return permissions
     */
    Set<String> listPermissions(Long userId, @Nullable Long teamId);

    /**
     * List all users without tokens
     *
     * @return List of User
     */
    List<User> listNoTokenUser();

    /**
     * Populate the LastTeam field in User
     *
     * @param teamId team id
     * @param userId user id
     */
    void setLastTeam(Long teamId, Long userId);

    /**
     * Clean the LastTeam field in User
     *
     * @param userId user id
     * @param teamId team id
     */
    void clearLastTeam(Long userId, Long teamId);

    /**
     * Clean the LastTeam field in User
     *
     * @param teamId team id
     */
    void clearLastTeam(Long teamId);

    /**
     * List all Users by team id
     *
     * @param teamId team id
     * @return List of user
     */
    List<User> listByTeamId(Long teamId);

    /**
     * Generate user information for the front end
     *
     * @param user User
     * @param teamId team id
     * @param token JWTToken
     * @return
     */
    Map<String, Object> generateFrontendUserInfo(User user, Long teamId, JWTToken token);

    /**
     * transfer user resources to specified users
     *
     * @param userId The user ID sending the resource
     * @param targetUserId The user ID receiving the resource
     */
    void transferResource(Long userId, Long targetUserId);

    /**
     * Get login user information
     *
     * @param user User
     * @return RestResponse
     */
    RestResponse getLoginUserInfo(User user);

    void deleteUser(Long userId);
}
