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
import org.apache.streampark.console.core.entity.FlinkApplication;
import org.apache.streampark.console.core.entity.ManagedFlinkApplication;
import org.apache.streampark.console.core.entity.ManagedFlinkOperation;
import org.apache.streampark.console.core.managed.model.ManagedFlinkReleaseSnapshot;
import org.apache.streampark.console.core.mapper.FlinkApplicationMapper;
import org.apache.streampark.console.core.mapper.ManagedFlinkApplicationMapper;
import org.apache.streampark.console.core.mapper.ManagedFlinkOperationMapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Resolves the immutable release snapshot corresponding to the deployed definition hash. */
@Service
@RequiredArgsConstructor
public class ManagedFlinkDeployedDefinitionService {

    private final FlinkApplicationMapper applicationMapper;
    private final ManagedFlinkApplicationMapper managedApplicationMapper;
    private final ManagedFlinkOperationMapper operationMapper;
    private final CloudAccountAuthorizationService authorizationService;
    private final ObjectMapper objectMapper;

    public ManagedFlinkReleaseSnapshot getRequired(Long teamId, Long appId) {
        FlinkApplication application = applicationMapper.selectById(appId);
        ManagedFlinkApplication managed = managedApplicationMapper.selectById(appId);
        ApiAlertException.throwIfTrue(
            application == null
                || managed == null
                || teamId == null
                || !teamId.equals(application.getTeamId())
                || managed.getDeployedDefinitionHash() == null,
            "Managed Flink deployed definition does not exist.");
        authorizationService.requireEnvironmentAuthorized(
            teamId, managed.getManagedEnvId());

        ManagedFlinkOperation operation =
            operationMapper.selectOne(
                new LambdaQueryWrapper<ManagedFlinkOperation>()
                    .eq(ManagedFlinkOperation::getAppId, appId)
                    .eq(ManagedFlinkOperation::getOperationType, "RELEASE")
                    .eq(ManagedFlinkOperation::getState, "SUCCEEDED")
                    .eq(
                        ManagedFlinkOperation::getRequestHash,
                        managed.getDeployedDefinitionHash())
                    .orderByDesc(ManagedFlinkOperation::getFinishTime)
                    .last("LIMIT 1"));
        ApiAlertException.throwIfNull(
            operation, "Managed Flink deployed release snapshot does not exist.");
        ManagedFlinkReleaseSnapshot snapshot = read(operation.getRequestJson());
        ApiAlertException.throwIfFalse(
            appId.equals(snapshot.getAppId())
                && teamId.equals(snapshot.getTeamId())
                && managed.getDeployedDefinitionHash().equals(snapshot.getDefinitionHash()),
            "Managed Flink deployed release snapshot is inconsistent.");
        return snapshot;
    }

    private ManagedFlinkReleaseSnapshot read(String value) {
        try {
            return objectMapper.readValue(value, ManagedFlinkReleaseSnapshot.class);
        } catch (Exception exception) {
            throw new ApiAlertException(
                "Managed Flink deployed release snapshot is invalid.");
        }
    }
}
