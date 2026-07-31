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

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ManagedFlinkProviderRegistryTest {

    @Test
    void shouldResolveRegisteredProvider() {
        ManagedFlinkProvider provider = new FakeManagedFlinkProvider();
        ManagedFlinkProviderRegistry registry =
            new ManagedFlinkProviderRegistry(Collections.singletonList(provider));

        assertThat(registry.contains(ManagedFlinkProviderType.VOLCENGINE)).isTrue();
        assertThat(registry.getRequired(ManagedFlinkProviderType.VOLCENGINE)).isSameAs(provider);
    }

    @Test
    void shouldRejectDuplicateProviderType() {
        List<ManagedFlinkProvider> providers =
            Arrays.asList(new FakeManagedFlinkProvider(), new FakeManagedFlinkProvider());

        assertThatThrownBy(() -> new ManagedFlinkProviderRegistry(providers))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Duplicate managed Flink provider type");
    }

    @Test
    void shouldExposeRetryPolicyFromErrorCategory() {
        ManagedFlinkProviderException exception =
            new ManagedFlinkProviderException(
                ProviderErrorCategory.RATE_LIMIT,
                "Throttling",
                null,
                "Provider request was throttled");

        assertThat(exception.isRetryable()).isTrue();
        assertThat(exception.getProviderRequestId()).isNull();
    }

    private static class FakeManagedFlinkProvider implements ManagedFlinkProvider {

        @Override
        public ManagedFlinkProviderType type() {
            return ManagedFlinkProviderType.VOLCENGINE;
        }

        @Override
        public ManagedFlinkCapability getCapability(ProviderContext context) {
            return ManagedFlinkCapability.builder()
                .providerType(type())
                .apiVersion("2025-01-01")
                .build();
        }

        @Override
        public CredentialCheckResult validateCredential(ProviderContext context) {
            return CredentialCheckResult.builder().success(true).build();
        }

        @Override
        public List<CloudProject> listProjects(ProviderContext context, String keyword) {
            return Collections.emptyList();
        }

        @Override
        public List<ManagedResourcePool> listResourcePools(
                                                           ProviderContext context, String projectId, String keyword) {
            return Collections.emptyList();
        }

        @Override
        public StagedArtifact stageArtifact(
                                            ProviderContext context, ArtifactStageRequest request) {
            return null;
        }

        @Override
        public StagedArtifact findArtifact(
                                           ProviderContext context, ArtifactLookupRequest request) {
            return null;
        }
    }
}
