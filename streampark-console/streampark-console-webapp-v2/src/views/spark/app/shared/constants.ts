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

import { ReleaseStateEnum } from '@/enums/flinkEnum'
import { AppStateEnum, OptionStateEnum } from '@/enums/sparkEnum'

export interface StateMeta {
  color: string
  title: string
  class?: string
}

export { toTagColor } from '@/utils/tagColor'

export function createSparkStateMap(t: (key: string) => string): Record<number, StateMeta> {
  return {
    [AppStateEnum.ADDED]: { color: '#477de9', title: t('spark.app.runState.added') },
    [AppStateEnum.NEW_SAVING]: {
      color: '#738df8',
      title: t('spark.app.runState.saving'),
      class: 'status-processing-initializing',
    },
    [AppStateEnum.NEW]: { color: '#2f54eb', title: t('spark.app.runState.new') },
    [AppStateEnum.STARTING]: {
      color: '#1AB58E',
      title: t('spark.app.runState.starting'),
      class: 'status-processing-starting',
    },
    [AppStateEnum.SUBMITTED]: {
      color: '#13c2c2',
      title: t('spark.app.runState.submitted'),
      class: 'status-processing-restarting',
    },
    [AppStateEnum.ACCEPTED]: {
      color: '#13c2c2',
      title: t('spark.app.runState.accept'),
      class: 'status-processing-restarting',
    },
    [AppStateEnum.SUCCEEDED]: {
      color: '#1890ff',
      title: t('spark.app.runState.success'),
      class: 'status-processing-success',
    },
    [AppStateEnum.RUNNING]: {
      color: '#52c41a',
      title: t('spark.app.runState.running'),
      class: 'status-processing-running',
    },
    [AppStateEnum.FINISHED]: { color: '#1890ff', title: t('spark.app.runState.finished') },
    [AppStateEnum.FAILED]: { color: '#f5222d', title: t('spark.app.runState.failed') },
    [AppStateEnum.LOST]: { color: '#333333', title: t('spark.app.runState.lost') },
    [AppStateEnum.MAPPING]: {
      color: '#13c2c2',
      title: t('spark.app.runState.mapping'),
      class: 'status-processing-restarting',
    },
    [AppStateEnum.OTHER]: { color: '#722ed1', title: t('spark.app.runState.other') },
    [AppStateEnum.REVOKED]: {
      color: '#eb2f96',
      title: t('spark.app.runState.revoked'),
      class: 'status-processing-reconciling',
    },
    [AppStateEnum.STOPPING]: {
      color: '#faad14',
      title: t('spark.app.runState.stopping'),
      class: 'status-processing-cancelling',
    },
    [AppStateEnum.KILLED]: { color: '#8E50FF', title: t('spark.app.runState.killed') },
  }
}

export function createSparkOptionStateMap(t: (key: string) => string): Record<number, StateMeta> {
  return {
    [OptionStateEnum.RELEASING]: {
      color: '#1ABBDC',
      title: t('spark.app.releaseState.releasing'),
      class: 'status-processing-deploying',
    },
    [OptionStateEnum.STOPPING]: {
      color: '#faad14',
      title: t('spark.app.runState.cancelling'),
      class: 'status-processing-cancelling',
    },
    [OptionStateEnum.STARTING]: {
      color: '#1AB58E',
      title: t('spark.app.runState.starting'),
      class: 'status-processing-starting',
    },
  }
}

export function createSparkReleaseStateMap(t: (key: string) => string): Record<number, StateMeta> {
  return {
    [ReleaseStateEnum.FAILED]: { color: '#f5222d', title: t('spark.app.releaseState.failed') },
    [ReleaseStateEnum.DONE]: { color: '#52c41a', title: t('spark.app.releaseState.success') },
    [ReleaseStateEnum.NEED_RELEASE]: { color: '#fa8c16', title: t('spark.app.releaseState.waiting') },
    [ReleaseStateEnum.RELEASING]: {
      color: '#52c41a',
      title: t('spark.app.releaseState.releasing'),
      class: 'status-processing-deploying',
    },
    [ReleaseStateEnum.NEED_RESTART]: { color: '#fa8c16', title: t('spark.app.releaseState.pending') },
    [ReleaseStateEnum.NEED_ROLLBACK]: {
      color: '#fa8c16',
      title: t('spark.app.releaseState.waiting'),
    },
  }
}

export function createSparkReleaseTitleMap(t: (key: string) => string): Record<number, string> {
  return {
    [ReleaseStateEnum.FAILED]: t('spark.app.releaseHint.failed'),
    [ReleaseStateEnum.NEED_RELEASE]: t('spark.app.releaseHint.needRelease'),
    [ReleaseStateEnum.RELEASING]: t('spark.app.releaseHint.releasing'),
    [ReleaseStateEnum.NEED_RESTART]: t('spark.app.releaseHint.needRestart'),
    [ReleaseStateEnum.NEED_ROLLBACK]: t('spark.app.releaseHint.needRollback'),
  }
}

export const sparkBuildStatusMap: Record<number, StateMeta> = {
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
