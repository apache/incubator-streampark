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

import org.apache.streampark.console.core.entity.FlinkApplicationConfig;
import org.apache.streampark.console.core.request.common.IdRequest;
import org.apache.streampark.console.core.request.flink.FlinkAppIdRequest;
import org.apache.streampark.console.core.request.flink.FlinkConfListQueryRequest;
import org.apache.streampark.console.core.response.flink.FlinkConfHadoopResponse;
import org.apache.streampark.console.core.response.flink.FlinkConfResponse;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.BeanUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Converts between Flink application config entities and API request/response contracts.
 */
public final class FlinkConfAssembler {

    private FlinkConfAssembler() {
    }

    public static FlinkApplicationConfig toEntity(FlinkConfListQueryRequest request) {
        if (request == null) {
            return null;
        }
        FlinkApplicationConfig config = new FlinkApplicationConfig();
        config.setAppId(request.getAppId());
        return config;
    }

    public static FlinkApplicationConfig toEntity(IdRequest request) {
        if (request == null) {
            return null;
        }
        FlinkApplicationConfig config = new FlinkApplicationConfig();
        config.setId(request.getId());
        return config;
    }

    public static Long toAppId(FlinkAppIdRequest request) {
        return request == null ? null : request.getId();
    }

    public static FlinkConfResponse toResponse(FlinkApplicationConfig config) {
        if (config == null) {
            return null;
        }
        FlinkConfResponse response = new FlinkConfResponse();
        BeanUtils.copyProperties(config, response);
        return response;
    }

    public static List<FlinkConfResponse> toListResponse(List<FlinkApplicationConfig> configs) {
        if (configs == null) {
            return Collections.emptyList();
        }
        return configs.stream().map(FlinkConfAssembler::toResponse).collect(Collectors.toList());
    }

    public static IPage<FlinkConfResponse> toPageResponse(IPage<FlinkApplicationConfig> page) {
        return DtoAssembler.toPage(page, FlinkConfAssembler::toResponse);
    }

    public static FlinkConfHadoopResponse toHadoopResponse(Map<String, Map<String, String>> hadoopConf) {
        if (hadoopConf == null) {
            return null;
        }
        FlinkConfHadoopResponse response = new FlinkConfHadoopResponse();
        response.setHadoop(hadoopConf.get("hadoop"));
        response.setHive(hadoopConf.get("hive"));
        return response;
    }
}
