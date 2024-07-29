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
package org.apache.streampark.gateway.service;

import org.apache.streampark.gateway.ExecutionConfiguration;
import org.apache.streampark.gateway.OperationHandle;
import org.apache.streampark.gateway.OperationStatusEnum;
import org.apache.streampark.gateway.exception.SqlGatewayException;
import org.apache.streampark.gateway.results.Column;
import org.apache.streampark.gateway.results.GatewayInfo;
import org.apache.streampark.gateway.results.OperationInfo;
import org.apache.streampark.gateway.results.ResultQueryCondition;
import org.apache.streampark.gateway.results.ResultSet;
import org.apache.streampark.gateway.session.SessionEnvironment;
import org.apache.streampark.gateway.session.SessionHandle;

/** A service of SQL gateway is responsible for handling requests from streampark console. */
public interface SqlGatewayService {

    // -------------------------------------------------------------------------------------------
    // Validate API
    // -------------------------------------------------------------------------------------------

    /**
     * Check if the SQL gateway is available with the given flink major version.
     *
     * @param flinkMajorVersion flink major version
     * @return true if the SQL gateway is available with the given flink major version.
     */
    boolean check(String flinkMajorVersion);

    // -------------------------------------------------------------------------------------------
    // Info API
    // -------------------------------------------------------------------------------------------

    GatewayInfo getGatewayInfo() throws SqlGatewayException;

    // -------------------------------------------------------------------------------------------
    // Session Management
    // -------------------------------------------------------------------------------------------

    /**
     * Open the {@code Session}.
     *
     * @param environment Environment to initialize the Session.
     * @return Returns a handle that used to identify the Session.
     */
    SessionHandle openSession(SessionEnvironment environment) throws SqlGatewayException;

    /**
     * Heartbeat for session
     *
     * @param sessionHandle handle to identify the Session.
     */
    void heartbeat(SessionHandle sessionHandle) throws SqlGatewayException;

    /**
     * Close the {@code Session}.
     *
     * @param sessionHandle handle to identify the Session needs to be closed.
     */
    void closeSession(SessionHandle sessionHandle) throws SqlGatewayException;

    // -------------------------------------------------------------------------------------------
    // Operation Management
    // -------------------------------------------------------------------------------------------

    /**
     * Cancel the operation when it is not in terminal status.
     *
     * <p>It can't cancel an Operation if it is terminated.
     *
     * @param sessionHandle handle to identify the session.
     * @param operationHandle handle to identify the operation.JarURLConnection
     */
    void cancelOperation(SessionHandle sessionHandle, OperationHandle operationHandle) throws SqlGatewayException;

    /**
     * Close the operation and release all used resource by the operation.
     *
     * @param sessionHandle handle to identify the session.
     * @param operationHandle handle to identify the operation.
     */
    void closeOperation(SessionHandle sessionHandle, OperationHandle operationHandle) throws SqlGatewayException;

    /**
     * Get the {@link OperationInfo} of the operation.
     *
     * @param sessionHandle handle to identify the session.
     * @param operationHandle handle to identify the operation.
     */
    OperationInfo getOperationInfo(SessionHandle sessionHandle,
                                   OperationHandle operationHandle) throws SqlGatewayException;

    /**
     * Get the result schema for the specified Operation.
     *
     * <p>Note: The result schema is available when the Operation is in the {@link
     * OperationStatusEnum#FINISHED}.
     *
     * @param sessionHandle handle to identify the session.
     * @param operationHandle handle to identify the operation.
     */
    Column getOperationResultSchema(SessionHandle sessionHandle,
                                    OperationHandle operationHandle) throws SqlGatewayException;

    // -------------------------------------------------------------------------------------------
    // Statements API
    // -------------------------------------------------------------------------------------------

    /**
     * Execute the submitted statement.
     *
     * @param sessionHandle handle to identify the session.
     * @param statement the SQL to execute.
     * @param executionTimeoutMs the execution timeout. Please use non-positive value to forbid the
     *     timeout mechanism.
     * @param executionConfig execution config for the statement.
     * @return handle to identify the operation.
     */
    OperationHandle executeStatement(
                                     SessionHandle sessionHandle,
                                     String statement,
                                     long executionTimeoutMs,
                                     ExecutionConfiguration executionConfig) throws SqlGatewayException;

    /**
     * Fetch the results from the operation. When maxRows is Integer.MAX_VALUE, it means to fetch all
     * available data.
     *
     * @param sessionHandle handle to identify the session.
     * @param operationHandle handle to identify the operation.
     * @param resultQueryCondition condition of result query.
     * @return Returns the results.
     */
    ResultSet fetchResults(
                           SessionHandle sessionHandle,
                           OperationHandle operationHandle,
                           ResultQueryCondition resultQueryCondition) throws SqlGatewayException;
}
