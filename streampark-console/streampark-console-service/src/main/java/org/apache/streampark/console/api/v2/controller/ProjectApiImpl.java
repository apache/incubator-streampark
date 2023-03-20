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

import org.apache.streampark.console.api.controller.ProjectsApi;

import org.springframework.http.ResponseEntity;

public class ProjectApiImpl implements ProjectsApi {

  @Override
  public ResponseEntity<Void> buildProject(Long projectId) {
    return ProjectsApi.super.buildProject(projectId);
  }

  @Override
  public ResponseEntity<Void> checkProject(Long projectId) {
    return ProjectsApi.super.checkProject(projectId);
  }

  @Override
  public ResponseEntity<Void> createProject() {
    return ProjectsApi.super.createProject();
  }

  @Override
  public ResponseEntity<Void> deleteProject(Long projectId) {
    return ProjectsApi.super.deleteProject(projectId);
  }

  @Override
  public ResponseEntity<Void> getProject(Long projectId) {
    return ProjectsApi.super.getProject(projectId);
  }

  @Override
  public ResponseEntity<Void> getProjectBuildLog(Long projectId) {
    return ProjectsApi.super.getProjectBuildLog(projectId);
  }

  @Override
  public ResponseEntity<Void> listProjectConfiguration(Long projectId) {
    return ProjectsApi.super.listProjectConfiguration(projectId);
  }

  @Override
  public ResponseEntity<Void> listProjectGitBranches(Long projectId) {
    return ProjectsApi.super.listProjectGitBranches(projectId);
  }

  @Override
  public ResponseEntity<Void> listProjectJar(Long projectId) {
    return ProjectsApi.super.listProjectJar(projectId);
  }

  @Override
  public ResponseEntity<Void> listProjectModule(Long projectId) {
    return ProjectsApi.super.listProjectModule(projectId);
  }

  @Override
  public ResponseEntity<Void> updateProject(Long projectId) {
    return ProjectsApi.super.updateProject(projectId);
  }

  @Override
  public ResponseEntity<Void> verifyProjectGitAuthorization(Long projectId) {
    return ProjectsApi.super.verifyProjectGitAuthorization(projectId);
  }

  @Override
  public ResponseEntity<Void> listProject() {
    return ProjectsApi.super.listProject();
  }
}
