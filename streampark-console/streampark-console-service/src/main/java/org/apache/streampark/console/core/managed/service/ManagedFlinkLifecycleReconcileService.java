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

import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.core.entity.ManagedFlinkOperation;
import org.apache.streampark.console.core.managed.api.ManagedFlinkProviderException;
import org.apache.streampark.console.core.managed.api.ManagedJobActionResult;
import org.apache.streampark.console.core.managed.api.ManagedJobLookupRequest;
import org.apache.streampark.console.core.managed.api.ManagedJobState;
import org.apache.streampark.console.core.managed.api.ManagedJobStatus;
import org.apache.streampark.console.core.managed.model.ManagedFlinkLifecycleSnapshot;
import org.apache.streampark.console.core.managed.model.ManagedFlinkOperationView;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Reconciles an unknown lifecycle write through provider job lookup without replay. */
@Service
@RequiredArgsConstructor
public class ManagedFlinkLifecycleReconcileService {

    private final ManagedFlinkApplicationService applicationService;
    private final ManagedFlinkOperationService operationService;
    private final ManagedFlinkProviderContextService providerContextService;
    private final ManagedFlinkLifecycleStateService lifecycleStateService;
    private final ObjectMapper objectMapper;

    public ManagedFlinkOperationView reconcile(Long teamId, Long appId, Long operationId) {
        applicationService.get(teamId, appId);
        ManagedFlinkOperation operation =
            operationService.getRequired(appId, operationId);
        ApiAlertException.throwIfFalse(
            isLifecycle(operation.getOperationType()),
            "Only managed Flink lifecycle operations can use lifecycle reconciliation.");
        if (!"UNKNOWN".equals(operation.getState())) {
            return operationService.getView(appId, operationId);
        }
        ManagedFlinkLifecycleSnapshot snapshot = read(operation.getRequestJson());
        ApiAlertException.throwIfFalse(
            teamId.equals(snapshot.getTeamId())
                && appId.equals(snapshot.getAppId())
                && operation.getOperationType().equals(snapshot.getOperationType()),
            "Managed Flink lifecycle snapshot ownership is invalid.");
        if (!operationService.markReconciling(operationId)) {
            return operationService.getView(appId, operationId);
        }

        try {
            ManagedFlinkProviderSession session =
                providerContextService.resolve(
                    teamId, snapshot.getCloudAccountId(), snapshot.getProjectId());
            ApiAlertException.throwIfFalse(
                session.getProviderType().name().equals(snapshot.getProviderType()),
                "Managed Flink provider routing changed.");
            ManagedJobStatus status =
                session.getProvider().getJob(
                    session.getContext(),
                    ManagedJobLookupRequest.builder()
                        .projectId(snapshot.getProjectId())
                        .jobName(snapshot.getJobName())
                        .jobId(snapshot.getJobId())
                        .build());
            if (!confirmsAcceptance(snapshot, status)) {
                return remainUnknown(
                    appId,
                    operationId,
                    status == null ? null : status.getProviderRequestId(),
                    "UNKNOWN:LifecycleTransitionNotVisible",
                    "Managed Flink lifecycle transition is not visible during reconciliation.");
            }
            ManagedJobActionResult result =
                ManagedJobActionResult.builder()
                    .jobId(status.getJobId())
                    .instanceId(status.getInstanceId())
                    .providerRequestId(status.getProviderRequestId())
                    .providerState(status.getProviderState())
                    .build();
            lifecycleStateService.recordAccepted(snapshot, result);
            operationService.markSucceeded(
                operationId,
                status.getProviderRequestId(),
                null,
                result(status));
            return operationService.getView(appId, operationId);
        } catch (ManagedFlinkProviderException exception) {
            return remainUnknown(
                appId,
                operationId,
                exception.getProviderRequestId(),
                exception.getCategory().name() + ":ReconcileLookupFailed",
                "Managed Flink lifecycle reconciliation could not determine the outcome.");
        } catch (Exception exception) {
            return remainUnknown(
                appId,
                operationId,
                null,
                "UNKNOWN:ReconcileExecutionError",
                "Managed Flink lifecycle reconciliation could not determine the outcome.");
        }
    }

    private ManagedFlinkOperationView remainUnknown(
                                                    Long appId,
                                                    Long operationId,
                                                    String providerRequestId,
                                                    String errorCode,
                                                    String errorMessage) {
        operationService.markFailed(
            operationId, true, providerRequestId, errorCode, errorMessage);
        return operationService.getView(appId, operationId);
    }

    private ManagedFlinkLifecycleSnapshot read(String value) {
        try {
            return objectMapper.readValue(value, ManagedFlinkLifecycleSnapshot.class);
        } catch (Exception exception) {
            throw new ApiAlertException("Managed Flink lifecycle snapshot is invalid.");
        }
    }

    private boolean confirmsAcceptance(
                                       ManagedFlinkLifecycleSnapshot snapshot,
                                       ManagedJobStatus status) {
        if (status == null
            || !snapshot.getJobId().equals(status.getJobId())
            || status.getState() == null) {
            return false;
        }
        boolean newInstance =
            status.getInstanceId() != null
                && !Objects.equals(snapshot.getInstanceId(), status.getInstanceId());
        switch (snapshot.getOperationType()) {
            case "START":
                return status.getState() == ManagedJobState.STARTING
                    || status.getState() == ManagedJobState.RUNNING
                        && (snapshot.getInstanceId() == null || newInstance);
            case "STOP":
                return status.getState() == ManagedJobState.STOPPING
                    || status.getState() == ManagedJobState.STOPPED
                    || status.getState() == ManagedJobState.SUCCEEDED;
            case "RESTART":
                return status.getState() == ManagedJobState.RESTARTING
                    || status.getState() == ManagedJobState.STARTING
                    || status.getState() == ManagedJobState.RUNNING && newInstance;
            default:
                return false;
        }
    }

    private String result(ManagedJobStatus status) {
        try {
            Map<String, String> value = new LinkedHashMap<>();
            value.put("jobId", status.getJobId());
            value.put("instanceId", status.getInstanceId());
            value.put("providerState", status.getProviderState());
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new ApiAlertException("Managed Flink lifecycle reconciliation result is invalid.");
        }
    }

    private static boolean isLifecycle(String operationType) {
        return "START".equals(operationType)
            || "STOP".equals(operationType)
            || "RESTART".equals(operationType);
    }
}
