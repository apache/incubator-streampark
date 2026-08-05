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
import org.apache.streampark.console.core.managed.api.ArtifactStageRequest;
import org.apache.streampark.console.core.managed.api.CloudProject;
import org.apache.streampark.console.core.managed.api.CredentialCheckResult;
import org.apache.streampark.console.core.managed.api.ManagedDeployRequest;
import org.apache.streampark.console.core.managed.api.ManagedDeployment;
import org.apache.streampark.console.core.managed.api.ManagedDeploymentLookupRequest;
import org.apache.streampark.console.core.managed.api.ManagedDraft;
import org.apache.streampark.console.core.managed.api.ManagedDraftDirectory;
import org.apache.streampark.console.core.managed.api.ManagedDraftRequest;
import org.apache.streampark.console.core.managed.api.ManagedFlinkCapability;
import org.apache.streampark.console.core.managed.api.ManagedFlinkProvider;
import org.apache.streampark.console.core.managed.api.ManagedFlinkProviderException;
import org.apache.streampark.console.core.managed.api.ManagedFlinkProviderType;
import org.apache.streampark.console.core.managed.api.ManagedJobActionResult;
import org.apache.streampark.console.core.managed.api.ManagedJobLookupRequest;
import org.apache.streampark.console.core.managed.api.ManagedJobRestartRequest;
import org.apache.streampark.console.core.managed.api.ManagedJobRestoreMode;
import org.apache.streampark.console.core.managed.api.ManagedJobStartRequest;
import org.apache.streampark.console.core.managed.api.ManagedJobState;
import org.apache.streampark.console.core.managed.api.ManagedJobStatus;
import org.apache.streampark.console.core.managed.api.ManagedJobStopRequest;
import org.apache.streampark.console.core.managed.api.ManagedResourcePool;
import org.apache.streampark.console.core.managed.api.ManagedSnapshot;
import org.apache.streampark.console.core.managed.api.ManagedSnapshotCreateRequest;
import org.apache.streampark.console.core.managed.api.ManagedSnapshotCreateResult;
import org.apache.streampark.console.core.managed.api.ManagedSnapshotLookupRequest;
import org.apache.streampark.console.core.managed.api.ManagedSnapshotState;
import org.apache.streampark.console.core.managed.api.ProviderContext;
import org.apache.streampark.console.core.managed.api.ProviderErrorCategory;
import org.apache.streampark.console.core.managed.api.StagedArtifact;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.volcengine.ApiException;
import com.volcengine.ApiResponse;
import com.volcengine.flink20250101.model.AppForStartApplicationInstanceInput;
import com.volcengine.flink20250101.model.CancelApplicationInstanceRequest;
import com.volcengine.flink20250101.model.CancelApplicationInstanceResponse;
import com.volcengine.flink20250101.model.CreateGWSApplicationDraftRequest;
import com.volcengine.flink20250101.model.CreateGWSSavepointRequest;
import com.volcengine.flink20250101.model.CreateGWSSavepointResponse;
import com.volcengine.flink20250101.model.DeployGWSApplicationDraftRequest;
import com.volcengine.flink20250101.model.DeployGWSApplicationDraftResponse;
import com.volcengine.flink20250101.model.DeployRequestForStartApplicationInstanceInput;
import com.volcengine.flink20250101.model.GetApplicationInstanceRequest;
import com.volcengine.flink20250101.model.GetApplicationInstanceResponse;
import com.volcengine.flink20250101.model.ListApplicationInstanceRequest;
import com.volcengine.flink20250101.model.ListApplicationInstanceResponse;
import com.volcengine.flink20250101.model.ListGWSApplicationRequest;
import com.volcengine.flink20250101.model.ListGWSApplicationResponse;
import com.volcengine.flink20250101.model.ListGWSSavepointRequest;
import com.volcengine.flink20250101.model.ListGWSSavepointResponse;
import com.volcengine.flink20250101.model.RecordForListApplicationInstanceOutput;
import com.volcengine.flink20250101.model.RecordForListGWSApplicationOutput;
import com.volcengine.flink20250101.model.RestartGWSApplicationRequest;
import com.volcengine.flink20250101.model.RestartGWSApplicationResponse;
import com.volcengine.flink20250101.model.RestoreStrategyForStartApplicationInstanceInput;
import com.volcengine.flink20250101.model.SavepointInfoForListGWSSavepointOutput;
import com.volcengine.flink20250101.model.StartApplicationInstanceRequest;
import com.volcengine.flink20250101.model.StartApplicationInstanceResponse;
import com.volcengine.model.ResponseMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Volcengine implementation of the provider-neutral managed Flink metadata contract. */
@Slf4j
@Component
public class VolcengineManagedFlinkProvider implements ManagedFlinkProvider {

    private static final int PAGE_SIZE = 200;
    private static final int MAX_PAGES = 100;

    private static final String PROJECT_ACTION = "ListGMSProject";
    private static final String PROJECT_API_VERSION = "2021-06-01";
    private static final String RESOURCE_POOL_ACTION = "ListGMCSResourcePool";
    private static final String RESOURCE_POOL_API_VERSION = "2022-06-01";
    private static final String DRAFT_DIRECTORY_ACTION = "ListGWSDirectory";
    private static final String DRAFT_DIRECTORY_API_VERSION = "2021-06-01";
    private static final String FLINK_API_VERSION = "2025-01-01";
    private static final String DRAFT_WRITE_API_VERSION = "2021-06-01";
    private static final String DRAFT_GET_ACTION = "GetGWSApplicationDraft";
    private static final String DRAFT_GET_API_VERSION = "2021-06-01";
    private static final String JOB_GET_ACTION = "GetGWSApplication";
    private static final String JOB_GET_API_VERSION = "2021-06-01";

    private final VolcengineOpenApiClient openApiClient;
    private final VolcengineFlinkProperties properties;
    private final ObjectMapper objectMapper;
    private final VolcengineCredentialResolver credentialResolver;
    private final VolcengineSdkClientFactory sdkClientFactory;

    @Autowired
    public VolcengineManagedFlinkProvider(
                                          VolcengineOpenApiClient openApiClient,
                                          VolcengineFlinkProperties properties,
                                          ObjectMapper objectMapper,
                                          VolcengineCredentialResolver credentialResolver,
                                          VolcengineSdkClientFactory sdkClientFactory) {
        this.openApiClient = openApiClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.credentialResolver = credentialResolver;
        this.sdkClientFactory = sdkClientFactory;
    }

    VolcengineManagedFlinkProvider(
                                   VolcengineOpenApiClient openApiClient,
                                   VolcengineFlinkProperties properties) {
        this(openApiClient, properties, new ObjectMapper(), null, null);
    }

