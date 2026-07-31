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

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Routes explicit reconciliation to the operation-specific read-only strategy. */
@Service
@RequiredArgsConstructor
public class ManagedFlinkOperationReconcileService {

    private final ManagedFlinkApplicationService applicationService;
    private final ManagedFlinkOperationService operationService;
    private final ManagedFlinkReleaseReconcileService releaseReconcileService;
    private final ManagedFlinkLifecycleReconcileService lifecycleReconcileService;
    private final ManagedFlinkSnapshotReconcileService snapshotReconcileService;

    public ManagedFlinkOperationView reconcile(Long teamId, Long appId, Long operationId) {
        ManagedFlinkOperation operation =
            operationService.getRequired(appId, operationId);
        if ("RELEASE".equals(operation.getOperationType())) {
            return releaseReconcileService.reconcile(teamId, appId, operationId);
        }
        if ("START".equals(operation.getOperationType())
            || "STOP".equals(operation.getOperationType())
            || "RESTART".equals(operation.getOperationType())) {
            return lifecycleReconcileService.reconcile(teamId, appId, operationId);
        }
        if ("SNAPSHOT".equals(operation.getOperationType())) {
            return snapshotReconcileService.reconcile(teamId, appId, operationId);
        }
        applicationService.get(teamId, appId);
        throw new ApiAlertException(
            "Managed Flink operation type does not support reconciliation.");
    }
}
