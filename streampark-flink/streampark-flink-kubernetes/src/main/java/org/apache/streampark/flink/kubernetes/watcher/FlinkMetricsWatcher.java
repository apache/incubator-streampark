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
import org.apache.streampark.flink.kubernetes.TrackConfig;
import org.apache.streampark.flink.kubernetes.event.FlinkClusterMetricChangeEvent;
import org.apache.streampark.flink.kubernetes.model.ClusterKey;
import org.apache.streampark.flink.kubernetes.model.FlinkMetricCV;
import org.apache.streampark.flink.kubernetes.model.TrackId;

import org.apache.flink.configuration.JobManagerOptions;
import org.apache.flink.configuration.MemorySize;
import org.apache.flink.configuration.TaskManagerOptions;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.util.Timeout;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

public class FlinkMetricsWatcher implements FlinkWatcher {

  private static final Logger LOG = LoggerFactory.getLogger(FlinkMetricsWatcher.class);

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final TrackConfig.MetricWatcherConfig conf;

  private final FlinkK8sWatchController watchController;

  private final ChangeEventBus eventBus;

  private final ExecutorService trackTaskExecPool = Executors.newWorkStealingPool();

  private final ScheduledExecutorService timerExec = Executors.newSingleThreadScheduledExecutor();

  private ScheduledFuture<?> timerSchedule;

  public FlinkMetricsWatcher(
      TrackConfig.MetricWatcherConfig conf,
      FlinkK8sWatchController watchController,
      ChangeEventBus eventBus) {
    this.conf = conf;
    this.watchController = watchController;
    this.eventBus = eventBus;
  }

  @Override
  public void doStart() {
    timerSchedule =
        timerExec.scheduleAtFixedRate(
            () -> doWatch(), 0, conf.requestIntervalSec, TimeUnit.SECONDS);
    LOG.info("Flink metrics watcher started.");
  }

  @Override
  public void doStop() {
    timerSchedule.cancel(true);
    LOG.info("Flink metrics watch stopped.");
  }

  @Override
  public void doClose() {
    timerExec.shutdownNow();
    trackTaskExecPool.shutdownNow();
    LOG.info("Flink metrics watcher closed.");
  }

  @Override
  public void doWatch() {
    Set<TrackId> allWatchingIds = watchController.getAllWatchingIds();
    if (allWatchingIds.isEmpty()) {
      return;
    }
    List<CompletableFuture<Optional<FlinkMetricCV>>> futures =
        allWatchingIds.stream()
            .map(
                trackId -> {
                  CompletableFuture<Optional<FlinkMetricCV>> future =
                      CompletableFuture.supplyAsync(() -> collectMetrics(trackId));
                  future.thenAccept(
                      optionalFlinkMetricCV -> {
                        optionalFlinkMetricCV.ifPresent(
                            flinkMetricCV -> {
                              ClusterKey clusterKey = trackId.toClusterKey();
                              watchController.getMetricCache().put(clusterKey, flinkMetricCV);
                              FlinkMetricCV preMetric =
                                  watchController.getMetricCache().get(clusterKey);
                              if (preMetric == null || !preMetric.equals(flinkMetricCV)) {
                                eventBus.postAsync(
                                    new FlinkClusterMetricChangeEvent(trackId, flinkMetricCV));
                              }
                            });
                      });
                  return future;
                })
            .collect(Collectors.toList());
    CompletableFuture<Void> allFutures =
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    try {
      allFutures.get(conf.requestTimeoutSec, TimeUnit.SECONDS);
    } catch (InterruptedException | ExecutionException e) {
      LOG.warn("Future interrupted");
    } catch (TimeoutException e) {
      LOG.error(
          "[FlinkMetricStatusWatcher] tracking flink job status on kubernetes mode timeout, "
              + "limitSeconds = {}, trackingClusterKeys = {}",
          conf.requestTimeoutSec,
          allWatchingIds.stream().map(TrackId::getClusterId).collect(Collectors.joining(",")));
    }
  }

