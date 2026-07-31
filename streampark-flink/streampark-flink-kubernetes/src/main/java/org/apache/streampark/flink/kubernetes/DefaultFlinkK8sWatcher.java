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

package org.apache.streampark.flink.kubernetes;

import org.apache.streampark.common.util.LoggerSupport;
import org.apache.streampark.flink.kubernetes.enums.FlinkJobState;
import org.apache.streampark.flink.kubernetes.event.BuildInEvent;
import org.apache.streampark.flink.kubernetes.event.FlinkJobStateEvent;
import org.apache.streampark.flink.kubernetes.event.FlinkJobStatusChangeEvent;
import org.apache.streampark.flink.kubernetes.model.ClusterKey;
import org.apache.streampark.flink.kubernetes.model.FlinkMetricCV;
import org.apache.streampark.flink.kubernetes.model.JobStatusCV;
import org.apache.streampark.flink.kubernetes.model.TrackId;
import org.apache.streampark.flink.kubernetes.watcher.FlinkCheckpointWatcher;
import org.apache.streampark.flink.kubernetes.watcher.FlinkJobStatusWatcher;
import org.apache.streampark.flink.kubernetes.watcher.FlinkK8sEventWatcher;
import org.apache.streampark.flink.kubernetes.watcher.FlinkMetricWatcher;
import org.apache.streampark.flink.kubernetes.watcher.FlinkWatcher;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import javax.annotation.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Default K8sFlinkTrackMonitor implementation. */
public class DefaultFlinkK8sWatcher extends LoggerSupport implements FlinkK8sWatcher {

    public final FlinkK8sWatchController watchController;

    private final ChangeEventBus eventBus;
    private final FlinkK8sEventWatcher k8sEventWatcher;
    private final FlinkJobStatusWatcher jobStatusWatcher;
    private final FlinkMetricWatcher metricsWatcher;
    private final FlinkCheckpointWatcher checkpointWatcher;
    private final FlinkWatcher[] allWatchers;

    public DefaultFlinkK8sWatcher(FlinkTrackConfig conf) {
        this.watchController = new FlinkK8sWatchController(conf.jobStatusWatcherConf());
        this.eventBus = new ChangeEventBus();
        this.eventBus.registerListener(new BuildInEventListener());
        this.k8sEventWatcher = new FlinkK8sEventWatcher(watchController);
        this.jobStatusWatcher =
            new FlinkJobStatusWatcher(
                conf.jobStatusWatcherConf(), watchController, eventBus);
        this.metricsWatcher =
            new FlinkMetricWatcher(conf.metricWatcherConf(), watchController, eventBus);
        this.checkpointWatcher =
            new FlinkCheckpointWatcher(conf.metricWatcherConf(), watchController, eventBus);
        this.allWatchers =
            new FlinkWatcher[]{
                    k8sEventWatcher, jobStatusWatcher, metricsWatcher, checkpointWatcher
            };
    }

    @Override
    public void registerListener(Object listener) {
        eventBus.registerListener(listener);
    }

    @Override
    public void start() {
        for (FlinkWatcher watcher : allWatchers) {
            watcher.start();
        }
    }

    @Override
    public void stop() {
        for (FlinkWatcher watcher : allWatchers) {
            watcher.stop();
        }
    }

    @Override
    public void restart() {
        for (FlinkWatcher watcher : allWatchers) {
            watcher.restart();
        }
    }

    @Override
    public void close() {
        for (FlinkWatcher watcher : allWatchers) {
            watcher.close();
        }
        watchController.close();
    }

    @Override
    public void doWatching(TrackId trackId) {
        if (trackId.isLegal()) {
            watchController.trackIds.set(trackId);
        }
    }

    @Override
    public void unWatching(TrackId trackId) {
        watchController.canceling.set(trackId);
    }

    @Override
    public boolean isInWatching(TrackId trackId) {
        return watchController.isInWatching(trackId);
    }

    @Override
    public Optional<JobStatusCV> getJobStatus(TrackId trackId) {
        return Optional.ofNullable(watchController.jobStatuses.get(trackId));
    }

    @Override
    public Map<CacheKey, JobStatusCV> getJobStatus(Set<TrackId> trackIds) {
        return watchController.jobStatuses.getAsMap(trackIds);
    }

    @Override
    public Map<CacheKey, JobStatusCV> getAllJobStatus() {
        return watchController.jobStatuses.asMap();
    }

    @Override
    public FlinkMetricCV getAccGroupMetrics(@Nullable String groupId) {
        return watchController.collectAccGroupMetric(groupId);
    }

    @Override
    public Optional<FlinkMetricCV> getClusterMetrics(ClusterKey clusterKey) {
        return Optional.ofNullable(watchController.flinkMetrics.get(clusterKey));
    }

    @Override
    public Set<TrackId> getAllWatchingIds() {
        return watchController.getAllWatchingIds();
    }

    @Override
    public boolean checkIsInRemoteCluster(TrackId trackId) {
        if (!trackId.isLegal()) {
            return false;
        }
        java.util.function.Predicate<FlinkJobState> nonLost =
            state -> state != FlinkJobState.LOST || state != FlinkJobState.SILENT;
        switch (trackId.executeMode()) {
            case SESSION:
                return jobStatusWatcher
                    .touchSessionJob(trackId)
                    .map(e -> nonLost.test(e.jobState()))
                    .orElse(false);
            case APPLICATION:
                return jobStatusWatcher
                    .touchApplicationJob(trackId)
                    .map(e -> nonLost.test(e.jobState()))
                    .orElse(false);
            default:
                return false;
        }
    }

    @Override
    public void postEvent(BuildInEvent event, boolean sync) {
        if (sync) {
            eventBus.postSync(event);
        } else {
            eventBus.postAsync(event);
        }
    }

    @Override
    @Nullable
    public String getRemoteRestUrl(TrackId trackId) {
        return watchController.endpoints.get(trackId.toClusterKey());
    }

    /** Build-in Event Listener of K8sFlinkTrackMonitor. */
    private class BuildInEventListener {

        @Subscribe
        @AllowConcurrentEvents
        public void subscribeFlinkJobStateEvent(FlinkJobStateEvent event) {
            if (!event.trackId().isLegal()) {
                return;
            }
            JobStatusCV latest = watchController.jobStatuses.get(event.trackId());
            boolean shouldIgnore;
            if (latest == null) {
                shouldIgnore = false;
            } else if (latest.jobState() == event.jobState()) {
                shouldIgnore = true;
            } else if (event.pollTime() <= latest.pollAckTime()) {
                shouldIgnore = true;
            } else {
                shouldIgnore = false;
            }
            if (!shouldIgnore) {
                JobStatusCV newCache;
                if (latest != null) {
                    newCache = latest.toBuilder().jobState(event.jobState()).build();
                } else {
                    newCache =
                        JobStatusCV.builder()
                            .jobState(event.jobState())
                            .jobId(event.trackId().jobId())
                            .pollEmitTime(event.pollTime())
                            .pollAckTime(System.currentTimeMillis())
                            .build();
                }
                watchController.jobStatuses.put(event.trackId(), newCache);
                eventBus.postAsync(new FlinkJobStatusChangeEvent(event.trackId(), newCache));
            }
        }
    }
}
