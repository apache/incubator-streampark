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

import org.apache.streampark.flink.kubernetes.event.BuildInEvent;
import org.apache.streampark.flink.kubernetes.model.ClusterKey;
import org.apache.streampark.flink.kubernetes.model.FlinkMetricCV;
import org.apache.streampark.flink.kubernetes.model.JobStatusCV;
import org.apache.streampark.flink.kubernetes.model.TrackId;

import org.apache.flink.annotation.Public;

import javax.annotation.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Tracking monitor for flink-k8s-native mode. */
@Public
public interface FlinkK8sWatcher extends AutoCloseable {

    void registerListener(Object listener);

    void start();

    void stop();

    void restart();

    void doWatching(TrackId trackId);

    void unWatching(TrackId trackId);

    boolean isInWatching(TrackId trackId);

    Set<TrackId> getAllWatchingIds();

    Optional<JobStatusCV> getJobStatus(TrackId trackId);

    Map<CacheKey, JobStatusCV> getJobStatus(Set<TrackId> trackIds);

    Map<CacheKey, JobStatusCV> getAllJobStatus();

    FlinkMetricCV getAccGroupMetrics(@Nullable String groupId);

    Optional<FlinkMetricCV> getClusterMetrics(ClusterKey clusterKey);

    boolean checkIsInRemoteCluster(TrackId trackId);

    void postEvent(BuildInEvent event, boolean sync);

    @Nullable
    String getRemoteRestUrl(TrackId trackId);
}
