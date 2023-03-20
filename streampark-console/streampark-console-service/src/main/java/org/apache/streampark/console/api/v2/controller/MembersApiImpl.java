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

import org.apache.streampark.console.api.controller.MembersApi;

import org.springframework.http.ResponseEntity;

public class MembersApiImpl implements MembersApi {
  @Override
  public ResponseEntity<Void> checkTeamMember(Long teamId, String username) {
    return MembersApi.super.checkTeamMember(teamId, username);
  }

  @Override
  public ResponseEntity<Void> createMember() {
    return MembersApi.super.createMember();
  }

  @Override
  public ResponseEntity<Void> deleteMember(Long memberId) {
    return MembersApi.super.deleteMember(memberId);
  }

  @Override
  public ResponseEntity<Void> listMember() {
    return MembersApi.super.listMember();
  }

  @Override
  public ResponseEntity<Void> listMemberTeam(Long memberId) {
    return MembersApi.super.listMemberTeam(memberId);
  }

  @Override
  public ResponseEntity<Void> updateMember(Long memberId) {
    return MembersApi.super.updateMember(memberId);
  }
}
