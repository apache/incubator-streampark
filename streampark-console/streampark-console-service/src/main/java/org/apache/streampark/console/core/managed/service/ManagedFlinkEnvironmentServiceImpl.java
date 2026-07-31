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
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.base.util.JacksonUtils;
import org.apache.streampark.console.core.entity.CloudAccount;
import org.apache.streampark.console.core.entity.CloudAccountTeam;
import org.apache.streampark.console.core.entity.FlinkCluster;
import org.apache.streampark.console.core.entity.ManagedFlinkEnvironment;
import org.apache.streampark.console.core.managed.api.CloudProject;
import org.apache.streampark.console.core.managed.api.CredentialCheckResult;
import org.apache.streampark.console.core.managed.api.ManagedFlinkCapability;
import org.apache.streampark.console.core.managed.api.ManagedFlinkProviderException;
import org.apache.streampark.console.core.managed.api.ManagedResourcePool;
import org.apache.streampark.console.core.managed.model.ManagedFlinkEnvironmentCreateRequest;
import org.apache.streampark.console.core.managed.model.ManagedFlinkEnvironmentListRequest;
import org.apache.streampark.console.core.managed.model.ManagedFlinkEnvironmentUpdateRequest;
import org.apache.streampark.console.core.managed.model.ManagedFlinkEnvironmentVersionedIdRequest;
import org.apache.streampark.console.core.managed.model.ManagedFlinkEnvironmentView;
import org.apache.streampark.console.core.mapper.CloudAccountMapper;
import org.apache.streampark.console.core.mapper.CloudAccountTeamMapper;
import org.apache.streampark.console.core.mapper.FlinkClusterMapper;
import org.apache.streampark.console.core.mapper.ManagedFlinkEnvironmentMapper;
import org.apache.streampark.console.core.service.application.FlinkApplicationInfoService;

import org.apache.commons.lang3.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Default managed Flink environment registration and probe service. */
@Service
@RequiredArgsConstructor
public class ManagedFlinkEnvironmentServiceImpl implements ManagedFlinkEnvironmentService {

    private static final String PERMISSION_USE = "USE";
    private static final int MAX_PROBE_ERROR_LENGTH = 512;

