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
import org.apache.streampark.console.base.domain.Result;
import org.apache.streampark.console.base.exception.InternalException;
import org.apache.streampark.console.core.entity.FlinkCluster;
import org.apache.streampark.console.core.service.FlinkClusterService;
import org.apache.streampark.console.core.util.ServiceHelper;

import org.apache.shiro.authz.annotation.RequiresPermissions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("flink/cluster")
public class FlinkClusterController {

    @Autowired
    private FlinkClusterService flinkClusterService;

    @PostMapping("alive")
    public Result<List<FlinkCluster>> listAvailableCluster() {
        List<FlinkCluster> flinkClusters = flinkClusterService.listAvailableCluster();
        return Result.success(flinkClusters);
    }

    @PostMapping("list")
    public Result<List<FlinkCluster>> list() {
        List<FlinkCluster> flinkClusters = flinkClusterService.list();
        return Result.success(flinkClusters);
    }

    @PostMapping("remote_url")
    public Result<String> remoteUrl(Long id) {
        FlinkCluster cluster = flinkClusterService.getById(id);
        return Result.success(cluster.getAddress());
    }

    @PostMapping("check")
    public Result<Integer> check(FlinkCluster cluster) {
        return flinkClusterService.check(cluster);
    }

    @PostMapping("create")
    @RequiresPermissions("cluster:create")
    public Result<Boolean> create(FlinkCluster cluster) {
        Long userId = ServiceHelper.getUserId();
        Boolean success = flinkClusterService.create(cluster, userId);
        return Result.success(success);
    }

    @PostMapping("update")
    @RequiresPermissions("cluster:update")
    public Result<Void> update(FlinkCluster cluster) {
        flinkClusterService.update(cluster);
        return Result.success();
    }

    @PostMapping("get")
    public Result<FlinkCluster> get(Long id) throws InternalException {
        FlinkCluster cluster = flinkClusterService.getById(id);
        return Result.success(cluster);
    }

    @PostMapping("start")
    public Result<Void> start(FlinkCluster cluster) {
        flinkClusterService.updateClusterState(cluster.getId(), ClusterState.STARTING);
        flinkClusterService.start(cluster);
        return Result.success();
    }

    @PostMapping("shutdown")
    public Result<Void> shutdown(FlinkCluster cluster) {
        if (flinkClusterService.allowShutdownCluster(cluster)) {
            flinkClusterService.updateClusterState(cluster.getId(), ClusterState.CANCELLING);
            flinkClusterService.shutdown(cluster);
        }
        return Result.success();
    }

    @PostMapping("delete")
    public Result<Void> delete(FlinkCluster cluster) {
        flinkClusterService.remove(cluster.getId());
        return Result.success();
    }
}
