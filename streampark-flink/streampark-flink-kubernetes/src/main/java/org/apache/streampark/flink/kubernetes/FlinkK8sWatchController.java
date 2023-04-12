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

import org.apache.streampark.flink.kubernetes.cache.EndpointCache;
import org.apache.streampark.flink.kubernetes.cache.JobStatusCache;
import org.apache.streampark.flink.kubernetes.cache.K8sDeploymentEventCache;
import org.apache.streampark.flink.kubernetes.cache.MetricCache;
import org.apache.streampark.flink.kubernetes.cache.TrackIdCache;
import org.apache.streampark.flink.kubernetes.model.ClusterKey;
import org.apache.streampark.flink.kubernetes.model.FlinkMetricCV;
import org.apache.streampark.flink.kubernetes.model.TrackId;

import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class FlinkK8sWatchController implements AutoCloseable {

  private final TrackIdCache trackIdCache = TrackIdCache.getInstance();

  private final TrackIdCache cancellingCache = TrackIdCache.getInstance();

  private final EndpointCache endpointCache = EndpointCache.getInstance();

  private final JobStatusCache jobStatusCache = JobStatusCache.getInstance();

  private final K8sDeploymentEventCache k8sDeploymentEventCache =
      K8sDeploymentEventCache.getInstance();

  private final MetricCache metricCache = MetricCache.getInstance();

  private FlinkK8sWatchController() {}

  public static FlinkK8sWatchController getInstance() {
    return new FlinkK8sWatchController();
  }

  @Override
  public void close() throws Exception {
    jobStatusCache.cleanUp();
    k8sDeploymentEventCache.cleanUp();
    trackIdCache.cleanUp();
  }

  public Set<TrackId> getAllWatchingIds() {
    return trackIdCache.getAll();
  }

  public boolean isInWatching(TrackId trackId) {
    if (!trackId.isLegal()) {
      return false;
    }
    return Objects.nonNull(trackIdCache.get(trackId));
  }

  public void unWatching(TrackId trackId) {
    if (trackId.isLegal()) {
      trackIdCache.invalidate(trackId);
      cancellingCache.invalidate(trackId);
      jobStatusCache.invalidate(trackId);
      metricCache.invalidate(ClusterKey.of(trackId));
    }
  }

  public Set<TrackId> getActiveWatchingIds() {
    return getAllWatchingIds().stream().filter(TrackId::isActive).collect(Collectors.toSet());
  }

  public FlinkMetricCV collectAccGroupMetric(String groupId) {
    FlinkMetricCV empty = FlinkMetricCV.empty(groupId);
    Set<TrackId> allWatchingIds = getAllWatchingIds();
    if (allWatchingIds.isEmpty()) {
      return empty;
    }
    Set<ClusterKey> clusterKeys =
        allWatchingIds.stream().map(ClusterKey::of).collect(Collectors.toSet());
    Map<ClusterKey, FlinkMetricCV> kvs = metricCache.getAll(clusterKeys);
    if (kvs.isEmpty()) {
      return empty;
    }
    return kvs.values().stream().reduce(empty, FlinkMetricCV::add);
  }

  public Optional<String> getClusterRestUrl(ClusterKey clusterKey) {
    String result = endpointCache.get(clusterKey);
    if (StringUtils.isNotBlank(result)) {
      return Optional.of(result);
    }
    return refreshClusterRestUrl(clusterKey);
  }

  public Optional<String> refreshClusterRestUrl(ClusterKey clusterKey) {
    Optional<String> optionalRestUrl = KubernetesRetriever.retrieveFlinkRestUrl(clusterKey);
    optionalRestUrl.ifPresent(restUrl -> endpointCache.put(clusterKey, restUrl));
    return optionalRestUrl;
  }

  public TrackIdCache getTrackIdCache() {
    return trackIdCache;
  }

  public TrackIdCache getCancellingCache() {
    return cancellingCache;
  }

  public EndpointCache getEndpointCache() {
    return endpointCache;
  }

  public JobStatusCache getJobStatusCache() {
    return jobStatusCache;
  }

  public K8sDeploymentEventCache getK8sDeploymentEventCache() {
    return k8sDeploymentEventCache;
  }

  public MetricCache getMetricCache() {
    return metricCache;
  }
}
