<!--
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
<script setup lang="ts">
import type { SparkApplication } from '@/types/api/spark/app.type'
import { OptionStateEnum } from '@/enums/sparkEnum'
import { computeStateTagWidth } from '@/views/shared/utils/tagWidth'
import {
  createSparkOptionStateMap,
  createSparkReleaseStateMap,
  createSparkReleaseTitleMap,
  createSparkStateMap,
  sparkBuildStatusMap,
  toTagColor,
  type StateMeta,
} from '../shared/constants'

const props = withDefaults(defineProps<{
  option?: 'state' | 'release' | 'build'
  data: Partial<SparkApplication>
  maxTitle?: string
}>(), {
  option: 'state',
})

const { t } = useI18n()

const stateMap = computed(() => createSparkStateMap(t))
const optionStateMap = computed(() => createSparkOptionStateMap(t))
const releaseStateMap = computed(() => createSparkReleaseStateMap(t))
const releaseTitleMap = computed(() => createSparkReleaseTitleMap(t))

const tagWidth = computed(() => computeStateTagWidth(props.maxTitle))

const tagStyle = computed(() => {
  if (tagWidth.value > 0)
    return { minWidth: `${tagWidth.value}px`, textAlign: 'center' as const }
  return {}
})

function resolveMeta(map: Record<number, StateMeta>, key: number) {
  return Reflect.has(map, key) ? map[key] : null
}
</script>

<template>
  <span v-if="option === 'state'" class="bold-tag">
    <template v-if="data.optionState === OptionStateEnum.NONE">
      <n-tag
        v-if="resolveMeta(stateMap, data.state!)"
        :color="toTagColor(resolveMeta(stateMap, data.state!)!.color)"
        :class="resolveMeta(stateMap, data.state!)!.class"
        :style="tagStyle"
        size="small"
      >
        {{ resolveMeta(stateMap, data.state!)!.title }}
      </n-tag>
    </template>
    <template v-else>
      <n-tag
        v-if="resolveMeta(optionStateMap, data.optionState!)"
        :color="toTagColor(resolveMeta(optionStateMap, data.optionState!)!.color)"
        :class="resolveMeta(optionStateMap, data.optionState!)!.class"
        :style="tagStyle"
        size="small"
      >
        {{ resolveMeta(optionStateMap, data.optionState!)!.title }}
      </n-tag>
    </template>
  </span>

  <span v-else-if="option === 'release'" class="bold-tag">
    <n-tooltip v-if="releaseTitleMap[data.release!]" trigger="hover">
      <template #trigger>
        <n-tag
          v-if="resolveMeta(releaseStateMap, data.release!)"
          :color="toTagColor(resolveMeta(releaseStateMap, data.release!)!.color)"
          :class="resolveMeta(releaseStateMap, data.release!)!.class"
          :style="tagStyle"
          size="small"
        >
          {{ resolveMeta(releaseStateMap, data.release!)!.title }}
        </n-tag>
      </template>
      {{ releaseTitleMap[data.release!] }}
    </n-tooltip>
    <n-tag
      v-else-if="resolveMeta(releaseStateMap, data.release!)"
      :color="toTagColor(resolveMeta(releaseStateMap, data.release!)!.color)"
      :class="resolveMeta(releaseStateMap, data.release!)!.class"
      :style="tagStyle"
      size="small"
    >
      {{ resolveMeta(releaseStateMap, data.release!)!.title }}
    </n-tag>
  </span>

  <span v-else-if="option === 'build'" class="bold-tag">
    <n-tag
      v-if="resolveMeta(sparkBuildStatusMap, data.buildStatus!)"
      :color="toTagColor(resolveMeta(sparkBuildStatusMap, data.buildStatus!)!.color)"
      :class="resolveMeta(sparkBuildStatusMap, data.buildStatus!)!.class"
      :style="tagStyle"
      size="small"
    >
      {{ resolveMeta(sparkBuildStatusMap, data.buildStatus!)!.title }}
    </n-tag>
  </span>

  <span v-else>-</span>
</template>

<style scoped>
.bold-tag :deep(.n-tag) {
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.bold-tag :deep(.n-tag__content) {
  white-space: nowrap;
}

:deep(.status-processing-deploying) {
  animation: deploying-color 800ms ease-out infinite alternate;
}

:deep(.status-processing-initializing) {
  animation: initializing-color 800ms ease-out infinite alternate;
}

:deep(.status-processing-starting) {
  animation: starting-color 800ms ease-out infinite alternate;
}

:deep(.status-processing-restarting) {
  animation: restarting-color 800ms ease-out infinite alternate;
}

:deep(.status-processing-running) {
  animation: running-color 800ms ease-out infinite alternate;
}

:deep(.status-processing-cancelling) {
  animation: cancelling-color 800ms ease-out infinite alternate;
}

:deep(.status-processing-reconciling) {
  animation: reconciling-color 800ms ease-out infinite alternate;
}

@keyframes deploying-color {
  0% { border-color: #1abbdc; box-shadow: 0 0 1px #1abbdc, inset 0 0 2px #1abbdc; }
  100% { border-color: #1abbdc; box-shadow: 0 0 10px #1abbdc, inset 0 0 5px #1abbdc; }
}

@keyframes initializing-color {
  0% { border-color: #738df8; box-shadow: 0 0 1px #738df8, inset 0 0 2px #738df8; }
  100% { border-color: #738df8; box-shadow: 0 0 10px #738df8, inset 0 0 5px #738df8; }
}

@keyframes starting-color {
  0% { border-color: #1ab58e; box-shadow: 0 0 1px #1ab58e, inset 0 0 2px #1ab58e; }
  100% { border-color: #1ab58e; box-shadow: 0 0 10px #1ab58e, inset 0 0 5px #1ab58e; }
}

@keyframes restarting-color {
  0% { border-color: #13c2c2; box-shadow: 0 0 1px #13c2c2, inset 0 0 2px #13c2c2; }
  100% { border-color: #13c2c2; box-shadow: 0 0 10px #13c2c2, inset 0 0 5px #13c2c2; }
}

@keyframes running-color {
  0% { border-color: #52c41a; box-shadow: 0 0 1px #52c41a, inset 0 0 2px #52c41a; }
  100% { border-color: #52c41a; box-shadow: 0 0 10px #52c41a, inset 0 0 5px #52c41a; }
}

@keyframes cancelling-color {
  0% { border-color: #faad14; box-shadow: 0 0 1px #faad14, inset 0 0 2px #faad14; }
  100% { border-color: #faad14; box-shadow: 0 0 10px #faad14, inset 0 0 5px #faad14; }
}

@keyframes reconciling-color {
  0% { border-color: #eb2f96; box-shadow: 0 0 1px #eb2f96, inset 0 0 2px #eb2f96; }
  100% { border-color: #eb2f96; box-shadow: 0 0 10px #eb2f96, inset 0 0 5px #eb2f96; }
}
</style>
