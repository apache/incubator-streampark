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
import org.apache.streampark.console.core.entity.ManagedFlinkApplication;
import org.apache.streampark.console.core.entity.ManagedFlinkOperation;
import org.apache.streampark.console.core.managed.api.ManagedDeployment;
import org.apache.streampark.console.core.managed.api.ManagedDeploymentLookupRequest;
import org.apache.streampark.console.core.managed.api.ManagedFlinkProviderException;
import org.apache.streampark.console.core.managed.model.ManagedFlinkOperationView;
import org.apache.streampark.console.core.managed.model.ManagedFlinkReleaseSnapshot;
import org.apache.streampark.console.core.mapper.ManagedFlinkApplicationMapper;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Reconciles an unknown release through provider application lookup without replaying writes. */
@Service
@RequiredArgsConstructor
public class ManagedFlinkReleaseReconcileService {

    private final ManagedFlinkApplicationService applicationService;
    private final ManagedFlinkApplicationMapper managedApplicationMapper;
    private final ManagedFlinkOperationService operationService;
    private final ManagedFlinkProviderContextService providerContextService;
    private final ManagedFlinkReleaseStateService releaseStateService;
    private final ObjectMapper objectMapper;

    public ManagedFlinkOperationView reconcile(Long teamId, Long appId, Long operationId) {
        applicationService.get(teamId, appId);
        ManagedFlinkOperation operation =
            operationService.getRequired(appId, operationId);
        ApiAlertException.throwIfFalse(
            "RELEASE".equals(operation.getOperationType()),
            "Only managed Flink release operations can use release reconciliation.");
        if (!"UNKNOWN".equals(operation.getState())) {
            return operationService.getView(appId, operationId);
        }
        ManagedFlinkReleaseSnapshot snapshot = read(operation.getRequestJson());
        ApiAlertException.throwIfFalse(
            teamId.equals(snapshot.getTeamId()) && appId.equals(snapshot.getAppId()),
            "Managed Flink release snapshot ownership is invalid.");
        if (!operationService.markReconciling(operationId)) {
            return operationService.getView(appId, operationId);
        }

        ManagedFlinkApplication managed = managedApplicationMapper.selectById(appId);
        String draftId = managed == null ? null : managed.getExternalDraftId();
        if (StringUtils.isBlank(draftId)) {
            return remainUnknown(
                appId,
                operationId,
                null,
                "UNKNOWN:DraftIdentityUnavailable",
                "Managed Flink draft identity is unavailable for reconciliation.");
        }
        try {
            ManagedFlinkProviderSession session =
                providerContextService.resolve(
                    teamId, snapshot.getCloudAccountId(), snapshot.getProjectId());
            ApiAlertException.throwIfFalse(
                session.getProviderType().name().equals(snapshot.getProviderType()),
                "Managed Flink provider routing changed.");
            ManagedDeployment deployment =
                session.getProvider().findDeployment(
                    session.getContext(),
                    ManagedDeploymentLookupRequest.builder()
                        .projectId(snapshot.getProjectId())
                        .draftId(draftId)
                        .jobName(snapshot.getJobName())
                        .definitionHash(snapshot.getDefinitionHash())
                        .build());
            if (deployment == null || StringUtils.isBlank(deployment.getApplicationId())) {
                return remainUnknown(
                    appId,
                    operationId,
                    null,
                    "UNKNOWN:DeploymentNotVisible",
                    "Managed Flink deployment is not visible during reconciliation.");
            }
            releaseStateService.recordDeployment(
                snapshot, draftId, deployment.getApplicationId());
            operationService.markSucceeded(
                operationId,
                deployment.getProviderRequestId(),
                deployment.getProviderOperationId(),
                result(draftId, deployment));
            return operationService.getView(appId, operationId);
        } catch (ManagedFlinkProviderException exception) {
            return remainUnknown(
                appId,
                operationId,
                exception.getProviderRequestId(),
                exception.getCategory().name() + ":ReconcileLookupFailed",
                "Managed Flink release reconciliation could not determine the outcome.");
        } catch (Exception exception) {
            return remainUnknown(
                appId,
                operationId,
                null,
                "UNKNOWN:ReconcileExecutionError",
                "Managed Flink release reconciliation could not determine the outcome.");
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

    private ManagedFlinkReleaseSnapshot read(String value) {
        try {
            return objectMapper.readValue(value, ManagedFlinkReleaseSnapshot.class);
        } catch (Exception exception) {
            throw new ApiAlertException("Managed Flink release snapshot is invalid.");
        }
    }

    private String result(String draftId, ManagedDeployment deployment) {
        try {
            java.util.Map<String, String> value = new java.util.LinkedHashMap<>();
            value.put("draftId", draftId);
            value.put("applicationId", deployment.getApplicationId());
            value.put("providerState", deployment.getProviderState());
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new ApiAlertException("Managed Flink reconciliation result is invalid.");
        }
    }
}
