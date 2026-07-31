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

package org.apache.streampark.console.core.managed.service;

import org.apache.streampark.console.core.managed.api.ManagedFlinkCapability;
import org.apache.streampark.console.core.managed.api.ManagedFlinkCapabilitySource;
import org.apache.streampark.console.core.managed.api.ManagedFlinkProvider;
import org.apache.streampark.console.core.managed.api.ManagedFlinkProviderException;
import org.apache.streampark.console.core.managed.api.ManagedFlinkProviderRegistry;
import org.apache.streampark.console.core.managed.api.ManagedFlinkProviderType;
import org.apache.streampark.console.core.managed.api.ProviderContext;
import org.apache.streampark.console.core.managed.api.ProviderErrorCategory;
import org.apache.streampark.console.core.managed.api.ResolvedManagedFlinkCapability;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Value;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Resolves provider capabilities with explicit cache and built-in fallback semantics.
 *
 * <p>Expired entries are retained for degraded reads when the provider is temporarily unavailable.
 * Callers must inspect the returned source and surface degraded data to users.
 */
public class ManagedFlinkCapabilityResolver {

    private static final long DEFAULT_MAXIMUM_CACHE_SIZE = 1024;

    private static final Duration DEFAULT_MAX_STALE_AGE = Duration.ofHours(24);

    private final ManagedFlinkProviderRegistry providerRegistry;

    private final Map<ManagedFlinkProviderType, ManagedFlinkCapability> builtInCapabilities;

    private final Clock clock;

    private final Duration maxStaleAge;

    private final Cache<CapabilityCacheKey, CapabilityCacheEntry> capabilityCache;

    public ManagedFlinkCapabilityResolver(
                                          ManagedFlinkProviderRegistry providerRegistry,
                                          Map<ManagedFlinkProviderType, ManagedFlinkCapability> builtInCapabilities) {
        this(
            providerRegistry,
            builtInCapabilities,
            Clock.systemUTC(),
            DEFAULT_MAXIMUM_CACHE_SIZE,
            DEFAULT_MAX_STALE_AGE);
    }

    ManagedFlinkCapabilityResolver(
                                   ManagedFlinkProviderRegistry providerRegistry,
                                   Map<ManagedFlinkProviderType, ManagedFlinkCapability> builtInCapabilities,
                                   Clock clock,
                                   long maximumCacheSize) {
        this(
            providerRegistry,
            builtInCapabilities,
            clock,
            maximumCacheSize,
            DEFAULT_MAX_STALE_AGE);
    }

    ManagedFlinkCapabilityResolver(
                                   ManagedFlinkProviderRegistry providerRegistry,
                                   Map<ManagedFlinkProviderType, ManagedFlinkCapability> builtInCapabilities,
                                   Clock clock,
                                   long maximumCacheSize,
                                   Duration maxStaleAge) {
        this.providerRegistry =
            Objects.requireNonNull(providerRegistry, "providerRegistry must not be null");
        this.builtInCapabilities = immutableCapabilities(builtInCapabilities);
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.maxStaleAge = Objects.requireNonNull(maxStaleAge, "maxStaleAge must not be null");
        if (maximumCacheSize <= 0) {
            throw new IllegalArgumentException("maximumCacheSize must be greater than zero");
        }
        if (maxStaleAge.isNegative() || maxStaleAge.isZero()) {
            throw new IllegalArgumentException("maxStaleAge must be greater than zero");
        }
        this.capabilityCache = Caffeine.newBuilder().maximumSize(maximumCacheSize).build();
    }

