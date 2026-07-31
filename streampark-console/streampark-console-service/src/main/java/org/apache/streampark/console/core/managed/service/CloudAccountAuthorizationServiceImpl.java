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
import org.apache.streampark.console.core.entity.CloudAccount;
import org.apache.streampark.console.core.entity.CloudAccountTeam;
import org.apache.streampark.console.core.entity.ManagedFlinkEnvironment;
import org.apache.streampark.console.core.managed.api.ManagedFlinkProviderType;
import org.apache.streampark.console.core.mapper.CloudAccountMapper;
import org.apache.streampark.console.core.mapper.CloudAccountTeamMapper;
import org.apache.streampark.console.core.mapper.ManagedFlinkEnvironmentMapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Default fail-closed Team authorization checks. */
@Service
@RequiredArgsConstructor
public class CloudAccountAuthorizationServiceImpl
    implements
        CloudAccountAuthorizationService {

    private static final int STATUS_ENABLED = 1;
    private static final String PERMISSION_USE = "USE";
    private static final String AUTHORIZATION_ERROR =
        "Team is not authorized to use this managed Flink cloud account.";

    private final CloudAccountMapper cloudAccountMapper;
    private final CloudAccountTeamMapper cloudAccountTeamMapper;
    private final ManagedFlinkEnvironmentMapper managedFlinkEnvironmentMapper;
    private final ManagedFlinkFeatureGate featureGate;

    @Override
    public void requireAuthorized(Long teamId, Long accountId) {
        ApiAlertException.throwIfNull(teamId, "Team id is required.");
        ApiAlertException.throwIfNull(accountId, "Cloud account id is required.");
        Long grants =
            cloudAccountTeamMapper.selectCount(
                new LambdaQueryWrapper<CloudAccountTeam>()
                    .eq(CloudAccountTeam::getCloudAccountId, accountId)
                    .eq(CloudAccountTeam::getTeamId, teamId)
                    .eq(CloudAccountTeam::getPermissionLevel, PERMISSION_USE));
        ApiAlertException.throwIfTrue(
            grants == null || grants != 1,
            AUTHORIZATION_ERROR);

        CloudAccount account = cloudAccountMapper.selectById(accountId);
        ApiAlertException.throwIfTrue(
            account == null || account.getStatus() == null || account.getStatus() != STATUS_ENABLED,
            AUTHORIZATION_ERROR);
        featureGate.requireWriteEnabled(
            ManagedFlinkProviderType.valueOf(account.getProviderType()));
    }

    @Override
    public void requireEnvironmentAuthorized(Long teamId, Long clusterId) {
        ApiAlertException.throwIfNull(clusterId, "Managed Flink environment id is required.");
        ManagedFlinkEnvironment environment =
            managedFlinkEnvironmentMapper.selectById(clusterId);
        ApiAlertException.throwIfNull(
            environment, "Managed Flink environment does not exist.");
        requireAuthorized(teamId, environment.getCloudAccountId());
    }
}
