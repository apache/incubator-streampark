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

import org.apache.streampark.console.core.managed.api.ArtifactLookupRequest;
import org.apache.streampark.console.core.managed.api.ArtifactStageRequest;
import org.apache.streampark.console.core.managed.api.CloudProject;
import org.apache.streampark.console.core.managed.api.CredentialCheckResult;
import org.apache.streampark.console.core.managed.api.ManagedDeployRequest;
import org.apache.streampark.console.core.managed.api.ManagedDeployment;
import org.apache.streampark.console.core.managed.api.ManagedDeploymentLookupRequest;
import org.apache.streampark.console.core.managed.api.ManagedDraft;
import org.apache.streampark.console.core.managed.api.ManagedDraftRequest;
import org.apache.streampark.console.core.managed.api.ManagedFlinkCapability;
import org.apache.streampark.console.core.managed.api.ManagedFlinkProvider;
import org.apache.streampark.console.core.managed.api.ManagedFlinkProviderException;
import org.apache.streampark.console.core.managed.api.ManagedFlinkProviderType;
import org.apache.streampark.console.core.managed.api.ManagedJobActionResult;
import org.apache.streampark.console.core.managed.api.ManagedJobLookupRequest;
import org.apache.streampark.console.core.managed.api.ManagedJobRestartRequest;
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

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Deterministic fake provider for managed Flink contract and service tests. */
public class FakeManagedFlinkProvider implements ManagedFlinkProvider {

    public static final long VALID_ACCOUNT_ID = 1L;

    public static final long INVALID_ACCOUNT_ID = -1L;

    private final Clock clock;

    private final Duration capabilityTtl;

    private final AtomicInteger capabilityRequestCount = new AtomicInteger();

    private ProviderErrorCategory capabilityFailure;

    private final Map<String, StagedArtifact> artifacts = new ConcurrentHashMap<>();

    private final AtomicInteger artifactStageCount = new AtomicInteger();

    private ProviderErrorCategory artifactFailure;

    private boolean failArtifactAfterPersist;

    private final Map<String, ManagedDraft> drafts = new ConcurrentHashMap<>();

    private final Map<String, ManagedDeployment> deployments = new ConcurrentHashMap<>();

    private ProviderErrorCategory draftFailure;

    private ProviderErrorCategory deploymentFailure;

    private boolean failDeploymentAfterPersist;

    private final Map<String, ManagedJobActionResult> jobActions = new ConcurrentHashMap<>();

    private final Map<String, ManagedJobStatus> jobs = new ConcurrentHashMap<>();

    private final AtomicInteger jobActionCount = new AtomicInteger();

    private final AtomicInteger jobLookupCount = new AtomicInteger();

    private ProviderErrorCategory jobActionFailure;

    private ProviderErrorCategory jobLookupFailure;

    private boolean failJobActionAfterPersist;

    private final Map<String, Map<String, ManagedSnapshot>> snapshots =
        new ConcurrentHashMap<>();

    private final AtomicInteger snapshotSequence = new AtomicInteger();

    private final AtomicInteger snapshotCreateCount = new AtomicInteger();

    public FakeManagedFlinkProvider(Clock clock, Duration capabilityTtl) {
        this.clock = clock;
        this.capabilityTtl = capabilityTtl;
    }

    @Override
    public ManagedFlinkProviderType type() {
        return ManagedFlinkProviderType.VOLCENGINE;
    }

    @Override
    public ManagedFlinkCapability getCapability(ProviderContext context) {
        capabilityRequestCount.incrementAndGet();
        if (capabilityFailure != null) {
            throw new ManagedFlinkProviderException(
                capabilityFailure, "FakeCapabilityFailure", "fake-request-id", "Fake failure");
        }
        return capability();
    }

    @Override
    public CredentialCheckResult validateCredential(ProviderContext context) {
        if (context.getCloudAccountId() == INVALID_ACCOUNT_ID) {
            throw new ManagedFlinkProviderException(
                ProviderErrorCategory.AUTHENTICATION,
                "InvalidCredential",
                null,
                "Credential is invalid");
        }
        return CredentialCheckResult.builder()
            .success(true)
            .providerRequestId("fake-request-id")
            .message("Credential is valid")
            .build();
    }

