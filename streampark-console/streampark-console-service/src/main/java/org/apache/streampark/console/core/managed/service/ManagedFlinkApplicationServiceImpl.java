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

import org.apache.streampark.common.enums.ApplicationType;
import org.apache.streampark.common.enums.ClusterState;
import org.apache.streampark.common.enums.FlinkDeployMode;
import org.apache.streampark.common.enums.FlinkJobType;
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.core.entity.FlinkApplication;
import org.apache.streampark.console.core.entity.FlinkCluster;
import org.apache.streampark.console.core.entity.FlinkSql;
import org.apache.streampark.console.core.entity.ManagedFlinkApplication;
import org.apache.streampark.console.core.entity.ManagedFlinkEnvironment;
import org.apache.streampark.console.core.entity.Resource;
import org.apache.streampark.console.core.enums.EngineTypeEnum;
import org.apache.streampark.console.core.enums.FlinkAppStateEnum;
import org.apache.streampark.console.core.enums.OptionStateEnum;
import org.apache.streampark.console.core.enums.ReleaseStateEnum;
import org.apache.streampark.console.core.enums.ResourceFromEnum;
import org.apache.streampark.console.core.enums.ResourceTypeEnum;
import org.apache.streampark.console.core.managed.api.ManagedFlinkProviderType;
import org.apache.streampark.console.core.managed.api.ManagedJobLookupRequest;
import org.apache.streampark.console.core.managed.api.ManagedJobStatus;
import org.apache.streampark.console.core.managed.model.ManagedFlinkApplicationSaveRequest;
import org.apache.streampark.console.core.managed.model.ManagedFlinkApplicationStatisticsView;
import org.apache.streampark.console.core.managed.model.ManagedFlinkApplicationView;
import org.apache.streampark.console.core.managed.model.ManagedFlinkReleaseConfig;
import org.apache.streampark.console.core.managed.model.ManagedFlinkRuntimeConfig;
import org.apache.streampark.console.core.mapper.FlinkApplicationMapper;
import org.apache.streampark.console.core.mapper.FlinkClusterMapper;
import org.apache.streampark.console.core.mapper.ManagedFlinkApplicationMapper;
import org.apache.streampark.console.core.mapper.ManagedFlinkEnvironmentMapper;
import org.apache.streampark.console.core.service.FlinkSqlService;
import org.apache.streampark.console.core.service.ResourceService;

import org.apache.commons.lang3.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** Default candidate configuration service for managed Flink applications. */
@Service
@RequiredArgsConstructor
public class ManagedFlinkApplicationServiceImpl implements ManagedFlinkApplicationService {

