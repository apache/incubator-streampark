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
import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.base.domain.RestResponseBody;
import org.apache.streampark.console.base.exception.InternalException;
import org.apache.streampark.console.base.web.FormOrJson;
import org.apache.streampark.console.core.assembler.FlinkClusterAssembler;
import org.apache.streampark.console.core.bean.ResponseResult;
import org.apache.streampark.console.core.entity.FlinkCluster;
import org.apache.streampark.console.core.request.common.IdRequest;
import org.apache.streampark.console.core.request.flink.FlinkClusterCheckRequest;
import org.apache.streampark.console.core.request.flink.FlinkClusterCreateRequest;
import org.apache.streampark.console.core.request.flink.FlinkClusterPageQueryRequest;
import org.apache.streampark.console.core.request.flink.FlinkClusterUpdateRequest;
import org.apache.streampark.console.core.response.flink.FlinkClusterCheckResponse;
import org.apache.streampark.console.core.response.flink.FlinkClusterResponse;
import org.apache.streampark.console.core.service.FlinkClusterService;
import org.apache.streampark.console.core.util.ServiceHelper;

import org.apache.shiro.authz.annotation.RequiresPermissions;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("flink/cluster")
public class FlinkClusterController {

    @Autowired
    private FlinkClusterService flinkClusterService;

    @PostMapping("page")
    public RestResponseBody<IPage<FlinkClusterResponse>> findPage(FlinkClusterPageQueryRequest query,
                                                                  RestRequest restRequest) {
        FlinkCluster flinkCluster = FlinkClusterAssembler.toEntity(query);
        IPage<FlinkCluster> flinkClusters = flinkClusterService.findPage(flinkCluster, restRequest);
        return RestResponseBody.success(FlinkClusterAssembler.toPageResponse(flinkClusters));
    }

    @PostMapping("alive")
    public RestResponseBody<List<FlinkClusterResponse>> listAvailableCluster() {
        List<FlinkCluster> flinkClusters = flinkClusterService.listAvailableCluster();
        return RestResponseBody.success(FlinkClusterAssembler.toListResponse(flinkClusters));
    }

    @PostMapping("list")
    public RestResponseBody<java.util.List<FlinkClusterResponse>> list() {
        List<FlinkCluster> flinkClusters = flinkClusterService.list();
        return RestResponseBody.success(FlinkClusterAssembler.toListResponse(flinkClusters));
    }

    @PostMapping("remote_url")
    public RestResponseBody<String> remoteUrl(@Valid IdRequest request) {
        FlinkCluster cluster = flinkClusterService.getById(request.getId());
        return RestResponseBody.success(cluster.getAddress());
    }

    @PostMapping("check")
    public RestResponseBody<FlinkClusterCheckResponse> check(FlinkClusterCheckRequest request) {
        ResponseResult checkResult = flinkClusterService.check(FlinkClusterAssembler.toEntity(request));
        return RestResponseBody.success(FlinkClusterAssembler.toCheckResponse(checkResult));
    }

    @PostMapping("create")
    @RequiresPermissions("cluster:create")
    public RestResponseBody<Boolean> create(@Valid @FormOrJson FlinkClusterCreateRequest request) {
        Long userId = ServiceHelper.getUserId();
        Boolean success = flinkClusterService.create(FlinkClusterAssembler.toEntity(request), userId);
        return RestResponseBody.success(success);
    }

    @PostMapping("update")
    @RequiresPermissions("cluster:update")
    public RestResponseBody<Void> update(@Valid @FormOrJson FlinkClusterUpdateRequest request) {
        flinkClusterService.update(FlinkClusterAssembler.toEntity(request));
        return RestResponseBody.success();
    }

    @PostMapping("get")
    public RestResponseBody<FlinkClusterResponse> get(@Valid IdRequest request) throws InternalException {
        FlinkCluster cluster = flinkClusterService.getById(request.getId());
        return RestResponseBody.success(FlinkClusterAssembler.toResponse(cluster));
    }

    @PostMapping("start")
    public RestResponseBody<Void> start(@Valid @FormOrJson IdRequest request) {
        flinkClusterService.updateClusterState(request.getId(), ClusterState.STARTING);
        flinkClusterService.start(FlinkClusterAssembler.toEntity(request));
        return RestResponseBody.success();
    }

    @PostMapping("shutdown")
    public RestResponseBody<Void> shutdown(@Valid @FormOrJson IdRequest request) {
        FlinkCluster cluster = FlinkClusterAssembler.toEntity(request);
        if (cluster != null && flinkClusterService.allowShutdownCluster(cluster)) {
            flinkClusterService.updateClusterState(cluster.getId(), ClusterState.CANCELLING);
            flinkClusterService.shutdown(cluster);
        }
        return RestResponseBody.success();
    }

    @PostMapping("delete")
    public RestResponseBody<Void> delete(@Valid @FormOrJson IdRequest request) {
        flinkClusterService.remove(request.getId());
        return RestResponseBody.success();
    }
}
