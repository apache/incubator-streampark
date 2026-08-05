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
import org.apache.streampark.common.enums.FlinkJobType;
import org.apache.streampark.console.SpringUnitTestBase;
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.core.entity.FlinkApplication;
import org.apache.streampark.console.core.entity.ManagedFlinkApplication;
import org.apache.streampark.console.core.entity.Resource;
import org.apache.streampark.console.core.enums.FlinkAppStateEnum;
import org.apache.streampark.console.core.managed.api.ManagedFlinkProviderRegistry;
import org.apache.streampark.console.core.managed.api.ManagedFlinkProviderType;
import org.apache.streampark.console.core.managed.model.CloudAccountCreateRequest;
import org.apache.streampark.console.core.managed.model.CloudAccountGrantRequest;
import org.apache.streampark.console.core.managed.model.ManagedFlinkApplicationSaveRequest;
import org.apache.streampark.console.core.managed.model.ManagedFlinkApplicationStatisticsView;
import org.apache.streampark.console.core.managed.model.ManagedFlinkApplicationView;
import org.apache.streampark.console.core.managed.model.ManagedFlinkEnvironmentCreateRequest;
import org.apache.streampark.console.core.managed.support.FakeManagedFlinkProvider;
import org.apache.streampark.console.core.mapper.FlinkApplicationMapper;
import org.apache.streampark.console.core.mapper.ManagedFlinkApplicationMapper;
import org.apache.streampark.console.core.service.ResourceService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.util.Collections;
import java.util.Date;

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
class ManagedFlinkApplicationServiceTest extends SpringUnitTestBase {

    private static final long USER_ID = 100000L;
    private static final long TEAM_ID = 100000L;
    private static final long OTHER_TEAM_ID = 100001L;

    @Autowired
    private CloudAccountService cloudAccountService;

    @Autowired
    private CloudAccountGrantService cloudAccountGrantService;

    @Autowired
    private ManagedFlinkEnvironmentService environmentService;

    @Autowired
    private ManagedFlinkApplicationService applicationService;

    @Autowired
    private FlinkApplicationMapper applicationMapper;

    @Autowired
    private ManagedFlinkApplicationMapper managedApplicationMapper;

    @MockBean
    private ManagedFlinkAuditContext auditContext;

    @MockBean
    private ManagedFlinkProviderRegistry providerRegistry;

    @MockBean
    private ResourceService resourceService;

    private final FakeManagedFlinkProvider fakeProvider =
        new FakeManagedFlinkProvider(Clock.systemUTC(), Duration.ofHours(1));

    @BeforeEach
    void setUp() {
        when(auditContext.currentUserId()).thenReturn(USER_ID);
        when(providerRegistry.getRequired(ManagedFlinkProviderType.VOLCENGINE))
            .thenReturn(fakeProvider);
    }