    private final ManagedFlinkApplicationMapper managedApplicationMapper;
    private final ManagedFlinkEnvironmentMapper environmentMapper;
    private final FlinkApplicationMapper applicationMapper;
    private final FlinkClusterMapper clusterMapper;
    private final CloudAccountAuthorizationService authorizationService;
    private final ManagedFlinkAuditContext auditContext;
    private final ManagedFlinkApplicationValidator validator;
    private final ManagedFlinkProviderContextService providerContextService;
    private final ManagedFlinkDefinitionHasher definitionHasher;
    private final FlinkSqlService flinkSqlService;
    private final ResourceService resourceService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ManagedFlinkApplicationSaveRequest request) {
        ApiAlertException.throwIfTrue(
            request.getAppId() != null || request.getVersion() != null,
            "Managed Flink application create request cannot contain an app ID or version.");
        normalize(request);
        EnvironmentContext environment =
            requireEnvironment(request.getTeamId(), request.getManagedEnvironmentId());
        ensureNameUnique(request.getJobName(), null);
        validateArtifactResources(request);
        validator.validate(request, environment.getCapability());
        String definitionHash = definitionHasher.hash(request);

        FlinkApplication application = new FlinkApplication();
        applyBaseApplication(application, request);
        Date now = new Date();
        application.setUserId(auditContext.currentUserId());
        application.setState(FlinkAppStateEnum.ADDED.getValue());
        application.setRelease(ReleaseStateEnum.NEED_RELEASE.get());
        application.setOptionState(OptionStateEnum.NONE.getValue());
        application.setTracking(0);
        application.setBuild(false);
        application.setCreateTime(now);
        application.setModifyTime(now);
        ApiAlertException.throwIfFalse(
            applicationMapper.insert(application) == 1,
            "Failed to create the managed Flink application.");

        persistSqlCandidate(application.getId(), request);

        ManagedFlinkApplication managed = new ManagedFlinkApplication();
        managed.setAppId(application.getId());
        managed.setManagedEnvId(request.getManagedEnvironmentId());
        managed.setProviderType(environment.getEnvironment().getProviderType());
        managed.setEngineVersion(request.getRuntimeConfig().getEngineVersion());
        managed.setExecutionMode(request.getRuntimeConfig().getExecutionMode());
        managed.setRuntimeConfigJson(write(request.getRuntimeConfig()));
        managed.setReleaseConfigJson(write(request.getReleaseConfig()));
        managed.setLocalDefinitionHash(definitionHash);
        managed.setConsecutiveSyncFailures(0);
        managed.setVersion(0);
        ApiAlertException.throwIfFalse(
            managedApplicationMapper.insert(managed) == 1,
            "Failed to create the managed Flink application configuration.");
        return application.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void copyLocalConfiguration(Long sourceAppId, Long targetAppId) {
        FlinkApplication source = applicationMapper.selectById(sourceAppId);
        FlinkApplication target = applicationMapper.selectById(targetAppId);
        ManagedFlinkApplication sourceManaged = managedApplicationMapper.selectById(sourceAppId);
        ApiAlertException.throwIfTrue(
            source == null
                || target == null
                || sourceManaged == null
                || !Objects.equals(source.getTeamId(), target.getTeamId())
                || !FlinkDeployMode.isManagedMode(source.getDeployModeEnum())
                || !FlinkDeployMode.isManagedMode(target.getDeployModeEnum()),
            "Managed Flink application copy source is invalid.");

        ManagedFlinkRuntimeConfig runtime =
            read(sourceManaged.getRuntimeConfigJson(), ManagedFlinkRuntimeConfig.class);
        ManagedFlinkReleaseConfig release =
            read(sourceManaged.getReleaseConfigJson(), ManagedFlinkReleaseConfig.class);
        ManagedFlinkApplication copied = new ManagedFlinkApplication();
        copied.setAppId(targetAppId);
        copied.setManagedEnvId(sourceManaged.getManagedEnvId());
        copied.setProviderType(sourceManaged.getProviderType());
        copied.setEngineVersion(sourceManaged.getEngineVersion());
        copied.setExecutionMode(sourceManaged.getExecutionMode());
        copied.setRuntimeConfigJson(sourceManaged.getRuntimeConfigJson());
        copied.setReleaseConfigJson(sourceManaged.getReleaseConfigJson());
        copied.setConsecutiveSyncFailures(0);
        copied.setVersion(0);
        copied.setLocalDefinitionHash(
            definitionHasher.hash(viewRequest(target, copied, runtime, release)));
        ApiAlertException.throwIfFalse(
            managedApplicationMapper.insert(copied) == 1,
            "Failed to copy the managed Flink application configuration.");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(ManagedFlinkApplicationSaveRequest request) {
        ApiAlertException.throwIfTrue(
            request.getAppId() == null || request.getVersion() == null,
            "Managed Flink application ID and version are required.");
        normalize(request);
        ApplicationContext existing = requireApplication(request.getTeamId(), request.getAppId());
        ApiAlertException.throwIfFalse(
            existing.getApplication().getJobTypeEnum() == jobType(request.getJobType()),
            "Managed Flink application job type cannot be changed.");
        EnvironmentContext environment =
            requireEnvironment(request.getTeamId(), request.getManagedEnvironmentId());
        ensureNameUnique(request.getJobName(), request.getAppId());
        validateArtifactResources(request);
        validator.validate(request, environment.getCapability());
        String definitionHash = definitionHasher.hash(request);

        int managedUpdated = managedApplicationMapper.update(
            null,
            new LambdaUpdateWrapper<ManagedFlinkApplication>()
                .eq(ManagedFlinkApplication::getAppId, request.getAppId())
                .eq(ManagedFlinkApplication::getVersion, request.getVersion())
                .set(
                    ManagedFlinkApplication::getManagedEnvId,
                    request.getManagedEnvironmentId())
                .set(
                    ManagedFlinkApplication::getProviderType,
                    environment.getEnvironment().getProviderType())
                .set(
                    ManagedFlinkApplication::getEngineVersion,
                    request.getRuntimeConfig().getEngineVersion())
                .set(
                    ManagedFlinkApplication::getExecutionMode,
                    request.getRuntimeConfig().getExecutionMode())
                .set(
                    ManagedFlinkApplication::getRuntimeConfigJson,
                    write(request.getRuntimeConfig()))
                .set(
                    ManagedFlinkApplication::getReleaseConfigJson,
                    write(request.getReleaseConfig()))
                .set(ManagedFlinkApplication::getLocalDefinitionHash, definitionHash)
                .set(ManagedFlinkApplication::getVersion, request.getVersion() + 1));
        ApiAlertException.throwIfFalse(
            managedUpdated == 1,
            "Managed Flink application was modified by another request. Refresh and retry.");

        FlinkApplication updated = new FlinkApplication();
        updated.setId(request.getAppId());
        applyBaseApplication(updated, request);
        updated.setRelease(ReleaseStateEnum.NEED_RELEASE.get());
        updated.setModifyTime(new Date());
        ApiAlertException.throwIfFalse(
            applicationMapper.update(
                updated,
                new LambdaUpdateWrapper<FlinkApplication>()
                    .eq(FlinkApplication::getId, request.getAppId())
                    .eq(FlinkApplication::getTeamId, request.getTeamId())
                    .eq(
                        FlinkApplication::getDeployMode,
                        FlinkDeployMode.MANAGED_APPLICATION.getMode())) == 1,
            "Managed Flink application record is invalid.");
        persistSqlCandidate(request.getAppId(), request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteLocal(Long appId) {
        ApiAlertException.throwIfNull(appId, "Managed Flink application ID is required.");
        FlinkApplication application = applicationMapper.selectById(appId);
        ManagedFlinkApplication managed = managedApplicationMapper.selectById(appId);
        ApiAlertException.throwIfTrue(
            application == null
                || managed == null
                || !FlinkDeployMode.isManagedMode(application.getDeployModeEnum()),
            "Managed Flink application does not exist.");
        ApiAlertException.throwIfFalse(
            Arrays.asList(
                FlinkAppStateEnum.ADDED.getValue(),
                FlinkAppStateEnum.FAILED.getValue(),
                FlinkAppStateEnum.CANCELED.getValue(),
                FlinkAppStateEnum.FINISHED.getValue(),
                FlinkAppStateEnum.LOST.getValue(),
                FlinkAppStateEnum.TERMINATED.getValue(),
                FlinkAppStateEnum.POS_TERMINATED.getValue(),
                FlinkAppStateEnum.SUCCEEDED.getValue(),
                FlinkAppStateEnum.KILLED.getValue())
                .contains(application.getState()),
            "Managed Flink application must be stopped before deleting its local record.");

        flinkSqlService.removeByAppId(appId);
        ApiAlertException.throwIfFalse(
            applicationMapper.deleteById(appId) == 1,
            "Failed to delete the managed Flink application local record.");
    }

    @Override
    public ManagedFlinkApplicationView get(Long teamId, Long appId) {
        ApplicationContext context = requireApplication(teamId, appId);
        FlinkApplication application = context.getApplication();
        ManagedFlinkApplication managed = context.getManaged();
        ManagedFlinkRuntimeConfig runtime =
            read(managed.getRuntimeConfigJson(), ManagedFlinkRuntimeConfig.class);
        ManagedFlinkReleaseConfig release =
            read(managed.getReleaseConfigJson(), ManagedFlinkReleaseConfig.class);
        ManagedFlinkApplicationSaveRequest current =
            viewRequest(application, managed, runtime, release);
        BigDecimal estimatedCu =
            validator.validate(current,
                requireEnvironment(teamId, managed.getManagedEnvId()).getCapability());
        return ManagedFlinkApplicationView.builder()
            .appId(application.getId())
            .teamId(application.getTeamId())
            .jobName(application.getJobName())
            .description(application.getDescription())
            .managedEnvironmentId(managed.getManagedEnvId())
            .providerType(managed.getProviderType())
            .jobType(jobTypeName(application.getJobTypeEnum()))
            .sql(current.getSql())
            .jar(application.getJar())
            .mainClass(application.isFlinkJar() ? application.getMainClass() : null)
            .args(application.getArgs())
            .runtimeConfig(runtime)
            .releaseConfig(release)
            .estimatedCu(estimatedCu)
            .localDefinitionHash(managed.getLocalDefinitionHash())
            .deployedDefinitionHash(managed.getDeployedDefinitionHash())
            .providerDefinitionHash(managed.getProviderDefinitionHash())
            .externalApplicationId(managed.getExternalApplicationId())
            .externalInstanceId(managed.getExternalInstanceId())
            .state(application.getState())
            .optionState(application.getOptionState())
            .tracking(application.getTracking())
            .providerRawState(managed.getProviderRawState())
            .syncState(managed.getSyncState())
            .lastSyncTime(managed.getLastSyncTime())
            .consecutiveSyncFailures(managed.getConsecutiveSyncFailures())
            .nextSyncTime(managed.getNextSyncTime())
            .consoleUrl(
                StringUtils.defaultIfBlank(
                    managed.getConsoleUrl(),
                    ManagedFlinkProviderType.consoleUrl(managed.getProviderType())))
            .version(managed.getVersion())
            .build();
    }

    @Override
    public String getFlinkUiUrl(Long teamId, Long appId) {
        ApplicationContext applicationContext = requireApplication(teamId, appId);
        FlinkApplication application = applicationContext.getApplication();
        ManagedFlinkApplication managed = applicationContext.getManaged();
        ApiAlertException.throwIfFalse(
            FlinkAppStateEnum.getState(application.getState()) == FlinkAppStateEnum.RUNNING,
            "Managed Flink job is not running.");
        ApiAlertException.throwIfTrue(
            StringUtils.isBlank(managed.getExternalApplicationId())
                || StringUtils.isBlank(managed.getExternalInstanceId()),
            "Managed Flink running job identity is unavailable.");
        ManagedFlinkEnvironment environment = environmentMapper.selectById(managed.getManagedEnvId());
        ApiAlertException.throwIfTrue(
            environment == null
                || environment.getCloudAccountId() == null
                || StringUtils.isBlank(environment.getProviderConfigJson())
                || environment.getProviderConfigVersion() == null,
            "Managed Flink environment is unavailable.");
        ManagedFlinkProviderSession session =
            providerContextService.resolve(
                teamId,
                environment.getCloudAccountId(),
                environment.getProviderConfigJson(),
                environment.getProviderConfigVersion());
        ManagedJobStatus status =
            session
                .getProvider()
                .getJob(
                    session.getContext(),
                    ManagedJobLookupRequest.builder()
                        .jobName(application.getJobName())
                        .jobId(managed.getExternalApplicationId())
                        .instanceId(managed.getExternalInstanceId())
                        .build());
        ApiAlertException.throwIfTrue(
            status == null || StringUtils.isBlank(status.getFlinkUiUrl()),
            "Managed Flink Web UI is temporarily unavailable.");
        return status.getFlinkUiUrl();
    }

    @Override
    public ManagedFlinkApplicationStatisticsView statistics(Long teamId) {
        ApiAlertException.throwIfNull(teamId, "Managed Flink Team ID is required.");
        return managedApplicationMapper.selectStatistics(
            teamId, FlinkDeployMode.MANAGED_APPLICATION.getMode());
    }

    private EnvironmentContext requireEnvironment(Long teamId, Long environmentId) {
        authorizationService.requireEnvironmentAuthorized(teamId, environmentId);
        ManagedFlinkEnvironment environment = environmentMapper.selectById(environmentId);
        FlinkCluster cluster = clusterMapper.selectById(environmentId);
        ApiAlertException.throwIfTrue(
            environment == null
                || cluster == null
                || !FlinkDeployMode.isManagedMode(cluster.getDeployMode()),
            "Managed Flink environment does not exist.");
        ApiAlertException.throwIfFalse(
            ClusterState.RUNNING.getState().equals(cluster.getClusterState())
                && StringUtils.isNotBlank(environment.getCapabilityJson()),
            "Managed Flink environment must be probed successfully before saving applications.");
        return new EnvironmentContext(environment, capability(environment.getCapabilityJson()));
    }

    private ApplicationContext requireApplication(Long teamId, Long appId) {
        FlinkApplication application = applicationMapper.selectById(appId);
        ManagedFlinkApplication managed = managedApplicationMapper.selectById(appId);
        ApiAlertException.throwIfTrue(
            application == null
                || managed == null
                || !teamId.equals(application.getTeamId())
                || !FlinkDeployMode.isManagedMode(application.getDeployMode()),
            "Managed Flink application does not exist.");
        authorizationService.requireEnvironmentAuthorized(teamId, managed.getManagedEnvId());
        return new ApplicationContext(application, managed);
    }

    private void ensureNameUnique(String jobName, Long excludedAppId) {
        Long count = applicationMapper.selectCount(
            new LambdaQueryWrapper<FlinkApplication>()
                .eq(FlinkApplication::getJobName, jobName)
                .ne(excludedAppId != null, FlinkApplication::getId, excludedAppId));
        ApiAlertException.throwIfTrue(
            count != null && count > 0, "Flink application name already exists.");
    }

    private void validateArtifactResources(ManagedFlinkApplicationSaveRequest request) {
        if ("STREAMING_JAR".equals(request.getJobType())) {
            Resource applicationJar =
                resourceService.findByResourceName(request.getTeamId(), request.getJar());
            ApiAlertException.throwIfNull(
                applicationJar,
                "Managed Flink JAR resource does not exist in this Team.");
            ApiAlertException.throwIfTrue(
                applicationJar.getResourceType() != ResourceTypeEnum.APP
                    || applicationJar.getEngineType() != EngineTypeEnum.FLINK,
                "Managed Flink application JAR must be a Flink APP resource.");
        }
        for (String dependency : request.getReleaseConfig().getDependencyResourceNames()) {
            ApiAlertException.throwIfNull(
                resourceService.findByResourceName(request.getTeamId(), dependency),
                "Managed Flink dependency resource does not exist in this Team.");
        }
    }

    private void persistSqlCandidate(
                                     Long appId, ManagedFlinkApplicationSaveRequest request) {
        if (!"STREAMING_SQL".equals(request.getJobType())) {
            return;
        }
        String normalized = ManagedFlinkDefinitionHasher.normalizeSql(request.getSql());
        FlinkSql latest = flinkSqlService.getLatestFlinkSql(appId, true);
        if (latest != null && normalized.equals(
            ManagedFlinkDefinitionHasher.normalizeSql(latest.getSql()))) {
            return;
        }
        FlinkSql candidate = new FlinkSql();
        candidate.setAppId(appId);
        candidate.setSql(normalized);
        candidate.setCreateTime(new Date());
        flinkSqlService.create(candidate);
    }

    private void applyBaseApplication(
                                      FlinkApplication application,
                                      ManagedFlinkApplicationSaveRequest request) {
        FlinkJobType jobType = jobType(request.getJobType());
        application.setTeamId(request.getTeamId());
        application.setJobName(request.getJobName());
        application.setDescription(StringUtils.trimToNull(request.getDescription()));
        application.setDeployMode(FlinkDeployMode.MANAGED_APPLICATION.getMode());
        application.setFlinkClusterId(request.getManagedEnvironmentId());
        application.setVersionId(null);
        application.setJobType(jobType.getMode());
        application.setAppType(ApplicationType.APACHE_FLINK.getType());
        application.setArgs(StringUtils.trimToNull(request.getArgs()));
        application.setJar(
            jobType == FlinkJobType.FLINK_JAR ? request.getJar() : null);
        application.setMainClass(
            jobType == FlinkJobType.FLINK_JAR ? request.getMainClass() : null);
        application.setResourceFrom(
            jobType == FlinkJobType.FLINK_JAR ? ResourceFromEnum.UPLOAD.getValue() : null);
    }

    private ManagedFlinkApplicationSaveRequest viewRequest(
                                                           FlinkApplication application,
                                                           ManagedFlinkApplication managed,
                                                           ManagedFlinkRuntimeConfig runtime,
                                                           ManagedFlinkReleaseConfig release) {
        ManagedFlinkApplicationSaveRequest request =
            new ManagedFlinkApplicationSaveRequest();
        request.setAppId(application.getId());
        request.setVersion(managed.getVersion());
        request.setTeamId(application.getTeamId());
        request.setJobName(application.getJobName());
        request.setManagedEnvironmentId(managed.getManagedEnvId());
        request.setJobType(jobTypeName(application.getJobTypeEnum()));
        request.setJar(application.getJar());
        request.setMainClass(
            application.isFlinkJar() ? application.getMainClass() : null);
        request.setArgs(application.getArgs());
        request.setRuntimeConfig(runtime);
        request.setReleaseConfig(release);
        if (application.isFlinkSql()) {
            FlinkSql latest = flinkSqlService.getLatestFlinkSql(application.getId(), true);
            request.setSql(latest == null ? null : latest.getSql());
        }
        return request;
    }

    private Map<String, Object> capability(String value) {
        try {
            return objectMapper.readValue(
                value, new TypeReference<Map<String, Object>>() {
                });
        } catch (Exception exception) {
            throw new ApiAlertException("Managed Flink environment capability is invalid.");
        }
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new ApiAlertException(
                "Managed Flink application configuration cannot be serialized.");
        }
    }

    private <T> T read(String value, Class<T> type) {
        try {
            return objectMapper.readValue(value, type);
        } catch (Exception exception) {
            throw new ApiAlertException(
                "Managed Flink application configuration is invalid.");
        }
    }

    private static void normalize(ManagedFlinkApplicationSaveRequest request) {
        request.setJobName(request.getJobName().trim());
        request.setJobType(request.getJobType().trim().toUpperCase(java.util.Locale.ROOT));
        request.setSql(ManagedFlinkDefinitionHasher.normalizeSql(request.getSql()));
        request.setJar(StringUtils.trimToNull(request.getJar()));
        request.setMainClass(StringUtils.trimToNull(request.getMainClass()));
        request.setArgs(StringUtils.trimToNull(request.getArgs()));
        ManagedFlinkRuntimeConfig runtime = request.getRuntimeConfig();
        if (runtime != null) {
            runtime.setEngineVersion(StringUtils.trimToEmpty(runtime.getEngineVersion()));
            runtime.setExecutionMode(
                StringUtils.defaultIfBlank(runtime.getExecutionMode(), "STREAMING")
                    .trim()
                    .toUpperCase(java.util.Locale.ROOT));
            if (runtime.getCustomProperties() == null) {
                runtime.setCustomProperties(Collections.emptyMap());
            }
        }
        ManagedFlinkReleaseConfig release = request.getReleaseConfig();
        if (release != null) {
            release.setSchedulingStrategy(
                StringUtils.defaultIfBlank(release.getSchedulingStrategy(), "DEFAULT")
                    .trim()
                    .toUpperCase(java.util.Locale.ROOT));
            if (release.getDependencyResourceNames() == null) {
                release.setDependencyResourceNames(Collections.emptyList());
            } else {
                release.setDependencyResourceNames(
                    release.getDependencyResourceNames().stream()
                        .map(StringUtils::trimToNull)
                        .filter(Objects::nonNull)
                        .distinct()
                        .collect(Collectors.toList()));
            }
            if (release.getCustomProperties() == null) {
                release.setCustomProperties(Collections.emptyMap());
            }
        }
    }

    private static FlinkJobType jobType(String name) {
        if ("STREAMING_SQL".equals(name)) {
            return FlinkJobType.FLINK_SQL;
        }
        if ("STREAMING_JAR".equals(name)) {
            return FlinkJobType.FLINK_JAR;
        }
        return FlinkJobType.UNKNOWN;
    }

    private static String jobTypeName(FlinkJobType type) {
        return type == FlinkJobType.FLINK_SQL ? "STREAMING_SQL" : "STREAMING_JAR";
    }

    @lombok.Value
    private static class EnvironmentContext {

        ManagedFlinkEnvironment environment;

        Map<String, Object> capability;
    }

    @lombok.Value
    private static class ApplicationContext {

        FlinkApplication application;

        ManagedFlinkApplication managed;
    }
}
