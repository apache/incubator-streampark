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
import org.apache.streampark.console.core.managed.api.ManagedFlinkProviderException;
import org.apache.streampark.console.core.managed.api.ManagedFlinkProviderRegistry;
import org.apache.streampark.console.core.managed.api.ManagedFlinkProviderType;
import org.apache.streampark.console.core.managed.api.ProviderContext;
import org.apache.streampark.console.core.managed.api.ProviderErrorCategory;
import org.apache.streampark.console.core.managed.api.ResolvedManagedFlinkCapability;
import org.apache.streampark.console.core.managed.support.FakeManagedFlinkProvider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ManagedFlinkCapabilityResolverTest {

    private MutableClock clock;

    private FakeManagedFlinkProvider provider;

    private ProviderContext context;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-07-29T00:00:00Z"));
        provider = new FakeManagedFlinkProvider(clock, Duration.ofMinutes(5));
        context = ProviderContext.builder()
            .cloudAccountId(FakeManagedFlinkProvider.VALID_ACCOUNT_ID)
            .credentialVersion(1L)
            .region("cn-beijing")
            .providerConfigJson("{\"fixture\":true}")
            .providerConfigVersion(1)
            .build();
    }

    @Test
    void shouldResolveLiveThenReuseFreshCache() {
        ManagedFlinkCapabilityResolver resolver = resolver(Collections.emptyMap());

        ResolvedManagedFlinkCapability first =
            resolver.resolve(ManagedFlinkProviderType.VOLCENGINE, context);
        ResolvedManagedFlinkCapability second =
            resolver.resolve(ManagedFlinkProviderType.VOLCENGINE, context);

        assertThat(first.getSource()).isEqualTo(ManagedFlinkCapabilitySource.LIVE);
        assertThat(first.isDegraded()).isFalse();
        assertThat(second.getSource()).isEqualTo(ManagedFlinkCapabilitySource.CACHE);
        assertThat(second.getCapability()).isSameAs(first.getCapability());
        assertThat(provider.getCapabilityRequestCount()).isEqualTo(1);
    }

    @Test
    void shouldUseStaleCacheWhenRefreshFails() {
        ManagedFlinkCapabilityResolver resolver = resolver(Collections.emptyMap());
        ManagedFlinkCapability live =
            resolver.resolve(ManagedFlinkProviderType.VOLCENGINE, context).getCapability();
        clock.advance(Duration.ofMinutes(6));
        provider.failCapabilityWith(ProviderErrorCategory.TRANSIENT);

        ResolvedManagedFlinkCapability degraded =
            resolver.resolve(ManagedFlinkProviderType.VOLCENGINE, context);

        assertThat(degraded.getSource()).isEqualTo(ManagedFlinkCapabilitySource.STALE_CACHE);
        assertThat(degraded.isDegraded()).isTrue();
        assertThat(degraded.getCapability()).isSameAs(live);
        assertThat(provider.getCapabilityRequestCount()).isEqualTo(2);
    }

    @Test
    void shouldUseBuiltInCapabilityWhenProviderFailsWithoutCache() {
        ManagedFlinkCapability builtIn = provider.capability();
        provider.failCapabilityWith(ProviderErrorCategory.TRANSIENT);
        ManagedFlinkCapabilityResolver resolver =
            resolver(Collections.singletonMap(ManagedFlinkProviderType.VOLCENGINE, builtIn));

        ResolvedManagedFlinkCapability degraded =
            resolver.resolve(ManagedFlinkProviderType.VOLCENGINE, context);

        assertThat(degraded.getSource()).isEqualTo(ManagedFlinkCapabilitySource.BUILT_IN);
        assertThat(degraded.isDegraded()).isTrue();
        assertThat(degraded.getCapability()).isSameAs(builtIn);
    }

    @Test
    void shouldPropagateProviderFailureWhenNoFallbackExists() {
        provider.failCapabilityWith(ProviderErrorCategory.AUTHORIZATION);
        ManagedFlinkCapabilityResolver resolver = resolver(Collections.emptyMap());

        assertThatThrownBy(
            () -> resolver.resolve(ManagedFlinkProviderType.VOLCENGINE, context))
                .isInstanceOfSatisfying(
                    ManagedFlinkProviderException.class,
                    exception -> assertThat(exception.getCategory())
                        .isEqualTo(ProviderErrorCategory.AUTHORIZATION));
    }

    @Test
    void shouldNotMaskNonRetryableFailureWithStaleCache() {
        ManagedFlinkCapabilityResolver resolver = resolver(Collections.emptyMap());
        resolver.resolve(ManagedFlinkProviderType.VOLCENGINE, context);
        clock.advance(Duration.ofMinutes(6));
        provider.failCapabilityWith(ProviderErrorCategory.AUTHENTICATION);

        assertThatThrownBy(
            () -> resolver.resolve(ManagedFlinkProviderType.VOLCENGINE, context))
                .isInstanceOfSatisfying(
                    ManagedFlinkProviderException.class,
                    exception -> assertThat(exception.getCategory())
                        .isEqualTo(ProviderErrorCategory.AUTHENTICATION));
    }

    @Test
    void shouldRejectCacheOlderThanMaximumStaleAge() {
        ManagedFlinkCapabilityResolver resolver =
            resolver(Collections.emptyMap(), Duration.ofHours(1));
        resolver.resolve(ManagedFlinkProviderType.VOLCENGINE, context);
        clock.advance(Duration.ofHours(2));
        provider.failCapabilityWith(ProviderErrorCategory.TRANSIENT);

        assertThatThrownBy(
            () -> resolver.resolve(ManagedFlinkProviderType.VOLCENGINE, context))
                .isInstanceOf(ManagedFlinkProviderException.class);
    }

    @Test
    void shouldRefreshAfterExplicitInvalidation() {
        ManagedFlinkCapabilityResolver resolver = resolver(Collections.emptyMap());
        resolver.resolve(ManagedFlinkProviderType.VOLCENGINE, context);

        resolver.invalidate(ManagedFlinkProviderType.VOLCENGINE, context);
        ResolvedManagedFlinkCapability refreshed =
            resolver.resolve(ManagedFlinkProviderType.VOLCENGINE, context);

        assertThat(refreshed.getSource()).isEqualTo(ManagedFlinkCapabilitySource.LIVE);
        assertThat(provider.getCapabilityRequestCount()).isEqualTo(2);
    }

    private ManagedFlinkCapabilityResolver resolver(
                                                    java.util.Map<ManagedFlinkProviderType, ManagedFlinkCapability> builtIns) {
        return resolver(builtIns, Duration.ofHours(24));
    }

    private ManagedFlinkCapabilityResolver resolver(
                                                    java.util.Map<ManagedFlinkProviderType, ManagedFlinkCapability> builtIns,
                                                    Duration maxStaleAge) {
        ManagedFlinkProviderRegistry registry =
            new ManagedFlinkProviderRegistry(Collections.singletonList(provider));
        return new ManagedFlinkCapabilityResolver(registry, builtIns, clock, 16, maxStaleAge);
    }

    private static class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
