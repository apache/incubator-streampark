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
import org.apache.streampark.flink.kubernetes.KubernetesRetriever;
import org.apache.streampark.flink.kubernetes.MetricWatcherConfig;
import org.apache.streampark.flink.kubernetes.event.FlinkClusterMetricChangeEvent;
import org.apache.streampark.flink.kubernetes.model.ClusterKey;
import org.apache.streampark.flink.kubernetes.model.FlinkMetricCV;
import org.apache.streampark.flink.kubernetes.model.TrackId;
import org.apache.streampark.flink.kubernetes.rest.FlinkJmConfigItem;
import org.apache.streampark.flink.kubernetes.rest.FlinkRestOverview;

import org.apache.flink.configuration.JobManagerOptions;
import org.apache.flink.configuration.MemorySize;
import org.apache.flink.configuration.TaskManagerOptions;
import org.apache.hc.client5.http.fluent.Request;

import javax.annotation.concurrent.ThreadSafe;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@ThreadSafe
public class FlinkMetricWatcher extends FlinkWatcher {

    private final MetricWatcherConfig conf;
    private final FlinkK8sWatchController watchController;
    private final ChangeEventBus eventBus;

    private ScheduledFuture<?> timerSchedule;

    public FlinkMetricWatcher(
                              MetricWatcherConfig conf,
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
        logInfo("[flink-k8s] FlinkMetricWatcher started.");
    }

    @Override
    protected void doStop() {
        if (timerSchedule != null && !timerSchedule.isCancelled()) {
            timerSchedule.cancel(true);
        }
        logInfo("[flink-k8s] FlinkMetricWatcher stopped.");
    }

    @Override
    protected void doClose() {
        if (timerSchedule != null && !timerSchedule.isCancelled()) {
            timerSchedule.cancel(true);
        }
        logInfo("[flink-k8s] FlinkMetricWatcher closed.");
    }

    @Override
    public void doWatch() {
        Set<TrackId> trackIds;
        try {
            trackIds = watchController.getActiveWatchingIds();
            if (trackIds.isEmpty()) {
                return;
            }
        } catch (Exception e) {
            return;
        }

        Set<CompletableFuture<Optional<FlinkMetricCV>>> futures =
            trackIds.stream()
                .map(
                    id -> CompletableFuture
                        .supplyAsync(() -> collectMetrics(id), watchExecutor)
                        .whenComplete(
                            (metricOpt, error) -> {
                                if (error == null) {
                                    metricOpt.ifPresent(
                                        metric -> {
                                            ClusterKey clusterKey = id.toClusterKey();
                                            FlinkMetricCV preMetric =
                                                watchController.flinkMetrics.get(clusterKey);
                                            boolean isMetricChanged =
                                                preMetric == null
                                                    || !preMetric.equalsPayload(metric);
                                            if (isMetricChanged) {
                                                eventBus.postAsync(
                                                    new FlinkClusterMetricChangeEvent(id, metric));
                                                watchController.flinkMetrics.put(
                                                    clusterKey, metric);
                                            }
                                        });
                                }
                            }))
                .collect(Collectors.toSet());

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .get(conf.requestTimeoutSec(), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logError(
                "[FlinkMetricWatcher] tracking flink metrics on kubernetes mode interrupted,"
                    + " limitSeconds="
                    + conf.requestTimeoutSec()
                    + ", trackingClusterKeys="
                    + trackIds.stream().map(Object::toString).collect(Collectors.joining(",")));
        } catch (ExecutionException | TimeoutException e) {
            logError(
                "[FlinkMetricWatcher] tracking flink metrics on kubernetes mode timeout,"
                    + " limitSeconds="
                    + conf.requestTimeoutSec()
                    + ", trackingClusterKeys="
                    + trackIds.stream().map(Object::toString).collect(Collectors.joining(",")));
        }
    }

    private Optional<FlinkMetricCV> collectMetrics(TrackId id) {
        ClusterKey clusterKey = ClusterKey.of(id);
        Optional<String> flinkJmRestUrl =
            watchController.getClusterRestUrl(clusterKey).filter(url -> !url.isEmpty());
        if (!flinkJmRestUrl.isPresent()) {
            return Optional.empty();
        }

        Optional<FlinkRestOverview> flinkOverview;
        try {
            flinkOverview =
                FlinkRestOverview.parse(request(flinkJmRestUrl.get() + "/overview"));
        } catch (Exception e) {
            return Optional.empty();
        }
        if (!flinkOverview.isPresent()) {
            return Optional.empty();
        }

        Map<String, String> flinkJmConfigs;
        try {
            flinkJmConfigs =
                FlinkJmConfigItem.parse(request(flinkJmRestUrl.get() + "/jobmanager/config"))
                    .stream()
                    .collect(
                        HashMap::new,
                        (map, item) -> map.put(item.key(), item.value()),
                        Map::putAll);
        } catch (Exception e) {
            return Optional.empty();
        }

        long ackTime = System.currentTimeMillis();
        FlinkRestOverview overview = flinkOverview.get();
        String tmMemStr =
            flinkJmConfigs.getOrDefault(TaskManagerOptions.TOTAL_PROCESS_MEMORY.key(), "0b");
        String jmMemStr =
            flinkJmConfigs.getOrDefault(JobManagerOptions.TOTAL_PROCESS_MEMORY.key(), "0b");

        FlinkMetricCV flinkMetricCV =
            FlinkMetricCV.builder()
                .groupId(id.groupId())
                .totalJmMemory((int) MemorySize.parse(jmMemStr).getMebiBytes())
                .totalTmMemory(
                    (int) MemorySize.parse(tmMemStr).getMebiBytes() * overview.taskManagers())
                .totalTm(overview.taskManagers())
                .totalSlot(overview.slotsTotal())
                .availableSlot(overview.slotsAvailable())
                .runningJob(overview.jobsRunning())
                .finishedJob(overview.jobsFinished())
                .cancelledJob(overview.jobsCancelled())
                .failedJob(overview.jobsFailed())
                .pollAckTime(ackTime)
                .build();
        return Optional.of(flinkMetricCV);
    }

    private String request(String url) throws Exception {
        return Request.get(url)
            .connectTimeout(KubernetesRetriever.FLINK_REST_AWAIT_TIMEOUT_SEC)
            .responseTimeout(KubernetesRetriever.FLINK_CLIENT_TIMEOUT_SEC)
            .execute()
            .returnContent()
            .asString(StandardCharsets.UTF_8);
    }
}
