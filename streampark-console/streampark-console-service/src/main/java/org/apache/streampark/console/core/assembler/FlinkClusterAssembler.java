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

package org.apache.streampark.console.core.assembler;

import org.apache.streampark.console.core.bean.ResponseResult;
import org.apache.streampark.console.core.entity.FlinkCluster;
import org.apache.streampark.console.core.request.common.IdRequest;
import org.apache.streampark.console.core.request.flink.FlinkClusterCheckRequest;
import org.apache.streampark.console.core.request.flink.FlinkClusterCreateRequest;
import org.apache.streampark.console.core.request.flink.FlinkClusterPageQueryRequest;
import org.apache.streampark.console.core.request.flink.FlinkClusterUpdateRequest;
import org.apache.streampark.console.core.response.flink.FlinkClusterCheckResponse;
import org.apache.streampark.console.core.response.flink.FlinkClusterResponse;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Converts between Flink cluster entities and API request/response contracts.
 */
public final class FlinkClusterAssembler {

    private FlinkClusterAssembler() {
    }

    public static FlinkCluster toEntity(FlinkClusterCreateRequest request) {
        if (request == null) {
            return null;
        }
        FlinkCluster cluster = new FlinkCluster();
        BeanUtils.copyProperties(request, cluster);
        return cluster;
    }

    public static FlinkCluster toEntity(FlinkClusterUpdateRequest request) {
        if (request == null) {
            return null;
        }
        FlinkCluster cluster = toEntity((FlinkClusterCreateRequest) request);
        cluster.setId(request.getId());
        return cluster;
    }

    public static FlinkCluster toEntity(FlinkClusterCheckRequest request) {
        if (request == null) {
            return null;
        }
        FlinkCluster cluster = toEntity((FlinkClusterCreateRequest) request);
        cluster.setId(request.getId());
        return cluster;
    }

    public static FlinkCluster toEntity(FlinkClusterPageQueryRequest request) {
        if (request == null) {
            return null;
        }
        FlinkCluster cluster = new FlinkCluster();
        cluster.setClusterName(request.getClusterName());
        return cluster;
    }

    public static FlinkCluster toEntity(IdRequest request) {
        if (request == null) {
            return null;
        }
        FlinkCluster cluster = new FlinkCluster();
        cluster.setId(request.getId());
        return cluster;
    }

    public static FlinkClusterResponse toResponse(FlinkCluster cluster) {
        if (cluster == null) {
            return null;
        }
        FlinkClusterResponse response = new FlinkClusterResponse();
        BeanUtils.copyProperties(cluster, response);
        return response;
    }

    public static List<FlinkClusterResponse> toListResponse(List<FlinkCluster> clusters) {
        if (clusters == null) {
            return Collections.emptyList();
        }
        return clusters.stream().map(FlinkClusterAssembler::toResponse).collect(Collectors.toList());
    }

    public static IPage<FlinkClusterResponse> toPageResponse(IPage<FlinkCluster> page) {
        return DtoAssembler.toPage(page, FlinkClusterAssembler::toResponse);
    }

    public static FlinkClusterCheckResponse toCheckResponse(ResponseResult<?> checkResult) {
        if (checkResult == null) {
            return null;
        }
        FlinkClusterCheckResponse response = new FlinkClusterCheckResponse();
        response.setStatus(checkResult.getStatus());
        response.setMsg(checkResult.getMsg());
        response.setResult((Serializable) checkResult.getResult());
        return response;
    }
}
