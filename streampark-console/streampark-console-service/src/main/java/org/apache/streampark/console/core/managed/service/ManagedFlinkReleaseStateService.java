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
import org.apache.streampark.console.core.entity.FlinkSql;
import org.apache.streampark.console.core.entity.ManagedFlinkApplication;
import org.apache.streampark.console.core.enums.ReleaseStateEnum;
import org.apache.streampark.console.core.managed.model.ManagedFlinkReleaseSnapshot;
import org.apache.streampark.console.core.mapper.FlinkApplicationMapper;
import org.apache.streampark.console.core.mapper.ManagedFlinkApplicationMapper;
import org.apache.streampark.console.core.service.FlinkSqlService;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/** Applies release-side state changes under the managed application row lock. */
@Service
@RequiredArgsConstructor
class ManagedFlinkReleaseStateService {

    private final ManagedFlinkApplicationMapper managedApplicationMapper;
    private final FlinkApplicationMapper applicationMapper;
    private final FlinkSqlService flinkSqlService;

    @Transactional(rollbackFor = Exception.class)
    void markReleasing(Long appId, String definitionHash) {
        ManagedFlinkApplication managed = lock(appId);
        if (definitionHash.equals(managed.getLocalDefinitionHash())) {
            updateReleaseState(appId, ReleaseStateEnum.RELEASING);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    void recordDraft(Long appId, String draftId) {
        lock(appId);
        ApiAlertException.throwIfFalse(
            managedApplicationMapper.update(
                null,
                new LambdaUpdateWrapper<ManagedFlinkApplication>()
                    .eq(ManagedFlinkApplication::getAppId, appId)
                    .set(ManagedFlinkApplication::getExternalDraftId, draftId)) == 1,
            "Managed Flink draft state could not be persisted.");
    }

    @Transactional(rollbackFor = Exception.class)
    void recordDeployment(
                          ManagedFlinkReleaseSnapshot snapshot,
                          String draftId,
                          String applicationId) {
        ManagedFlinkApplication managed = lock(snapshot.getAppId());
        boolean current =
            snapshot.getDefinitionHash().equals(managed.getLocalDefinitionHash());
        ApiAlertException.throwIfFalse(
            managedApplicationMapper.update(
                null,
                new LambdaUpdateWrapper<ManagedFlinkApplication>()
                    .eq(ManagedFlinkApplication::getAppId, snapshot.getAppId())
                    .set(ManagedFlinkApplication::getExternalDraftId, draftId)
                    .set(
                        ManagedFlinkApplication::getExternalApplicationId,
                        applicationId)
                    .set(
                        ManagedFlinkApplication::getDeployedDefinitionHash,
                        snapshot.getDefinitionHash())
                    .set(
                        ManagedFlinkApplication::getProviderDefinitionHash,
                        snapshot.getDefinitionHash())
                    .set(ManagedFlinkApplication::getProviderRawState, "DEPLOYED")
                    .set(ManagedFlinkApplication::getSyncState, "PENDING")
                    .set(ManagedFlinkApplication::getNextSyncTime, new Date())) == 1,
            "Managed Flink deployment state could not be persisted.");

        if (snapshot.getSqlCandidateId() != null) {
            FlinkSql releasedSql = flinkSqlService.getById(snapshot.getSqlCandidateId());
            ApiAlertException.throwIfTrue(
                releasedSql == null
                    || !snapshot.getAppId().equals(releasedSql.getAppId()),
                "Managed Flink release SQL candidate is invalid.");
            flinkSqlService.toEffective(
                snapshot.getAppId(), snapshot.getSqlCandidateId());
            flinkSqlService.cleanCandidate(snapshot.getSqlCandidateId());
        }
        updateReleaseState(
            snapshot.getAppId(),
            current ? ReleaseStateEnum.DONE : ReleaseStateEnum.NEED_RELEASE);
    }

    @Transactional(rollbackFor = Exception.class)
    void markKnownFailure(Long appId, String definitionHash) {
        ManagedFlinkApplication managed = lock(appId);
        if (definitionHash.equals(managed.getLocalDefinitionHash())) {
            updateReleaseState(appId, ReleaseStateEnum.FAILED);
        }
    }

    private ManagedFlinkApplication lock(Long appId) {
        ApiAlertException.throwIfNull(
            managedApplicationMapper.lockByAppId(appId),
            "Managed Flink application does not exist.");
        ManagedFlinkApplication managed =
            managedApplicationMapper.selectById(appId);
        ApiAlertException.throwIfNull(
            managed, "Managed Flink application does not exist.");
        return managed;
    }

    private void updateReleaseState(Long appId, ReleaseStateEnum state) {
        ApiAlertException.throwIfFalse(
            applicationMapper.update(
                null,
                new LambdaUpdateWrapper<FlinkApplication>()
                    .eq(FlinkApplication::getId, appId)
                    .set(FlinkApplication::getRelease, state.get())
                    .set(FlinkApplication::getModifyTime, new Date())) == 1,
            "Managed Flink application release state could not be persisted.");
    }
}
