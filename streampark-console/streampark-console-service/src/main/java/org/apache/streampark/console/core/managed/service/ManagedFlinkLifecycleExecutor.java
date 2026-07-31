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
import org.apache.streampark.console.core.managed.api.ManagedJobActionResult;
import org.apache.streampark.console.core.managed.api.ManagedJobRestartRequest;
import org.apache.streampark.console.core.managed.api.ManagedJobStartRequest;
import org.apache.streampark.console.core.managed.api.ManagedJobStopRequest;
import org.apache.streampark.console.core.managed.model.ManagedFlinkLifecycleSnapshot;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/** Executes one accepted lifecycle write without retrying a provider mutation. */
@Slf4j
@Service
@RequiredArgsConstructor
class ManagedFlinkLifecycleExecutor {

    private static final Pattern SAFE_CODE = Pattern.compile("[A-Za-z0-9._-]{1,128}");

    private final ManagedFlinkOperationService operationService;
    private final ManagedFlinkProviderContextService providerContextService;
    private final ManagedFlinkLifecycleStateService lifecycleStateService;
    private final ObjectMapper objectMapper;

    void execute(Long operationId) {
        if (!operationService.markRunning(operationId)) {
            return;
        }
        ManagedFlinkOperation operation = operationService.getRequired(operationId);
        ManagedFlinkLifecycleSnapshot snapshot;
        try {
            snapshot =
                objectMapper.readValue(
                    operation.getRequestJson(), ManagedFlinkLifecycleSnapshot.class);
        } catch (Exception exception) {
            operationService.markFailed(
                operationId,
                false,
                null,
                "INTERNAL:InvalidLifecycleSnapshot",
                "Managed Flink lifecycle snapshot is invalid.");
            return;
        }

        boolean providerWriteStarted = false;
        try {
            ManagedFlinkProviderSession session =
                providerContextService.resolve(
                    snapshot.getTeamId(),
                    snapshot.getCloudAccountId(),
                    snapshot.getProjectId());
            if (!session.getProviderType().name().equals(snapshot.getProviderType())) {
                throw new IllegalStateException("Managed Flink provider routing changed.");
            }
            providerWriteStarted = true;
            ManagedJobActionResult result = invoke(session, snapshot);
            lifecycleStateService.recordAccepted(snapshot, result);
            operationService.markSucceeded(
                operationId,
                result.getProviderRequestId(),
                result.getProviderOperationId(),
                result(result));
        } catch (ManagedFlinkProviderException exception) {
            boolean outcomeUnknown = exception.isRetryable();
            persistFailure(
                operationId,
                snapshot,
                outcomeUnknown,
                exception.getProviderRequestId(),
                exception.getCategory().name() + ":" + safeCode(exception.getProviderCode()),
                outcomeUnknown
                    ? "Managed Flink provider lifecycle outcome is unknown."
                    : "Managed Flink provider rejected the lifecycle operation.");
        } catch (Exception exception) {
            persistFailure(
                operationId,
                snapshot,
                providerWriteStarted,
                null,
                providerWriteStarted
                    ? "UNKNOWN:LifecycleExecutionError"
                    : "INTERNAL:LifecyclePreparationError",
                providerWriteStarted
                    ? "Managed Flink lifecycle outcome requires reconciliation."
                    : "Managed Flink lifecycle operation could not be prepared.");
            log.warn(
                "Managed Flink lifecycle operation {} failed in local orchestration.",
                operationId);
        }
    }

    private ManagedJobActionResult invoke(
                                          ManagedFlinkProviderSession session,
                                          ManagedFlinkLifecycleSnapshot snapshot) {
        switch (snapshot.getOperationType()) {
            case "START":
                return session
                    .getProvider()
                    .startJob(
                        session.getContext(),
                        ManagedJobStartRequest.builder()
                            .jobId(snapshot.getJobId())
                            .resourcePool(snapshot.getResourcePool())
                            .queue(snapshot.getQueue())
                            .priority(snapshot.getPriority())
                            .schedulePolicy(snapshot.getSchedulePolicy())
                            .scheduleTimeoutSeconds(snapshot.getScheduleTimeoutSeconds())
                            .restoreMode(snapshot.getRestoreMode())
                            .snapshotId(snapshot.getSnapshotId())
                            .build());
            case "STOP":
                return session
                    .getProvider()
                    .stopJob(
                        session.getContext(),
                        ManagedJobStopRequest.builder()
                            .jobId(snapshot.getJobId())
                            .instanceId(snapshot.getInstanceId())
                            .withSnapshot(snapshot.isWithSnapshot())
                            .build());
            case "RESTART":
                return session
                    .getProvider()
                    .restartJob(
                        session.getContext(),
                        ManagedJobRestartRequest.builder()
                            .jobId(snapshot.getJobId())
                            .restoreMode(snapshot.getRestoreMode())
                            .snapshotId(snapshot.getSnapshotId())
                            .build());
            default:
                throw new IllegalStateException("Unsupported managed Flink lifecycle operation.");
        }
    }

    private void persistFailure(
                                Long operationId,
                                ManagedFlinkLifecycleSnapshot snapshot,
                                boolean outcomeUnknown,
                                String providerRequestId,
                                String errorCode,
                                String errorMessage) {
        try {
            operationService.markFailed(
                operationId,
                outcomeUnknown,
                providerRequestId,
                errorCode,
                errorMessage);
            if (outcomeUnknown) {
                lifecycleStateService.recordOutcomeUnknown(snapshot);
            }
        } catch (Exception persistenceFailure) {
            log.error(
                "Managed Flink lifecycle operation {} failure state could not be persisted.",
                operationId);
        }
    }

    private String result(ManagedJobActionResult action) {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("jobId", action.getJobId());
        result.put("instanceId", action.getInstanceId());
        result.put("providerState", action.getProviderState());
        try {
            return objectMapper.writeValueAsString(result);
        } catch (Exception exception) {
            throw new IllegalStateException("Managed Flink lifecycle result cannot be serialized.");
        }
    }

    private static String safeCode(String value) {
        return value != null && SAFE_CODE.matcher(value).matches()
            ? value
            : "UnknownProviderError";
    }
}