  private Optional<FlinkMetricCV> collectMetrics(TrackId id) {
    ClusterKey clusterKey = ClusterKey.of(id);
    Optional<String> clusterRestUrl = watchController.getClusterRestUrl(clusterKey);
    if (!clusterRestUrl.isPresent()) {
      return Optional.empty();
    }
    try {
      String overviewResponse =
          Request.get(String.format("%s/overview", clusterRestUrl))
              .connectTimeout(Timeout.ofSeconds(KubernetesRetriever.FLINK_REST_AWAIT_TIMEOUT_SEC))
              .responseTimeout(Timeout.ofSeconds(KubernetesRetriever.FLINK_CLIENT_TIMEOUT_SEC))
              .execute()
              .returnContent()
              .asString(StandardCharsets.UTF_8);

      String flinkConfigResponse =
          Request.get(String.format("%s/jobmanager/config", clusterRestUrl))
              .connectTimeout(Timeout.ofSeconds(KubernetesRetriever.FLINK_REST_AWAIT_TIMEOUT_SEC))
              .responseTimeout(Timeout.ofSeconds(KubernetesRetriever.FLINK_CLIENT_TIMEOUT_SEC))
              .execute()
              .returnContent()
              .asString(StandardCharsets.UTF_8);

      FlinkRestOverview flinkRestOverview = FlinkRestOverview.of(overviewResponse);
      List<FlinkRestJmConfigItem> flinkRestJmConfigItems =
          FlinkRestJmConfigItem.of(flinkConfigResponse);
      Map<String, String> configMap = new HashMap<>();
      flinkRestJmConfigItems.forEach(config -> configMap.put(config.getKey(), config.getValue()));
      long ackTime = System.currentTimeMillis();
      MemorySize temMemStr =
          MemorySize.parse(
              configMap.getOrDefault(TaskManagerOptions.TOTAL_PROCESS_MEMORY.key(), "0b"));
      MemorySize jobMemStr =
          MemorySize.parse(
              configMap.getOrDefault(JobManagerOptions.TOTAL_PROCESS_MEMORY.key(), "0b"));
      FlinkMetricCV flinkMetricCV =
          new FlinkMetricCV(
              id.getGroupId(),
              jobMemStr.getMebiBytes(),
              temMemStr.getMebiBytes(),
              flinkRestOverview.getTaskManagers(),
              flinkRestOverview.getSlotsTotal(),
              flinkRestOverview.getSlotsAvailable(),
              flinkRestOverview.getJobsRunning(),
              flinkRestOverview.getJobsFinished(),
              flinkRestOverview.getJobsCancelled(),
              flinkRestOverview.getJobsFailed(),
              ackTime);
      return Optional.of(flinkMetricCV);
    } catch (IOException e) {
      LOG.warn("Failed to get metrics from job managers", e);
      return Optional.empty();
    }
  }

  public static class FlinkRestOverview {
    private final int taskManagers;

    private final int slotsTotal;

    private final int slotsAvailable;

    private final int jobsRunning;

    private final int jobsFinished;

    private final int jobsCancelled;

    private final int jobsFailed;

    private final String flinkVersion;

    public FlinkRestOverview(
        int taskManagers,
        int slotsTotal,
        int slotsAvailable,
        int jobsRunning,
        int jobsFinished,
        int jobsCancelled,
        int jobsFailed,
        String flinkVersion) {
      this.taskManagers = taskManagers;
      this.slotsTotal = slotsTotal;
      this.slotsAvailable = slotsAvailable;
      this.jobsRunning = jobsRunning;
      this.jobsFinished = jobsFinished;
      this.jobsCancelled = jobsCancelled;
      this.jobsFailed = jobsFailed;
      this.flinkVersion = flinkVersion;
    }

    public int getTaskManagers() {
      return taskManagers;
    }

    public int getSlotsTotal() {
      return slotsTotal;
    }

    public int getSlotsAvailable() {
      return slotsAvailable;
    }

    public int getJobsRunning() {
      return jobsRunning;
    }

    public int getJobsFinished() {
      return jobsFinished;
    }

    public int getJobsCancelled() {
      return jobsCancelled;
    }

    public int getJobsFailed() {
      return jobsFailed;
    }

    public String getFlinkVersion() {
      return flinkVersion;
    }

    public static FlinkRestOverview of(String json) throws JsonProcessingException {
      JsonNode jsonNode = OBJECT_MAPPER.readTree(json);
      int taskManagers = jsonNode.get("taskmanagers").asInt(0);
      int totalSlots = jsonNode.get("slots-total").asInt(0);
      int availableSlots = jsonNode.get("slots-available").asInt(0);
      int runningJobs = jsonNode.get("jobs-running").asInt(0);
      int finishedJobs = jsonNode.get("jobs-finished").asInt(0);
      int cancelledJobs = jsonNode.get("jobs-cancelled").asInt(0);
      int failedJobs = jsonNode.get("jobs-failed").asInt(0);
      String flinkVersion = jsonNode.get("flink-version").asText(null);
      return new FlinkRestOverview(
          taskManagers,
          totalSlots,
          availableSlots,
          runningJobs,
          finishedJobs,
          cancelledJobs,
          failedJobs,
          flinkVersion);
    }
  }

  public static class FlinkRestJmConfigItem {
    private final String key;
    private final String value;

    public FlinkRestJmConfigItem(String key, String value) {
      this.key = key;
      this.value = value;
    }

    public String getKey() {
      return key;
    }

    public String getValue() {
      return value;
    }

    public static List<FlinkRestJmConfigItem> of(String json) throws JsonProcessingException {
      JsonNode jsonNode = OBJECT_MAPPER.readTree(json);
      List<FlinkRestJmConfigItem> flinkRestJmConfigItems = new ArrayList<>();
      jsonNode.forEach(
          node -> {
            String key = node.get("key").asText(null);
            String value = node.get("value").asText(null);
            flinkRestJmConfigItems.add(new FlinkRestJmConfigItem(key, value));
          });
      return flinkRestJmConfigItems;
    }
  }
}
