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

import org.apache.streampark.flink.kubernetes.model.K8sDeploymentEventCV;
import org.apache.streampark.flink.kubernetes.model.K8sEventKey;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.Map;

public class K8sDeploymentEventCache {

  private final Cache<K8sEventKey, K8sDeploymentEventCV> cache = Caffeine.newBuilder().build();

  private K8sDeploymentEventCache() {}

  public static K8sDeploymentEventCache getInstance() {
    return new K8sDeploymentEventCache();
  }

  public void put(K8sEventKey k8sEventKey, K8sDeploymentEventCV k8sDeploymentEventCV) {
    cache.put(k8sEventKey, k8sDeploymentEventCV);
  }

  public void get(K8sEventKey k8sEventKey) {
    cache.getIfPresent(k8sEventKey);
  }

  public Map<K8sEventKey, K8sDeploymentEventCV> asMap() {
    return cache.asMap();
  }

  public void cleanUp() {
    cache.cleanUp();
  }
}
