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
import org.apache.streampark.console.core.entity.ManagedFlinkArtifact;
import org.apache.streampark.console.core.entity.Resource;
import org.apache.streampark.console.core.enums.EngineTypeEnum;
import org.apache.streampark.console.core.enums.ResourceTypeEnum;
import org.apache.streampark.console.core.managed.api.ManagedFlinkProviderRegistry;
import org.apache.streampark.console.core.managed.api.ManagedFlinkProviderType;
import org.apache.streampark.console.core.managed.api.ProviderErrorCategory;
import org.apache.streampark.console.core.managed.model.CloudAccountCreateRequest;
import org.apache.streampark.console.core.managed.model.CloudAccountGrantRequest;
import org.apache.streampark.console.core.managed.model.ManagedFlinkApplicationSaveRequest;
import org.apache.streampark.console.core.managed.model.ManagedFlinkArtifactView;
import org.apache.streampark.console.core.managed.model.ManagedFlinkEnvironmentCreateRequest;
import org.apache.streampark.console.core.managed.support.FakeManagedFlinkProvider;
import org.apache.streampark.console.core.mapper.ManagedFlinkArtifactMapper;
import org.apache.streampark.console.core.mapper.ResourceMapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
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
class ManagedFlinkArtifactServiceTest extends SpringUnitTestBase {

    private static final long USER_ID = 100000L;
    private static final long TEAM_ID = 100000L;

    @Autowired
    private CloudAccountService cloudAccountService;

    @Autowired
    private CloudAccountGrantService cloudAccountGrantService;

    @Autowired
    private ManagedFlinkEnvironmentService environmentService;

    @Autowired
    private ManagedFlinkApplicationService applicationService;

    @Autowired
    private ManagedFlinkArtifactService artifactService;

    @Autowired
    private ManagedFlinkArtifactMapper artifactMapper;

    @Autowired
    private ResourceMapper resourceMapper;

    @MockBean
    private ManagedFlinkAuditContext auditContext;

    @MockBean
    private ManagedFlinkProviderRegistry providerRegistry;

    @TempDir
    Path tempDirectory;

    private final FakeManagedFlinkProvider fakeProvider =
        new FakeManagedFlinkProvider(Clock.systemUTC(), Duration.ofHours(1));

    @BeforeEach
    void setUp() {
        when(auditContext.currentUserId()).thenReturn(USER_ID);
        when(providerRegistry.getRequired(ManagedFlinkProviderType.VOLCENGINE))
            .thenReturn(fakeProvider);
    }

    @Test
    void shouldStageMainJarAndDependencyOncePerChecksum() throws Exception {
        Long environmentId = createEnvironment("artifact-cache-env");
        createResource("main.jar", "main-content");
        createResource("dependency.jar", "dependency-content");
        Long appId =
            createJarApplication(
                environmentId,
                "artifact-cache-app",
                "main.jar",
                Collections.singletonList("dependency.jar"));

        List<ManagedFlinkArtifactView> first =
            artifactService.stageApplicationArtifacts(TEAM_ID, appId);
        List<ManagedFlinkArtifactView> second =
            artifactService.stageApplicationArtifacts(TEAM_ID, appId);

        assertThat(first).hasSize(2).allMatch(item -> "READY".equals(item.getState()));
        assertThat(second)
            .extracting(ManagedFlinkArtifactView::getProviderArtifactId)
            .containsExactlyElementsOf(
                first.stream()
                    .map(ManagedFlinkArtifactView::getProviderArtifactId)
                    .collect(java.util.stream.Collectors.toList()));
        assertThat(fakeProvider.getArtifactStageCount()).isEqualTo(2);
        assertThat(artifactMapper.selectCount(null)).isEqualTo(2);
    }

    @Test
    void shouldReconcileProviderReferenceAfterRetryableTimeout() throws Exception {
        Long environmentId = createEnvironment("artifact-reconcile-env");
        createResource("reconcile.jar", "reconcile-content");
        Long appId =
            createJarApplication(
                environmentId,
                "artifact-reconcile-app",
                "reconcile.jar",
                Collections.emptyList());
        fakeProvider.failArtifactWith(ProviderErrorCategory.TRANSIENT, true);

        List<ManagedFlinkArtifactView> staged =
            artifactService.stageApplicationArtifacts(TEAM_ID, appId);

        assertThat(staged).singleElement()
            .satisfies(
                artifact -> {
                    assertThat(artifact.getState()).isEqualTo("READY");
                    assertThat(artifact.getProviderArtifactVersion()).isEqualTo(1);
                    assertThat(artifact.getProviderUri()).startsWith("tos://");
                });
        assertThat(fakeProvider.getArtifactStageCount()).isEqualTo(1);
    }

    @Test
    void shouldRetryFailedCacheEntryWithoutCreatingDuplicate() throws Exception {
        Long environmentId = createEnvironment("artifact-retry-env");
        createResource("retry.jar", "retry-content");
        Long appId =
            createJarApplication(
                environmentId,
                "artifact-retry-app",
                "retry.jar",
                Collections.emptyList());
        fakeProvider.failArtifactWith(ProviderErrorCategory.TRANSIENT, false);

        assertThatExceptionOfType(ApiAlertException.class)
            .isThrownBy(() -> artifactService.stageApplicationArtifacts(TEAM_ID, appId))
            .withMessageContaining("TRANSIENT:FakeArtifactFailure");
        ManagedFlinkArtifact failed = onlyArtifact();
        assertThat(failed.getState()).isEqualTo("FAILED");
        assertThat(failed.getStageAttempts()).isEqualTo(1);

        fakeProvider.clearArtifactFailure();
        List<ManagedFlinkArtifactView> retried =
            artifactService.stageApplicationArtifacts(TEAM_ID, appId);

        assertThat(retried).singleElement()
            .extracting(ManagedFlinkArtifactView::getState)
            .isEqualTo("READY");
        assertThat(artifactMapper.selectCount(null)).isEqualTo(1);
        assertThat(onlyArtifact().getStageAttempts()).isEqualTo(2);
    }

    private ManagedFlinkArtifact onlyArtifact() {
        return artifactMapper.selectOne(
            new LambdaQueryWrapper<ManagedFlinkArtifact>());
    }

    private void createResource(String name, String content) throws Exception {
        Path file = tempDirectory.resolve(name);
        Files.writeString(file, content, StandardCharsets.UTF_8);
        Resource resource = new Resource();
        resource.setResourceName(name);
        resource.setResourceType(ResourceTypeEnum.APP);
        resource.setResourcePath(name + ":" + file);
        resource.setResource("[\"" + name + ":" + file + "\"]");
        resource.setEngineType(EngineTypeEnum.FLINK);
        resource.setMainClass("org.example.Main");
        resource.setCreatorId(USER_ID);
        resource.setTeamId(TEAM_ID);
        assertThat(resourceMapper.insert(resource)).isEqualTo(1);
    }

    private Long createJarApplication(
                                      Long environmentId,
                                      String jobName,
                                      String jar,
                                      List<String> dependencies) {
        ManagedFlinkApplicationSaveRequest request =
            ManagedFlinkApplicationValidatorTest.request();
        request.setManagedEnvironmentId(environmentId);
        request.setJobName(jobName);
        request.setJobType("STREAMING_JAR");
        request.setSql(null);
        request.setJar(jar);
        request.setMainClass("org.example.Main");
        request.getReleaseConfig().setDependencyResourceNames(dependencies);
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
