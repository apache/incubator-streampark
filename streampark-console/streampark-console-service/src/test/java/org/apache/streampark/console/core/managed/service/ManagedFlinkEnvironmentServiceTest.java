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

import org.apache.streampark.common.enums.ClusterState;
import org.apache.streampark.common.enums.FlinkDeployMode;
import org.apache.streampark.console.SpringUnitTestBase;
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.base.util.JacksonUtils;
import org.apache.streampark.console.core.entity.FlinkCluster;
import org.apache.streampark.console.core.managed.api.ManagedFlinkProviderRegistry;
import org.apache.streampark.console.core.managed.api.ManagedFlinkProviderType;
import org.apache.streampark.console.core.managed.model.CloudAccountCreateRequest;
import org.apache.streampark.console.core.managed.model.CloudAccountGrantRequest;
import org.apache.streampark.console.core.managed.model.ManagedFlinkEnvironmentCreateRequest;
import org.apache.streampark.console.core.managed.model.ManagedFlinkEnvironmentListRequest;
import org.apache.streampark.console.core.managed.model.ManagedFlinkEnvironmentUpdateRequest;
import org.apache.streampark.console.core.managed.model.ManagedFlinkEnvironmentVersionedIdRequest;
import org.apache.streampark.console.core.managed.model.ManagedFlinkEnvironmentView;
import org.apache.streampark.console.core.managed.model.ManagedFlinkMetadataRequest;
import org.apache.streampark.console.core.managed.model.ManagedFlinkResourcePoolRequest;
import org.apache.streampark.console.core.managed.support.FakeManagedFlinkProvider;
import org.apache.streampark.console.core.mapper.FlinkClusterMapper;
import org.apache.streampark.console.core.mapper.ManagedFlinkEnvironmentMapper;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;

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
class ManagedFlinkEnvironmentServiceTest extends SpringUnitTestBase {

    private static final long USER_ID = 100000L;
    private static final long DEFAULT_TEAM_ID = 100000L;
    private static final long TEST_TEAM_ID = 100001L;

    @Autowired
    private CloudAccountService cloudAccountService;

    @Autowired
    private CloudAccountGrantService cloudAccountGrantService;

    @Autowired
    private ManagedFlinkEnvironmentService environmentService;

    @Autowired
    private ManagedFlinkMetadataService metadataService;

    @Autowired
    private FlinkClusterMapper clusterMapper;

    @Autowired
    private ManagedFlinkEnvironmentMapper environmentMapper;

    @MockBean
    private ManagedFlinkAuditContext auditContext;

    @MockBean
    private ManagedFlinkProviderRegistry providerRegistry;

    private final FakeManagedFlinkProvider fakeProvider =
        new FakeManagedFlinkProvider(Clock.systemUTC(), Duration.ofHours(1));

    @BeforeEach
    void setUpAuditUser() {
        when(auditContext.currentUserId()).thenReturn(USER_ID);
        when(providerRegistry.getRequired(ManagedFlinkProviderType.VOLCENGINE))
            .thenReturn(fakeProvider);
    }

    @Test
    void shouldRegisterAndListManagedEnvironmentWithoutLegacyFlinkVersion() {
        Long accountId = createGrantedAccount("environment-registration-account");
        Long clusterId =
            environmentService.create(
                createRequest(
                    accountId, "managed-registration", "fake-project", "fake-pool"));

        ManagedFlinkEnvironmentView view =
            environmentService.get(DEFAULT_TEAM_ID, clusterId);
        assertThat(view.getClusterName()).isEqualTo("managed-registration");
        assertThat(view.getProviderType()).isEqualTo("VOLCENGINE");
        assertThat(view.getRegion()).isEqualTo("cn-beijing");
        assertThat(view.getClusterState()).isEqualTo(ClusterState.CREATED.getState());
        assertThat(view.getVersion()).isZero();
        assertThat(view.getConsoleUrl()).isNull();

        FlinkCluster cluster = clusterMapper.selectById(clusterId);
        assertThat(cluster.getDeployMode())
            .isEqualTo(FlinkDeployMode.MANAGED_APPLICATION.getMode());
        assertThat(cluster.getVersionId()).isNull();
        assertThat(environmentService.list(listRequest(DEFAULT_TEAM_ID)))
            .extracting(ManagedFlinkEnvironmentView::getClusterId)
            .contains(clusterId);
        assertThat(environmentService.list(listRequest(TEST_TEAM_ID))).isEmpty();
    }

