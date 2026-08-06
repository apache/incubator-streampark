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
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.core.entity.FlinkApplication;
import org.apache.streampark.console.core.entity.FlinkCluster;
import org.apache.streampark.console.core.entity.ManagedFlinkApplication;
import org.apache.streampark.console.core.entity.ManagedFlinkArtifact;
import org.apache.streampark.console.core.entity.ManagedFlinkEnvironment;
import org.apache.streampark.console.core.managed.api.ArtifactLookupRequest;
import org.apache.streampark.console.core.managed.api.ArtifactStageRequest;
import org.apache.streampark.console.core.managed.api.ManagedFlinkProviderException;
import org.apache.streampark.console.core.managed.api.StagedArtifact;
import org.apache.streampark.console.core.managed.model.ManagedFlinkArtifactView;
import org.apache.streampark.console.core.managed.model.ManagedFlinkReleaseConfig;
import org.apache.streampark.console.core.mapper.FlinkApplicationMapper;
import org.apache.streampark.console.core.mapper.FlinkClusterMapper;
import org.apache.streampark.console.core.mapper.ManagedFlinkApplicationMapper;
import org.apache.streampark.console.core.mapper.ManagedFlinkArtifactMapper;
import org.apache.streampark.console.core.mapper.ManagedFlinkEnvironmentMapper;

import org.apache.commons.lang3.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** Checksum-addressed managed Flink artifact staging with lease-based failure recovery. */
@Service
@RequiredArgsConstructor
public class ManagedFlinkArtifactServiceImpl implements ManagedFlinkArtifactService {

    private static final String STATE_STAGING = "STAGING";
    private static final String STATE_READY = "READY";
    private static final String STATE_FAILED = "FAILED";
    private static final long DEFAULT_MAX_ARTIFACT_BYTES = 500L * 1024 * 1024;
    private static final long MAX_TOTAL_ARTIFACT_BYTES = 1024L * 1024 * 1024;
    private static final long STAGING_LEASE_MILLIS = TimeUnit.MINUTES.toMillis(5);
    private static final int MAX_ERROR_LENGTH = 512;
    private static final Set<String> SAFE_PROVIDER_URI_SCHEMES =
        Set.of("tos", "s3", "oss", "gs");

    private final ManagedFlinkArtifactMapper artifactMapper;
    private final ManagedFlinkApplicationMapper managedApplicationMapper;
    private final ManagedFlinkEnvironmentMapper environmentMapper;
    private final FlinkApplicationMapper applicationMapper;
    private final FlinkClusterMapper clusterMapper;
    private final CloudAccountAuthorizationService authorizationService;
    private final ManagedFlinkProviderContextService contextService;
    private final ManagedFlinkArtifactSourceResolver sourceResolver;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    @Override
    public List<ManagedFlinkArtifactView> stageApplicationArtifacts(Long teamId, Long appId) {
        ApplicationContext application = requireApplication(teamId, appId);
        ManagedFlinkReleaseConfig release =
            readRelease(application.getManaged().getReleaseConfigJson());
        Set<String> resourceNames = new LinkedHashSet<>();
        if (application.getApplication().isFlinkJar()) {
            resourceNames.add(application.getApplication().getJar());
        }
        resourceNames.addAll(release.getDependencyResourceNames());
        if (resourceNames.isEmpty()) {
            return Collections.emptyList();
        }
        EnvironmentContext environment =
            requireEnvironment(teamId, application.getManaged().getManagedEnvId());
        ManagedFlinkProviderSession session =
            contextService.resolve(
                teamId,
                environment.getEnvironment().getCloudAccountId(),
                environment.getEnvironment().getProviderConfigJson(),
                environment.getEnvironment().getProviderConfigVersion());
        ApiAlertException.throwIfFalse(
            session.getProvider().getCapability(session.getContext()).isSupportsJarDirectUpload(),
            "Managed Flink environment does not support direct artifact staging.");

        List<ResolvedArtifactSource> sources = new ArrayList<>();
        long totalBytes = 0;
        for (String resourceName : resourceNames) {
            ResolvedArtifactSource source =
                sourceResolver.resolve(
                    teamId, resourceName, environment.getMaxArtifactBytes());
            totalBytes = Math.addExact(totalBytes, source.getFileSize());
            ApiAlertException.throwIfTrue(
                totalBytes > MAX_TOTAL_ARTIFACT_BYTES,
                "Managed Flink artifact total size exceeds the release limit.");
            sources.add(source);
        }

        Map<String, ManagedFlinkArtifactView> stagedByChecksum = new LinkedHashMap<>();
        for (ResolvedArtifactSource source : sources) {
            ManagedFlinkArtifactView staged =
                stage(
                    environment.getEnvironment().getClusterId(),
                    session,
                    source);
            stagedByChecksum.putIfAbsent(staged.getChecksum(), staged);
        }
        return new ArrayList<>(stagedByChecksum.values());
    }

