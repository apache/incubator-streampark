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

import type { SparkApplication } from '@/types/api/spark/app.type'
import { fetchSparkYarn } from '@/service'
import { PipelineStepEnum, ReleaseStateEnum } from '@/enums/flinkEnum'
import { AppStateEnum, OptionStateEnum } from '@/enums/sparkEnum'

export async function handleView(app: SparkApplication, yarn: string | null) {
  const base = yarn ?? (await fetchSparkYarn()).data
  if (!base || !app.clusterId)
    return
  window.open(`${base}/proxy/${app.clusterId}/`)
}

export function handleAppBuildStatusColor(statusCode: number) {
  switch (statusCode) {
    case 0: return '#99A3A4'
    case 1: return '#F5B041'
    case 2: return '#3498DB'
    case 3: return '#2ECC71'
    case 4: return '#E74C3C'
    default: return '#99A3A4'
  }
}

export function handleAppBuildStatueText(statusCode: number) {
  switch (statusCode) {
    case 0: return 'UNKNOWN'
    case 1: return 'PENDING'
    case 2: return 'RUNNING'
    case 3: return 'SUCCESS'
    case 4: return 'FAILURE'
    default: return 'UNKNOWN'
  }
}

export function handleAppBuildStepTimelineColor(step: Recordable | null) {
  if (step == null)
    return 'gray'
  switch (step.status) {
    case 0:
    case 1: return '#99A3A4'
    case 2: return '#3498DB'
    case 3: return '#2ECC71'
    case 4: return '#E74C3C'
    case 5: return '#F5B041'
    default: return '#99A3A4'
  }
}

export function handleAppBuildStepText(stepStatus: number) {
  const buildStepMap: Record<number, string> = {
    [PipelineStepEnum.UNKNOWN]: 'UNKNOWN',
    [PipelineStepEnum.WAITING]: 'WAITING',
    [PipelineStepEnum.RUNNING]: 'RUNNING',
    [PipelineStepEnum.SUCCESS]: 'SUCCESS',
    [PipelineStepEnum.FAILURE]: 'FAILURE',
    [PipelineStepEnum.SKIPPED]: 'SKIPPED',
  }
  return buildStepMap[stepStatus] ?? 'UNKNOWN'
}

export function handleIsStart(app: SparkApplication, optionApps: { starting: Map<string, number> }) {
  const status = [
    AppStateEnum.ADDED,
    AppStateEnum.FAILED,
    AppStateEnum.FINISHED,
    AppStateEnum.LOST,
    AppStateEnum.REVOKED,
    AppStateEnum.SUCCEEDED,
    AppStateEnum.KILLED,
  ].includes(app.state as AppStateEnum)

  const release = [ReleaseStateEnum.DONE, ReleaseStateEnum.NEED_RESTART].includes(app.release as number)
  const optionState = !optionApps.starting.get(app.id!) || app.optionState === OptionStateEnum.NONE

  return status && release && optionState
}

export function canCancelSpark(app: SparkApplication) {
  return (
    [AppStateEnum.ACCEPTED, AppStateEnum.RUNNING, AppStateEnum.SUBMITTED].includes(app.state as AppStateEnum)
    && app.optionState === OptionStateEnum.NONE
  )
}

export function canAbortSpark(app: SparkApplication) {
  const optionTime = new Date(app.optionTime || 0).getTime()
  if (Date.now() - optionTime < 60 * 1000)
    return false
  if (app.optionState === OptionStateEnum.NONE)
    return [AppStateEnum.STARTING, AppStateEnum.MAPPING].includes(app.state as AppStateEnum)
  return true
}

export function canDeleteSpark(app: SparkApplication) {
  return [
    AppStateEnum.ADDED,
    AppStateEnum.FAILED,
    AppStateEnum.FINISHED,
    AppStateEnum.LOST,
    AppStateEnum.SUCCEEDED,
    AppStateEnum.KILLED,
  ].includes(app.state as AppStateEnum)
}
