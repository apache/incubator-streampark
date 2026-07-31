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

package org.apache.streampark.console.core.managed.api;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Registry that resolves exactly one managed Flink provider per provider type. */
public class ManagedFlinkProviderRegistry {

    private final Map<ManagedFlinkProviderType, ManagedFlinkProvider> providers;

    public ManagedFlinkProviderRegistry(List<ManagedFlinkProvider> providers) {
        Objects.requireNonNull(providers, "providers must not be null");
        Map<ManagedFlinkProviderType, ManagedFlinkProvider> providerMap =
            new EnumMap<>(ManagedFlinkProviderType.class);
        for (ManagedFlinkProvider provider : providers) {
            Objects.requireNonNull(provider, "provider must not be null");
            ManagedFlinkProviderType type =
                Objects.requireNonNull(provider.type(), "provider type must not be null");
            ManagedFlinkProvider previous = providerMap.put(type, provider);
            if (previous != null) {
                throw new IllegalArgumentException(
                    String.format("Duplicate managed Flink provider type: %s", type));
            }
        }
        this.providers = Collections.unmodifiableMap(providerMap);
    }

    public ManagedFlinkProvider getRequired(ManagedFlinkProviderType type) {
        Objects.requireNonNull(type, "provider type must not be null");
        ManagedFlinkProvider provider = providers.get(type);
        if (provider == null) {
            throw new IllegalArgumentException(
                String.format("Managed Flink provider is not registered: %s", type));
        }
        return provider;
    }

    public boolean contains(ManagedFlinkProviderType type) {
        return type != null && providers.containsKey(type);
    }
}
