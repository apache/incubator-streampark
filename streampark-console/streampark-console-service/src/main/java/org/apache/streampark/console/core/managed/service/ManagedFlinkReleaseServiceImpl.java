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

import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.core.entity.FlinkSql;
import org.apache.streampark.console.core.entity.ManagedFlinkApplication;
import org.apache.streampark.console.core.entity.ManagedFlinkEnvironment;
import org.apache.streampark.console.core.entity.Resource;
import org.apache.streampark.console.core.managed.model.ManagedFlinkApplicationView;
import org.apache.streampark.console.core.managed.model.ManagedFlinkArtifactView;
import org.apache.streampark.console.core.managed.model.ManagedFlinkOperationView;
import org.apache.streampark.console.core.managed.model.ManagedFlinkReleaseConfig;
import org.apache.streampark.console.core.managed.model.ManagedFlinkReleaseRequest;
import org.apache.streampark.console.core.managed.model.ManagedFlinkReleaseSnapshot;
import org.apache.streampark.console.core.mapper.ManagedFlinkApplicationMapper;
import org.apache.streampark.console.core.mapper.ManagedFlinkEnvironmentMapper;
import org.apache.streampark.console.core.service.FlinkSqlService;
import org.apache.streampark.console.core.service.ResourceService;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Builds a secret-free release snapshot and admits it before asynchronous execution. */
@Service
@RequiredArgsConstructor
public class ManagedFlinkReleaseServiceImpl implements ManagedFlinkReleaseService {

    private final ManagedFlinkApplicationService applicationService;
    private final ManagedFlinkApplicationMapper managedApplicationMapper;
    private final ManagedFlinkEnvironmentMapper environmentMapper;
    private final ManagedFlinkOperationService operationService;
    private final ManagedFlinkReleaseStateService releaseStateService;
    private final ManagedFlinkReleaseDispatcher dispatcher;
    private final FlinkSqlService flinkSqlService;
    private final ResourceService resourceService;
    private final ManagedFlinkArtifactService artifactService;
    private final ObjectMapper objectMapper;

    @Override
    public ManagedFlinkOperationView release(ManagedFlinkReleaseRequest request) {
        ManagedFlinkReleaseSnapshot snapshot =
            snapshot(request.getTeamId(), request.getAppId());
        String requestJson = write(snapshot);
        ManagedFlinkOperationView operation =
            operationService.accept(
                snapshot.getAppId(),
                "RELEASE",
                request.getIdempotencyKey(),
                snapshot.getDefinitionHash(),
                requestJson);
        if (operation.isIdempotentReplay()) {
            return operation;
        }

        releaseStateService.markReleasing(
            snapshot.getAppId(), snapshot.getDefinitionHash());
        if (!dispatcher.dispatch(operation.getOperationId())) {
            if (operationService.markRunning(operation.getOperationId())) {
                operationService.markFailed(
                    operation.getOperationId(),
                    false,
                    null,
                    "INTERNAL:ExecutorRejected",
                    "Managed Flink release executor is temporarily unavailable.");
                releaseStateService.markKnownFailure(
                    snapshot.getAppId(), snapshot.getDefinitionHash());
            }
            return operationService.getView(
                snapshot.getAppId(), operation.getOperationId());
        }
        return operation;
    }

