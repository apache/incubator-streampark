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
import { ionIconComponent } from '@/utils/ionIcon'
import { fetchSparkBuildDetail } from '@/service'
import BuildLayer from '@/views/shared/components/BuildLayer.vue'
import { toTagColor } from '@/utils/tagColor'
import {
  handleAppBuildStatusColor,
  handleAppBuildStatueText,
  handleAppBuildStepText,
  handleAppBuildStepTimelineColor,
} from '@/views/spark/app/utils'
import { useTimeoutFn } from '@vueuse/core'
const props = defineProps<{
  show: boolean
  appId: string | null
}>()

const emit = defineEmits<{
  'update:show': [value: boolean]
}>()

const { t } = useI18n()
const pipeline = ref<Recordable | null>(null)
const docker = ref<Recordable | null>(null)
const errorLogVisible = ref(false)

const { start: startPolling, stop: stopPolling } = useTimeoutFn(() => {
  loadDetail(true)
  startPolling()
}, 1000, { immediate: false })

watch(
  () => [props.show, props.appId] as const,
  ([show, appId]) => {
    if (show && appId) {
      pipeline.value = null
      docker.value = null
      loadDetail(false)
      startPolling()
    }
    else {
      stopPolling()
    }
  },
)

function closeDrawer() {
  stopPolling()
  emit('update:show', false)
}

async function loadDetail(silent = false) {
  if (!props.appId)
    return
  try {
    const result = await fetchSparkBuildDetail({ appId: props.appId })
    if (!result.isSuccess)
      throwApiFailure(result, t('sys.api.apiRequestFailed'))
    pipeline.value = result.data?.pipeline ?? null
    docker.value = result.data?.docker ?? null
  }
  catch (e: any) {
    if (!silent) {
      showCatchError(e, t('sys.api.apiRequestFailed'))
      stopPolling()
    }
  }
}
</script>

<template>
  <n-drawer :show="show" :width="500" @update:show="(v) => { if (!v) closeDrawer() }">
    <n-drawer-content :title="t('spark.app.view.buildTitle')" closable>
      <h3 class="mb-8px flex items-center gap-8px">
        <n-icon :component="ionIconComponent('SpeedometerOutline')" />
        Summary
      </h3>
      <template v-if="pipeline">
        <n-progress
          :percentage="pipeline.percent ?? 0"
          :status="pipeline.hasError ? 'error' : (pipeline.percent < 100 ? 'default' : 'success')"
        />
        <div class="mt-10px">
          <n-tag :color="toTagColor(handleAppBuildStatusColor(pipeline.pipeStatus))">
            {{ handleAppBuildStatueText(pipeline.pipeStatus) }}
          </n-tag>
          <span class="ml-8px">cost {{ pipeline.costSec }} seconds</span>
        </div>
      </template>
      <n-empty v-else />

      <n-divider />

      <h3 class="mb-12px">
        {{ t('flink.app.view.stepTitle') }}
      </h3>
      <n-timeline v-if="pipeline?.steps?.length">
        <n-timeline-item
          v-for="stepItem in pipeline.steps"
          :key="stepItem.seq"
          :color="handleAppBuildStepTimelineColor(stepItem)"
        >
          <p>
            <n-tag :color="toTagColor(handleAppBuildStepTimelineColor(stepItem))" size="small">
              {{ handleAppBuildStepText(stepItem.status) }}
            </n-tag>
            <b class="ml-8px">Step-{{ stepItem.seq }}</b> {{ stepItem.desc }}
          </p>
          <p v-if="stepItem.status !== 0 && stepItem.status !== 1" class="text-12px text-gray-500">
            {{ stepItem.ts }}
          </p>
          <template v-if="pipeline?.pipeType === 2 && docker !== null">
            <template
              v-if="stepItem.seq === 5 && docker.pull?.layers"
            >
              <BuildLayer
                v-for="layer in docker.pull.layers"
                :key="layer.layerId"
                :layer="layer"
              />
            </template>
            <template
              v-else-if="stepItem.seq === 6 && docker.build?.steps"
            >
              <n-list bordered size="small">
                <n-list-item
                  v-for="(step, stepIndex) in docker.build.steps"
                  :key="stepIndex"
                >
                  <n-space>
                    <n-icon><IonIcon name="ArrowForwardOutline" /></n-icon>
                    <span class="text-12px">{{ step }}</span>
                  </n-space>
                </n-list-item>
              </n-list>
            </template>
            <template
              v-else-if="stepItem.seq === 7 && docker.push?.layers"
            >
              <BuildLayer
                v-for="layer in docker.push.layers"
                :key="layer.layerId"
                :layer="layer"
              />
            </template>
          </template>
        </n-timeline-item>
      </n-timeline>
      <n-empty v-else />

      <template v-if="pipeline?.hasError" #footer>
        <n-space justify="end">
          <n-button type="primary" @click="errorLogVisible = true">
            {{ t('flink.app.view.errorLog') }}
          </n-button>
        </n-space>
      </template>
    </n-drawer-content>
  </n-drawer>

  <n-drawer v-model:show="errorLogVisible" :width="640">
    <n-drawer-content :title="t('flink.app.view.errorLog')" closable>
      <h3>{{ t('flink.app.view.errorSummary') }}</h3>
      <p class="my-12px">
        {{ pipeline?.errorSummary }}
      </p>
      <n-divider />
      <h3>{{ t('flink.app.view.errorStack') }}</h3>
      <pre class="error-stack">{{ pipeline?.errorStack }}</pre>
    </n-drawer-content>
  </n-drawer>
</template>

<style scoped>
.error-stack {
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
