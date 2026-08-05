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
import org.apache.streampark.flink.kubernetes.event.FlinkJobCheckpointChangeEvent;
import org.apache.streampark.flink.kubernetes.model.CheckpointCV;
import org.apache.streampark.flink.kubernetes.model.ClusterKey;
import org.apache.streampark.flink.kubernetes.model.TrackId;
import org.apache.streampark.flink.kubernetes.rest.CheckpointInfo;
import org.apache.streampark.flink.kubernetes.rest.FlinkCheckpointResponse;

import org.apache.hc.client5.http.fluent.Request;

import javax.annotation.concurrent.ThreadSafe;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@ThreadSafe
public class FlinkCheckpointWatcher extends FlinkWatcher {

    private final MetricWatcherConfig conf;
    private final FlinkK8sWatchController watchController;
    private final ChangeEventBus eventBus;

    private ScheduledFuture<?> timerSchedule;

    public FlinkCheckpointWatcher(
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
        logInfo("[flink-k8s] FlinkCheckpointWatcher started.");
    }

    @Override
    protected void doStop() {
        if (timerSchedule != null && !timerSchedule.isCancelled()) {
            timerSchedule.cancel(true);
        }
        logInfo("[flink-k8s] FlinkCheckpointWatcher stopped.");
    }

    @Override
    protected void doClose() {
        if (timerSchedule != null && !timerSchedule.isCancelled()) {
            timerSchedule.cancel(true);
        }
        logInfo("[flink-k8s] FlinkCheckpointWatcher closed.");
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

        Set<CompletableFuture<Optional<CheckpointCV>>> futures =
            trackIds.stream()
                .map(
                    id -> CompletableFuture
                        .supplyAsync(() -> collect(id), watchExecutor)
                        .whenComplete(
                            (cpOpt, error) -> {
                                if (error == null) {
                                    cpOpt.ifPresent(
                                        cp -> eventBus.postAsync(
                                            new FlinkJobCheckpointChangeEvent(id, cp)));
                                }
                            }))
                .collect(Collectors.toSet());

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .get(conf.requestTimeoutSec(), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logError(
                "[FlinkCheckpointWatcher] tracking flink-job checkpoint on kubernetes mode interrupted,"
                    + " limitSeconds="
                    + conf.requestTimeoutSec()
                    + ", trackingClusterKeys="
                    + trackIds.stream().map(Object::toString).collect(Collectors.joining(",")));
        } catch (ExecutionException | TimeoutException e) {
            logError(
                "[FlinkCheckpointWatcher] tracking flink-job checkpoint on kubernetes mode timeout,"
                    + " limitSeconds="
                    + conf.requestTimeoutSec()
                    + ", trackingClusterKeys="
                    + trackIds.stream().map(Object::toString).collect(Collectors.joining(",")));
        }
    }

    public Optional<CheckpointCV> collect(TrackId trackId) {
        if (trackId.jobId() == null) {
            return Optional.empty();
        }
        Optional<String> flinkJmRestUrl =
            watchController
                .getClusterRestUrl(ClusterKey.of(trackId))
                .filter(url -> !url.isEmpty());
        if (!flinkJmRestUrl.isPresent()) {
            return Optional.empty();
        }
        try {
            String json =
                Request.get(flinkJmRestUrl.get() + "/jobs/" + trackId.jobId() + "/checkpoints")
                    .connectTimeout(KubernetesRetriever.FLINK_REST_AWAIT_TIMEOUT_SEC)
                    .responseTimeout(KubernetesRetriever.FLINK_CLIENT_TIMEOUT_SEC)
                    .execute()
                    .returnContent()
                    .asString(StandardCharsets.UTF_8);
            Optional<CheckpointInfo> checkpoint = FlinkCheckpointResponse.parseCompleted(json);
            if (!checkpoint.isPresent()) {
                return Optional.empty();
            }
            CheckpointInfo cp = checkpoint.get();
            return Optional.of(
                CheckpointCV.builder()
                    .id(cp.id())
                    .externalPath(cp.externalPath())
                    .isSavepoint(cp.isSavepoint())
                    .checkpointType(cp.checkpointType())
                    .status(cp.status())
                    .triggerTimestamp(cp.triggerTimestamp())
                    .build());
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
