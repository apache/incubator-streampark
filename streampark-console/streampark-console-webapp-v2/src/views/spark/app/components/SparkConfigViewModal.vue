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
import { fetchGetSparkConf } from '@/service'
import { getMonacoOptions } from '@/views/flink/app/shared/data/index'
import { decodeByBase64 } from '@/utils/cipher'
import { useMonaco } from '@/hooks/web/useMonaco'

const props = defineProps<{
    show: boolean
    configId?: number | string | null
    version?: number | string | null
}>()

const emit = defineEmits<{
    'update:show': [value: boolean]
}>()

const { t } = useI18n()
const loading = ref(false)
const editorRef = ref<HTMLElement | null>(null)

const { setContent } = useMonaco(editorRef, {
    language: 'yaml',
    code: '',
    options: {
        ...(getMonacoOptions(true) as Recordable),
        readOnly: true,
    },
})

watch(
    () => [props.show, props.configId] as const,
    async ([show, configId]) => {
        if (!show || configId == null) return
        loading.value = true
        try {
            const result = await fetchGetSparkConf({ id: configId })
            if (!result.isSuccess) throwApiFailure(result, t('sys.api.apiRequestFailed'))
            const content = result.data?.content ? decodeByBase64(result.data.content) : ''
            await setContent(content)
        } catch (e: any) {
            showCatchError(e, t('sys.api.apiRequestFailed'))
            await setContent('')
        } finally {
            loading.value = false
        }
    },
)
</script>

<template>
    <n-modal
        :show="show"
        preset="card"
        :style="{ width: '860px' }"
        :title="`${t('spark.app.detail.detailTab.configDetail')} v${version ?? ''}`"
        @update:show="emit('update:show', $event)"
    >
        <n-spin :show="loading">
            <div ref="editorRef" class="config-view-editor" />
        </n-spin>
    </n-modal>
</template>

<style scoped>
.config-view-editor {
    height: 480px;
    width: 100%;
}
</style>
