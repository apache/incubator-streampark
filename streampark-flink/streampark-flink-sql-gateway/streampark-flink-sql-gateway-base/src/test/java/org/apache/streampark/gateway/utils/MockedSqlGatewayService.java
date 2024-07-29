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

package org.apache.streampark.gateway.utils;

import org.apache.streampark.gateway.ExecutionConfiguration;
import org.apache.streampark.gateway.OperationHandle;
import org.apache.streampark.gateway.exception.SqlGatewayException;
import org.apache.streampark.gateway.results.Column;
import org.apache.streampark.gateway.results.GatewayInfo;
import org.apache.streampark.gateway.results.OperationInfo;
import org.apache.streampark.gateway.results.ResultQueryCondition;
import org.apache.streampark.gateway.results.ResultSet;
import org.apache.streampark.gateway.service.SqlGatewayService;
import org.apache.streampark.gateway.session.SessionEnvironment;
import org.apache.streampark.gateway.session.SessionHandle;

import java.util.Objects;

/** Mocked implementation of {@link SqlGatewayService}. */
public class MockedSqlGatewayService implements SqlGatewayService {

    public final String host;
    public final int port;

    public final String description;

    public MockedSqlGatewayService(String host, int port, String description) {
        this.host = host;
        this.port = port;
        this.description = description;
    }

    @Override
    public boolean check(String flinkMajorVersion) {
        return !Objects.equals(flinkMajorVersion, "1.11");
    }

    @Override
    public GatewayInfo getGatewayInfo() throws SqlGatewayException {
        throw new UnsupportedOperationException();
    }

    @Override
    public SessionHandle openSession(SessionEnvironment environment) throws SqlGatewayException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void heartbeat(SessionHandle sessionHandle) throws SqlGatewayException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void closeSession(SessionHandle sessionHandle) throws SqlGatewayException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void cancelOperation(SessionHandle sessionHandle,
                                OperationHandle operationHandle) throws SqlGatewayException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void closeOperation(SessionHandle sessionHandle,
                               OperationHandle operationHandle) throws SqlGatewayException {
        throw new UnsupportedOperationException();
    }

    @Override
    public OperationInfo getOperationInfo(
                                          SessionHandle sessionHandle,
                                          OperationHandle operationHandle) throws SqlGatewayException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Column getOperationResultSchema(
                                           SessionHandle sessionHandle,
                                           OperationHandle operationHandle) throws SqlGatewayException {
        throw new UnsupportedOperationException();
    }

    @Override
    public OperationHandle executeStatement(
                                            SessionHandle sessionHandle,
                                            String statement,
                                            long executionTimeoutMs,
                                            ExecutionConfiguration executionConfig) throws SqlGatewayException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ResultSet fetchResults(
                                  SessionHandle sessionHandle,
                                  OperationHandle operationHandle,
                                  ResultQueryCondition resultQueryCondition) throws SqlGatewayException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MockedSqlGatewayService that = (MockedSqlGatewayService) o;
        return port == that.port
            && Objects.equals(host, that.host)
            && Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port, description);
    }
}
