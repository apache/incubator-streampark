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

import org.apache.streampark.flink.kubernetes.model.JobStatusCV;
import org.apache.streampark.flink.kubernetes.model.TrackId;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class JobStatusCache {

    private final Cache<CacheKey, JobStatusCV> cache;

    public JobStatusCache(int timeout) {
        this.cache =
            Caffeine.newBuilder().expireAfterWrite(timeout, TimeUnit.SECONDS).build();
    }

    public void putAll(Map<TrackId, JobStatusCV> kvs) {
        Map<CacheKey, JobStatusCV> mapped = new HashMap<>();
        kvs.forEach((trackId, cv) -> mapped.put(new CacheKey(trackId.appId()), cv));
        cache.putAll(mapped);
    }

    public void put(TrackId trackId, JobStatusCV value) {
        cache.put(new CacheKey(trackId.appId()), value);
    }

    public Map<CacheKey, JobStatusCV> asMap() {
        return new HashMap<>(cache.asMap());
    }

    public Map<CacheKey, JobStatusCV> getAsMap(Set<TrackId> trackIds) {
        Set<CacheKey> keys =
            trackIds.stream()
                .map(trackId -> new CacheKey(trackId.appId()))
                .collect(Collectors.toSet());
        return new HashMap<>(cache.getAllPresent(keys));
    }

    public JobStatusCV get(TrackId trackId) {
        return cache.getIfPresent(new CacheKey(trackId.appId()));
    }

    public void invalidate(TrackId trackId) {
        cache.invalidate(new CacheKey(trackId.appId()));
    }

    public void cleanUp() {
        cache.cleanUp();
    }

    public static JobStatusCache build(int timeout) {
        return new JobStatusCache(timeout);
    }
}
