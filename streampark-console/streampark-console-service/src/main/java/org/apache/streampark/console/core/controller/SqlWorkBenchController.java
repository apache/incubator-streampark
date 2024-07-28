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

import org.apache.streampark.console.base.domain.Result;
import org.apache.streampark.console.core.service.SqlWorkBenchService;
import org.apache.streampark.gateway.OperationHandle;
import org.apache.streampark.gateway.session.SessionHandle;

import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequestMapping("flink/sqlWorkBench/{flinkGatewayId}")
public class SqlWorkBenchController {

    private final SqlWorkBenchService sqlWorkBenchService;

    public SqlWorkBenchController(SqlWorkBenchService sqlWorkBenchService) {
        this.sqlWorkBenchService = sqlWorkBenchService;
    }

    // -------------------------------------------------------------------------------------------
    // Validation API
    // -------------------------------------------------------------------------------------------
    public Result<Boolean> check(@PathVariable Long flinkGatewayId, @PathVariable Long flinkClusterId) {
        return Result.success(sqlWorkBenchService.check(flinkGatewayId, flinkClusterId));
    }

    // -------------------------------------------------------------------------------------------
    // Info API
    // -------------------------------------------------------------------------------------------

    @GetMapping("getGatewayInfo")
    public Result<GatewayInfo> getGatewayInfo(@PathVariable Long flinkGatewayId) {
        return Result.success(sqlWorkBenchService.getGatewayInfo(flinkGatewayId));
    }

    // -------------------------------------------------------------------------------------------
    // Session Management
    // -------------------------------------------------------------------------------------------

    @PostMapping("/{flinkClusterId}/sessions")
    public Result<SessionHandle> openSession(
                                             @PathVariable Long flinkGatewayId, @PathVariable Long flinkClusterId) {
        SessionHandle sessionHandle = sqlWorkBenchService.openSession(flinkGatewayId, flinkClusterId);
        return Result.success(sessionHandle);
    }

    @PostMapping("sessions/{sessionHandle}/heartbeat")
    public Result<Void> heartbeat(
                                  @PathVariable Long flinkGatewayId, @PathVariable String sessionHandle) {
        sqlWorkBenchService.heartbeat(flinkGatewayId, sessionHandle);
        return Result.success();
    }

    @DeleteMapping("sessions/{sessionHandle}")
    public Result<Void> closeSession(
                                     @PathVariable Long flinkGatewayId, @PathVariable String sessionHandle) {
        sqlWorkBenchService.closeSession(flinkGatewayId, sessionHandle);
        return Result.success();
    }

    // -------------------------------------------------------------------------------------------
    // Operation Management
    // -------------------------------------------------------------------------------------------

    @PostMapping("sessions/{sessionHandle}/operations/{operationHandle}/cancel")
    public Result<Void> cancelOperation(
                                        @PathVariable Long flinkGatewayId,
                                        @PathVariable String sessionHandle,
                                        @PathVariable String operationHandle) {
        sqlWorkBenchService.cancelOperation(flinkGatewayId, sessionHandle, operationHandle);
        return Result.success();
    }

    @DeleteMapping("sessions/{sessionHandle}/operations/{operationHandle}/close")
    public Result<Void> closeOperation(
                                       @PathVariable Long flinkGatewayId,
                                       @PathVariable String sessionHandle,
                                       @PathVariable String operationHandle) {
        sqlWorkBenchService.closeOperation(flinkGatewayId, sessionHandle, operationHandle);
        return Result.success();
    }

    @PostMapping("sessions/{sessionHandle}/operations/{operationHandle}/info")
    public Result<OperationInfo> getOperationInfo(
                                                  @PathVariable Long flinkGatewayId,
                                                  @PathVariable String sessionHandle,
                                                  @PathVariable String operationHandle) {
        return Result.success(
            sqlWorkBenchService.getOperationInfo(flinkGatewayId, sessionHandle, operationHandle));
    }

    @PostMapping("sessions/{sessionHandle}/operations/{operationHandle}/resultSchema")
    public Result<Column> getOperationResultSchema(
                                                   @PathVariable Long flinkGatewayId,
                                                   @PathVariable String sessionHandle,
                                                   @PathVariable String operationHandle) {
        return Result.success(
            sqlWorkBenchService.getOperationResultSchema(
                flinkGatewayId, sessionHandle, operationHandle));
    }

    // -------------------------------------------------------------------------------------------
    // Statements API
    // -------------------------------------------------------------------------------------------

    @PostMapping("sessions/{sessionHandle}/statements")
    public Result<OperationHandle> executeStatement(
                                                    @PathVariable Long flinkGatewayId,
                                                    @PathVariable String sessionHandle,
                                                    @RequestParam String statement) {
        return Result.success(
            sqlWorkBenchService.executeStatement(flinkGatewayId, sessionHandle, statement));
    }

    @PostMapping("sessions/{sessionHandle}/statements/{operationHandle}/info")
    public Result<ResultSet> fetchResults(
                                          @PathVariable Long flinkGatewayId,
                                          @PathVariable String sessionHandle,
                                          @PathVariable String operationHandle,
                                          @RequestBody ResultQueryCondition resultQueryCondition) {
        return Result.success(
            sqlWorkBenchService.fetchResults(
                flinkGatewayId, sessionHandle, operationHandle, resultQueryCondition));
    }

    // -------------------------------------------------------------------------------------------
    // Catalog API
    // -------------------------------------------------------------------------------------------
    // TODO: 2023/5/5 because of catalog with fixed statement, so frontend can use above methods to
    // get catalog info

}
