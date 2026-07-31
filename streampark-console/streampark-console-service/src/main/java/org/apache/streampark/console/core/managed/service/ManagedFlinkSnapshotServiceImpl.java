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
import org.apache.streampark.console.core.entity.ManagedFlinkEnvironment;
import org.apache.streampark.console.core.entity.ManagedFlinkOperation;
import org.apache.streampark.console.core.entity.ManagedFlinkSnapshot;
import org.apache.streampark.console.core.enums.FlinkAppStateEnum;
import org.apache.streampark.console.core.managed.api.ManagedSnapshot;
import org.apache.streampark.console.core.managed.api.ManagedSnapshotLookupRequest;
import org.apache.streampark.console.core.managed.api.ManagedSnapshotState;
import org.apache.streampark.console.core.managed.model.ManagedFlinkOperationView;
import org.apache.streampark.console.core.managed.model.ManagedFlinkReleaseSnapshot;
import org.apache.streampark.console.core.managed.model.ManagedFlinkSnapshotCreateRequest;
import org.apache.streampark.console.core.managed.model.ManagedFlinkSnapshotOperationSnapshot;
import org.apache.streampark.console.core.managed.model.ManagedFlinkSnapshotView;
import org.apache.streampark.console.core.mapper.FlinkApplicationMapper;
import org.apache.streampark.console.core.mapper.ManagedFlinkApplicationMapper;
import org.apache.streampark.console.core.mapper.ManagedFlinkEnvironmentMapper;
import org.apache.streampark.console.core.mapper.ManagedFlinkSnapshotMapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Provider-backed snapshot cache with app-scoped upsert and expiry reconciliation. */
@Service
@RequiredArgsConstructor
public class ManagedFlinkSnapshotServiceImpl implements ManagedFlinkSnapshotService {

