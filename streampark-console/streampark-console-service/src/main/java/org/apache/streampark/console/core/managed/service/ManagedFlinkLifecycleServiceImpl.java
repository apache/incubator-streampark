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
import org.apache.streampark.console.core.entity.FlinkApplication;
import org.apache.streampark.console.core.entity.ManagedFlinkApplication;
import org.apache.streampark.console.core.entity.ManagedFlinkOperation;
import org.apache.streampark.console.core.enums.FlinkAppStateEnum;
import org.apache.streampark.console.core.managed.api.ManagedJobRestoreMode;
import org.apache.streampark.console.core.managed.model.ManagedFlinkLifecycleRequest;
import org.apache.streampark.console.core.managed.model.ManagedFlinkLifecycleSnapshot;
import org.apache.streampark.console.core.managed.model.ManagedFlinkOperationView;
import org.apache.streampark.console.core.managed.model.ManagedFlinkReleaseSnapshot;
import org.apache.streampark.console.core.managed.model.ManagedFlinkSnapshotView;
import org.apache.streampark.console.core.managed.model.ManagedFlinkStopRequest;
import org.apache.streampark.console.core.mapper.FlinkApplicationMapper;
import org.apache.streampark.console.core.mapper.ManagedFlinkApplicationMapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** Validates, snapshots, and dispatches managed Flink lifecycle operations. */
@Service
@RequiredArgsConstructor
public class ManagedFlinkLifecycleServiceImpl implements ManagedFlinkLifecycleService {

    private static final Set<FlinkAppStateEnum> STARTABLE_STATES =
        new HashSet<>(Arrays.asList(FlinkAppStateEnum.ADDED, FlinkAppStateEnum.CANCELED));

    private final FlinkApplicationMapper applicationMapper;
    private final ManagedFlinkApplicationMapper managedApplicationMapper;
    private final ManagedFlinkDeployedDefinitionService deployedDefinitionService;
    private final ManagedFlinkSnapshotService snapshotService;
    private final ManagedFlinkOperationService operationService;
    private final ManagedFlinkLifecycleDispatcher dispatcher;
    private final ObjectMapper objectMapper;

    @Override
    public ManagedFlinkOperationView start(ManagedFlinkLifecycleRequest request) {
        ManagedFlinkOperationView replay =
            replay(
                request.getTeamId(),
                request.getAppId(),
                "START",
                request.getIdempotencyKey(),
                request.getRestoreMode(),
                request.getSnapshotId(),
                false);
        if (replay != null) {
            return replay;
        }
        ManagedFlinkLifecycleSnapshot snapshot =
            snapshot(
                request.getTeamId(),
                request.getAppId(),
                "START",
                request.getRestoreMode(),
                request.getSnapshotId(),
                false);
        FlinkApplication application = applicationMapper.selectById(request.getAppId());
        ApiAlertException.throwIfFalse(
            STARTABLE_STATES.contains(FlinkAppStateEnum.getState(application.getState())),
            "Managed Flink job is not in a startable state.");
        if (snapshot.getInstanceId() == null) {
            ApiAlertException.throwIfFalse(
                request.getRestoreMode() == ManagedJobRestoreMode.FRESH,
                "The first managed Flink start must use FRESH mode.");
        }
        return accept(snapshot, request.getIdempotencyKey());
    }

    @Override
    public ManagedFlinkOperationView stop(ManagedFlinkStopRequest request) {
        ManagedFlinkOperationView replay =
            replay(
                request.getTeamId(),
                request.getAppId(),
                "STOP",
                request.getIdempotencyKey(),
                null,
                null,
                request.isWithSnapshot());
        if (replay != null) {
            return replay;
        }
        ApiAlertException.throwIfTrue(
            request.isWithSnapshot(),
            "Managed Flink stop with snapshot is delivered by the snapshot lifecycle slice.");
        ManagedFlinkLifecycleSnapshot snapshot =
            snapshot(
                request.getTeamId(),
                request.getAppId(),
                "STOP",
                null,
                null,
                request.isWithSnapshot());
        FlinkApplication application = applicationMapper.selectById(request.getAppId());
        ApiAlertException.throwIfFalse(
            FlinkAppStateEnum.getState(application.getState()) == FlinkAppStateEnum.RUNNING,
            "Managed Flink job is not running.");
        ApiAlertException.throwIfTrue(
            snapshot.getInstanceId() == null || snapshot.getInstanceId().trim().isEmpty(),
            "Managed Flink running instance ID is unavailable.");
        return accept(snapshot, request.getIdempotencyKey());
    }

    @Override
    public ManagedFlinkOperationView restart(ManagedFlinkLifecycleRequest request) {
        ManagedFlinkOperationView replay =
            replay(
                request.getTeamId(),
                request.getAppId(),
                "RESTART",
                request.getIdempotencyKey(),
                request.getRestoreMode(),
                request.getSnapshotId(),
                false);
        if (replay != null) {
            return replay;
        }
        ManagedFlinkLifecycleSnapshot snapshot =
            snapshot(
                request.getTeamId(),
                request.getAppId(),
                "RESTART",
                request.getRestoreMode(),
                request.getSnapshotId(),
                false);
        FlinkApplication application = applicationMapper.selectById(request.getAppId());
        ApiAlertException.throwIfFalse(
            FlinkAppStateEnum.getState(application.getState()) == FlinkAppStateEnum.RUNNING,
            "Managed Flink job is not running.");
        return accept(snapshot, request.getIdempotencyKey());
    }

