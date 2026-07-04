<!--
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
-->
<script setup lang="ts">
import type { ProjectRecord } from '@/types/api/resource/project/model/projectModel'
import { useTimeoutFn } from '@vueuse/core'
const props = defineProps<{
  show: boolean
  project: ProjectRecord | null
  fetchLog: (data: Recordable) => Promise<Service.RequestResult<Recordable | string>>
}>()

const emit = defineEmits<{
  'update:show': [value: boolean]
}>()

const { t } = useI18n()

const loading = ref(false)
const logText = ref('')
const logTime = ref('')
const startOffset = ref<number | null>(null)

const modalTitle = computed(() =>
  props.project ? `${props.project.name} Build Log` : t('flink.project.operationTips.seeBuildLog'),
)

function closeModal() {
  stopPolling()
  emit('update:show', false)
}

async function refreshLog() {
  if (!props.project)
    return
  loading.value = true
  try {
    const result = await props.fetchLog({
      id: props.project.id,
      startOffset: startOffset.value,
    })
    if (!result.isSuccess)
      throwApiFailure(result, t('sys.api.apiRequestFailed'))
    const payload = (result.data ?? {}) as Recordable
    if (payload.readFinished === false)
      startPolling()
    else
      stopPolling()
    if (payload.data)
      logText.value = String(payload.data)
    logTime.value = new Date().toLocaleString()
  }
  catch (e: any) {
    showCatchError(e, t('sys.api.apiRequestFailed'))
    closeModal()
  }
  finally {
    loading.value = false
  }
}

const { start: startPolling, stop: stopPolling } = useTimeoutFn(() => {
  refreshLog()
}, 2000, { immediate: false })

watch(
  () => props.show,
  (show) => {
    if (!show) {
      stopPolling()
      return
    }
    logText.value = ''
    startOffset.value = null
    refreshLog()
  },
)
</script>

<template>
  <n-modal
    :show="show"
    preset="card"
    :title="modalTitle"
    class="w-80%"
    @update:show="(v: boolean) => !v && closeModal()"
  >
    <template #header-extra>
      <n-icon :size="18" color="#477de9">
        <IonIcon name="CodeSlashOutline" />
      </n-icon>
    </template>
    <n-spin :show="loading">
      <pre class="log-content max-h-500px min-h-300px overflow-auto whitespace-pre-wrap break-all">{{ logText }}</pre>
    </n-spin>
    <template #footer>
      <div class="flex items-center justify-between">
        <span class="text-13px text-gray-500">
          {{ t('flink.app.view.refreshTime') }}: {{ logTime }}
        </span>
        <n-button type="primary" @click="closeModal">
          {{ t('common.closeText') }}
        </n-button>
      </div>
    </template>
  </n-modal>
</template>

<style scoped>
.log-content {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 13px;
  line-height: 1.5;
  background: #fafafa;
  padding: 12px;
  border-radius: 4px;
}
</style>