    @Test
    void shouldCreateAndUpdateSqlCandidateWithStableRoutingMetadata() {
        Long environmentId = createEnvironment("managed-app-sql-env", true);
        ManagedFlinkApplicationSaveRequest create =
            request(environmentId, "managed-app-sql", "SELECT 1");

        Long appId = applicationService.create(create);
        ManagedFlinkApplicationView created = applicationService.get(TEAM_ID, appId);

        assertThat(created.getJobType()).isEqualTo("STREAMING_SQL");
        assertThat(created.getSql()).isEqualTo("SELECT 1");
        assertThat(created.getEstimatedCu()).isEqualByComparingTo("97");
        assertThat(created.getLocalDefinitionHash()).hasSize(64);
        assertThat(created.getDeployedDefinitionHash()).isNull();
        assertThat(created.getVersion()).isZero();

        FlinkApplication application = applicationMapper.selectById(appId);
        assertThat(application.getDeployMode())
            .isEqualTo(FlinkDeployMode.MANAGED_APPLICATION.getMode());
        assertThat(application.getJobType()).isEqualTo(FlinkJobType.FLINK_SQL.getMode());
        assertThat(application.getVersionId()).isNull();
        assertThat(application.getFlinkClusterId()).isEqualTo(environmentId);

        ManagedFlinkApplicationSaveRequest update =
            request(environmentId, "managed-app-sql", "SELECT 2");
        update.setAppId(appId);
        update.setVersion(created.getVersion());
        applicationService.update(update);

        ManagedFlinkApplicationView updated = applicationService.get(TEAM_ID, appId);
        assertThat(updated.getSql()).isEqualTo("SELECT 2");
        assertThat(updated.getVersion()).isEqualTo(1);
        assertThat(updated.getLocalDefinitionHash())
            .isNotEqualTo(created.getLocalDefinitionHash());
        assertThat(managedApplicationMapper.selectById(appId).getDeployedDefinitionHash())
            .isNull();

        assertThatExceptionOfType(ApiAlertException.class)
            .isThrownBy(() -> applicationService.update(update))
            .withMessageContaining("modified by another request");
        assertThatExceptionOfType(ApiAlertException.class)
            .isThrownBy(() -> applicationService.get(OTHER_TEAM_ID, appId))
            .withMessage("Managed Flink application does not exist.");
    }

    @Test
    void shouldCopyManagedConfigurationWithoutCloudIdentity() {
        Long environmentId = createEnvironment("managed-copy-env", true);
        Long sourceAppId =
            applicationService.create(
                request(environmentId, "managed-copy-source", "SELECT 1"));
        ManagedFlinkApplication sourceManaged =
            managedApplicationMapper.selectById(sourceAppId);
        sourceManaged.setExternalDraftId("draft-1");
        sourceManaged.setExternalApplicationId("job-1");
        sourceManaged.setExternalInstanceId("instance-1");
        sourceManaged.setDeployedDefinitionHash(sourceManaged.getLocalDefinitionHash());
        assertThat(managedApplicationMapper.updateById(sourceManaged)).isEqualTo(1);

        FlinkApplication target = applicationMapper.selectById(sourceAppId);
        target.setId(null);
        target.setJobName("managed-copy-target");
        target.setJobId(null);
        target.setState(FlinkAppStateEnum.ADDED.getValue());
        target.setCreateTime(new Date());
        target.setModifyTime(new Date());
        assertThat(applicationMapper.insert(target)).isEqualTo(1);

        applicationService.copyLocalConfiguration(sourceAppId, target.getId());

        ManagedFlinkApplication copied =
            managedApplicationMapper.selectById(target.getId());
        assertThat(copied.getManagedEnvId()).isEqualTo(environmentId);
        assertThat(copied.getLocalDefinitionHash())
            .hasSize(64)
            .isNotEqualTo(sourceManaged.getLocalDefinitionHash());
        assertThat(copied.getExternalDraftId()).isNull();
        assertThat(copied.getExternalApplicationId()).isNull();
        assertThat(copied.getExternalInstanceId()).isNull();
        assertThat(copied.getDeployedDefinitionHash()).isNull();
        assertThat(copied.getVersion()).isZero();
    }

    @Test
    void shouldAggregateManagedStatisticsByTeam() {
        Long environmentId = createEnvironment("managed-statistics-env", true);
        Long appId =
            applicationService.create(
                request(environmentId, "managed-statistics-app", "SELECT 1"));

        ManagedFlinkApplicationStatisticsView pending =
            applicationService.statistics(TEAM_ID);
        assertThat(pending.getTotal()).isEqualTo(1);
        assertThat(pending.getRunning()).isZero();
        assertThat(pending.getPending()).isEqualTo(1);

        FlinkApplication application = applicationMapper.selectById(appId);
        application.setState(FlinkAppStateEnum.RUNNING.getValue());
        assertThat(applicationMapper.updateById(application)).isEqualTo(1);
        ManagedFlinkApplication managed = managedApplicationMapper.selectById(appId);
        managed.setSyncState("HEALTHY");
        assertThat(managedApplicationMapper.updateById(managed)).isEqualTo(1);

        ManagedFlinkApplicationStatisticsView healthy =
            applicationService.statistics(TEAM_ID);
        assertThat(healthy.getRunning()).isEqualTo(1);
        assertThat(healthy.getHealthy()).isEqualTo(1);
        assertThat(healthy.getPending()).isZero();
        assertThat(applicationService.statistics(OTHER_TEAM_ID).getTotal()).isZero();
    }

