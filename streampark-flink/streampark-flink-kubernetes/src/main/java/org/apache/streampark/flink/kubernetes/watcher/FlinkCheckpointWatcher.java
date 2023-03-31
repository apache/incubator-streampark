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
import org.apache.streampark.flink.kubernetes.TrackConfig.MetricWatcherConfig;
import org.apache.streampark.flink.kubernetes.event.FlinkJobCheckpointChangeEvent;
import org.apache.streampark.flink.kubernetes.model.CheckpointCV;
import org.apache.streampark.flink.kubernetes.model.ClusterKey;
import org.apache.streampark.flink.kubernetes.model.TrackId;

import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.util.Timeout;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
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

public class FlinkCheckpointWatcher extends FlinkWatcher {

  private static final Logger LOG = LoggerFactory.getLogger(FlinkCheckpointWatcher.class);

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final MetricWatcherConfig conf;

  private final FlinkK8sWatchController watchController;

  private final ChangeEventBus eventBus;

  private final ExecutorService trackTaskExecPool = Executors.newWorkStealingPool();

  private final ScheduledExecutorService timerExec = Executors.newSingleThreadScheduledExecutor();

  private ScheduledFuture<?> timerSchedule;

  public FlinkCheckpointWatcher(
      MetricWatcherConfig conf, FlinkK8sWatchController watchController, ChangeEventBus eventBus) {
    this.conf = conf;
    this.watchController = watchController;
    this.eventBus = eventBus;
  }

  public MetricWatcherConfig getConf() {
    return conf;
  }

  public FlinkK8sWatchController getWatchController() {
    return watchController;
  }

  public ChangeEventBus getEventBus() {
    return eventBus;
  }

  @Override
  public void doStart() {
    timerSchedule =
        timerExec.scheduleAtFixedRate(this::doWatch, 0, conf.requestIntervalSec, TimeUnit.SECONDS);
    LOG.info("[flink-k8s] FlinkCheckpointWatcher started.");
  }

  @Override
  public void doStop() {
    timerSchedule.cancel(true);
    LOG.info("[flink-k8s] FlinkCheckpointWatcher stopped.");
  }

  @Override
  public void doClose() {
    timerExec.shutdownNow();
    trackTaskExecPool.shutdownNow();
    LOG.info("[flink-k8s] FlinkCheckpointWatcher closed.");
  }

  @Override
  public void doWatch() {
    Set<TrackId> activeWatchingIds = watchController.getActiveWatchingIds();
    List<CompletableFuture<Optional<CheckpointCV>>> futures =
        activeWatchingIds.stream()
            .filter(Objects::nonNull)
            .map(
                trackId -> {
                  CompletableFuture<Optional<CheckpointCV>> future =
                      CompletableFuture.supplyAsync(() -> collect(trackId));
                  future.thenAccept(
                      optionalCheckpointCV ->
                          optionalCheckpointCV.ifPresent(
                              cp ->
                                  eventBus.postAsync(
                                      new FlinkJobCheckpointChangeEvent(trackId, cp))));
                  return future;
                })
            .collect(Collectors.toList());
    CompletableFuture<Void> allFutures =
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    try {
      allFutures.get(conf.requestTimeoutSec, TimeUnit.SECONDS);
    } catch (InterruptedException | ExecutionException e) {
      e.printStackTrace();
    } catch (TimeoutException e) {
      LOG.error(
          "[FlinkCheckpointWatcher] tracking flink-job checkpoint on kubernetes mode timeout, "
              + "limitSeconds = {}, trackingClusterKeys = {}",
          conf.requestTimeoutSec,
          activeWatchingIds.stream().map(TrackId::getClusterId).collect(Collectors.joining(",")));
    }
  }

  public Optional<CheckpointCV> collect(TrackId trackId) {
    if (StringUtils.isBlank(trackId.getJobId())) {
      return Optional.empty();
    }
    Optional<String> clusterRestUrl = watchController.getClusterRestUrl(ClusterKey.of(trackId));
    if (!clusterRestUrl.isPresent()) {
      return Optional.empty();
    }
    String requestUrl =
        String.format("%s/jobs/%s/checkpoints", clusterRestUrl.get(), trackId.getJobId());
    try {
      String response =
          Request.get(requestUrl)
              .connectTimeout(Timeout.ofSeconds(KubernetesRetriever.FLINK_REST_AWAIT_TIMEOUT_SEC))
              .responseTimeout(Timeout.ofSeconds(KubernetesRetriever.FLINK_CLIENT_TIMEOUT_SEC))
              .execute()
              .returnContent()
              .asString(StandardCharsets.UTF_8);
      JsonNode root = OBJECT_MAPPER.readTree(response);
      JsonNode completed = root.get("latest").get("completed");
      if (completed.isNull()) {
        return Optional.empty();
      }
      long id = completed.get("id").asLong(0L);
      String status = completed.get("status").asText(null);
      String externalPath = completed.get("external_path").asText(null);
      boolean isSavepoint = completed.get("is_savepoint").asBoolean(false);
      String checkpointType = completed.get("checkpoint_type").asText(null);
      long triggerTimestamp = completed.get("trigger_timestamp").asLong(0L);
      return Optional.of(
          new CheckpointCV(
              id, status, externalPath, isSavepoint, checkpointType, triggerTimestamp));
    } catch (IOException e) {
      LOG.warn(
          "Failed to request [{}], get checkpoint information failed, {}",
          requestUrl,
          e.getMessage());
      return Optional.empty();
    }
  }
}
