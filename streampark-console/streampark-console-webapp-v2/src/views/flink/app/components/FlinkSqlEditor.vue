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
import { fetchFlinkSqlVerify, fetchVariableAll } from '@/service'
import { getMonacoOptions } from '@/views/flink/app/shared/data/index'
import VariableReviewDrawer from '@/views/shared/editors/VariableReviewDrawer.vue'
import { useMonaco } from '@/hooks/web/useMonaco'
import { onKeyStroke } from '@vueuse/core'

const props = defineProps<{
    modelValue: string
    versionId?: string | null
    appId?: string | null
    height?: string
    readonly?: boolean
}>()

const emit = defineEmits<{
    'update:modelValue': [value: string]
}>()

const { t } = useI18n()
const editorRef = ref<HTMLElement | null>(null)
const verifying = ref(false)
const verifyError = ref('')
const fullscreen = ref(false)
const previewVisible = ref(false)
const variableSuggestions = ref<Array<{ text: string; description: string; value: string }>>([])

const { setContent, onUpdateValue, setMonacoSuggest, getInstance } = useMonaco(editorRef, {
    language: 'sql',
    code: props.modelValue ?? '',
    options: {
        ...(getMonacoOptions(props.readonly ?? false) as Recordable),
        readOnly: props.readonly ?? false,
        minimap: { enabled: true },
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
    verifyError.value = ''
    emit('update:modelValue', value)
})

watch(
    () => props.modelValue,
    (val) => setContent(val ?? ''),
)

watch(fullscreen, (on) => {
    relayoutEditor()
    stopEscListener?.()
    stopEscListener = null
    if (on) {
        stopEscListener = onKeyStroke('Escape', () => {
            fullscreen.value = false
        })
    }
})

let stopEscListener: (() => void) | null = null

onUnmounted(() => stopEscListener?.())

function toggleFullscreen() {
    fullscreen.value = !fullscreen.value
}

const canPreview = computed(() => /\$\{.+}/.test(props.modelValue))

const editorHeight = computed(() => {
    if (fullscreen.value) return undefined
    return props.height ?? '480px'
})

async function loadVariables() {
    const result = await fetchVariableAll()
    if (result.isSuccess && Array.isArray(result.data)) {
        variableSuggestions.value = result.data.map((v) => ({
            text: v.variableCode,
            description: v.description ?? v.variableCode,
            value: v.variableValue ?? '',
        }))
        setMonacoSuggest(variableSuggestions.value)
    }
}

async function handleFormat() {
    const { format } = await import('@/views/flink/app/FlinkSqlFormatter')
    const formatted = format(props.modelValue)
    setContent(formatted)
    emit('update:modelValue', formatted)
}

async function handleVerifySql(showSuccess = true): Promise<boolean> {
    if (!props.modelValue?.trim()) {
        verifyError.value = t('flink.app.editStreamPark.flinkSqlRequired')
        return false
    }
    if (!props.versionId) {
        window.$message?.error(t('flink.app.dependencyError'))
        return false
    }
    verifying.value = true
    verifyError.value = ''
    try {
        const result = await fetchFlinkSqlVerify({
            sql: props.modelValue,
            versionId: props.versionId,
            appId: props.appId,
        })
        if (!result.isSuccess) throwApiFailure(result, t('sys.api.apiRequestFailed'))
        const payload = result.data as Recordable | undefined
        if (payload?.code === 200 || payload?.data?.verified) {
            if (showSuccess) window.$message?.success(t('flink.app.flinkSql.successful'))
            return true
        }
        verifyError.value = payload?.data?.errorMsg || payload?.msg || t('sys.api.apiRequestFailed')
        return false
    } catch (e: any) {
        verifyError.value = e?.message || t('sys.api.apiRequestFailed')
        return false
    } finally {
        verifying.value = false
    }
}

onMounted(async () => {
    await loadVariables()
    relayoutEditor()
})

defineExpose({ handleVerifySql })
</script>

<template>
    <Teleport to="body" :disabled="!fullscreen">
        <div
            class="w-full min-w-0"
            :class="
                fullscreen
                    ? 'fixed inset-0 z-[2000] flex flex-col bg-[var(--body-color)] p-16px'
                    : ''
            "
        >
            <div v-if="fullscreen" class="mb-8px flex shrink-0 items-center justify-between">
                <span class="font-medium">{{ t('flink.app.flinkSqlLabel') }}</span>
                <n-button quaternary circle @click="fullscreen = false">
                    <template #icon>
                        <n-icon><IonIcon name="ContractOutline" /></n-icon>
                    </template>
                </n-button>
            </div>
            <div v-if="!readonly" class="mb-8px flex shrink-0 flex-wrap justify-end gap-8px">
                <n-button size="small" @click="handleFormat">
                    {{ t('flink.app.flinkSql.format') }}
                </n-button>
                <n-button v-if="canPreview" size="small" @click="previewVisible = true">
                    {{ t('flink.app.flinkSql.preview') }}
                </n-button>
                <n-button size="small" @click="toggleFullscreen">
                    <template #icon>
                        <n-icon
                            ><IonIcon :name="fullscreen ? 'ContractOutline' : 'ExpandOutline'"
                        /></n-icon>
                    </template>
                    {{
                        fullscreen
                            ? t('flink.app.flinkSql.exit')
                            : t('flink.app.flinkSql.fullScreen')
                    }}
                </n-button>
                <n-button
                    size="small"
                    type="primary"
                    :loading="verifying"
                    @click="handleVerifySql(true)"
                >
                    {{ t('flink.app.flinkSql.verify') }}
                </n-button>
            </div>
            <div
                ref="editorRef"
                class="w-full min-w-0 border border-[var(--border-color)] rounded-4px"
                :class="fullscreen ? 'min-h-0 flex-1' : ''"
                :style="editorHeight ? { height: editorHeight } : undefined"
            />
            <n-text v-if="verifyError" type="error" class="mt-8px block shrink-0 text-12px">
                {{ verifyError }}
            </n-text>
            <VariableReviewDrawer
                v-model:show="previewVisible"
                :value="modelValue"
                :suggestions="variableSuggestions"
                :title="t('flink.app.flinkSql.preview')"
            />
        </div>
    </Teleport>
</template>
