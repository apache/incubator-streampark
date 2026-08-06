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

import org.apache.streampark.console.core.entity.FlinkSql;
import org.apache.streampark.console.core.request.flink.FlinkAppIdRequest;
import org.apache.streampark.console.core.request.flink.FlinkSqlDeleteRequest;
import org.apache.streampark.console.core.request.flink.FlinkSqlListQueryRequest;
import org.apache.streampark.console.core.response.flink.FlinkSqlResponse;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.BeanUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Converts between Flink SQL entities and API request/response contracts.
 */
public final class FlinkSqlAssembler {

    private FlinkSqlAssembler() {
    }

    public static FlinkSql toQueryEntity(FlinkSqlListQueryRequest request) {
        if (request == null) {
            return null;
        }
        FlinkSql flinkSql = new FlinkSql();
        flinkSql.setAppId(request.getAppId());
        flinkSql.setTeamId(request.getTeamId());
        return flinkSql;
    }

    public static FlinkSql toEntity(FlinkSqlDeleteRequest request) {
        if (request == null) {
            return null;
        }
        FlinkSql flinkSql = new FlinkSql();
        flinkSql.setAppId(request.getAppId());
        flinkSql.setTeamId(request.getTeamId());
        flinkSql.setId(request.getId());
        return flinkSql;
    }

    public static Long toAppId(FlinkAppIdRequest request) {
        return request == null ? null : request.getId();
    }

    public static FlinkSqlResponse toResponse(FlinkSql flinkSql) {
        if (flinkSql == null) {
            return null;
        }
        FlinkSqlResponse response = new FlinkSqlResponse();
        BeanUtils.copyProperties(flinkSql, response);
        return response;
    }

    public static FlinkSqlResponse[] toArrayResponse(FlinkSql[] flinkSqls) {
        if (flinkSqls == null) {
            return new FlinkSqlResponse[0];
        }
        FlinkSqlResponse[] responses = new FlinkSqlResponse[flinkSqls.length];
        for (int i = 0; i < flinkSqls.length; i++) {
            responses[i] = toResponse(flinkSqls[i]);
        }
        return responses;
    }

    public static List<FlinkSqlResponse> toListResponse(List<FlinkSql> flinkSqls) {
        if (flinkSqls == null) {
            return Collections.emptyList();
        }
        return flinkSqls.stream().map(FlinkSqlAssembler::toResponse).collect(Collectors.toList());
    }

    public static IPage<FlinkSqlResponse> toPageResponse(IPage<FlinkSql> page) {
        return DtoAssembler.toPage(page, FlinkSqlAssembler::toResponse);
    }
}
