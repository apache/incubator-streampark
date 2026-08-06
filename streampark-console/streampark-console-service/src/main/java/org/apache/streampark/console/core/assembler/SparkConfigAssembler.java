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

import org.apache.streampark.console.core.entity.SparkApplicationConfig;
import org.apache.streampark.console.core.request.spark.SparkConfListQueryRequest;
import org.apache.streampark.console.core.response.spark.SparkConfResponse;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

/**
 * Converts between Spark application config entities and API request/response contracts.
 */
public final class SparkConfigAssembler {

    private SparkConfigAssembler() {
    }

    public static SparkApplicationConfig toEntity(SparkConfListQueryRequest request) {
        return DtoAssembler.toDto(request, SparkApplicationConfig.class);
    }

    public static SparkConfResponse toResponse(SparkApplicationConfig config) {
        if (config == null) {
            return null;
        }
        SparkConfResponse response = DtoAssembler.toDto(config, SparkConfResponse.class);
        response.setEffective(config.isEffective());
        return response;
    }

    public static IPage<SparkConfResponse> toPageResponse(IPage<SparkApplicationConfig> page) {
        return DtoAssembler.toPage(page, SparkConfigAssembler::toResponse);
    }

    public static List<SparkConfResponse> toListResponse(List<SparkApplicationConfig> configs) {
        return DtoAssembler.toList(configs, SparkConfigAssembler::toResponse);
    }
}
