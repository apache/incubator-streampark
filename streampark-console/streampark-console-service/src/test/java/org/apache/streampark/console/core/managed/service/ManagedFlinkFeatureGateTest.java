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

import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.core.managed.api.ManagedFlinkProviderType;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ManagedFlinkFeatureGateTest {

    private final ApplicationContextRunner contextRunner =
        new ApplicationContextRunner().withBean(ManagedFlinkFeatureGate.class);

    @Test
    void shouldBindMissingPropertiesAsDisabled() {
        contextRunner.run(
            context -> {
                ManagedFlinkFeatureGate gate = context.getBean(ManagedFlinkFeatureGate.class);
                assertThat(gate.isEnabled()).isFalse();
                assertThat(gate.isProviderEnabled(ManagedFlinkProviderType.VOLCENGINE)).isFalse();
            });
    }

    @Test
    void shouldBindGlobalAndProviderProperties() {
        contextRunner
            .withPropertyValues(
                "streampark.managed-flink.enabled=true",
                "streampark.managed-flink.providers.volcengine.enabled=true")
            .run(
                context -> {
                    ManagedFlinkFeatureGate gate =
                        context.getBean(ManagedFlinkFeatureGate.class);
                    assertThat(gate.isEnabled()).isTrue();
                    assertThat(gate.isProviderEnabled(ManagedFlinkProviderType.VOLCENGINE))
                        .isTrue();
                });
    }

    @Test
    void shouldDisableManagedFlinkByDefaultConfigurationValues() {
        ManagedFlinkFeatureGate gate = new ManagedFlinkFeatureGate(false, false);

        assertThat(gate.isEnabled()).isFalse();
        assertThat(gate.isProviderEnabled(ManagedFlinkProviderType.VOLCENGINE)).isFalse();
        assertThatThrownBy(
            () -> gate.requireWriteEnabled(ManagedFlinkProviderType.VOLCENGINE))
                .isInstanceOf(ApiAlertException.class)
                .hasMessageContaining("streampark.managed-flink.enabled");
    }

    @Test
    void shouldRequireGlobalAndProviderFlags() {
        ManagedFlinkFeatureGate globallyDisabled = new ManagedFlinkFeatureGate(false, true);
        ManagedFlinkFeatureGate providerDisabled = new ManagedFlinkFeatureGate(true, false);

        assertThat(
            globallyDisabled.isProviderEnabled(ManagedFlinkProviderType.VOLCENGINE))
                .isFalse();
        assertThat(providerDisabled.isProviderEnabled(ManagedFlinkProviderType.VOLCENGINE))
            .isFalse();
        assertThatThrownBy(
            () -> providerDisabled.requireWriteEnabled(ManagedFlinkProviderType.VOLCENGINE))
                .isInstanceOf(ApiAlertException.class)
                .hasMessageContaining("provider feature flag");
    }

    @Test
    void shouldEnableProviderWritesOnlyWhenBothFlagsAreEnabled() {
        ManagedFlinkFeatureGate gate = new ManagedFlinkFeatureGate(true, true);

        assertThat(gate.isEnabled()).isTrue();
        assertThat(gate.isProviderEnabled(ManagedFlinkProviderType.VOLCENGINE)).isTrue();
        assertThatCode(
            () -> gate.requireWriteEnabled(ManagedFlinkProviderType.VOLCENGINE))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectMissingProviderType() {
        ManagedFlinkFeatureGate gate = new ManagedFlinkFeatureGate(true, true);

        assertThat(gate.isProviderEnabled(null)).isFalse();
        assertThatThrownBy(() -> gate.requireWriteEnabled(null))
            .isInstanceOf(ApiAlertException.class)
            .hasMessageContaining("provider type is required");
    }
}
