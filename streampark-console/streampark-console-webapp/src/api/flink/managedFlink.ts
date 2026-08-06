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

import { defHttp } from '/@/utils/http/axios';
import { ContentTypeEnum } from '/@/enums/httpEnum';
import type {
  ManagedCloudAccount,
  ManagedCloudProject,
  ManagedDraftDirectory,
  ManagedFlinkEnvironment,
  ManagedFlinkEnvironmentForm,
  ManagedFlinkEnvironmentUpdateForm,
  ManagedFlinkApplication,
  ManagedFlinkApplicationStatistics,
  ManagedFlinkApplicationForm,
  ManagedFlinkCapability,
  ManagedFlinkLifecycleRequest,
  ManagedFlinkOperation,
  ManagedFlinkSnapshot,
  ManagedFlinkSnapshotCreateRequest,
  ManagedFlinkStopRequest,
  ManagedResourcePool,
} from './managedFlink.type';

enum Api {
  AVAILABLE_ACCOUNTS = '/cloud/account/available',
  CAPABILITY = '/flink/managed/capability',
  PROJECTS = '/flink/managed/projects',
  RESOURCE_POOLS = '/flink/managed/resource-pools',
  DRAFT_DIRECTORIES = '/flink/managed/draft-directories',
  ENVIRONMENT_LIST = '/flink/managed/environment/list',
  ENVIRONMENT_GET = '/flink/managed/environment/get',
  ENVIRONMENT_CREATE = '/flink/managed/environment/create',
  ENVIRONMENT_UPDATE = '/flink/managed/environment/update',
  ENVIRONMENT_DELETE = '/flink/managed/environment/delete',
  ENVIRONMENT_PROBE = '/flink/managed/environment/probe',
  APPLICATION_GET = '/flink/managed/application/get',
  APPLICATION_FLINK_UI = '/flink/managed/application/flink-ui',
  APPLICATION_CREATE = '/flink/managed/application/create',
  APPLICATION_UPDATE = '/flink/managed/application/update',
  APPLICATION_RELEASE = '/flink/managed/application/release',
  APPLICATION_STATISTICS = '/flink/managed/application/statistics',
  APPLICATION_START = '/flink/managed/application/start',
  APPLICATION_STOP = '/flink/managed/application/stop',
  APPLICATION_RESTART = '/flink/managed/application/restart',
  SNAPSHOT_LIST = '/flink/managed/snapshot/list',
  SNAPSHOT_CREATE = '/flink/managed/snapshot/create',
  OPERATION_GET = '/flink/managed/operation/get',
  OPERATION_LIST = '/flink/managed/operation/list',
  OPERATION_RECONCILE = '/flink/managed/operation/reconcile',
}

export function fetchAvailableCloudAccounts(teamId: string): Promise<ManagedCloudAccount[]> {
  return defHttp.post({ url: Api.AVAILABLE_ACCOUNTS, data: { teamId } });
}

export function fetchManagedCapability(data: {
  teamId: string;
  cloudAccountId: string;
}): Promise<ManagedFlinkCapability> {
  return defHttp.post({ url: Api.CAPABILITY, data });
}

export function fetchManagedProjects(data: {
  teamId: string;
  cloudAccountId: string;
  keyword?: string;
}): Promise<ManagedCloudProject[]> {
  return defHttp.post({ url: Api.PROJECTS, data });
}

export function fetchManagedResourcePools(data: {
  teamId: string;
  cloudAccountId: string;
  projectId: string;
  keyword?: string;
}): Promise<ManagedResourcePool[]> {
  return defHttp.post({ url: Api.RESOURCE_POOLS, data });
}

export function fetchManagedDraftDirectories(data: {
  teamId: string;
  cloudAccountId: string;
  projectId: string;
  keyword?: string;
}): Promise<ManagedDraftDirectory[]> {
  return defHttp.post({ url: Api.DRAFT_DIRECTORIES, data });
}

export function fetchManagedEnvironments(data: {
  teamId: string;
  clusterName?: string;
}): Promise<ManagedFlinkEnvironment[]> {
  return defHttp.post({ url: Api.ENVIRONMENT_LIST, data });
}

export function fetchManagedEnvironment(data: {
  teamId: string;
  clusterId: string;
}): Promise<ManagedFlinkEnvironment> {
  return defHttp.post({ url: Api.ENVIRONMENT_GET, data });
}

