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

package org.apache.streampark.console.core.managed.service;

import org.apache.streampark.console.core.entity.ManagedFlinkOperation;
import org.apache.streampark.console.core.managed.model.ManagedFlinkOperationView;

import java.util.List;

/** Admission and lookup contract for durable managed Flink write operations. */
public interface ManagedFlinkOperationService {

    ManagedFlinkOperationView accept(
                                     Long appId,
                                     String operationType,
                                     String idempotencyKey,
                                     String requestHash,
                                     String requestJson);

    ManagedFlinkOperation getRequired(Long appId, Long operationId);

    ManagedFlinkOperation getRequired(Long operationId);

    ManagedFlinkOperation findByIdempotency(
                                            Long appId,
                                            String operationType,
                                            String idempotencyKey);

    ManagedFlinkOperationView getView(Long appId, Long operationId);

    List<ManagedFlinkOperationView> list(Long appId);

    boolean markRunning(Long operationId);

    boolean markReconciling(Long operationId);

    void recordProviderProgress(
                                Long operationId,
                                String providerRequestId,
                                String providerOperationId,
                                String resultJson);

    void markAwaitingReconcile(
                               Long operationId,
                               String providerRequestId,
                               String providerOperationId,
                               String resultJson);

    void markSucceeded(
                       Long operationId,
                       String providerRequestId,
                       String providerOperationId,
                       String resultJson);

    void markFailed(
                    Long operationId,
                    boolean outcomeUnknown,
                    String providerRequestId,
                    String errorCode,
                    String errorMessage);
}
