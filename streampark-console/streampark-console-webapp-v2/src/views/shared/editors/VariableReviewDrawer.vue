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
  show: boolean
  value?: string
  suggestions?: Array<{ text: string, value: string }>
  title?: string
}>()

const emit = defineEmits<{
  'update:show': [value: boolean]
}>()

const { t } = useI18n()
const editorRef = ref<HTMLElement | null>(null)

const { setContent } = useMonaco(editorRef, {
  language: 'sql',
  code: '',
  options: {
    ...(getMonacoOptions(true) as Recordable),
    readOnly: true,
  },
})

const previewContent = computed(() => {
  if (!props.value)
    return ''
  const map = (props.suggestions ?? []).reduce<Record<string, string>>((acc, cur) => {
    acc[cur.text] = cur.value
    return acc
  }, {})
  return props.value.replace(/\$\{(.*?)}/g, (_node, key: string) => map[key] ?? `\${${key}}`)
})

watch(
  () => [props.show, previewContent.value] as const,
  async ([show]) => {
    if (show)
      await setContent(previewContent.value)
  },
)
</script>

<template>
  <n-drawer
    :show="show"
    :width="720"
    placement="right"
    @update:show="emit('update:show', $event)"
  >
    <n-drawer-content :title="title ?? t('spark.app.sparkSql.preview')" closable>
      <div ref="editorRef" class="preview-editor" />
    </n-drawer-content>
  </n-drawer>
</template>

<style scoped>
.preview-editor {
  height: calc(100vh - 150px);
  width: 100%;
}
</style>
