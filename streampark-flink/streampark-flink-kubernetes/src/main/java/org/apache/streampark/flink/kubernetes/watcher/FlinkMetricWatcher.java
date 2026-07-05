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
import org.apache.streampark.flink.kubernetes.watcher.FlinkRestModels.FlinkRestJmConfigItem;
import org.apache.streampark.flink.kubernetes.watcher.FlinkRestModels.FlinkRestOverview;

import org.apache.flink.configuration.JobManagerOptions;
import org.apache.flink.configuration.MemorySize;
import org.apache.flink.configuration.TaskManagerOptions;
import org.apache.hc.client5.http.fluent.Request;

import lombok.extern.slf4j.Slf4j;

import javax.annotation.concurrent.ThreadSafe;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
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
        log.info("[flink-k8s] FlinkMetricWatcher started.");
    }

    @Override
    protected void doStop() {
        if (timerSchedule != null && !timerSchedule.isCancelled()) {
            timerSchedule.cancel(true);
        }
        log.info("[flink-k8s] FlinkMetricWatcher stopped.");
    }

    @Override
    protected void doClose() {
        if (timerSchedule != null && !timerSchedule.isCancelled()) {
            timerSchedule.cancel(true);
        }
        log.info("[flink-k8s] FlinkMetricWatcher closed.");
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
                                id ->
                                        CompletableFuture.supplyAsync(
                                                        () -> collectMetrics(id), watchExecutor)
                                                .whenComplete(
                                                        (metric, error) -> {
                                                            if (metric == null || !metric.isPresent()) {
                                                                return;
                                                            }
                                                            ClusterKey clusterKey = id.toClusterKey();
                                                            FlinkMetricCV preMetric =
                                                                    watchController.flinkMetrics.get(
                                                                            clusterKey);
                                                            boolean isMetricChanged =
                                                                    preMetric == null
                                                                            || !preMetric.equalsPayload(
                                                                                    metric.get());
                                                            if (isMetricChanged) {
                                                                eventBus.postAsync(
                                                                        new FlinkClusterMetricChangeEvent(
                                                                                id, metric.get()));
                                                                watchController.flinkMetrics.put(
                                                                        clusterKey, metric.get());
                                                            }
                                                        }))
                        .collect(Collectors.toSet());

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(conf.requestTimeoutSec(), TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error(
                    "[FlinkMetricWatcher] tracking flink metrics on kubernetes mode timeout, limitSeconds={}, trackingClusterKeys={}",
                    conf.requestTimeoutSec(),
                    trackIds);
        }
    }

    private Optional<FlinkMetricCV> collectMetrics(TrackId id) {
        ClusterKey clusterKey = ClusterKey.of(id);
        Optional<String> flinkJmRestUrl =
                watchController.getClusterRestUrl(clusterKey).filter(url -> !url.isEmpty());
        if (!flinkJmRestUrl.isPresent()) {
            return Optional.empty();
        }
        try {
            String overviewJson =
                    Request.get(flinkJmRestUrl.get() + "/overview")
                            .connectTimeout(KubernetesRetriever.FLINK_REST_AWAIT_TIMEOUT_SEC)
                            .responseTimeout(KubernetesRetriever.FLINK_CLIENT_TIMEOUT_SEC)
                            .execute()
                            .returnContent()
                            .asString(StandardCharsets.UTF_8);
            Optional<FlinkRestOverview> flinkOverview = FlinkRestModels.parseOverview(overviewJson);
            if (!flinkOverview.isPresent()) {
                return Optional.empty();
            }

            String configJson =
                    Request.get(flinkJmRestUrl.get() + "/jobmanager/config")
                            .connectTimeout(KubernetesRetriever.FLINK_REST_AWAIT_TIMEOUT_SEC)
                            .responseTimeout(KubernetesRetriever.FLINK_CLIENT_TIMEOUT_SEC)
                            .execute()
                            .returnContent()
                            .asString(StandardCharsets.UTF_8);
            List<FlinkRestJmConfigItem> configItems = FlinkRestModels.parseJmConfig(configJson);
            if (configItems == null) {
                return Optional.empty();
            }
            Map<String, String> flinkJmConfigs = new HashMap<>();
            for (FlinkRestJmConfigItem item : configItems) {
                flinkJmConfigs.put(item.key(), item.value());
            }

            long ackTime = System.currentTimeMillis();
            String tmMemStr =
                    flinkJmConfigs.getOrDefault(TaskManagerOptions.TOTAL_PROCESS_MEMORY.key(), "0b");
            String jmMemStr =
                    flinkJmConfigs.getOrDefault(JobManagerOptions.TOTAL_PROCESS_MEMORY.key(), "0b");
            FlinkMetricCV flinkMetricCV =
                    new FlinkMetricCV(
                            id.groupId(),
                            (int) MemorySize.parse(jmMemStr).getMebiBytes(),
                            (int)
                                    (MemorySize.parse(tmMemStr).getMebiBytes()
                                            * flinkOverview.get().taskManagers()),
                            flinkOverview.get().taskManagers(),
                            flinkOverview.get().slotsTotal(),
                            flinkOverview.get().slotsAvailable(),
                            flinkOverview.get().jobsRunning(),
                            flinkOverview.get().jobsFinished(),
                            flinkOverview.get().jobsCancelled(),
                            flinkOverview.get().jobsFailed(),
                            ackTime);
            return Optional.of(flinkMetricCV);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
