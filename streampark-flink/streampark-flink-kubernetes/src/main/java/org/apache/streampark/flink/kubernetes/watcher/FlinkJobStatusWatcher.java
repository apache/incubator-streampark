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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.util.Timeout;
import org.apache.streampark.flink.kubernetes.ChangeEventBus;
import org.apache.streampark.flink.kubernetes.FlinkK8sWatchController;
import org.apache.streampark.flink.kubernetes.IngressController;
import org.apache.streampark.flink.kubernetes.KubernetesRetriever;
import org.apache.streampark.flink.kubernetes.TrackConfig.JobStatusWatcherConfig;
import org.apache.streampark.flink.kubernetes.TrackConfig.MetricWatcherConfig;
import org.apache.streampark.flink.kubernetes.enums.FlinkJobState;
import org.apache.streampark.flink.kubernetes.enums.FlinkK8sExecuteMode;
import org.apache.streampark.flink.kubernetes.event.FlinkJobStatusChangeEvent;
import org.apache.streampark.flink.kubernetes.helper.KubernetesDeploymentHelper;
import org.apache.streampark.flink.kubernetes.model.ClusterKey;
import org.apache.streampark.flink.kubernetes.model.JobStatusCV;
import org.apache.streampark.flink.kubernetes.model.TrackId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class FlinkJobStatusWatcher implements FlinkWatcher {

    private static final Logger LOG = LoggerFactory.getLogger(FlinkJobStatusWatcher.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final JobStatusWatcherConfig conf;

    private final FlinkK8sWatchController watchController;

    private final ChangeEventBus eventBus;

    private final ExecutorService trackTaskExecPool = Executors.newWorkStealingPool();

    private final ScheduledExecutorService timerExec = Executors.newSingleThreadScheduledExecutor();

    private ScheduledFuture<?> timerSchedule;

    public FlinkJobStatusWatcher(JobStatusWatcherConfig conf, FlinkK8sWatchController watchController, ChangeEventBus eventBus) {
        this.conf = conf;
        this.watchController = watchController;
        this.eventBus = eventBus;
    }

    @Override
    public void doStart() {
        timerSchedule = timerExec.scheduleAtFixedRate(this::doWatch, 0, conf.requestIntervalSec, TimeUnit.SECONDS);
        LOG.info("[flink-k8s] FlinkJobStatusWatcher started.");
    }

    @Override
    public void doStop() {
        timerSchedule.cancel(true);
        LOG.info("[flink-k8s] FlinkJobStatusWatcher stopped.");
    }

    @Override
    public void doClose() {
        timerExec.shutdownNow();
        trackTaskExecPool.shutdownNow();
        LOG.info("[flink-k8s] FlinkJobStatusWatcher closed.");
    }

    @Override
    public void doWatch() {
        synchronized (this) {
            LOG.debug("[FlinkJobStatusWatcher]: Status monitoring process begins - {}", Thread.currentThread().getName());
            Set<TrackId> allWatchingIds = watchController.getAllWatchingIds();
            if (allWatchingIds.isEmpty()) {
                return;
            }
            List<CompletableFuture<Optional<JobStatusCV>>> futures = allWatchingIds.stream().map(trackId -> {
                CompletableFuture<Optional<JobStatusCV>> future;
                if (Objects.requireNonNull(trackId.getExecuteMode()) == FlinkK8sExecuteMode.SESSION) {
                    future = CompletableFuture.supplyAsync(() -> touchSessionJob(trackId));
                } else {
                    future = CompletableFuture.supplyAsync(() -> touchApplicationJob(trackId));
                }
                future.thenAccept(optionalJobStatus -> {
                    optionalJobStatus.ifPresent(jobStatusCV -> {
                        TrackId copy = trackId.copy(jobStatusCV.getJobId());
                        JobStatusCV latest = watchController.getJobStatusCache().get(copy);
                        if (Objects.isNull(latest) || latest.getJobState() != jobStatusCV.getJobState() || !Objects.equals(latest.getJobId(), jobStatusCV.getJobId())) {
                            watchController.getJobStatusCache().put(copy, jobStatusCV);
                            watchController.getTrackIdCache().update(copy);
                            eventBus.postAsync(new FlinkJobStatusChangeEvent(copy, jobStatusCV));
                        }
                        if (FlinkJobState.isEndState(jobStatusCV.getJobState())) {
                            watchController.unWatching(copy);
                            if (copy.getExecuteMode() == FlinkK8sExecuteMode.APPLICATION) {
                                watchController.getEndpointCache().invalidate(copy.toClusterKey());
                            }
                        }
                    });
                });
                return future;
            }).collect(Collectors.toList());
            CompletableFuture<Void> allFutures =
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            try {
                allFutures.get(conf.requestTimeoutSec, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException e) {
                LOG.warn("Future interrupted");
            } catch (TimeoutException e) {
                LOG.error(
                    "[FlinkJobStatusWatcher] tracking flink job status on kubernetes mode timeout, "
                        + "limitSeconds = {}, trackingClusterKeys = {}",
                    conf.requestTimeoutSec,
                    allWatchingIds.stream().map(TrackId::getClusterId).collect(Collectors.joining(",")));
            }
            LOG.debug("[FlinkJobStatusWatcher]: End of status monitoring process - {}", Thread.currentThread().getName());
        }
    }

    public Optional<JobStatusCV> touchApplicationJob(TrackId trackId) {
        long pollEmitTime = System.currentTimeMillis();
        String clusterId = trackId.getClusterId();
        String namespace = trackId.getNamespace();
        Optional<List<JobDetail>> jobDetails = listJobDetails(new ClusterKey(FlinkK8sExecuteMode.APPLICATION, namespace, clusterId));
        if (!jobDetails.isPresent() || jobDetails.get().isEmpty()) {
            return inferApplicationFlinkJobStateFromK8sEvent(trackId, pollEmitTime);
        }
        JobStatusCV jobStatusCV = jobDetails.get().get(0).toJobStatusCV(pollEmitTime, System.currentTimeMillis());
        return Optional.of(jobStatusCV);
    }

    public Optional<JobStatusCV> inferApplicationFlinkJobStateFromK8sEvent(TrackId trackId, long pollEmitTime) {
        JobStatusCV latest = watchController.getJobStatusCache().get(trackId);
        FlinkJobState jobState;
        if (watchController.getCancellingCache().has(trackId)) {
            jobState = FlinkJobState.CANCELED;
        } else {
            boolean deploymentExists = KubernetesRetriever.isDeploymentExists(trackId.getClusterId(), trackId.getNamespace());
            boolean deploymentStatusChanges = KubernetesDeploymentHelper.getDeploymentStatusChanges(trackId.getNamespace(), trackId.getClusterId());
            boolean theK8sConnectionNormal = KubernetesDeploymentHelper.isTheK8sConnectionNormal();
            if (deploymentExists) {
                if (!deploymentStatusChanges) {
                    jobState = FlinkJobState.K8S_INITIALIZING;
                } else if (theK8sConnectionNormal) {
                    KubernetesDeploymentHelper.watchPodTerminatedLog(trackId.getNamespace(), trackId.getClusterId(), trackId.getJobId());
                    KubernetesDeploymentHelper.deleteTaskDeployment(trackId.getNamespace(), trackId.getClusterId());
                    IngressController.deleteIngress(trackId.getNamespace(), trackId.getClusterId());
                    jobState = FlinkJobState.FAILED;
                } else {
                    jobState = inferSilentOrLostFromPreCache(latest);
                }
            } else if (theK8sConnectionNormal) {
                jobState = FlinkJobState.of(FlinkHistoryArchives.getJobStateFromArchiveFile(trackId.getJobId()));
            } else {
                jobState = inferSilentOrLostFromPreCache(latest);
            }
        }
        JobStatusCV jobStatusCV;
        if (jobState == FlinkJobState.SILENT && Objects.nonNull(latest) && latest.getJobState() == FlinkJobState.SILENT) {
            jobStatusCV = new JobStatusCV(jobState, trackId.getJobId(), latest.getPollEmitTime(), latest.getPollAckTime());
        } else {
            jobStatusCV = new JobStatusCV(jobState, trackId.getJobId(), pollEmitTime, System.currentTimeMillis());
        }
        return Optional.of(jobStatusCV);
    }

    public Optional<JobStatusCV> touchSessionJob(TrackId trackId) {
        long pollEmitTime = System.currentTimeMillis();
        String clusterId = trackId.getClusterId();
        String namespace = trackId.getNamespace();
        long appId = trackId.getAppId();
        String jobId = trackId.getJobId();
        Map<TrackId, JobStatusCV> trackIdJobStatusCVMap = touchSessionAllJob(clusterId, namespace, appId, trackId.getGroupId());
        TrackId id = new TrackId(FlinkK8sExecuteMode.SESSION, namespace, appId, jobId, trackId.getGroupId());
        JobStatusCV jobStatusCV = trackIdJobStatusCVMap.get(id);
        if (jobStatusCV.getJobState() != FlinkJobState.SILENT) {
            return Optional.of(jobStatusCV);
        }
        JobStatusCV preCache = watchController.getJobStatusCache().get(id);
        FlinkJobState flinkJobState = inferSilentOrLostFromPreCache(preCache);
        if (flinkJobState == FlinkJobState.SILENT && preCache != null && preCache.getJobState() == FlinkJobState.SILENT) {
            JobStatusCV result = new JobStatusCV(flinkJobState, id.getJobId(), preCache.getPollEmitTime(), preCache.getPollAckTime());
            return Optional.of(result);
        }
        JobStatusCV result = new JobStatusCV(flinkJobState, id.getJobId(), pollEmitTime, System.currentTimeMillis());
        return Optional.of(result);
    }

    private FlinkJobState inferSilentOrLostFromPreCache(JobStatusCV preCache) {
        if (Objects.isNull(preCache)) {
            return FlinkJobState.SILENT;
        }
        if (preCache.getJobState() == FlinkJobState.SILENT && System.currentTimeMillis() - preCache.getPollAckTime() >= conf.silentStateJobKeepTrackingSec * 1000L) {
            return FlinkJobState.LOST;
        }
        return FlinkJobState.SILENT;
    }

    public Map<TrackId, JobStatusCV> touchSessionAllJob(String clusterId, String namespace, long appId, String groupId) {
        Map<TrackId, JobStatusCV> result = new HashMap<>();
        long pollEmitTime = System.currentTimeMillis();
        Optional<List<JobDetail>> jobDetails = listJobDetails(new ClusterKey(FlinkK8sExecuteMode.SESSION, namespace, clusterId));
        jobDetails.ifPresent(details -> details.forEach(jobDetail -> {
            TrackId trackId = new TrackId(FlinkK8sExecuteMode.SESSION, namespace, clusterId, appId, jobDetail.jid, groupId);
            JobStatusCV jobStatusCV = jobDetail.toJobStatusCV(pollEmitTime, System.currentTimeMillis());
            result.put(trackId, jobStatusCV);
        }));
        return result;
    }

    public Optional<List<JobDetail>> listJobDetails(ClusterKey clusterKey) {
        Optional<String> clusterRestUrl = watchController.getClusterRestUrl(clusterKey);
        if (!clusterRestUrl.isPresent()) {
            return Optional.empty();
        }
        String requestUrl = String.format("%s/jobs/overview", clusterRestUrl.get());
        try {
            String response = Request.get(requestUrl)
                .connectTimeout(Timeout.ofSeconds(KubernetesRetriever.FLINK_REST_AWAIT_TIMEOUT_SEC))
                .responseTimeout(Timeout.ofSeconds(KubernetesRetriever.FLINK_CLIENT_TIMEOUT_SEC))
                .execute()
                .returnContent()
                .asString(StandardCharsets.UTF_8);
            JsonNode root = OBJECT_MAPPER.readTree(response);
            JsonNode jobs = root.get("jobs");
            List<JobDetail> jobDetails = new ArrayList<>();
            jobs.forEach(job -> {
                JsonNode tasks = job.get("tasks");
                String jid = job.get("jid").asText(null);
                String name = job.get("name").asText(null);
                String state = job.get("state").asText(null);
                long startTime = job.get("start-time").asLong(0L);
                long endTime = job.get("end-time").asLong(0L);
                long duration = job.get("duration").asLong(0L);
                long lastModification = job.get("last-modification").asLong(0L);
                int total = tasks.get("total").asInt(0);
                int created = tasks.get("created").asInt(0);
                int scheduled = tasks.get("scheduled").asInt(0);
                int deploying = tasks.get("deploying").asInt(0);
                int running = tasks.get("running").asInt(0);
                int finished = tasks.get("finished").asInt(0);
                int canceling = tasks.get("canceling").asInt(0);
                int canceled = tasks.get("canceled").asInt(0);
                int failed = tasks.get("failed").asInt(0);
                int reconciling = tasks.get("reconciling").asInt(0);
                int initializing = tasks.get("initializing").asInt(0);
                JobTask jobTask = new JobTask(total, created, scheduled, deploying, running, finished, canceling, canceled, failed, reconciling, initializing);
                JobDetail jobDetail = new JobDetail(jid, name, state, startTime, endTime, duration, lastModification, jobTask);
                jobDetails.add(jobDetail);
            });
            return Optional.of(jobDetails);
        } catch (IOException e) {
            // TODO: Add retry logic
            LOG.warn("Get job details failed from url [{}] failed", requestUrl);
            return Optional.empty();
        }
    }

    public static class JobDetail {
        private final String jid;

        private final String name;

        private final String state;

        private final long startTime;

        private final long endTime;

        private final long duration;

        private final long lastModification;

        private final JobTask jobTask;

        public JobStatusCV toJobStatusCV(long pollEmitTime, long pollAckTime) {
            return new JobStatusCV(FlinkJobState.of(state), jid, pollEmitTime, pollAckTime, name, startTime, endTime, duration, jobTask.getTotal());
        }

        public JobDetail(String jid, String name, String state, long startTime, long endTime, long duration, long lastModification, JobTask jobTask) {
            this.jid = jid;
            this.name = name;
            this.state = state;
            this.startTime = startTime;
            this.endTime = endTime;
            this.duration = duration;
            this.lastModification = lastModification;
            this.jobTask = jobTask;
        }

        public String getJid() {
            return jid;
        }

        public String getName() {
            return name;
        }

        public String getState() {
            return state;
        }

        public long getStartTime() {
            return startTime;
        }

        public long getEndTime() {
            return endTime;
        }

        public long getDuration() {
            return duration;
        }

        public long getLastModification() {
            return lastModification;
        }

        public JobTask getJobTask() {
            return jobTask;
        }
    }

    public static class JobTask {
        private final int total;
        private final int created;
        private final int scheduled;
        private final int deploying;
        private final int running;
        private final int finished;
        private final int canceling;
        private final int canceled;
        private final int failed;
        private final int reconciling;
        private final int initializing;

        public JobTask(int total, int created, int scheduled, int deploying, int running, int finished, int canceling, int canceled, int failed, int reconciling, int initializing) {
            this.total = total;
            this.created = created;
            this.scheduled = scheduled;
            this.deploying = deploying;
            this.running = running;
            this.finished = finished;
            this.canceling = canceling;
            this.canceled = canceled;
            this.failed = failed;
            this.reconciling = reconciling;
            this.initializing = initializing;
        }

        public int getTotal() {
            return total;
        }

        public int getCreated() {
            return created;
        }

        public int getScheduled() {
            return scheduled;
        }

        public int getDeploying() {
            return deploying;
        }

        public int getRunning() {
            return running;
        }

        public int getFinished() {
            return finished;
        }

        public int getCanceling() {
            return canceling;
        }

        public int getCanceled() {
            return canceled;
        }

        public int getFailed() {
            return failed;
        }

        public int getReconciling() {
            return reconciling;
        }

        public int getInitializing() {
            return initializing;
        }
    }
}
