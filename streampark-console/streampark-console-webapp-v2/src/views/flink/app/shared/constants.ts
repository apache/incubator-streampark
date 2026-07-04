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

import { AppStateEnum, OptionStateEnum, ReleaseStateEnum } from '@/enums/flinkEnum'

export interface StateMeta {
  color: string
  title: string
  class?: string
}

export { toTagColor } from '@/utils/tagColor'
export type { TagColor } from '@/utils/tagColor'

export function createStateMap(t: (key: string) => string): Record<number, StateMeta> {
  return {
    [AppStateEnum.ADDED]: { color: '#2f54eb', title: t('flink.app.runState.added') },
    [AppStateEnum.INITIALIZING]: {
      color: '#738df8',
      title: t('flink.app.runState.initializing'),
      class: 'status-processing-initializing',
    },
    [AppStateEnum.CREATED]: { color: '#2f54eb', title: t('flink.app.runState.created') },
    [AppStateEnum.STARTING]: {
      color: '#1AB58E',
      title: t('flink.app.runState.starting'),
      class: 'status-processing-starting',
    },
    [AppStateEnum.RESTARTING]: {
      color: '#13c2c2',
      title: t('flink.app.runState.restarting'),
      class: 'status-processing-restarting',
    },
    [AppStateEnum.RUNNING]: {
      color: '#52c41a',
      title: t('flink.app.runState.running'),
      class: 'status-processing-running',
    },
    [AppStateEnum.FAILING]: {
      color: '#fa541c',
      title: t('flink.app.runState.failing'),
      class: 'status-processing-failing',
    },
    [AppStateEnum.FAILED]: { color: '#f5222d', title: t('flink.app.runState.failed') },
    [AppStateEnum.CANCELLING]: { color: '#faad14', title: t('flink.app.runState.cancelling') },
    [AppStateEnum.CANCELED]: { color: '#fa8c16', title: t('flink.app.runState.canceled') },
    [AppStateEnum.FINISHED]: { color: '#1890ff', title: t('flink.app.runState.finished') },
    [AppStateEnum.SUSPENDED]: { color: '#722ed1', title: t('flink.app.runState.suspended') },
    [AppStateEnum.RECONCILING]: {
      color: '#eb2f96',
      title: t('flink.app.runState.reconciling'),
      class: 'status-processing-reconciling',
    },
    [AppStateEnum.LOST]: { color: '#333333', title: t('flink.app.runState.lost') },
    [AppStateEnum.MAPPING]: {
      color: '#13c2c2',
      title: t('flink.app.runState.mapping'),
      class: 'status-processing-restarting',
    },
    [AppStateEnum.SILENT]: {
      color: '#738df8',
      title: t('flink.app.runState.silent'),
      class: 'status-processing-initializing',
    },
    [AppStateEnum.TERMINATED]: { color: '#8E50FF', title: t('flink.app.runState.terminated') },
  }
}

export function createOptionStateMap(t: (key: string) => string): Record<number, StateMeta> {
  return {
    [OptionStateEnum.RELEASING]: {
      color: '#1ABBDC',
      title: t('flink.app.releaseState.releasing'),
      class: 'status-processing-deploying',
    },
    [OptionStateEnum.CANCELLING]: {
      color: '#faad14',
      title: t('flink.app.runState.cancelling'),
      class: 'status-processing-cancelling',
    },
    [OptionStateEnum.STARTING]: {
      color: '#1AB58E',
      title: t('flink.app.runState.starting'),
      class: 'status-processing-starting',
    },
    [OptionStateEnum.SAVEPOINTING]: {
      color: '#faad14',
      title: t('flink.app.runState.savepoint'),
      class: 'status-processing-cancelling',
    },
  }
}

export function createReleaseStateMap(t: (key: string) => string): Record<number, StateMeta> {
  return {
    [ReleaseStateEnum.FAILED]: { color: '#f5222d', title: t('flink.app.releaseState.failed') },
    [ReleaseStateEnum.DONE]: { color: '#52c41a', title: t('flink.app.releaseState.success') },
    [ReleaseStateEnum.NEED_RELEASE]: { color: '#fa8c16', title: t('flink.app.releaseState.waiting') },
    [ReleaseStateEnum.RELEASING]: {
      color: '#52c41a',
      title: t('flink.app.releaseState.releasing'),
      class: 'status-processing-deploying',
    },
    [ReleaseStateEnum.NEED_RESTART]: { color: '#fa8c16', title: t('flink.app.releaseState.pending') },
    [ReleaseStateEnum.NEED_ROLLBACK]: {
      color: '#fa8c16',
      title: t('flink.app.releaseState.waiting'),
    },
  }
}

export const buildStatusMap: Record<number, StateMeta> = {
  0: { color: '#99A3A4', title: 'UNKNOWN' },
  1: { color: '#F5B041', title: 'PENDING' },
  2: {
    color: '#3498DB',
    title: 'BUILDING',
    class: 'status-processing-deploying',
  },
  3: { color: '#2ECC71', title: 'SUCCESS' },
  4: { color: '#E74C3C', title: 'FAILURE' },
}

export function createReleaseTitleMap(t: (key: string) => string): Record<number, string> {
  return {
    [ReleaseStateEnum.FAILED]: t('flink.app.releaseHint.failed'),
    [ReleaseStateEnum.NEED_RELEASE]: t('flink.app.releaseHint.needRelease'),
    [ReleaseStateEnum.RELEASING]: t('flink.app.releaseHint.releasing'),
    [ReleaseStateEnum.NEED_RESTART]: t('flink.app.releaseHint.needRestart'),
    [ReleaseStateEnum.NEED_ROLLBACK]: t('flink.app.releaseHint.needRollback'),
  }
}
