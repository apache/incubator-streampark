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

package org.apache.streampark.console.core.managed.support;

import org.apache.streampark.console.core.managed.api.ManagedJobRestartRequest;
import org.apache.streampark.console.core.managed.api.ManagedJobRestoreMode;
import org.apache.streampark.console.core.managed.api.ManagedJobStartRequest;
import org.apache.streampark.console.core.managed.api.ManagedJobStopRequest;
import org.apache.streampark.console.core.managed.api.ProviderContext;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class FakeManagedFlinkProviderLifecycleTest {

    private final FakeManagedFlinkProvider provider =
        new FakeManagedFlinkProvider(Clock.systemUTC(), Duration.ofMinutes(1));

    private final ProviderContext context =
        ProviderContext.builder()
            .cloudAccountId(FakeManagedFlinkProvider.VALID_ACCOUNT_ID)
            .region("cn-beijing")
            .providerConfigJson("{\"fixture\":true}")
            .providerConfigVersion(1)
            .build();

    @Test
    void shouldRecordDeterministicLifecycleActions() {
        assertThat(
            provider.startJob(
                context,
                ManagedJobStartRequest.builder()
                    .jobId("job-1")
                    .restoreMode(ManagedJobRestoreMode.FRESH)
                    .build()))
                        .satisfies(
                            result -> {
                                assertThat(result.getJobId()).isEqualTo("job-1");
                                assertThat(result.getInstanceId()).isEqualTo("fake-instance-1");
                                assertThat(result.getProviderState()).isEqualTo("STARTING");
                            });

        provider.stopJob(
            context,
            ManagedJobStopRequest.builder()
                .jobId("job-1")
                .instanceId("fake-instance-1")
                .build());
        provider.restartJob(
            context,
            ManagedJobRestartRequest.builder()
                .jobId("job-1")
                .restoreMode(ManagedJobRestoreMode.LATEST_STATE)
                .build());

        assertThat(provider.getJobActionCount()).isEqualTo(3);
        assertThat(provider.getJobAction("STOP", "job-1").getInstanceId())
            .isEqualTo("fake-instance-1");
        assertThat(provider.getJobAction("RESTART", "job-1").getProviderState())
            .isEqualTo("RESTARTING");
    }
}
