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

package org.apache.streampark.flink.kubernetes.watcher;

import org.apache.streampark.flink.kubernetes.ChangeEventBus;
import org.apache.streampark.flink.kubernetes.FlinkK8sWatchController;
import org.apache.streampark.flink.kubernetes.JobStatusWatcherConfig;
import org.apache.streampark.flink.kubernetes.KubernetesRetriever;
import org.apache.streampark.flink.kubernetes.enums.FlinkJobState;
import org.apache.streampark.flink.kubernetes.enums.FlinkK8sDeployMode;
import org.apache.streampark.flink.kubernetes.event.FlinkJobStatusChangeEvent;
import org.apache.streampark.flink.kubernetes.helper.KubernetesDeploymentHelper;
import org.apache.streampark.flink.kubernetes.model.ClusterKey;
import org.apache.streampark.flink.kubernetes.model.JobStatusCV;
import org.apache.streampark.flink.kubernetes.model.TrackId;
import org.apache.streampark.flink.kubernetes.rest.FlinkHistoryArchives;
import org.apache.streampark.flink.kubernetes.rest.JobDetail;
import org.apache.streampark.flink.kubernetes.rest.JobsOverview;

import org.apache.hc.client5.http.fluent.Request;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Watcher for continuously monitor flink job status on kubernetes-mode, the traced flink
 * identifiers from FlinkTrackCachePool.trackIds, the traced result of flink jobs status would
 * written to FlinkTrackCachePool.jobStatuses.
 */
@ThreadSafe
public class FlinkJobStatusWatcher extends FlinkWatcher {

    private static final String TRACK_IDS_SUFFIX = ", trackIds=";

    private final JobStatusWatcherConfig conf;
    private final FlinkK8sWatchController watchController;
    private final ChangeEventBus eventBus;

    private ScheduledFuture<?> timerSchedule;

    public FlinkJobStatusWatcher(
                                 JobStatusWatcherConfig conf,
                                 FlinkK8sWatchController watchController,
                                 ChangeEventBus eventBus) {
        this.conf = conf;
        this.watchController = watchController;
        this.eventBus = eventBus;
    }

    @Override
    protected void doStart() {
        timerSchedule =
            watchExecutor.scheduleAtFixedRate(
                this::doWatch, 0, conf.requestIntervalSec(), TimeUnit.SECONDS);
        logInfo("[flink-k8s] FlinkJobStatusWatcher started.");
    }

    @Override
    protected void doStop() {
        if (timerSchedule != null && !timerSchedule.isCancelled()) {
            timerSchedule.cancel(true);
        }
        logInfo("[flink-k8s] FlinkJobStatusWatcher stopped.");
    }

    @Override
    protected void doClose() {
        logInfo("[flink-k8s] FlinkJobStatusWatcher closed.");
    }