    private ManagedFlinkOperationView replay(
                                             Long teamId,
                                             Long appId,
                                             String operationType,
                                             String idempotencyKey,
                                             ManagedJobRestoreMode restoreMode,
                                             String snapshotId,
                                             boolean withSnapshot) {
        ManagedFlinkOperation existing =
            operationService.findByIdempotency(appId, operationType, idempotencyKey);
        if (existing == null) {
            return null;
        }
        FlinkApplication application = applicationMapper.selectById(appId);
        ApiAlertException.throwIfTrue(
            application == null || !teamId.equals(application.getTeamId()),
            "Managed Flink application does not exist.");
        ManagedFlinkLifecycleSnapshot snapshot;
        try {
            snapshot =
                objectMapper.readValue(
                    existing.getRequestJson(), ManagedFlinkLifecycleSnapshot.class);
        } catch (Exception exception) {
            throw new ApiAlertException("Managed Flink lifecycle operation is invalid.");
        }
        ApiAlertException.throwIfFalse(
            teamId.equals(snapshot.getTeamId())
                && operationType.equals(snapshot.getOperationType())
                && restoreMode == snapshot.getRestoreMode()
                && Objects.equals(trimToNull(snapshotId), trimToNull(snapshot.getSnapshotId()))
                && withSnapshot == snapshot.isWithSnapshot(),
            "The idempotency key was already used for a different request.");
        return ManagedFlinkOperationView.builder()
            .operationId(existing.getId())
            .appId(existing.getAppId())
            .type(existing.getOperationType())
            .state(existing.getState())
            .idempotentReplay(true)
            .providerRequestId(existing.getProviderRequestId())
            .errorCode(existing.getErrorCode())
            .errorMessage(existing.getErrorMessage())
            .createUserId(existing.getCreateUserId())
            .createTime(existing.getCreateTime())
            .startTime(existing.getStartTime())
            .finishTime(existing.getFinishTime())
            .build();
    }

    private ManagedFlinkLifecycleSnapshot snapshot(
                                                   Long teamId,
                                                   Long appId,
                                                   String operationType,
                                                   ManagedJobRestoreMode restoreMode,
                                                   String snapshotId,
                                                   boolean withSnapshot) {
        ApiAlertException.throwIfTrue(
            restoreMode != ManagedJobRestoreMode.SPECIFIED_SNAPSHOT
                && snapshotId != null
                && !snapshotId.trim().isEmpty(),
            "Managed Flink snapshot ID is only valid with SPECIFIED_SNAPSHOT mode.");

        ManagedFlinkReleaseSnapshot deployed =
            deployedDefinitionService.getRequired(teamId, appId);
        ManagedFlinkApplication managed = managedApplicationMapper.selectById(appId);
        ApiAlertException.throwIfTrue(
            managed == null
                || managed.getExternalApplicationId() == null
                || managed.getExternalApplicationId().trim().isEmpty(),
            "Managed Flink deployed job ID is unavailable.");

        ManagedFlinkLifecycleSnapshot snapshot = new ManagedFlinkLifecycleSnapshot();
        snapshot.setTeamId(teamId);
        snapshot.setAppId(appId);
        snapshot.setCloudAccountId(deployed.getCloudAccountId());
        snapshot.setProviderType(deployed.getProviderType());
        snapshot.setProjectId(deployed.getProjectId());
        snapshot.setJobName(deployed.getJobName());
        snapshot.setOperationType(operationType);
        snapshot.setJobId(managed.getExternalApplicationId());
        snapshot.setInstanceId(managed.getExternalInstanceId());
        snapshot.setResourcePool(deployed.getResourcePoolName());
        snapshot.setQueue(deployed.getResourcePoolId());
        snapshot.setPriority(deployed.getPriority());
        snapshot.setSchedulePolicy(deployed.getSchedulePolicy());
        snapshot.setScheduleTimeoutSeconds(deployed.getScheduleTimeoutSeconds());
        snapshot.setRestoreMode(restoreMode);
        snapshot.setSnapshotId(trimToNull(snapshotId));
        if (restoreMode == ManagedJobRestoreMode.SPECIFIED_SNAPSHOT) {
            ManagedFlinkSnapshotView restorable =
                snapshotService.getRestorable(teamId, appId, snapshotId);
            snapshot.setSnapshotSourceInstanceId(restorable.getInstanceId());
        }
        snapshot.setWithSnapshot(withSnapshot);
        snapshot.setDeployedDefinitionHash(deployed.getDefinitionHash());
        return snapshot;
    }

    private ManagedFlinkOperationView accept(
                                             ManagedFlinkLifecycleSnapshot snapshot,
                                             String idempotencyKey) {
        String requestJson = write(snapshot);
        String requestHash = hash(requestJson);
        ManagedFlinkOperationView operation =
            operationService.accept(
                snapshot.getAppId(),
                snapshot.getOperationType(),
                idempotencyKey,
                requestHash,
                requestJson);
        if (operation.isIdempotentReplay()) {
            return operation;
        }
        if (!dispatcher.dispatch(operation.getOperationId())) {
            if (operationService.markRunning(operation.getOperationId())) {
                operationService.markFailed(
                    operation.getOperationId(),
                    false,
                    null,
                    "INTERNAL:ExecutorRejected",
                    "Managed Flink lifecycle executor is temporarily unavailable.");
            }
            return operationService.getView(
                snapshot.getAppId(), operation.getOperationId());
        }
        return operation;
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new ApiAlertException("Managed Flink lifecycle request cannot be serialized.");
        }
    }

    private String hash(String value) {
        try {
            byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                result.append(String.format("%02x", item & 0xff));
            }
            return result.toString();
        } catch (Exception exception) {
            throw new ApiAlertException("Managed Flink lifecycle request cannot be hashed.");
        }
    }

    private static String trimToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
