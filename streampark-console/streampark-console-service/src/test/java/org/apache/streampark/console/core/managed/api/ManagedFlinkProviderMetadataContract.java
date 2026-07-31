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

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Reusable contract for the read-only metadata slice of a managed Flink provider. */
public abstract class ManagedFlinkProviderMetadataContract {

    protected abstract ManagedFlinkProvider provider();

    protected abstract ProviderContext validContext();

    protected abstract ProviderContext invalidCredentialContext();

    @Test
    void shouldReturnCompleteCapabilityMetadata() {
        ManagedFlinkProvider provider = provider();

        ManagedFlinkCapability capability = provider.getCapability(validContext());

        assertThat(capability).isNotNull();
        assertThat(capability.getProviderType()).isEqualTo(provider.type());
        assertThat(capability.getApiVersion()).isNotBlank();
        assertThat(capability.getEngineVersions()).isNotNull();
        assertThat(capability.getJobTypes()).isNotNull();
        assertThat(capability.getExecutionModes()).isNotNull();
        assertThat(capability.getStartModes()).isNotNull();
        assertThat(capability.getSchedulingStrategies()).isNotNull();
        assertThat(capability.getCustomParameterRules()).isNotNull();
        assertThat(capability.getCapabilityRevision()).isNotBlank();
        assertThat(capability.getExpireAt()).isAfter(Instant.now());
        assertThatThrownBy(() -> capability.getEngineVersions().add("unexpected"))
            .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(
            () -> capability.getCustomParameterRules().put("unexpected", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldValidateCredentialWithoutReturningSecrets() {
        CredentialCheckResult result = provider().validateCredential(validContext());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.toString()).doesNotContain("secret", "accessKey", "access_key");
    }

    @Test
    void shouldClassifyInvalidCredentialAsAuthenticationError() {
        assertThatThrownBy(() -> provider().validateCredential(invalidCredentialContext()))
            .isInstanceOfSatisfying(
                ManagedFlinkProviderException.class,
                exception -> {
                    assertThat(exception.getCategory())
                        .isEqualTo(ProviderErrorCategory.AUTHENTICATION);
                    assertThat(exception.isRetryable()).isFalse();
                });
    }

    @Test
    void shouldReturnMetadataWhenCapabilityDeclaresSupport() {
        ManagedFlinkProvider provider = provider();
        ManagedFlinkCapability capability = provider.getCapability(validContext());

        if (capability.isSupportsProjectList()) {
            assertThat(provider.listProjects(validContext(), null)).isNotNull().isNotEmpty();
        }
        if (capability.isSupportsResourcePoolList()) {
            assertThat(provider.listResourcePools(validContext(), "fake-project", null))
                .isNotNull()
                .isNotEmpty();
        }
    }
}