    @Override
    public void doWatch() {
        synchronized (this) {
            Set<TrackId> trackIds;
            try {
                trackIds = watchController.getAllWatchingIds();
                if (trackIds.isEmpty()) {
                    return;
                }
            } catch (Exception e) {
                return;
            }

            Set<CompletableFuture<Optional<JobStatusCV>>> appFuture =
                trackIds.stream()
                    .filter(id -> id.executeMode() == FlinkK8sDeployMode.APPLICATION)
                    .map(
                        id -> CompletableFuture
                            .supplyAsync(() -> touchApplicationJob(id), watchExecutor)
                            .whenComplete(
                                (jobStateOpt, error) -> {
                                    if (error == null) {
                                        jobStateOpt.ifPresent(
                                            jobState -> updateState(
                                                id.toBuilder().jobId(jobState.jobId()).build(),
                                                jobState));
                                    }
                                }))
                    .collect(Collectors.toSet());

            Set<TrackId> sessionIds =
                trackIds.stream()
                    .filter(id -> id.executeMode() == FlinkK8sDeployMode.SESSION)
                    .collect(Collectors.toSet());
            Set<TrackId> sessionCluster = new HashSet<>();
            for (TrackId sessionId : sessionIds) {
                sessionCluster.add(sessionId);
            }

            Set<CompletableFuture<Map<TrackId, JobStatusCV>>> sessionFuture =
                sessionCluster.stream()
                    .map(
                        trackId -> CompletableFuture
                            .supplyAsync(() -> touchSessionAllJob(trackId), watchExecutor)
                            .whenComplete(
                                (map, error) -> {
                                    if (map != null) {
                                        Optional<Map.Entry<TrackId, JobStatusCV>> matched =
                                            map.entrySet().stream()
                                                .filter(
                                                    entry -> trackId.jobId() != null
                                                        && trackId
                                                            .jobId()
                                                            .equals(entry.getKey().jobId()))
                                                .findFirst();
                                        if (matched.isPresent()) {
                                            Map.Entry<TrackId, JobStatusCV> job =
                                                matched.get();
                                            updateState(
                                                job.getKey()
                                                    .toBuilder()
                                                    .appId(trackId.appId())
                                                    .build(),
                                                job.getValue());
                                        } else {
                                            touchSessionJob(trackId)
                                                .ifPresent(
                                                    state -> updateState(trackId, state));
                                        }
                                    }
                                }))
                    .collect(Collectors.toSet());

            awaitAll(appFuture, trackIds, "application");
            awaitAllSession(sessionFuture, trackIds);
        }
    }

    private void awaitAll(
                          Set<CompletableFuture<Optional<JobStatusCV>>> futures,
                          Set<TrackId> trackIds,
                          String mode) {
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .get(conf.requestTimeoutSec(), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logWarn(
                "[FlinkJobStatusWatcher] tracking flink job status on kubernetes native "
                    + mode
                    + " mode interrupted, limitSeconds="
                    + conf.requestTimeoutSec()
                    + TRACK_IDS_SUFFIX
                    + formatTrackIds(trackIds));
        } catch (ExecutionException | TimeoutException e) {
            logWarn(
                "[FlinkJobStatusWatcher] tracking flink job status on kubernetes native "
                    + mode
                    + " mode timeout, limitSeconds="
                    + conf.requestTimeoutSec()
                    + TRACK_IDS_SUFFIX
                    + formatTrackIds(trackIds));
        }
    }

    private void awaitAllSession(
                                 Set<CompletableFuture<Map<TrackId, JobStatusCV>>> futures,
                                 Set<TrackId> trackIds) {
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .get(conf.requestTimeoutSec(), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logWarn(
                "[FlinkJobStatusWatcher] tracking flink job status on kubernetes native session"
                    + " mode interrupted, limitSeconds="
                    + conf.requestTimeoutSec()
                    + TRACK_IDS_SUFFIX
                    + formatTrackIds(trackIds));
        } catch (ExecutionException | TimeoutException e) {
            logWarn(
                "[FlinkJobStatusWatcher] tracking flink job status on kubernetes native session"
                    + " mode timeout, limitSeconds="
                    + conf.requestTimeoutSec()
                    + TRACK_IDS_SUFFIX
                    + formatTrackIds(trackIds));
        }
    }

    private static String formatTrackIds(Set<TrackId> trackIds) {
        return trackIds.stream().map(Object::toString).collect(Collectors.joining(","));
    }

    public Optional<JobStatusCV> touchSessionJob(@Nonnull TrackId trackId) {
        Optional<JobStatusCV> fromAll =
            touchSessionAllJob(trackId).entrySet().stream()
                .filter(
                    entry -> trackId.jobId() != null
                        && trackId.jobId().equals(entry.getKey().jobId())
                        && entry.getValue().jobState() != FlinkJobState.SILENT)
                .map(Map.Entry::getValue)
                .findFirst();
        return fromAll.isPresent() ? fromAll : inferState(trackId);
    }

