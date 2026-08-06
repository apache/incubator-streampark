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
import org.apache.streampark.console.core.assembler.FlinkSqlAssembler;
import org.apache.streampark.console.core.entity.FlinkSql;
import org.apache.streampark.console.core.request.flink.FlinkAppIdRequest;
import org.apache.streampark.console.core.request.flink.FlinkSqlCompleteRequest;
import org.apache.streampark.console.core.request.flink.FlinkSqlDeleteRequest;
import org.apache.streampark.console.core.request.flink.FlinkSqlGetRequest;
import org.apache.streampark.console.core.request.flink.FlinkSqlListQueryRequest;
import org.apache.streampark.console.core.request.flink.FlinkSqlVerifyRequest;
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

import javax.validation.Valid;

import java.util.List;

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
    public RestResponse verify(@Valid FlinkSqlVerifyRequest request) {
        String sql = variableService.replaceVariable(request.getTeamId(), request.getSql());
        FlinkSqlValidationResult flinkSqlValidationResult =
            flinkSqlService.verifySql(sql, request.getVersionId());
        if (!flinkSqlValidationResult.success()) {
            String exception = flinkSqlValidationResult.exception();
            RestResponse response = RestResponse.success()
                .data(false)
                .message(exception)
                .put(TYPE, flinkSqlValidationResult.failedType().getFailedType())
                .put(START, flinkSqlValidationResult.lineStart())
                .put(END, flinkSqlValidationResult.lineEnd());

            if (flinkSqlValidationResult.errorLine() > 0) {
                response
                    .put(START, flinkSqlValidationResult.errorLine())
                    .put(END, flinkSqlValidationResult.errorLine() + 1);
            }
            return response;
        }
        return RestResponse.success(true);
    }

    @PostMapping("list")
    @Permission(app = "#query.appId", team = "#query.teamId")
    public RestResponse list(@Valid FlinkSqlListQueryRequest query, RestRequest request) {
        IPage<FlinkSql> page = flinkSqlService.getPage(query.getAppId(), request);
        return RestResponse.success(FlinkSqlAssembler.toPageResponse(page));
    }

    @PostMapping("delete")
    @RequiresPermissions("sql:delete")
    @Permission(app = "#request.appId", team = "#request.teamId")
    public RestResponse delete(@Valid FlinkSqlDeleteRequest request) {
        Boolean deleted = flinkSqlService.removeById(request.getId());
        return RestResponse.success(deleted);
    }

    @PostMapping("get")
    @Permission(app = "#request.appId", team = "#request.teamId")
    public RestResponse get(@Valid FlinkSqlGetRequest request) throws InternalException {
        ApiAlertException.throwIfTrue(
            request.getAppId() == null || request.getTeamId() == null,
            "Permission denied, appId and teamId cannot be null");
        String[] array = request.getId().split(",");
        FlinkSql flinkSql1 = flinkSqlService.getById(array[0]);
        ApiAlertException.throwIfNull(flinkSql1, "Flink SQL not found.");
        flinkSql1.base64Encode();
        if (array.length == 1) {
            return RestResponse.success(FlinkSqlAssembler.toResponse(flinkSql1));
        }
        FlinkSql flinkSql2 = flinkSqlService.getById(array[1]);
        ApiAlertException.throwIfNull(flinkSql2, "Flink SQL not found.");
        flinkSql2.base64Encode();
        return RestResponse.success(
            FlinkSqlAssembler.toArrayResponse(new FlinkSql[]{flinkSql1, flinkSql2}));
    }

    @PostMapping("history")
    @Permission(app = "#request.id", team = "#request.teamId")
    public RestResponse history(@Valid FlinkAppIdRequest request) {
        List<FlinkSql> sqlList = flinkSqlService.listFlinkSqlHistory(request.getId());
        return RestResponse.success(FlinkSqlAssembler.toListResponse(sqlList));
    }

    @PostMapping("sql_complete")
    public RestResponse getSqlComplete(@Valid FlinkSqlCompleteRequest request) {
        return RestResponse.success().put("word", sqlComplete.getComplete(request.getSql()));
    }
}
