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

import org.apache.streampark.flink.kubernetes.cache.CacheKey;
import org.apache.streampark.flink.kubernetes.event.BuildInEvent;
import org.apache.streampark.flink.kubernetes.model.ClusterKey;
import org.apache.streampark.flink.kubernetes.model.FlinkMetricCV;
import org.apache.streampark.flink.kubernetes.model.JobStatusCV;
import org.apache.streampark.flink.kubernetes.model.TrackId;

import javax.annotation.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface FlinkK8sWatcher extends AutoCloseable {

  /**
   * Register listener to EventBus.
   *
   * <p>At present, the implementation of listener is in the same form as Guava EvenBus Listener.
   * The events that can be subscribed are included in org.apache.streampark.flink.kubernetes.event
   */
  void registerListener(Object listener);

  /** start monitor tracking activities immediately. */
  void start();

  /** stop monitor tracking activities immediately. */
  void stop();

  /** restart monitor tracking activities immediately. */
  void restart();

  /**
   * add tracking for the specified flink job which on k8s cluster.
   *
   * @param trackId identifier of flink job
   */
  void doWatching(TrackId trackId);

  /**
   * remove tracking for the specified flink job which on k8s cluster.
   *
   * @param trackId identifier of flink job
   */
  void unWatching(TrackId trackId);

  /**
   * check whether the specified flink job is in tracking.
   *
   * @param trackId identifier of flink job
   */
  boolean isInWatching(TrackId trackId);

  /** collect all TrackId which in tracking */
  Set<TrackId> getAllWatchingIds();

  /** get flink status */
  @Nullable
  Optional<JobStatusCV> getJobStatus(TrackId trackId);

  /** get flink status */
  Map<CacheKey, JobStatusCV> getJobStatus(Set<TrackId> trackIds);

  /** get all flink status in tracking result pool */
  Map<CacheKey, JobStatusCV> getAllJobStatus();

  /** get flink cluster metrics aggregation */
  FlinkMetricCV getAccGroupMetrics(@Nullable String groupId);

  /** get flink cluster metrics */
  @Nullable
  Optional<FlinkMetricCV> getClusterMetrics(ClusterKey clusterKey);

  /** check whether flink job is in remote kubernetes cluster */
  boolean checkIsInRemoteCluster(TrackId trackId);

  /**
   * post event to build-in EventBus of K8sFlinkTrackMonitor
   *
   * @param sync should this event be consumed sync or async
   */
  void postEvent(BuildInEvent event, boolean sync);

  /** get flink web rest url of k8s cluster */
  @Nullable
  String getRemoteRestUrl(TrackId trackId);
}
