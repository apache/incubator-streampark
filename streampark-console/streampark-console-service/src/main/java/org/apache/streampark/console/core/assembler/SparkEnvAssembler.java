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

import org.apache.streampark.console.core.entity.SparkEnv;
import org.apache.streampark.console.core.request.spark.SparkEnvCheckRequest;
import org.apache.streampark.console.core.request.spark.SparkEnvCreateRequest;
import org.apache.streampark.console.core.request.spark.SparkEnvUpdateRequest;
import org.apache.streampark.console.core.response.spark.SparkEnvResponse;

import java.util.List;

/**
 * Converts between Spark environment entities and API request/response contracts.
 */
public final class SparkEnvAssembler {

    private SparkEnvAssembler() {
    }

    public static SparkEnv toEntity(SparkEnvCreateRequest request) {
        return DtoAssembler.toDto(request, SparkEnv.class);
    }

    public static SparkEnv toEntity(SparkEnvUpdateRequest request) {
        if (request == null) {
            return null;
        }
        SparkEnv env = toEntity((SparkEnvCreateRequest) request);
        env.setId(request.getId());
        return env;
    }

    public static SparkEnv toEntity(SparkEnvCheckRequest request) {
        return DtoAssembler.toDto(request, SparkEnv.class);
    }

    public static SparkEnvResponse toResponse(SparkEnv env) {
        return DtoAssembler.toDto(env, SparkEnvResponse.class);
    }

    public static List<SparkEnvResponse> toListResponse(List<SparkEnv> envs) {
        return DtoAssembler.toList(envs, SparkEnvAssembler::toResponse);
    }
}
