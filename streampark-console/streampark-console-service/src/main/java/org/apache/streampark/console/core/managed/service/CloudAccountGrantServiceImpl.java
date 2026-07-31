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
import org.apache.streampark.console.core.managed.api.ManagedFlinkProviderType;
import org.apache.streampark.console.core.managed.model.CloudAccountGrantRequest;
import org.apache.streampark.console.core.managed.model.CloudAccountTeamGrantView;
import org.apache.streampark.console.core.managed.model.CloudAccountView;
import org.apache.streampark.console.core.mapper.CloudAccountMapper;
import org.apache.streampark.console.core.mapper.CloudAccountTeamMapper;
import org.apache.streampark.console.system.entity.Team;
import org.apache.streampark.console.system.service.TeamService;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Default explicit Team grant service. */
@Service
@RequiredArgsConstructor
public class CloudAccountGrantServiceImpl implements CloudAccountGrantService {

    private static final int STATUS_ENABLED = 1;
    private static final String PERMISSION_USE = "USE";

    private final CloudAccountMapper cloudAccountMapper;
    private final CloudAccountTeamMapper cloudAccountTeamMapper;
    private final TeamService teamService;
    private final ManagedFlinkFeatureGate featureGate;
    private final ManagedFlinkAuditContext auditContext;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceGrants(CloudAccountGrantRequest request) {
        CloudAccount account = requireAccount(request.getAccountId());
        ManagedFlinkProviderType providerType =
            ManagedFlinkProviderType.valueOf(account.getProviderType());
        featureGate.requireWriteEnabled(providerType);

        Set<Long> requestedTeamIds = new LinkedHashSet<>(request.getTeamIds());
        validateTeams(requestedTeamIds);

        int versionUpdated =
            cloudAccountMapper.update(
                null,
                new LambdaUpdateWrapper<CloudAccount>()
                    .eq(CloudAccount::getId, request.getAccountId())
                    .eq(CloudAccount::getVersion, request.getAccountVersion())
                    .set(CloudAccount::getVersion, request.getAccountVersion() + 1));
        ApiAlertException.throwIfFalse(
            versionUpdated == 1,
            "Cloud account was modified by another request. Refresh and retry.");

        cloudAccountTeamMapper.delete(
            new LambdaQueryWrapper<CloudAccountTeam>()
                .eq(CloudAccountTeam::getCloudAccountId, request.getAccountId()));

        Date createTime = new Date();
        Long createUserId = auditContext.currentUserId();
        for (Long teamId : requestedTeamIds) {
            CloudAccountTeam grant = new CloudAccountTeam();
            grant.setCloudAccountId(request.getAccountId());
            grant.setTeamId(teamId);
            grant.setPermissionLevel(PERMISSION_USE);
            grant.setCreateUserId(createUserId);
            grant.setCreateTime(createTime);
            ApiAlertException.throwIfFalse(
                cloudAccountTeamMapper.insert(grant) == 1,
                "Failed to create the cloud account Team authorization.");
        }
    }

    @Override
    public List<CloudAccountTeamGrantView> listGrants(Long accountId) {
        requireAccount(accountId);
        List<CloudAccountTeam> grants =
            cloudAccountTeamMapper.selectList(
                new LambdaQueryWrapper<CloudAccountTeam>()
                    .eq(CloudAccountTeam::getCloudAccountId, accountId)
                    .orderByAsc(CloudAccountTeam::getTeamId));
        if (grants.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, Team> teams =
            teamService.listByIds(
                grants.stream()
                    .map(CloudAccountTeam::getTeamId)
                    .collect(Collectors.toList()))
                .stream()
                .collect(Collectors.toMap(Team::getId, Function.identity()));
        return grants.stream()
            .map(
                grant -> {
                    Team team = teams.get(grant.getTeamId());
                    return CloudAccountTeamGrantView.builder()
                        .teamId(grant.getTeamId())
                        .teamName(team == null ? null : team.getTeamName())
                        .permissionLevel(grant.getPermissionLevel())
                        .createUserId(grant.getCreateUserId())
                        .createTime(grant.getCreateTime())
                        .build();
                })
            .collect(Collectors.toList());
    }

    @Override
    public List<CloudAccountView> listAvailableAccounts(Long teamId) {
        requireTeam(teamId);
        List<Long> accountIds =
            cloudAccountTeamMapper.selectList(
                new LambdaQueryWrapper<CloudAccountTeam>()
                    .eq(CloudAccountTeam::getTeamId, teamId)
                    .eq(CloudAccountTeam::getPermissionLevel, PERMISSION_USE))
                .stream()
                .map(CloudAccountTeam::getCloudAccountId)
                .collect(Collectors.toList());
        if (accountIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<CloudAccountView> available = new ArrayList<>();
        cloudAccountMapper.selectList(
            new LambdaQueryWrapper<CloudAccount>()
                .in(CloudAccount::getId, accountIds)
                .eq(CloudAccount::getStatus, STATUS_ENABLED)
                .orderByAsc(CloudAccount::getAccountName))
            .stream()
            .filter(
                account -> featureGate.isProviderEnabled(
                    ManagedFlinkProviderType.valueOf(account.getProviderType())))
            .map(CloudAccountModelMapper::toView)
            .forEach(available::add);
        return available;
    }

    private CloudAccount requireAccount(Long accountId) {
        ApiAlertException.throwIfNull(accountId, "Cloud account id is required.");
        CloudAccount account = cloudAccountMapper.selectById(accountId);
        ApiAlertException.throwIfNull(account, "Cloud account does not exist.");
        return account;
    }

    private void validateTeams(Set<Long> teamIds) {
        if (teamIds.isEmpty()) {
            return;
        }
        List<Team> teams = teamService.listByIds(teamIds);
        ApiAlertException.throwIfFalse(
            teams.size() == teamIds.size(),
            "One or more Teams selected for cloud account authorization do not exist.");
    }

    private void requireTeam(Long teamId) {
        ApiAlertException.throwIfNull(teamId, "Team id is required.");
        ApiAlertException.throwIfNull(teamService.getById(teamId), "Team does not exist.");
    }
}
