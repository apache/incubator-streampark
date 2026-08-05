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

import org.apache.streampark.console.core.entity.FlinkApplication;
import org.apache.streampark.console.core.entity.ManagedFlinkApplication;
import org.apache.streampark.console.core.entity.ManagedFlinkEnvironment;
import org.apache.streampark.console.core.enums.FlinkAppStateEnum;
import org.apache.streampark.console.core.enums.OptionStateEnum;
import org.apache.streampark.console.core.managed.api.ManagedFlinkProviderException;
import org.apache.streampark.console.core.managed.api.ManagedJobLookupRequest;
import org.apache.streampark.console.core.managed.api.ManagedJobState;
import org.apache.streampark.console.core.managed.api.ManagedJobStatus;
import org.apache.streampark.console.core.managed.api.ProviderErrorCategory;
import org.apache.streampark.console.core.mapper.FlinkApplicationMapper;
import org.apache.streampark.console.core.mapper.ManagedFlinkApplicationMapper;
import org.apache.streampark.console.core.mapper.ManagedFlinkEnvironmentMapper;

import org.apache.commons.lang3.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Synchronizes managed Flink job state through short database leases.
 *
 * <p>Provider reads happen outside database transactions. State application rechecks the lease and
 * stable provider job id under the managed application row lock.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ManagedFlinkJobSyncService {

    private static final String SYNC_HEALTHY = "HEALTHY";
    private static final String SYNC_DEGRADED = "DEGRADED";
    private static final String SYNC_NOT_FOUND = "NOT_FOUND";

    private final ManagedFlinkApplicationMapper managedApplicationMapper;
    private final ManagedFlinkEnvironmentMapper environmentMapper;
    private final FlinkApplicationMapper applicationMapper;
    private final ManagedFlinkProviderContextService providerContextService;
    private final ManagedFlinkStateEventService stateEventService;
    private final ManagedFlinkWatcherProperties properties;
    private final TransactionTemplate transactionTemplate;

    private final String owner = "managed-watcher-" + UUID.randomUUID();

    /** Claims and synchronizes one bounded batch. */
    public int synchronizeDue() {
        stateEventService.dispatchPending();
        Date now = new Date();
        List<ManagedFlinkApplication> candidates =
            managedApplicationMapper.selectList(
                new LambdaQueryWrapper<ManagedFlinkApplication>()
                    .isNotNull(ManagedFlinkApplication::getExternalApplicationId)
                    .isNotNull(ManagedFlinkApplication::getNextSyncTime)
                    .le(ManagedFlinkApplication::getNextSyncTime, now)
                    .and(
                        wrapper -> wrapper
                            .isNull(ManagedFlinkApplication::getSyncLeaseUntil)
                            .or()
                            .lt(ManagedFlinkApplication::getSyncLeaseUntil, now))
                    .orderByAsc(ManagedFlinkApplication::getNextSyncTime)
                    .last("limit " + boundedBatchSize()));
        int synchronizedCount = 0;
        for (ManagedFlinkApplication candidate : candidates) {
            if (synchronizeIfDue(candidate.getAppId())) {
                synchronizedCount++;
            }
        }
        return synchronizedCount;
    }

    boolean synchronizeIfDue(Long appId) {
        Date now = new Date();
        Date leaseUntil = plusSeconds(now, positive(properties.getLeaseSeconds()));
        if (!claim(appId, now, leaseUntil)) {
            return false;
        }
        SyncTarget target = loadTarget(appId);
        if (target == null) {
            releaseLease(appId);
            return true;
        }
        try {
            ManagedFlinkProviderSession session =
                providerContextService.resolveForSystem(
                    target.environment.getCloudAccountId(),
                    target.environment.getProjectId());
            ManagedJobStatus status =
                session.getProvider().getJob(
                    session.getContext(),
                    ManagedJobLookupRequest.builder()
                        .projectId(target.environment.getProjectId())
                        .jobName(target.application.getJobName())
                        .jobId(target.managed.getExternalApplicationId())
                        .instanceId(target.managed.getExternalInstanceId())
                        .build());
            if (status == null) {
                recordNotFound(target.appId);
            } else if (!target.managed.getExternalApplicationId().equals(status.getJobId())) {
                recordFailure(target.appId, null, null);
            } else {
                recordSuccess(target.appId, status);
            }
        } catch (ManagedFlinkProviderException exception) {
            if (exception.getCategory() == ProviderErrorCategory.NOT_FOUND) {
                recordNotFound(target.appId);
            } else {
                recordFailure(
                    target.appId,
                    exception.getCategory(),
                    exception.getRetryAfterMillis());
            }
        } catch (Exception exception) {
            log.warn(
                "[StreamPark][ManagedFlinkJobWatcher] state lookup failed for app {}, type={}",
                appId,
                exception.getClass().getSimpleName());
            recordFailure(target.appId, null, null);
        }
        return true;
    }

    private boolean claim(Long appId, Date now, Date leaseUntil) {
        return managedApplicationMapper.update(
            null,
            new LambdaUpdateWrapper<ManagedFlinkApplication>()
                .eq(ManagedFlinkApplication::getAppId, appId)
                .isNotNull(ManagedFlinkApplication::getExternalApplicationId)
                .isNotNull(ManagedFlinkApplication::getNextSyncTime)
                .le(ManagedFlinkApplication::getNextSyncTime, now)
                .and(
                    wrapper -> wrapper
                        .isNull(ManagedFlinkApplication::getSyncLeaseUntil)
                        .or()
                        .lt(ManagedFlinkApplication::getSyncLeaseUntil, now))
                .set(ManagedFlinkApplication::getSyncOwner, owner)
                .set(ManagedFlinkApplication::getSyncLeaseUntil, leaseUntil)) == 1;
    }

    private SyncTarget loadTarget(Long appId) {
        ManagedFlinkApplication managed = managedApplicationMapper.selectById(appId);
        FlinkApplication application = applicationMapper.selectById(appId);
        ManagedFlinkEnvironment environment =
            managed == null ? null : environmentMapper.selectById(managed.getManagedEnvId());
        if (managed == null
            || application == null
            || environment == null
            || !owner.equals(managed.getSyncOwner())
            || StringUtils.isBlank(managed.getExternalApplicationId())
            || StringUtils.isBlank(environment.getProjectId())
            || environment.getCloudAccountId() == null) {
            return null;
        }
        return new SyncTarget(appId, managed, application, environment);
    }

    private void recordSuccess(Long appId, ManagedJobStatus status) {
        Long eventId =
            transactionTemplate.execute(
                ignored -> {
                    CurrentState current = lockCurrent(appId);
                    if (current == null) {
                        return null;
                    }
                    StateMapping mapping = map(current.application, current.managed, status);
                    Date now = new Date();
                    Date nextSync =
                        mapping.nextIntervalSeconds == null
                            ? null
                            : plusSeconds(now, jitter(mapping.nextIntervalSeconds));
                    int updated =
                        managedApplicationMapper.update(
                            null,
                            new LambdaUpdateWrapper<ManagedFlinkApplication>()
                                .eq(ManagedFlinkApplication::getAppId, appId)
                                .eq(ManagedFlinkApplication::getSyncOwner, owner)
                                .set(
                                    status.getInstanceId() != null,
                                    ManagedFlinkApplication::getExternalInstanceId,
                                    status.getInstanceId())
                                .set(
                                    ManagedFlinkApplication::getProviderRawState,
                                    safeProviderState(status))
                                .set(
                                    StringUtils.isNotBlank(status.getConsoleUrl()),
                                    ManagedFlinkApplication::getConsoleUrl,
                                    status.getConsoleUrl())
                                .set(ManagedFlinkApplication::getSyncState, SYNC_HEALTHY)
                                .set(ManagedFlinkApplication::getLastSyncTime, now)
                                .set(ManagedFlinkApplication::getConsecutiveSyncFailures, 0)
                                .set(ManagedFlinkApplication::getNextSyncTime, nextSync)
                                .set(ManagedFlinkApplication::getSyncOwner, null)
                                .set(ManagedFlinkApplication::getSyncLeaseUntil, null));
                    if (updated != 1) {
                        return null;
                    }
                    LambdaUpdateWrapper<FlinkApplication> applicationUpdate =
                        new LambdaUpdateWrapper<FlinkApplication>()
                            .eq(FlinkApplication::getId, appId)
                            .set(FlinkApplication::getState, mapping.state.getValue())
                            .set(
                                FlinkApplication::getOptionState,
                                mapping.optionState.getValue())
                            .set(FlinkApplication::getTracking, mapping.tracking)
                            .set(FlinkApplication::getModifyTime, now);
                    if (mapping.state == FlinkAppStateEnum.RUNNING
                        && current.application.getStartTime() == null) {
                        applicationUpdate.set(FlinkApplication::getStartTime, now);
                    }
                    if (isTerminal(mapping.state)) {
                        applicationUpdate.set(FlinkApplication::getEndTime, now);
                    }
                    applicationMapper.update(null, applicationUpdate);
                    return stateEventService.create(
                        appId,
                        FlinkAppStateEnum.getState(current.application.getState()).name(),
                        mapping.state.name(),
                        status.getInstanceId() == null
                            ? current.managed.getExternalInstanceId()
                            : status.getInstanceId(),
                        now);
                });
        stateEventService.dispatch(eventId);
    }

    private void recordNotFound(Long appId) {
        transactionTemplate.executeWithoutResult(
            ignored -> {
                CurrentState current = lockCurrent(appId);
                if (current == null) {
                    return;
                }
                Date now = new Date();
                managedApplicationMapper.update(
                    null,
                    new LambdaUpdateWrapper<ManagedFlinkApplication>()
                        .eq(ManagedFlinkApplication::getAppId, appId)
                        .eq(ManagedFlinkApplication::getSyncOwner, owner)
                        .set(ManagedFlinkApplication::getProviderRawState, "NOT_FOUND")
                        .set(ManagedFlinkApplication::getSyncState, SYNC_NOT_FOUND)
                        .set(ManagedFlinkApplication::getLastSyncTime, now)
                        .set(ManagedFlinkApplication::getConsecutiveSyncFailures, 0)
                        .set(
                            ManagedFlinkApplication::getNextSyncTime,
                            plusSeconds(
                                now,
                                jitter(
                                    positive(
                                        properties
                                            .getObservationIntervalSeconds()))))
                        .set(ManagedFlinkApplication::getSyncOwner, null)
                        .set(ManagedFlinkApplication::getSyncLeaseUntil, null));
            });
    }

    private void recordFailure(
                               Long appId,
                               ProviderErrorCategory category,
                               Long retryAfterMillis) {
        Long eventId =
            transactionTemplate.execute(
                ignored -> {
                    CurrentState current = lockCurrent(appId);
                    if (current == null) {
                        return null;
                    }
                    int failures =
                        (current.managed.getConsecutiveSyncFailures() == null
                            ? 0
                            : current.managed.getConsecutiveSyncFailures())
                            + 1;
                    boolean lost = failures >= Math.max(1, properties.getLostFailureThreshold());
                    Date now = new Date();
                    long delay = failureDelaySeconds(failures, retryAfterMillis);
                    int updated =
                        managedApplicationMapper.update(
                            null,
                            new LambdaUpdateWrapper<ManagedFlinkApplication>()
                                .eq(ManagedFlinkApplication::getAppId, appId)
                                .eq(ManagedFlinkApplication::getSyncOwner, owner)
                                .set(ManagedFlinkApplication::getSyncState, SYNC_DEGRADED)
                                .set(
                                    category != null,
                                    ManagedFlinkApplication::getProviderRawState,
                                    "LOOKUP_" + category.name())
                                .set(
                                    ManagedFlinkApplication::getConsecutiveSyncFailures,
                                    failures)
                                .set(
                                    ManagedFlinkApplication::getNextSyncTime,
                                    plusSeconds(now, jitter(delay)))
                                .set(ManagedFlinkApplication::getSyncOwner, null)
                                .set(ManagedFlinkApplication::getSyncLeaseUntil, null));
                    if (updated == 1 && lost) {
                        applicationMapper.update(
                            null,
                            new LambdaUpdateWrapper<FlinkApplication>()
                                .eq(FlinkApplication::getId, appId)
                                .set(
                                    FlinkApplication::getState,
                                    FlinkAppStateEnum.LOST.getValue())
                                .set(
                                    FlinkApplication::getOptionState,
                                    OptionStateEnum.NONE.getValue())
                                .set(FlinkApplication::getTracking, 1)
                                .set(FlinkApplication::getModifyTime, now));
                        return stateEventService.create(
                            appId,
                            FlinkAppStateEnum.getState(current.application.getState()).name(),
                            FlinkAppStateEnum.LOST.name(),
                            current.managed.getExternalInstanceId(),
                            now);
                    }
                    return null;
                });
        stateEventService.dispatch(eventId);
    }

    private CurrentState lockCurrent(Long appId) {
        if (managedApplicationMapper.lockByAppId(appId) == null) {
            return null;
        }
        ManagedFlinkApplication managed = managedApplicationMapper.selectById(appId);
        FlinkApplication application = applicationMapper.selectById(appId);
        if (managed == null
            || application == null
            || !owner.equals(managed.getSyncOwner())) {
            return null;
        }
        return new CurrentState(managed, application);
    }

    private void releaseLease(Long appId) {
        managedApplicationMapper.update(
            null,
            new LambdaUpdateWrapper<ManagedFlinkApplication>()
                .eq(ManagedFlinkApplication::getAppId, appId)
                .eq(ManagedFlinkApplication::getSyncOwner, owner)
                .set(ManagedFlinkApplication::getSyncOwner, null)
                .set(ManagedFlinkApplication::getSyncLeaseUntil, null));
    }

    private StateMapping map(
                             FlinkApplication application,
                             ManagedFlinkApplication managed,
                             ManagedJobStatus status) {
        FlinkAppStateEnum local = FlinkAppStateEnum.getState(application.getState());
        ManagedJobState provider = status.getState();
        if (provider == null) {
            provider = ManagedJobState.OTHER;
        }
        if (local == FlinkAppStateEnum.CANCELLING
            && provider == ManagedJobState.RUNNING) {
            return transition(
                FlinkAppStateEnum.CANCELLING, OptionStateEnum.CANCELLING);
        }
        if (local == FlinkAppStateEnum.STARTING
            && provider == ManagedJobState.CREATED) {
            return transition(FlinkAppStateEnum.STARTING, OptionStateEnum.STARTING);
        }
        if (local == FlinkAppStateEnum.RESTARTING
            && (provider == ManagedJobState.STARTING
                || provider == ManagedJobState.RUNNING
                    && Objects.equals(
                        managed.getExternalInstanceId(), status.getInstanceId()))) {
            return transition(FlinkAppStateEnum.RESTARTING, OptionStateEnum.NONE);
        }
        switch (provider) {
            case CREATED:
                return terminal(FlinkAppStateEnum.CANCELED);
            case STARTING:
                return transition(FlinkAppStateEnum.STARTING, OptionStateEnum.STARTING);
            case RUNNING:
                return running();
            case RESTARTING:
                return runningTransition(FlinkAppStateEnum.RESTARTING);
            case STOPPING:
                return transition(
                    FlinkAppStateEnum.CANCELLING, OptionStateEnum.CANCELLING);
            case STOPPED:
                return terminal(FlinkAppStateEnum.CANCELED);
            case SAVEPOINTING:
                return transition(local, OptionStateEnum.SAVEPOINTING);
            case SUCCEEDED:
                return terminal(FlinkAppStateEnum.FINISHED);
            case FAILED:
                return observe(FlinkAppStateEnum.FAILED, OptionStateEnum.NONE);
            case SUSPENDED:
                return observe(FlinkAppStateEnum.SUSPENDED, OptionStateEnum.NONE);
            case OTHER:
            default:
                return observe(
                    FlinkAppStateEnum.OTHER,
                    optionState(application.getOptionState()));
        }
    }

    private StateMapping transition(FlinkAppStateEnum state, OptionStateEnum optionState) {
        return new StateMapping(
            state,
            optionState,
            1,
            positive(properties.getTransitionIntervalSeconds()));
    }

    private StateMapping runningTransition(FlinkAppStateEnum state) {
        return new StateMapping(
            state,
            OptionStateEnum.NONE,
            1,
            positive(properties.getRunningIntervalSeconds()));
    }

    private StateMapping running() {
        return new StateMapping(
            FlinkAppStateEnum.RUNNING,
            OptionStateEnum.NONE,
            1,
            positive(properties.getRunningIntervalSeconds()));
    }

    private StateMapping observe(FlinkAppStateEnum state, OptionStateEnum optionState) {
        return new StateMapping(
            state,
            optionState,
            1,
            positive(properties.getObservationIntervalSeconds()));
    }

    private static StateMapping terminal(FlinkAppStateEnum state) {
        return new StateMapping(state, OptionStateEnum.NONE, 0, null);
    }

    private long failureDelaySeconds(int failures, Long retryAfterMillis) {
        long base = positive(properties.getTransitionIntervalSeconds());
        int shift = Math.min(Math.max(0, failures - 1), 10);
        long exponential = Math.min(positive(properties.getMaxBackoffSeconds()), base << shift);
        if (retryAfterMillis == null || retryAfterMillis <= 0) {
            return exponential;
        }
        long retryAfterSeconds =
            Math.max(1, TimeUnit.MILLISECONDS.toSeconds(retryAfterMillis));
        return Math.min(
            positive(properties.getMaxBackoffSeconds()),
            Math.max(exponential, retryAfterSeconds));
    }

    private long jitter(long seconds) {
        long bound = seconds * Math.max(0, properties.getJitterPercent()) / 100;
        return bound == 0
            ? seconds
            : seconds + ThreadLocalRandom.current().nextLong(bound + 1);
    }

    private int boundedBatchSize() {
        return Math.min(Math.max(1, properties.getBatchSize()), 1000);
    }

    private static long positive(long value) {
        return Math.max(1, value);
    }

    private static Date plusSeconds(Date value, long seconds) {
        return new Date(value.getTime() + TimeUnit.SECONDS.toMillis(seconds));
    }

    private static String safeProviderState(ManagedJobStatus status) {
        return StringUtils.abbreviate(
            StringUtils.defaultIfBlank(
                status.getProviderState(),
                status.getState() == null ? "OTHER" : status.getState().name()),
            64);
    }

    private static OptionStateEnum optionState(Integer value) {
        OptionStateEnum state = OptionStateEnum.getState(value);
        return state == null ? OptionStateEnum.NONE : state;
    }

    private static boolean isTerminal(FlinkAppStateEnum state) {
        return state == FlinkAppStateEnum.CANCELED
            || state == FlinkAppStateEnum.FINISHED;
    }

    @RequiredArgsConstructor
    private static class SyncTarget {

        private final Long appId;
        private final ManagedFlinkApplication managed;
        private final FlinkApplication application;
        private final ManagedFlinkEnvironment environment;
    }

    @RequiredArgsConstructor
    private static class CurrentState {

        private final ManagedFlinkApplication managed;
        private final FlinkApplication application;
    }

    @RequiredArgsConstructor
    private static class StateMapping {

        private final FlinkAppStateEnum state;
        private final OptionStateEnum optionState;
        private final int tracking;
        private final Long nextIntervalSeconds;
    }
}
