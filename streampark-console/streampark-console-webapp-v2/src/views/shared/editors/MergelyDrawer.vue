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
import { fetchConfTemplate, fetchSparkConfTemplate } from '@/service'
import { getMonacoOptions } from '@/views/flink/app/shared/data/index'
import { useDiffMonaco } from '@/views/shared/composables/useDiffMonaco'
import { useMonaco } from '@/hooks/web/useMonaco'
import { decodeByBase64 } from '@/utils/cipher'

const props = withDefaults(defineProps<{
  show: boolean
  originalValue?: string
  readOnly?: boolean
  templateSource?: 'spark' | 'flink'
}>(), {
  templateSource: 'spark',
})

const emit = defineEmits<{
  'update:show': [value: boolean]
  ok: [payload: { isSetConfig: boolean, configOverride: string | null }]
}>()

const { t } = useI18n()
const loading = ref(false)
const showDiff = ref(false)
const templateValue = ref('')
const currentValue = ref('')
const editorRef = ref<HTMLElement | null>(null)
const diffRef = ref<HTMLElement | null>(null)

const { setContent, onUpdateValue, getContent } = useMonaco(editorRef, {
  language: 'yaml',
  code: '',
  options: getMonacoOptions(props.readOnly ?? false) as Recordable,
})

onUpdateValue((value) => {
  currentValue.value = value
})

useDiffMonaco(
  diffRef,
  'yaml',
  () => templateValue.value,
  () => currentValue.value,
  getMonacoOptions(true) as Recordable,
)

watch(
  () => props.show,
  async (show) => {
    if (!show) {
      showDiff.value = false
      return
    }
    loading.value = true
    try {
      const templateResult = props.templateSource === 'flink'
        ? await fetchConfTemplate()
        : await fetchSparkConfTemplate()
      const raw = templateResult.isSuccess ? (templateResult.data ?? '') : ''
      templateValue.value = raw ? decodeByBase64(raw) : ''
      const initial = props.originalValue?.trim() || templateValue.value
      currentValue.value = initial
      await setContent(initial)
    }
    finally {
      loading.value = false
    }
  },
)

async function handleCompare() {
  const value = await getContent()
  if (value != null)
    currentValue.value = value
  showDiff.value = true
}

function handleBackFromDiff() {
  showDiff.value = false
  nextTick(() => setContent(currentValue.value))
}

async function handleOk() {
  let value = currentValue.value
  if (!showDiff.value) {
    const content = await getContent()
    value = content?.trim() ?? ''
  }
  if (!value?.trim())
    emit('ok', { isSetConfig: false, configOverride: null })
  else
    emit('ok', { isSetConfig: true, configOverride: value })
  emit('update:show', false)
}
</script>

<template>
  <n-drawer :show="show" :width="860" placement="right" @update:show="emit('update:show', $event)">
    <n-drawer-content :title="t('spark.app.appConf')" closable>
      <n-spin :show="loading">
        <n-space v-if="!readOnly && !showDiff" class="mb-12px">
          <n-button size="small" @click="handleCompare">
            {{ t('spark.app.detail.compareConfig') }}
          </n-button>
        </n-space>
        <div v-if="!showDiff" ref="editorRef" class="config-editor" />
        <div v-else>
          <n-space class="mb-12px">
            <n-button size="small" @click="handleBackFromDiff">
              {{ t('common.previous') }}
            </n-button>
          </n-space>
          <div ref="diffRef" class="config-editor" />
        </div>
      </n-spin>
      <template v-if="!readOnly" #footer>
        <n-space justify="end">
          <n-button @click="emit('update:show', false)">
            {{ t('common.cancelText') }}
          </n-button>
          <n-button type="primary" @click="handleOk">
            {{ t('common.okText') }}
          </n-button>
        </n-space>
      </template>
    </n-drawer-content>
  </n-drawer>
</template>

<style scoped>
.config-editor {
  height: calc(100vh - 200px);
  width: 100%;
  border: 1px solid var(--border-color);
}
</style>
