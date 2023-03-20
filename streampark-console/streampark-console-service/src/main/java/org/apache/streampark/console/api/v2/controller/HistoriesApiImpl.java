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

import org.apache.streampark.console.api.controller.HistoriesApi;

import org.springframework.http.ResponseEntity;

public class HistoriesApiImpl implements HistoriesApi {

  @Override
  public ResponseEntity<Void> listFlinkBaseImageHistory() {
    return HistoriesApi.super.listFlinkBaseImageHistory();
  }

  @Override
  public ResponseEntity<Void> listJarHistory() {
    return HistoriesApi.super.listJarHistory();
  }

  @Override
  public ResponseEntity<Void> listJmPodTemplateHistory() {
    return HistoriesApi.super.listJmPodTemplateHistory();
  }

  @Override
  public ResponseEntity<Void> listK8sNamespaceHistory() {
    return HistoriesApi.super.listK8sNamespaceHistory();
  }

  @Override
  public ResponseEntity<Void> listPodTemplateHistory() {
    return HistoriesApi.super.listPodTemplateHistory();
  }

  @Override
  public ResponseEntity<Void> listSessionClusterHistory() {
    return HistoriesApi.super.listSessionClusterHistory();
  }

  @Override
  public ResponseEntity<Void> listTmPodTemplateHistory() {
    return HistoriesApi.super.listTmPodTemplateHistory();
  }
}
