/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.console.core.controller;

import com.streamxhub.streamx.console.base.domain.RestResponse;
import com.streamxhub.streamx.console.base.exception.ServiceException;
import com.streamxhub.streamx.console.core.entity.FlinkCluster;
import com.streamxhub.streamx.console.core.entity.ResponseResult;
import com.streamxhub.streamx.console.core.service.FlinkClusterService;

import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.MalformedURLException;
import java.util.List;

/**
 * @author benjobs
 */
@Slf4j
@Validated
@RestController
@RequestMapping("flink/cluster")
public class FlinkClusterController {

    @Autowired
    private FlinkClusterService flinkClusterService;

    @PostMapping("list")
    public RestResponse list() {
        List<FlinkCluster> flinkClusters = flinkClusterService.list();
        return RestResponse.create().data(flinkClusters);
    }

    @PostMapping("activeUrl")
    public RestResponse activeUrl(Long id) throws MalformedURLException {
        FlinkCluster cluster = flinkClusterService.getById(id);
        return RestResponse.create().data(cluster.getActiveAddress().toURL());
    }

    @PostMapping("check")
    public RestResponse check(FlinkCluster cluster) {
        String checkResult = flinkClusterService.check(cluster);
        return RestResponse.create().data(checkResult);
    }

    @PostMapping("create")
    @RequiresPermissions("cluster:create")
    public RestResponse create(FlinkCluster cluster) {
        ResponseResult result = flinkClusterService.create(cluster);
        return RestResponse.create().data(result);
    }

    @PostMapping("update")
    @RequiresPermissions("cluster:update")
    public RestResponse update(FlinkCluster cluster) {
        FlinkCluster flinkCluster = flinkClusterService.getById(cluster.getId());
        flinkCluster.setClusterId(cluster.getClusterId());
        flinkCluster.setClusterName(cluster.getClusterName());
        flinkCluster.setAddress(cluster.getAddress());
        flinkCluster.setExecutionMode(cluster.getExecutionMode());
        flinkCluster.setDynamicOptions(cluster.getDynamicOptions());
        flinkCluster.setFlameGraph(cluster.getFlameGraph());
        flinkCluster.setFlinkImage(cluster.getFlinkImage());
        flinkCluster.setOptions(cluster.getOptions());
        flinkCluster.setYarnQueue(cluster.getYarnQueue());
        flinkCluster.setK8sHadoopIntegration(cluster.getK8sHadoopIntegration());
        flinkCluster.setK8sConf(cluster.getK8sConf());
        flinkCluster.setK8sNamespace(cluster.getK8sNamespace());
        flinkCluster.setK8sRestExposedType(cluster.getK8sRestExposedType());
        flinkCluster.setResolveOrder(cluster.getResolveOrder());
        flinkCluster.setServiceAccount(cluster.getServiceAccount());
        flinkCluster.setDescription(cluster.getDescription());
        return RestResponse.create().data(flinkClusterService.update(flinkCluster));
    }

    @PostMapping("get")
    public RestResponse get(Long id) throws ServiceException {
        FlinkCluster cluster = flinkClusterService.getById(id);
        return RestResponse.create().data(cluster);
    }

    @PostMapping("start")
    public RestResponse start(FlinkCluster flinkCluster) {
        FlinkCluster cluster = flinkClusterService.getById(flinkCluster.getId());
        ResponseResult start = flinkClusterService.start(cluster);
        return RestResponse.create().data(start);
    }

    @PostMapping("shutdown")
    public RestResponse shutdown(FlinkCluster flinkCluster) {
        FlinkCluster cluster = flinkClusterService.getById(flinkCluster.getId());
        ResponseResult shutdown = flinkClusterService.shutdown(cluster);
        return RestResponse.create().data(shutdown);
    }

    @PostMapping("delete")
    public RestResponse delete(FlinkCluster flinkCluster) {
        FlinkCluster cluster = flinkClusterService.getById(flinkCluster.getId());
        ResponseResult delete = flinkClusterService.delete(cluster);
        return RestResponse.create().data(delete);
    }
}
