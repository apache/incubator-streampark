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

import org.apache.streampark.console.base.domain.RestResponse;
import org.apache.streampark.console.base.exception.InternalException;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.entity.FlinkSql;
import org.apache.streampark.console.core.service.FlinkSqlService;
import org.apache.streampark.console.core.service.SqlCompleteService;
import org.apache.streampark.console.core.service.VariableService;
import org.apache.streampark.flink.core.FlinkSqlValidationResult;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotNull;

import java.util.List;

@Api(tags = "[flink sql] related operations", consumes = "Content-Type=application/x-www-form-urlencoded")
@Slf4j
@Validated
@RestController
@RequestMapping("flink/sql")
public class FlinkSqlController {

    @Autowired
    private FlinkSqlService flinkSqlService;

    @Autowired
    private VariableService variableService;

    @Autowired
    private SqlCompleteService sqlComplete;

    @PostMapping("verify")
    public RestResponse verify(String sql, Long versionId, Long teamId) {
        sql = variableService.replaceVariable(teamId, sql);
        FlinkSqlValidationResult flinkSqlValidationResult = flinkSqlService.verifySql(sql, versionId);
        if (!flinkSqlValidationResult.success()) {
            // record error type, such as error sql, reason and error start/end line
            String exception = flinkSqlValidationResult.exception();
            RestResponse response = RestResponse.success()
                .data(false)
                .message(exception)
                .put("type", flinkSqlValidationResult.failedType().getValue())
                .put("start", flinkSqlValidationResult.lineStart())
                .put("end", flinkSqlValidationResult.lineEnd());

            if (flinkSqlValidationResult.errorLine() > 0) {
                response
                    .put("start", flinkSqlValidationResult.errorLine())
                    .put("end", flinkSqlValidationResult.errorLine() + 1);
            }
            return response;
        } else {
            return RestResponse.success(true);
        }
    }

    @PostMapping("get")
    public RestResponse get(String id) throws InternalException {
        String[] array = id.split(",");
        FlinkSql flinkSql1 = flinkSqlService.getById(array[0]);
        flinkSql1.base64Encode();
        if (array.length == 1) {
            return RestResponse.success(flinkSql1);
        }
        FlinkSql flinkSql2 = flinkSqlService.getById(array[1]);
        flinkSql2.base64Encode();
        return RestResponse.success(new FlinkSql[]{flinkSql1, flinkSql2});
    }

    @PostMapping("history")
    public RestResponse sqlhistory(Application application) {
        List<FlinkSql> sqlList = flinkSqlService.history(application);
        return RestResponse.success(sqlList);
    }

    @PostMapping("sqlComplete")
    public RestResponse getSqlComplete(@NotNull(message = "{required}") String sql) {
        return RestResponse.success().put("word", sqlComplete.getComplete(sql));
    }

}
