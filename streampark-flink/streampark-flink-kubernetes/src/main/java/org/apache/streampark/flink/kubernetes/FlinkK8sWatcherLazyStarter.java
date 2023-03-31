/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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

public class FlinkK8sWatcherLazyStarter extends DefaultFlinkK8sWatcher {

  public FlinkK8sWatcherLazyStarter(TrackConfig.FlinkTrackConfig conf) {
    super(conf);
  }

  @Override
  public void doWatching(TrackId trackId) {
    start();
    super.doWatching(trackId);
  }

  @Override
  public void unWatching(TrackId trackId) {
    start();
    super.unWatching(trackId);
  }

  @Override
  public boolean isInWatching(TrackId trackId) {
    start();
    return super.isInWatching(trackId);
  }

  @Override
  public Set<TrackId> getAllWatchingIds() {
    start();
    return super.getAllWatchingIds();
  }

  @Nullable
  @Override
  public Optional<JobStatusCV> getJobStatus(TrackId trackId) {
    start();
    return super.getJobStatus(trackId);
  }

  @Override
  public Map<CacheKey, JobStatusCV> getJobStatus(Set<TrackId> trackIds) {
    start();
    return super.getJobStatus(trackIds);
  }

  @Override
  public Map<CacheKey, JobStatusCV> getAllJobStatus() {
    start();
    return super.getAllJobStatus();
  }

  @Override
  public FlinkMetricCV getAccGroupMetrics(@Nullable String groupId) {
    start();
    return super.getAccGroupMetrics(groupId);
  }

  @Nullable
  @Override
  public Optional<FlinkMetricCV> getClusterMetrics(ClusterKey clusterKey) {
    start();
    return super.getClusterMetrics(clusterKey);
  }

  @Override
  public boolean checkIsInRemoteCluster(TrackId trackId) {
    start();
    return super.checkIsInRemoteCluster(trackId);
  }

  @Override
  public void postEvent(BuildInEvent event, boolean sync) {
    start();
    super.postEvent(event, sync);
  }
}
