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
import org.apache.streampark.console.core.entity.ManagedFlinkOperation;
import org.apache.streampark.console.core.managed.model.ManagedFlinkOperationView;
import org.apache.streampark.console.core.mapper.ManagedFlinkApplicationMapper;
import org.apache.streampark.console.core.mapper.ManagedFlinkOperationMapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/** Database-backed operation admission with per-application serialization. */
@Service
@RequiredArgsConstructor
public class ManagedFlinkOperationServiceImpl implements ManagedFlinkOperationService {

    private static final List<String> ACTIVE_STATES =
        Arrays.asList("ACCEPTED", "RUNNING", "UNKNOWN");

    private final ManagedFlinkOperationMapper operationMapper;

    private final ManagedFlinkApplicationMapper applicationMapper;

    private final ManagedFlinkAuditContext auditContext;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ManagedFlinkOperationView accept(
                                            Long appId,
                                            String operationType,
                                            String idempotencyKey,
                                            String requestHash,
                                            String requestJson) {
        validate(appId, operationType, idempotencyKey, requestHash, requestJson);
        ApiAlertException.throwIfNull(
            applicationMapper.lockByAppId(appId),
            "Managed Flink application does not exist.");

        ManagedFlinkOperation existing =
            operationMapper.selectOne(
                new LambdaQueryWrapper<ManagedFlinkOperation>()
                    .eq(ManagedFlinkOperation::getAppId, appId)
                    .eq(ManagedFlinkOperation::getOperationType, operationType)
                    .eq(ManagedFlinkOperation::getIdempotencyKey, idempotencyKey));
        if (existing != null) {
            ApiAlertException.throwIfFalse(
                requestHash.equals(existing.getRequestHash()),
                "The idempotency key was already used for a different request.");
            return view(existing, true);
        }

        Long active =
            operationMapper.selectCount(
                new LambdaQueryWrapper<ManagedFlinkOperation>()
                    .eq(ManagedFlinkOperation::getAppId, appId)
                    .in(ManagedFlinkOperation::getState, ACTIVE_STATES));
        ApiAlertException.throwIfTrue(
            active != null && active > 0,
            "Another managed Flink write operation is still active.");

        Date now = new Date();
        ManagedFlinkOperation operation = new ManagedFlinkOperation();
        operation.setAppId(appId);
        operation.setOperationType(operationType);
        operation.setIdempotencyKey(idempotencyKey);
        operation.setRequestHash(requestHash);
        operation.setState("ACCEPTED");
        operation.setRequestJson(requestJson);
        operation.setRetryCount(0);
        operation.setCreateUserId(auditContext.currentUserId());
        operation.setCreateTime(now);
        operation.setModifyTime(now);
        operation.setVersion(0);
        ApiAlertException.throwIfFalse(
            operationMapper.insert(operation) == 1,
            "Failed to create the managed Flink operation.");
        return view(operation, false);
    }

    @Override
    public ManagedFlinkOperation getRequired(Long appId, Long operationId) {
        ManagedFlinkOperation operation = getRequired(operationId);
        ApiAlertException.throwIfTrue(
            appId == null || !appId.equals(operation.getAppId()),
            "Managed Flink operation does not exist.");
        return operation;
    }

    @Override
    public ManagedFlinkOperation getRequired(Long operationId) {
        ApiAlertException.throwIfNull(
            operationId, "Managed Flink operation ID is required.");
        ManagedFlinkOperation operation = operationMapper.selectById(operationId);
        ApiAlertException.throwIfNull(
            operation, "Managed Flink operation does not exist.");
        return operation;
    }

    @Override
    public ManagedFlinkOperation findByIdempotency(
                                                   Long appId,
                                                   String operationType,
                                                   String idempotencyKey) {
        if (appId == null || isBlank(operationType) || isBlank(idempotencyKey)) {
            return null;
        }
        return operationMapper.selectOne(
            new LambdaQueryWrapper<ManagedFlinkOperation>()
                .eq(ManagedFlinkOperation::getAppId, appId)
                .eq(ManagedFlinkOperation::getOperationType, operationType)
                .eq(ManagedFlinkOperation::getIdempotencyKey, idempotencyKey));
    }

