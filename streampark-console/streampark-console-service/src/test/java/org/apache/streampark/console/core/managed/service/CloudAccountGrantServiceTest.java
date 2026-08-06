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

import org.apache.streampark.common.enums.FlinkDeployMode;
import org.apache.streampark.console.SpringUnitTestBase;
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.core.entity.FlinkCluster;
import org.apache.streampark.console.core.entity.ManagedFlinkEnvironment;
import org.apache.streampark.console.core.managed.model.CloudAccountCreateRequest;
import org.apache.streampark.console.core.managed.model.CloudAccountGrantRequest;
import org.apache.streampark.console.core.managed.model.CloudAccountTeamGrantView;
import org.apache.streampark.console.core.managed.model.CloudAccountVersionedIdRequest;
import org.apache.streampark.console.core.managed.model.CloudAccountView;
import org.apache.streampark.console.core.mapper.CloudAccountMapper;
import org.apache.streampark.console.core.mapper.FlinkClusterMapper;
import org.apache.streampark.console.core.mapper.ManagedFlinkEnvironmentMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

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
class CloudAccountGrantServiceTest extends SpringUnitTestBase {

    private static final long USER_ID = 100000L;
    private static final long DEFAULT_TEAM_ID = 100000L;
    private static final long TEST_TEAM_ID = 100001L;

    @Autowired
    private CloudAccountService cloudAccountService;

    @Autowired
    private CloudAccountGrantService cloudAccountGrantService;

    @Autowired
    private CloudAccountAuthorizationService authorizationService;

    @Autowired
    private CloudAccountMapper cloudAccountMapper;

    @Autowired
    private FlinkClusterMapper flinkClusterMapper;

    @Autowired
    private ManagedFlinkEnvironmentMapper managedFlinkEnvironmentMapper;

    @MockBean
    private ManagedFlinkAuditContext auditContext;

    @BeforeEach
    void setUpAuditUser() {
        when(auditContext.currentUserId()).thenReturn(USER_ID);
    }

    @Test
    void shouldAtomicallyReplaceAndAuditUseGrants() {
        Long accountId = createAccount("grant-replace-account");
        CloudAccountGrantRequest request =
            grantRequest(
                accountId,
                0,
                List.of(TEST_TEAM_ID, DEFAULT_TEAM_ID, TEST_TEAM_ID));

        cloudAccountGrantService.replaceGrants(request);

        List<CloudAccountTeamGrantView> grants =
            cloudAccountGrantService.listGrants(accountId);
        assertThat(grants)
            .extracting(
                CloudAccountTeamGrantView::getTeamId,
                CloudAccountTeamGrantView::getPermissionLevel,
                CloudAccountTeamGrantView::getCreateUserId)
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple(DEFAULT_TEAM_ID, "USE", USER_ID),
                org.assertj.core.groups.Tuple.tuple(TEST_TEAM_ID, "USE", USER_ID));
        assertThat(grants)
            .allSatisfy(
                grant -> {
                    assertThat(grant.getTeamName()).isNotBlank();
                    assertThat(grant.getCreateTime()).isNotNull();
                });
        assertThat(cloudAccountMapper.selectById(accountId).getVersion()).isEqualTo(1);

