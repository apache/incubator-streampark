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

import org.apache.streampark.console.api.controller.FlinkClustersApi;

import org.springframework.http.ResponseEntity;

public class FlinkClustersApiImpl implements FlinkClustersApi {

  @Override
  public ResponseEntity<Void> createFlinkCluster() {
    return FlinkClustersApi.super.createFlinkCluster();
  }

  @Override
  public ResponseEntity<Void> deleteFlinkCluster(Long clusterId) {
    return FlinkClustersApi.super.deleteFlinkCluster(clusterId);
  }

  @Override
  public ResponseEntity<Void> getFlinkCluster(Long clusterId) {
    return FlinkClustersApi.super.getFlinkCluster(clusterId);
  }

  @Override
  public ResponseEntity<Void> getFlinkClusterAddress(Long clusterId) {
    return FlinkClustersApi.super.getFlinkClusterAddress(clusterId);
  }

  @Override
  public ResponseEntity<Void> getFlinkClusterStatus(Long clusterId) {
    return FlinkClustersApi.super.getFlinkClusterStatus(clusterId);
  }

  @Override
  public ResponseEntity<Void> listFlinkCluster() {
    return FlinkClustersApi.super.listFlinkCluster();
  }

  @Override
  public ResponseEntity<Void> shutdownFlinkCluster(Long clusterId) {
    return FlinkClustersApi.super.shutdownFlinkCluster(clusterId);
  }

  @Override
  public ResponseEntity<Void> startFlinkCluster(Long clusterId) {
    return FlinkClustersApi.super.startFlinkCluster(clusterId);
  }

  @Override
  public ResponseEntity<Void> updateFlinkCluster(Long clusterId) {
    return FlinkClustersApi.super.updateFlinkCluster(clusterId);
  }
}