    @Test
    void shouldExposeOnlyTeamAuthorizedProviderMetadata() {
        Long accountId = createGrantedAccount("environment-metadata-account");
        ManagedFlinkMetadataRequest metadataRequest = new ManagedFlinkMetadataRequest();
        metadataRequest.setTeamId(DEFAULT_TEAM_ID);
        metadataRequest.setCloudAccountId(accountId);

        assertThat(metadataService.capability(metadataRequest).getProviderType().name())
            .isEqualTo("VOLCENGINE");
        assertThat(metadataService.projects(metadataRequest))
            .extracting(project -> project.getId())
            .containsExactly("fake-project");

        ManagedFlinkResourcePoolRequest poolRequest =
            new ManagedFlinkResourcePoolRequest();
        poolRequest.setTeamId(DEFAULT_TEAM_ID);
        poolRequest.setCloudAccountId(accountId);
        poolRequest.setProjectId("fake-project");
        assertThat(metadataService.resourcePools(poolRequest))
            .extracting(pool -> pool.getId())
            .containsExactly("fake-pool");

        metadataRequest.setTeamId(TEST_TEAM_ID);
        assertThatExceptionOfType(ApiAlertException.class)
            .isThrownBy(() -> metadataService.projects(metadataRequest))
            .withMessage(
                "Team is not authorized to use this managed Flink cloud account.");
    }

    @Test
    void shouldProbeAndPersistCapabilitySnapshot() throws Exception {
        Long accountId = createGrantedAccount("environment-probe-account");
        Long clusterId =
            environmentService.create(
                createRequest(accountId, "managed-probe", "fake-project", "fake-pool"));

        ManagedFlinkEnvironmentView probed =
            environmentService.probe(DEFAULT_TEAM_ID, clusterId);

        assertThat(probed.getClusterState()).isEqualTo(ClusterState.RUNNING.getState());
        assertThat(probed.getProjectName()).isEqualTo("Fake Project");
        assertThat(probed.getResourcePoolName()).isEqualTo("Fake Pool");
        assertThat(probed.getLastProbeTime()).isNotNull();
        assertThat(probed.getLastProbeError()).isNull();
        assertThat(probed.getVersion()).isEqualTo(1);
        Map<String, Object> snapshot =
            JacksonUtils.read(
                probed.getCapabilityJson(),
                new TypeReference<Map<String, Object>>() {
                });
        assertThat(snapshot)
            .containsEntry("providerType", "VOLCENGINE")
            .containsEntry("capabilityRevision", "fake-r1");
    }

    @Test
    void shouldPersistSafeFailureAndUseOptimisticUpdate() {
        Long accountId = createGrantedAccount("environment-failure-account");
        Long clusterId =
            environmentService.create(
                createRequest(
                    accountId, "managed-failure", "fake-project", "missing-pool"));

        ManagedFlinkEnvironmentView failed =
            environmentService.probe(DEFAULT_TEAM_ID, clusterId);
        assertThat(failed.getClusterState()).isEqualTo(ClusterState.FAILED.getState());
        assertThat(failed.getLastProbeError())
            .isEqualTo("VALIDATION:ResourcePoolNotFound");
        assertThat(failed.getLastProbeError()).doesNotContain("secret");

        ManagedFlinkEnvironmentUpdateRequest update =
            updateRequest(failed, "managed-failure-updated", "fake-project", "fake-pool");
        environmentService.update(update);
        ManagedFlinkEnvironmentView updated =
            environmentService.get(DEFAULT_TEAM_ID, clusterId);
        assertThat(updated.getClusterName()).isEqualTo("managed-failure-updated");
        assertThat(updated.getClusterState()).isEqualTo(ClusterState.CREATED.getState());
        assertThat(updated.getLastProbeError()).isNull();
        assertThat(updated.getVersion()).isEqualTo(2);

        assertThatExceptionOfType(ApiAlertException.class)
            .isThrownBy(() -> environmentService.update(update))
            .withMessageContaining("modified by another request");
        assertThatExceptionOfType(ApiAlertException.class)
            .isThrownBy(() -> environmentService.get(TEST_TEAM_ID, clusterId))
            .withMessage(
                "Team is not authorized to use this managed Flink cloud account.");
    }

