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

import org.apache.streampark.console.SpringUnitTestBase;
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.core.entity.ManagedFlinkOperation;
import org.apache.streampark.console.core.managed.api.ManagedFlinkProviderRegistry;
import org.apache.streampark.console.core.managed.api.ManagedFlinkProviderType;
import org.apache.streampark.console.core.managed.model.CloudAccountCreateRequest;
import org.apache.streampark.console.core.managed.model.CloudAccountGrantRequest;
import org.apache.streampark.console.core.managed.model.ManagedFlinkApplicationSaveRequest;
import org.apache.streampark.console.core.managed.model.ManagedFlinkEnvironmentCreateRequest;
import org.apache.streampark.console.core.managed.model.ManagedFlinkOperationView;
import org.apache.streampark.console.core.managed.support.FakeManagedFlinkProvider;
import org.apache.streampark.console.core.mapper.ManagedFlinkOperationMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

@Transactional
@TestPropertySource(properties = {
        "server.port=0",
        "streampark.managed-flink.enabled=true",
        "streampark.managed-flink.providers.volcengine.enabled=true",
        "streampark.managed-flink.credentials.active-key-version=1",
        "streampark.managed-flink.credentials.master-keys="
            + "1=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
class ManagedFlinkOperationServiceTest extends SpringUnitTestBase {

    private static final long USER_ID = 100000L;
    private static final long TEAM_ID = 100000L;
    private static final String REQUEST_HASH = "a".repeat(64);

    @Autowired
    private CloudAccountService cloudAccountService;

    @Autowired
    private CloudAccountGrantService cloudAccountGrantService;

    @Autowired
    private ManagedFlinkEnvironmentService environmentService;

    @Autowired
    private ManagedFlinkApplicationService applicationService;

    @Autowired
    private ManagedFlinkOperationService operationService;

    @Autowired
    private ManagedFlinkOperationMapper operationMapper;

    @MockBean
    private ManagedFlinkAuditContext auditContext;

    @MockBean
    private ManagedFlinkProviderRegistry providerRegistry;

    private final FakeManagedFlinkProvider fakeProvider =
        new FakeManagedFlinkProvider(Clock.systemUTC(), Duration.ofHours(1));

    @BeforeEach
    void setUp() {
        when(auditContext.currentUserId()).thenReturn(USER_ID);
        when(providerRegistry.getRequired(ManagedFlinkProviderType.VOLCENGINE))
            .thenReturn(fakeProvider);
    }

    @Test
    void shouldReplaySameOperationIntent() {
        Long appId = createApplication("operation-replay");

        ManagedFlinkOperationView accepted =
            operationService.accept(appId, "RELEASE", "release-1", REQUEST_HASH, "{\"version\":1}");
        ManagedFlinkOperationView replay =
            operationService.accept(appId, "RELEASE", "release-1", REQUEST_HASH, "{\"version\":1}");

        assertThat(accepted.getOperationId()).isEqualTo(replay.getOperationId());
        assertThat(accepted.isIdempotentReplay()).isFalse();
        assertThat(replay.isIdempotentReplay()).isTrue();
        assertThat(operationMapper.selectCount(null)).isEqualTo(1);
    }

    @Test
    void shouldRejectReusedKeyWithDifferentRequest() {
        Long appId = createApplication("operation-key-conflict");
        operationService.accept(
            appId, "RELEASE", "release-1", REQUEST_HASH, "{\"version\":1}");

        assertThatExceptionOfType(ApiAlertException.class)
            .isThrownBy(
                () -> operationService.accept(
                    appId,
                    "RELEASE",
                    "release-1",
                    "b".repeat(64),
                    "{\"version\":2}"))
            .withMessageContaining("already used for a different request");
    }

    @Test
    void shouldSerializeActiveWritesPerApplication() {
        Long appId = createApplication("operation-serialization");
        ManagedFlinkOperationView first =
            operationService.accept(appId, "RELEASE", "release-1", REQUEST_HASH, "{\"version\":1}");

        assertThatExceptionOfType(ApiAlertException.class)
            .isThrownBy(
                () -> operationService.accept(
                    appId,
                    "STOP",
                    "stop-1",
                    "b".repeat(64),
                    "{\"savepoint\":false}"))
            .withMessageContaining("still active");

        ManagedFlinkOperation completed = operationMapper.selectById(first.getOperationId());
        completed.setState("SUCCEEDED");
        assertThat(operationMapper.updateById(completed)).isEqualTo(1);

        ManagedFlinkOperationView second =
            operationService.accept(
                appId,
                "STOP",
                "stop-1",
                "b".repeat(64),
                "{\"savepoint\":false}");
        assertThat(second.getOperationId()).isNotEqualTo(first.getOperationId());
    }

    @Test
    void shouldValidateRequestAndApplicationOwnership() {
        assertThatExceptionOfType(ApiAlertException.class)
            .isThrownBy(
                () -> operationService.accept(
                    1L, "RELEASE", "release-1", "not-a-hash", "{}"))
            .withMessageContaining("request hash is invalid");

        Long appId = createApplication("operation-lookup");
        ManagedFlinkOperationView accepted =
            operationService.accept(appId, "RELEASE", "release-1", REQUEST_HASH, "{}");
        assertThat(operationService.getRequired(appId, accepted.getOperationId()).getAppId())
            .isEqualTo(appId);
        assertThatExceptionOfType(ApiAlertException.class)
            .isThrownBy(() -> operationService.getRequired(appId + 1, accepted.getOperationId()))
            .withMessage("Managed Flink operation does not exist.");
    }

    @Test
    void shouldApplyGuardedSuccessfulStateTransitions() {
        Long appId = createApplication("operation-success-transition");
        ManagedFlinkOperationView accepted =
            operationService.accept(appId, "RELEASE", "release-1", REQUEST_HASH, "{}");

        assertThat(operationService.markRunning(accepted.getOperationId())).isTrue();
        assertThat(operationService.markRunning(accepted.getOperationId())).isFalse();
        operationService.recordProviderProgress(
            accepted.getOperationId(),
            "draft-request",
            null,
            "{\"draftId\":\"draft-1\"}");
        operationService.markSucceeded(
            accepted.getOperationId(),
            "deploy-request",
            "deploy-operation",
            "{\"applicationId\":\"application-1\"}");

        ManagedFlinkOperation completed =
            operationService.getRequired(appId, accepted.getOperationId());
        assertThat(completed.getState()).isEqualTo("SUCCEEDED");
        assertThat(completed.getProviderRequestId()).isEqualTo("deploy-request");
        assertThat(completed.getProviderOperationId()).isEqualTo("deploy-operation");
        assertThat(completed.getStartTime()).isNotNull();
        assertThat(completed.getFinishTime()).isNotNull();
        assertThat(completed.getVersion()).isEqualTo(3);
    }

    @Test
    void shouldPersistUnknownProviderOutcomeWithoutRetrying() {
        Long appId = createApplication("operation-unknown-transition");
        ManagedFlinkOperationView accepted =
            operationService.accept(appId, "RELEASE", "release-1", REQUEST_HASH, "{}");

        assertThat(operationService.markRunning(accepted.getOperationId())).isTrue();
        operationService.markFailed(
            accepted.getOperationId(),
            true,
            "provider-request",
            "TRANSIENT:NetworkError",
            "Managed Flink provider outcome is unknown.");

        ManagedFlinkOperation failed =
            operationService.getRequired(appId, accepted.getOperationId());
        assertThat(failed.getState()).isEqualTo("UNKNOWN");
        assertThat(failed.getProviderRequestId()).isEqualTo("provider-request");
        assertThat(failed.getErrorCode()).isEqualTo("TRANSIENT:NetworkError");
        assertThat(failed.getFinishTime()).isNotNull();
    }

    private Long createApplication(String name) {
        Long environmentId = createEnvironment(name + "-env");
        ManagedFlinkApplicationSaveRequest request =
            ManagedFlinkApplicationValidatorTest.request();
        request.setManagedEnvironmentId(environmentId);
        request.setJobName(name);
        request.setSql("SELECT 1");
        return applicationService.create(request);
    }

    private Long createEnvironment(String name) {
        CloudAccountCreateRequest account = new CloudAccountCreateRequest();
        account.setAccountName(name + "-account");
        account.setProviderType("VOLCENGINE");
        account.setRegion("cn-beijing");
        account.setAccessKey("AKLT-" + name);
        account.setSecretKey("secret-" + name);
        Long accountId = cloudAccountService.create(account);

        CloudAccountGrantRequest grant = new CloudAccountGrantRequest();
        grant.setAccountId(accountId);
        grant.setAccountVersion(0);
        grant.setTeamIds(Collections.singletonList(TEAM_ID));
        cloudAccountGrantService.replaceGrants(grant);

        ManagedFlinkEnvironmentCreateRequest environment =
            new ManagedFlinkEnvironmentCreateRequest();
        environment.setTeamId(TEAM_ID);
        environment.setClusterName(name);
        environment.setCloudAccountId(accountId);
        environment.setProjectId("fake-project");
        environment.setResourcePoolId("fake-pool");
        environment.setDraftDirectoryId(1L);
        Long environmentId = environmentService.create(environment);
        environmentService.probe(TEAM_ID, environmentId);
        return environmentId;
    }
}
