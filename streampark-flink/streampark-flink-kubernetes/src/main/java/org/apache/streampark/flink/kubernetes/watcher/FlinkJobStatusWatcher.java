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
import org.apache.streampark.flink.kubernetes.watcher.FlinkRestModels.JobDetail;
import org.apache.streampark.flink.kubernetes.watcher.FlinkRestModels.JobDetails;

import org.apache.commons.io.FileUtils;
import org.apache.flink.configuration.JobManagerOptions;
import org.apache.flink.core.fs.Path;
import org.apache.flink.runtime.history.FsJobArchivist;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.util.Timeout;

import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@ThreadSafe
public class FlinkJobStatusWatcher extends FlinkWatcher {

    private static final String FAILED_STATE = "FAILED";

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
                        this::doWatch,
                        0,
                        conf.requestIntervalSec(),
                        TimeUnit.SECONDS);
        log.info("[flink-k8s] FlinkJobStatusWatcher started.");
    }

    @Override
    protected void doStop() {
        if (timerSchedule != null && !timerSchedule.isCancelled()) {
            timerSchedule.cancel(true);
        }
        log.info("[flink-k8s] FlinkJobStatusWatcher stopped.");
    }

    @Override
    protected void doClose() {
        log.info("[flink-k8s] FlinkJobStatusWatcher closed.");
    }

    @Override
    public synchronized void doWatch() {
        Set<TrackId> trackIds;
        try {
            trackIds = watchController.getAllWatchingIds();
            if (trackIds.isEmpty()) {
                return;
            }
        } catch (Exception e) {
            return;
        }

        Set<CompletableFuture<Optional<JobStatusCV>>> appFutures =
                trackIds.stream()
                        .filter(id -> id.executeMode() == FlinkK8sDeployMode.APPLICATION)
                        .map(
                                id ->
                                        CompletableFuture.supplyAsync(
                                                        () -> touchApplicationJob(id),
                                                        watchExecutor)
                                                .whenComplete(
                                                        (jobState, error) ->
                                                                jobState.ifPresent(
                                                                        state ->
                                                                                updateState(
                                                                                        id.copy()
                                                                                                .jobId(
                                                                                                        state
                                                                                                                .jobId()),
                                                                                        state))))
                        .collect(Collectors.toSet());

        Set<TrackId> sessionIds =
                trackIds.stream()
                        .filter(id -> id.executeMode() == FlinkK8sDeployMode.SESSION)
                        .collect(Collectors.toSet());
        Set<TrackId> sessionCluster =
                sessionIds.stream()
                        .collect(
                                Collectors.groupingBy(
                                        id -> id.toClusterKey().toString(),
                                        Collectors.toSet()))
                        .values()
                        .stream()
                        .flatMap(Set::stream)
                        .collect(Collectors.toSet());

        Set<CompletableFuture<Map<TrackId, JobStatusCV>>> sessionFutures =
                sessionCluster.stream()
                        .map(
                                trackId ->
                                        CompletableFuture.supplyAsync(
                                                        () -> touchSessionAllJob(trackId),
                                                        watchExecutor)
                                                .whenComplete(
                                                        (map, error) -> {
                                                            if (map == null) {
                                                                return;
                                                            }
                                                            Optional<Map.Entry<TrackId, JobStatusCV>> matched =
                                                                    map.entrySet().stream()
                                                                            .filter(
                                                                                    e ->
                                                                                            e.getKey()
                                                                                                    .jobId()
                                                                                                    .equals(
                                                                                                            trackId
                                                                                                                    .jobId()))
                                                                            .findFirst();
                                                            matched.ifPresent(
                                                                    job ->
                                                                            updateState(
                                                                                    job.getKey()
                                                                                            .copy()
                                                                                            .appId(
                                                                                                    trackId
                                                                                                            .appId()),
                                                                                    job.getValue()));
                                                            if (!matched.isPresent()) {
                                                                touchSessionJob(trackId)
                                                                        .ifPresent(
                                                                                state ->
                                                                                        updateState(
                                                                                                trackId,
                                                                                                state));
                                                            }
                                                        }))
                        .collect(Collectors.toSet());

        try {
            CompletableFuture.allOf(appFutures.toArray(new CompletableFuture[0]))
                    .get(conf.requestTimeoutSec(), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[FlinkJobStatusWatcher] interrupted while waiting for application job status");
        } catch (Exception e) {
            log.warn(
                    "[FlinkJobStatusWatcher] tracking flink job status on kubernetes native application mode timeout, limitSeconds={}, trackIds={}",
                    conf.requestTimeoutSec(),
                    trackIds);
        }

        try {
            CompletableFuture.allOf(sessionFutures.toArray(new CompletableFuture[0]))
                    .get(conf.requestTimeoutSec(), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[FlinkJobStatusWatcher] interrupted while waiting for session job status");
        } catch (Exception e) {
            log.warn(
                    "[FlinkJobStatusWatcher] tracking flink job status on kubernetes native session mode timeout, limitSeconds={}, trackIds={}",
                    conf.requestTimeoutSec(),
                    trackIds);
        }
    }

    public Optional<JobStatusCV> touchSessionJob(@Nonnull TrackId trackId) {
        return touchSessionAllJob(trackId).entrySet().stream()
                .filter(
                        e ->
                                e.getKey().jobId().equals(trackId.jobId())
                                        && e.getValue().jobState() != FlinkJobState.SILENT)
                .map(Map.Entry::getValue)
                .findFirst()
                .or(() -> inferState(trackId));
    }

    private Map<TrackId, JobStatusCV> touchSessionAllJob(TrackId trackId) {
        long pollEmitTime = System.currentTimeMillis();
        Optional<JobDetails> jobDetails = listJobsDetails(ClusterKey.of(trackId));
        if (!jobDetails.isPresent() || jobDetails.get().jobs().length == 0) {
            return new HashMap<>();
        }
        Map<TrackId, JobStatusCV> result = new HashMap<>();
        for (JobDetail detail : jobDetails.get().jobs()) {
            JobStatusCV jobStatus = detail.toJobStatusCV(pollEmitTime, System.currentTimeMillis());
            TrackId trackItem = trackId.copy().jobId(detail.jid()).appId(null);
            result.put(trackItem, jobStatus);
        }
        return result;
    }

    public Optional<JobStatusCV> touchApplicationJob(@Nonnull TrackId trackId) {
        long pollEmitTime = System.currentTimeMillis();
        Optional<JobDetails> jobDetails = listJobsDetails(ClusterKey.of(trackId));
        if (!jobDetails.isPresent() || jobDetails.get().jobs().length == 0) {
            return inferStateFromK8sEvent(trackId, pollEmitTime);
        }
        return Optional.of(
                jobDetails.get().jobs()[0].toJobStatusCV(pollEmitTime, System.currentTimeMillis()));
    }

    private void updateState(TrackId trackId, JobStatusCV jobState) {
        JobStatusCV latest = watchController.jobStatuses.get(trackId);
        if (jobState.diff(latest)) {
            watchController.jobStatuses.put(trackId, jobState);
            watchController.trackIds.update(trackId);
            eventBus.postSync(new FlinkJobStatusChangeEvent(trackId, jobState));
        }
        if (FlinkJobState.isEndState(jobState.jobState())) {
            if (trackId.executeMode() == FlinkK8sDeployMode.APPLICATION) {
                boolean deployExists =
                        KubernetesRetriever.isDeploymentExists(
                                trackId.namespace(), trackId.clusterId());
                if (!deployExists) {
                    watchController.endpoints.invalidate(trackId.toClusterKey());
                    watchController.unWatching(trackId);
                }
            } else if (trackId.executeMode() == FlinkK8sDeployMode.SESSION) {
                watchController.unWatching(trackId);
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
                    new JobStatusCV(
                            state,
                            id.jobId(),
                            preCache.jobName(),
                            preCache.jobStartTime(),
                            preCache.jobEndTime(),
                            preCache.duration(),
                            preCache.taskTotal(),
                            preCache.pollEmitTime(),
                            preCache.pollAckTime());
        } else {
            jobState =
                    new JobStatusCV(
                            state,
                            id.jobId(),
                            "",
                            -1,
                            -1,
                            0,
                            0,
                            pollEmitTime,
                            System.currentTimeMillis());
        }
        return Optional.of(jobState);
    }

    private Optional<JobDetails> listJobsDetails(ClusterKey clusterKey) {
        try {
            Optional<String> clusterRestUrl =
                    watchController.getClusterRestUrl(clusterKey).filter(url -> !url.isEmpty());
            if (!clusterRestUrl.isPresent()) {
                return Optional.empty();
            }
            return callJobsOverviewsApi(clusterRestUrl.get());
        } catch (Exception e) {
            log.warn(
                    "Failed to visit remote flink jobs on kubernetes-native-mode cluster, and the retry access logic is performed.");
            Optional<String> clusterRestUrl =
                    watchController.refreshClusterRestUrl(clusterKey);
            if (!clusterRestUrl.isPresent()) {
                return Optional.empty();
            }
            try {
                Optional<JobDetails> result = callJobsOverviewsApi(clusterRestUrl.get());
                log.info("The retry is successful.");
                return result;
            } catch (Exception retryError) {
                log.warn(
                        "The retry fetch failed, final status failed, errorStack={}.",
                        retryError.getMessage());
                return Optional.empty();
            }
        }
    }

    private Optional<JobDetails> callJobsOverviewsApi(String restUrl) throws Exception {
        String json =
                Request.get(restUrl + "/jobs/overview")
                        .connectTimeout(KubernetesRetriever.FLINK_REST_AWAIT_TIMEOUT_SEC)
                        .responseTimeout(KubernetesRetriever.FLINK_CLIENT_TIMEOUT_SEC)
                        .execute()
                        .returnContent()
                        .asString(StandardCharsets.UTF_8);
        return FlinkRestModels.parseJobDetails(json);
    }

    private Optional<JobStatusCV> inferStateFromK8sEvent(TrackId trackId, long pollEmitTime) {
        JobStatusCV latest = watchController.jobStatuses.get(trackId);
        FlinkJobState jobState;
        if (watchController.canceling.has(trackId)) {
            log.info("trackId {} is canceling", trackId);
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
                    log.info("Task Enter the initialization process.");
                    jobState = FlinkJobState.K8S_INITIALIZING;
                } else if (isConnection) {
                    log.info("Enter the task failure deletion process.");
                    KubernetesDeploymentHelper.watchPodTerminatedLog(
                            trackId.namespace(), trackId.clusterId(), trackId.jobId());
                    jobState = FlinkJobState.FAILED;
                } else {
                    jobState = inferFromPreCache(latest);
                }
            } else if (isConnection) {
                log.info("The deployment is deleted and enters the task failure process.");
                jobState =
                        FlinkJobState.of(
                                FlinkHistoryArchives.getJobStateFromArchiveFile(trackId));
            } else {
                jobState = inferFromPreCache(latest);
            }
        }

        JobStatusCV jobStatusCV =
                new JobStatusCV(
                        jobState,
                        trackId.jobId(),
                        "",
                        -1,
                        -1,
                        0,
                        0,
                        pollEmitTime,
                        System.currentTimeMillis());

        if (jobState == FlinkJobState.SILENT
                && latest != null
                && latest.jobState() == FlinkJobState.SILENT) {
            return Optional.of(
                    new JobStatusCV(
                            jobState,
                            trackId.jobId(),
                            latest.jobName(),
                            latest.jobStartTime(),
                            latest.jobEndTime(),
                            latest.duration(),
                            latest.taskTotal(),
                            latest.pollEmitTime(),
                            latest.pollAckTime()));
        }
        return Optional.of(jobStatusCV);
    }

    private FlinkJobState inferFromPreCache(JobStatusCV preCache) {
        if (preCache == null) {
            return FlinkJobState.SILENT;
        }
        if (preCache.jobState() == FlinkJobState.SILENT
                && System.currentTimeMillis() - preCache.pollAckTime()
                        >= conf.silentStateJobKeepTrackingSec() * 1000L) {
            return FlinkJobState.LOST;
        }
        return FlinkJobState.SILENT;
    }

    public static FlinkJobState inferFlinkJobStateFromPersist(
            FlinkJobState current, FlinkJobState previous) {
        switch (current) {
            case POS_TERMINATED:
            case TERMINATED:
                switch (previous) {
                    case CANCELLING:
                        return FlinkJobState.CANCELED;
                    case FAILING:
                        return FlinkJobState.FAILED;
                    default:
                        return current == FlinkJobState.POS_TERMINATED
                                ? FlinkJobState.FINISHED
                                : FlinkJobState.TERMINATED;
                }
            default:
                return current;
        }
    }

    static final class FlinkHistoryArchives {
        private FlinkHistoryArchives() {}

        static String getJobStateFromArchiveFile(TrackId trackId) {
            try {
                if (trackId.jobId() == null) {
                    throw new IllegalArgumentException(
                            "[StreamPark] getJobStateFromArchiveFile: JobId cannot be null.");
                }
                String archiveDir = trackId.properties().getProperty(JobManagerOptions.ARCHIVE_DIR.key());
                if (archiveDir == null) {
                    return FAILED_STATE;
                }
                Path archivePath = new Path(archiveDir, trackId.jobId());
                var archivedJsons = FsJobArchivist.getArchivedJsons(archivePath);
                if (archivedJsons.isEmpty()) {
                    return FAILED_STATE;
                }
                for (var archivedJson : archivedJsons) {
                    if (("/jobs/" + trackId.jobId() + "/exceptions").equals(archivedJson.getPath())) {
                        try {
                            var ok =
                                    org.apache.streampark.common.util.JsonUtils.read(
                                            archivedJson.getJson(),
                                            org.apache.streampark.shaded.com.fasterxml.jackson.databind
                                                    .JsonNode.class);
                            String logText = ok.path("root-exception").asText(null);
                            if (logText != null) {
                                String path =
                                        KubernetesDeploymentHelper.getJobErrorLog(trackId.jobId());
                                FileUtils.writeStringToFile(new File(path), logText, StandardCharsets.UTF_8);
                                log.info(" error path: {}", path);
                            }
                        } catch (Exception ignored) {
                        }
                    } else if ("/jobs/overview".equals(archivedJson.getPath())) {
                        try {
                            var ok =
                                    org.apache.streampark.common.util.JsonUtils.read(
                                            archivedJson.getJson(),
                                            org.apache.streampark.shaded.com.fasterxml.jackson.databind
                                                    .JsonNode.class);
                            var jobs = ok.get("jobs");
                            if (jobs != null && jobs.isArray()) {
                                for (var node : jobs) {
                                    if (trackId.jobId().equals(node.path("jid").asText(null))) {
                                        return node.path("state").asText(FAILED_STATE);
                                    }
                                }
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
                return FAILED_STATE;
            } catch (Exception e) {
                return FAILED_STATE;
            }
        }
    }
}
