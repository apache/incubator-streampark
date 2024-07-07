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

package org.apache.streampark.gateway.flink;

import org.apache.streampark.gateway.ExecutionConfiguration;
import org.apache.streampark.gateway.OperationHandle;
import org.apache.streampark.gateway.OperationStatusEnum;
import org.apache.streampark.gateway.exception.SqlGatewayException;
import org.apache.streampark.gateway.flink.client.dto.ExecuteStatementRequestBody;
import org.apache.streampark.gateway.flink.client.dto.FetchResultsResponseBody;
import org.apache.streampark.gateway.flink.client.dto.GetInfoResponseBody;
import org.apache.streampark.gateway.flink.client.dto.OpenSessionRequestBody;
import org.apache.streampark.gateway.flink.client.dto.OperationStatusResponseBody;
import org.apache.streampark.gateway.flink.client.dto.ResultSetColumnsInner;
import org.apache.streampark.gateway.flink.client.dto.ResultSetDataInner;
import org.apache.streampark.gateway.flink.client.rest.ApiClient;
import org.apache.streampark.gateway.flink.client.rest.ApiException;
import org.apache.streampark.gateway.flink.client.rest.v1.DefaultApi;
import org.apache.streampark.gateway.results.Column;
import org.apache.streampark.gateway.results.GatewayInfo;
import org.apache.streampark.gateway.results.OperationInfo;
import org.apache.streampark.gateway.results.ResultKindEnum;
import org.apache.streampark.gateway.results.ResultQueryCondition;
import org.apache.streampark.gateway.results.ResultSet;
import org.apache.streampark.gateway.results.RowData;
import org.apache.streampark.gateway.service.SqlGatewayService;
import org.apache.streampark.gateway.session.SessionEnvironment;
import org.apache.streampark.gateway.session.SessionHandle;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Implement {@link SqlGatewayService} with Flink native SqlGateway. */
public class FlinkSqlGatewayImpl implements SqlGatewayService {

    private final DefaultApi defaultApi;

    public FlinkSqlGatewayImpl(String baseUri) {
        ApiClient client = new ApiClient();
        client.setBasePath(baseUri);
        defaultApi = new DefaultApi(client);
    }

    @Override
    public boolean check(String flinkMajorVersion) {
        // flink gateway v1 api is supported from flink 1.16
        return Double.parseDouble(flinkMajorVersion) >= 1.16;
    }

    @Override
    public GatewayInfo getGatewayInfo() throws SqlGatewayException {
        GetInfoResponseBody info = null;
        try {
            info = defaultApi.getInfo();
            return new GatewayInfo(info.getProductName(), info.getVersion());
        } catch (ApiException e) {
            throw new SqlGatewayException("Flink native SqlGateWay getGatewayInfo failed!", e);
        }
    }

    @Override
    public SessionHandle openSession(SessionEnvironment environment) throws SqlGatewayException {
        try {
            return new SessionHandle(
                    Objects.requireNonNull(
                            defaultApi
                                    .openSession(
                                            new OpenSessionRequestBody()
                                                    .sessionName(environment.getSessionName())
                                                    .properties(environment.getSessionConfig()))
                                    .getSessionHandle()));
        } catch (ApiException e) {
            throw new SqlGatewayException("Flink native SqlGateWay openSession failed!", e);
        }
    }

    @Override
    public void heartbeat(SessionHandle sessionHandle) throws SqlGatewayException {
        try {
            defaultApi.triggerSession(
                    new org.apache.streampark.gateway.flink.client.dto.SessionHandle()
                            .identifier(UUID.fromString(sessionHandle.getIdentifier())));
        } catch (ApiException e) {
            throw new SqlGatewayException("Flink native SqlGateWay heartbeat failed!", e);
        }
    }

    @Override
    public void closeSession(SessionHandle sessionHandle) throws SqlGatewayException {
        try {
            defaultApi.closeSession(UUID.fromString(sessionHandle.getIdentifier()));
        } catch (ApiException e) {
            throw new SqlGatewayException("Flink native SqlGateWay closeSession failed!", e);
        }
    }

    @Override
    public void cancelOperation(SessionHandle sessionHandle,
                                OperationHandle operationHandle) throws SqlGatewayException {
        try {
            defaultApi.cancelOperation(
                    new org.apache.streampark.gateway.flink.client.dto.SessionHandle()
                            .identifier(UUID.fromString(sessionHandle.getIdentifier())),
                    new org.apache.streampark.gateway.flink.client.dto.OperationHandle()
                            .identifier(UUID.fromString(operationHandle.getIdentifier())));
        } catch (ApiException e) {
            throw new SqlGatewayException("Flink native SqlGateWay cancelOperation failed!", e);
        }
    }

