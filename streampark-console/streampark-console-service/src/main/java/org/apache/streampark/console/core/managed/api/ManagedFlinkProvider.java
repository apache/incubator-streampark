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

package org.apache.streampark.console.core.managed.api;

import java.util.List;

/**
 * Provider-neutral contract for managed Flink services.
 *
 * <p>SDK request and response types must never cross this boundary.
 */
public interface ManagedFlinkProvider {

    ManagedFlinkProviderType type();

    ManagedFlinkCapability getCapability(ProviderContext context);

    CredentialCheckResult validateCredential(ProviderContext context);

    List<CloudProject> listProjects(ProviderContext context, String keyword);

    List<ManagedResourcePool> listResourcePools(
                                                ProviderContext context, String projectId, String keyword);

    default List<ManagedDraftDirectory> listDraftDirectories(
                                                             ProviderContext context,
                                                             String projectId,
                                                             String keyword) {
        throw new UnsupportedOperationException("Managed draft directory discovery is not supported.");
    }

    StagedArtifact stageArtifact(ProviderContext context, ArtifactStageRequest request);

    StagedArtifact findArtifact(ProviderContext context, ArtifactLookupRequest request);

    default ManagedDraft upsertDraft(ProviderContext context, ManagedDraftRequest request) {
        throw new UnsupportedOperationException("Managed draft upsert is not supported.");
    }

    default ManagedDeployment deployDraft(
                                          ProviderContext context, ManagedDeployRequest request) {
        throw new UnsupportedOperationException("Managed draft deployment is not supported.");
    }

    default ManagedDeployment findDeployment(
                                             ProviderContext context,
                                             ManagedDeploymentLookupRequest request) {
        throw new UnsupportedOperationException("Managed deployment lookup is not supported.");
    }

    default ManagedJobActionResult startJob(
                                            ProviderContext context,
                                            ManagedJobStartRequest request) {
        throw new UnsupportedOperationException("Managed job start is not supported.");
    }

    default ManagedJobActionResult stopJob(
                                           ProviderContext context,
                                           ManagedJobStopRequest request) {
        throw new UnsupportedOperationException("Managed job stop is not supported.");
    }

    default ManagedJobActionResult restartJob(
                                              ProviderContext context,
                                              ManagedJobRestartRequest request) {
        throw new UnsupportedOperationException("Managed job restart is not supported.");
    }

    default ManagedJobStatus getJob(
                                    ProviderContext context,
                                    ManagedJobLookupRequest request) {
        throw new UnsupportedOperationException("Managed job lookup is not supported.");
    }

    default List<ManagedSnapshot> listSnapshots(
                                                ProviderContext context,
                                                ManagedSnapshotLookupRequest request) {
        throw new UnsupportedOperationException("Managed snapshot lookup is not supported.");
    }

    default ManagedSnapshotCreateResult createSnapshot(
                                                       ProviderContext context,
                                                       ManagedSnapshotCreateRequest request) {
        throw new UnsupportedOperationException("Managed snapshot creation is not supported.");
    }
}
