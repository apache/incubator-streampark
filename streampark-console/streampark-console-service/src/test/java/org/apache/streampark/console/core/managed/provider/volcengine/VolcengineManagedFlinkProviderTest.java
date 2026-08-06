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

package org.apache.streampark.console.core.managed.provider.volcengine;

import org.apache.streampark.console.core.managed.api.ArtifactLookupRequest;
import org.apache.streampark.console.core.managed.api.ManagedDeployRequest;
import org.apache.streampark.console.core.managed.api.ManagedDraftRequest;
import org.apache.streampark.console.core.managed.api.ManagedFlinkProvider;
import org.apache.streampark.console.core.managed.api.ManagedFlinkProviderException;
import org.apache.streampark.console.core.managed.api.ManagedFlinkProviderMetadataContract;
import org.apache.streampark.console.core.managed.api.ManagedJobLookupRequest;
import org.apache.streampark.console.core.managed.api.ManagedJobRestartRequest;
import org.apache.streampark.console.core.managed.api.ManagedJobRestoreMode;
import org.apache.streampark.console.core.managed.api.ManagedJobStartRequest;
import org.apache.streampark.console.core.managed.api.ManagedJobState;
import org.apache.streampark.console.core.managed.api.ManagedJobStopRequest;
import org.apache.streampark.console.core.managed.api.ManagedSnapshotCreateRequest;
import org.apache.streampark.console.core.managed.api.ManagedSnapshotLookupRequest;
import org.apache.streampark.console.core.managed.api.ManagedSnapshotState;
import org.apache.streampark.console.core.managed.api.ProviderContext;
import org.apache.streampark.console.core.managed.api.ProviderErrorCategory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.volcengine.flink20250101.model.DeployRequestForStartApplicationInstanceInput;
import com.volcengine.flink20250101.model.GetApplicationInstanceResponse;
import com.volcengine.flink20250101.model.RecordForListApplicationInstanceOutput;
import com.volcengine.flink20250101.model.RecordForListGWSApplicationOutput;
import com.volcengine.flink20250101.model.RestartGWSApplicationRequest;
import com.volcengine.flink20250101.model.SavepointInfoForListGWSSavepointOutput;
import com.volcengine.tos.TOSV2;
import com.volcengine.tos.TOSV2ClientBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VolcengineManagedFlinkProviderTest extends ManagedFlinkProviderMetadataContract {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private VolcengineOpenApiClient client;
    private VolcengineManagedFlinkProvider provider;

    @Test
    void shouldInitializeTosTransportWithCompatibleOkioRuntime() throws Exception {
        try (
            TOSV2 tosClient =
                new TOSV2ClientBuilder()
                    .build(
                        "cn-beijing",
                        "https://tos-cn-beijing.volces.com",
                        "test-access-key",
                        "test-secret-key")) {
            assertThat(tosClient).isNotNull();
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        client = mock(VolcengineOpenApiClient.class);
        VolcengineFlinkProperties properties = new VolcengineFlinkProperties();
        properties.setCapabilityTtlMinutes(10);
        provider = new VolcengineManagedFlinkProvider(client, properties);
        when(client.get(
            any(),
            eq("ListGMSProject"),
            eq("2021-06-01"),
            anyMap()))
                .thenAnswer(
                    invocation -> {
                        ProviderContext context = invocation.getArgument(0);
                        if (context.getCloudAccountId() < 0) {
                            throw new ManagedFlinkProviderException(
                                ProviderErrorCategory.AUTHENTICATION,
                                "InvalidCredential",
                                "request-auth",
                                "Credential is invalid");
                        }
                        return response(
                            "{\"Result\":{\"Total\":1,\"ProjectList\":[{"
                                + "\"ProjectId\":\"project-1\","
                                + "\"ProjectName\":\"project-name\","
                                + "\"DisplayName\":\"Project One\"}]}}",
                            "request-project");
                    });
        when(client.get(
            any(),
            eq("ListGMCSResourcePool"),
            eq("2022-06-01"),
            anyMap()))
                .thenReturn(
                    response(
                        "{\"Result\":{\"Total\":1,\"DataList\":[{"
                            + "\"ResourcePoolId\":\"pool-1\","
                            + "\"ResourcePoolName\":\"Pool One\","
                            + "\"Resource\":{\"CapacityCU\":3000,\"UsedCU\":12.5}}]}}",
                        "request-pool"));
        when(client.get(
            any(),
            eq("ListGMSMetaResource"),
            eq("2021-06-01"),
            anyMap()))
                .thenReturn(
                    response(
                        "{\"Result\":{\"Total\":0,\"MetaResourceList\":[]}}",
                        "request-artifact-list"));
        when(client.post(
            any(),
            eq("ListGWSDirectory"),
            eq("2021-06-01"),
            anyMap(),
            any()))
                .thenReturn(
                    response(
                        "{\"Result\":{\"DirectoryTree\":[{"
                            + "\"DirectoryId\":\"2082077328433315800\","
                            + "\"DirectoryName\":\"team\","
                            + "\"ChildDirectoryDtoList\":[{"
                            + "\"DirectoryId\":\"2082077328433315841\","
                            + "\"DirectoryName\":\"StreamPark\"}]}]}}",
                        "request-directory"));
    }

    @Test
    void shouldListDraftDirectoriesByDisplayName() {
        assertThat(provider.listDraftDirectories(validContext(), "project-1", "stream"))
            .singleElement()
            .satisfies(
                directory -> {
                    assertThat(directory.getId()).isEqualTo("2082077328433315841");
                    assertThat(directory.getName()).isEqualTo("StreamPark");
                    assertThat(directory.getPath()).isEqualTo("/team/StreamPark");
                    assertThat(directory.getParentId()).isEqualTo("2082077328433315800");
                });

        verify(client)
            .post(
                eq(validContext()),
                eq("ListGWSDirectory"),
                eq("2021-06-01"),
                argThat(parameters -> "project-1".equals(parameters.get("ProjectId"))),
                eq(Collections.emptyMap()));
    }

    @Test
    void shouldValidateNumericDraftDirectoryIdReturnedAsString() throws Exception {
        when(client.get(
            any(),
            eq("ListGMCSResourcePool"),
            eq("2022-06-01"),
            anyMap()))
                .thenReturn(
                    response(
                        "{\"Result\":{\"Total\":1,\"DataList\":[{"
                            + "\"ResourcePoolId\":\"o-00g0ok9qhjcc\","
                            + "\"ResourcePoolName\":\"Pool One\"}]}}",
                        "request-pool"));

        provider.validateEnvironmentConfig(validContext());
    }

    @Override
    protected ManagedFlinkProvider provider() {
        return provider;
    }

    @Override
    protected ProviderContext validContext() {
        return context(1L);
    }

    @Override
    protected ProviderContext invalidCredentialContext() {
        return context(-1L);
    }

    @Test
    void shouldMapRealMetadataAliasesAndExactApiVersions() {
        assertThat(provider.listProjects(validContext(), "Project"))
            .singleElement()
            .satisfies(
                project -> {
                    assertThat(project.getId()).isEqualTo("project-1");
                    assertThat(project.getName()).isEqualTo("Project One");
                });
        assertThat(provider.listResourcePools(validContext(), "project-1", "Pool"))
            .singleElement()
            .satisfies(
                pool -> {
                    assertThat(pool.getId()).isEqualTo("pool-1");
                    assertThat(pool.getName()).isEqualTo("Pool One");
                    assertThat(pool.getFullName()).isEqualTo("pool-1");
                    assertThat(pool.getTotalCu()).isEqualByComparingTo("3000");
                    assertThat(pool.getUsedCu()).isEqualByComparingTo("12.5");
                });

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> parameters =
            ArgumentCaptor.forClass(Map.class);
        verify(client)
            .get(
                any(),
                eq("ListGMCSResourcePool"),
                eq("2022-06-01"),
                parameters.capture());
        assertThat(parameters.getValue())
            .containsEntry("ProjectId", "project-1")
            .containsEntry("NameKey", "Pool");
    }

    @Test
    void shouldExposeConservativeVerifiedCapability() {
        assertThat(provider.getCapability(validContext()).getJobTypes())
            .containsExactly("STREAMING_SQL", "STREAMING_JAR");
        assertThat(provider.getCapability(validContext()).isSupportsSkipPrecheck()).isFalse();
        assertThat(provider.getCapability(validContext()).isSupportsCustomEndpoint()).isFalse();
        assertThat(provider.getCapability(validContext()).isSupportsJarDirectUpload()).isTrue();
        assertThat(provider.getCapability(validContext()).isSupportsStopWithSnapshot()).isTrue();
        assertThat(provider.getCapability(validContext()).getMemoryPerCpuGiB())
            .isEqualByComparingTo("4");
    }

    @Test
    void shouldReturnNullWhenArtifactDoesNotExist() {
        assertThat(
            provider.findArtifact(
                validContext(),
                ArtifactLookupRequest.builder()
                    .checksum(
                        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                            + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
                    .contentAddressedName("streampark-a.jar")
                    .build()))
                        .isNull();
    }

    @Test
    void shouldResolveRegisteredArtifactAndLatestVersion() throws Exception {
        when(client.get(
            any(),
            eq("ListGMSMetaResource"),
            eq("2021-06-01"),
            anyMap()))
                .thenReturn(
                    response(
                        "{\"Result\":{\"MetaResourceList\":[{"
                            + "\"Id\":\"resource-1\",\"Name\":\"streampark-a.jar\","
                            + "\"Uri\":\"tos://bucket/original\"}]}}",
                        "request-artifact-list"));
        when(client.get(
            any(),
            eq("ListGWSResourceVersion"),
            eq("2021-06-01"),
            anyMap()))
                .thenReturn(
                    response(
                        "{\"Result\":{\"ResourceVersionList\":[{"
                            + "\"VersionNum\":1,\"Uri\":\"tos://bucket/v1\"},{"
                            + "\"VersionNum\":2,\"Uri\":\"tos://bucket/v2\"}]}}",
                        "request-artifact-version"));

        assertThat(
            provider.findArtifact(
                validContext(),
                ArtifactLookupRequest.builder()
                    .checksum(
                        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                            + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
                    .contentAddressedName("streampark-a.jar")
                    .build()))
                        .satisfies(
                            artifact -> {
                                assertThat(artifact.getProviderArtifactId())
                                    .isEqualTo("resource-1");
                                assertThat(artifact.getProviderArtifactVersion()).isEqualTo(2);
                                assertThat(artifact.getProviderUri())
                                    .isEqualTo("tos://bucket/v2");
                            });
    }

    @Test
    void shouldTranslateProviderNeutralDraftEnums() {
        assertThat(VolcengineManagedFlinkProvider.providerJobType("STREAMING_SQL"))
            .isEqualTo("FLINK_STREAMING_SQL");
        assertThat(VolcengineManagedFlinkProvider.providerJobType("STREAMING_JAR"))
            .isEqualTo("FLINK_STREAMING_JAR");
        assertThat(VolcengineManagedFlinkProvider.providerEngineVersion("1.20"))
            .isEqualTo("FLINK_VERSION_1_20");
        assertThat(
            VolcengineManagedFlinkProvider.providerEngineVersion("FLINK_VERSION_1_19"))
                .isEqualTo("FLINK_VERSION_1_19");
    }

    @Test
    void shouldMapDraftDirectoryAndResourcePoolRouting() {
        assertThat(
            VolcengineManagedFlinkProvider.createRequestBody(
                ManagedDraftRequest.builder()
                    .jobName("job-1")
                    .jobType("STREAMING_SQL")
                    .engineVersion("1.17")
                    .build(),
                config()))
                    .containsEntry("ProjectId", "project-1")
                    .containsEntry("DirectoryId", 2082077328433315841L)
                    .containsEntry("JobType", "FLINK_STREAMING_SQL")
                    .containsEntry("EngineVersion", "FLINK_VERSION_1_17");

        assertThat(
            VolcengineManagedFlinkProvider.createRequest(
                ManagedDraftRequest.builder()
                    .jobName("job-1")
                    .jobType("STREAMING_SQL")
                    .engineVersion("1.17")
                    .build(),
                config()))
                    .satisfies(
                        request -> {
                            assertThat(request.getDirectoryId())
                                .isEqualTo(2082077328433315841L);
                            assertThat(request.getJobType())
                                .isEqualTo("FLINK_STREAMING_SQL");
                            assertThat(request.getEngineVersion())
                                .isEqualTo("FLINK_VERSION_1_17");
                        });

        assertThat(
            VolcengineManagedFlinkProvider.deployRequest(
                ManagedDeployRequest.builder()
                    .draftId("draft-1")
                    .schedulePolicy("GANG")
                    .build(),
                config()))
                    .satisfies(
                        request -> {
                            assertThat(request.getResourcePool()).isEqualTo("paimon-test2");
                            assertThat(request.getQueue()).isEqualTo("o-00g0ok9qhjcc");
                        });
    }

    @Test
    void shouldPreserveProviderDraftFieldsAndFlattenDynamicOptions() throws Exception {
        String providerDraft =
            "{"
                + "\"Id\":\"draft-1\","
                + "\"ProjectId\":\"project-1\","
                + "\"AccountId\":\"account-1\","
                + "\"UserId\":\"user-1\","
                + "\"Platform\":\"StreamX\","
                + "\"JobId\":\"job-uuid-1\","
                + "\"State\":\"CREATED\","
                + "\"ResourceVersion\":\"0\","
                + "\"CreateTime\":\"2026-08-04 18:55:11\""
                + "}";
        String runtimeConfig =
            "{"
                + "\"resource\":{"
                + "\"parallelism\":2,"
                + "\"taskManagerCpu\":1,"
                + "\"taskManagerMemoryGiB\":4,"
                + "\"taskManagerSlots\":2,"
                + "\"jobManagerCpu\":1,"
                + "\"jobManagerMemoryGiB\":4},"
                + "\"checkpoint\":{"
                + "\"enabled\":true,"
                + "\"intervalMs\":300000,"
                + "\"timeoutMs\":600000,"
                + "\"backend\":\"rocksdb\"},"
                + "\"restartStrategy\":{"
                + "\"type\":\"exponential-delay\","
                + "\"parameters\":{"
                + "\"restart-strategy.exponential-delay.initial-backoff\":\"1s\"}},"
                + "\"retryOnFailure\":true,"
                + "\"retryIntervalMin\":1,"
                + "\"retryMaxCount\":3,"
                + "\"customProperties\":{\"custom.runtime\":\"runtime\"}"
                + "}";

        JsonNode body =
            provider.updateRequestBody(
                objectMapper.readTree(providerDraft),
                ManagedDraftRequest.builder()
                    .jobName("updated-name")
                    .jobType("STREAMING_SQL")
                    .engineVersion("1.17")
                    .sqlText("SELECT 1")
                    .optionsJson(runtimeConfig)
                    .dynamicOptionsJson(
                        "{\"custom.runtime\":\"release\",\"paimon.connector.version\":\"1.1\"}")
                    .dependencyJson("[]")
                    .build(),
                config());

        assertThat(body.path("AccountId").asText()).isEqualTo("account-1");
        assertThat(body.path("UserId").asText()).isEqualTo("user-1");
        assertThat(body.path("Platform").asText()).isEqualTo("StreamX");
        assertThat(body.path("JobId").asText()).isEqualTo("job-uuid-1");
        assertThat(body.path("ResourceVersion").asText()).isEqualTo("0");
        assertThat(body.path("Options").asText()).isEqualTo("{}");
        assertThat(body.path("JobName").asText()).isEqualTo("updated-name");
        assertThat(body.path("SqlText").asText()).isEqualTo("SELECT 1");
        assertThat(body.path("Dependency").asText()).isEqualTo("[]");

        JsonNode dynamicOptions = objectMapper.readTree(body.path("DynamicOptions").asText());
        assertThat(dynamicOptions.path("parallelism.default").asText()).isEqualTo("2");
        assertThat(dynamicOptions.path("jobmanager.memory.process.size").asText())
            .isEqualTo("4096mb");
        assertThat(dynamicOptions.path("taskmanager.memory.process.size").asText())
            .isEqualTo("4096mb");
        assertThat(dynamicOptions.path("execution.checkpointing.interval").asText())
            .isEqualTo("300s");
        assertThat(dynamicOptions.path("execution.checkpointing.timeout").asText())
            .isEqualTo("600s");
        assertThat(dynamicOptions.path("restart-strategy").asText())
            .isEqualTo("exponential-delay");
        assertThat(dynamicOptions.path("restart.attempt.enable").asText()).isEqualTo("true");
        assertThat(dynamicOptions.path("restart.attempt.interval.min").asText()).isEqualTo("1");
        assertThat(dynamicOptions.path("restart.attempt.max.count").asText()).isEqualTo("3");
        assertThat(dynamicOptions.path("custom.runtime").asText()).isEqualTo("release");
        assertThat(dynamicOptions.path("paimon.connector.version").asText()).isEqualTo("1.1");
    }

    @Test
    void shouldBuildJarDraftUpdateWithMainArtifactAndEntrypoint() throws Exception {
        JsonNode body =
            provider.updateRequestBody(
                objectMapper.readTree(
                    "{"
                        + "\"Id\":\"draft-1\","
                        + "\"ProjectId\":\"project-1\","
                        + "\"AccountId\":\"account-1\","
                        + "\"UserId\":\"user-1\","
                        + "\"Platform\":\"StreamX\","
                        + "\"JobId\":\"job-uuid-1\","
                        + "\"State\":\"CREATED\","
                        + "\"ResourceVersion\":\"0\","
                        + "\"CreateTime\":\"2026-08-05 19:58:16\""
                        + "}"),
                ManagedDraftRequest.builder()
                    .jobName("jar-job")
                    .jobType("STREAMING_JAR")
                    .engineVersion("FLINK_VERSION_1_17")
                    .jar("file-resource-id")
                    .mainClass("com.main.Main")
                    .args("--key1 value1 --key2 value2")
                    .optionsJson("{}")
                    .dynamicOptionsJson("{}")
                    .dependencyJson("{\"jars\":[]}")
                    .build(),
                config());

        assertThat(body.path("JobType").asText()).isEqualTo("FLINK_STREAMING_JAR");
        assertThat(body.path("ResourceVersion").asText()).isEqualTo("0");
        assertThat(body.path("Jar").asText()).isEqualTo("file-resource-id");
        assertThat(body.path("MainClass").asText()).isEqualTo("com.main.Main");
        assertThat(body.path("Args").asText()).isEqualTo("--key1 value1 --key2 value2");
        assertThat(body.path("Dependency").asText()).isEqualTo("{\"jars\":[]}");
        assertThat(body.has("SqlText")).isFalse();
    }

    @Test
    void shouldGetExistingDraftWithLegacyPostProtocolBeforeUpdate() throws Exception {
        when(client.postOnce(
            any(),
            eq("GetGWSApplicationDraft"),
            eq("2021-06-01"),
            anyMap(),
            any()))
                .thenReturn(
                    response(
                        "{\"Result\":{"
                            + "\"Id\":\"draft-1\","
                            + "\"AccountId\":\"account-1\","
                            + "\"UserId\":\"user-1\","
                            + "\"Platform\":\"StreamX\","
                            + "\"JobId\":\"job-uuid-1\","
                            + "\"State\":\"CREATED\","
                            + "\"ResourceVersion\":\"0\","
                            + "\"CreateTime\":\"2026-08-04 18:55:11\"}}",
                        "request-get-draft"));
        when(client.postOnce(
            any(),
            eq("UpdateGWSApplicationDraft"),
            eq("2021-06-01"),
            anyMap(),
            any()))
                .thenReturn(response("{\"Result\":{\"Success\":true}}", "request-update"));

        provider.upsertDraft(
            validContext(),
            ManagedDraftRequest.builder()
                .existingDraftId("draft-1")
                .jobName("job-1")
                .jobType("STREAMING_SQL")
                .engineVersion("1.17")
                .sqlText("SELECT 1")
                .optionsJson("{}")
                .dynamicOptionsJson("{}")
                .dependencyJson("[]")
                .definitionHash("definition-hash")
                .build());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> query = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Object> body = ArgumentCaptor.forClass(Object.class);
        verify(client)
            .postOnce(
                any(),
                eq("GetGWSApplicationDraft"),
                eq("2021-06-01"),
                query.capture(),
                body.capture());
        assertThat(query.getValue())
            .hasSize(1)
            .containsEntry("ProjectId", "project-1");
        assertThat(body.getValue()).isEqualTo(Map.of("Id", "draft-1"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> updateQuery = ArgumentCaptor.forClass(Map.class);
        verify(client)
            .postOnce(
                any(),
                eq("UpdateGWSApplicationDraft"),
                eq("2021-06-01"),
                updateQuery.capture(),
                any());
        assertThat(updateQuery.getValue())
            .hasSize(1)
            .containsEntry("ProjectId", "project-1");
    }

    @Test
    void shouldMapStartRestoreStrategiesToExactSdkValues() {
        assertThat(
            VolcengineManagedFlinkProvider.startRequest(
                ManagedJobStartRequest.builder()
                    .jobId("job-1")
                    .priority("5")
                    .schedulePolicy("GANG")
                    .scheduleTimeoutSeconds(300)
                    .restoreMode(ManagedJobRestoreMode.FRESH)
                    .build(),
                config()))
                    .satisfies(
                        request -> {
                            assertThat(request.getId()).isEqualTo("job-1");
                            assertThat(request.getApp().getDeployRequest().getResourcePool())
                                .isEqualTo("paimon-test2");
                            assertThat(request.getApp().getDeployRequest().getQueue())
                                .isEqualTo("o-00g0ok9qhjcc");
                            assertThat(request.getApp().getDeployRequest().getPriority())
                                .isEqualTo("5");
                            assertThat(request.getApp().getDeployRequest().getSchedulePolicy())
                                .isEqualTo(
                                    DeployRequestForStartApplicationInstanceInput.SchedulePolicyEnum.GANG);
                            assertThat(
                                request.getApp().getDeployRequest().getScheduleTimeout())
                                    .isEqualTo("300");
                            assertThat(request.getRestoreStrategy().getType())
                                .isEqualTo("FROM_NEW");
                            assertThat(request.getRestoreStrategy().getSavepointId()).isNull();
                        });

        assertThat(
            VolcengineManagedFlinkProvider.startRequest(
                ManagedJobStartRequest.builder()
                    .jobId("job-1")
                    .restoreMode(ManagedJobRestoreMode.SPECIFIED_SNAPSHOT)
                    .snapshotId("savepoint-1")
                    .build(),
                config()))
                    .satisfies(
                        request -> {
                            assertThat(request.getRestoreStrategy().getType())
                                .isEqualTo("FROM_SAVEPOINT");
                            assertThat(request.getRestoreStrategy().getSavepointId())
                                .isEqualTo("savepoint-1");
                        });
    }

    @Test
    void shouldMapStopToInstanceIdRatherThanStableJobId() {
        assertThat(
            VolcengineManagedFlinkProvider.stopRequest(
                ManagedJobStopRequest.builder()
                    .jobId("job-1")
                    .instanceId("s-instance-1")
                    .build())
                .getInstanceId())
                    .isEqualTo("s-instance-1");
    }

    @Test
    void shouldPreferStableListRecordIdOverRuntimeApplicationId() {
        RecordForListGWSApplicationOutput record =
            new RecordForListGWSApplicationOutput();
        record.setId("job-1");
        record.setApplicationId("s-runtime-1");

        assertThat(VolcengineManagedFlinkProvider.providerJobId(record))
            .isEqualTo("job-1");
    }

    @Test
    void shouldMapProviderLifecycleStatesConservatively() {
        assertThat(VolcengineManagedFlinkProvider.providerJobState("DEPLOYING"))
            .isEqualTo(ManagedJobState.STARTING);
        assertThat(VolcengineManagedFlinkProvider.providerJobState("RUNNING"))
            .isEqualTo(ManagedJobState.RUNNING);
        assertThat(VolcengineManagedFlinkProvider.providerJobState("CANCELLING"))
            .isEqualTo(ManagedJobState.STOPPING);
        assertThat(VolcengineManagedFlinkProvider.providerJobState("CANCELED"))
            .isEqualTo(ManagedJobState.STOPPED);
        assertThat(VolcengineManagedFlinkProvider.providerJobState("SUSPENDED"))
            .isEqualTo(ManagedJobState.SUSPENDED);
        assertThat(VolcengineManagedFlinkProvider.providerJobState("new-provider-state"))
            .isEqualTo(ManagedJobState.OTHER);
    }

    @Test
    void shouldGetJobThroughStableReadOnlyOpenApi() throws Exception {
        when(client.post(
            any(),
            eq("GetGWSApplication"),
            eq("2021-06-01"),
            anyMap(),
            any()))
                .thenReturn(
                    response(
                        "{\"Result\":{\"Id\":\"job-1\",\"JobName\":\"job-name\","
                            + "\"State\":\"RUNNING\"}}",
                        "request-job"));

        assertThat(
            provider.getJob(
                validContext(),
                ManagedJobLookupRequest.builder()
                    .jobName("job-name")
                    .jobId("job-1")
                    .build()))
                        .satisfies(
                            status -> {
                                assertThat(status.getJobId()).isEqualTo("job-1");
                                assertThat(status.getState()).isEqualTo(ManagedJobState.RUNNING);
                                assertThat(status.getProviderState()).isEqualTo("RUNNING");
                                assertThat(status.getProviderRequestId())
                                    .isEqualTo("request-job");
                            });
        verify(client)
            .post(
                any(),
                eq("GetGWSApplication"),
                eq("2021-06-01"),
                eq(Map.of("ProjectId", "project-1")),
                eq(Map.of("AccountId", "", "Id", "job-1")));
    }

    @Test
    void shouldSelectRunningInstanceWhenRestartHasCreatedANewInstance() {
        assertThat(
            VolcengineManagedFlinkProvider.selectLatestInstanceId(
                List.of(
                    new RecordForListApplicationInstanceOutput()
                        .id("new-instance")
                        .state("RUNNING"),
                    new RecordForListApplicationInstanceOutput()
                        .id("old-instance")
                        .state("STOPPED")),
                "RUNNING"))
                    .isEqualTo("new-instance");
    }

    @Test
    void shouldBuildApplicationConsoleUrlFromInstanceMetadata() {
        GetApplicationInstanceResponse instance =
            new GetApplicationInstanceResponse()
                .id("2083124027591327745")
                .applicationId("s-2083124027591327745")
                .deploymentId("gts-job-uuid");

        assertThat(
            VolcengineManagedFlinkProvider.consoleUrl(
                validContext(), "2083106196409372673", instance))
                    .isEqualTo(
                        "https://console.volcengine.com/flink/"
                            + "region:flink+cn-beijing/project/project-1/job/manage/"
                            + "2083106196409372673/detail?"
                            + "ClusterId=s-2083124027591327745"
                            + "&GtsJobUuid=gts-job-uuid"
                            + "&AppId=2083106196409372673");
    }

    @Test
    void shouldMapRestartRestoreStrategiesToExactSdkEnum() {
        assertThat(
            VolcengineManagedFlinkProvider.restartRequest(
                ManagedJobRestartRequest.builder()
                    .jobId("job-1")
                    .restoreMode(ManagedJobRestoreMode.LATEST_STATE)
                    .build()))
                        .satisfies(
                            request -> {
                                assertThat(request.getId()).isEqualTo("job-1");
                                assertThat(request.getType())
                                    .isEqualTo(RestartGWSApplicationRequest.TypeEnum.FROM_LATEST);
                                assertThat(request.getSavepointId()).isNull();
                            });
    }

    @Test
    void shouldStopWithSnapshotThroughLegacyGwsAction() throws Exception {
        when(client.postOnce(
            any(),
            eq("StopGWSApplicationWithSp"),
            eq("2021-06-01"),
            anyMap(),
            any()))
                .thenReturn(
                    response(
                        "{\"Result\":{\"Success\":true,\"Id\":\"job-1\","
                            + "\"InstanceId\":\"s-instance-1\"}}",
                        "request-stop-with-sp"));

        assertThat(
            provider.stopJob(
                validContext(),
                ManagedJobStopRequest.builder()
                    .jobId("job-1")
                    .instanceId("s-instance-1")
                    .withSnapshot(true)
                    .build()))
                        .satisfies(
                            result -> {
                                assertThat(result.getJobId()).isEqualTo("job-1");
                                assertThat(result.getInstanceId()).isEqualTo("s-instance-1");
                                assertThat(result.getProviderRequestId())
                                    .isEqualTo("request-stop-with-sp");
                                assertThat(result.getProviderState()).isEqualTo("STOPPING");
                            });

        verify(client)
            .postOnce(
                eq(validContext()),
                eq("StopGWSApplicationWithSp"),
                eq("2021-06-01"),
                eq(Map.of("ProjectId", "project-1")),
                eq(Map.of("Id", "job-1")));
    }

    @Test
    void shouldMapSnapshotRequestsAndProviderStates() {
        assertThat(
            VolcengineManagedFlinkProvider.snapshotLookupRequest(
                ManagedSnapshotLookupRequest.builder()
                    .jobId("job-1")
                    .build())
                .getId())
                    .isEqualTo("job-1");
        assertThat(
            VolcengineManagedFlinkProvider.snapshotCreateRequest(
                ManagedSnapshotCreateRequest.builder()
                    .jobId("job-1")
                    .instanceId("s-instance-1")
                    .description("manual")
                    .build()))
                        .satisfies(
                            request -> {
                                assertThat(request.getId()).isEqualTo("job-1");
                                assertThat(request.getDescription()).isEqualTo("manual");
                            });
        assertThat(VolcengineManagedFlinkProvider.snapshotState("RUNNING"))
            .isEqualTo(ManagedSnapshotState.CREATING);
        assertThat(VolcengineManagedFlinkProvider.snapshotState("COMPLETED"))
            .isEqualTo(ManagedSnapshotState.COMPLETED);
        assertThat(VolcengineManagedFlinkProvider.snapshotState("FAILED"))
            .isEqualTo(ManagedSnapshotState.FAILED);
        assertThat(VolcengineManagedFlinkProvider.snapshotState("FAIL"))
            .isEqualTo(ManagedSnapshotState.FAILED);
        assertThat(VolcengineManagedFlinkProvider.snapshotState("future-state"))
            .isEqualTo(ManagedSnapshotState.OTHER);
    }

    @Test
    void shouldMapSnapshotWithoutLeakingSdkModel() {
        SavepointInfoForListGWSSavepointOutput item =
            new SavepointInfoForListGWSSavepointOutput();
        item.setSavepointId("savepoint-1");
        item.setCreateType("MANUAL");
        item.setStatus("COMPLETED");
        item.setSavepointPath("tos://managed/savepoint-1");
        item.setDescription("manual");
        item.setCreateTime("2026-07-30T10:00:00Z");
        item.setCompeleteTime("2026-07-30T10:00:05Z");

        assertThat(VolcengineManagedFlinkProvider.snapshot(item))
            .satisfies(
                snapshot -> {
                    assertThat(snapshot.getSnapshotId()).isEqualTo("savepoint-1");
                    assertThat(snapshot.getState()).isEqualTo(ManagedSnapshotState.COMPLETED);
                    assertThat(snapshot.getLocation()).isEqualTo("tos://managed/savepoint-1");
                    assertThat(snapshot.getCompletionTime())
                        .isEqualTo("2026-07-30T10:00:05Z");
                });
    }

    private VolcengineOpenApiResponse response(String json, String requestId) throws Exception {
        return new VolcengineOpenApiResponse(objectMapper.readTree(json), requestId);
    }

    private static ProviderContext context(long accountId) {
        return ProviderContext.builder()
            .cloudAccountId(accountId)
            .credentialVersion(0L)
            .region("cn-beijing")
            .providerConfigJson(
                "{\"projectId\":\"project-1\",\"projectName\":\"Project\","
                    + "\"resourcePoolId\":\"o-00g0ok9qhjcc\","
                    + "\"resourcePoolName\":\"paimon-test2\","
                    + "\"draftDirectoryId\":2082077328433315841,"
                    + "\"tosBucket\":\"test-flink-bucket\"}")
            .providerConfigVersion(1)
            .build();
    }

    private static VolcengineEnvironmentConfig config() {
        VolcengineEnvironmentConfig config = new VolcengineEnvironmentConfig();
        config.setProjectId("project-1");
        config.setResourcePoolId("o-00g0ok9qhjcc");
        config.setResourcePoolName("paimon-test2");
        config.setDraftDirectoryId(2082077328433315841L);
        config.setTosBucket("test-flink-bucket");
        return config;
    }
}