    @Override
    public List<CloudProject> listProjects(ProviderContext context, String keyword) {
        return Collections.singletonList(
            CloudProject.builder().id("fake-project").name("Fake Project").build());
    }

    @Override
    public List<ManagedResourcePool> listResourcePools(
                                                       ProviderContext context,
                                                       String projectId,
                                                       String keyword) {
        return Collections.singletonList(
            ManagedResourcePool.builder()
                .id("fake-pool")
                .name("Fake Pool")
                .fullName("fake-project/fake-pool")
                .totalCu(new BigDecimal("100"))
                .usedCu(BigDecimal.ZERO)
                .build());
    }

    @Override
    public StagedArtifact stageArtifact(
                                        ProviderContext context, ArtifactStageRequest request) {
        artifactStageCount.incrementAndGet();
        if (artifactFailure != null && !failArtifactAfterPersist) {
            throw artifactFailure();
        }
        StagedArtifact artifact =
            StagedArtifact.builder()
                .providerArtifactId("fake-" + request.getChecksum().substring(0, 12))
                .providerArtifactVersion(1)
                .providerUri("tos://fake/" + request.getContentAddressedName())
                .providerRequestId("fake-artifact-request-id")
                .build();
        artifacts.put(request.getChecksum(), artifact);
        if (artifactFailure != null) {
            throw artifactFailure();
        }
        return artifact;
    }

    @Override
    public StagedArtifact findArtifact(
                                       ProviderContext context, ArtifactLookupRequest request) {
        return artifacts.get(request.getChecksum());
    }

    @Override
    public ManagedDraft upsertDraft(ProviderContext context, ManagedDraftRequest request) {
        if (draftFailure != null) {
            throw writeFailure(draftFailure, "FakeDraftFailure", "fake-draft-request-id");
        }
        String draftId =
            request.getExistingDraftId() == null
                ? "fake-draft-" + request.getDefinitionHash().substring(0, 12)
                : request.getExistingDraftId();
        ManagedDraft draft =
            ManagedDraft.builder()
                .draftId(draftId)
                .providerRequestId("fake-draft-request-id")
                .definitionHash(request.getDefinitionHash())
                .build();
        drafts.put(draftId, draft);
        return draft;
    }

    @Override
    public ManagedDeployment deployDraft(
                                         ProviderContext context, ManagedDeployRequest request) {
        if (deploymentFailure != null && !failDeploymentAfterPersist) {
            throw writeFailure(
                deploymentFailure, "FakeDeploymentFailure", "fake-deploy-request-id");
        }
        ManagedDeployment deployment =
            ManagedDeployment.builder()
                .applicationId("fake-app-" + request.getDefinitionHash().substring(0, 12))
                .providerOperationId("fake-deploy-operation-id")
                .providerRequestId("fake-deploy-request-id")
                .definitionHash(request.getDefinitionHash())
                .build();
        deployments.put(request.getDraftId(), deployment);
        if (deploymentFailure != null) {
            throw writeFailure(
                deploymentFailure, "FakeDeploymentFailure", "fake-deploy-request-id");
        }
        return deployment;
    }

    @Override
    public ManagedDeployment findDeployment(
                                            ProviderContext context,
                                            ManagedDeploymentLookupRequest request) {
        return deployments.get(request.getDraftId());
    }

    @Override
    public ManagedJobActionResult startJob(
                                           ProviderContext context, ManagedJobStartRequest request) {
        return performJobAction("START", request.getJobId(), "fake-instance-1");
    }

    @Override
    public ManagedJobActionResult stopJob(
                                          ProviderContext context, ManagedJobStopRequest request) {
        return performJobAction("STOP", request.getJobId(), request.getInstanceId());
    }

    @Override
    public ManagedJobActionResult restartJob(
                                             ProviderContext context,
                                             ManagedJobRestartRequest request) {
        return performJobAction("RESTART", request.getJobId(), null);
    }

