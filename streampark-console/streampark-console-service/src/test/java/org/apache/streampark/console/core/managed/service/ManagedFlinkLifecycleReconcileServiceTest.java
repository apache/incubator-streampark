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

import org.apache.streampark.console.core.entity.ManagedFlinkOperation;
import org.apache.streampark.console.core.managed.api.ManagedFlinkProvider;
import org.apache.streampark.console.core.managed.api.ManagedFlinkProviderType;
import org.apache.streampark.console.core.managed.api.ManagedJobState;
import org.apache.streampark.console.core.managed.api.ManagedJobStatus;
import org.apache.streampark.console.core.managed.api.ProviderContext;
import org.apache.streampark.console.core.managed.model.ManagedFlinkLifecycleSnapshot;
import org.apache.streampark.console.core.managed.model.ManagedFlinkOperationView;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ManagedFlinkLifecycleReconcileServiceTest {

    @Test
    void shouldFailRestartWhenProviderRemainsStoppedOnSameInstance() throws Exception {
        ManagedFlinkApplicationService applicationService =
            mock(ManagedFlinkApplicationService.class);
        ManagedFlinkOperationService operationService =
            mock(ManagedFlinkOperationService.class);
        ManagedFlinkProviderContextService contextService =
            mock(ManagedFlinkProviderContextService.class);
        ManagedFlinkLifecycleStateService lifecycleStateService =
            mock(ManagedFlinkLifecycleStateService.class);
        ObjectMapper objectMapper = new ObjectMapper();
        ManagedFlinkLifecycleReconcileService service =
            new ManagedFlinkLifecycleReconcileService(
                applicationService,
                operationService,
                contextService,
                lifecycleStateService,
                objectMapper);

        ManagedFlinkLifecycleSnapshot snapshot = new ManagedFlinkLifecycleSnapshot();
        snapshot.setTeamId(1L);
        snapshot.setAppId(100004L);
        snapshot.setCloudAccountId(2L);
        snapshot.setProviderType("VOLCENGINE");
        snapshot.setProviderConfigJson("{\"fixture\":true}");
        snapshot.setProviderConfigVersion(1);
        snapshot.setJobName("job-1");
        snapshot.setOperationType("RESTART");
        snapshot.setJobId("job-1");
        snapshot.setInstanceId("instance-1");

        ManagedFlinkOperation operation = new ManagedFlinkOperation();
        operation.setId(17L);
        operation.setAppId(100004L);
        operation.setOperationType("RESTART");
        operation.setState("UNKNOWN");
        operation.setRequestJson(objectMapper.writeValueAsString(snapshot));
        operation.setModifyTime(new Date(System.currentTimeMillis() - 31_000L));
        when(operationService.getRequired(100004L, 17L)).thenReturn(operation);
        when(operationService.markReconciling(17L)).thenReturn(true);

        ManagedFlinkProvider provider = mock(ManagedFlinkProvider.class);
        ManagedFlinkProviderSession session =
            new ManagedFlinkProviderSession(
                ManagedFlinkProviderType.VOLCENGINE,
                provider,
                ProviderContext.builder()
                    .providerConfigJson("{\"fixture\":true}")
                    .providerConfigVersion(1)
                    .build(),
                null);
        when(contextService.resolve(1L, 2L, "{\"fixture\":true}", 1)).thenReturn(session);
        when(provider.getJob(any(), any()))
            .thenReturn(
                ManagedJobStatus.builder()
                    .jobId("job-1")
                    .instanceId("instance-1")
                    .state(ManagedJobState.STOPPED)
                    .providerState("STOPPED")
                    .build());
        ManagedFlinkOperationView failed =
            ManagedFlinkOperationView.builder()
                .operationId(17L)
                .appId(100004L)
                .type("RESTART")
                .state("FAILED")
                .build();
        when(operationService.getView(100004L, 17L)).thenReturn(failed);

        assertThat(service.reconcile(1L, 100004L, 17L).getState()).isEqualTo("FAILED");
        verify(operationService)
            .markFailed(
                eq(17L),
                eq(false),
                eq(null),
                eq("PROVIDER:LifecycleTransitionFailed"),
                any());
    }
}
