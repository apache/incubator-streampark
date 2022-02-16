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
import com.streamxhub.streamx.console.core.service.FlinkClusterService;
import lombok.extern.slf4j.Slf4j;
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
    public RestResponse create(FlinkCluster cluster) {
        boolean saved = flinkClusterService.save(cluster);
        return RestResponse.create().data(saved);
    }

    @PostMapping("update")
    public RestResponse update(FlinkCluster cluster) {
        FlinkCluster flinkCluster = flinkClusterService.getById(cluster.getId());
        flinkCluster.setClusterName(cluster.getClusterName());
        flinkCluster.setAddress(cluster.getAddress());
        flinkCluster.setDescription(cluster.getDescription());
        boolean updated = flinkClusterService.updateById(flinkCluster);
        return RestResponse.create().data(updated);
    }

    @PostMapping("get")
    public RestResponse get(Long id) throws ServiceException {
        FlinkCluster cluster = flinkClusterService.getById(id);
        return RestResponse.create().data(cluster);
    }

}