    @Override
    public ManagedFlinkProviderType type() {
        return ManagedFlinkProviderType.VOLCENGINE;
    }

    @Override
    public ManagedFlinkCapability getCapability(ProviderContext context) {
        return ManagedFlinkCapability.builder()
            .providerType(type())
            .apiVersion(FLINK_API_VERSION)
            .engineVersions(
                Arrays.asList(
                    "FLINK_VERSION_1_11",
                    "FLINK_VERSION_1_16",
                    "FLINK_VERSION_1_17",
                    "FLINK_VERSION_1_20",
                    "FLINK_VERSION_2_2"))
            .jobTypes(Arrays.asList("STREAMING_SQL", "STREAMING_JAR"))
            .executionModes(Collections.singletonList("APPLICATION"))
            .startModes(Arrays.asList("FRESH", "LATEST_STATE", "SPECIFIED_SNAPSHOT"))
            .schedulingStrategies(Collections.singletonList("DEFAULT"))
            .supportsProjectList(true)
            .supportsResourcePoolList(true)
            .supportsSqlDeepCheck(true)
            .supportsSkipPrecheck(false)
            .supportsStopWithSnapshot(false)
            .supportsCreateSnapshot(true)
            .supportsJarDirectUpload(false)
            .supportsCustomEndpoint(false)
            .minCpu(new BigDecimal("0.5"))
            .cpuStep(new BigDecimal("0.5"))
            .memoryPerCpuGiB(new BigDecimal("4"))
            .maxArtifactBytes(500L * 1024 * 1024)
            .customParameterRules(Collections.emptyMap())
            .capabilityRevision("volcengine-m0-20260728")
            .expireAt(
                Instant.now()
                    .plus(Duration.ofMinutes(properties.getCapabilityTtlMinutes())))
            .build();
    }

    @Override
    public CredentialCheckResult validateCredential(ProviderContext context) {
        VolcengineOpenApiResponse response =
            openApiClient.get(
                context,
                PROJECT_ACTION,
                PROJECT_API_VERSION,
                projectParameters(1, 1, null));
        return CredentialCheckResult.builder()
            .success(true)
            .providerRequestId(response.getRequestId())
            .message("Volcengine Flink credential is valid.")
            .build();
    }

    @Override
    public List<CloudProject> listProjects(ProviderContext context, String keyword) {
        List<CloudProject> projects = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (int page = 1; page <= MAX_PAGES; page++) {
            VolcengineOpenApiResponse response =
                openApiClient.get(
                    context,
                    PROJECT_ACTION,
                    PROJECT_API_VERSION,
                    projectParameters(page, PAGE_SIZE, keyword));
            JsonNode data = result(response.getRoot());
            JsonNode items = firstArray(data, "ProjectList", "Items", "List");
            int pageCount = appendProjects(items, projects, seen);
            if (pageCount < PAGE_SIZE || reachedTotal(data, projects.size())) {
                break;
            }
        }
        return Collections.unmodifiableList(projects);
    }

    @Override
    public List<ManagedResourcePool> listResourcePools(
                                                       ProviderContext context,
                                                       String projectId,
                                                       String keyword) {
        List<ManagedResourcePool> pools = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (int page = 1; page <= MAX_PAGES; page++) {
            VolcengineOpenApiResponse response =
                openApiClient.get(
                    context,
                    RESOURCE_POOL_ACTION,
                    RESOURCE_POOL_API_VERSION,
                    resourcePoolParameters(page, PAGE_SIZE, projectId, keyword));
            JsonNode data = result(response.getRoot());
            JsonNode items =
                firstArray(
                    data,
                    "DataList",
                    "ResourcePools",
                    "ResourcePoolList",
                    "Items",
                    "List");
            int pageCount = appendPools(items, pools, seen);
            if (pageCount < PAGE_SIZE || reachedTotal(data, pools.size())) {
                break;
            }
        }
        return Collections.unmodifiableList(pools);
    }

    @Override
    public List<ManagedDraftDirectory> listDraftDirectories(
                                                            ProviderContext context,
                                                            String projectId,
                                                            String keyword) {
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("ProjectId", trimToEmpty(projectId));
        VolcengineOpenApiResponse response =
            openApiClient.post(
                context,
                DRAFT_DIRECTORY_ACTION,
                DRAFT_DIRECTORY_API_VERSION,
                parameters,
                Collections.emptyMap());
        String normalizedKeyword = trimToEmpty(keyword).toLowerCase(Locale.ROOT);
        List<ManagedDraftDirectory> directories = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        JsonNode items = draftDirectoryItems(response.getRoot());
        if (items != null) {
            appendDraftDirectories(
                items, null, null, normalizedKeyword, directories, seen);
        }
        return Collections.unmodifiableList(directories);
    }

    @Override
    public StagedArtifact stageArtifact(
                                        ProviderContext context, ArtifactStageRequest request) {
        throw artifactStagingUnavailable();
    }

    @Override
    public StagedArtifact findArtifact(
                                       ProviderContext context, ArtifactLookupRequest request) {
        throw artifactStagingUnavailable();
    }

    @Override
    public ManagedDraft upsertDraft(ProviderContext context, ManagedDraftRequest request) {
        requireDraftRequest(request);
        String draftId = request.getExistingDraftId();
        String createRequestId = null;
        JsonNode providerDraft;
        if (isBlank(draftId)) {
            VolcengineOpenApiResponse created =
                openApiClient.postOnce(
                    context,
                    "CreateGWSApplicationDraft",
                    DRAFT_WRITE_API_VERSION,
                    Collections.singletonMap("ProjectId", request.getProjectId()),
                    createRequestBody(request));
            providerDraft = result(created.getRoot());
            draftId = text(providerDraft, "Id", "ID");
            createRequestId = created.getRequestId();
            if (isBlank(draftId)) {
                throw invalidResponse(createRequestId);
            }
            if (!hasDraftUpdateContext(providerDraft)) {
                providerDraft = getDraft(context, request.getProjectId(), draftId);
            }
        } else {
            providerDraft = getDraft(context, request.getProjectId(), draftId);
        }

        VolcengineOpenApiResponse updated =
            openApiClient.postOnce(
                context,
                "UpdateGWSApplicationDraft",
                DRAFT_WRITE_API_VERSION,
                Collections.singletonMap("ProjectId", request.getProjectId()),
                updateRequestBody(providerDraft, request));
        JsonNode updateResult = result(updated.getRoot());
        if (updateResult.has("Success") && !updateResult.path("Success").asBoolean()) {
            throw invalidResponse(updated.getRequestId());
        }
        return ManagedDraft.builder()
            .draftId(draftId)
            .providerRequestId(firstNonBlank(updated.getRequestId(), createRequestId))
            .definitionHash(request.getDefinitionHash())
            .build();
    }

