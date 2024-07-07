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

package org.apache.streampark.console.core.service;

import org.apache.streampark.gateway.OperationHandle;
import org.apache.streampark.gateway.results.Column;
import org.apache.streampark.gateway.results.GatewayInfo;
import org.apache.streampark.gateway.results.OperationInfo;
import org.apache.streampark.gateway.results.ResultQueryCondition;
import org.apache.streampark.gateway.results.ResultSet;
import org.apache.streampark.gateway.session.SessionHandle;

public interface SqlWorkBenchService {

    /**
     * Open a session for a flink cluster
     *
     * @param flinkGatewayId flink gateway id
     * @param flinkClusterId flink cluster id
     * @return session handle
     */
    SessionHandle openSession(Long flinkGatewayId, Long flinkClusterId);

    /**
     * Close a session
     *
     * @param flinkGatewayId flink gateway id
     * @param sessionHandleUUIDStr session handle uuid string
     */
    void closeSession(Long flinkGatewayId, String sessionHandleUUIDStr);

    /**
     * Get the gateway info
     *
     * @param flinkGatewayId flink gateway id
     * @return gateway info
     */
    GatewayInfo getGatewayInfo(Long flinkGatewayId);

    /**
     * Get the session info
     *
     * @param flinkGatewayId flink gateway id
     * @param sessionHandleUUIDStr session handle uuid string
     * @param operationId operation id
     */
    void cancelOperation(Long flinkGatewayId, String sessionHandleUUIDStr, String operationId);

    /**
     * Close the operation
     *
     * @param flinkGatewayId flink gateway id
     * @param sessionHandleUUIDStr session handle uuid string
     * @param operationId operation id
     */
    void closeOperation(Long flinkGatewayId, String sessionHandleUUIDStr, String operationId);

    /**
     * Get operation info
     *
     * @param flinkGatewayId flink gateway id
     * @param sessionHandleUUIDStr session handle uuid string
     * @param operationId operation id
     * @return operation info
     */
    OperationInfo getOperationInfo(
                                   Long flinkGatewayId, String sessionHandleUUIDStr, String operationId);

    /**
     * Get operation result schema
     *
     * @param flinkGatewayId flink gateway id
     * @param sessionHandleUUIDStr session handle uuid string
     * @param operationId operation id
     * @return operation result schema
     */
    Column getOperationResultSchema(
                                    Long flinkGatewayId, String sessionHandleUUIDStr, String operationId);

    /**
     * Execute statement
     *
     * @param flinkGatewayId flink gateway id
     * @param sessionHandleUUIDStr session handle uuid string
     * @param statement statement
     * @return operation handle
     */
    OperationHandle executeStatement(
                                     Long flinkGatewayId, String sessionHandleUUIDStr, String statement);

    /**
     * Fetch results
     *
     * @param flinkGatewayId flink gateway id
     * @param sessionHandleUUIDStr session handle uuid string
     * @param operationId operation id
     * @param resultQueryCondition result query condition
     * @return result set
     */
    ResultSet fetchResults(
                           Long flinkGatewayId,
                           String sessionHandleUUIDStr,
                           String operationId,
                           ResultQueryCondition resultQueryCondition);

    /**
     * Send heartbeat
     *
     * @param flinkGatewayId flink gateway id
     * @param sessionHandle session handle
     */
    void heartbeat(Long flinkGatewayId, String sessionHandle);

    /**
     * check flink cluster version is supported
     *
     * @param flinkGatewayId flink gateway id
     * @param flinkClusterId flink cluster id
     * @return true if supported
     */
    boolean check(Long flinkGatewayId, Long flinkClusterId);
}
