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

import org.apache.streampark.flink.kubernetes.model.ClusterKey;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.TimeUnit;

public class EndpointCache {

  private final Cache<ClusterKey, String> cache =
      Caffeine.newBuilder().expireAfterWrite(24, TimeUnit.HOURS).build();

  private EndpointCache() {}

  public static EndpointCache getInstance() {
    return new EndpointCache();
  }

  public void invalidate(ClusterKey clusterKey) {
    cache.invalidate(clusterKey);
  }

  public void put(ClusterKey clusterKey, String value) {
    cache.put(clusterKey, value);
  }

  public String get(ClusterKey clusterKey) {
    return cache.getIfPresent(clusterKey);
  }
}
