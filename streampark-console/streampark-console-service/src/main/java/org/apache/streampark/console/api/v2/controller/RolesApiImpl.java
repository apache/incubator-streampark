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

import org.apache.streampark.console.api.controller.RolesApi;

import org.springframework.http.ResponseEntity;

public class RolesApiImpl implements RolesApi {
  @Override
  public ResponseEntity<Void> checkRole(String roleName) {
    return RolesApi.super.checkRole(roleName);
  }

  @Override
  public ResponseEntity<Void> createRole() {
    return RolesApi.super.createRole();
  }

  @Override
  public ResponseEntity<Void> deleteRole(Long roleId) {
    return RolesApi.super.deleteRole(roleId);
  }

  @Override
  public ResponseEntity<Void> listRole() {
    return RolesApi.super.listRole();
  }

  @Override
  public ResponseEntity<Void> listRoleMenu(Long roleId) {
    return RolesApi.super.listRoleMenu(roleId);
  }

  @Override
  public ResponseEntity<Void> updateRole(Long roleId) {
    return RolesApi.super.updateRole(roleId);
  }
}
