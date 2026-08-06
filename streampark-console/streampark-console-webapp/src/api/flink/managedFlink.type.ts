/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import type { CloudAccount } from '/@/api/setting/cloudAccount.type';

export type ManagedCloudAccount = CloudAccount;

export interface ManagedCloudProject {
  id: string;
  name: string;
}

export interface ManagedResourcePool {
  id: string;
  name: string;
  fullName?: string;
  totalCu?: number;
  usedCu?: number;
}

export interface ManagedDraftDirectory {
  id: string;
  name: string;
  path?: string;
  parentId?: string;
}

export interface ManagedFlinkEnvironment {
  clusterId: string;
  clusterName: string;
  description?: string;
  clusterState: number;
  createTime: string;
  createUserId: string;
  providerType: string;
  cloudAccountId: string;
  region: string;
  providerConfigJson: string;
  providerConfigVersion: number;
  consoleUrl?: string;
  capabilityJson?: string;
  lastProbeTime?: string;
  lastProbeError?: string;
  version: number;
}

export interface ManagedFlinkEnvironmentForm {
  teamId: string;
  clusterName: string;
  description?: string;
  cloudAccountId: string;
  providerConfigJson: string;
  providerConfigVersion: number;
}

export interface VolcengineEnvironmentConfig {
  projectId: string;
  projectName?: string;
  resourcePoolId: string;
  resourcePoolName?: string;
  draftDirectoryId: string;
  tosBucket: string;
}

export function parseVolcengineEnvironmentConfig(
  environment?: Pick<ManagedFlinkEnvironment, 'providerConfigJson' | 'providerConfigVersion'>,
): Partial<VolcengineEnvironmentConfig> {
  if (!environment || environment.providerConfigVersion !== 1) {
    return {};
  }
  try {
    return JSON.parse(environment.providerConfigJson) as VolcengineEnvironmentConfig;
  } catch (_error) {
    return {};
  }
}

export interface ManagedFlinkEnvironmentUpdateForm extends ManagedFlinkEnvironmentForm {
  clusterId: string;
  version: number;
}

export interface ManagedFlinkCapability {
  providerType?: string;
  apiVersion?: string;
  engineVersions: string[];
  jobTypes: string[];
  executionModes: string[];
  startModes: string[];
  schedulingStrategies: string[];
  supportsCreateSnapshot?: boolean;
  supportsStopWithSnapshot?: boolean;
  minCpu: number;
  cpuStep: number;
  memoryPerCpuGiB: number;
  maxArtifactBytes?: number;
  capabilityRevision?: string;
  expireAt?: string;
}

export interface ManagedFlinkResourceConfig {
  parallelism: number;
  taskManagerCpu: number;
  taskManagerMemoryGiB: number;
  taskManagerSlots: number;
  jobManagerCpu: number;
  jobManagerMemoryGiB: number;
}

export interface ManagedFlinkCheckpointConfig {
  enabled: boolean;
  intervalMs?: number;
  timeoutMs?: number;
  stateTtlMs?: number;
  backend?: string;
}

export interface ManagedFlinkRestartStrategyConfig {
  type: string;
  parameters: Record<string, string>;
}

export interface ManagedFlinkRuntimeConfig {
  engineVersion: string;
  executionMode: string;
  resource: ManagedFlinkResourceConfig;
  checkpoint: ManagedFlinkCheckpointConfig;
  restartStrategy: ManagedFlinkRestartStrategyConfig;
  retryOnFailure: boolean;
  retryIntervalMin?: number;
  retryMaxCount?: number;
  customProperties: Record<string, string>;
}

export interface ManagedFlinkReleaseConfig {
  priority?: number;
  schedulingStrategy: string;
  dependencyResourceNames: string[];
  customProperties: Record<string, string>;
}

export interface ManagedFlinkApplicationForm {
  appId?: string;
  version?: number;
  teamId: string;
  jobName: string;
  description?: string;
  managedEnvironmentId: string;
  jobType: string;
  sql?: string;
  jar?: string;
  mainClass?: string;
  args?: string;
  runtimeConfig: ManagedFlinkRuntimeConfig;
  releaseConfig: ManagedFlinkReleaseConfig;
}

export interface ManagedFlinkApplication extends ManagedFlinkApplicationForm {
  appId: string;
  version: number;
  providerType: string;
  estimatedCu: number;
  localDefinitionHash: string;
  deployedDefinitionHash?: string;
  providerDefinitionHash?: string;
  externalApplicationId?: string;
  externalInstanceId?: string;
  state: number;
  optionState: number;
  tracking: number;
  providerRawState?: string;
  syncState?: string;
  lastSyncTime?: string;
  consecutiveSyncFailures?: number;
  nextSyncTime?: string;
  consoleUrl?: string;
}

export type ManagedFlinkOperationState =
  | 'ACCEPTED'
  | 'RUNNING'
  | 'SUCCEEDED'
  | 'FAILED'
  | 'UNKNOWN';

export interface ManagedFlinkOperation {
  operationId: string;
  appId: string;
  type: string;
  state: ManagedFlinkOperationState;
  idempotentReplay: boolean;
  providerRequestId?: string;
  errorCode?: string;
  errorMessage?: string;
  createUserId: string;
  createTime: string;
  startTime?: string;
  finishTime?: string;
}

export interface ManagedFlinkApplicationStatistics {
  total: number;
  running: number;
  healthy: number;
  degraded: number;
  notFound: number;
  drifted: number;
  pending: number;
}

export type ManagedJobRestoreMode = 'FRESH' | 'LATEST_STATE' | 'SPECIFIED_SNAPSHOT';

export interface ManagedFlinkSnapshot {
  id: string;
  appId: string;
  snapshotId: string;
  instanceId?: string;
  snapshotType?: string;
  state: string;
  providerState?: string;
  location?: string;
  description?: string;
  latest: boolean;
  triggerTime?: string;
  completionTime?: string;
}

export interface ManagedFlinkLifecycleRequest {
  teamId: string;
  appId: string;
  idempotencyKey: string;
  restoreMode: ManagedJobRestoreMode;
  snapshotId?: string;
}

export interface ManagedFlinkStopRequest {
  teamId: string;
  appId: string;
  idempotencyKey: string;
  withSnapshot: boolean;
}

export interface ManagedFlinkSnapshotCreateRequest {
  teamId: string;
  appId: string;
  idempotencyKey: string;
  description?: string;
}