    private ManagedFlinkArtifactView stage(
                                           Long environmentId,
                                           ManagedFlinkProviderSession session,
                                           ResolvedArtifactSource source) {
        String owner = UUID.randomUUID().toString();
        ArtifactClaim claim = claim(environmentId, source, owner);
        if (!claim.isOwned()) {
            return view(claim.getArtifact());
        }
        ArtifactStageRequest request =
            ArtifactStageRequest.builder()
                .checksum(source.getChecksum())
                .fileName(source.getFileName())
                .contentAddressedName(source.getContentAddressedName())
                .fileSize(source.getFileSize())
                .content(source.getContent())
                .build();
        try {
            StagedArtifact staged =
                session.getProvider().stageArtifact(session.getContext(), request);
            validateProviderArtifact(staged);
            return persistReady(claim.getArtifact(), owner, staged);
        } catch (ManagedFlinkProviderException failure) {
            if (failure.isRetryable()) {
                StagedArtifact reconciled =
                    reconcile(session, source);
                if (reconciled != null) {
                    validateProviderArtifact(reconciled);
                    return persistReady(claim.getArtifact(), owner, reconciled);
                }
            }
            persistFailure(claim.getArtifact(), owner, safeCode(failure));
            throw new ApiAlertException(
                "Managed Flink artifact staging failed: " + safeCode(failure));
        } catch (RuntimeException failure) {
            persistFailure(claim.getArtifact(), owner, "UNKNOWN:ArtifactStageFailure");
            throw failure;
        }
    }

    private StagedArtifact reconcile(
                                     ManagedFlinkProviderSession session,
                                     ResolvedArtifactSource source) {
        try {
            return session.getProvider()
                .findArtifact(
                    session.getContext(),
                    ArtifactLookupRequest.builder()
                        .checksum(source.getChecksum())
                        .contentAddressedName(source.getContentAddressedName())
                        .build());
        } catch (ManagedFlinkProviderException ignored) {
            return null;
        }
    }

