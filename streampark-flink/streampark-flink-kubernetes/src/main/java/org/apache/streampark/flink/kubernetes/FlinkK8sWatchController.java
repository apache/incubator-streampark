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

import org.apache.streampark.flink.kubernetes.model.ClusterKey;
import org.apache.streampark.flink.kubernetes.model.FlinkMetricCV;
import org.apache.streampark.flink.kubernetes.model.JobStatusCV;
import org.apache.streampark.flink.kubernetes.model.K8sDeploymentEventCV;
import org.apache.streampark.flink.kubernetes.model.K8sEventKey;
import org.apache.streampark.flink.kubernetes.model.TrackId;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/** Tracking info cache pool on flink kubernetes mode. */
@Slf4j
public class FlinkK8sWatchController implements AutoCloseable {

    public final TrackIdCache trackIds = TrackIdCache.build();
    public final TrackIdCache canceling = TrackIdCache.build();
    public final EndpointCache endpoints = EndpointCache.build();
    public final JobStatusCache jobStatuses;
    public final K8sDeploymentEventCache k8sDeploymentEvents = K8sDeploymentEventCache.build();
    public final MetricCache flinkMetrics = MetricCache.build();

    public FlinkK8sWatchController() {
        this(JobStatusWatcherConfig.defaultConf());
    }

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
                .filter(id -> groupId.equals(id.groupId()))
                .collect(Collectors.toSet());
        if (activeIds.isEmpty()) {
            return empty;
        }
        Set<ClusterKey> keys =
            activeIds.stream().map(ClusterKey::of).collect(Collectors.toSet());
        Map<ClusterKey, FlinkMetricCV> metrics = flinkMetrics.getAll(keys);
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
        restUrl.ifPresent(url -> endpoints.put(clusterKey, url));
        return restUrl;
    }

    public static final class TrackIdCache {

        private final Cache<CacheKey, TrackId> cache = Caffeine.newBuilder().build();

        static TrackIdCache build() {
            return new TrackIdCache();
        }

        public void update(TrackId k) {
            CacheKey key = new CacheKey(k.appId());
            cache.invalidate(key);
            cache.put(key, k);
        }

        public void set(TrackId k) {
            cache.put(new CacheKey(k.appId()), k);
        }

        public void invalidate(TrackId k) {
            cache.invalidate(new CacheKey(k.appId()));
        }

        public TrackId get(TrackId k) {
            return cache.getIfPresent(new CacheKey(k.appId()));
        }

        public boolean has(TrackId k) {
            return get(k) != null;
        }

        public Set<TrackId> getAll() {
            return cache.asMap().values().stream().collect(Collectors.toSet());
        }

        public void cleanUp() {
            cache.cleanUp();
        }
    }

    public static final class JobStatusCache {

        private final Cache<CacheKey, JobStatusCV> cache;

        JobStatusCache(int timeout) {
            cache =
                Caffeine.newBuilder()
                    .expireAfterWrite(timeout, TimeUnit.SECONDS)
                    .build();
        }

        static JobStatusCache build(int timeout) {
            return new JobStatusCache(timeout);
        }

        public void putAll(Map<TrackId, JobStatusCV> kvs) {
            Map<CacheKey, JobStatusCV> mapped = new HashMap<>();
            kvs.forEach((k, v) -> mapped.put(new CacheKey(k.appId()), v));
            cache.putAll(mapped);
        }

        public void put(TrackId k, JobStatusCV v) {
            cache.put(new CacheKey(k.appId()), v);
        }

        public Map<CacheKey, JobStatusCV> asMap() {
            return new HashMap<>(cache.asMap());
        }

        public Map<CacheKey, JobStatusCV> getAsMap(Set<TrackId> trackIds) {
            Map<CacheKey, JobStatusCV> result = new HashMap<>();
            for (TrackId trackId : trackIds) {
                JobStatusCV value = cache.getIfPresent(new CacheKey(trackId.appId()));
                if (value != null) {
                    result.put(new CacheKey(trackId.appId()), value);
                }
            }
            return result;
        }

        public JobStatusCV get(TrackId k) {
            return cache.getIfPresent(new CacheKey(k.appId()));
        }

        public void invalidate(TrackId k) {
            cache.invalidate(new CacheKey(k.appId()));
        }

        public void cleanUp() {
            cache.cleanUp();
        }
    }

    public static final class EndpointCache {

        private final Cache<ClusterKey, String> cache =
            Caffeine.newBuilder().expireAfterWrite(24, TimeUnit.HOURS).build();

        static EndpointCache build() {
            return new EndpointCache();
        }

        public void invalidate(ClusterKey k) {
            cache.invalidate(k);
        }

        public void put(ClusterKey k, String v) {
            cache.put(k, v);
        }

        public String get(ClusterKey key) {
            return cache.getIfPresent(key);
        }
    }

    public static final class K8sDeploymentEventCache {

        private final Cache<K8sEventKey, K8sDeploymentEventCV> cache = Caffeine.newBuilder().build();

        static K8sDeploymentEventCache build() {
            return new K8sDeploymentEventCache();
        }

        public void put(K8sEventKey k, K8sDeploymentEventCV v) {
            cache.put(k, v);
        }

        public K8sDeploymentEventCV get(K8sEventKey k) {
            return cache.getIfPresent(k);
        }

        public Map<K8sEventKey, K8sDeploymentEventCV> asMap() {
            return new HashMap<>(cache.asMap());
        }

        public void cleanUp() {
            cache.cleanUp();
        }
    }

    public static final class MetricCache {

        private final Cache<ClusterKey, FlinkMetricCV> cache = Caffeine.newBuilder().build();

        static MetricCache build() {
            return new MetricCache();
        }

        public void put(ClusterKey k, FlinkMetricCV v) {
            cache.put(k, v);
        }

        public Map<ClusterKey, FlinkMetricCV> asMap() {
            return new HashMap<>(cache.asMap());
        }

        public Map<ClusterKey, FlinkMetricCV> getAll(Set<ClusterKey> keys) {
            return cache.getAllPresent(keys);
        }

        public FlinkMetricCV get(ClusterKey key) {
            return cache.getIfPresent(key);
        }

        public void invalidate(ClusterKey key) {
            cache.invalidate(key);
        }
    }
}