    @Override
    public ManagedDeployment deployDraft(
                                         ProviderContext context, ManagedDeployRequest request) {
        requireDeployRequest(request);
        try (
            VolcengineCredentials credentials = credentialResolver.resolve(context);
            VolcengineSdkClientFactory.Session session =
                sdkClientFactory.open(context, credentials)) {
            ApiResponse<DeployGWSApplicationDraftResponse> deployed =
                session.api().deployGWSApplicationDraftWithHttpInfo(deployRequest(request));
            String requestId = requestId(deployed);
            DeployGWSApplicationDraftResponse response = deployed.getData();
            if (response == null
                || !Boolean.TRUE.equals(response.isSuccess())
                || isBlank(response.getId())) {
                throw invalidResponse(requestId);
            }
            return ManagedDeployment.builder()
                .applicationId(response.getId())
                .providerRequestId(requestId)
                .definitionHash(request.getDefinitionHash())
                .build();
        } catch (ApiException exception) {
            throw sdkFailure("DeployGWSApplicationDraft", exception);
        }
    }

    @Override
    public ManagedDeployment findDeployment(
                                            ProviderContext context,
                                            ManagedDeploymentLookupRequest request) {
        requireDeploymentLookupRequest(request);
        try (
            VolcengineCredentials credentials = credentialResolver.resolve(context);
            VolcengineSdkClientFactory.Session session =
                sdkClientFactory.open(context, credentials)) {
            for (int page = 1; page <= MAX_PAGES; page++) {
                ListGWSApplicationRequest body = new ListGWSApplicationRequest();
                body.setProjectId(request.getProjectId());
                body.setJobName(request.getJobName());
                body.setPageNum(page);
                body.setPageSize(PAGE_SIZE);
                ApiResponse<ListGWSApplicationResponse> listed =
                    session.api().listGWSApplicationWithHttpInfo(body);
                ListGWSApplicationResponse response = listed.getData();
                List<RecordForListGWSApplicationOutput> records =
                    response == null ? null : response.getRecords();
                if (records == null || records.isEmpty()) {
                    return null;
                }
                for (RecordForListGWSApplicationOutput record : records) {
                    if (request.getDraftId().equals(record.getAppDraftId())) {
                        return ManagedDeployment.builder()
                            .applicationId(providerJobId(record))
                            .providerRequestId(requestId(listed))
                            .definitionHash(request.getDefinitionHash())
                            .providerState(record.getState())
                            .build();
                    }
                }
                if (records.size() < PAGE_SIZE
                    || response.getTotal() != null
                        && page * PAGE_SIZE >= response.getTotal()) {
                    return null;
                }
            }
            return null;
        } catch (ApiException exception) {
            throw sdkFailure(exception);
        }
    }

    @Override
    public ManagedJobActionResult startJob(
                                           ProviderContext context, ManagedJobStartRequest request) {
        requireStartRequest(request);
        try (
            VolcengineCredentials credentials = credentialResolver.resolve(context);
            VolcengineSdkClientFactory.Session session =
                sdkClientFactory.open(context, credentials)) {
            ApiResponse<StartApplicationInstanceResponse> started =
                session
                    .api()
                    .startApplicationInstanceWithHttpInfo(startRequest(request));
            String requestId = requestId(started);
            StartApplicationInstanceResponse response = started.getData();
            if (response == null
                || !Boolean.TRUE.equals(response.isSuccess())
                || isBlank(response.getId())
                || isBlank(response.getInstanceId())) {
                throw invalidResponse(requestId);
            }
            return ManagedJobActionResult.builder()
                .jobId(response.getId())
                .instanceId(response.getInstanceId())
                .providerRequestId(requestId)
                .providerState("STARTING")
                .build();
        } catch (ApiException exception) {
            throw sdkFailure(exception);
        }
    }

    @Override
    public ManagedJobActionResult stopJob(
                                          ProviderContext context, ManagedJobStopRequest request) {
        requireStopRequest(request);
        try (
            VolcengineCredentials credentials = credentialResolver.resolve(context);
            VolcengineSdkClientFactory.Session session =
                sdkClientFactory.open(context, credentials)) {
            ApiResponse<CancelApplicationInstanceResponse> stopped =
                session
                    .api()
                    .cancelApplicationInstanceWithHttpInfo(stopRequest(request));
            String requestId = requestId(stopped);
            CancelApplicationInstanceResponse response = stopped.getData();
            if (response == null || !Boolean.TRUE.equals(response.isSuccess())) {
                throw invalidResponse(requestId);
            }
            return ManagedJobActionResult.builder()
                .jobId(firstNonBlank(response.getId(), request.getJobId()))
                .instanceId(
                    firstNonBlank(response.getInstanceId(), request.getInstanceId()))
                .providerRequestId(requestId)
                .providerState("STOPPING")
                .build();
        } catch (ApiException exception) {
            throw sdkFailure(exception);
        }
    }

    @Override
    public ManagedJobActionResult restartJob(
                                             ProviderContext context,
                                             ManagedJobRestartRequest request) {
        requireRestartRequest(request);
        try (
            VolcengineCredentials credentials = credentialResolver.resolve(context);
            VolcengineSdkClientFactory.Session session =
                sdkClientFactory.open(context, credentials)) {
            ApiResponse<RestartGWSApplicationResponse> restarted =
                session
                    .api()
                    .restartGWSApplicationWithHttpInfo(restartRequest(request));
            String requestId = requestId(restarted);
            RestartGWSApplicationResponse response = restarted.getData();
            if (response == null
                || !Boolean.TRUE.equals(response.isSuccess())
                || isBlank(response.getId())) {
                throw invalidResponse(requestId);
            }
            return ManagedJobActionResult.builder()
                .jobId(response.getId())
                .providerRequestId(requestId)
                .providerState("RESTARTING")
                .build();
        } catch (ApiException exception) {
            throw sdkFailure(exception);
        }
    }