    private ArtifactClaim claim(
                                Long environmentId,
                                ResolvedArtifactSource source,
                                String owner) {
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                ArtifactClaim claim =
                    transactionTemplate.execute(
                        status -> claimInTransaction(environmentId, source, owner));
                ApiAlertException.throwIfNull(
                    claim, "Managed Flink artifact claim failed.");
                return claim;
            } catch (DuplicateKeyException ignored) {
                // Another worker inserted the checksum cache entry first; reload and claim it.
            }
        }
        throw new ApiAlertException(
            "Managed Flink artifact is being staged by another worker.");
    }

    private ArtifactClaim claimInTransaction(
                                             Long environmentId,
                                             ResolvedArtifactSource source,
                                             String owner) {
        ManagedFlinkArtifact existing = find(environmentId, source.getChecksum());
        Date now = new Date();
        if (existing == null) {
            ManagedFlinkArtifact artifact = new ManagedFlinkArtifact();
            artifact.setManagedEnvId(environmentId);
            artifact.setSourceResourceId(source.getResourceId());
            artifact.setChecksum(source.getChecksum());
            artifact.setFileName(source.getFileName());
            artifact.setFileSize(source.getFileSize());
            artifact.setState(STATE_STAGING);
            artifact.setReferenceCount(0);
            artifact.setStageAttempts(1);
            artifact.setStagingOwner(owner);
            artifact.setStagingLeaseUntil(new Date(now.getTime() + STAGING_LEASE_MILLIS));
            artifact.setVersion(0);
            artifact.setCreateTime(now);
            artifact.setModifyTime(now);
            ApiAlertException.throwIfFalse(
                artifactMapper.insert(artifact) == 1,
                "Managed Flink artifact claim could not be created.");
            return new ArtifactClaim(artifact, true);
        }
        if (STATE_READY.equals(existing.getState())) {
            validatePersistedArtifact(existing);
            return new ArtifactClaim(existing, false);
        }
        ApiAlertException.throwIfTrue(
            STATE_STAGING.equals(existing.getState())
                && existing.getStagingLeaseUntil() != null
                && existing.getStagingLeaseUntil().after(now),
            "Managed Flink artifact is being staged by another worker.");
        int nextVersion = existing.getVersion() + 1;
        Integer updated =
            artifactMapper.update(
                null,
                new LambdaUpdateWrapper<ManagedFlinkArtifact>()
                    .eq(ManagedFlinkArtifact::getId, existing.getId())
                    .eq(ManagedFlinkArtifact::getVersion, existing.getVersion())
                    .set(ManagedFlinkArtifact::getSourceResourceId, source.getResourceId())
                    .set(ManagedFlinkArtifact::getFileName, source.getFileName())
                    .set(ManagedFlinkArtifact::getFileSize, source.getFileSize())
                    .set(ManagedFlinkArtifact::getState, STATE_STAGING)
                    .set(
                        ManagedFlinkArtifact::getStageAttempts,
                        existing.getStageAttempts() + 1)
                    .set(ManagedFlinkArtifact::getLastErrorCode, null)
                    .set(ManagedFlinkArtifact::getLastErrorMessage, null)
                    .set(ManagedFlinkArtifact::getStagingOwner, owner)
                    .set(
                        ManagedFlinkArtifact::getStagingLeaseUntil,
                        new Date(now.getTime() + STAGING_LEASE_MILLIS))
                    .set(ManagedFlinkArtifact::getModifyTime, now)
                    .set(ManagedFlinkArtifact::getVersion, nextVersion));
        if (updated == 0) {
            throw new DuplicateKeyException("Managed Flink artifact claim raced.");
        }
        existing.setSourceResourceId(source.getResourceId());
        existing.setFileName(source.getFileName());
        existing.setFileSize(source.getFileSize());
        existing.setState(STATE_STAGING);
        existing.setStageAttempts(existing.getStageAttempts() + 1);
        existing.setStagingOwner(owner);
        existing.setVersion(nextVersion);
        return new ArtifactClaim(existing, true);
    }

    private ManagedFlinkArtifactView persistReady(
                                                  ManagedFlinkArtifact artifact,
                                                  String owner,
                                                  StagedArtifact staged) {
        Date now = new Date();
        Integer updated =
            transactionTemplate.execute(
                status -> artifactMapper.update(
                    null,
                    new LambdaUpdateWrapper<ManagedFlinkArtifact>()
                        .eq(ManagedFlinkArtifact::getId, artifact.getId())
                        .eq(ManagedFlinkArtifact::getVersion, artifact.getVersion())
                        .eq(ManagedFlinkArtifact::getStagingOwner, owner)
                        .set(
                            ManagedFlinkArtifact::getProviderArtifactId,
                            staged.getProviderArtifactId())
                        .set(
                            ManagedFlinkArtifact::getProviderArtifactVersion,
                            staged.getProviderArtifactVersion())
                        .set(ManagedFlinkArtifact::getProviderUri, staged.getProviderUri())
                        .set(
                            ManagedFlinkArtifact::getProviderRequestId,
                            staged.getProviderRequestId())
                        .set(ManagedFlinkArtifact::getState, STATE_READY)
                        .set(ManagedFlinkArtifact::getLastErrorCode, null)
                        .set(ManagedFlinkArtifact::getLastErrorMessage, null)
                        .set(ManagedFlinkArtifact::getStagingOwner, null)
                        .set(ManagedFlinkArtifact::getStagingLeaseUntil, null)
                        .set(ManagedFlinkArtifact::getModifyTime, now)
                        .set(
                            ManagedFlinkArtifact::getVersion,
                            artifact.getVersion() + 1)));
        ApiAlertException.throwIfTrue(
            updated == null || updated != 1,
            "Managed Flink artifact staging result was superseded.");
        artifact.setProviderArtifactId(staged.getProviderArtifactId());
        artifact.setProviderArtifactVersion(staged.getProviderArtifactVersion());
        artifact.setProviderUri(staged.getProviderUri());
        artifact.setProviderRequestId(staged.getProviderRequestId());
        artifact.setState(STATE_READY);
        artifact.setVersion(artifact.getVersion() + 1);
        return view(artifact);
    }

    private void persistFailure(
                                ManagedFlinkArtifact artifact,
                                String owner,
                                String errorCode) {
        Date now = new Date();
        transactionTemplate.executeWithoutResult(
            status -> artifactMapper.update(
                null,
                new LambdaUpdateWrapper<ManagedFlinkArtifact>()
                    .eq(ManagedFlinkArtifact::getId, artifact.getId())
                    .eq(ManagedFlinkArtifact::getVersion, artifact.getVersion())
                    .eq(ManagedFlinkArtifact::getStagingOwner, owner)
                    .set(ManagedFlinkArtifact::getState, STATE_FAILED)
                    .set(
                        ManagedFlinkArtifact::getLastErrorCode,
                        StringUtils.abbreviate(errorCode, MAX_ERROR_LENGTH))
                    .set(
                        ManagedFlinkArtifact::getLastErrorMessage,
                        "Artifact staging did not produce a reusable provider reference.")
                    .set(ManagedFlinkArtifact::getStagingOwner, null)
                    .set(ManagedFlinkArtifact::getStagingLeaseUntil, null)
                    .set(ManagedFlinkArtifact::getModifyTime, now)
                    .set(
                        ManagedFlinkArtifact::getVersion,
                        artifact.getVersion() + 1)));
    }

    private ApplicationContext requireApplication(Long teamId, Long appId) {
        FlinkApplication application = applicationMapper.selectById(appId);
        ManagedFlinkApplication managed = managedApplicationMapper.selectById(appId);
        ApiAlertException.throwIfTrue(
            application == null
                || managed == null
                || !teamId.equals(application.getTeamId()),
            "Managed Flink application does not exist.");
        authorizationService.requireEnvironmentAuthorized(teamId, managed.getManagedEnvId());
        return new ApplicationContext(application, managed);
    }

    private EnvironmentContext requireEnvironment(Long teamId, Long environmentId) {
        ManagedFlinkEnvironment environment = environmentMapper.selectById(environmentId);
        FlinkCluster cluster = clusterMapper.selectById(environmentId);
        ApiAlertException.throwIfTrue(
            environment == null
                || cluster == null
                || !ClusterState.RUNNING.getState().equals(cluster.getClusterState())
                || StringUtils.isBlank(environment.getCapabilityJson()),
            "Managed Flink environment must be probed successfully before staging artifacts.");
        authorizationService.requireEnvironmentAuthorized(teamId, environmentId);
        try {
            JsonNode capability = objectMapper.readTree(environment.getCapabilityJson());
            long configuredMaxArtifactBytes =
                capability.path("maxArtifactBytes").asLong(DEFAULT_MAX_ARTIFACT_BYTES);
            return new EnvironmentContext(
                environment,
                capability.path("supportsJarDirectUpload").asBoolean(false),
                configuredMaxArtifactBytes > 0
                    ? configuredMaxArtifactBytes
                    : DEFAULT_MAX_ARTIFACT_BYTES);
        } catch (Exception exception) {
            throw new ApiAlertException(
                "Managed Flink environment capability is invalid.");
        }
    }

    private ManagedFlinkReleaseConfig readRelease(String value) {
        try {
            ManagedFlinkReleaseConfig release =
                objectMapper.readValue(value, ManagedFlinkReleaseConfig.class);
            if (release.getDependencyResourceNames() == null) {
                release.setDependencyResourceNames(new ArrayList<>());
            }
            return release;
        } catch (Exception exception) {
            throw new ApiAlertException(
                "Managed Flink release configuration is invalid.");
        }
    }

    private ManagedFlinkArtifact find(Long environmentId, String checksum) {
        return artifactMapper.selectOne(
            new LambdaQueryWrapper<ManagedFlinkArtifact>()
                .eq(ManagedFlinkArtifact::getManagedEnvId, environmentId)
                .eq(ManagedFlinkArtifact::getChecksum, checksum));
    }

    private static void validateProviderArtifact(StagedArtifact artifact) {
        ApiAlertException.throwIfTrue(
            artifact == null
                || StringUtils.isBlank(artifact.getProviderArtifactId())
                || artifact.getProviderArtifactId().length() > 128
                || artifact.getProviderArtifactVersion() == null
                || artifact.getProviderArtifactVersion() <= 0
                || StringUtils.isBlank(artifact.getProviderUri())
                || artifact.getProviderUri().length() > 1024
                || StringUtils.length(artifact.getProviderRequestId()) > 128,
            "Managed Flink provider returned an invalid artifact reference.");
        validateProviderUri(artifact.getProviderUri());
    }

    private static void validatePersistedArtifact(ManagedFlinkArtifact artifact) {
        ApiAlertException.throwIfTrue(
            StringUtils.isBlank(artifact.getProviderArtifactId())
                || artifact.getProviderArtifactId().length() > 128
                || artifact.getProviderArtifactVersion() == null
                || artifact.getProviderArtifactVersion() <= 0
                || StringUtils.isBlank(artifact.getProviderUri())
                || artifact.getProviderUri().length() > 1024,
            "Managed Flink artifact cache record is invalid.");
        validateProviderUri(artifact.getProviderUri());
    }

    private static void validateProviderUri(String value) {
        try {
            URI uri = URI.create(value);
            ApiAlertException.throwIfTrue(
                !uri.isAbsolute()
                    || !SAFE_PROVIDER_URI_SCHEMES.contains(
                        StringUtils.lowerCase(uri.getScheme()))
                    || uri.getUserInfo() != null
                    || uri.getQuery() != null
                    || uri.getFragment() != null,
                "Managed Flink provider returned an unsafe artifact URI.");
        } catch (IllegalArgumentException exception) {
            throw new ApiAlertException(
                "Managed Flink provider returned an invalid artifact URI.");
        }
    }

    private static ManagedFlinkArtifactView view(ManagedFlinkArtifact artifact) {
        return ManagedFlinkArtifactView.builder()
            .id(artifact.getId())
            .managedEnvironmentId(artifact.getManagedEnvId())
            .sourceResourceId(artifact.getSourceResourceId())
            .checksum(artifact.getChecksum())
            .fileName(artifact.getFileName())
            .fileSize(artifact.getFileSize())
            .providerArtifactId(artifact.getProviderArtifactId())
            .providerArtifactVersion(artifact.getProviderArtifactVersion())
            .providerUri(artifact.getProviderUri())
            .state(artifact.getState())
            .build();
    }

    private static String safeCode(ManagedFlinkProviderException failure) {
        return failure.getCategory().name()
            + ":"
            + StringUtils.defaultIfBlank(failure.getProviderCode(), "Unknown");
    }

    @Value
    private static class ArtifactClaim {

        ManagedFlinkArtifact artifact;

        boolean owned;
    }

    @Value
    private static class ApplicationContext {

        FlinkApplication application;

        ManagedFlinkApplication managed;
    }

    @Value
    private static class EnvironmentContext {

        ManagedFlinkEnvironment environment;

        boolean directUploadSupported;

        long maxArtifactBytes;
    }
}