    @Override
    public ManagedFlinkOperationView getView(Long appId, Long operationId) {
        return view(getRequired(appId, operationId), false);
    }

    @Override
    public List<ManagedFlinkOperationView> list(Long appId) {
        ApiAlertException.throwIfNull(
            appId, "Managed Flink application ID is required.");
        return operationMapper.selectList(
            new LambdaQueryWrapper<ManagedFlinkOperation>()
                .eq(ManagedFlinkOperation::getAppId, appId)
                .orderByDesc(ManagedFlinkOperation::getCreateTime)
                .orderByDesc(ManagedFlinkOperation::getId)
                .last("limit 100"))
            .stream()
            .map(operation -> view(operation, false))
            .collect(java.util.stream.Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markRunning(Long operationId) {
        Date now = new Date();
        return operationMapper.update(
            null,
            new LambdaUpdateWrapper<ManagedFlinkOperation>()
                .eq(ManagedFlinkOperation::getId, operationId)
                .eq(ManagedFlinkOperation::getState, "ACCEPTED")
                .set(ManagedFlinkOperation::getState, "RUNNING")
                .set(ManagedFlinkOperation::getStartTime, now)
                .set(ManagedFlinkOperation::getModifyTime, now)
                .setSql("version = version + 1")) == 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markReconciling(Long operationId) {
        Date now = new Date();
        return operationMapper.update(
            null,
            new LambdaUpdateWrapper<ManagedFlinkOperation>()
                .eq(ManagedFlinkOperation::getId, operationId)
                .eq(ManagedFlinkOperation::getState, "UNKNOWN")
                .set(ManagedFlinkOperation::getState, "RUNNING")
                .set(ManagedFlinkOperation::getStartTime, now)
                .set(ManagedFlinkOperation::getFinishTime, null)
                .set(ManagedFlinkOperation::getModifyTime, now)
                .setSql("retry_count = retry_count + 1, version = version + 1")) == 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordProviderProgress(
                                       Long operationId,
                                       String providerRequestId,
                                       String providerOperationId,
                                       String resultJson) {
        int updated = operationMapper.update(
            null,
            new LambdaUpdateWrapper<ManagedFlinkOperation>()
                .eq(ManagedFlinkOperation::getId, operationId)
                .eq(ManagedFlinkOperation::getState, "RUNNING")
                .set(
                    providerRequestId != null,
                    ManagedFlinkOperation::getProviderRequestId,
                    providerRequestId)
                .set(
                    providerOperationId != null,
                    ManagedFlinkOperation::getProviderOperationId,
                    providerOperationId)
                .set(resultJson != null, ManagedFlinkOperation::getResultJson, resultJson)
                .set(ManagedFlinkOperation::getModifyTime, new Date())
                .setSql("version = version + 1"));
        ApiAlertException.throwIfFalse(
            updated == 1, "Managed Flink operation is not running.");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAwaitingReconcile(
                                      Long operationId,
                                      String providerRequestId,
                                      String providerOperationId,
                                      String resultJson) {
        Date now = new Date();
        int updated = operationMapper.update(
            null,
            new LambdaUpdateWrapper<ManagedFlinkOperation>()
                .eq(ManagedFlinkOperation::getId, operationId)
                .eq(ManagedFlinkOperation::getState, "RUNNING")
                .set(ManagedFlinkOperation::getState, "UNKNOWN")
                .set(
                    providerRequestId != null,
                    ManagedFlinkOperation::getProviderRequestId,
                    providerRequestId)
                .set(
                    providerOperationId != null,
                    ManagedFlinkOperation::getProviderOperationId,
                    providerOperationId)
                .set(resultJson != null, ManagedFlinkOperation::getResultJson, resultJson)
                .set(ManagedFlinkOperation::getErrorCode, null)
                .set(ManagedFlinkOperation::getErrorMessage, null)
                .set(ManagedFlinkOperation::getFinishTime, now)
                .set(ManagedFlinkOperation::getModifyTime, now)
                .setSql("version = version + 1"));
        ApiAlertException.throwIfFalse(
            updated == 1, "Managed Flink operation is not running.");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markSucceeded(
                              Long operationId,
                              String providerRequestId,
                              String providerOperationId,
                              String resultJson) {
        Date now = new Date();
        int updated = operationMapper.update(
            null,
            new LambdaUpdateWrapper<ManagedFlinkOperation>()
                .eq(ManagedFlinkOperation::getId, operationId)
                .eq(ManagedFlinkOperation::getState, "RUNNING")
                .set(ManagedFlinkOperation::getState, "SUCCEEDED")
                .set(
                    providerRequestId != null,
                    ManagedFlinkOperation::getProviderRequestId,
                    providerRequestId)
                .set(
                    providerOperationId != null,
                    ManagedFlinkOperation::getProviderOperationId,
                    providerOperationId)
                .set(resultJson != null, ManagedFlinkOperation::getResultJson, resultJson)
                .set(ManagedFlinkOperation::getErrorCode, null)
                .set(ManagedFlinkOperation::getErrorMessage, null)
                .set(ManagedFlinkOperation::getFinishTime, now)
                .set(ManagedFlinkOperation::getModifyTime, now)
                .setSql("version = version + 1"));
        ApiAlertException.throwIfFalse(
            updated == 1, "Managed Flink operation is not running.");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markFailed(
                           Long operationId,
                           boolean outcomeUnknown,
                           String providerRequestId,
                           String errorCode,
                           String errorMessage) {
        ApiAlertException.throwIfTrue(
            isBlank(errorCode) || errorCode.length() > 128,
            "Managed Flink operation error code is invalid.");
        ApiAlertException.throwIfTrue(
            isBlank(errorMessage) || errorMessage.length() > 512,
            "Managed Flink operation error message is invalid.");
        Date now = new Date();
        int updated = operationMapper.update(
            null,
            new LambdaUpdateWrapper<ManagedFlinkOperation>()
                .eq(ManagedFlinkOperation::getId, operationId)
                .eq(ManagedFlinkOperation::getState, "RUNNING")
                .set(
                    ManagedFlinkOperation::getState,
                    outcomeUnknown ? "UNKNOWN" : "FAILED")
                .set(
                    providerRequestId != null,
                    ManagedFlinkOperation::getProviderRequestId,
                    providerRequestId)
                .set(ManagedFlinkOperation::getErrorCode, errorCode)
                .set(ManagedFlinkOperation::getErrorMessage, errorMessage)
                .set(ManagedFlinkOperation::getFinishTime, now)
                .set(ManagedFlinkOperation::getModifyTime, now)
                .setSql("version = version + 1"));
        ApiAlertException.throwIfFalse(
            updated == 1, "Managed Flink operation is not running.");
    }

    private static void validate(
                                 Long appId,
                                 String operationType,
                                 String idempotencyKey,
                                 String requestHash,
                                 String requestJson) {
        ApiAlertException.throwIfNull(appId, "Managed Flink application ID is required.");
        ApiAlertException.throwIfTrue(
            isBlank(operationType) || operationType.length() > 32,
            "Managed Flink operation type is invalid.");
        ApiAlertException.throwIfTrue(
            isBlank(idempotencyKey) || idempotencyKey.length() > 255,
            "Managed Flink idempotency key is invalid.");
        ApiAlertException.throwIfTrue(
            requestHash == null || !requestHash.matches("[0-9a-f]{64}"),
            "Managed Flink operation request hash is invalid.");
        ApiAlertException.throwIfTrue(
            isBlank(requestJson), "Managed Flink operation request snapshot is required.");
    }

    private static ManagedFlinkOperationView view(
                                                  ManagedFlinkOperation operation,
                                                  boolean replay) {
        return ManagedFlinkOperationView.builder()
            .operationId(operation.getId())
            .appId(operation.getAppId())
            .type(operation.getOperationType())
            .state(operation.getState())
            .idempotentReplay(replay)
            .providerRequestId(operation.getProviderRequestId())
            .errorCode(operation.getErrorCode())
            .errorMessage(operation.getErrorMessage())
            .createUserId(operation.getCreateUserId())
            .createTime(operation.getCreateTime())
            .startTime(operation.getStartTime())
            .finishTime(operation.getFinishTime())
            .build();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