    @Override
    public void closeOperation(SessionHandle sessionHandle,
                               OperationHandle operationHandle) throws SqlGatewayException {
        try {
            defaultApi.closeOperation(
                    UUID.fromString(sessionHandle.getIdentifier()),
                    UUID.fromString(operationHandle.getIdentifier()));
        } catch (ApiException e) {
            throw new SqlGatewayException("Flink native SqlGateWay closeOperation failed!", e);
        }
    }

    @Override
    public OperationInfo getOperationInfo(
                                          SessionHandle sessionHandle,
                                          OperationHandle operationHandle) throws SqlGatewayException {

        try {
            OperationStatusResponseBody operationStatus = defaultApi.getOperationStatus(
                    UUID.fromString(sessionHandle.getIdentifier()),
                    UUID.fromString(operationHandle.getIdentifier()));
            return new OperationInfo(OperationStatusEnum.valueOf(operationStatus.getStatus()), null);
        } catch (ApiException e) {
            throw new SqlGatewayException("Flink native SqlGateWay closeOperation failed!", e);
        }
    }

    @Override
    public Column getOperationResultSchema(
                                           SessionHandle sessionHandle,
                                           OperationHandle operationHandle) throws SqlGatewayException {
        throw new SqlGatewayException(
                "Flink native SqlGateWay don`t support operation:getOperationResultSchema!");
    }

    @Override
    public OperationHandle executeStatement(
                                            SessionHandle sessionHandle,
                                            String statement,
                                            long executionTimeoutMs,
                                            ExecutionConfiguration executionConfig) throws SqlGatewayException {
        try {
            return new OperationHandle(
                    Objects.requireNonNull(
                            defaultApi
                                    .executeStatement(
                                            UUID.fromString(sessionHandle.getIdentifier()),
                                            new ExecuteStatementRequestBody()
                                                    .statement(statement)
                                                    // currently, sql gateway don't support execution timeout
                                                    // .executionTimeout(executionTimeoutMs)
                                                    .executionConfig(null))
                                    .getOperationHandle()));
        } catch (ApiException e) {
            throw new SqlGatewayException("Flink native SqlGateWay executeStatement failed!", e);
        }
    }

    @Override
    public ResultSet fetchResults(
                                  SessionHandle sessionHandle,
                                  OperationHandle operationHandle,
                                  ResultQueryCondition resultQueryCondition) throws SqlGatewayException {
        try {

            List<RowData> data = new ArrayList<>();
            List<Column> columns = new ArrayList<>();
            FetchResultsResponseBody fetchResultsResponseBody = defaultApi.fetchResults(
                    UUID.fromString(sessionHandle.getIdentifier()),
                    UUID.fromString(operationHandle.getIdentifier()),
                    resultQueryCondition.getToken());
            String resultTypeStr = fetchResultsResponseBody.getResultType();
            Long nextToken = null;
            if (fetchResultsResponseBody.getNextResultUri() != null) {
                String nextResultUri = fetchResultsResponseBody.getNextResultUri();
                nextToken = Long.valueOf(nextResultUri.substring(nextResultUri.lastIndexOf("/") + 1));
            }

            org.apache.streampark.gateway.flink.client.dto.ResultSet results = fetchResultsResponseBody.getResults();

            List<ResultSetColumnsInner> resultsColumns = results.getColumns();
            List<ResultSetDataInner> resultsData = results.getData();

            resultsColumns.forEach(
                    column -> columns.add(
                            new Column(
                                    column.getName(), column.getLogicalType().toJson(), column.getComment())));

            resultsData.forEach(row -> data.add(new RowData(row.getKind().getValue(), row.getFields())));

            ResultKindEnum resultKindEnum = columns.size() == 1 && columns.get(0).getName().equals("result")
                    ? ResultKindEnum.SUCCESS
                    : ResultKindEnum.SUCCESS_WITH_CONTENT;

            return new ResultSet(
                    ResultSet.ResultType.valueOf(resultTypeStr),
                    nextToken,
                    columns,
                    data,
                    true,
                    null,
                    resultKindEnum);
        } catch (ApiException e) {
            throw new SqlGatewayException("Flink native SqlGateWay fetchResults failed!", e);
        }
    }
}
