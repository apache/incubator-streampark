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

import org.apache.streampark.common.enums.ExecutionMode;
import org.apache.streampark.common.util.HadoopConfigUtils;
import org.apache.streampark.console.core.bean.SqlGatewayExecuteResult;
import org.apache.streampark.console.core.entity.FlinkCatalog;
import org.apache.streampark.console.core.entity.FlinkCluster;
import org.apache.streampark.console.core.entity.FlinkEnv;
import org.apache.streampark.console.core.entity.FlinkGateWay;
import org.apache.streampark.console.core.enums.CatalogMetaType;
import org.apache.streampark.console.core.service.FlinkCatalogService;
import org.apache.streampark.console.core.service.FlinkClusterService;
import org.apache.streampark.console.core.service.FlinkEnvService;
import org.apache.streampark.console.core.service.FlinkGateWayService;
import org.apache.streampark.console.core.service.SqlWorkBenchService;
import org.apache.streampark.flink.kubernetes.KubernetesRetriever;
import org.apache.streampark.flink.kubernetes.enums.FlinkK8sExecuteMode;
import org.apache.streampark.flink.kubernetes.ingress.IngressController;
import org.apache.streampark.gateway.OperationHandle;
import org.apache.streampark.gateway.OperationStatus;
import org.apache.streampark.gateway.exception.SqlGatewayException;
import org.apache.streampark.gateway.factories.FactoryUtil;
import org.apache.streampark.gateway.factories.SqlGatewayServiceFactoryUtils;
import org.apache.streampark.gateway.flink.FlinkSqlGatewayServiceFactory;
import org.apache.streampark.gateway.results.Column;
import org.apache.streampark.gateway.results.FetchOrientation;
import org.apache.streampark.gateway.results.GatewayInfo;
import org.apache.streampark.gateway.results.OperationInfo;
import org.apache.streampark.gateway.results.ResultQueryCondition;
import org.apache.streampark.gateway.results.ResultSet;
import org.apache.streampark.gateway.results.RowData;
import org.apache.streampark.gateway.service.SqlGatewayService;
import org.apache.streampark.gateway.session.SessionEnvironment;
import org.apache.streampark.gateway.session.SessionHandle;

import org.apache.commons.collections.CollectionUtils;
import org.apache.flink.client.program.ClusterClient;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.lang.Thread.sleep;

@Slf4j
@Service
@RequiredArgsConstructor
public class SqlWorkBenchServiceImpl implements SqlWorkBenchService {

  private final FlinkClusterService flinkClusterService;
  private final FlinkGateWayService flinkGateWayService;
  private final FlinkEnvService flinkEnvService;
  private final FlinkCatalogService flinkCatalogService;

  /** Get SqlGatewayService instance by flinkGatewayId */
  private SqlGatewayService getSqlGateWayService(Long flinkGatewayId) {
    FlinkGateWay flinkGateWay = flinkGateWayService.getById(flinkGatewayId);
    if (flinkGateWay == null) {
      throw new IllegalArgumentException(
          "flinkGateWay is not exist, please check your config, id: " + flinkGatewayId);
    }
    Map<String, String> config = new HashMap<>(2);
    config.put(
        FactoryUtil.SQL_GATEWAY_SERVICE_TYPE.getKey(),
        flinkGateWay.getGatewayType().getIdentifier());
    config.put(FlinkSqlGatewayServiceFactory.BASE_URI.getKey(), flinkGateWay.getAddress());
    List<SqlGatewayService> actual = SqlGatewayServiceFactoryUtils.createSqlGatewayService(config);
    if (actual.size() > 1) {
      log.warn("There are more than one SqlGatewayService instance, please check your config");
    }
    return actual.get(0);
  }

  @Override
  public GatewayInfo getGatewayInfo(Long flinkGatewayId) {
    SqlGatewayService sqlGateWayService = getSqlGateWayService(flinkGatewayId);
    return sqlGateWayService.getGatewayInfo();
  }

