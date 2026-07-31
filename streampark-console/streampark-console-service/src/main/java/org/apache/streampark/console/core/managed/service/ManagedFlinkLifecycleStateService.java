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
import org.apache.streampark.console.core.enums.FlinkAppStateEnum;
import org.apache.streampark.console.core.enums.OptionStateEnum;
import org.apache.streampark.console.core.managed.api.ManagedJobActionResult;
import org.apache.streampark.console.core.managed.model.ManagedFlinkLifecycleSnapshot;
import org.apache.streampark.console.core.mapper.FlinkApplicationMapper;
import org.apache.streampark.console.core.mapper.ManagedFlinkApplicationMapper;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/** Persists accepted and unknown managed lifecycle transitions under the app row lock. */
@Service
@RequiredArgsConstructor
class ManagedFlinkLifecycleStateService {

    private final ManagedFlinkApplicationMapper managedApplicationMapper;
    private final FlinkApplicationMapper applicationMapper;

    @Transactional(rollbackFor = Exception.class)
    void recordAccepted(
                        ManagedFlinkLifecycleSnapshot snapshot,
                        ManagedJobActionResult result) {
        ManagedFlinkApplication managed = lock(snapshot);
        Date now = new Date();
        ApiAlertException.throwIfFalse(
            managedApplicationMapper.update(
                null,
                new LambdaUpdateWrapper<ManagedFlinkApplication>()
                    .eq(ManagedFlinkApplication::getAppId, snapshot.getAppId())
                    .set(
                        result.getInstanceId() != null,
                        ManagedFlinkApplication::getExternalInstanceId,
                        result.getInstanceId())
                    .set(
                        ManagedFlinkApplication::getProviderRawState,
                        result.getProviderState())
                    .set(ManagedFlinkApplication::getSyncState, "PENDING")
                    .set(ManagedFlinkApplication::getNextSyncTime, now)) == 1,
            "Managed Flink lifecycle state could not be persisted.");

        FlinkAppStateEnum state;
        OptionStateEnum optionState;
        if ("START".equals(snapshot.getOperationType())) {
            state = FlinkAppStateEnum.STARTING;
            optionState = OptionStateEnum.STARTING;
        } else if ("STOP".equals(snapshot.getOperationType())) {
            state = FlinkAppStateEnum.CANCELLING;
            optionState = OptionStateEnum.CANCELLING;
        } else if ("RESTART".equals(snapshot.getOperationType())) {
            state = FlinkAppStateEnum.RESTARTING;
            optionState = OptionStateEnum.NONE;
        } else {
            throw new ApiAlertException("Managed Flink lifecycle operation type is invalid.");
        }
        ApiAlertException.throwIfFalse(
            applicationMapper.update(
                null,
                new LambdaUpdateWrapper<FlinkApplication>()
                    .eq(FlinkApplication::getId, snapshot.getAppId())
                    .set(FlinkApplication::getState, state.getValue())
                    .set(FlinkApplication::getOptionState, optionState.getValue())
                    .set(FlinkApplication::getTracking, 1)
                    .set(FlinkApplication::getOptionTime, now)
                    .set(FlinkApplication::getModifyTime, now)) == 1,
            "Managed Flink application lifecycle state could not be persisted.");
    }

    @Transactional(rollbackFor = Exception.class)
    void recordOutcomeUnknown(ManagedFlinkLifecycleSnapshot snapshot) {
        lock(snapshot);
        ApiAlertException.throwIfFalse(
            managedApplicationMapper.update(
                null,
                new LambdaUpdateWrapper<ManagedFlinkApplication>()
                    .eq(ManagedFlinkApplication::getAppId, snapshot.getAppId())
                    .set(ManagedFlinkApplication::getProviderRawState, "UNKNOWN")
                    .set(ManagedFlinkApplication::getSyncState, "PENDING")
                    .set(ManagedFlinkApplication::getNextSyncTime, new Date())) == 1,
            "Managed Flink unknown lifecycle outcome could not be persisted.");
    }

    private ManagedFlinkApplication lock(ManagedFlinkLifecycleSnapshot snapshot) {
        ApiAlertException.throwIfNull(
            managedApplicationMapper.lockByAppId(snapshot.getAppId()),
            "Managed Flink application does not exist.");
        ManagedFlinkApplication managed =
            managedApplicationMapper.selectById(snapshot.getAppId());
        ApiAlertException.throwIfTrue(
            managed == null
                || !snapshot.getJobId().equals(managed.getExternalApplicationId())
                || "STOP".equals(snapshot.getOperationType())
                    && !snapshot.getInstanceId().equals(managed.getExternalInstanceId())
                || !snapshot.getDeployedDefinitionHash()
                    .equals(managed.getDeployedDefinitionHash()),
            "Managed Flink deployed lifecycle target changed.");
        return managed;
    }
}
