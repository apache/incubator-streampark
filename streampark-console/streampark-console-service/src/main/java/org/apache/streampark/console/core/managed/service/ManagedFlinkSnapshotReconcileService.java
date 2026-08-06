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
import org.apache.streampark.console.core.managed.api.ManagedSnapshot;
import org.apache.streampark.console.core.managed.api.ManagedSnapshotLookupRequest;
import org.apache.streampark.console.core.managed.api.ManagedSnapshotState;
import org.apache.streampark.console.core.managed.model.ManagedFlinkOperationView;
import org.apache.streampark.console.core.managed.model.ManagedFlinkSnapshotOperationSnapshot;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Reconciles an accepted snapshot mutation through provider reads only. */
@Service
@RequiredArgsConstructor
@Slf4j
public class ManagedFlinkSnapshotReconcileService {

    private final ManagedFlinkApplicationService applicationService;
    private final ManagedFlinkOperationService operationService;
    private final ManagedFlinkProviderContextService providerContextService;
    private final ManagedFlinkSnapshotServiceImpl snapshotService;
    private final ObjectMapper objectMapper;

    public ManagedFlinkOperationView reconcile(Long teamId, Long appId, Long operationId) {
        ManagedFlinkOperation operation =
            operationService.getRequired(appId, operationId);
        ApiAlertException.throwIfFalse(
            "SNAPSHOT".equals(operation.getOperationType()),
            "Managed Flink operation is not a snapshot operation.");
        applicationService.get(teamId, appId);
        if ("SUCCEEDED".equals(operation.getState()) || "FAILED".equals(operation.getState())) {
            return operationService.getView(appId, operationId);
        }
        ManagedFlinkSnapshotOperationSnapshot request = read(operation.getRequestJson());
        ApiAlertException.throwIfFalse(
            teamId.equals(request.getTeamId()) && appId.equals(request.getAppId()),
            "Managed Flink snapshot operation ownership is invalid.");
        ApiAlertException.throwIfFalse(
            operationService.markReconciling(operationId),
            "Managed Flink snapshot operation is not ready for reconciliation.");
        try {
            ManagedFlinkProviderSession session =
                providerContextService.resolve(
                    teamId,
                    request.getCloudAccountId(),
                    request.getProviderConfigJson(),
                    request.getProviderConfigVersion());
            ApiAlertException.throwIfFalse(
                session.getProviderType().name().equals(request.getProviderType()),
                "Managed Flink provider routing has changed.");
            List<ManagedSnapshot> snapshots =
                session
                    .getProvider()
                    .listSnapshots(
                        session.getContext(),
                        ManagedSnapshotLookupRequest.builder()
                            .jobId(request.getJobId())
                            .build());
            if (snapshots == null) {
                snapshots = Collections.emptyList();
            }
            snapshotService.cacheProviderSnapshots(appId, snapshots);
            ManagedSnapshot matched = match(request, snapshots);
            if (matched == null
                || matched.getState() == null
                || matched.getState() == ManagedSnapshotState.OTHER) {
                operationService.markAwaitingReconcile(
                    operationId,
                    operation.getProviderRequestId(),
                    operation.getProviderOperationId(),
                    operation.getResultJson());
            } else if (matched.getState() == ManagedSnapshotState.CREATING
                || matched.getState() == ManagedSnapshotState.COMPLETED) {
                operationService.markSucceeded(
                    operationId,
                    operation.getProviderRequestId(),
                    operation.getProviderOperationId(),
                    result(matched));
            } else {
                operationService.markFailed(
                    operationId,
                    false,
                    operation.getProviderRequestId(),
                    "PROVIDER:Snapshot" + matched.getState().name(),
                    "Managed Flink snapshot did not complete successfully.");
            }
        } catch (ManagedFlinkProviderException exception) {
            operationService.markFailed(
                operationId,
                true,
                operation.getProviderRequestId(),
                exception.getCategory().name() + ":SnapshotLookupFailed",
                "Managed Flink snapshot reconciliation is temporarily unavailable.");
        } catch (Exception exception) {
            log.warn(
                "Managed Flink snapshot operation {} reconciliation failed.",
                operationId,
                exception);
            operationService.markFailed(
                operationId,
                true,
                operation.getProviderRequestId(),
                "UNKNOWN:SnapshotReconcileError",
                "Managed Flink snapshot reconciliation is temporarily unavailable.");
        }
        return operationService.getView(appId, operationId);
    }

    private ManagedFlinkSnapshotOperationSnapshot read(String value) {
        try {
            return objectMapper.readValue(
                value, ManagedFlinkSnapshotOperationSnapshot.class);
        } catch (Exception exception) {
            throw new ApiAlertException("Managed Flink snapshot operation is invalid.");
        }
    }

    private static ManagedSnapshot match(
                                         ManagedFlinkSnapshotOperationSnapshot request,
                                         List<ManagedSnapshot> snapshots) {
        Set<String> baseline =
            request.getBaselineSnapshotIds() == null
                ? Collections.emptySet()
                : new HashSet<>(request.getBaselineSnapshotIds());
        Comparator<ManagedSnapshot> newestFirst =
            Comparator.comparing(
                value -> defaultValue(
                    value.getCompletionTime(),
                    defaultValue(value.getTriggerTime(), "")));
        List<ManagedSnapshot> candidates =
            snapshots.stream()
                .filter(
                    value -> value != null
                        && value.getSnapshotId() != null
                        && !baseline.contains(value.getSnapshotId()))
                .collect(java.util.stream.Collectors.toList());
        ManagedSnapshot descriptionMatch =
            candidates.stream()
                .filter(
                    value -> request.getProviderDescription() != null
                        && request.getProviderDescription().equals(value.getDescription()))
                .max(newestFirst)
                .orElse(null);
        if (descriptionMatch != null) {
            return descriptionMatch;
        }
        return candidates.stream()
            .max(newestFirst)
            .orElse(null);
    }

    private String result(ManagedSnapshot value) {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("snapshotId", value.getSnapshotId());
        result.put("providerState", value.getProviderState());
        result.put("location", value.getLocation());
        try {
            return objectMapper.writeValueAsString(result);
        } catch (Exception exception) {
            throw new IllegalStateException("Managed Flink snapshot result cannot be serialized.");
        }
    }

    private static String defaultValue(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }
}
