/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.streampark.console.api.v2.controller;

import org.apache.streampark.console.api.controller.UsersApi;

import org.springframework.http.ResponseEntity;

public class UsersApiImpl implements UsersApi {

  @Override
  public ResponseEntity<Void> checkUsername(String username) {
    return UsersApi.super.checkUsername(username);
  }

  @Override
  public ResponseEntity<Void> createUser() {
    return UsersApi.super.createUser();
  }

  @Override
  public ResponseEntity<Void> deleteUser(Long userId) {
    return UsersApi.super.deleteUser(userId);
  }

  @Override
  public ResponseEntity<Void> getUser(String username) {
    return UsersApi.super.getUser(username);
  }

  @Override
  public ResponseEntity<Void> initUserTeam(Long userId, Long teamId) {
    return UsersApi.super.initUserTeam(userId, teamId);
  }

  @Override
  public ResponseEntity<Void> listMenuRoute() {
    return UsersApi.super.listMenuRoute();
  }

  @Override
  public ResponseEntity<Void> listUser() {
    return UsersApi.super.listUser();
  }

  @Override
  public ResponseEntity<Void> listUserType() {
    return UsersApi.super.listUserType();
  }

  @Override
  public ResponseEntity<Void> resetPassword(String username) {
    return UsersApi.super.resetPassword(username);
  }

  @Override
  public ResponseEntity<Void> setUserTeam() {
    return UsersApi.super.setUserTeam();
  }

  @Override
  public ResponseEntity<Void> updatePassword(String username) {
    return UsersApi.super.updatePassword(username);
  }

  @Override
  public ResponseEntity<Void> updateUser(Long userId) {
    return UsersApi.super.updateUser(userId);
  }

  @Override
  public ResponseEntity<Void> verifyPassword(String username) {
    return UsersApi.super.verifyPassword(username);
  }
}