    private Map<TrackId, JobStatusCV> touchSessionAllJob(TrackId trackId) {
        long pollEmitTime = System.currentTimeMillis();
        Optional<JobsOverview> jobDetails = listJobsDetails(ClusterKey.of(trackId));
        if (jobDetails.isPresent() && !jobDetails.get().jobs().isEmpty()) {
            Map<TrackId, JobStatusCV> result = new HashMap<>();
            for (JobDetail detail : jobDetails.get().jobs()) {
                JobStatusCV jobStatus =
                    detail.toJobStatusCV(pollEmitTime, System.currentTimeMillis());
                TrackId trackItem =
                    trackId.toBuilder().jobId(detail.jid()).appId(null).build();
                result.put(trackItem, jobStatus);
            }
            return result;
        }
        return Collections.emptyMap();
    }

    public Optional<JobStatusCV> touchApplicationJob(@Nonnull TrackId trackId) {
        long pollEmitTime = System.currentTimeMillis();
        Optional<JobsOverview> jobDetails = listJobsDetails(ClusterKey.of(trackId));
        if (!jobDetails.isPresent() || jobDetails.get().jobs().isEmpty()) {
            return inferStateFromK8sEvent(trackId, pollEmitTime);
        }
        JobDetail first = jobDetails.get().jobs().get(0);
        return Optional.of(first.toJobStatusCV(pollEmitTime, System.currentTimeMillis()));
    }

    private void updateState(TrackId trackId, JobStatusCV jobState) {
        JobStatusCV latest = watchController.jobStatuses.get(trackId);
        if (jobState.diff(latest)) {
            watchController.jobStatuses.put(trackId, jobState);
            watchController.trackIds.update(trackId);
            eventBus.postSync(new FlinkJobStatusChangeEvent(trackId, jobState));
        }

        if (FlinkJobState.isEndState(jobState.jobState())) {
            switch (trackId.executeMode()) {
                case APPLICATION:
                    boolean deployExists =
                        KubernetesRetriever.isDeploymentExists(
                            trackId.namespace(), trackId.clusterId());
                    if (!deployExists) {
                        watchController.endpoints.invalidate(trackId.toClusterKey());
                        watchController.unWatching(trackId);
                    }
                    break;
                case SESSION:
                    watchController.unWatching(trackId);
                    break;
                default:
                    break;
            }
        }
    }

    private Optional<JobStatusCV> inferState(TrackId id) {
        long pollEmitTime = System.currentTimeMillis();
        JobStatusCV preCache = watchController.jobStatuses.get(id);
        FlinkJobState state = inferFromPreCache(preCache);
        boolean nonFirstSilent =
            state == FlinkJobState.SILENT
                && preCache != null
                && preCache.jobState() == FlinkJobState.SILENT;
        JobStatusCV jobState;
        if (nonFirstSilent) {
            jobState =
                JobStatusCV.builder()
                    .jobState(state)
                    .jobId(id.jobId())
                    .pollEmitTime(preCache.pollEmitTime())
                    .pollAckTime(preCache.pollAckTime())
                    .build();
        } else {
            jobState =
                JobStatusCV.builder()
                    .jobState(state)
                    .jobId(id.jobId())
                    .pollEmitTime(pollEmitTime)
                    .pollAckTime(System.currentTimeMillis())
                    .build();
        }
        return Optional.of(jobState);
    }

    private Optional<JobsOverview> listJobsDetails(ClusterKey clusterKey) {
        try {
            Optional<String> clusterRestUrl =
                watchController.getClusterRestUrl(clusterKey).filter(url -> !url.isEmpty());
            if (!clusterRestUrl.isPresent()) {
                return Optional.empty();
            }
            return callJobsOverviewsApi(clusterRestUrl.get());
        } catch (Exception e) {
            logger().warn(
                "Failed to visit remote flink jobs on kubernetes-native-mode cluster, and the retry"
                    + " access logic is performed.");
            Optional<String> clusterRestUrl = watchController.refreshClusterRestUrl(clusterKey);
            if (!clusterRestUrl.isPresent()) {
                return Optional.empty();
            }
            try {
                Optional<JobsOverview> result = callJobsOverviewsApi(clusterRestUrl.get());
                logger().info("The retry is successful.");
                return result;
            } catch (Exception retryEx) {
                logger()
                    .warn(
                        "The retry fetch failed, final status failed, errorStack="
                            + retryEx.getMessage()
                            + ".");
                return Optional.empty();
            }
        }
    }

