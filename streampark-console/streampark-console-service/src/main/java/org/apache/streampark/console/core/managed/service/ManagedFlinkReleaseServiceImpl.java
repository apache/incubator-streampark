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
import org.apache.streampark.console.core.managed.model.ManagedFlinkApplicationView;
import org.apache.streampark.console.core.managed.model.ManagedFlinkOperationView;
import org.apache.streampark.console.core.managed.model.ManagedFlinkReleaseConfig;
import org.apache.streampark.console.core.managed.model.ManagedFlinkReleaseRequest;
import org.apache.streampark.console.core.managed.model.ManagedFlinkReleaseSnapshot;
import org.apache.streampark.console.core.mapper.ManagedFlinkApplicationMapper;
import org.apache.streampark.console.core.mapper.ManagedFlinkEnvironmentMapper;
import org.apache.streampark.console.core.service.FlinkSqlService;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;

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
        ApiAlertException.throwIfFalse(
            "STREAMING_SQL".equals(application.getJobType()),
            "Managed Flink JAR release is unavailable until artifact transport is configured.");
        ManagedFlinkApplication managed =
            managedApplicationMapper.selectById(appId);
        ManagedFlinkEnvironment environment =
            environmentMapper.selectById(application.getManagedEnvironmentId());
        ApiAlertException.throwIfTrue(
            managed == null || environment == null,
            "Managed Flink release routing is unavailable.");
        ApiAlertException.throwIfNull(
            sqlCandidate, "Managed Flink SQL candidate does not exist.");

        ManagedFlinkReleaseConfig release = application.getReleaseConfig();
        ManagedFlinkReleaseSnapshot snapshot = new ManagedFlinkReleaseSnapshot();
        snapshot.setTeamId(teamId);
        snapshot.setAppId(appId);
        snapshot.setManagedEnvironmentId(application.getManagedEnvironmentId());
        snapshot.setCloudAccountId(environment.getCloudAccountId());
        snapshot.setProviderType(managed.getProviderType());
        snapshot.setProjectId(environment.getProjectId());
        snapshot.setResourcePoolId(
            StringUtils.defaultIfBlank(
                release.getResourcePoolId(), environment.getResourcePoolId()));
        snapshot.setResourcePoolName(environment.getResourcePoolName());
        snapshot.setDraftDirectoryId(environment.getDraftDirectoryId());
        snapshot.setExistingDraftId(managed.getExternalDraftId());
        snapshot.setJobName(application.getJobName());
        snapshot.setJobType(application.getJobType());
        snapshot.setEngineVersion(application.getRuntimeConfig().getEngineVersion());
        snapshot.setSqlText(application.getSql());
        snapshot.setSqlCandidateId(sqlCandidate.getId());
        snapshot.setOptionsJson(write(application.getRuntimeConfig()));
        snapshot.setDynamicOptionsJson(write(release.getCustomProperties()));
        snapshot.setDependencyJson(write(Collections.emptyList()));
        snapshot.setPriority(
            release.getPriority() == null ? null : release.getPriority().toString());
        snapshot.setSchedulePolicy(release.getSchedulingStrategy());
        snapshot.setDefinitionHash(application.getLocalDefinitionHash());
        ApiAlertException.throwIfTrue(
            snapshot.getDraftDirectoryId() == null
                || StringUtils.isAnyBlank(
                    snapshot.getProviderType(),
                    snapshot.getProjectId(),
                    snapshot.getResourcePoolId(),
                    snapshot.getResourcePoolName(),
                    snapshot.getJobName(),
                    snapshot.getEngineVersion(),
                    snapshot.getSqlText(),
                    snapshot.getDefinitionHash()),
            "Managed Flink release snapshot is incomplete.");
        return snapshot;
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
