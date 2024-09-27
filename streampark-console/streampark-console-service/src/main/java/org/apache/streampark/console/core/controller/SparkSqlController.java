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

import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.base.domain.RestResponse;
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.base.exception.InternalException;
import org.apache.streampark.console.core.annotation.Permission;
import org.apache.streampark.console.core.entity.FlinkApplication;
import org.apache.streampark.console.core.entity.SparkSql;
import org.apache.streampark.console.core.service.SparkSqlService;
import org.apache.streampark.console.core.service.SqlCompleteService;
import org.apache.streampark.console.core.service.VariableService;
import org.apache.streampark.spark.core.util.SparkSqlValidationResult;

import org.apache.shiro.authz.annotation.RequiresPermissions;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotNull;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("spark/sql")
public class SparkSqlController {

    public static final String TYPE = "type";
    public static final String START = "start";
    public static final String END = "end";

    @Autowired
    private SparkSqlService sparkSqlService;

    @Autowired
    private VariableService variableService;

    @Autowired
    private SqlCompleteService sqlComplete;

    @PostMapping("verify")
    public RestResponse verify(String sql, Long versionId, Long teamId) {
        sql = variableService.replaceVariable(teamId, sql);
        SparkSqlValidationResult sparkSqlValidationResult = sparkSqlService.verifySql(sql, versionId);
        if (!sparkSqlValidationResult.success()) {
            // record error type, such as error sql, reason and error start/end line
            String exception = sparkSqlValidationResult.exception();
            RestResponse response = RestResponse.success()
                .data(false)
                .message(exception)
                .put(TYPE, sparkSqlValidationResult.failedType().getFailedType())
                .put(START, sparkSqlValidationResult.lineStart())
                .put(END, sparkSqlValidationResult.lineEnd());

            if (sparkSqlValidationResult.errorLine() > 0) {
                response
                    .put(START, sparkSqlValidationResult.errorLine())
                    .put(END, sparkSqlValidationResult.errorLine() + 1);
            }
            return response;
        }
        return RestResponse.success(true);
    }

    @PostMapping("list")
    @Permission(app = "#sparkSql.appId", team = "#sparkSql.teamId")
    public RestResponse list(SparkSql sparkSql, RestRequest request) {
        IPage<SparkSql> page = sparkSqlService.getPage(sparkSql.getAppId(), request);
        return RestResponse.success(page);
    }

    @PostMapping("delete")
    @RequiresPermissions("sql:delete")
    @Permission(app = "#sparkSql.appId", team = "#sparkSql.teamId")
    public RestResponse delete(SparkSql sparkSql) {
        Boolean deleted = sparkSqlService.removeById(sparkSql.getSql());
        return RestResponse.success(deleted);
    }

    @PostMapping("get")
    @Permission(app = "#appId", team = "#teamId")
    public RestResponse get(Long appId, Long teamId, String id) throws InternalException {
        ApiAlertException.throwIfTrue(
            appId == null || teamId == null, "Permission denied, appId and teamId cannot be null");
        String[] array = id.split(",");
        SparkSql sparkSql1 = sparkSqlService.getById(array[0]);
        sparkSql1.base64Encode();
        if (array.length == 1) {
            return RestResponse.success(sparkSql1);
        }
        SparkSql sparkSql2 = sparkSqlService.getById(array[1]);
        sparkSql2.base64Encode();
        return RestResponse.success(new SparkSql[]{sparkSql1, sparkSql2});
    }

    @PostMapping("history")
    @Permission(app = "#app.id", team = "#app.teamId")
    public RestResponse history(FlinkApplication app) {
        List<SparkSql> sqlList = sparkSqlService.listSparkSqlHistory(app.getId());
        return RestResponse.success(sqlList);
    }

    @PostMapping("sqlComplete")
    public RestResponse getSqlComplete(@NotNull(message = "{required}") String sql) {
        return RestResponse.success().put("word", sqlComplete.getComplete(sql));
    }
}
