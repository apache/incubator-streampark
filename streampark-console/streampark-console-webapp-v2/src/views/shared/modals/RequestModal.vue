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
import type { AppListRecord } from '@/types/api/flink/app.type'
import { fetchApiSchema, fetchCheckToken, fetchCopyCurl } from '@/service'
import { baseUrl } from '@/utils/url'
import { useClipboard } from '@vueuse/core'

const props = defineProps<{
  show: boolean
  app?: Partial<AppListRecord> | null
  apiName?: string
}>()

const emit = defineEmits<{
  'update:show': [value: boolean]
}>()

const { t } = useI18n()
const { copy } = useClipboard({ legacy: true })
const loading = ref(false)
const schema = ref<Recordable | null>(null)

watch(
  () => [props.show, props.apiName] as const,
  async ([show, name]) => {
    if (!show || !name) {
      schema.value = null
      return
    }
    loading.value = true
    try {
      const result = await fetchApiSchema({ name })
      if (result.isSuccess)
        schema.value = result.data as Recordable
    }
    catch {
      schema.value = null
    }
    finally {
      loading.value = false
    }
  },
)

async function handleCopyCurl() {
  if (!props.app?.id || !props.apiName)
    return
  loading.value = true
  try {
    const checkResult = await fetchCheckToken({})
    if (!checkResult.isSuccess) {
      showResultError(checkResult, t('sys.api.apiRequestFailed'))
      return
    }
    const code = Number(checkResult.data)
    if (code === 0) {
      window.$message?.error(t('flink.app.detail.nullAccessToken'))
      return
    }
    if (code === 1) {
      window.$message?.error(t('flink.app.detail.invalidAccessToken'))
      return
    }
    if (code === 2) {
      window.$message?.error(t('flink.app.detail.invalidTokenUser'))
      return
    }
    const result = await fetchCopyCurl({
      baseUrl: baseUrl(),
      appId: props.app.id,
      name: props.apiName,
    })
    if (!result.isSuccess) {
      showResultError(result, t('sys.api.apiRequestFailed'))
      return
    }
    await copy(result.data ?? '')
    window.$message?.success(t('flink.app.detail.detailTab.copySuccess'))
    emit('update:show', false)
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
    :style="{ width: '800px' }"
    :title="t('flink.app.detail.apiTitle')"
    @update:show="emit('update:show', $event)"
  >
    <n-spin :show="loading">
      <n-descriptions v-if="schema" bordered :column="1" label-placement="left" size="small">
        <n-descriptions-item label="Method">
          {{ schema.method ?? '-' }}
        </n-descriptions-item>
        <n-descriptions-item label="Path">
          {{ schema.path ?? '-' }}
        </n-descriptions-item>
        <n-descriptions-item label="Description">
          {{ schema.description ?? '-' }}
        </n-descriptions-item>
      </n-descriptions>
      <n-empty v-else />
    </n-spin>
    <template #footer>
      <n-space justify="end">
        <n-button @click="emit('update:show', false)">
          {{ t('common.cancelText') }}
        </n-button>
        <n-button type="primary" :loading="loading" @click="handleCopyCurl">
          {{ t('flink.app.detail.copyCurl') }}
        </n-button>
      </n-space>
    </template>
  </n-modal>
</template>