        assertThatExceptionOfType(ApiAlertException.class)
            .isThrownBy(() -> cloudAccountGrantService.replaceGrants(request))
            .withMessageContaining("modified by another request");
        assertThat(cloudAccountGrantService.listGrants(accountId)).hasSize(2);
    }

    @Test
    void shouldReturnOnlyEnabledAccountsGrantedToTheRequestedTeam() {
        Long defaultAccountId = createAccount("default-team-account");
        Long testAccountId = createAccount("test-team-account");
        cloudAccountGrantService.replaceGrants(
            grantRequest(defaultAccountId, 0, List.of(DEFAULT_TEAM_ID)));
        cloudAccountGrantService.replaceGrants(
            grantRequest(testAccountId, 0, List.of(TEST_TEAM_ID)));

        assertThat(cloudAccountGrantService.listAvailableAccounts(DEFAULT_TEAM_ID))
            .extracting(CloudAccountView::getId)
            .containsExactly(defaultAccountId);
        assertThat(cloudAccountGrantService.listAvailableAccounts(TEST_TEAM_ID))
            .extracting(CloudAccountView::getId)
            .containsExactly(testAccountId);

        CloudAccountVersionedIdRequest disable = new CloudAccountVersionedIdRequest();
        disable.setId(defaultAccountId);
        disable.setVersion(1);
        cloudAccountService.disable(disable);
        assertThat(cloudAccountGrantService.listAvailableAccounts(DEFAULT_TEAM_ID))
            .isEmpty();
        assertAuthorizationDenied(DEFAULT_TEAM_ID, defaultAccountId);
    }

    @Test
    void shouldApplyRevocationImmediately() {
        Long accountId = createAccount("revoked-account");
        cloudAccountGrantService.replaceGrants(
            grantRequest(accountId, 0, List.of(DEFAULT_TEAM_ID)));
        authorizationService.requireAuthorized(DEFAULT_TEAM_ID, accountId);

        cloudAccountGrantService.replaceGrants(
            grantRequest(accountId, 1, Collections.emptyList()));

        assertThat(cloudAccountGrantService.listGrants(accountId)).isEmpty();
        assertThat(cloudAccountMapper.selectById(accountId).getVersion()).isEqualTo(2);
        assertAuthorizationDenied(DEFAULT_TEAM_ID, accountId);
    }

    @Test
    void shouldAuthorizeEnvironmentByOwningTeamAndDenyOtherTeams() {
        Long accountId = createAccount("environment-authorization-account");
        cloudAccountGrantService.replaceGrants(
            grantRequest(accountId, 0, List.of(DEFAULT_TEAM_ID)));

        FlinkCluster cluster = new FlinkCluster();
        cluster.setClusterName("team-authorized-managed-environment");
        cluster.setDeployMode(FlinkDeployMode.MANAGED_APPLICATION.getMode());
        cluster.setVersionId(USER_ID);
        assertThat(flinkClusterMapper.insert(cluster)).isEqualTo(1);

        ManagedFlinkEnvironment environment = new ManagedFlinkEnvironment();
        environment.setClusterId(cluster.getId());
        environment.setProviderType("VOLCENGINE");
        environment.setCloudAccountId(accountId);
        environment.setRegion("cn-beijing");
        environment.setProviderConfigJson("{\"fixture\":true}");
        environment.setProviderConfigVersion(1);
        environment.setVersion(0);
        assertThat(managedFlinkEnvironmentMapper.insert(environment)).isEqualTo(1);

        authorizationService.requireEnvironmentAuthorized(
            DEFAULT_TEAM_ID, cluster.getId());
        assertThatExceptionOfType(ApiAlertException.class)
            .isThrownBy(
                () -> authorizationService.requireEnvironmentAuthorized(
                    TEST_TEAM_ID, cluster.getId()))
            .withMessage("Team is not authorized to use this managed Flink cloud account.");
    }

    @Test
    void shouldRejectUnknownTeamsWithoutChangingExistingGrants() {
        Long accountId = createAccount("invalid-team-account");
        cloudAccountGrantService.replaceGrants(
            grantRequest(accountId, 0, List.of(DEFAULT_TEAM_ID)));

        CloudAccountGrantRequest invalid =
            grantRequest(accountId, 1, List.of(DEFAULT_TEAM_ID, 999999999L));
        assertThatExceptionOfType(ApiAlertException.class)
            .isThrownBy(() -> cloudAccountGrantService.replaceGrants(invalid))
            .withMessageContaining("do not exist");

        assertThat(cloudAccountMapper.selectById(accountId).getVersion()).isEqualTo(1);
        assertThat(cloudAccountGrantService.listGrants(accountId))
            .extracting(CloudAccountTeamGrantView::getTeamId)
            .containsExactly(DEFAULT_TEAM_ID);
    }

    private void assertAuthorizationDenied(Long teamId, Long accountId) {
        assertThatExceptionOfType(ApiAlertException.class)
            .isThrownBy(() -> authorizationService.requireAuthorized(teamId, accountId))
            .withMessage("Team is not authorized to use this managed Flink cloud account.");
    }

    private Long createAccount(String accountName) {
        CloudAccountCreateRequest request = new CloudAccountCreateRequest();
        request.setAccountName(accountName);
        request.setProviderType("VOLCENGINE");
        request.setRegion("cn-beijing");
        request.setAccessKey("AKLT-" + accountName);
        request.setSecretKey("secret-" + accountName);
        return cloudAccountService.create(request);
    }

    private static CloudAccountGrantRequest grantRequest(
                                                         Long accountId,
                                                         int accountVersion,
                                                         List<Long> teamIds) {
        CloudAccountGrantRequest request = new CloudAccountGrantRequest();
        request.setAccountId(accountId);
        request.setAccountVersion(accountVersion);
        request.setTeamIds(teamIds);
        return request;
    }
}