    public ResolvedManagedFlinkCapability resolve(
                                                  ManagedFlinkProviderType providerType,
                                                  ProviderContext context) {
        Objects.requireNonNull(providerType, "providerType must not be null");
        Objects.requireNonNull(context, "context must not be null");

        Instant now = clock.instant();
        CapabilityCacheKey cacheKey = new CapabilityCacheKey(providerType, context);
        CapabilityCacheEntry cacheEntry = capabilityCache.getIfPresent(cacheKey);
        ManagedFlinkCapability cached = cacheEntry == null ? null : cacheEntry.getCapability();
        if (isFresh(cached, now)) {
            return resolved(cached, ManagedFlinkCapabilitySource.CACHE, now);
        }

        ManagedFlinkProvider provider = providerRegistry.getRequired(providerType);
        try {
            ManagedFlinkCapability live = validate(providerType, provider.getCapability(context), now);
            capabilityCache.put(cacheKey, new CapabilityCacheEntry(live, now));
            return resolved(live, ManagedFlinkCapabilitySource.LIVE, now);
        } catch (ManagedFlinkProviderException exception) {
            if (!exception.isRetryable()) {
                throw exception;
            }
            if (cacheEntry != null && isWithinStaleAge(cacheEntry, now)) {
                return resolved(cached, ManagedFlinkCapabilitySource.STALE_CACHE, now);
            }
            ManagedFlinkCapability builtIn = builtInCapabilities.get(providerType);
            if (builtIn != null) {
                return resolved(builtIn, ManagedFlinkCapabilitySource.BUILT_IN, now);
            }
            throw exception;
        }
    }

    public void invalidate(ManagedFlinkProviderType providerType, ProviderContext context) {
        Objects.requireNonNull(providerType, "providerType must not be null");
        Objects.requireNonNull(context, "context must not be null");
        capabilityCache.invalidate(new CapabilityCacheKey(providerType, context));
    }

    private ManagedFlinkCapability validate(
                                            ManagedFlinkProviderType expectedProviderType,
                                            ManagedFlinkCapability capability,
                                            Instant now) {
        if (capability == null) {
            throw invalidCapability("Provider returned a null capability");
        }
        if (capability.getProviderType() != expectedProviderType) {
            throw invalidCapability("Capability provider type does not match the requested provider");
        }
        if (capability.getExpireAt() == null || !capability.getExpireAt().isAfter(now)) {
            throw invalidCapability("Capability expiration must be later than the resolution time");
        }
        return capability;
    }

    private ManagedFlinkProviderException invalidCapability(String message) {
        return new ManagedFlinkProviderException(
            ProviderErrorCategory.VALIDATION, "InvalidCapability", null, message);
    }

    private boolean isFresh(ManagedFlinkCapability capability, Instant now) {
        return capability != null
            && capability.getExpireAt() != null
            && capability.getExpireAt().isAfter(now);
    }

    private boolean isWithinStaleAge(CapabilityCacheEntry cacheEntry, Instant now) {
        return cacheEntry.getCachedAt().plus(maxStaleAge).isAfter(now);
    }

    private ResolvedManagedFlinkCapability resolved(
                                                    ManagedFlinkCapability capability,
                                                    ManagedFlinkCapabilitySource source,
                                                    Instant resolvedAt) {
        return ResolvedManagedFlinkCapability.builder()
            .capability(capability)
            .source(source)
            .resolvedAt(resolvedAt)
            .build();
    }

    private Map<ManagedFlinkProviderType, ManagedFlinkCapability> immutableCapabilities(
                                                                                        Map<ManagedFlinkProviderType, ManagedFlinkCapability> capabilities) {
        if (capabilities == null || capabilities.isEmpty()) {
            return Collections.emptyMap();
        }
        EnumMap<ManagedFlinkProviderType, ManagedFlinkCapability> copy =
            new EnumMap<>(ManagedFlinkProviderType.class);
        capabilities.forEach(
            (providerType, capability) -> {
                Objects.requireNonNull(providerType, "built-in provider type must not be null");
                Objects.requireNonNull(capability, "built-in capability must not be null");
                if (capability.getProviderType() != providerType) {
                    throw new IllegalArgumentException(
                        "Built-in capability provider type does not match its map key");
                }
                copy.put(providerType, capability);
            });
        return Collections.unmodifiableMap(copy);
    }

    @Value
    private static class CapabilityCacheKey {

        ManagedFlinkProviderType providerType;

        ProviderContext context;
    }

    @Value
    private static class CapabilityCacheEntry {

        ManagedFlinkCapability capability;

        Instant cachedAt;
    }
}
