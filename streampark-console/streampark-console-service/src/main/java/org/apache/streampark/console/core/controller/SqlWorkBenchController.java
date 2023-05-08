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
import org.apache.streampark.console.core.service.SqlWorkBenchService;
import org.apache.streampark.gateway.results.ResultQueryCondition;
import org.apache.streampark.gateway.session.SessionHandle;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
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

@Api(tags = {"FLINK_GATEWAY_TAG"})
@Slf4j
@Validated
@RestController
@RequestMapping("flink/sqlWorkBench/{flinkClusterId}")
public class SqlWorkBenchController {
  private final SqlWorkBenchService sqlWorkBenchService;

  public SqlWorkBenchController(SqlWorkBenchService sqlWorkBenchService) {
    this.sqlWorkBenchService = sqlWorkBenchService;
  }

  // -------------------------------------------------------------------------------------------
  // Info API
  // -------------------------------------------------------------------------------------------

  @ApiAccess
  @ApiOperation(value = "Get gateway info", notes = "Get gateway info", tags = "FLINK_GATEWAY_TAG")
  @GetMapping("getGatewayInfo")
  public RestResponse getGatewayInfo(@PathVariable Long flinkClusterId) {
    return RestResponse.success(sqlWorkBenchService.getGatewayInfo(flinkClusterId));
  }

  // -------------------------------------------------------------------------------------------
  // Session Management
  // -------------------------------------------------------------------------------------------

  @ApiAccess
  @ApiOperation(value = "Open sessions", notes = "Open sessions", tags = "FLINK_GATEWAY_TAG")
  @PostMapping("sessions")
  public RestResponse openSession(@PathVariable Long flinkClusterId, String serviceType) {
    SessionHandle sessionHandle = sqlWorkBenchService.openSession(flinkClusterId);
    return RestResponse.success(sessionHandle);
  }

  @ApiAccess
  @ApiOperation(value = "Heartbeat", notes = "Heartbeat", tags = "FLINK_GATEWAY_TAG")
  @PostMapping("sessions/{sessionHandle}/heartbeat")
  public RestResponse heartbeat(
      @PathVariable Long flinkClusterId, @PathVariable String sessionHandle) {
    sqlWorkBenchService.heartbeat(flinkClusterId, sessionHandle);
    return RestResponse.success();
  }

  @ApiAccess
  @ApiOperation(value = "Close session", notes = "Close session", tags = "FLINK_GATEWAY_TAG")
  @DeleteMapping("sessions/{sessionHandle}")
  public RestResponse closeSession(
      @PathVariable Long flinkClusterId, @PathVariable String sessionHandle) {
    sqlWorkBenchService.closeSession(flinkClusterId, sessionHandle);
    return RestResponse.success();
  }

  // -------------------------------------------------------------------------------------------
  // Operation Management
  // -------------------------------------------------------------------------------------------

  @ApiAccess
  @ApiOperation(value = "Cancel operation", notes = "Cancel operation", tags = "FLINK_GATEWAY_TAG")
  @PostMapping("sessions/{sessionHandle}/operations/{operationHandle}/cancel")
  public RestResponse cancelOperation(
      @PathVariable Long flinkClusterId,
      @PathVariable String sessionHandle,
      @PathVariable String operationHandle) {
    sqlWorkBenchService.cancelOperation(flinkClusterId, sessionHandle, operationHandle);
    return RestResponse.success();
  }

  @ApiAccess
  @ApiOperation(value = "Close operation", notes = "Close operation", tags = "FLINK_GATEWAY_TAG")
  @DeleteMapping("sessions/{sessionHandle}/operations/{operationHandle}/close")
  public RestResponse closeOperation(
      @PathVariable Long flinkClusterId,
      @PathVariable String sessionHandle,
      @PathVariable String operationHandle) {
    sqlWorkBenchService.closeOperation(flinkClusterId, sessionHandle, operationHandle);
    return RestResponse.success();
  }

  @ApiAccess
  @ApiOperation(
      value = "Get operation info",
      notes = "Get operation info",
      tags = "FLINK_GATEWAY_TAG")
  @PostMapping("sessions/{sessionHandle}/operations/{operationHandle}/info")
  public RestResponse getOperationInfo(
      @PathVariable Long flinkClusterId,
      @PathVariable String sessionHandle,
      @PathVariable String operationHandle) {
    return RestResponse.success(
        sqlWorkBenchService.getOperationInfo(flinkClusterId, sessionHandle, operationHandle));
  }

  @ApiAccess
  @ApiOperation(
      value = "Get operation result schema",
      notes = "Get operation result schema",
      tags = "FLINK_GATEWAY_TAG")
  @PostMapping("sessions/{sessionHandle}/operations/{operationHandle}/resultSchema")
  public RestResponse getOperationResultSchema(
      @PathVariable Long flinkClusterId,
      @PathVariable String sessionHandle,
      @PathVariable String operationHandle) {
    return RestResponse.success(
        sqlWorkBenchService.getOperationResultSchema(
            flinkClusterId, sessionHandle, operationHandle));
  }

  // -------------------------------------------------------------------------------------------
  // Statements API
  // -------------------------------------------------------------------------------------------

  @ApiAccess
  @ApiOperation(
      value = "Execute statement",
      notes = "Execute statement",
      tags = "FLINK_GATEWAY_TAG")
  @PostMapping("sessions/{sessionHandle}/statements")
  public RestResponse executeStatement(
      @PathVariable Long flinkClusterId,
      @PathVariable String sessionHandle,
      @RequestParam String statement) {
    return RestResponse.success(
        sqlWorkBenchService.executeStatement(flinkClusterId, sessionHandle, statement));
  }

  @ApiAccess
  @ApiOperation(value = "Fetch results", notes = "Fetch results", tags = "FLINK_GATEWAY_TAG")
  @PostMapping("sessions/{sessionHandle}/statements/{operationHandle}/info")
  public RestResponse fetchResults(
      @PathVariable Long flinkClusterId,
      @PathVariable String sessionHandle,
      @PathVariable String operationHandle,
      @RequestBody ResultQueryCondition resultQueryCondition) {
    return RestResponse.success(
        sqlWorkBenchService.fetchResults(
            flinkClusterId, sessionHandle, operationHandle, resultQueryCondition));
  }

  // -------------------------------------------------------------------------------------------
  // Catalog API
  // -------------------------------------------------------------------------------------------
  // TODO: 2023/5/5 because of catalog with fixed statement, so frontend can use above methods to
  // get catalog info

}
