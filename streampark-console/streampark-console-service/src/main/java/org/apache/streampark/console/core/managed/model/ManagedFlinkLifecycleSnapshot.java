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

package org.apache.streampark.console.core.managed.model;

import org.apache.streampark.console.core.managed.api.ManagedJobRestoreMode;

import lombok.Getter;
import lombok.Setter;

/** Immutable-in-use, secret-free snapshot for one managed Flink lifecycle operation. */
@Getter
@Setter
public class ManagedFlinkLifecycleSnapshot {

    private Long teamId;

    private Long appId;

    private Long cloudAccountId;

    private String providerType;

    private String providerConfigJson;

    private Integer providerConfigVersion;

    private String jobName;

    private String operationType;

    private String jobId;

    private String instanceId;

    private String priority;

    private String schedulePolicy;

    private Integer scheduleTimeoutSeconds;

    private ManagedJobRestoreMode restoreMode;

    private String snapshotId;

    private String snapshotSourceInstanceId;

    private boolean withSnapshot;

    private String deployedDefinitionHash;
}
