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

import org.apache.streampark.console.api.controller.FlinkApplicationsApi;
import org.apache.streampark.console.api.controller.model.CreateFlinkApplicationRequest;
import org.apache.streampark.console.api.controller.model.FlinkApplication;
import org.apache.streampark.console.api.controller.model.ListFlinkApplication;
import org.apache.streampark.console.core.service.ApplicationService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class FlinkApplicationsApiImpl implements FlinkApplicationsApi {

  @Autowired ApplicationService applicationService;

  @Override
  public ResponseEntity<Void> cancelApplication(Long applicationId) {
    return FlinkApplicationsApi.super.cancelApplication(applicationId);
  }

  @Override
  public ResponseEntity<Void> checkApplicationSavepointPath(
      Long applicationId, String savepointPath) {
    return FlinkApplicationsApi.super.checkApplicationSavepointPath(applicationId, savepointPath);
  }

  @Override
  public ResponseEntity<Void> checkFlinkApplication(Long applicationId) {
    return FlinkApplicationsApi.super.checkFlinkApplication(applicationId);
  }

  @Override
  public ResponseEntity<FlinkApplication> copyFlinkApplication(Long applicationId) {
    return FlinkApplicationsApi.super.copyFlinkApplication(applicationId);
  }

  @Override
  public ResponseEntity<FlinkApplication> createFlinkApplication(
      CreateFlinkApplicationRequest createFlinkApplicationRequest) {
    return FlinkApplicationsApi.super.createFlinkApplication(createFlinkApplicationRequest);
  }

  @Override
  public ResponseEntity<Void> deleteApplicationBackup(Long applicationId, Long backupId) {
    return FlinkApplicationsApi.super.deleteApplicationBackup(applicationId, backupId);
  }

  @Override
  public ResponseEntity<Void> deleteApplicationOperationLog(Long applicationId, Long operationId) {
    return FlinkApplicationsApi.super.deleteApplicationOperationLog(applicationId, operationId);
  }

  @Override
  public ResponseEntity<Void> deleteFlinkApplication(Long applicationId) {
    return FlinkApplicationsApi.super.deleteFlinkApplication(applicationId);
  }

  @Override
  public ResponseEntity<Void> forceStopApplication(Long applicationId) {
    return FlinkApplicationsApi.super.forceStopApplication(applicationId);
  }

  @Override
  public ResponseEntity<Void> getApplicationConf(Long applicationId) {
    return FlinkApplicationsApi.super.getApplicationConf(applicationId);
  }

  @Override
  public ResponseEntity<Void> getApplicationK8sStartLog(Long applicationId) {
    return FlinkApplicationsApi.super.getApplicationK8sStartLog(applicationId);
  }

  @Override
  public ResponseEntity<Void> getApplicationMainClass(Long applicationId) {
    return FlinkApplicationsApi.super.getApplicationMainClass(applicationId);
  }

  @Override
  public ResponseEntity<Void> getApplicationOperationLogs(Long applicationId) {
    return FlinkApplicationsApi.super.getApplicationOperationLogs(applicationId);
  }

  @Override
  public ResponseEntity<Void> getApplicationReleasePipeline(Long applicationId) {
    return FlinkApplicationsApi.super.getApplicationReleasePipeline(applicationId);
  }

  @Override
  public ResponseEntity<Void> getApplicationYarnName(Long applicationId) {
    return FlinkApplicationsApi.super.getApplicationYarnName(applicationId);
  }

  @Override
  public ResponseEntity<Void> getApplicationYarnProxy(Long applicationId) {
    return FlinkApplicationsApi.super.getApplicationYarnProxy(applicationId);
  }

  @Override
  public ResponseEntity<Void> getApplicationsDashboard(Long teamId) {
    return FlinkApplicationsApi.super.getApplicationsDashboard(teamId);
  }

  @Override
  public ResponseEntity<FlinkApplication> getFlinkApplication(Long applicationId) {
    return FlinkApplicationsApi.super.getFlinkApplication(applicationId);
  }

  @Override
  public ResponseEntity<Void> getLatestSavepoint(Long applicationId) {
    return FlinkApplicationsApi.super.getLatestSavepoint(applicationId);
  }

  @Override
  public ResponseEntity<Void> listApplicationBackups() {
    return FlinkApplicationsApi.super.listApplicationBackups();
  }

  @Override
  public ResponseEntity<Void> listApplicationConfig(Long applicationId) {
    return FlinkApplicationsApi.super.listApplicationConfig(applicationId);
  }

  @Override
  public ResponseEntity<Void> listApplicationConfigHistory(Long applicationId) {
    return FlinkApplicationsApi.super.listApplicationConfigHistory(applicationId);
  }

  @Override
  public ResponseEntity<Void> listApplicationExternalLink(Long applicationId) {
    return FlinkApplicationsApi.super.listApplicationExternalLink(applicationId);
  }

  @Override
  public ResponseEntity<Void> listApplicationSqlHistory(Long applicationId) {
    return FlinkApplicationsApi.super.listApplicationSqlHistory(applicationId);
  }

  @Override
  public ResponseEntity<ListFlinkApplication> listFlinkApplication(
      Long teamId, Integer offset, Integer limit) {
    return FlinkApplicationsApi.super.listFlinkApplication(teamId, offset, limit);
  }

  @Override
  public ResponseEntity<Void> listSavepointHistory(Long applicationId) {
    return FlinkApplicationsApi.super.listSavepointHistory(applicationId);
  }

  @Override
  public ResponseEntity<Void> releaseApplication(Long applicationId) {
    return FlinkApplicationsApi.super.releaseApplication(applicationId);
  }

  @Override
  public ResponseEntity<Void> revokeApplication(Long applicationId) {
    return FlinkApplicationsApi.super.revokeApplication(applicationId);
  }

  @Override
  public ResponseEntity<Void> startApplication(Long applicationId) {
    return FlinkApplicationsApi.super.startApplication(applicationId);
  }

  @Override
  public ResponseEntity<Void> triggerSavepoint(Long applicationId) {
    return FlinkApplicationsApi.super.triggerSavepoint(applicationId);
  }

  @Override
  public ResponseEntity<Void> mappingApplication(Long applicationId) {
    return FlinkApplicationsApi.super.mappingApplication(applicationId);
  }

  @Override
  public ResponseEntity<FlinkApplication> updateFlinkApplication(Long applicationId) {
    return FlinkApplicationsApi.super.updateFlinkApplication(applicationId);
  }

  @Override
  public ResponseEntity<Void> uploadApplicationJar(MultipartFile applicationJar) {
    return FlinkApplicationsApi.super.uploadApplicationJar(applicationJar);
  }
}
