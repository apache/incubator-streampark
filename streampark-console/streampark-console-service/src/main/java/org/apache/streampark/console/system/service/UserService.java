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
  User findByName(String username);

  /**
   * find uer detail, contains basic info, role, department
   *
   * @param user user
   * @param restRequest queryRequest
   * @return IPage
   */
  IPage<User> page(User user, RestRequest restRequest);

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
   */
  void updateUser(User user) throws Exception;

  /**
   * delete user
   *
   * @param userId user id
   */
  void deleteUser(Long userId) throws Exception;

  /**
   * update password
   *
   * @param user
   * @throws Exception
   */
  void updatePassword(User user) throws Exception;

  void updateSaltPassword(User user) throws Exception;

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
  Set<String> getPermissions(Long userId, @Nullable Long teamId);

  List<User> getNoTokenUser();

  void setLastTeam(Long teamId, Long userId);

  void clearLastTeam(Long userId, Long teamId);

  void clearLastTeam(Long teamId);

  List<User> findByAppOwner(Long teamId);

  Map<String, Object> generateFrontendUserInfo(User user, JWTToken token);
}
