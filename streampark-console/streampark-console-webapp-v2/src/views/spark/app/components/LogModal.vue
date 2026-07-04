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
import type { SparkApplication } from '@/types/api/spark/app.type'
import { fetchK8sStartLog } from '@/service'
import { useTimeoutFn } from '@vueuse/core'

const props = defineProps<{
  show: boolean
  app: SparkApplication | null
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
  if (!props.app?.id)
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
    if (!silent)
      showCatchError(e, t('sys.api.apiRequestFailed'))
  }
  finally {
    loading.value = false
  }
}
</script>

<template>
  <n-modal
    :show="show"
    preset="card"
    :style="{ width: '860px' }"
    :title="t('spark.app.operation.startLog')"
    @update:show="(v) => { if (!v) closeModal() }"
  >
    <template #header-extra>
      <n-button quaternary circle :loading="loading" @click="refreshLog(false)">
        <template #icon>
          <n-icon :component="ionIconComponent('CodeSlashOutline')" />
        </template>
      </n-button>
    </template>
    <n-spin :show="loading && !logContent">
      <n-text v-if="logTime" depth="3" class="mb-8px block text-12px">
        {{ logTime }}
      </n-text>
      <pre class="log-content">{{ logContent || t('common.noData') }}</pre>
    </n-spin>
  </n-modal>
</template>

<style scoped>
.log-content {
  min-height: 360px;
  max-height: 520px;
  overflow: auto;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
  background: var(--code-color);
  padding: 12px;
  border-radius: 4px;
}
</style>
