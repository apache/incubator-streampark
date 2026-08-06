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
import org.apache.streampark.console.core.managed.api.ManagedFlinkProviderException;
import org.apache.streampark.console.core.managed.api.ManagedSnapshotCreateRequest;
import org.apache.streampark.console.core.managed.api.ManagedSnapshotCreateResult;
import org.apache.streampark.console.core.managed.model.ManagedFlinkSnapshotOperationSnapshot;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/** Executes one snapshot mutation exactly once and leaves completion to read-only reconciliation. */
@Slf4j
@Service
@RequiredArgsConstructor
class ManagedFlinkSnapshotExecutor {

    private static final Pattern SAFE_CODE = Pattern.compile("[A-Za-z0-9._-]{1,128}");

    private final ManagedFlinkOperationService operationService;
    private final ManagedFlinkProviderContextService providerContextService;
    private final ObjectMapper objectMapper;

    void execute(Long operationId) {
        if (!operationService.markRunning(operationId)) {
            return;
        }
        ManagedFlinkOperation operation = operationService.getRequired(operationId);
        ManagedFlinkSnapshotOperationSnapshot snapshot;
        try {
            snapshot =
                objectMapper.readValue(
                    operation.getRequestJson(),
                    ManagedFlinkSnapshotOperationSnapshot.class);
        } catch (Exception exception) {
            operationService.markFailed(
                operationId,
                false,
                null,
                "INTERNAL:InvalidSnapshotRequest",
                "Managed Flink snapshot request is invalid.");
            return;
        }

        boolean providerWriteStarted = false;
        try {
            ManagedFlinkProviderSession session =
                providerContextService.resolve(
                    snapshot.getTeamId(),
                    snapshot.getCloudAccountId(),
                    snapshot.getProviderConfigJson(),
                    snapshot.getProviderConfigVersion());
            if (!session.getProviderType().name().equals(snapshot.getProviderType())) {
                throw new IllegalStateException("Managed Flink provider routing changed.");
            }
            providerWriteStarted = true;
            ManagedSnapshotCreateResult result =
                session
                    .getProvider()
                    .createSnapshot(
                        session.getContext(),
                        ManagedSnapshotCreateRequest.builder()
                            .jobId(snapshot.getJobId())
                            .instanceId(snapshot.getInstanceId())
                            .description(snapshot.getProviderDescription())
                            .build());
            if (result == null
                || !snapshot.getJobId().equals(result.getJobId())) {
                throw new IllegalStateException("Managed Flink snapshot receipt is invalid.");
            }
            operationService.markSucceeded(
                operationId,
                result.getProviderRequestId(),
                null,
                result(result));
        } catch (ManagedFlinkProviderException exception) {
            operationService.markFailed(
                operationId,
                exception.isRetryable(),
                exception.getProviderRequestId(),
                exception.getCategory().name() + ":" + safeCode(exception.getProviderCode()),
                exception.isRetryable()
                    ? "Managed Flink snapshot outcome requires reconciliation."
                    : "Managed Flink provider rejected snapshot creation.");
        } catch (Exception exception) {
            operationService.markFailed(
                operationId,
                providerWriteStarted,
                null,
                providerWriteStarted
                    ? "UNKNOWN:SnapshotExecutionError"
                    : "INTERNAL:SnapshotPreparationError",
                providerWriteStarted
                    ? "Managed Flink snapshot outcome requires reconciliation."
                    : "Managed Flink snapshot operation could not be prepared.");
            log.warn(
                "Managed Flink snapshot operation {} failed in local orchestration.",
                operationId,
                exception);
        }
    }

    private String result(ManagedSnapshotCreateResult value) {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("jobId", value.getJobId());
        result.put("instanceId", value.getInstanceId());
        result.put("providerState", value.getProviderState());
        try {
            return objectMapper.writeValueAsString(result);
        } catch (Exception exception) {
            throw new IllegalStateException("Managed Flink snapshot result cannot be serialized.");
        }
    }

    private static String safeCode(String value) {
        return value != null && SAFE_CODE.matcher(value).matches()
            ? value
            : "UnknownProviderError";
    }
}