    @Override
    public ManagedJobStatus getJob(
                                   ProviderContext context, ManagedJobLookupRequest request) {
        requireJobLookupRequest(request);
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("ProjectId", request.getProjectId());
        Map<String, String> body = new LinkedHashMap<>();
        body.put("AccountId", "");
        body.put("Id", request.getJobId());
        VolcengineOpenApiResponse response =
            openApiClient.post(
                context,
                JOB_GET_ACTION,
                JOB_GET_API_VERSION,
                parameters,
                body);
        JsonNode data = result(response.getRoot());
        String jobId = text(data, "Id", "ID", "ApplicationId");
        if (isBlank(jobId)) {
            return null;
        }
        String providerState = text(data, "State", "Status");
        String instanceId = text(data, "InstanceId", "ApplicationInstanceId");
        if (isBlank(instanceId)) {
            instanceId = latestInstanceId(context, jobId, providerState);
        }
        instanceId = firstNonBlank(instanceId, request.getInstanceId());
        InstanceLinks instanceLinks = getInstanceLinks(context, jobId, instanceId);
        return ManagedJobStatus.builder()
            .jobId(jobId)
            .instanceId(instanceId)
            .state(providerJobState(providerState))
            .providerState(providerState)
            .providerRequestId(response.getRequestId())
            .flinkUiUrl(instanceLinks.flinkUiUrl)
            .consoleUrl(instanceLinks.consoleUrl)
            .build();
    }

    private String latestInstanceId(
                                    ProviderContext context,
                                    String jobId,
                                    String providerState) {
        if (credentialResolver == null || sdkClientFactory == null) {
            return null;
        }
        try (
            VolcengineCredentials credentials = credentialResolver.resolve(context);
            VolcengineSdkClientFactory.Session session =
                sdkClientFactory.open(context, credentials)) {
            ListApplicationInstanceRequest request =
                new ListApplicationInstanceRequest()
                    .projectId(context.getProjectId())
                    .jobId(Long.valueOf(jobId))
                    .pageNum("1")
                    .pageSize("20")
                    .sortField("StartTime")
                    .sortOrder("DESC");
            ListApplicationInstanceResponse response =
                session.api().listApplicationInstance(request);
            return selectLatestInstanceId(
                response == null ? null : response.getRecords(), providerState);
        } catch (Exception exception) {
            log.warn(
                "Unable to resolve latest Volcengine instance for job {}: {}",
                jobId,
                exception.getClass().getSimpleName());
            return null;
        }
    }

    static String selectLatestInstanceId(
                                         List<RecordForListApplicationInstanceOutput> records,
                                         String providerState) {
        if (records == null || records.isEmpty()) {
            return null;
        }
        if ("RUNNING".equalsIgnoreCase(providerState)) {
            for (RecordForListApplicationInstanceOutput record : records) {
                if (record != null
                    && "RUNNING".equalsIgnoreCase(record.getState())
                    && !isBlank(record.getId())) {
                    return record.getId();
                }
            }
        }
        for (RecordForListApplicationInstanceOutput record : records) {
            if (record != null && !isBlank(record.getId())) {
                return record.getId();
            }
        }
        return null;
    }

    private InstanceLinks getInstanceLinks(
                                           ProviderContext context,
                                           String applicationId,
                                           String instanceId) {
        if (isBlank(instanceId)
            || credentialResolver == null
            || sdkClientFactory == null) {
            return InstanceLinks.EMPTY;
        }
        try (
            VolcengineCredentials credentials = credentialResolver.resolve(context);
            VolcengineSdkClientFactory.Session session =
                sdkClientFactory.open(context, credentials)) {
            GetApplicationInstanceResponse instance =
                session
                    .api()
                    .getApplicationInstanceWithHttpInfo(
                        new GetApplicationInstanceRequest().instanceId(instanceId))
                    .getData();
            if (instance == null) {
                return InstanceLinks.EMPTY;
            }
            return new InstanceLinks(
                firstNonBlank(instance.getCompleteRestUrl(), instance.getRestUrl()),
                consoleUrl(context, applicationId, instance));
        } catch (Exception exception) {
            log.warn(
                "Unable to resolve Volcengine instance links for instance {}: {}",
                instanceId,
                exception.getClass().getSimpleName());
            return InstanceLinks.EMPTY;
        }
    }

    static String consoleUrl(
                             ProviderContext context,
                             String applicationId,
                             GetApplicationInstanceResponse instance) {
        String region = context == null ? null : context.getRegion();
        String projectId = context == null ? null : context.getProjectId();
        String clusterId =
            instance == null
                ? null
                : firstNonBlank(
                    instance.getApplicationId(),
                    isBlank(instance.getId()) ? null : "s-" + instance.getId());
        String gtsJobUuid = instance == null ? null : instance.getDeploymentId();
        if (isBlank(region)
            || isBlank(projectId)
            || isBlank(applicationId)
            || isBlank(clusterId)
            || isBlank(gtsJobUuid)) {
            return null;
        }
        return "https://console.volcengine.com/flink/region:flink+"
            + region
            + "/project/"
            + projectId
            + "/job/manage/"
            + applicationId
            + "/detail?ClusterId="
            + clusterId
            + "&GtsJobUuid="
            + gtsJobUuid
            + "&AppId="
            + applicationId;
    }

    private static final class InstanceLinks {

        private static final InstanceLinks EMPTY = new InstanceLinks(null, null);

        private final String flinkUiUrl;
        private final String consoleUrl;

        private InstanceLinks(String flinkUiUrl, String consoleUrl) {
            this.flinkUiUrl = flinkUiUrl;
            this.consoleUrl = consoleUrl;
        }
    }

    @Override
    public List<ManagedSnapshot> listSnapshots(
                                               ProviderContext context,
                                               ManagedSnapshotLookupRequest request) {
        requireSnapshotLookupRequest(request);
        try (
            VolcengineCredentials credentials = credentialResolver.resolve(context);
            VolcengineSdkClientFactory.Session session =
                sdkClientFactory.open(context, credentials)) {
            ApiResponse<ListGWSSavepointResponse> listed =
                session
                    .api()
                    .listGWSSavepointWithHttpInfo(snapshotLookupRequest(request));
            ListGWSSavepointResponse response = listed.getData();
            if (response == null || response.getSavepointInfos() == null) {
                return Collections.emptyList();
            }
            List<ManagedSnapshot> snapshots = new ArrayList<>();
            for (SavepointInfoForListGWSSavepointOutput item : response.getSavepointInfos()) {
                if (item != null && !isBlank(item.getSavepointId())) {
                    snapshots.add(snapshot(item));
                }
            }
            return snapshots;
        } catch (ApiException exception) {
            throw sdkFailure(exception);
        }
    }