    private ManagedFlinkReleaseSnapshot snapshot(Long teamId, Long appId) {
        // Read the raw candidate before applicationService.get decodes the same MyBatis session
        // object. A second decoded read in one request can otherwise attempt to unzip plain SQL.
        FlinkSql sqlCandidate = flinkSqlService.getLatestFlinkSql(appId, false);
        ManagedFlinkApplicationView application =
            applicationService.get(teamId, appId);
        boolean sqlJob = "STREAMING_SQL".equals(application.getJobType());
        boolean jarJob = "STREAMING_JAR".equals(application.getJobType());
        ApiAlertException.throwIfFalse(
            sqlJob || jarJob, "Managed Flink release job type is unsupported.");
        ManagedFlinkApplication managed =
            managedApplicationMapper.selectById(appId);
        ManagedFlinkEnvironment environment =
            environmentMapper.selectById(application.getManagedEnvironmentId());
        ApiAlertException.throwIfTrue(
            managed == null || environment == null,
            "Managed Flink release routing is unavailable.");
        if (sqlJob) {
            ApiAlertException.throwIfNull(
                sqlCandidate, "Managed Flink SQL candidate does not exist.");
        }

        ManagedFlinkReleaseConfig release = application.getReleaseConfig();
        List<ManagedFlinkArtifactView> artifacts =
            artifactService.stageApplicationArtifacts(teamId, appId);
        ManagedFlinkReleaseSnapshot snapshot = new ManagedFlinkReleaseSnapshot();
        snapshot.setTeamId(teamId);
        snapshot.setAppId(appId);
        snapshot.setManagedEnvironmentId(application.getManagedEnvironmentId());
        snapshot.setCloudAccountId(environment.getCloudAccountId());
        snapshot.setProviderType(managed.getProviderType());
        snapshot.setProviderConfigJson(environment.getProviderConfigJson());
        snapshot.setProviderConfigVersion(environment.getProviderConfigVersion());
        snapshot.setExistingDraftId(managed.getExternalDraftId());
        snapshot.setJobName(application.getJobName());
        snapshot.setJobType(application.getJobType());
        snapshot.setEngineVersion(application.getRuntimeConfig().getEngineVersion());
        if (sqlJob) {
            snapshot.setSqlText(application.getSql());
            snapshot.setSqlCandidateId(sqlCandidate.getId());
        } else {
            Resource applicationJar =
                resourceService.findByResourceName(teamId, application.getJar());
            ApiAlertException.throwIfNull(
                applicationJar, "Managed Flink JAR resource does not exist in this Team.");
            ManagedFlinkArtifactView mainArtifact =
                artifacts.stream()
                    .filter(
                        artifact -> applicationJar.getId().equals(artifact.getSourceResourceId()))
                    .findFirst()
                    .orElseThrow(
                        () -> new ApiAlertException(
                            "Managed Flink main JAR was not staged."));
            snapshot.setJar(mainArtifact.getProviderArtifactId());
            snapshot.setMainClass(application.getMainClass());
            snapshot.setArgs(application.getArgs());
            artifacts = new ArrayList<>(artifacts);
            artifacts.remove(mainArtifact);
        }
        snapshot.setOptionsJson(write(application.getRuntimeConfig()));
        snapshot.setDynamicOptionsJson(write(release.getCustomProperties()));
        snapshot.setDependencyJson(dependencyJson(artifacts));
        snapshot.setPriority(
            release.getPriority() == null ? null : release.getPriority().toString());
        snapshot.setSchedulePolicy(release.getSchedulingStrategy());
        snapshot.setDefinitionHash(application.getLocalDefinitionHash());
        ApiAlertException.throwIfTrue(
            snapshot.getProviderConfigVersion() == null
                || StringUtils.isAnyBlank(
                    snapshot.getProviderType(),
                    snapshot.getProviderConfigJson(),
                    snapshot.getJobName(),
                    snapshot.getEngineVersion(),
                    snapshot.getDefinitionHash()),
            "Managed Flink release snapshot is incomplete.");
        ApiAlertException.throwIfTrue(
            sqlJob
                ? StringUtils.isBlank(snapshot.getSqlText())
                : StringUtils.isAnyBlank(snapshot.getJar(), snapshot.getMainClass()),
            "Managed Flink release definition is incomplete.");
        return snapshot;
    }

    private String dependencyJson(List<ManagedFlinkArtifactView> artifacts) {
        if (artifacts == null || artifacts.isEmpty()) {
            Map<String, Object> dependency = new LinkedHashMap<>();
            dependency.put("jars", Collections.emptyList());
            return write(dependency);
        }
        List<String> jars = new ArrayList<>();
        Map<String, String> versions = new LinkedHashMap<>();
        for (ManagedFlinkArtifactView artifact : artifacts) {
            jars.add(artifact.getProviderArtifactId());
            versions.put(
                artifact.getProviderArtifactId(),
                String.valueOf(artifact.getProviderArtifactVersion()));
        }
        Map<String, Object> dependency = new LinkedHashMap<>();
        dependency.put("jars", jars);
        dependency.put("dependencyVersions", versions);
        return write(dependency);
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new ApiAlertException(
                "Managed Flink release snapshot cannot be serialized.");
        }
    }
}
