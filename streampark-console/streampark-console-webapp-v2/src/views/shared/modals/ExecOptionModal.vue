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
import { isDark, useMonaco } from '@/hooks/web/useMonaco'

const props = defineProps<{
    show: boolean
    content?: string
}>()

const emit = defineEmits<{
    'update:show': [value: boolean]
}>()

const { t } = useI18n()
const editorRef = ref<HTMLElement | null>(null)

const { setContent } = useMonaco(
    editorRef,
    {
        language: 'log',
        options: {
            theme: 'log',
            readOnly: true,
            scrollBeyondLastLine: false,
            overviewRulerBorder: false,
            tabSize: 2,
            minimap: { enabled: true },
        },
    },
    handleLogMonaco,
)

async function handleLogMonaco(monaco: any) {
    monaco.languages.register({ id: 'log' })
    monaco.languages.setMonarchTokensProvider('log', {
        tokenizer: {
            root: [
                [/.*\.Exception.*/, 'log-error'],
                [/.*Caused\s+by:.*/, 'log-error'],
                [/\s+at\s+.*/, 'log-info'],
                [/http:\/\/(.*):\d+(.*)\/application_\d+_\d+/, 'yarn-info'],
                [/\[20\d+-\d+-\d+\s+\d+:\d+:\d+\d+|.\d+]/, 'log-date'],
                [/\[[a-zA-Z 0-9:]+]/, 'log-date'],
            ],
        },
    })
    monaco.editor.defineTheme('log', {
        base: unref(isDark) ? 'vs-dark' : 'vs',
        inherit: true,
        colors: {},
        rules: [
            { token: 'log-info', foreground: '808080' },
            { token: 'log-error', foreground: 'ff0000', fontStyle: 'bold' },
            { token: 'yarn-info', foreground: '0066FF', fontStyle: 'bold' },
            { token: 'log-date', foreground: '008800' },
        ],
    })
}

watch(
    () => [props.show, props.content] as const,
    async ([show, content]) => {
        if (show) await setContent(content ?? '')
    },
)
</script>

<template>
    <n-modal
        :show="show"
        preset="card"
        :style="{ width: '80%' }"
        :title="t('flink.app.detail.exceptionModal.title')"
        @update:show="emit('update:show', $event)"
    >
        <div ref="editorRef" class="exception-editor" />
    </n-modal>
</template>

<style scoped>
.exception-editor {
    height: 540px;
    width: 100%;
}
</style>