    private final ManagedFlinkEnvironmentMapper environmentMapper;
    private final FlinkClusterMapper clusterMapper;
    private final CloudAccountMapper cloudAccountMapper;
    private final CloudAccountTeamMapper cloudAccountTeamMapper;
    private final CloudAccountAuthorizationService authorizationService;
    private final ManagedFlinkProviderContextService contextService;
    private final ManagedFlinkAuditContext auditContext;
    private final FlinkApplicationInfoService applicationInfoService;
    private final TransactionTemplate transactionTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ManagedFlinkEnvironmentCreateRequest request) {
        authorizationService.requireAuthorized(request.getTeamId(), request.getCloudAccountId());
        CloudAccount account = requireAccount(request.getCloudAccountId());
        ensureClusterNameUnique(request.getClusterName(), null);

        FlinkCluster cluster = new FlinkCluster();
        cluster.setClusterName(request.getClusterName().trim());
        cluster.setDescription(StringUtils.trimToNull(request.getDescription()));
        cluster.setDeployMode(FlinkDeployMode.MANAGED_APPLICATION.getMode());
        cluster.setVersionId(null);
        cluster.setUserId(auditContext.currentUserId());
        cluster.setClusterState(ClusterState.CREATED.getState());
        cluster.setCreateTime(new Date());
        ApiAlertException.throwIfFalse(
            clusterMapper.insert(cluster) == 1,
            "Failed to create the managed Flink environment registration.");

        ManagedFlinkEnvironment environment = new ManagedFlinkEnvironment();
        environment.setClusterId(cluster.getId());
        applyRegistration(
            environment,
            account,
            request.getProjectId(),
            request.getProjectName(),
            request.getResourcePoolId(),
            request.getResourcePoolName(),
            request.getDraftDirectoryId());
        environment.setVersion(0);
        ApiAlertException.throwIfFalse(
            environmentMapper.insert(environment) == 1,
            "Failed to create the managed Flink environment registration.");
        return cluster.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(ManagedFlinkEnvironmentUpdateRequest request) {
        ManagedFlinkEnvironment existing =
            requireAuthorizedEnvironment(request.getTeamId(), request.getClusterId());
        authorizationService.requireAuthorized(request.getTeamId(), request.getCloudAccountId());
        CloudAccount account = requireAccount(request.getCloudAccountId());
        ensureClusterNameUnique(request.getClusterName(), request.getClusterId());

        int updated = environmentMapper.update(
            null,
            new LambdaUpdateWrapper<ManagedFlinkEnvironment>()
                .eq(ManagedFlinkEnvironment::getClusterId, request.getClusterId())
                .eq(ManagedFlinkEnvironment::getVersion, request.getVersion())
                .set(ManagedFlinkEnvironment::getProviderType, account.getProviderType())
                .set(ManagedFlinkEnvironment::getCloudAccountId, account.getId())
                .set(ManagedFlinkEnvironment::getRegion, account.getRegion())
                .set(ManagedFlinkEnvironment::getProjectId, request.getProjectId().trim())
                .set(
                    ManagedFlinkEnvironment::getProjectName,
                    StringUtils.trimToNull(request.getProjectName()))
                .set(
                    ManagedFlinkEnvironment::getResourcePoolId,
                    request.getResourcePoolId().trim())
                .set(
                    ManagedFlinkEnvironment::getResourcePoolName,
                    StringUtils.trimToNull(request.getResourcePoolName()))
                .set(
                    ManagedFlinkEnvironment::getDraftDirectoryId,
                    request.getDraftDirectoryId())
                .set(ManagedFlinkEnvironment::getConsoleUrl, null)
                .set(ManagedFlinkEnvironment::getCapabilityJson, null)
                .set(ManagedFlinkEnvironment::getLastProbeTime, null)
                .set(ManagedFlinkEnvironment::getLastProbeError, null)
                .set(ManagedFlinkEnvironment::getVersion, request.getVersion() + 1));
        requireVersionUpdated(updated);

        int clusterUpdated = clusterMapper.update(
            null,
            new LambdaUpdateWrapper<FlinkCluster>()
                .eq(FlinkCluster::getId, existing.getClusterId())
                .eq(FlinkCluster::getDeployMode, FlinkDeployMode.MANAGED_APPLICATION.getMode())
                .set(FlinkCluster::getClusterName, request.getClusterName().trim())
                .set(
                    FlinkCluster::getDescription,
                    StringUtils.trimToNull(request.getDescription()))
                .set(FlinkCluster::getClusterState, ClusterState.CREATED.getState())
                .set(FlinkCluster::getException, null)
                .set(FlinkCluster::getStartTime, null)
                .set(FlinkCluster::getEndTime, null));
        ApiAlertException.throwIfFalse(
            clusterUpdated == 1, "Managed Flink environment cluster record is invalid.");
    }

    @Override
    public ManagedFlinkEnvironmentView get(Long teamId, Long clusterId) {
        ManagedFlinkEnvironment environment = requireAuthorizedEnvironment(teamId, clusterId);
        return toView(requireManagedCluster(clusterId), environment);
    }

    @Override
    public List<ManagedFlinkEnvironmentView> list(ManagedFlinkEnvironmentListRequest request) {
        List<Long> accountIds;
        if (request.getCloudAccountId() != null) {
            authorizationService.requireAuthorized(
                request.getTeamId(), request.getCloudAccountId());
            accountIds = Collections.singletonList(request.getCloudAccountId());
        } else {
            accountIds =
                cloudAccountTeamMapper.selectList(
                    new LambdaQueryWrapper<CloudAccountTeam>()
                        .eq(CloudAccountTeam::getTeamId, request.getTeamId())
                        .eq(CloudAccountTeam::getPermissionLevel, PERMISSION_USE))
                    .stream()
                    .map(CloudAccountTeam::getCloudAccountId)
                    .collect(Collectors.toList());
        }
        if (accountIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<ManagedFlinkEnvironment> environments =
            environmentMapper.selectList(
                new LambdaQueryWrapper<ManagedFlinkEnvironment>()
                    .in(ManagedFlinkEnvironment::getCloudAccountId, accountIds)
                    .orderByAsc(ManagedFlinkEnvironment::getClusterId));
        if (environments.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, FlinkCluster> clusters =
            clusterMapper.selectBatchIds(
                environments.stream()
                    .map(ManagedFlinkEnvironment::getClusterId)
                    .collect(Collectors.toList()))
                .stream()
                .filter(
                    cluster -> FlinkDeployMode.isManagedMode(cluster.getDeployMode()))
                .filter(
                    cluster -> StringUtils.isBlank(request.getClusterName())
                        || StringUtils.containsIgnoreCase(
                            cluster.getClusterName(), request.getClusterName().trim()))
                .collect(Collectors.toMap(FlinkCluster::getId, Function.identity()));
        return environments.stream()
            .filter(environment -> clusters.containsKey(environment.getClusterId()))
            .map(environment -> toView(clusters.get(environment.getClusterId()), environment))
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(ManagedFlinkEnvironmentVersionedIdRequest request) {
        ManagedFlinkEnvironment environment =
            requireAuthorizedEnvironment(request.getTeamId(), request.getClusterId());
        ApiAlertException.throwIfTrue(
            applicationInfoService.existsByClusterId(request.getClusterId()),
            "Applications reference this managed Flink environment and it cannot be deleted.");
        int deleted = environmentMapper.delete(
            new LambdaQueryWrapper<ManagedFlinkEnvironment>()
                .eq(ManagedFlinkEnvironment::getClusterId, environment.getClusterId())
                .eq(ManagedFlinkEnvironment::getVersion, request.getVersion()));
        requireVersionUpdated(deleted);
        ApiAlertException.throwIfFalse(
            clusterMapper.deleteById(environment.getClusterId()) == 1,
            "Failed to delete the managed Flink environment cluster record.");
    }

    @Override
    public ManagedFlinkEnvironmentView probe(Long teamId, Long clusterId) {
        ManagedFlinkEnvironment environment =
            requireAuthorizedEnvironment(teamId, clusterId);
        try {
            ProbeSuccess success = executeProbe(teamId, environment);
            transactionTemplate.executeWithoutResult(
                status -> persistProbeSuccess(environment, success));
        } catch (ProbeValidationException exception) {
            transactionTemplate.executeWithoutResult(
                status -> persistProbeFailure(
                    environment, ClusterState.FAILED, exception.getSafeError()));
        } catch (ManagedFlinkProviderException exception) {
            ClusterState state =
                exception.isRetryable() ? ClusterState.LOST : ClusterState.FAILED;
            transactionTemplate.executeWithoutResult(
                status -> persistProbeFailure(
                    environment, state, providerError(exception)));
        } catch (RuntimeException exception) {
            transactionTemplate.executeWithoutResult(
                status -> persistProbeFailure(
                    environment, ClusterState.FAILED, "CONFIGURATION:ProviderUnavailable"));
        }
        return get(teamId, clusterId);
    }

    private ProbeSuccess executeProbe(
                                      Long teamId, ManagedFlinkEnvironment environment) {
        ManagedFlinkProviderSession session =
            contextService.resolve(
                teamId, environment.getCloudAccountId(), environment.getProjectId());
        CredentialCheckResult credential =
            session.getProvider().validateCredential(session.getContext());
        if (credential == null || !credential.isSuccess()) {
            throw new ProbeValidationException("AUTHENTICATION:CredentialRejected");
        }
        ManagedFlinkCapability capability =
            session.getProvider().getCapability(session.getContext());
        if (capability == null || capability.getProviderType() != session.getProviderType()) {
            throw new ProbeValidationException("VALIDATION:InvalidCapability");
        }

        String projectName = environment.getProjectName();
        if (capability.isSupportsProjectList()) {
            CloudProject project =
                session.getProvider().listProjects(session.getContext(), null).stream()
                    .filter(item -> Objects.equals(item.getId(), environment.getProjectId()))
                    .findFirst()
                    .orElseThrow(
                        () -> new ProbeValidationException("VALIDATION:ProjectNotFound"));
            projectName = project.getName();
        }

        String resourcePoolName = environment.getResourcePoolName();
        if (capability.isSupportsResourcePoolList()) {
            ManagedResourcePool resourcePool =
                session
                    .getProvider()
                    .listResourcePools(
                        session.getContext(), environment.getProjectId(), null)
                    .stream()
                    .filter(
                        item -> Objects.equals(
                            item.getId(), environment.getResourcePoolId()))
                    .findFirst()
                    .orElseThrow(
                        () -> new ProbeValidationException(
                            "VALIDATION:ResourcePoolNotFound"));
            resourcePoolName = resourcePool.getName();
        }
        return new ProbeSuccess(
            projectName, resourcePoolName, capabilitySnapshot(capability));
    }

    private void persistProbeSuccess(
                                     ManagedFlinkEnvironment environment, ProbeSuccess success) {
        Date probeTime = new Date();
        int updated = environmentMapper.update(
            null,
            new LambdaUpdateWrapper<ManagedFlinkEnvironment>()
                .eq(ManagedFlinkEnvironment::getClusterId, environment.getClusterId())
                .eq(ManagedFlinkEnvironment::getVersion, environment.getVersion())
                .set(ManagedFlinkEnvironment::getProjectName, success.getProjectName())
                .set(
                    ManagedFlinkEnvironment::getResourcePoolName,
                    success.getResourcePoolName())
                .set(
                    ManagedFlinkEnvironment::getCapabilityJson,
                    success.getCapabilityJson())
                .set(ManagedFlinkEnvironment::getLastProbeTime, probeTime)
                .set(ManagedFlinkEnvironment::getLastProbeError, null)
                .set(
                    ManagedFlinkEnvironment::getVersion,
                    environment.getVersion() + 1));
        requireVersionUpdated(updated);
        updateClusterProbeState(
            environment.getClusterId(), ClusterState.RUNNING, null, probeTime);
    }

    private void persistProbeFailure(
                                     ManagedFlinkEnvironment environment,
                                     ClusterState state,
                                     String safeError) {
        Date probeTime = new Date();
        String error = StringUtils.abbreviate(safeError, MAX_PROBE_ERROR_LENGTH);
        int updated = environmentMapper.update(
            null,
            new LambdaUpdateWrapper<ManagedFlinkEnvironment>()
                .eq(ManagedFlinkEnvironment::getClusterId, environment.getClusterId())
                .eq(ManagedFlinkEnvironment::getVersion, environment.getVersion())
                .set(ManagedFlinkEnvironment::getLastProbeTime, probeTime)
                .set(ManagedFlinkEnvironment::getLastProbeError, error)
                .set(
                    ManagedFlinkEnvironment::getVersion,
                    environment.getVersion() + 1));
        requireVersionUpdated(updated);
        updateClusterProbeState(environment.getClusterId(), state, error, probeTime);
    }

    private void updateClusterProbeState(
                                         Long clusterId,
                                         ClusterState state,
                                         String error,
                                         Date probeTime) {
        LambdaUpdateWrapper<FlinkCluster> wrapper =
            new LambdaUpdateWrapper<FlinkCluster>()
                .eq(FlinkCluster::getId, clusterId)
                .eq(FlinkCluster::getDeployMode, FlinkDeployMode.MANAGED_APPLICATION.getMode())
                .set(FlinkCluster::getClusterState, state.getState())
                .set(FlinkCluster::getException, error);
        if (state == ClusterState.RUNNING) {
            wrapper
                .set(FlinkCluster::getStartTime, probeTime)
                .set(FlinkCluster::getEndTime, null);
        } else {
            wrapper.set(FlinkCluster::getEndTime, probeTime);
        }
        ApiAlertException.throwIfFalse(
            clusterMapper.update(null, wrapper) == 1,
            "Managed Flink environment cluster record is invalid.");
    }

    private ManagedFlinkEnvironment requireAuthorizedEnvironment(
                                                                 Long teamId, Long clusterId) {
        authorizationService.requireEnvironmentAuthorized(teamId, clusterId);
        ManagedFlinkEnvironment environment = environmentMapper.selectById(clusterId);
        ApiAlertException.throwIfNull(
            environment, "Managed Flink environment does not exist.");
        return environment;
    }

    private FlinkCluster requireManagedCluster(Long clusterId) {
        FlinkCluster cluster = clusterMapper.selectById(clusterId);
        ApiAlertException.throwIfTrue(
            cluster == null || !FlinkDeployMode.isManagedMode(cluster.getDeployMode()),
            "Managed Flink environment cluster record is invalid.");
        return cluster;
    }

    private CloudAccount requireAccount(Long accountId) {
        CloudAccount account = cloudAccountMapper.selectById(accountId);
        ApiAlertException.throwIfNull(
            account, "Managed Flink cloud account is no longer available.");
        return account;
    }

    private void ensureClusterNameUnique(String clusterName, Long excludedId) {
        Long count = clusterMapper.selectCount(
            new LambdaQueryWrapper<FlinkCluster>()
                .eq(FlinkCluster::getClusterName, clusterName.trim())
                .ne(excludedId != null, FlinkCluster::getId, excludedId));
        ApiAlertException.throwIfTrue(
            count != null && count > 0, "Flink cluster name already exists.");
    }

    private static void applyRegistration(
                                          ManagedFlinkEnvironment environment,
                                          CloudAccount account,
                                          String projectId,
                                          String projectName,
                                          String resourcePoolId,
                                          String resourcePoolName,
                                          Long draftDirectoryId) {
        environment.setProviderType(account.getProviderType());
        environment.setCloudAccountId(account.getId());
        environment.setRegion(account.getRegion());
        environment.setProjectId(projectId.trim());
        environment.setProjectName(StringUtils.trimToNull(projectName));
        environment.setResourcePoolId(resourcePoolId.trim());
        environment.setResourcePoolName(StringUtils.trimToNull(resourcePoolName));
        environment.setDraftDirectoryId(draftDirectoryId);
        environment.setConsoleUrl(null);
    }

    private static ManagedFlinkEnvironmentView toView(
                                                      FlinkCluster cluster,
                                                      ManagedFlinkEnvironment environment) {
        return ManagedFlinkEnvironmentView.builder()
            .clusterId(cluster.getId())
            .clusterName(cluster.getClusterName())
            .description(cluster.getDescription())
            .clusterState(cluster.getClusterState())
            .createTime(cluster.getCreateTime())
            .createUserId(cluster.getUserId())
            .providerType(environment.getProviderType())
            .cloudAccountId(environment.getCloudAccountId())
            .region(environment.getRegion())
            .projectId(environment.getProjectId())
            .projectName(environment.getProjectName())
            .resourcePoolId(environment.getResourcePoolId())
            .resourcePoolName(environment.getResourcePoolName())
            .draftDirectoryId(environment.getDraftDirectoryId())
            .consoleUrl(environment.getConsoleUrl())
            .capabilityJson(environment.getCapabilityJson())
            .lastProbeTime(environment.getLastProbeTime())
            .lastProbeError(environment.getLastProbeError())
            .version(environment.getVersion())
            .build();
    }

    private static String capabilitySnapshot(ManagedFlinkCapability capability) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("providerType", capability.getProviderType().name());
        snapshot.put("apiVersion", capability.getApiVersion());
        snapshot.put("engineVersions", capability.getEngineVersions());
        snapshot.put("jobTypes", capability.getJobTypes());
        snapshot.put("executionModes", capability.getExecutionModes());
        snapshot.put("startModes", capability.getStartModes());
        snapshot.put("schedulingStrategies", capability.getSchedulingStrategies());
        snapshot.put("supportsProjectList", capability.isSupportsProjectList());
        snapshot.put("supportsResourcePoolList", capability.isSupportsResourcePoolList());
        snapshot.put("supportsSqlDeepCheck", capability.isSupportsSqlDeepCheck());
        snapshot.put("supportsSkipPrecheck", capability.isSupportsSkipPrecheck());
        snapshot.put("supportsStopWithSnapshot", capability.isSupportsStopWithSnapshot());
        snapshot.put("supportsCreateSnapshot", capability.isSupportsCreateSnapshot());
        snapshot.put("supportsJarDirectUpload", capability.isSupportsJarDirectUpload());
        snapshot.put("supportsCustomEndpoint", capability.isSupportsCustomEndpoint());
        snapshot.put("minCpu", decimal(capability.getMinCpu()));
        snapshot.put("cpuStep", decimal(capability.getCpuStep()));
        snapshot.put("memoryPerCpuGiB", decimal(capability.getMemoryPerCpuGiB()));
        snapshot.put("maxArtifactBytes", capability.getMaxArtifactBytes());
        snapshot.put("customParameterRules", capability.getCustomParameterRules());
        snapshot.put("capabilityRevision", capability.getCapabilityRevision());
        snapshot.put(
            "expireAt",
            capability.getExpireAt() == null ? null : capability.getExpireAt().toString());
        try {
            return JacksonUtils.write(snapshot);
        } catch (Exception exception) {
            throw new ProbeValidationException("VALIDATION:CapabilitySerializationFailed");
        }
    }

    private static String decimal(BigDecimal value) {
        return value == null ? null : value.toPlainString();
    }

    private static String providerError(ManagedFlinkProviderException exception) {
        String code =
            StringUtils.defaultIfBlank(exception.getProviderCode(), "UnknownProviderError");
        return exception.getCategory().name() + ":" + code;
    }

    private static void requireVersionUpdated(int updated) {
        ApiAlertException.throwIfFalse(
            updated == 1,
            "Managed Flink environment was modified by another request. Refresh and retry.");
    }

    @lombok.Value
    private static class ProbeSuccess {

        String projectName;

        String resourcePoolName;

        String capabilityJson;
    }

    private static class ProbeValidationException extends RuntimeException {

        private final String safeError;

        ProbeValidationException(String safeError) {
            super(safeError);
            this.safeError = safeError;
        }

        String getSafeError() {
            return safeError;
        }
    }
}
