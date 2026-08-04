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
import type { AppListRecord } from '@/types/api/flink/app.type'
import { fetchCancel, fetchCheckSavepointPath } from '@/service'
import { unwrapBooleanResult } from '@/utils/apiResult'
const props = defineProps<{
    show: boolean
    application: AppListRecord | null
}>()

const emit = defineEmits<{
    'update:show': [value: boolean]
    'update-option': [data: { type: 'stopping'; key: string; value: number }]
}>()

const { t } = useI18n()
const submitting = ref(false)

const formModel = ref({
    triggerSavepoint: false,
    customSavepoint: '',
    drain: false,
})

watch(
    () => [props.show, props.application] as const,
    ([show]) => {
        if (!show) return
        formModel.value = {
            triggerSavepoint: false,
            customSavepoint: '',
            drain: false,
        }
    },
)

function closeModal() {
    emit('update:show', false)
}

async function handleStopAction() {
    if (!props.application) return
    const result = await fetchCancel({
        id: props.application.id,
        restoreOrTriggerSavepoint: formModel.value.triggerSavepoint,
        savepointPath: formModel.value.customSavepoint,
        drain: formModel.value.drain,
    })
    if (!result.isSuccess) throwApiFailure(result, t('sys.api.apiRequestFailed'))
    window.$message?.success(t('flink.app.operation.canceling'))
    emit('update-option', {
        type: 'stopping',
        key: props.application.id,
        value: Date.now(),
    })
    closeModal()
}

async function handleSubmit() {
    if (!props.application) return
    submitting.value = true
    try {
        if (formModel.value.triggerSavepoint) {
            if (formModel.value.customSavepoint) {
                const checkResult = await fetchCheckSavepointPath({
                    savepointPath: formModel.value.customSavepoint,
                })
                if (!checkResult.isSuccess)
                    throwApiFailure(checkResult, t('sys.api.apiRequestFailed'))
                const { ok, message } = unwrapBooleanResult(checkResult.data, checkResult.message)
                if (!ok) {
                    window.$message?.error(
                        t('flink.app.operation.invalidSavePoint') + (message || ''),
                    )
                    return
                }
            } else {
                const checkResult = await fetchCheckSavepointPath({ id: props.application.id })
                if (!checkResult.isSuccess)
                    throwApiFailure(checkResult, t('sys.api.apiRequestFailed'))
                const { ok, message } = unwrapBooleanResult(checkResult.data, checkResult.message)
                if (!ok) {
                    window.$message?.error(message || t('sys.api.apiRequestFailed'))
                    return
                }
            }
        }
        await handleStopAction()
    } catch (e: any) {
        showCatchError(e, t('sys.api.apiRequestFailed'))
    } finally {
        submitting.value = false
    }
}
</script>

<template>
    <n-modal
        :show="show"
        preset="card"
        :style="{ width: '560px' }"
        @update:show="emit('update:show', $event)"
    >
        <template #header>
            <div class="flex items-center gap-8px">
                <n-icon :component="ionIconComponent('PauseCircleOutline')" color="#f5222d" />
                <span>{{ t('flink.app.view.stop') }}</span>
            </div>
        </template>
        <n-form :model="formModel" label-placement="top">
            <n-form-item :label="t('flink.app.operation.triggerSavePoint')">
                <n-switch v-model:value="formModel.triggerSavepoint" />
                <n-text depth="3" class="mt-4px text-12px">
                    {{ t('flink.app.operation.enableSavePoint') }}
                </n-text>
            </n-form-item>
            <n-form-item
                v-if="formModel.triggerSavepoint"
                :label="t('flink.app.operation.savepointPath')"
            >
                <n-input
                    v-model:value="formModel.customSavepoint"
                    :placeholder="t('flink.app.operation.customSavepoint')"
                    clearable
                />
            </n-form-item>
            <n-form-item v-if="formModel.triggerSavepoint" :label="t('flink.app.operation.drain')">
                <n-switch v-model:value="formModel.drain" />
                <n-text depth="3" class="mt-4px text-12px">
                    {{ t('flink.app.operation.enableDrain') }}
                </n-text>
            </n-form-item>
        </n-form>
        <template #footer>
            <n-space justify="end">
                <n-button id="e2e-flinkapp-stop-cancel" @click="closeModal">
                    {{ t('common.cancelText') }}
                </n-button>
                <n-button
                    id="e2e-flinkapp-stop-submit"
                    type="primary"
                    :loading="submitting"
                    @click="handleSubmit"
                >
                    {{ t('common.apply') }}
                </n-button>
            </n-space>
        </template>
    </n-modal>
</template>