    @Test
    void shouldDeleteRegistrationAndClusterAtomically() {
        Long accountId = createGrantedAccount("environment-delete-account");
        Long clusterId =
            environmentService.create(
                createRequest(accountId, "managed-delete", "fake-project", "fake-pool"));
        ManagedFlinkEnvironmentVersionedIdRequest request =
            new ManagedFlinkEnvironmentVersionedIdRequest();
        request.setTeamId(DEFAULT_TEAM_ID);
        request.setClusterId(clusterId);
        request.setVersion(0);

        environmentService.delete(request);

        assertThat(environmentMapper.selectById(clusterId)).isNull();
        assertThat(clusterMapper.selectById(clusterId)).isNull();
    }

    private Long createGrantedAccount(String accountName) {
        CloudAccountCreateRequest create = new CloudAccountCreateRequest();
        create.setAccountName(accountName);
        create.setProviderType("VOLCENGINE");
        create.setRegion("cn-beijing");
        create.setAccessKey("AKLT-" + accountName);
        create.setSecretKey("secret-" + accountName);
        Long accountId = cloudAccountService.create(create);

        CloudAccountGrantRequest grant = new CloudAccountGrantRequest();
        grant.setAccountId(accountId);
        grant.setAccountVersion(0);
        grant.setTeamIds(Collections.singletonList(DEFAULT_TEAM_ID));
        cloudAccountGrantService.replaceGrants(grant);
        return accountId;
    }

    private static ManagedFlinkEnvironmentCreateRequest createRequest(
                                                                      Long accountId,
                                                                      String clusterName,
                                                                      String projectId,
                                                                      String poolId) {
        ManagedFlinkEnvironmentCreateRequest request =
            new ManagedFlinkEnvironmentCreateRequest();
        request.setTeamId(DEFAULT_TEAM_ID);
        request.setClusterName(clusterName);
        request.setDescription("managed environment test");
        request.setCloudAccountId(accountId);
        request.setProjectId(projectId);
        request.setResourcePoolId(poolId);
        request.setDraftDirectoryId(1L);
        return request;
    }

    private static ManagedFlinkEnvironmentUpdateRequest updateRequest(
                                                                      ManagedFlinkEnvironmentView view,
                                                                      String clusterName,
                                                                      String projectId,
                                                                      String poolId) {
        ManagedFlinkEnvironmentUpdateRequest request =
            new ManagedFlinkEnvironmentUpdateRequest();
        request.setTeamId(DEFAULT_TEAM_ID);
        request.setClusterId(view.getClusterId());
        request.setVersion(view.getVersion());
        request.setClusterName(clusterName);
        request.setDescription(view.getDescription());
        request.setCloudAccountId(view.getCloudAccountId());
        request.setProjectId(projectId);
        request.setResourcePoolId(poolId);
        request.setDraftDirectoryId(1L);
        return request;
    }

    private static ManagedFlinkEnvironmentListRequest listRequest(Long teamId) {
        ManagedFlinkEnvironmentListRequest request =
            new ManagedFlinkEnvironmentListRequest();
        request.setTeamId(teamId);
        return request;
    }
}