    @Override
    public ManagedJobStatus getJob(
                                   ProviderContext context, ManagedJobLookupRequest request) {
        jobLookupCount.incrementAndGet();
        if (jobLookupFailure != null) {
            throw writeFailure(
                jobLookupFailure, "FakeJobLookupFailure", "fake-job-lookup-request-id");
        }
        return jobs.get(request.getJobId());
    }

    @Override
    public List<ManagedSnapshot> listSnapshots(
                                               ProviderContext context,
                                               ManagedSnapshotLookupRequest request) {
        Map<String, ManagedSnapshot> jobSnapshots = snapshots.get(request.getJobId());
        return jobSnapshots == null
            ? Collections.emptyList()
            : Arrays.asList(jobSnapshots.values().toArray(new ManagedSnapshot[0]));
    }

    @Override
    public ManagedSnapshotCreateResult createSnapshot(
                                                      ProviderContext context,
                                                      ManagedSnapshotCreateRequest request) {
        snapshotCreateCount.incrementAndGet();
        String snapshotId = "fake-savepoint-" + snapshotSequence.incrementAndGet();
        snapshots
            .computeIfAbsent(request.getJobId(), ignored -> new ConcurrentHashMap<>())
            .put(
                snapshotId,
                ManagedSnapshot.builder()
                    .snapshotId(snapshotId)
                    .instanceId(request.getInstanceId())
                    .snapshotType("MANUAL")
                    .state(ManagedSnapshotState.COMPLETED)
                    .providerState("COMPLETED")
                    .description(request.getDescription())
                    .build());
        return ManagedSnapshotCreateResult.builder()
            .jobId(request.getJobId())
            .instanceId(request.getInstanceId())
            .providerRequestId("fake-snapshot-request-id")
            .providerState("CREATING")
            .build();
    }

    public void putSnapshot(String jobId, ManagedSnapshot snapshot) {
        snapshots
            .computeIfAbsent(jobId, ignored -> new ConcurrentHashMap<>())
            .put(snapshot.getSnapshotId(), snapshot);
    }

    public void removeSnapshot(String jobId, String snapshotId) {
        Map<String, ManagedSnapshot> jobSnapshots = snapshots.get(jobId);
        if (jobSnapshots != null) {
            jobSnapshots.remove(snapshotId);
        }
    }

    public void resetSnapshots() {
        snapshots.clear();
        snapshotSequence.set(0);
        snapshotCreateCount.set(0);
    }

    public int getSnapshotCreateCount() {
        return snapshotCreateCount.get();
    }

    public ManagedFlinkCapability capability() {
        return ManagedFlinkCapability.builder()
            .providerType(type())
            .apiVersion("fake-v1")
            .engineVersions(Collections.singletonList("1.20"))
            .jobTypes(Arrays.asList("STREAMING_SQL", "STREAMING_JAR"))
            .executionModes(Collections.singletonList("APPLICATION"))
            .startModes(Collections.singletonList("LATEST"))
            .schedulingStrategies(Collections.singletonList("DEFAULT"))
            .supportsProjectList(true)
            .supportsResourcePoolList(true)
            .supportsSqlDeepCheck(true)
            .supportsSkipPrecheck(false)
            .supportsStopWithSnapshot(true)
            .supportsCreateSnapshot(true)
            .supportsJarDirectUpload(true)
            .supportsCustomEndpoint(false)
            .minCpu(new BigDecimal("0.5"))
            .cpuStep(new BigDecimal("0.5"))
            .memoryPerCpuGiB(new BigDecimal("4"))
            .maxArtifactBytes(512L * 1024 * 1024)
            .customParameterRules(Collections.emptyMap())
            .capabilityRevision("fake-r1")
            .expireAt(clock.instant().plus(capabilityTtl))
            .build();
    }

    public void failCapabilityWith(ProviderErrorCategory category) {
        this.capabilityFailure = category;
    }

    public int getCapabilityRequestCount() {
        return capabilityRequestCount.get();
    }

    public void failArtifactWith(ProviderErrorCategory category, boolean afterPersist) {
        artifactFailure = category;
        failArtifactAfterPersist = afterPersist;
    }