  @Override
  public SessionHandle openSession(Long flinkGatewayId, Long flinkClusterId) {
    Map<String, String> streamParkConf = new HashMap<>();
    SqlGatewayService sqlGateWayService = getSqlGateWayService(flinkGatewayId);
    FlinkCluster flinkCluster = flinkClusterService.getById(flinkClusterId);
    URI remoteURI = flinkCluster.getRemoteURI();
    String host = remoteURI.getHost();
    String port = String.valueOf(remoteURI.getPort());
    String clusterId = flinkCluster.getClusterId();

    ExecutionMode executionMode = ExecutionMode.of(flinkCluster.getExecutionMode());
    if (executionMode == null) {
      throw new IllegalArgumentException("executionMode is null");
    }

    streamParkConf.put("execution.target", executionMode.getName());
    switch (Objects.requireNonNull(executionMode)) {
      case REMOTE:
        streamParkConf.put("rest.address", host);
        streamParkConf.put("rest.port", port);
        break;
      case YARN_SESSION:
        streamParkConf.put("yarn.application.id", clusterId);
        HadoopConfigUtils.readSystemHadoopConf()
            .forEach((k, v) -> streamParkConf.put("flink.hadoop." + k, v));
        break;
      case KUBERNETES_NATIVE_SESSION:
        String k8sNamespace = flinkCluster.getK8sNamespace();
        String restAddress;
        try (ClusterClient<?> clusterClient =
            (ClusterClient<?>)
                KubernetesRetriever.newFinkClusterClient(
                    clusterId, k8sNamespace, FlinkK8sExecuteMode.of(executionMode))) {
          restAddress = IngressController.ingressUrlAddress(k8sNamespace, clusterId, clusterClient);
        } catch (Exception e) {
          throw new IllegalArgumentException("get k8s rest address error", e);
        }
        streamParkConf.put("kubernetes.cluster-id", clusterId);
        streamParkConf.put(
            "kubernetes.jobmanager.service-account", flinkCluster.getServiceAccount());
        streamParkConf.put("kubernetes.namespace", k8sNamespace);
        streamParkConf.put("rest.address", restAddress);
        break;
      default:
        throw new IllegalArgumentException("Unsupported execution mode: " + executionMode);
    }

    return sqlGateWayService.openSession(
        new SessionEnvironment(
            flinkGatewayId + flinkClusterId + UUID.randomUUID().toString(), null, streamParkConf));
  }

  @Override
  public void closeSession(Long flinkGatewayId, String sessionHandleUUIDStr) {
    SqlGatewayService sqlGateWayService = getSqlGateWayService(flinkGatewayId);
    sqlGateWayService.closeSession(new SessionHandle(sessionHandleUUIDStr));
  }

  @Override
  public void cancelOperation(
      Long flinkGatewayId, String sessionHandleUUIDStr, String operationId) {
    getSqlGateWayService(flinkGatewayId)
        .cancelOperation(new SessionHandle(sessionHandleUUIDStr), new OperationHandle(operationId));
  }

  @Override
  public void closeOperation(Long flinkGatewayId, String sessionHandleUUIDStr, String operationId) {
    getSqlGateWayService(flinkGatewayId)
        .closeOperation(new SessionHandle(sessionHandleUUIDStr), new OperationHandle(operationId));
  }

  @Override
  public OperationInfo getOperationInfo(
      Long flinkGatewayId, String sessionHandleUUIDStr, String operationId) {
    return getSqlGateWayService(flinkGatewayId)
        .getOperationInfo(
            new SessionHandle(sessionHandleUUIDStr), new OperationHandle(operationId));
  }

  @Override
  public Column getOperationResultSchema(
      Long flinkGatewayId, String sessionHandleUUIDStr, String operationId) {
    return getSqlGateWayService(flinkGatewayId)
        .getOperationResultSchema(
            new SessionHandle(sessionHandleUUIDStr), new OperationHandle(operationId));
  }

  @Override
  public OperationHandle executeStatement(
      Long flinkGatewayId, String sessionHandleUUIDStr, String statement) {
    return getSqlGateWayService(flinkGatewayId)
        .executeStatement(new SessionHandle(sessionHandleUUIDStr), statement, 10000L, null);
  }

  @Override
  public ResultSet fetchResults(
      Long flinkGatewayId,
      String sessionHandleUUIDStr,
      String operationId,
      ResultQueryCondition resultQueryCondition) {
    return getSqlGateWayService(flinkGatewayId)
        .fetchResults(
            new SessionHandle(sessionHandleUUIDStr),
            new OperationHandle(operationId),
            resultQueryCondition);
  }

  @SneakyThrows
  @Override
  public SqlGatewayExecuteResult executeAndFetchResults(
      Long flinkGatewayId, String sessionHandleId, String statement) {

    SqlGatewayService sqlGateWayService = getSqlGateWayService(flinkGatewayId);
    SessionHandle sessionHandle = new SessionHandle(sessionHandleId);
    OperationHandle operationHandle =
        sqlGateWayService.executeStatement(sessionHandle, statement, 10000L, null);

    // Wait for the operation to complete then fetch the result
    OperationInfo operationInfo = new OperationInfo(OperationStatus.INITIALIZED, null);
    while (!operationInfo.getStatus().isTerminalStatus()) {
      // Sleep 100ms to avoid too many requests
      sleep(100L);
      operationInfo = sqlGateWayService.getOperationInfo(sessionHandle, operationHandle);
    }

    if (operationInfo.getStatus() == OperationStatus.FINISHED) {
      // fetch result and return when result data size > 0 or nextToken is null
      ResultSet resultSet =
          sqlGateWayService.fetchResults(
              sessionHandle, operationHandle, new ResultQueryCondition());

      List<RowData> data = resultSet.getData();
      Long nextToken = resultSet.getNextToken();

      while (nextToken != null && CollectionUtils.isEmpty(data)) {
        Thread.sleep(500);
        resultSet =
            sqlGateWayService.fetchResults(
                sessionHandle,
                operationHandle,
                new ResultQueryCondition(FetchOrientation.FETCH_NEXT, nextToken, 1000));
        data = resultSet.getData();
        nextToken = resultSet.getNextToken();
      }
      return new SqlGatewayExecuteResult(sessionHandle, operationHandle, operationInfo, resultSet);
    }
    throw new SqlGatewayException(
        "Execute statement :" + statement + "failed !", operationInfo.getException());
  }

