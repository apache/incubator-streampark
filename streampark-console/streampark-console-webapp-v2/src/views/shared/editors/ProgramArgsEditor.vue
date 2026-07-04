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
import { getMonacoOptions } from '@/views/flink/app/shared/data/index'
import { useMonaco } from '@/hooks/web/useMonaco'
const props = defineProps<{
  modelValue: string
  suggestions?: Array<{ text: string, description: string, value: string }>
  height?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
  preview: [value: string]
}>()

const { t } = useI18n()
const editorRef = ref<HTMLElement | null>(null)
const fullscreen = ref(false)

const { setContent, onUpdateValue, setMonacoSuggest, getInstance } = useMonaco(editorRef, {
  language: 'plaintext',
  code: props.modelValue ?? '',
  options: {
    ...(getMonacoOptions(false) as Recordable),
    autoClosingBrackets: 'never',
  },
})

async function relayoutEditor() {
  await nextTick()
  requestAnimationFrame(async () => {
    const editor = await getInstance()
    editor?.layout()
  })
}

onUpdateValue((value) => {
  emit('update:modelValue', value)
})

watch(
  () => props.modelValue,
  val => setContent(val ?? ''),
)

watch(
  () => props.suggestions,
  (list) => {
    if (list?.length)
      setMonacoSuggest(list)
  },
  { immediate: true },
)

watch(fullscreen, () => relayoutEditor())

onMounted(() => {
  relayoutEditor()
})

const canPreview = computed(() => /\$\{.+}/.test(props.modelValue))

const editorHeight = computed(() => {
  if (fullscreen.value)
    return 'calc(100vh - 120px)'
  return props.height ?? '340px'
})
</script>

<template>
  <div
    class="w-full min-w-0"
    :class="fullscreen ? 'fixed inset-0 z-1000 bg-[var(--body-color)] p-16px' : ''"
  >
    <div v-if="fullscreen" class="mb-8px flex items-center justify-between">
      <span class="font-medium">Program Args</span>
      <n-button quaternary circle @click="fullscreen = false">
        <template #icon>
          <n-icon><IonIcon name="ContractOutline" /></n-icon>
        </template>
      </n-button>
    </div>
    <div
      ref="editorRef"
      class="w-full min-w-0 border border-[var(--border-color)] rounded-4px"
      :style="{ height: editorHeight }"
    />
    <n-space v-if="!fullscreen" class="mt-8px">
      <n-button v-if="canPreview" size="small" type="primary" @click="emit('preview', modelValue)">
        {{ t('flink.app.flinkSql.preview') }}
      </n-button>
      <n-button size="small" @click="fullscreen = true">
        <template #icon>
          <n-icon><IonIcon name="ExpandOutline" /></n-icon>
        </template>
        {{ t('flink.app.flinkSql.fullScreen') }}
      </n-button>
    </n-space>
  </div>
</template>
