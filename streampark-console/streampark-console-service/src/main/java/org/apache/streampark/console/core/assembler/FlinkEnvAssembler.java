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

import org.apache.streampark.console.core.entity.FlinkEnv;
import org.apache.streampark.console.core.request.common.IdRequest;
import org.apache.streampark.console.core.request.flink.FlinkEnvCheckRequest;
import org.apache.streampark.console.core.request.flink.FlinkEnvCreateRequest;
import org.apache.streampark.console.core.request.flink.FlinkEnvPageQueryRequest;
import org.apache.streampark.console.core.request.flink.FlinkEnvUpdateRequest;
import org.apache.streampark.console.core.response.flink.FlinkEnvResponse;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.BeanUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Converts between Flink environment entities and API request/response contracts.
 */
public final class FlinkEnvAssembler {

    private FlinkEnvAssembler() {
    }

    public static FlinkEnv toEntity(FlinkEnvCreateRequest request) {
        if (request == null) {
            return null;
        }
        FlinkEnv env = new FlinkEnv();
        BeanUtils.copyProperties(request, env);
        return env;
    }

    public static FlinkEnv toEntity(FlinkEnvUpdateRequest request) {
        if (request == null) {
            return null;
        }
        FlinkEnv env = toEntity((FlinkEnvCreateRequest) request);
        env.setId(request.getId());
        return env;
    }

    public static FlinkEnv toEntity(FlinkEnvCheckRequest request) {
        if (request == null) {
            return null;
        }
        FlinkEnv env = new FlinkEnv();
        env.setId(request.getId());
        env.setFlinkName(request.getFlinkName());
        env.setFlinkHome(request.getFlinkHome());
        return env;
    }

    public static FlinkEnv toEntity(FlinkEnvPageQueryRequest request) {
        if (request == null) {
            return null;
        }
        FlinkEnv env = new FlinkEnv();
        env.setFlinkName(request.getFlinkName());
        return env;
    }

    public static FlinkEnv toEntity(IdRequest request) {
        if (request == null) {
            return null;
        }
        FlinkEnv env = new FlinkEnv();
        env.setId(request.getId());
        return env;
    }

    public static FlinkEnvResponse toResponse(FlinkEnv env) {
        if (env == null) {
            return null;
        }
        FlinkEnvResponse response = new FlinkEnvResponse();
        BeanUtils.copyProperties(env, response);
        return response;
    }

    public static List<FlinkEnvResponse> toListResponse(List<FlinkEnv> envs) {
        if (envs == null) {
            return Collections.emptyList();
        }
        return envs.stream().map(FlinkEnvAssembler::toResponse).collect(Collectors.toList());
    }

    public static IPage<FlinkEnvResponse> toPageResponse(IPage<FlinkEnv> page) {
        return DtoAssembler.toPage(page, FlinkEnvAssembler::toResponse);
    }
}
