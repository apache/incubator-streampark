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

import org.apache.streampark.flink.kubernetes.model.JobStatusCV;
import org.apache.streampark.flink.kubernetes.model.TrackId;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class JobStatusCache {

  private final Cache<CacheKey, JobStatusCV> cache = Caffeine.newBuilder().build();

  private JobStatusCache() {}

  public static JobStatusCache getInstance() {
    return new JobStatusCache();
  }

  public void putAll(Map<TrackId, JobStatusCV> kvs) {
    Map<CacheKey, JobStatusCV> kvMap = new HashMap<>();
    kvs.forEach((key, value) -> kvMap.put(CacheKey.of(key.getAppId()), value));
    cache.putAll(kvMap);
  }

  public void put(TrackId trackId, JobStatusCV status) {
    cache.put(CacheKey.of(trackId.getAppId()), status);
  }

  public Map<CacheKey, JobStatusCV> asMap() {
    return cache.asMap();
  }

  public Map<CacheKey, JobStatusCV> getAsMap(Set<TrackId> trackIds) {
    List<CacheKey> keys =
        trackIds.stream()
            .map(trackId -> CacheKey.of(trackId.getAppId()))
            .collect(Collectors.toList());
    return cache.getAllPresent(keys);
  }

  public JobStatusCV get(TrackId trackId) {
    return cache.getIfPresent(CacheKey.of(trackId.getAppId()));
  }

  public void invalidate(TrackId trackId) {
    cache.invalidate(CacheKey.of(trackId.getAppId()));
  }

  public void cleanUp() {
    cache.cleanUp();
  }
}
