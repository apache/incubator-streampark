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
import org.apache.streampark.console.base.domain.Result;
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.base.exception.InternalException;
import org.apache.streampark.console.core.annotation.Permission;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.entity.FlinkSql;
import org.apache.streampark.console.core.service.FlinkSqlService;
import org.apache.streampark.console.core.service.SqlCompleteService;
import org.apache.streampark.console.core.service.VariableService;
import org.apache.streampark.flink.core.FlinkSqlValidationResult;

import org.apache.shiro.authz.annotation.RequiresPermissions;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Validated
@RestController
@RequestMapping("flink/sql")
public class FlinkSqlController {

    public static final String TYPE = "type";
    public static final String START = "start";
    public static final String END = "end";

    @Autowired
    private FlinkSqlService flinkSqlService;

    @Autowired
    private VariableService variableService;

    @Autowired
    private SqlCompleteService sqlComplete;

    @PostMapping("verify")
    public Result<Map<String, Integer>> verify(String sql, Long versionId, Long teamId) {
        sql = variableService.replaceVariable(teamId, sql);
        FlinkSqlValidationResult flinkSqlValidationResult = flinkSqlService.verifySql(sql, versionId);

        Map<String, Integer> map = new HashMap<>();
        if (!flinkSqlValidationResult.success()) {
            // record error type, such as error sql, reason and error start/end line
            String exception = flinkSqlValidationResult.exception();
            map.put(TYPE, flinkSqlValidationResult.failedType().getFailedType());
            map.put(START, flinkSqlValidationResult.lineStart());
            map.put(END, flinkSqlValidationResult.lineEnd());

            if (flinkSqlValidationResult.errorLine() > 0) {
                map.put(START, flinkSqlValidationResult.errorLine());
                map.put(END, flinkSqlValidationResult.errorLine() + 1);
            }
            return Result.fail(exception, map);
        }
        return Result.success(null);
    }

    @PostMapping("list")
    @Permission(app = "#flinkSql.appId", team = "#flinkSql.teamId")
    public Result<IPage<FlinkSql>> list(FlinkSql flinkSql, RestRequest request) {
        IPage<FlinkSql> page = flinkSqlService.getPage(flinkSql.getAppId(), request);
        return Result.success(page);
    }

    @PostMapping("delete")
    @RequiresPermissions("sql:delete")
    @Permission(app = "#flinkSql.appId", team = "#flinkSql.teamId")
    public Result<Boolean> delete(FlinkSql flinkSql) {
        Boolean deleted = flinkSqlService.removeById(flinkSql.getSql());
        return Result.success(deleted);
    }

    @PostMapping("get")
    @Permission(app = "#appId", team = "#teamId")
    public Result<FlinkSql[]> get(Long appId, Long teamId, String id) throws InternalException {
        ApiAlertException.throwIfTrue(
            appId == null || teamId == null, "Permission denied, appId and teamId cannot be null");
        String[] array = id.split(",");
        FlinkSql flinkSql1 = flinkSqlService.getById(array[0]);
        flinkSql1.base64Encode();
        if (array.length == 1) {
            return Result.success(new FlinkSql[]{flinkSql1});
        }
        FlinkSql flinkSql2 = flinkSqlService.getById(array[1]);
        flinkSql2.base64Encode();
        return Result.success(new FlinkSql[]{flinkSql1, flinkSql2});
    }

    @PostMapping("history")
    @Permission(app = "#app.id", team = "#app.teamId")
    public Result<List<FlinkSql>> history(Application app) {
        List<FlinkSql> sqlList = flinkSqlService.listFlinkSqlHistory(app.getId());
        return Result.success(sqlList);
    }

    @PostMapping("sql_complete")
    public Result<List<String>> getSqlComplete(@NotNull(message = "{required}") String sql) {
        return Result.success(sqlComplete.getComplete(sql));
    }
}
