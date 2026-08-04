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
import { fetchSparkSql } from '@/service'
import { getMonacoOptions } from '@/views/flink/app/shared/data/index'
import { decodeByBase64 } from '@/utils/cipher'
import { useMonaco } from '@/hooks/web/useMonaco'

const props = defineProps<{
    show: boolean
    appId?: string | null
    sourceRecord?: Recordable | null
    versions: Array<{ label: string; value: string; version?: number }>
}>()

const emit = defineEmits<{
    'update:show': [value: boolean]
}>()

const { t } = useI18n()
const targetRecordId = ref<string | null>(null)
const loading = ref(false)
const sourceRef = ref<HTMLElement | null>(null)
const targetRef = ref<HTMLElement | null>(null)

const targetOptions = computed(() =>
    props.versions.filter((v) => v.value !== props.sourceRecord?.id),
)

const { setContent: setSource } = useMonaco(sourceRef, {
    language: 'sql',
    code: '',
    options: { ...(getMonacoOptions(true) as Recordable), readOnly: true },
})

const { setContent: setTarget } = useMonaco(targetRef, {
    language: 'sql',
    code: '',
    options: { ...(getMonacoOptions(true) as Recordable), readOnly: true },
})

function decodeSql(raw?: string) {
    if (!raw) return ''
    try {
        return decodeByBase64(raw)
    } catch {
        return raw
    }
}

async function loadSql(recordId: string, setter: (c: string) => Promise<void>) {
    if (!props.appId) return
    const result = await fetchSparkSql({ id: recordId, appId: props.appId })
    if (!result.isSuccess) throwApiFailure(result, t('sys.api.apiRequestFailed'))
    await setter(decodeSql(result.data?.sql))
}

watch(
    () => props.show,
    (show) => {
        if (show) {
            targetRecordId.value = null
            setSource('')
            setTarget('')
        }
    },
)

watch(
    () => [props.show, props.sourceRecord?.id] as const,
    async ([show, sourceId]) => {
        if (!show || !sourceId) return
        loading.value = true
        try {
            await loadSql(sourceId, setSource)
        } catch (e: any) {
            showCatchError(e, t('sys.api.apiRequestFailed'))
        } finally {
            loading.value = false
        }
    },
)

watch(targetRecordId, async (targetId) => {
    if (!targetId) return
    loading.value = true
    try {
        await loadSql(targetId, setTarget)
    } catch (e: any) {
        showCatchError(e, t('sys.api.apiRequestFailed'))
    } finally {
        loading.value = false
    }
})
</script>

<template>
    <n-modal
        :show="show"
        preset="card"
        :style="{ width: '960px' }"
        :title="t('flink.app.detail.compareFlinkSql')"
        @update:show="emit('update:show', $event)"
    >
        <n-spin :show="loading">
            <div class="mb-12px">
                <n-form-item
                    :label="t('flink.app.detail.targetVersion')"
                    label-placement="left"
                    :show-feedback="false"
                >
                    <n-select
                        v-model:value="targetRecordId"
                        :placeholder="t('flink.app.detail.compareSelectTips')"
                        :options="targetOptions"
                        clearable
                        class="max-w-320px"
                    />
                </n-form-item>
            </div>
            <n-grid :cols="2" :x-gap="12">
                <n-gi>
                    <n-text depth="3" class="mb-4px block text-12px">
                        source v{{ sourceRecord?.version ?? '-' }}
                    </n-text>
                    <div ref="sourceRef" class="compare-editor" />
                </n-gi>
                <n-gi>
                    <n-text depth="3" class="mb-4px block text-12px">
                        target v{{
                            targetOptions.find((o) => o.value === targetRecordId)?.version ?? '-'
                        }}
                    </n-text>
                    <div ref="targetRef" class="compare-editor" />
                </n-gi>
            </n-grid>
        </n-spin>
    </n-modal>
</template>

<style scoped>
.compare-editor {
    height: 420px;
    width: 100%;
    border: 1px solid var(--border-color);
    border-radius: 4px;
}
</style>
