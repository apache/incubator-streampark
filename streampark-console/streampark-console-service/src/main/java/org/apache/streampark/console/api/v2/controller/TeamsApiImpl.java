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

import org.apache.streampark.console.api.controller.TeamsApi;

import org.springframework.http.ResponseEntity;

public class TeamsApiImpl implements TeamsApi {

  @Override
  public ResponseEntity<Void> checkTeam(String teamName) {
    return TeamsApi.super.checkTeam(teamName);
  }

  @Override
  public ResponseEntity<Void> createTeam() {
    return TeamsApi.super.createTeam();
  }

  @Override
  public ResponseEntity<Void> deleteTeam(Long teamId) {
    return TeamsApi.super.deleteTeam(teamId);
  }

  @Override
  public ResponseEntity<Void> listCandidateUser(Long teamId) {
    return TeamsApi.super.listCandidateUser(teamId);
  }

  @Override
  public ResponseEntity<Void> listTeamUser(Long teamId) {
    return TeamsApi.super.listTeamUser(teamId);
  }

  @Override
  public ResponseEntity<Void> listTeams() {
    return TeamsApi.super.listTeams();
  }

  @Override
  public ResponseEntity<Void> updateTeam(Long teamId) {
    return TeamsApi.super.updateTeam(teamId);
  }
}
