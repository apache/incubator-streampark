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

import org.apache.streampark.flink.kubernetes.TrackConfig.FlinkTrackConfig;
import org.apache.streampark.flink.kubernetes.cache.CacheKey;
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
import org.apache.streampark.flink.kubernetes.watcher.FlinkMetricsWatcher;

import com.google.common.eventbus.Subscribe;

import javax.annotation.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public class DefaultFlinkK8sWatcher implements FlinkK8sWatcher {
  private FlinkTrackConfig conf = FlinkTrackConfig.defaultConf();

  private final FlinkK8sWatchController watchController = FlinkK8sWatchController.getInstance();

  private final ChangeEventBus eventBus = new ChangeEventBus();

  {
    eventBus.registerListener(new BuildInEventListener());
  }

  private final FlinkK8sEventWatcher k8sEventWatcher = new FlinkK8sEventWatcher(watchController);

  private final FlinkJobStatusWatcher jobStatusWatcher =
      new FlinkJobStatusWatcher(conf.jobStatusWatcherConf, watchController, eventBus);

  private final FlinkMetricsWatcher metricsWatcher =
      new FlinkMetricsWatcher(conf.metricWatcherConf, watchController, eventBus);

  private final FlinkCheckpointWatcher checkpointWatcher =
      new FlinkCheckpointWatcher(conf.metricWatcherConf, watchController, eventBus);

  public DefaultFlinkK8sWatcher(FlinkTrackConfig conf) {
    this.conf = conf;
    eventBus.registerListener(new BuildInEventListener());
  }

  @Override
  public void registerListener(Object listener) {
    eventBus.registerListener(listener);
  }

  @Override
  public void start() {
    k8sEventWatcher.start();
    jobStatusWatcher.start();
    metricsWatcher.start();
    checkpointWatcher.start();
  }

  @Override
  public void stop() {
    k8sEventWatcher.stop();
    jobStatusWatcher.stop();
    metricsWatcher.stop();
    checkpointWatcher.stop();
  }

  @Override
  public void restart() {
    k8sEventWatcher.restart();
    jobStatusWatcher.restart();
    metricsWatcher.restart();
    checkpointWatcher.restart();
  }

  @Override
  public void close() throws Exception {
    k8sEventWatcher.close();
    jobStatusWatcher.close();
    metricsWatcher.close();
    checkpointWatcher.close();
    watchController.close();
  }

  @Override
  public void doWatching(TrackId trackId) {
    if (trackId.isLegal()) {
      watchController.getTrackIdCache().set(trackId);
    }
  }

  @Override
  public void unWatching(TrackId trackId) {
    if (trackId.isLegal()) {
      watchController.getCancellingCache().set(trackId);
    }
  }

  @Override
  public boolean isInWatching(TrackId trackId) {
    return watchController.isInWatching(trackId);
  }

  @Override
  public Set<TrackId> getAllWatchingIds() {
    return watchController.getAllWatchingIds();
  }

  @Nullable
  @Override
  public Optional<JobStatusCV> getJobStatus(TrackId trackId) {
    return Optional.of(watchController.getJobStatusCache().get(trackId));
  }

  @Override
  public Map<CacheKey, JobStatusCV> getJobStatus(Set<TrackId> trackIds) {
    return watchController.getJobStatusCache().getAsMap(trackIds);
  }

  @Override
  public Map<CacheKey, JobStatusCV> getAllJobStatus() {
    return watchController.getJobStatusCache().asMap();
  }

  @Override
  public FlinkMetricCV getAccGroupMetrics(@Nullable String groupId) {
    return watchController.collectAccGroupMetric(groupId);
  }

  @Nullable
  @Override
  public Optional<FlinkMetricCV> getClusterMetrics(ClusterKey clusterKey) {
    return Optional.of(watchController.getMetricCache().get(clusterKey));
  }

  @Override
  public boolean checkIsInRemoteCluster(TrackId trackId) {
    Predicate<JobStatusCV> predicate =
        statusCV ->
            statusCV.getJobState() != FlinkJobState.LOST
                || statusCV.getJobState() != FlinkJobState.SILENT;
    if (!trackId.isLegal()) {
      return false;
    }
    switch (trackId.getExecuteMode()) {
      case SESSION:
        return jobStatusWatcher.touchSessionJob(trackId).filter(predicate).isPresent();
      case APPLICATION:
        return jobStatusWatcher.touchApplicationJob(trackId).filter(predicate).isPresent();
      default:
        return false;
    }
  }

  @Nullable
  @Override
  public String getRemoteRestUrl(TrackId trackId) {
    return watchController.getEndpointCache().get(trackId.toClusterKey());
  }

  @Override
  public void postEvent(BuildInEvent event, boolean sync) {
    if (sync) {
      eventBus.postSync(event);
    } else {
      eventBus.postAsync(event);
    }
  }

  public FlinkTrackConfig getConf() {
    return conf;
  }

  public void setConf(FlinkTrackConfig conf) {
    this.conf = conf;
  }

  public class BuildInEventListener {

    /**
     * Watch the FlinkJobOperaEvent, then update relevant cache record and trigger a new
     * FlinkJobStatusChangeEvent.
     */
    @Subscribe
    public void subscribeFlinkJobStateEvent(FlinkJobStateEvent event) {
      if (event.getTrackId().isLegal()) {
        JobStatusCV latest = watchController.getJobStatusCache().get(event.getTrackId());
        // determine if the current event should be ignored
        boolean shouldIgnore = false;
        if (latest != null) {
          if (latest.getJobState() == event.getJobState()) {
            shouldIgnore = true;
          } else if (event.getPollTime() <= latest.getPollAckTime()) {
            shouldIgnore = true;
          }
        }
        if (!shouldIgnore) {
          // update relevant cache
          JobStatusCV newCache = null;
          if (latest != null) {
            newCache = latest.copy(event.getJobState());
          } else {
            newCache =
                new JobStatusCV(
                    event.getJobState(),
                    event.getTrackId().getJobId(),
                    event.getPollTime(),
                    System.currentTimeMillis());
          }
          watchController.getJobStatusCache().put(event.getTrackId(), newCache);
          // post new FlinkJobStatusChangeEvent
          eventBus.postAsync(new FlinkJobStatusChangeEvent(event.getTrackId(), newCache));
        }
      }
    }
  }
}
