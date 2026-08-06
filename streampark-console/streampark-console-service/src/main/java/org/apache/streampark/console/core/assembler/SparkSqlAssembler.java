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

import org.apache.streampark.console.core.entity.SparkSql;
import org.apache.streampark.console.core.request.spark.SparkSqlDeleteRequest;
import org.apache.streampark.console.core.response.spark.SparkSqlResponse;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

/**
 * Converts between Spark SQL entities and API request/response contracts.
 */
public final class SparkSqlAssembler {

    private SparkSqlAssembler() {
    }

    public static SparkSql toDeleteEntity(SparkSqlDeleteRequest request) {
        if (request == null) {
            return null;
        }
        SparkSql sparkSql = new SparkSql();
        sparkSql.setAppId(request.getAppId());
        sparkSql.setSql(request.getSql());
        return sparkSql;
    }

    public static SparkSqlResponse toResponse(SparkSql sparkSql) {
        if (sparkSql == null) {
            return null;
        }
        SparkSqlResponse response = DtoAssembler.toDto(sparkSql, SparkSqlResponse.class);
        response.setEffective(sparkSql.isEffective());
        response.setSqlDifference(sparkSql.isSqlDifference());
        response.setDependencyDifference(sparkSql.isDependencyDifference());
        return response;
    }

    public static SparkSqlResponse[] toResponseArray(SparkSql[] sparkSqls) {
        if (sparkSqls == null) {
            return new SparkSqlResponse[0];
        }
        SparkSqlResponse[] responses = new SparkSqlResponse[sparkSqls.length];
        for (int i = 0; i < sparkSqls.length; i++) {
            responses[i] = toResponse(sparkSqls[i]);
        }
        return responses;
    }

    public static IPage<SparkSqlResponse> toPageResponse(IPage<SparkSql> page) {
        return DtoAssembler.toPage(page, SparkSqlAssembler::toResponse);
    }

    public static List<SparkSqlResponse> toListResponse(List<SparkSql> sqlList) {
        return DtoAssembler.toList(sqlList, SparkSqlAssembler::toResponse);
    }
}