  @Override
  public void heartbeat(Long flinkGatewayId, String sessionHandle) {
    getSqlGateWayService(flinkGatewayId).heartbeat(new SessionHandle(sessionHandle));
  }

  @Override
  public boolean check(Long flinkGatewayId, Long flinkClusterId) {
    FlinkCluster flinkCluster = flinkClusterService.getById(flinkClusterId);
    if (flinkCluster == null) {
      throw new IllegalArgumentException("FlinkCluster not found");
    }
    FlinkEnv flinkEnv = flinkEnvService.getById(flinkCluster.getVersionId());
    if (flinkEnv == null) {
      throw new IllegalArgumentException("FlinkEnv not found");
    }
    return getSqlGateWayService(flinkGatewayId).check(flinkEnv.getFlinkVersion().majorVersion());
  }

  @Override
  public List<String> listMetaData(
      Long flinkGatewayId,
      String sessionHandle,
      Long catalogId,
      String databaseName,
      CatalogMetaType catalogMetaType) {

    FlinkCatalog flinkCatalog = flinkCatalogService.getById(catalogId);
    if (flinkCatalog == null) {
      throw new IllegalArgumentException("FlinkCatalog not found");
    }

    // if catalog is not exist, create it and use it
    List<String> catalogs =
        executeAndFetchListResults(
            flinkGatewayId, sessionHandle, flinkCatalog.getShowCatalogsSql());
    if (CollectionUtils.isEmpty(catalogs) || !catalogs.contains(flinkCatalog.getCatalogName())) {
      executeAndFetchResults(flinkGatewayId, sessionHandle, flinkCatalog.getCreateCatalogSql());
    }
    executeAndFetchResults(flinkGatewayId, sessionHandle, flinkCatalog.getUseCatalogSql());

    String sql;
    switch (catalogMetaType) {
      case DATABASE:
        sql = flinkCatalog.getShowDatabasesSql();
        break;
      case TABLE:
        sql = flinkCatalog.getShowTablesSql(databaseName);
        break;
      case VIEW:
        sql = flinkCatalog.getShowViewsSql();
        break;
      case FUNCTION:
        sql = flinkCatalog.getShowFunctionsSql(databaseName);
        break;
      default:
        throw new IllegalArgumentException("Unsupported catalogMetaType: " + catalogMetaType);
    }
    return executeAndFetchListResults(flinkGatewayId, sessionHandle, sql);
  }

  @Override
  public String showCreateSql(
      Long flinkGatewayId,
      String sessionHandle,
      Long catalogId,
      String databaseName,
      String objectName,
      CatalogMetaType catalogMetaType) {

    FlinkCatalog flinkCatalog = flinkCatalogService.getById(catalogId);
    if (flinkCatalog == null) {
      throw new IllegalArgumentException("FlinkCatalog not found");
    }
    String sql;
    switch (catalogMetaType) {
      case TABLE:
        sql = flinkCatalog.getShowCreateTableSql(databaseName, objectName);
        break;
      case VIEW:
        sql = flinkCatalog.getShowCreateViewSql(databaseName, objectName);
        break;
      default:
        throw new IllegalArgumentException("Unsupported catalogMetaType: " + catalogMetaType);
    }
    return executeAndFetchListResults(flinkGatewayId, sessionHandle, sql).get(0);
  }

  // -------------------------------------------------------------------------------------------
  // Utils Methods
  // -------------------------------------------------------------------------------------------

  /**
   * Execute statement and return the first field of the result
   *
   * @param flinkGatewayId flink gateway id
   * @param sessionHandle session handle
   * @param statement statement
   * @return result
   */
  private List<String> executeAndFetchListResults(
      Long flinkGatewayId, String sessionHandle, String statement) {
    SqlGatewayExecuteResult sqlGatewayExecuteResult =
        executeAndFetchResults(flinkGatewayId, sessionHandle, statement);
    return sqlGatewayExecuteResult.getResultSet().getData().stream()
        .map(rowData -> (String) rowData.getFields().get(0))
        .collect(Collectors.toList());
  }
}
