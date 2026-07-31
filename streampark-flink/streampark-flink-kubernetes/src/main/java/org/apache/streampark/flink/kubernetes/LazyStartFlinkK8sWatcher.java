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

import javax.annotation.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Decorator for FlinkK8sWatcher used to trigger the run behavior. What more, this decorator has
 * the ability to automatically recover the FlinkK8sWatcher's internal FlinkWatcher.
 */
public class LazyStartFlinkK8sWatcher implements FlinkK8sWatcher {

    private final DefaultFlinkK8sWatcher delegate;

    public LazyStartFlinkK8sWatcher(DefaultFlinkK8sWatcher delegate) {
        this.delegate = delegate;
    }

    @Override
    public void registerListener(Object listener) {
        delegate.registerListener(listener);
    }

    @Override
    public void start() {
        delegate.start();
    }

    @Override
    public void stop() {
        delegate.stop();
    }

    @Override
    public void restart() {
        delegate.restart();
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public void doWatching(TrackId trackId) {
        start();
        delegate.doWatching(trackId);
    }

    @Override
    public void unWatching(TrackId trackId) {
        start();
        delegate.unWatching(trackId);
    }

    @Override
    public boolean isInWatching(TrackId trackId) {
        start();
        return delegate.isInWatching(trackId);
    }

    @Override
    public Set<TrackId> getAllWatchingIds() {
        start();
        return delegate.getAllWatchingIds();
    }

    @Override
    public Optional<JobStatusCV> getJobStatus(TrackId trackId) {
        start();
        return delegate.getJobStatus(trackId);
    }

    @Override
    public Map<CacheKey, JobStatusCV> getJobStatus(Set<TrackId> trackIds) {
        start();
        return delegate.getJobStatus(trackIds);
    }

    @Override
    public Map<CacheKey, JobStatusCV> getAllJobStatus() {
        start();
        return delegate.getAllJobStatus();
    }

    @Override
    public FlinkMetricCV getAccGroupMetrics(@Nullable String groupId) {
        return delegate.getAccGroupMetrics(groupId);
    }

    @Override
    public Optional<FlinkMetricCV> getClusterMetrics(ClusterKey clusterKey) {
        return delegate.getClusterMetrics(clusterKey);
    }

    @Override
    public boolean checkIsInRemoteCluster(TrackId trackId) {
        start();
        return delegate.checkIsInRemoteCluster(trackId);
    }

    @Override
    public void postEvent(BuildInEvent event, boolean sync) {
        start();
        delegate.postEvent(event, sync);
    }

    @Override
    @Nullable
    public String getRemoteRestUrl(TrackId trackId) {
        return delegate.getRemoteRestUrl(trackId);
    }
}