    @Override
    public ManagedSnapshotCreateResult createSnapshot(
                                                      ProviderContext context,
                                                      ManagedSnapshotCreateRequest request) {
        requireSnapshotCreateRequest(request);
        try (
            VolcengineCredentials credentials = credentialResolver.resolve(context);
            VolcengineSdkClientFactory.Session session =
                sdkClientFactory.open(context, credentials)) {
            ApiResponse<CreateGWSSavepointResponse> created =
                session
                    .api()
                    .createGWSSavepointWithHttpInfo(snapshotCreateRequest(request));
            String requestId = requestId(created);
            CreateGWSSavepointResponse response = created.getData();
            if (response == null
                || !Boolean.TRUE.equals(response.isSuccess())
                || isBlank(response.getId())
                || isBlank(response.getInstanceId())) {
                throw invalidResponse(requestId);
            }
            return ManagedSnapshotCreateResult.builder()
                .jobId(response.getId())
                .instanceId(response.getInstanceId())
                .providerRequestId(requestId)
                .providerState("CREATING")
                .build();
        } catch (ApiException exception) {
            throw sdkFailure(exception);
        }
    }

    static CreateGWSApplicationDraftRequest createRequest(ManagedDraftRequest request) {
        CreateGWSApplicationDraftRequest body = new CreateGWSApplicationDraftRequest();
        body.setProjectId(request.getProjectId());
        body.setDirectoryId(request.getDirectoryId());
        body.setJobName(request.getJobName());
        body.setJobType(providerJobType(request.getJobType()));
        body.setEngineVersion(providerEngineVersion(request.getEngineVersion()));
        return body;
    }

    static Map<String, Object> createRequestBody(ManagedDraftRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ProjectId", request.getProjectId());
        body.put("DirectoryId", request.getDirectoryId());
        body.put("JobName", request.getJobName());
        body.put("JobType", providerJobType(request.getJobType()));
        body.put("EngineVersion", providerEngineVersion(request.getEngineVersion()));
        return body;
    }

    private JsonNode getDraft(ProviderContext context, String projectId, String draftId) {
        Map<String, String> parameters = Collections.singletonMap("ProjectId", projectId);
        Map<String, String> body = Collections.singletonMap("Id", draftId);
        JsonNode draft =
            result(
                openApiClient
                    .postOnce(context, DRAFT_GET_ACTION, DRAFT_GET_API_VERSION, parameters, body)
                    .getRoot());
        if (!draft.isObject() || isBlank(text(draft, "Id", "ID"))) {
            throw invalidResponse(null);
        }
        return draft;
    }

    private static boolean hasDraftUpdateContext(JsonNode draft) {
        return draft != null
            && draft.isObject()
            && !isBlank(text(draft, "AccountId"))
            && !isBlank(text(draft, "UserId"))
            && !isBlank(text(draft, "Platform"))
            && !isBlank(text(draft, "JobId"))
            && !isBlank(text(draft, "State"))
            && !isBlank(text(draft, "ResourceVersion"))
            && !isBlank(text(draft, "CreateTime"));
    }

    ObjectNode updateRequestBody(JsonNode providerDraft, ManagedDraftRequest request) {
        if (providerDraft == null || !providerDraft.isObject()) {
            throw invalidResponse(null);
        }
        ObjectNode body = ((ObjectNode) providerDraft).deepCopy();
        body.put("ProjectId", request.getProjectId());
        body.put("JobName", request.getJobName());
        body.put("JobType", providerJobType(request.getJobType()));
        body.put("EngineVersion", providerEngineVersion(request.getEngineVersion()));
        body.put("SqlText", request.getSqlText());
        body.put("Options", "{}");
        body.put("DynamicOptions", dynamicOptions(request));
        if (!isBlank(request.getDependencyJson())) {
            body.put("Dependency", request.getDependencyJson());
        }
        return body;
    }

    private String dynamicOptions(ManagedDraftRequest request) {
        Map<String, String> options = new LinkedHashMap<>();
        JsonNode runtime = readObject(request.getOptionsJson(), "Options");
        JsonNode resource = runtime.path("resource");
        put(options, "parallelism.default", resource.get("parallelism"));
        put(options, "kubernetes.taskmanager.cpu", resource.get("taskManagerCpu"));
        putMemory(options, "taskmanager.memory.process.size", resource.get("taskManagerMemoryGiB"));
        put(options, "taskmanager.numberOfTaskSlots", resource.get("taskManagerSlots"));
        put(options, "kubernetes.jobmanager.cpu", resource.get("jobManagerCpu"));
        putMemory(options, "jobmanager.memory.process.size", resource.get("jobManagerMemoryGiB"));

        JsonNode checkpoint = runtime.path("checkpoint");
        if (checkpoint.path("enabled").asBoolean(false)) {
            putDuration(options, "execution.checkpointing.interval", checkpoint.get("intervalMs"));
            putDuration(options, "execution.checkpointing.timeout", checkpoint.get("timeoutMs"));
            put(options, "state.backend", checkpoint.get("backend"));
        }

        JsonNode restartStrategy = runtime.path("restartStrategy");
        put(options, "restart-strategy", restartStrategy.get("type"));
        merge(options, restartStrategy.path("parameters"));
        JsonNode retryOnFailure = runtime.get("retryOnFailure");
        if (retryOnFailure != null && retryOnFailure.isBoolean()) {
            options.put("restart.attempt.enable", retryOnFailure.asText());
        }
        put(options, "restart.attempt.interval.min", runtime.get("retryIntervalMin"));
        put(options, "restart.attempt.max.count", runtime.get("retryMaxCount"));
        merge(options, runtime.path("customProperties"));
        merge(options, readObject(request.getDynamicOptionsJson(), "DynamicOptions"));
        try {
            return objectMapper.writeValueAsString(options);
        } catch (Exception exception) {
            throw invalidDraftOptions("DynamicOptions");
        }
    }

    private JsonNode readObject(String value, String field) {
        if (isBlank(value)) {
            return objectMapper.createObjectNode();
        }
        try {
            JsonNode node = objectMapper.readTree(value);
            if (!node.isObject()) {
                throw invalidDraftOptions(field);
            }
            return node;
        } catch (ManagedFlinkProviderException exception) {
            throw exception;
        } catch (Exception exception) {
            throw invalidDraftOptions(field);
        }
    }

    private static void merge(Map<String, String> target, JsonNode source) {
        if (source == null || !source.isObject()) {
            return;
        }
        source.fields().forEachRemaining(entry -> put(target, entry.getKey(), entry.getValue()));
    }

    private static void put(Map<String, String> target, String key, JsonNode value) {
        if (value != null && !value.isNull() && !value.asText().trim().isEmpty()) {
            target.put(key, value.asText());
        }
    }

    private static void putDuration(Map<String, String> target, String key, JsonNode millis) {
        if (millis == null || !millis.canConvertToLong()) {
            return;
        }
        long value = millis.asLong();
        target.put(key, value % 1000 == 0 ? value / 1000 + "s" : value + "ms");
    }

