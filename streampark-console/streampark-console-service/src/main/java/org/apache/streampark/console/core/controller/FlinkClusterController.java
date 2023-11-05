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

package org.apache.streampark.console.core.controller;

import org.apache.streampark.common.enums.ClusterState;
import org.apache.streampark.console.base.domain.RestResponse;
import org.apache.streampark.console.base.exception.InternalException;
import org.apache.streampark.console.core.bean.ResponseResult;
import org.apache.streampark.console.core.entity.FlinkCluster;
import org.apache.streampark.console.core.service.FlinkClusterService;

import org.apache.shiro.authz.annotation.RequiresPermissions;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "FLINK_CLUSTER_TAG")
@Slf4j
@Validated
@RestController
@RequestMapping("flink/cluster")
public class FlinkClusterController {

  @Autowired private FlinkClusterService flinkClusterService;

  @Operation(summary = "List flink clusters")
  @PostMapping("list")
  public RestResponse list() {
    List<FlinkCluster> flinkClusters = flinkClusterService.list();
    return RestResponse.success(flinkClusters);
  }

  @Operation(summary = "Get flink cluster remote address")
  @PostMapping("remoteUrl")
  public RestResponse remoteUrl(Long id) {
    FlinkCluster cluster = flinkClusterService.getById(id);
    return RestResponse.success(cluster.getAddress());
  }

  @Operation(summary = "Check the cluster status")
  @PostMapping("check")
  public RestResponse check(FlinkCluster cluster) {
    ResponseResult checkResult = flinkClusterService.check(cluster);
    return RestResponse.success(checkResult);
  }

  @Operation(summary = "Create flink cluster")
  @PostMapping("create")
  @RequiresPermissions("cluster:create")
  public RestResponse create(FlinkCluster cluster) {
    Boolean success = flinkClusterService.create(cluster);
    return RestResponse.success(success);
  }

  @Operation(summary = "Update flink cluster")
  @PostMapping("update")
  @RequiresPermissions("cluster:update")
  public RestResponse update(FlinkCluster cluster) {
    flinkClusterService.update(cluster);
    return RestResponse.success();
  }

  @Operation(summary = "Get flink cluster")
  @PostMapping("get")
  public RestResponse get(Long id) throws InternalException {
    FlinkCluster cluster = flinkClusterService.getById(id);
    return RestResponse.success(cluster);
  }

  @Operation(summary = "Start flink cluster")
  @PostMapping("start")
  public RestResponse start(FlinkCluster cluster) {
    flinkClusterService.updateClusterState(cluster.getId(), ClusterState.STARTING);
    flinkClusterService.start(cluster);
    return RestResponse.success();
  }

  @Operation(summary = "Shutdown flink cluster")
  @PostMapping("shutdown")
  public RestResponse shutdown(FlinkCluster cluster) {
    if (flinkClusterService.allowShutdownCluster(cluster)) {
      flinkClusterService.updateClusterState(cluster.getId(), ClusterState.CANCELLING);
      flinkClusterService.shutdown(cluster);
    }
    return RestResponse.success();
  }

  @Operation(summary = "Delete flink cluster")
  @PostMapping("delete")
  public RestResponse delete(FlinkCluster cluster) {
    flinkClusterService.remove(cluster);
    return RestResponse.success();
  }
}
