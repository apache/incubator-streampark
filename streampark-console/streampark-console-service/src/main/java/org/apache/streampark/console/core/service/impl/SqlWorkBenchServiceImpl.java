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

package org.apache.streampark.console.core.service.impl;

import org.apache.streampark.console.core.entity.FlinkCluster;
import org.apache.streampark.console.core.entity.FlinkEnv;
import org.apache.streampark.console.core.service.FlinkClusterService;
import org.apache.streampark.console.core.service.FlinkEnvService;
import org.apache.streampark.console.core.service.SqlWorkBenchService;
import org.apache.streampark.gateway.OperationHandle;
import org.apache.streampark.gateway.factories.FactoryUtil;
import org.apache.streampark.gateway.factories.SqlGatewayServiceFactoryUtils;
import org.apache.streampark.gateway.flink.FlinkSqlGatewayServiceFactory;
import org.apache.streampark.gateway.results.Column;
import org.apache.streampark.gateway.results.GatewayInfo;
import org.apache.streampark.gateway.results.OperationInfo;
import org.apache.streampark.gateway.results.ResultQueryCondition;
import org.apache.streampark.gateway.results.ResultSet;
import org.apache.streampark.gateway.service.SqlGatewayService;
import org.apache.streampark.gateway.session.SessionEnvironment;
import org.apache.streampark.gateway.session.SessionHandle;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class SqlWorkBenchServiceImpl implements SqlWorkBenchService {

  private final FlinkClusterService flinkClusterService;

  private final FlinkEnvService flinkEnvService;

  public SqlWorkBenchServiceImpl(
      FlinkClusterService flinkClusterService, FlinkEnvService flinkEnvService) {
    this.flinkClusterService = flinkClusterService;
    this.flinkEnvService = flinkEnvService;
  }

  /** Get SqlGatewayService instance by flinkClusterId */
  private SqlGatewayService getSqlGateWayService(Long flinkClusterId) {
    FlinkCluster flinkCluster = flinkClusterService.getById(flinkClusterId);
    FlinkEnv flinkEnv = flinkEnvService.getById(flinkCluster.getVersionId());

    Map<String, String> config = new HashMap<>(8);
    config.put(
        FactoryUtil.SQL_GATEWAY_SERVICE_TYPE.getKey(), "flink-" + flinkEnv.getLargeVersion());
    config.put(FlinkSqlGatewayServiceFactory.BASE_URI.getKey(), flinkCluster.getGatewayAddress());

    // support read flink conf from streampark and set, sql gateway also support use `set xxx = xxx`
    // in code editor to set session conf.
    List<SqlGatewayService> actual = SqlGatewayServiceFactoryUtils.createSqlGatewayService(config);
    if (actual.size() > 1) {
      log.warn("There are more than one SqlGatewayService instance, please check your config");
    }
    return actual.get(0);
  }

  @Override
  public GatewayInfo getGatewayInfo(Long flinkClusterId) {
    SqlGatewayService sqlGateWayService = getSqlGateWayService(flinkClusterId);
    return sqlGateWayService.getGatewayInfo();
  }

  @Override
  public SessionHandle openSession(Long flinkClusterId) {
    SqlGatewayService sqlGateWayService = getSqlGateWayService(flinkClusterId);
    Map<String, String> streamParkConf = new HashMap<>();
    // TODO: 2023/4/30 read flink conf from streampark
    return sqlGateWayService.openSession(
        new SessionEnvironment("test-adien", null, streamParkConf));
  }

  @Override
  public void closeSession(Long flinkClusterId, String sessionHandleUUIDStr) {
    SqlGatewayService sqlGateWayService = getSqlGateWayService(flinkClusterId);
    sqlGateWayService.closeSession(new SessionHandle(UUID.fromString(sessionHandleUUIDStr)));
  }

  @Override
  public void cancelOperation(
      Long flinkClusterId, String sessionHandleUUIDStr, String operationId) {

    getSqlGateWayService(flinkClusterId)
        .cancelOperation(
            new SessionHandle(UUID.fromString(sessionHandleUUIDStr)),
            new OperationHandle(UUID.fromString(operationId)));
  }

  @Override
  public void closeOperation(Long flinkClusterId, String sessionHandleUUIDStr, String operationId) {

    getSqlGateWayService(flinkClusterId)
        .closeOperation(
            new SessionHandle(UUID.fromString(sessionHandleUUIDStr)),
            new OperationHandle(UUID.fromString(operationId)));
  }

  @Override
  public OperationInfo getOperationInfo(
      Long flinkClusterId, String sessionHandleUUIDStr, String operationId) {

    return getSqlGateWayService(flinkClusterId)
        .getOperationInfo(
            new SessionHandle(UUID.fromString(sessionHandleUUIDStr)),
            new OperationHandle(UUID.fromString(operationId)));
  }

  @Override
  public Column getOperationResultSchema(
      Long flinkClusterId, String sessionHandleUUIDStr, String operationId) {

    return getSqlGateWayService(flinkClusterId)
        .getOperationResultSchema(
            new SessionHandle(UUID.fromString(sessionHandleUUIDStr)),
            new OperationHandle(UUID.fromString(operationId)));
  }

  @Override
  public OperationHandle executeStatement(
      Long flinkClusterId, String sessionHandleUUIDStr, String statement) {

    return getSqlGateWayService(flinkClusterId)
        .executeStatement(
            new SessionHandle(UUID.fromString(sessionHandleUUIDStr)), statement, 10000L, null);
  }

  @Override
  public ResultSet fetchResults(
      Long flinkClusterId,
      String sessionHandleUUIDStr,
      String operationId,
      ResultQueryCondition resultQueryCondition) {
    return getSqlGateWayService(flinkClusterId)
        .fetchResults(
            new SessionHandle(UUID.fromString(sessionHandleUUIDStr)),
            new OperationHandle(UUID.fromString(operationId)),
            resultQueryCondition);
  }

  @Override
  public void heartbeat(Long flinkClusterId, String sessionHandle) {
    getSqlGateWayService(flinkClusterId)
        .heartbeat(new SessionHandle(UUID.fromString(sessionHandle)));
  }
}