    @Test
    void shouldDeleteOnlyLocalManagedApplicationRecords() {
        Long environmentId = createEnvironment("managed-delete-env", true);
        Long appId =
            applicationService.create(
                request(environmentId, "managed-delete-app", "SELECT 1"));

        applicationService.deleteLocal(appId);

        assertThat(applicationMapper.selectById(appId)).isNull();
        assertThat(managedApplicationMapper.selectById(appId)).isNull();
    }

    @Test
    void shouldRejectLocalDeletionWhileManagedApplicationIsRunning() {
        Long environmentId = createEnvironment("managed-delete-running-env", true);
        Long appId =
            applicationService.create(
                request(environmentId, "managed-delete-running-app", "SELECT 1"));
        FlinkApplication application = applicationMapper.selectById(appId);
        application.setState(FlinkAppStateEnum.RUNNING.getValue());
        assertThat(applicationMapper.updateById(application)).isEqualTo(1);

        assertThatExceptionOfType(ApiAlertException.class)
            .isThrownBy(() -> applicationService.deleteLocal(appId))
            .withMessageContaining("must be stopped");
        assertThat(applicationMapper.selectById(appId)).isNotNull();
        assertThat(managedApplicationMapper.selectById(appId)).isNotNull();
    }

    @Test
    void shouldCreateJarCandidateOnlyForTeamResource() {
        Long environmentId = createEnvironment("managed-app-jar-env", true);
        ManagedFlinkApplicationSaveRequest request =
            request(environmentId, "managed-app-jar", null);
        request.setJobType("STREAMING_JAR");
        request.setJar("managed-test.jar");
        request.setMainClass("org.example.Main");
        Resource resource = new Resource();
        resource.setResourceName("managed-test.jar");
        resource.setTeamId(TEAM_ID);
        when(resourceService.findByResourceName(TEAM_ID, "managed-test.jar"))
            .thenReturn(resource);

        Long appId = applicationService.create(request);

        assertThat(applicationService.get(TEAM_ID, appId))
            .extracting(
                ManagedFlinkApplicationView::getJobType,
                ManagedFlinkApplicationView::getJar,
                ManagedFlinkApplicationView::getMainClass)
            .containsExactly(
                "STREAMING_JAR", "managed-test.jar", "org.example.Main");
    }

    @Test
    void shouldRequireSuccessfullyProbedEnvironment() {
        Long environmentId = createEnvironment("managed-app-unprobed-env", false);

        assertThatExceptionOfType(ApiAlertException.class)
            .isThrownBy(
                () -> applicationService.create(
                    request(environmentId, "managed-app-unprobed", "SELECT 1")))
            .withMessageContaining("must be probed successfully");
        assertThat(managedApplicationMapper.selectCount(null)).isZero();
    }

    private Long createEnvironment(String name, boolean probe) {
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
        if (probe) {
            environmentService.probe(TEAM_ID, environmentId);
        }
        return environmentId;
    }

    private static ManagedFlinkApplicationSaveRequest request(
                                                              Long environmentId,
                                                              String jobName,
                                                              String sql) {
        ManagedFlinkApplicationSaveRequest request =
            ManagedFlinkApplicationValidatorTest.request();
        request.setManagedEnvironmentId(environmentId);
        request.setJobName(jobName);
        request.setSql(sql);
        return request;
    }
}