export function fetchCreateManagedEnvironment(data: ManagedFlinkEnvironmentForm): Promise<string> {
  return defHttp.post({ url: Api.ENVIRONMENT_CREATE, data });
}

export function fetchUpdateManagedEnvironment(
  data: ManagedFlinkEnvironmentUpdateForm,
): Promise<void> {
  return defHttp.post({ url: Api.ENVIRONMENT_UPDATE, data });
}

export function fetchDeleteManagedEnvironment(data: {
  teamId: string;
  clusterId: string;
  version: number;
}): Promise<void> {
  return defHttp.post({ url: Api.ENVIRONMENT_DELETE, data });
}

export function fetchProbeManagedEnvironment(data: {
  teamId: string;
  clusterId: string;
}): Promise<ManagedFlinkEnvironment> {
  return defHttp.post({ url: Api.ENVIRONMENT_PROBE, data }, { errorMessageMode: 'none' });
}

export function fetchManagedApplication(data: {
  teamId: string;
  appId: string;
}): Promise<ManagedFlinkApplication> {
  return defHttp.post({ url: Api.APPLICATION_GET, data });
}

export function fetchManagedFlinkUiUrl(data: { teamId: string; appId: string }): Promise<string> {
  return defHttp.post({ url: Api.APPLICATION_FLINK_UI, data });
}

export function fetchCreateManagedApplication(data: ManagedFlinkApplicationForm): Promise<string> {
  return defHttp.post({
    url: Api.APPLICATION_CREATE,
    data,
    headers: { 'Content-Type': ContentTypeEnum.JSON },
  });
}

export function fetchUpdateManagedApplication(data: ManagedFlinkApplicationForm): Promise<void> {
  return defHttp.post({
    url: Api.APPLICATION_UPDATE,
    data,
    timeout: 30_000,
    headers: { 'Content-Type': ContentTypeEnum.JSON },
  });
}

export function fetchReleaseManagedApplication(data: {
  teamId: string;
  appId: string;
  idempotencyKey: string;
}): Promise<ManagedFlinkOperation> {
  return defHttp.post({ url: Api.APPLICATION_RELEASE, data });
}

export function fetchManagedApplicationStatistics(data: {
  teamId: string;
}): Promise<ManagedFlinkApplicationStatistics> {
  return defHttp.post({ url: Api.APPLICATION_STATISTICS, data });
}

export function fetchStartManagedApplication(
  data: ManagedFlinkLifecycleRequest,
): Promise<ManagedFlinkOperation> {
  return defHttp.post({ url: Api.APPLICATION_START, data });
}

export function fetchStopManagedApplication(
  data: ManagedFlinkStopRequest,
): Promise<ManagedFlinkOperation> {
  return defHttp.post({ url: Api.APPLICATION_STOP, data });
}

export function fetchRestartManagedApplication(
  data: ManagedFlinkLifecycleRequest,
): Promise<ManagedFlinkOperation> {
  return defHttp.post({ url: Api.APPLICATION_RESTART, data });
}

export function fetchManagedSnapshots(data: {
  teamId: string;
  appId: string;
}): Promise<ManagedFlinkSnapshot[]> {
  return defHttp.post({ url: Api.SNAPSHOT_LIST, data });
}

export function fetchCreateManagedSnapshot(
  data: ManagedFlinkSnapshotCreateRequest,
): Promise<ManagedFlinkOperation> {
  return defHttp.post({ url: Api.SNAPSHOT_CREATE, data });
}

export function fetchManagedOperation(data: {
  teamId: string;
  appId: string;
  operationId: string;
}): Promise<ManagedFlinkOperation> {
  return defHttp.post({ url: Api.OPERATION_GET, data }, { errorMessageMode: 'none' });
}

export function fetchManagedOperations(data: {
  teamId: string;
  appId: string;
}): Promise<ManagedFlinkOperation[]> {
  return defHttp.post({ url: Api.OPERATION_LIST, data });
}

export function fetchReconcileManagedOperation(data: {
  teamId: string;
  appId: string;
  operationId: string;
}): Promise<ManagedFlinkOperation> {
  return defHttp.post({ url: Api.OPERATION_RECONCILE, data });
}
