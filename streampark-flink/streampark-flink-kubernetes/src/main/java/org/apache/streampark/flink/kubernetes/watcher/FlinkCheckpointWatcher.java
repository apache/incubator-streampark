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
import org.apache.streampark.flink.kubernetes.watcher.FlinkRestModels.CheckpointResponse;

import org.apache.hc.client5.http.fluent.Request;

import lombok.extern.slf4j.Slf4j;

import javax.annotation.concurrent.ThreadSafe;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
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
        log.info("[flink-k8s] FlinkCheckpointWatcher started.");
    }

    @Override
    protected void doStop() {
        if (timerSchedule != null && !timerSchedule.isCancelled()) {
            timerSchedule.cancel(true);
        }
        log.info("[flink-k8s] FlinkCheckpointWatcher stopped.");
    }

    @Override
    protected void doClose() {
        if (timerSchedule != null && !timerSchedule.isCancelled()) {
            timerSchedule.cancel(true);
        }
        log.info("[flink-k8s] FlinkCheckpointWatcher closed.");
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
            trackIds.stream().map(this::watchCheckpointAsync).collect(Collectors.toSet());

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .get(conf.requestTimeoutSec(), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[FlinkCheckpointWatcher] interrupted while waiting for checkpoint collection");
        } catch (Exception e) {
            log.error(
                "[FlinkCheckpointWatcher] tracking flink-job checkpoint on kubernetes mode timeout, limitSeconds={}, trackingClusterKeys={}",
                conf.requestTimeoutSec(),
                trackIds);
        }
    }

    private CompletableFuture<Optional<CheckpointCV>> watchCheckpointAsync(TrackId id) {
        return CompletableFuture.supplyAsync(() -> collect(id), watchExecutor)
            .whenComplete(
                (cp, error) -> cp.ifPresent(
                    checkpoint -> eventBus.postAsync(
                        new FlinkJobCheckpointChangeEvent(
                            id, checkpoint))));
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
            Optional<CheckpointResponse> checkpoint = FlinkRestModels.parseCheckpoint(json);
            if (!checkpoint.isPresent()) {
                return Optional.empty();
            }
            CheckpointResponse cp = checkpoint.get();
            return Optional.of(
                new CheckpointCV(
                    cp.id(),
                    cp.status(),
                    cp.externalPath(),
                    cp.isSavepoint(),
                    cp.checkpointType(),
                    cp.triggerTimestamp()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
