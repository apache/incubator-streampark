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

import org.apache.streampark.flink.kubernetes.model.K8sDeploymentEventCV;
import org.apache.streampark.flink.kubernetes.model.K8sEventKey;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.HashMap;
import java.util.Map;

public class K8sDeploymentEventCache {

    final Cache<K8sEventKey, K8sDeploymentEventCV> cache = Caffeine.newBuilder().build();

    public void put(K8sEventKey key, K8sDeploymentEventCV value) {
        cache.put(key, value);
    }

    public K8sDeploymentEventCV get(K8sEventKey key) {
        return cache.getIfPresent(key);
    }

    public Map<K8sEventKey, K8sDeploymentEventCV> asMap() {
        return new HashMap<>(cache.asMap());
    }

    public void cleanUp() {
        cache.cleanUp();
    }

    public static K8sDeploymentEventCache build() {
        return new K8sDeploymentEventCache();
    }
}