    public void clearArtifactFailure() {
        artifactFailure = null;
        failArtifactAfterPersist = false;
    }

    public int getArtifactStageCount() {
        return artifactStageCount.get();
    }

    public void failDraftWith(ProviderErrorCategory category) {
        draftFailure = category;
    }

    public void failDeploymentWith(ProviderErrorCategory category) {
        failDeploymentWith(category, false);
    }

    public void failDeploymentWith(ProviderErrorCategory category, boolean afterPersist) {
        deploymentFailure = category;
        failDeploymentAfterPersist = afterPersist;
    }

    public void clearWriteFailures() {
        draftFailure = null;
        deploymentFailure = null;
        failDeploymentAfterPersist = false;
        jobActionFailure = null;
        failJobActionAfterPersist = false;
    }

    public void failJobActionWith(ProviderErrorCategory category, boolean afterPersist) {
        jobActionFailure = category;
        failJobActionAfterPersist = afterPersist;
    }

    public int getJobActionCount() {
        return jobActionCount.get();
    }

    public void resetJobActions() {
        jobActions.clear();
        jobs.clear();
        jobActionCount.set(0);
        jobLookupCount.set(0);
        jobActionFailure = null;
        jobLookupFailure = null;
        failJobActionAfterPersist = false;
    }

    public ManagedJobActionResult getJobAction(String action, String jobId) {
        return jobActions.get(action + ":" + jobId);
    }

    public void setJobStatus(
                             String jobId,
                             String instanceId,
                             ManagedJobState state) {
        jobs.put(
            jobId,
            ManagedJobStatus.builder()
                .jobId(jobId)
                .instanceId(instanceId)
                .state(state)
                .providerState(state.name())
                .providerRequestId("fake-job-lookup-request-id")
                .build());
    }

    public void failJobLookupWith(ProviderErrorCategory category) {
        jobLookupFailure = category;
    }

    public void clearJobLookupFailure() {
        jobLookupFailure = null;
    }

    public int getJobLookupCount() {
        return jobLookupCount.get();
    }

    private ManagedJobActionResult performJobAction(
                                                    String action,
                                                    String jobId,
                                                    String instanceId) {
        jobActionCount.incrementAndGet();
        if (jobActionFailure != null && !failJobActionAfterPersist) {
            throw writeFailure(
                jobActionFailure, "FakeJobActionFailure", "fake-job-action-request-id");
        }
        ManagedJobActionResult result =
            ManagedJobActionResult.builder()
                .jobId(jobId)
                .instanceId(instanceId)
                .providerOperationId("fake-" + action.toLowerCase() + "-operation-id")
                .providerRequestId("fake-job-action-request-id")
                .providerState(action + "ING")
                .build();
        jobActions.put(action + ":" + jobId, result);
        jobs.put(
            jobId,
            ManagedJobStatus.builder()
                .jobId(jobId)
                .instanceId(instanceId)
                .state(jobState(action))
                .providerState(action + "ING")
                .providerRequestId("fake-job-lookup-request-id")
                .build());
        if (jobActionFailure != null) {
            throw writeFailure(
                jobActionFailure, "FakeJobActionFailure", "fake-job-action-request-id");
        }
        return result;
    }

    private static ManagedJobState jobState(String action) {
        switch (action) {
            case "START":
                return ManagedJobState.STARTING;
            case "STOP":
                return ManagedJobState.STOPPING;
            case "RESTART":
                return ManagedJobState.RESTARTING;
            default:
                return ManagedJobState.OTHER;
        }
    }

    private ManagedFlinkProviderException artifactFailure() {
        return new ManagedFlinkProviderException(
            artifactFailure,
            "FakeArtifactFailure",
            "fake-artifact-request-id",
            "Fake artifact failure");
    }

    private static ManagedFlinkProviderException writeFailure(
                                                              ProviderErrorCategory category,
                                                              String code,
                                                              String requestId) {
        return new ManagedFlinkProviderException(
            category, code, requestId, "Fake provider write failure");
    }
}
