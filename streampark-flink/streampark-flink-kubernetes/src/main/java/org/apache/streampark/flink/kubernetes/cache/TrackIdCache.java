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

package org.apache.streampark.flink.kubernetes.cache;

import org.apache.streampark.flink.kubernetes.model.TrackId;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class TrackIdCache {

  private final Cache<CacheKey, TrackId> cache = Caffeine.newBuilder().build();

  private TrackIdCache() {}

  public static TrackIdCache getInstance() {
    return new TrackIdCache();
  }

  public void update(TrackId trackId) {
    CacheKey cacheKey = CacheKey.of(trackId.getAppId());
    cache.invalidate(cacheKey);
    cache.put(cacheKey, trackId);
  }

  public void set(TrackId trackId) {
    CacheKey cacheKey = CacheKey.of(trackId.getAppId());
    cache.put(cacheKey, trackId);
  }

  public void invalidate(TrackId trackId) {
    CacheKey cacheKey = CacheKey.of(trackId.getAppId());
    cache.invalidate(cacheKey);
  }

  public TrackId get(TrackId trackId) {
    CacheKey cacheKey = CacheKey.of(trackId.getAppId());
    return cache.getIfPresent(cacheKey);
  }

  public boolean has(TrackId trackId) {
    return Objects.nonNull(get(trackId));
  }

  public Set<TrackId> getAll() {
    return new HashSet<>(cache.asMap().values());
  }

  public void cleanUp() {
    cache.cleanUp();
  }
}