    private Optional<JobsOverview> callJobsOverviewsApi(String restUrl) throws Exception {
        String json =
            Request.get(restUrl + "/jobs/overview")
                .connectTimeout(KubernetesRetriever.FLINK_REST_AWAIT_TIMEOUT_SEC)
                .responseTimeout(KubernetesRetriever.FLINK_CLIENT_TIMEOUT_SEC)
                .execute()
                .returnContent()
                .asString(StandardCharsets.UTF_8);
        return JobsOverview.parse(json);
    }

    private Optional<JobStatusCV> inferStateFromK8sEvent(TrackId trackId, long pollEmitTime) {
        JobStatusCV latest = watchController.jobStatuses.get(trackId);
        FlinkJobState jobState;
        if (watchController.canceling.has(trackId)) {
            logger().info("trackId " + trackId + " is canceling");
            watchController.trackIds.invalidate(trackId);
            jobState = FlinkJobState.CANCELED;
        } else {
            boolean deployExists =
                KubernetesRetriever.isDeploymentExists(trackId.namespace(), trackId.clusterId());
            boolean isConnection = KubernetesDeploymentHelper.checkConnection();
            if (deployExists) {
                boolean deployError =
                    KubernetesDeploymentHelper.isDeploymentError(
                        trackId.namespace(), trackId.clusterId());
                if (!deployError) {
                    logger().info("Task Enter the initialization process.");
                    jobState = FlinkJobState.K8S_INITIALIZING;
                } else if (isConnection) {
                    logger().info("Enter the task failure deletion process.");
                    KubernetesDeploymentHelper.watchPodTerminatedLog(
                        trackId.namespace(), trackId.clusterId(), trackId.jobId());
                    jobState = FlinkJobState.FAILED;
                } else {
                    jobState = inferFromPreCache(latest);
                }
            } else if (isConnection) {
                logger().info("The deployment is deleted and enters the task failure process.");
                jobState =
                    FlinkJobState.of(
                        FlinkHistoryArchives.getJobStateFromArchiveFile(trackId));
            } else {
                jobState = inferFromPreCache(latest);
            }
        }

        JobStatusCV jobStatusCV =
            JobStatusCV.builder()
                .jobState(jobState)
                .jobId(trackId.jobId())
                .pollEmitTime(pollEmitTime)
                .pollAckTime(System.currentTimeMillis())
                .build();

        if (jobState == FlinkJobState.SILENT
            && latest != null
            && latest.jobState() == FlinkJobState.SILENT) {
            return Optional.of(
                jobStatusCV.toBuilder()
                    .pollEmitTime(latest.pollEmitTime())
                    .pollAckTime(latest.pollAckTime())
                    .build());
        }
        return Optional.of(jobStatusCV);
    }

    private FlinkJobState inferFromPreCache(JobStatusCV preCache) {
        if (preCache == null) {
            return FlinkJobState.SILENT;
        }
        if (preCache.jobState() == FlinkJobState.SILENT
            && System.currentTimeMillis() - preCache.pollAckTime() >= conf.silentStateJobKeepTrackingSec() * 1000L) {
            return FlinkJobState.LOST;
        }
        return FlinkJobState.SILENT;
    }

    /**
     * infer flink job state before persistence.
     *
     * @param current current flink job state
     * @param previous previous flink job state from persistent storage
     */
    public static FlinkJobState inferFlinkJobStateFromPersist(
                                                              FlinkJobState current,
                                                              FlinkJobState previous) {
        switch (current) {
            case POS_TERMINATED:
            case TERMINATED:
                switch (previous) {
                    case CANCELLING:
                        return FlinkJobState.CANCELED;
                    case FAILING:
                        return FlinkJobState.FAILED;
                    default:
                        if (current == FlinkJobState.POS_TERMINATED) {
                            return FlinkJobState.FINISHED;
                        }
                        return FlinkJobState.TERMINATED;
                }
            default:
                return current;
        }
    }
}
