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
import org.apache.streampark.console.core.annotation.ApiAccess;
import org.apache.streampark.console.core.enums.CatalogMetaType;
import org.apache.streampark.console.core.service.SqlWorkBenchService;
import org.apache.streampark.gateway.results.ResultQueryCondition;
import org.apache.streampark.gateway.session.SessionHandle;

import io.swagger.annotations.ApiParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "SQL_WORK_BENCH_TAG")
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
  @ApiAccess
  @Operation(summary = "Check Support", description = "Check Support")
  @GetMapping("{flinkClusterId}/check")
  public RestResponse check(@PathVariable Long flinkGatewayId, @PathVariable Long flinkClusterId) {
    return RestResponse.success(sqlWorkBenchService.check(flinkGatewayId, flinkClusterId));
  }

  // -------------------------------------------------------------------------------------------
  // Info API
  // -------------------------------------------------------------------------------------------

  @ApiAccess
  @Operation(summary = "Get gateway info", description = "Get gateway info")
  @GetMapping("getGatewayInfo")
  public RestResponse getGatewayInfo(@PathVariable Long flinkGatewayId) {
    return RestResponse.success(sqlWorkBenchService.getGatewayInfo(flinkGatewayId));
  }

  // -------------------------------------------------------------------------------------------
  // Session Management
  // -------------------------------------------------------------------------------------------

  @ApiAccess
  @Operation(summary = "Open sessions", description = "Open sessions")
  @PostMapping("/{flinkClusterId}/sessions")
  public RestResponse openSession(
      @PathVariable Long flinkGatewayId, @PathVariable Long flinkClusterId) {
    SessionHandle sessionHandle = sqlWorkBenchService.openSession(flinkGatewayId, flinkClusterId);
    return RestResponse.success(sessionHandle);
  }

  @ApiAccess
  @Operation(summary = "Heartbeat", description = "Heartbeat")
  @PostMapping("sessions/{sessionHandle}/heartbeat")
  public RestResponse heartbeat(
      @PathVariable Long flinkGatewayId, @PathVariable String sessionHandle) {
    sqlWorkBenchService.heartbeat(flinkGatewayId, sessionHandle);
    return RestResponse.success();
  }

  @ApiAccess
  @Operation(summary = "Close session", description = "Close session")
  @DeleteMapping("sessions/{sessionHandle}")
  public RestResponse closeSession(
      @PathVariable Long flinkGatewayId, @PathVariable String sessionHandle) {
    sqlWorkBenchService.closeSession(flinkGatewayId, sessionHandle);
    return RestResponse.success();
  }

  // -------------------------------------------------------------------------------------------
  // Operation Management
  // -------------------------------------------------------------------------------------------

  @ApiAccess
  @Operation(summary = "Cancel operation", description = "Cancel operation")
  @PostMapping("sessions/{sessionHandle}/operations/{operationHandle}/cancel")
  public RestResponse cancelOperation(
      @PathVariable Long flinkGatewayId,
      @PathVariable String sessionHandle,
      @PathVariable String operationHandle) {
    sqlWorkBenchService.cancelOperation(flinkGatewayId, sessionHandle, operationHandle);
    return RestResponse.success();
  }

  @ApiAccess
  @Operation(summary = "Close operation", description = "Close operation")
  @DeleteMapping("sessions/{sessionHandle}/operations/{operationHandle}/close")
  public RestResponse closeOperation(
      @PathVariable Long flinkGatewayId,
      @PathVariable String sessionHandle,
      @PathVariable String operationHandle) {
    sqlWorkBenchService.closeOperation(flinkGatewayId, sessionHandle, operationHandle);
    return RestResponse.success();
  }

  @ApiAccess
  @Operation(summary = "Get operation info", description = "Get operation info")
  @PostMapping("sessions/{sessionHandle}/operations/{operationHandle}/info")
  public RestResponse getOperationInfo(
      @PathVariable Long flinkGatewayId,
      @PathVariable String sessionHandle,
      @PathVariable String operationHandle) {
    return RestResponse.success(
        sqlWorkBenchService.getOperationInfo(flinkGatewayId, sessionHandle, operationHandle));
  }

  @ApiAccess
  @Operation(summary = "Get operation result schema", description = "Get operation result schema")
  @PostMapping("sessions/{sessionHandle}/operations/{operationHandle}/resultSchema")
  public RestResponse getOperationResultSchema(
      @PathVariable Long flinkGatewayId,
      @PathVariable String sessionHandle,
      @PathVariable String operationHandle) {
    return RestResponse.success(
        sqlWorkBenchService.getOperationResultSchema(
            flinkGatewayId, sessionHandle, operationHandle));
  }

  // -------------------------------------------------------------------------------------------
  // Statements API
  // -------------------------------------------------------------------------------------------

  @ApiAccess
  @Operation(summary = "Execute statement", description = "Execute statement")
  @PostMapping("sessions/{sessionHandle}/statements")
  public RestResponse executeStatement(
      @PathVariable Long flinkGatewayId,
      @PathVariable String sessionHandle,
      @RequestParam String statement) {
    return RestResponse.success(
        sqlWorkBenchService.executeStatement(flinkGatewayId, sessionHandle, statement));
  }

  @ApiAccess
  @Operation(summary = "Fetch results", description = "Fetch results")
  @PostMapping("sessions/{sessionHandle}/operations/{operationHandle}/result")
  public RestResponse fetchResults(
      @PathVariable Long flinkGatewayId,
      @PathVariable String sessionHandle,
      @PathVariable String operationHandle,
      @RequestBody ResultQueryCondition resultQueryCondition) {
    return RestResponse.success(
        sqlWorkBenchService.fetchResults(
            flinkGatewayId, sessionHandle, operationHandle, resultQueryCondition));
  }

  @ApiAccess
  @Operation(
      summary = "Execute statement and fetch results",
      description =
          "Execute statement and fetch results , the statement will be executed in a new operation handle.")
  @PostMapping("sessions/{sessionHandle}/statement/result")
  public RestResponse executeStatementAndFetchResult(
      @PathVariable Long flinkGatewayId,
      @PathVariable String sessionHandle,
      @RequestParam String statement) {
    return RestResponse.success(
        sqlWorkBenchService.executeAndFetchResults(flinkGatewayId, sessionHandle, statement));
  }

  // -------------------------------------------------------------------------------------------
  // Catalog API
  // -------------------------------------------------------------------------------------------

  @ApiAccess
  @Operation(
      summary = "List the name of object in catalog",
      description =
          "List the name of object in catalog , when catalogMetaType is TABLE or VIEW or FUNCTION, the databaseName is required")
  @PostMapping("sessions/{sessionHandle}/catalogs/{catalogId}/databases/{catalogMetaType}")
  public RestResponse listMetaData(
      @PathVariable Long flinkGatewayId,
      @PathVariable String sessionHandle,
      @PathVariable Long catalogId,
      @RequestParam(required = false) String databaseName,
      @PathVariable
          @ApiParam(
              value = "Specify the type of catalog meta data",
              allowableValues = "DATABASE,TABLE,VIEW,FUNCTION")
          CatalogMetaType catalogMetaType) {
    return RestResponse.success(
        sqlWorkBenchService.listMetaData(
            flinkGatewayId, sessionHandle, catalogId, databaseName, catalogMetaType));
  }

  @ApiAccess
  @Operation(summary = "Show create sql of object", description = "only for table/view")
  @PostMapping("sessions/{sessionHandle}/catalogs/{catalogId}/{databaseName}/{objectName}")
  public RestResponse showCreateSql(
      @PathVariable Long flinkGatewayId,
      @PathVariable String sessionHandle,
      @PathVariable Long catalogId,
      @PathVariable String databaseName,
      @PathVariable String objectName,
      @RequestParam @ApiParam(allowableValues = "TABLE,VIEW") CatalogMetaType catalogMetaType) {
    return RestResponse.success(
        sqlWorkBenchService.showCreateSql(
            flinkGatewayId, sessionHandle, catalogId, databaseName, objectName, catalogMetaType));
  }

  // -------------------------------------------------------------------------------------------
  // JOB API (Only for flink 1.17+)
  // -------------------------------------------------------------------------------------------

  @ApiAccess
  @Operation(summary = "List jobs", description = "List jobs")
  @PostMapping("sessions/{sessionHandle}/jobs")
  public RestResponse listJobs(
      @PathVariable Long flinkGatewayId, @PathVariable String sessionHandle) {
    return RestResponse.success(
        sqlWorkBenchService.executeAndFetchResults(flinkGatewayId, sessionHandle, "SHOW JOBS;"));
  }

  @ApiAccess
  @Operation(summary = "Stop job", description = "stop job")
  @PostMapping("sessions/{sessionHandle}/jobs/{jobId}/stop")
  public RestResponse stopJob(
      @PathVariable Long flinkGatewayId,
      @PathVariable String sessionHandle,
      @PathVariable String jobId) {
    return RestResponse.success(
        sqlWorkBenchService.executeAndFetchResults(
            flinkGatewayId, sessionHandle, "STOP JOB '" + jobId + "';"));
  }
}
