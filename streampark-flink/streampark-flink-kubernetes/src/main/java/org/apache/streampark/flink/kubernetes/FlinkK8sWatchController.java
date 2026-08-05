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

import org.apache.streampark.common.util.LoggerSupport;
import org.apache.streampark.flink.kubernetes.model.ClusterKey;
import org.apache.streampark.flink.kubernetes.model.FlinkMetricCV;
import org.apache.streampark.flink.kubernetes.model.TrackId;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Tracking info cache pool on flink kubernetes mode. */
public class FlinkK8sWatchController extends LoggerSupport implements AutoCloseable {

    public final TrackIdCache trackIds = TrackIdCache.build();
    public final TrackIdCache canceling = TrackIdCache.build();
    public final EndpointCache endpoints = EndpointCache.build();
    public final JobStatusCache jobStatuses;
    public final K8sDeploymentEventCache k8sDeploymentEvents = K8sDeploymentEventCache.build();
    public final MetricCache flinkMetrics = MetricCache.build();

    public FlinkK8sWatchController(JobStatusWatcherConfig conf) {
        this.jobStatuses = JobStatusCache.build(conf.jobStatusCacheTimeOutSec());
    }

    @Override
    public void close() {
        jobStatuses.cleanUp();
        k8sDeploymentEvents.cleanUp();
        trackIds.cleanUp();
    }

    public Set<TrackId> getAllWatchingIds() {
        return trackIds.getAll();
    }

    public boolean isInWatching(TrackId trackId) {
        if (!trackId.isLegal()) {
            return false;
        }
        return trackIds.get(trackId) != null;
    }

    public void unWatching(TrackId trackId) {
        if (trackId.isLegal()) {
            trackIds.invalidate(trackId);
            canceling.invalidate(trackId);
            jobStatuses.invalidate(trackId);
            flinkMetrics.invalidate(ClusterKey.of(trackId));
        }
    }

    public Set<TrackId> getActiveWatchingIds() {
        return getAllWatchingIds().stream().filter(TrackId::isActive).collect(Collectors.toSet());
    }

    public FlinkMetricCV collectAccGroupMetric(String groupId) {
        FlinkMetricCV empty = FlinkMetricCV.empty(groupId);
        Set<TrackId> activeIds =
            getActiveWatchingIds().stream()
                .filter(id -> groupId != null && groupId.equals(id.groupId()))
                .collect(Collectors.toSet());
        if (activeIds.isEmpty()) {
            return empty;
        }
        Set<ClusterKey> clusterKeys = new HashSet<>();
        for (TrackId trackId : activeIds) {
            clusterKeys.add(ClusterKey.of(trackId));
        }
        Map<ClusterKey, FlinkMetricCV> metrics = flinkMetrics.getAll(clusterKeys);
        if (metrics.isEmpty()) {
            return empty;
        }
        FlinkMetricCV result = empty;
        for (FlinkMetricCV metric : metrics.values()) {
            result = result.add(metric);
        }
        return result;
    }

    public Optional<String> getClusterRestUrl(ClusterKey clusterKey) {
        String cached = endpoints.get(clusterKey);
        if (cached != null && !cached.isEmpty()) {
            return Optional.of(cached);
        }
        return refreshClusterRestUrl(clusterKey);
    }

    public Optional<String> refreshClusterRestUrl(ClusterKey clusterKey) {
        Optional<String> restUrl = KubernetesRetriever.retrieveFlinkRestUrl(clusterKey);
        if (restUrl.isPresent() && !restUrl.get().isEmpty()) {
            endpoints.put(clusterKey, restUrl.get());
        }
        return restUrl;
    }
}
