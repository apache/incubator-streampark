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

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** Immutable-in-use, secret-free request evidence for snapshot creation and reconciliation. */
@Getter
@Setter
public class ManagedFlinkSnapshotOperationSnapshot {

    private Long teamId;

    private Long appId;

    private Long cloudAccountId;

    private String providerType;

    private String projectId;

    private String jobId;

    private String instanceId;

    private String userDescription;

    private String providerDescription;

    private String requestedAt;

    private List<String> baselineSnapshotIds;
}
