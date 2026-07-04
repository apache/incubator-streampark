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
import type { AppListRecord } from '@/types/api/flink/app.type'
import { fetchK8sStartLog } from '@/service'
import { useTimeoutFn } from '@vueuse/core'

const props = defineProps<{
  show: boolean
  app: AppListRecord | null
}>()

const emit = defineEmits<{
  'update:show': [value: boolean]
}>()

const { t } = useI18n()
const loading = ref(false)
const logTime = ref('')
const logContent = ref('')
let offset = 0

const { start: startPolling, stop: stopPolling } = useTimeoutFn(() => {
  refreshLog(true)
  startPolling()
}, 3000, { immediate: false })

watch(
  () => [props.show, props.app] as const,
  ([show, app]) => {
    if (show && app) {
      offset = 0
      logContent.value = ''
      refreshLog(false)
      startPolling()
    }
    else {
      stopPolling()
    }
  },
)

function closeModal() {
  stopPolling()
  emit('update:show', false)
}

async function refreshLog(silent = false) {
  if (!props.app)
    return
  if (!silent)
    loading.value = true
  try {
    const result = await fetchK8sStartLog({
      id: props.app.id,
      offset,
      limit: 100,
    })
    if (!result.isSuccess)
      throwApiFailure(result, t('sys.api.apiRequestFailed'))
    const payload = result.data as { status?: string, data?: string }
    if (payload?.status === 'success' && payload.data) {
      logContent.value += payload.data
      offset += 100
      logTime.value = new Date().toLocaleString()
    }
  }
  catch (e: any) {
    if (!silent) {
      showCatchError(e, t('sys.api.apiRequestFailed'))
      closeModal()
    }
  }
  finally {
    if (!silent)
      loading.value = false
  }
}
</script>

<template>
  <n-modal
    :show="show"
    preset="card"
    :style="{ width: '80vw' }"
    @update:show="(v) => { if (!v) closeModal() }"
  >
    <template #header>
      <div class="flex items-center gap-8px">
        <n-icon :component="ionIconComponent('CodeSlashOutline')" color="#477de9" />
        <span>{{ t('flink.app.view.logTitle', [app?.jobName ?? '']) }}</span>
      </div>
    </template>
    <pre class="log-box">{{ logContent }}</pre>
    <template #footer>
      <div class="flex items-center justify-between">
        <span class="text-12px text-gray-500">
          {{ t('flink.app.view.refreshTime') }}: {{ logTime }}
        </span>
        <n-space>
          <n-button type="primary" :loading="loading" @click="refreshLog(false)">
            {{ t('flink.app.view.refresh') }}
          </n-button>
          <n-button type="primary" @click="closeModal">
            {{ t('common.closeText') }}
          </n-button>
        </n-space>
      </div>
    </template>
  </n-modal>
</template>

<style scoped>
.log-box {
  min-height: 480px;
  max-height: 60vh;
  overflow: auto;
  padding: 12px;
  background: #1e1e1e;
  color: #d4d4d4;
  font-family: monospace;
  font-size: 12px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-all;
  border-radius: 4px;
}
</style>