    private final ManagedFlinkApplicationMapper managedApplicationMapper;
    private final FlinkApplicationMapper applicationMapper;
    private final ManagedFlinkEnvironmentMapper environmentMapper;
    private final ManagedFlinkSnapshotMapper snapshotMapper;
    private final ManagedFlinkDeployedDefinitionService deployedDefinitionService;
    private final ManagedFlinkProviderContextService providerContextService;
    private final ManagedFlinkOperationService operationService;
    private final ManagedFlinkSnapshotDispatcher dispatcher;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public ManagedFlinkOperationView create(ManagedFlinkSnapshotCreateRequest request) {
        ManagedFlinkOperationView replay = replay(request);
        if (replay != null) {
            return replay;
        }
        FlinkApplication application = applicationMapper.selectById(request.getAppId());
        ApiAlertException.throwIfTrue(
            application == null || !request.getTeamId().equals(application.getTeamId()),
            "Managed Flink application does not exist.");
        ApiAlertException.throwIfFalse(
            FlinkAppStateEnum.getState(application.getState()) == FlinkAppStateEnum.RUNNING,
            "Managed Flink job is not running.");
        ManagedFlinkReleaseSnapshot deployed =
            deployedDefinitionService.getRequired(request.getTeamId(), request.getAppId());
        ManagedFlinkApplication managed =
            managedApplicationMapper.selectById(request.getAppId());
        ApiAlertException.throwIfTrue(
            managed == null
                || isBlank(managed.getExternalApplicationId())
                || isBlank(managed.getExternalInstanceId()),
            "Managed Flink running job identity is unavailable.");
        ManagedFlinkEnvironment environment =
            environmentMapper.selectById(managed.getManagedEnvId());
        ApiAlertException.throwIfTrue(
            environment == null
                || environment.getCloudAccountId() == null
                || isBlank(environment.getProjectId()),
            "Managed Flink environment is unavailable.");
        ManagedFlinkProviderSession session =
            providerContextService.resolve(
                request.getTeamId(),
                environment.getCloudAccountId(),
                environment.getProjectId());
        ApiAlertException.throwIfFalse(
            session.getProviderType().name().equals(deployed.getProviderType()),
            "Managed Flink provider routing has changed.");
        ApiAlertException.throwIfFalse(
            session.getProvider().getCapability(session.getContext()).isSupportsCreateSnapshot(),
            "Managed Flink provider does not support snapshot creation.");
        List<ManagedSnapshot> baseline =
            listProviderSnapshots(session, environment, managed);
        ManagedFlinkSnapshotOperationSnapshot snapshot =
            operationSnapshot(request, deployed, environment, managed, baseline);
        String requestJson = write(snapshot);
        ManagedFlinkOperationView operation =
            operationService.accept(
                request.getAppId(),
                "SNAPSHOT",
                request.getIdempotencyKey(),
                hash(requestJson),
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
                    "Managed Flink snapshot executor is temporarily unavailable.");
            }
            return operationService.getView(
                request.getAppId(), operation.getOperationId());
        }
        return operation;
    }

    @Override
    public List<ManagedFlinkSnapshotView> refreshAndList(Long teamId, Long appId) {
        ManagedFlinkReleaseSnapshot deployed =
            deployedDefinitionService.getRequired(teamId, appId);
        ManagedFlinkApplication managed = managedApplicationMapper.selectById(appId);
        ApiAlertException.throwIfTrue(
            managed == null || isBlank(managed.getExternalApplicationId()),
            "Managed Flink deployed job ID is unavailable.");
        ManagedFlinkEnvironment environment =
            environmentMapper.selectById(managed.getManagedEnvId());
        ApiAlertException.throwIfTrue(
            environment == null
                || environment.getCloudAccountId() == null
                || isBlank(environment.getProjectId()),
            "Managed Flink environment is unavailable.");
        ManagedFlinkProviderSession session =
            providerContextService.resolve(
                teamId, environment.getCloudAccountId(), environment.getProjectId());
        ApiAlertException.throwIfFalse(
            session.getProviderType().name().equals(deployed.getProviderType()),
            "Managed Flink provider routing has changed.");
        List<ManagedSnapshot> providerSnapshots =
            listProviderSnapshots(session, environment, managed);
        transactionTemplate.executeWithoutResult(
            ignored -> reconcileProviderSnapshots(appId, providerSnapshots));
        return views(appId);
    }

    @Override
    public ManagedFlinkSnapshotView getRestorable(
                                                  Long teamId,
                                                  Long appId,
                                                  String snapshotId) {
        String requiredSnapshotId = trimToNull(snapshotId);
        ApiAlertException.throwIfTrue(
            requiredSnapshotId == null,
            "Managed Flink snapshot ID is required.");
        return refreshAndList(teamId, appId).stream()
            .filter(value -> requiredSnapshotId.equals(value.getSnapshotId()))
            .filter(value -> ManagedSnapshotState.COMPLETED.name().equals(value.getState()))
            .findFirst()
            .orElseThrow(
                () -> new ApiAlertException(
                    "Managed Flink snapshot is not available for restore."));
    }

    void reconcileProviderSnapshots(
                                    Long appId, List<ManagedSnapshot> providerSnapshots) {
        Date now = new Date();
        Set<String> observed = new HashSet<>();
        String latest = latestCompleted(providerSnapshots);
        snapshotMapper.update(
            null,
            new LambdaUpdateWrapper<ManagedFlinkSnapshot>()
                .eq(ManagedFlinkSnapshot::getAppId, appId)
                .set(ManagedFlinkSnapshot::getIsLatest, 0)
                .set(ManagedFlinkSnapshot::getModifyTime, now));
        if (providerSnapshots != null) {
            for (ManagedSnapshot providerSnapshot : providerSnapshots) {
                if (providerSnapshot == null || isBlank(providerSnapshot.getSnapshotId())) {
                    continue;
                }
                observed.add(providerSnapshot.getSnapshotId());
                ManagedFlinkSnapshot stored =
                    snapshotMapper.selectOne(
                        new LambdaQueryWrapper<ManagedFlinkSnapshot>()
                            .eq(ManagedFlinkSnapshot::getAppId, appId)
                            .eq(
                                ManagedFlinkSnapshot::getExternalSnapshotId,
                                providerSnapshot.getSnapshotId()));
                ManagedFlinkSnapshot value =
                    stored == null ? new ManagedFlinkSnapshot() : stored;
                value.setAppId(appId);
                value.setExternalSnapshotId(providerSnapshot.getSnapshotId());
                value.setExternalInstanceId(providerSnapshot.getInstanceId());
                value.setSnapshotType(
                    defaultValue(providerSnapshot.getSnapshotType(), "MANUAL"));
                value.setState(
                    (providerSnapshot.getState() == null
                        ? ManagedSnapshotState.OTHER
                        : providerSnapshot.getState())
                            .name());
                value.setProviderState(providerSnapshot.getProviderState());
                value.setLocation(providerSnapshot.getLocation());
                value.setDescription(displayDescription(providerSnapshot.getDescription()));
                value.setIsLatest(
                    providerSnapshot.getSnapshotId().equals(latest) ? 1 : 0);
                value.setTriggerTime(parseTime(providerSnapshot.getTriggerTime()));
                value.setCompletionTime(
                    parseTime(providerSnapshot.getCompletionTime()));
                value.setModifyTime(now);
                if (stored == null) {
                    value.setCreateTime(now);
                    snapshotMapper.insert(value);
                } else {
                    snapshotMapper.updateById(value);
                }
            }
        }
        LambdaUpdateWrapper<ManagedFlinkSnapshot> expired =
            new LambdaUpdateWrapper<ManagedFlinkSnapshot>()
                .eq(ManagedFlinkSnapshot::getAppId, appId)
                .ne(ManagedFlinkSnapshot::getState, ManagedSnapshotState.EXPIRED.name())
                .set(ManagedFlinkSnapshot::getState, ManagedSnapshotState.EXPIRED.name())
                .set(ManagedFlinkSnapshot::getIsLatest, 0)
                .set(ManagedFlinkSnapshot::getModifyTime, now);
        if (!observed.isEmpty()) {
            expired.notIn(ManagedFlinkSnapshot::getExternalSnapshotId, observed);
        }
        snapshotMapper.update(null, expired);
    }

    void cacheProviderSnapshots(Long appId, List<ManagedSnapshot> providerSnapshots) {
        transactionTemplate.executeWithoutResult(
            ignored -> reconcileProviderSnapshots(appId, providerSnapshots));
    }

    List<ManagedFlinkSnapshotView> views(Long appId) {
        List<ManagedFlinkSnapshot> values =
            snapshotMapper.selectList(
                new LambdaQueryWrapper<ManagedFlinkSnapshot>()
                    .eq(ManagedFlinkSnapshot::getAppId, appId)
                    .orderByDesc(ManagedFlinkSnapshot::getTriggerTime)
                    .orderByDesc(ManagedFlinkSnapshot::getId));
        List<ManagedFlinkSnapshotView> result = new ArrayList<>();
        for (ManagedFlinkSnapshot value : values) {
            result.add(
                ManagedFlinkSnapshotView.builder()
                    .id(value.getId())
                    .appId(value.getAppId())
                    .snapshotId(value.getExternalSnapshotId())
                    .instanceId(value.getExternalInstanceId())
                    .snapshotType(value.getSnapshotType())
                    .state(value.getState())
                    .providerState(value.getProviderState())
                    .location(value.getLocation())
                    .description(value.getDescription())
                    .latest(Integer.valueOf(1).equals(value.getIsLatest()))
                    .triggerTime(value.getTriggerTime())
                    .completionTime(value.getCompletionTime())
                    .build());
        }
        return result;
    }

    private ManagedFlinkOperationView replay(
                                             ManagedFlinkSnapshotCreateRequest request) {
        ManagedFlinkOperation existing =
            operationService.findByIdempotency(
                request.getAppId(), "SNAPSHOT", request.getIdempotencyKey());
        if (existing == null) {
            return null;
        }
        FlinkApplication application = applicationMapper.selectById(request.getAppId());
        ApiAlertException.throwIfTrue(
            application == null || !request.getTeamId().equals(application.getTeamId()),
            "Managed Flink application does not exist.");
        ManagedFlinkSnapshotOperationSnapshot snapshot;
        try {
            snapshot =
                objectMapper.readValue(
                    existing.getRequestJson(),
                    ManagedFlinkSnapshotOperationSnapshot.class);
        } catch (Exception exception) {
            throw new ApiAlertException("Managed Flink snapshot operation is invalid.");
        }
        ApiAlertException.throwIfFalse(
            request.getTeamId().equals(snapshot.getTeamId())
                && request.getAppId().equals(snapshot.getAppId())
                && Objects.equals(
                    trimToNull(request.getDescription()),
                    trimToNull(snapshot.getUserDescription())),
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

    private ManagedFlinkSnapshotOperationSnapshot operationSnapshot(
                                                                    ManagedFlinkSnapshotCreateRequest request,
                                                                    ManagedFlinkReleaseSnapshot deployed,
                                                                    ManagedFlinkEnvironment environment,
                                                                    ManagedFlinkApplication managed,
                                                                    List<ManagedSnapshot> baseline) {
        List<String> baselineIds = new ArrayList<>();
        for (ManagedSnapshot value : baseline) {
            if (value != null && !isBlank(value.getSnapshotId())) {
                baselineIds.add(value.getSnapshotId());
            }
        }
        Collections.sort(baselineIds);
        String marker =
            "streampark:"
                + request.getAppId()
                + ":"
                + hash(request.getIdempotencyKey()).substring(0, 16);
        String userDescription = trimToNull(request.getDescription());
        ManagedFlinkSnapshotOperationSnapshot snapshot =
            new ManagedFlinkSnapshotOperationSnapshot();
        snapshot.setTeamId(request.getTeamId());
        snapshot.setAppId(request.getAppId());
        snapshot.setCloudAccountId(environment.getCloudAccountId());
        snapshot.setProviderType(deployed.getProviderType());
        snapshot.setProjectId(environment.getProjectId());
        snapshot.setJobId(managed.getExternalApplicationId());
        snapshot.setInstanceId(managed.getExternalInstanceId());
        snapshot.setUserDescription(userDescription);
        snapshot.setProviderDescription(
            userDescription == null ? marker : marker + " | " + userDescription);
        snapshot.setRequestedAt(Instant.now().toString());
        snapshot.setBaselineSnapshotIds(baselineIds);
        return snapshot;
    }

    private static List<ManagedSnapshot> listProviderSnapshots(
                                                               ManagedFlinkProviderSession session,
                                                               ManagedFlinkEnvironment environment,
                                                               ManagedFlinkApplication managed) {
        List<ManagedSnapshot> snapshots =
            session
                .getProvider()
                .listSnapshots(
                    session.getContext(),
                    ManagedSnapshotLookupRequest.builder()
                        .projectId(environment.getProjectId())
                        .jobId(managed.getExternalApplicationId())
                        .build());
        return snapshots == null ? Collections.emptyList() : snapshots;
    }

    private static String latestCompleted(List<ManagedSnapshot> snapshots) {
        if (snapshots == null) {
            return null;
        }
        return snapshots.stream()
            .filter(
                value -> value != null
                    && value.getState() == ManagedSnapshotState.COMPLETED
                    && !isBlank(value.getSnapshotId()))
            .max(
                Comparator.comparing(
                    value -> defaultValue(
                        value.getCompletionTime(),
                        defaultValue(value.getTriggerTime(), ""))))
            .map(ManagedSnapshot::getSnapshotId)
            .orElse(null);
    }

    private static Date parseTime(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return Date.from(Instant.parse(value));
        } catch (DateTimeParseException ignored) {
            try {
                return Date.from(OffsetDateTime.parse(value).toInstant());
            } catch (DateTimeParseException ignoredAgain) {
                return null;
            }
        }
    }

    private static String defaultValue(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new ApiAlertException("Managed Flink snapshot request cannot be serialized.");
        }
    }

    private static String hash(String value) {
        try {
            byte[] digest =
                MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                result.append(String.format("%02x", item & 0xff));
            }
            return result.toString();
        } catch (Exception exception) {
            throw new ApiAlertException("Managed Flink snapshot request cannot be hashed.");
        }
    }

    private static String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private static String displayDescription(String value) {
        if (value == null || !value.startsWith("streampark:")) {
            return value;
        }
        int separator = value.indexOf(" | ");
        return separator < 0 ? null : value.substring(separator + 3);
    }
}