    private static void putMemory(Map<String, String> target, String key, JsonNode gibibytes) {
        if (gibibytes == null || !gibibytes.isNumber()) {
            return;
        }
        BigDecimal mebibytes = gibibytes.decimalValue().multiply(BigDecimal.valueOf(1024));
        target.put(key, mebibytes.stripTrailingZeros().toPlainString() + "mb");
    }

    private static ManagedFlinkProviderException invalidDraftOptions(String field) {
        return new ManagedFlinkProviderException(
            ProviderErrorCategory.VALIDATION,
            "InvalidDraft" + field,
            null,
            "Managed Flink draft options are invalid.");
    }

    static String providerJobType(String value) {
        if ("STREAMING_SQL".equals(value)) {
            return "FLINK_STREAMING_SQL";
        }
        if ("STREAMING_JAR".equals(value)) {
            return "FLINK_STREAMING_JAR";
        }
        return value;
    }

    static String providerEngineVersion(String value) {
        if (value != null && value.matches("[0-9]+\\.[0-9]+")) {
            return "FLINK_VERSION_" + value.replace('.', '_');
        }
        return value;
    }

    static String providerJobId(RecordForListGWSApplicationOutput record) {
        return firstNonBlank(record.getId(), record.getApplicationId());
    }

    static ManagedJobState providerJobState(String value) {
        if (isBlank(value)) {
            return ManagedJobState.OTHER;
        }
        switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "CREATED":
            case "READY":
            case "DEPLOYED":
                return ManagedJobState.CREATED;
            case "STARTING":
            case "DEPLOYING":
                return ManagedJobState.STARTING;
            case "RUNNING":
                return ManagedJobState.RUNNING;
            case "RESTARTING":
                return ManagedJobState.RESTARTING;
            case "STOPPING":
            case "CANCELLING":
            case "CANCELING":
                return ManagedJobState.STOPPING;
            case "STOPPED":
            case "CANCELED":
            case "CANCELLED":
                return ManagedJobState.STOPPED;
            case "SAVEPOINTING":
                return ManagedJobState.SAVEPOINTING;
            case "FINISHED":
            case "SUCCEEDED":
                return ManagedJobState.SUCCEEDED;
            case "FAILED":
                return ManagedJobState.FAILED;
            case "SUSPENDED":
                return ManagedJobState.SUSPENDED;
            default:
                return ManagedJobState.OTHER;
        }
    }

    static DeployGWSApplicationDraftRequest deployRequest(ManagedDeployRequest request) {
        DeployGWSApplicationDraftRequest body = new DeployGWSApplicationDraftRequest();
        body.setDraftId(request.getDraftId());
        body.setProjectId(request.getProjectId());
        body.setResourcePool(request.getResourcePool());
        body.setQueue(request.getQueue());
        body.setPriority(request.getPriority());
        body.setSchedulePolicy(request.getSchedulePolicy());
        body.setScheduleTimeout(request.getScheduleTimeoutSeconds());
        return body;
    }

    static StartApplicationInstanceRequest startRequest(ManagedJobStartRequest request) {
        StartApplicationInstanceRequest body = new StartApplicationInstanceRequest();
        body.setId(request.getJobId());
        DeployRequestForStartApplicationInstanceInput deployRequest =
            new DeployRequestForStartApplicationInstanceInput();
        deployRequest.setResourcePool(request.getResourcePool());
        deployRequest.setQueue(request.getQueue());
        deployRequest.setPriority(request.getPriority());
        if (!isBlank(request.getSchedulePolicy())) {
            deployRequest.setSchedulePolicy(
                DeployRequestForStartApplicationInstanceInput.SchedulePolicyEnum.fromValue(
                    request.getSchedulePolicy()));
        }
        if (request.getScheduleTimeoutSeconds() != null) {
            deployRequest.setScheduleTimeout(
                request.getScheduleTimeoutSeconds().toString());
        }
        AppForStartApplicationInstanceInput app = new AppForStartApplicationInstanceInput();
        app.setDeployRequest(deployRequest);
        body.setApp(app);
        RestoreStrategyForStartApplicationInstanceInput restoreStrategy =
            new RestoreStrategyForStartApplicationInstanceInput();
        restoreStrategy.setType(providerRestoreType(request.getRestoreMode()));
        restoreStrategy.setSavepointId(request.getSnapshotId());
        body.setRestoreStrategy(restoreStrategy);
        return body;
    }

    static CancelApplicationInstanceRequest stopRequest(ManagedJobStopRequest request) {
        CancelApplicationInstanceRequest body = new CancelApplicationInstanceRequest();
        body.setInstanceId(request.getInstanceId());
        return body;
    }

    static RestartGWSApplicationRequest restartRequest(ManagedJobRestartRequest request) {
        RestartGWSApplicationRequest body = new RestartGWSApplicationRequest();
        body.setId(request.getJobId());
        body.setType(
            RestartGWSApplicationRequest.TypeEnum.fromValue(
                providerRestoreType(request.getRestoreMode())));
        body.setSavepointId(request.getSnapshotId());
        return body;
    }

    static ListGWSSavepointRequest snapshotLookupRequest(
                                                         ManagedSnapshotLookupRequest request) {
        ListGWSSavepointRequest body = new ListGWSSavepointRequest();
        body.setId(request.getJobId());
        return body;
    }

    static CreateGWSSavepointRequest snapshotCreateRequest(
                                                           ManagedSnapshotCreateRequest request) {
        CreateGWSSavepointRequest body = new CreateGWSSavepointRequest();
        body.setId(request.getJobId());
        body.setDescription(request.getDescription());
        return body;
    }

    static ManagedSnapshot snapshot(SavepointInfoForListGWSSavepointOutput item) {
        return ManagedSnapshot.builder()
            .snapshotId(item.getSavepointId())
            .snapshotType(item.getCreateType())
            .state(snapshotState(item.getStatus()))
            .providerState(item.getStatus())
            .location(item.getSavepointPath())
            .description(item.getDescription())
            .triggerTime(item.getCreateTime())
            .completionTime(item.getCompeleteTime())
            .build();
    }

    static ManagedSnapshotState snapshotState(String state) {
        if (state == null) {
            return ManagedSnapshotState.OTHER;
        }
        switch (state.trim().toUpperCase(Locale.ROOT)) {
            case "CREATING":
            case "PENDING":
            case "RUNNING":
                return ManagedSnapshotState.CREATING;
            case "COMPLETED":
            case "SUCCEEDED":
            case "SUCCESS":
            case "AVAILABLE":
                return ManagedSnapshotState.COMPLETED;
            case "FAILED":
            case "FAIL":
            case "ERROR":
                return ManagedSnapshotState.FAILED;
            case "EXPIRED":
            case "DELETED":
                return ManagedSnapshotState.EXPIRED;
            default:
                return ManagedSnapshotState.OTHER;
        }
    }

    static String providerRestoreType(ManagedJobRestoreMode restoreMode) {
        switch (restoreMode) {
            case FRESH:
                return "FROM_NEW";
            case LATEST_STATE:
                return "FROM_LATEST";
            case SPECIFIED_SNAPSHOT:
                return "FROM_SAVEPOINT";
            default:
                throw validation("InvalidRestoreMode");
        }
    }

    private static void requireDraftRequest(ManagedDraftRequest request) {
        if (request == null
            || isBlank(request.getProjectId())
            || request.getDirectoryId() == null
            || request.getDirectoryId() <= 0
            || isBlank(request.getJobName())
            || isBlank(request.getJobType())
            || isBlank(request.getEngineVersion())
            || isBlank(request.getDefinitionHash())) {
            throw validation("InvalidDraftRequest");
        }
    }

    private static void requireDeployRequest(ManagedDeployRequest request) {
        if (request == null
            || isBlank(request.getDraftId())
            || isBlank(request.getProjectId())
            || isBlank(request.getResourcePool())
            || isBlank(request.getDefinitionHash())) {
            throw validation("InvalidDeployRequest");
        }
    }

    private static void requireDeploymentLookupRequest(
                                                       ManagedDeploymentLookupRequest request) {
        if (request == null
            || isBlank(request.getProjectId())
            || isBlank(request.getDraftId())
            || isBlank(request.getJobName())
            || isBlank(request.getDefinitionHash())) {
            throw validation("InvalidDeploymentLookupRequest");
        }
    }

    private static void requireStartRequest(ManagedJobStartRequest request) {
        if (request == null
            || isBlank(request.getJobId())
            || isBlank(request.getResourcePool())
            || isBlank(request.getQueue())
            || !validRestoreRequest(request.getRestoreMode(), request.getSnapshotId())) {
            throw validation("InvalidStartRequest");
        }
    }

    private static void requireStopRequest(ManagedJobStopRequest request) {
        if (request == null
            || isBlank(request.getJobId())
            || isBlank(request.getInstanceId())) {
            throw validation("InvalidStopRequest");
        }
        if (request.isWithSnapshot()) {
            throw new ManagedFlinkProviderException(
                ProviderErrorCategory.PROVIDER_CONFIGURATION,
                "StopWithSnapshotNotImplemented",
                null,
                "Volcengine stop with snapshot is not available in this release.");
        }
    }

    private static void requireRestartRequest(ManagedJobRestartRequest request) {
        if (request == null
            || isBlank(request.getJobId())
            || !validRestoreRequest(request.getRestoreMode(), request.getSnapshotId())) {
            throw validation("InvalidRestartRequest");
        }
    }

    private static void requireJobLookupRequest(ManagedJobLookupRequest request) {
        if (request == null
            || isBlank(request.getProjectId())
            || isBlank(request.getJobName())
            || isBlank(request.getJobId())) {
            throw validation("InvalidJobLookupRequest");
        }
    }

    private static void requireSnapshotLookupRequest(ManagedSnapshotLookupRequest request) {
        if (request == null
            || isBlank(request.getProjectId())
            || isBlank(request.getJobId())) {
            throw validation("InvalidSnapshotLookupRequest");
        }
    }

    private static void requireSnapshotCreateRequest(ManagedSnapshotCreateRequest request) {
        if (request == null
            || isBlank(request.getProjectId())
            || isBlank(request.getJobId())
            || isBlank(request.getInstanceId())) {
            throw validation("InvalidSnapshotCreateRequest");
        }
    }

    private static boolean validRestoreRequest(
                                               ManagedJobRestoreMode restoreMode,
                                               String snapshotId) {
        return restoreMode != null
            && (restoreMode == ManagedJobRestoreMode.SPECIFIED_SNAPSHOT
                ? !isBlank(snapshotId)
                : isBlank(snapshotId));
    }

    private static ManagedFlinkProviderException validation(String code) {
        return new ManagedFlinkProviderException(
            ProviderErrorCategory.VALIDATION,
            code,
            null,
            "Volcengine managed Flink write request is invalid.");
    }

    private static ManagedFlinkProviderException invalidResponse(String requestId) {
        return new ManagedFlinkProviderException(
            ProviderErrorCategory.UNKNOWN,
            "InvalidWriteResponse",
            requestId,
            "Volcengine managed Flink returned an invalid write response.");
    }

    private static ManagedFlinkProviderException sdkFailure(ApiException exception) {
        return sdkFailure("UnknownAction", exception);
    }

    private static ManagedFlinkProviderException sdkFailure(
                                                            String action,
                                                            ApiException exception) {
        int status = exception.getCode();
        Throwable cause = exception.getCause();
        ResponseMetadata metadata = exception.getResponseMetadata();
        String providerErrorCode =
            metadata == null || metadata.getError() == null
                ? null
                : metadata.getError().getCode();
        String providerErrorMessage =
            metadata == null || metadata.getError() == null
                ? null
                : safeProviderMessage(metadata.getError().getMessage());
        log.warn(
            "Volcengine managed Flink SDK request failed: action={}, status={}, providerCode={}, "
                + "providerMessage={}, cause={}",
            action,
            status,
            providerErrorCode,
            providerErrorMessage,
            cause == null ? "unavailable" : cause.getClass().getName());
        ProviderErrorCategory category;
        if (status == 401) {
            category = ProviderErrorCategory.AUTHENTICATION;
        } else if (status == 403) {
            category = ProviderErrorCategory.AUTHORIZATION;
        } else if (status == 404) {
            category = ProviderErrorCategory.NOT_FOUND;
        } else if (status == 409) {
            category = ProviderErrorCategory.CONFLICT;
        } else if (status == 429) {
            category = ProviderErrorCategory.RATE_LIMIT;
        } else if (status == 0 || status >= 500) {
            category = ProviderErrorCategory.TRANSIENT;
        } else {
            category = ProviderErrorCategory.UNKNOWN;
        }
        return new ManagedFlinkProviderException(
            category,
            isBlank(providerErrorCode) ? "SdkRequestFailed" : providerErrorCode,
            metadata == null
                ? requestId(exception.getResponseHeaders())
                : firstNonBlank(metadata.getRequestId(), requestId(exception.getResponseHeaders())),
            "Volcengine managed Flink write request failed.");
    }

    private static String safeProviderMessage(String value) {
        if (value == null) {
            return null;
        }
        String singleLine = value.replace('\r', ' ').replace('\n', ' ');
        return singleLine.length() <= 256
            ? singleLine
            : singleLine.substring(0, 253) + "...";
    }

    private static String requestId(ApiResponse<?> response) {
        return response == null ? null : requestId(response.getHeaders());
    }

    private static String requestId(Map<String, List<String>> headers) {
        if (headers == null) {
            return null;
        }
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if ("x-request-id".equalsIgnoreCase(entry.getKey())
                && entry.getValue() != null
                && !entry.getValue().isEmpty()) {
                return entry.getValue().get(0);
            }
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static ManagedFlinkProviderException artifactStagingUnavailable() {
        return new ManagedFlinkProviderException(
            ProviderErrorCategory.PROVIDER_CONFIGURATION,
            "ArtifactTransportNotConfigured",
            null,
            "Volcengine artifact transport is not configured.");
    }

    private static Map<String, String> projectParameters(
                                                         int page,
                                                         int pageSize,
                                                         String keyword) {
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("PageNum", String.valueOf(page));
        parameters.put("PageSize", String.valueOf(pageSize));
        parameters.put("SearchKey", trimToEmpty(keyword));
        return parameters;
    }

    private static Map<String, String> resourcePoolParameters(
                                                              int page,
                                                              int pageSize,
                                                              String projectId,
                                                              String keyword) {
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("Name", "");
        parameters.put("NameKey", trimToEmpty(keyword));
        parameters.put("PageNum", String.valueOf(page));
        parameters.put("PageSize", String.valueOf(pageSize));
        parameters.put("ProjectId", trimToEmpty(projectId));
        return parameters;
    }

    private static int appendProjects(
                                      JsonNode items,
                                      List<CloudProject> projects,
                                      Set<String> seen) {
        if (items == null) {
            return 0;
        }
        int count = 0;
        for (JsonNode item : items) {
            count++;
            String id = text(item, "ProjectId", "ProjectID", "Id", "ID");
            String name = text(item, "DisplayName", "ProjectName", "Name");
            if (id != null && name != null && seen.add(id)) {
                projects.add(CloudProject.builder().id(id).name(name).build());
            }
        }
        return count;
    }

    private static int appendPools(
                                   JsonNode items,
                                   List<ManagedResourcePool> pools,
                                   Set<String> seen) {
        if (items == null) {
            return 0;
        }
        int count = 0;
        for (JsonNode item : items) {
            count++;
            String id =
                text(item, "ResourcePoolId", "ResourcePoolID", "Id", "ID");
            String name = text(item, "ResourcePoolName", "Name");
            String fullName =
                firstNonBlank(text(item, "FullName", "ResourcePoolFullName"), id);
            if (id != null && name != null && seen.add(id)) {
                pools.add(
                    ManagedResourcePool.builder()
                        .id(id)
                        .name(name)
                        .fullName(fullName)
                        .totalCu(
                            decimalRecursive(
                                item,
                                "CapacityCU",
                                "CapacityCu",
                                "TotalCU",
                                "TotalCu",
                                "AllocatedCU"))
                        .usedCu(
                            decimalRecursive(item, "UsedCU", "UsedCu", "AllocatedUsedCU"))
                        .build());
            }
        }
        return count;
    }

    private static JsonNode draftDirectoryItems(JsonNode root) {
        JsonNode data = root == null ? null : root.get("Result");
        if (data == null || data.isNull()) {
            data = root;
        }
        if (data != null && data.isArray()) {
            return data;
        }
        JsonNode items =
            firstArray(
                data,
                "DirectoryTree",
                "DirectoryList",
                "Directories",
                "DataList",
                "Items",
                "List");
        if (items == null && data != null && data.has("Data")) {
            items =
                firstArray(
                    data.get("Data"),
                    "DirectoryTree",
                    "DirectoryList",
                    "Directories",
                    "DataList",
                    "Items",
                    "List");
        }
        return items;
    }

    private static void appendDraftDirectories(
                                               JsonNode items,
                                               String inheritedParentId,
                                               String parentPath,
                                               String keyword,
                                               List<ManagedDraftDirectory> directories,
                                               Set<String> seen) {
        for (JsonNode item : items) {
            String id = text(item, "DirectoryId", "DirectoryID", "Id", "ID");
            String name = text(item, "DirectoryName", "Name");
            String path = firstNonBlank(text(item, "Path", "DirectoryPath"), childPath(parentPath, name));
            String parentId =
                firstNonBlank(
                    text(
                        item,
                        "ParentDirectoryId",
                        "ParentDirectoryID",
                        "ParentId",
                        "ParentID"),
                    inheritedParentId);
            if (id != null && name != null && seen.add(id)) {
                if (keyword.isEmpty()
                    || name.toLowerCase(Locale.ROOT).contains(keyword)
                    || (path != null && path.toLowerCase(Locale.ROOT).contains(keyword))) {
                    directories.add(
                        ManagedDraftDirectory.builder()
                            .id(id)
                            .name(name)
                            .path(path)
                            .parentId(parentId)
                            .build());
                }
                JsonNode children =
                    firstArray(
                        item,
                        "ChildDirectoryDtoList",
                        "ChildDirectories",
                        "Children",
                        "DirectoryTree");
                if (children != null) {
                    appendDraftDirectories(
                        children, id, path, keyword, directories, seen);
                }
            }
        }
    }

    private static String childPath(String parentPath, String name) {
        if (name == null) {
            return null;
        }
        return isBlank(parentPath) ? "/" + name : parentPath + "/" + name;
    }

    private static boolean reachedTotal(JsonNode data, int count) {
        BigDecimal total = decimal(data, "Total", "TotalCount", "total");
        return total != null && count >= total.intValue();
    }

    private static JsonNode result(JsonNode root) {
        JsonNode result = root.get("Result");
        return result != null && result.isObject() ? result : root;
    }

    private static JsonNode firstArray(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && value.isArray()) {
                return value;
            }
        }
        return null;
    }

    private static String text(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && value.isValueNode()) {
                String text = value.asText().trim();
                if (!text.isEmpty()) {
                    return text;
                }
            }
        }
        return null;
    }

    private static BigDecimal decimal(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && value.isNumber()) {
                return value.decimalValue();
            }
            if (value != null && value.isTextual()) {
                try {
                    return new BigDecimal(value.asText().trim());
                } catch (NumberFormatException ignored) {
                    // Try the next compatible field.
                }
            }
        }
        return null;
    }

    private static BigDecimal decimalRecursive(JsonNode node, String... fields) {
        BigDecimal direct = decimal(node, fields);
        if (direct != null) {
            return direct;
        }
        if (node.isContainerNode()) {
            for (JsonNode child : node) {
                BigDecimal nested = decimalRecursive(child, fields);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return null;
    }
}
