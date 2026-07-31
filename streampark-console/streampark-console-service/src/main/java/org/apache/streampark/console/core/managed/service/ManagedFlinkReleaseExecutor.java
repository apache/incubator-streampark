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
import org.apache.streampark.console.core.managed.api.ManagedDeployRequest;
import org.apache.streampark.console.core.managed.api.ManagedDeployment;
import org.apache.streampark.console.core.managed.api.ManagedDraft;
import org.apache.streampark.console.core.managed.api.ManagedDraftRequest;
import org.apache.streampark.console.core.managed.api.ManagedFlinkProviderException;
import org.apache.streampark.console.core.managed.model.ManagedFlinkReleaseSnapshot;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/** Executes one accepted release snapshot without retrying provider writes. */
@Slf4j
@Service
@RequiredArgsConstructor
class ManagedFlinkReleaseExecutor {

    private static final Pattern SAFE_CODE = Pattern.compile("[A-Za-z0-9._-]{1,128}");

    private final ManagedFlinkOperationService operationService;
    private final ManagedFlinkProviderContextService providerContextService;
    private final ManagedFlinkReleaseStateService releaseStateService;
    private final ObjectMapper objectMapper;

    void execute(Long operationId) {
        if (!operationService.markRunning(operationId)) {
            return;
        }
        ManagedFlinkOperation operation = operationService.getRequired(operationId);
        ManagedFlinkReleaseSnapshot snapshot;
        try {
            snapshot = read(operation.getRequestJson());
        } catch (Exception exception) {
            operationService.markFailed(
                operationId,
                false,
                null,
                "INTERNAL:InvalidReleaseSnapshot",
                "Managed Flink release snapshot is invalid.");
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
            ManagedDraft draft =
                session.getProvider().upsertDraft(
                    session.getContext(),
                    ManagedDraftRequest.builder()
                        .existingDraftId(snapshot.getExistingDraftId())
                        .projectId(snapshot.getProjectId())
                        .directoryId(snapshot.getDraftDirectoryId())
                        .jobName(snapshot.getJobName())
                        .jobType(snapshot.getJobType())
                        .engineVersion(snapshot.getEngineVersion())
                        .sqlText(snapshot.getSqlText())
                        .optionsJson(snapshot.getOptionsJson())
                        .dynamicOptionsJson(snapshot.getDynamicOptionsJson())
                        .dependencyJson(snapshot.getDependencyJson())
                        .definitionHash(snapshot.getDefinitionHash())
                        .build());
            releaseStateService.recordDraft(snapshot.getAppId(), draft.getDraftId());
            operationService.recordProviderProgress(
                operationId,
                draft.getProviderRequestId(),
                null,
                result("draftId", draft.getDraftId()));

            ManagedDeployment deployment =
                session.getProvider().deployDraft(
                    session.getContext(),
                    ManagedDeployRequest.builder()
                        .draftId(draft.getDraftId())
                        .projectId(snapshot.getProjectId())
                        .resourcePool(snapshot.getResourcePoolName())
                        .queue(snapshot.getResourcePoolId())
                        .priority(snapshot.getPriority())
                        .schedulePolicy(snapshot.getSchedulePolicy())
                        .scheduleTimeoutSeconds(snapshot.getScheduleTimeoutSeconds())
                        .definitionHash(snapshot.getDefinitionHash())
                        .build());
            releaseStateService.recordDeployment(
                snapshot, draft.getDraftId(), deployment.getApplicationId());
            operationService.markSucceeded(
                operationId,
                deployment.getProviderRequestId(),
                deployment.getProviderOperationId(),
                deploymentResult(draft.getDraftId(), deployment.getApplicationId()));
        } catch (ManagedFlinkProviderException exception) {
            boolean outcomeUnknown = exception.isRetryable();
            persistFailure(
                operationId,
                snapshot,
                outcomeUnknown,
                exception.getProviderRequestId(),
                exception.getCategory().name() + ":" + safeCode(exception.getProviderCode()),
                outcomeUnknown
                    ? "Managed Flink provider write outcome is unknown."
                    : "Managed Flink provider rejected the release.");
        } catch (Exception exception) {
            persistFailure(
                operationId,
                snapshot,
                providerWriteStarted,
                null,
                providerWriteStarted
                    ? "UNKNOWN:ReleaseExecutionError"
                    : "INTERNAL:ReleasePreparationError",
                providerWriteStarted
                    ? "Managed Flink release outcome requires reconciliation."
                    : "Managed Flink release could not be prepared.");
            log.warn(
                "Managed Flink release operation {} failed in local orchestration.",
                operationId);
        }
    }

    private void persistFailure(
                                Long operationId,
                                ManagedFlinkReleaseSnapshot snapshot,
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
            if (!outcomeUnknown) {
                releaseStateService.markKnownFailure(
                    snapshot.getAppId(), snapshot.getDefinitionHash());
            }
        } catch (Exception persistenceFailure) {
            log.error(
                "Managed Flink release operation {} failure state could not be persisted.",
                operationId);
        }
    }

    private ManagedFlinkReleaseSnapshot read(String value) {
        try {
            return objectMapper.readValue(value, ManagedFlinkReleaseSnapshot.class);
        } catch (Exception exception) {
            throw new IllegalStateException("Invalid managed Flink release snapshot.");
        }
    }

    private String result(String key, String value) {
        Map<String, String> result = new LinkedHashMap<>();
        result.put(key, value);
        return write(result);
    }

    private String deploymentResult(String draftId, String applicationId) {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("draftId", draftId);
        result.put("applicationId", applicationId);
        return write(result);
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Managed Flink operation result cannot be serialized.");
        }
    }

    private static String safeCode(String value) {
        return value != null && SAFE_CODE.matcher(value).matches()
            ? value
            : "UnknownProviderError";
    }
}
