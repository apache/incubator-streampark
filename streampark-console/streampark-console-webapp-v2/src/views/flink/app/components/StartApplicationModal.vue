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
import type { FormInst } from 'naive-ui'
import type { AppListRecord } from '@/types/api/flink/app.type'
import { fetchAbort, fetchCheckStart, fetchStart } from '@/service'
import { AppExistsEnum } from '@/enums/flinkEnum'
import { unwrapBooleanResult } from '@/utils/apiResult'
export interface StartModalPayload {
    application: AppListRecord
    historySavePoint?: Array<{ path: string; createTime?: string; type?: number; latest?: boolean }>
    selected?: { path: string } | null
}

const props = defineProps<{
    show: boolean
    payload: StartModalPayload | null
}>()

const emit = defineEmits<{
    'update:show': [value: boolean]
    'update-option': [data: { type: 'starting'; key: string; value: number }]
}>()

const { t } = useI18n()
const router = useRouter()
const formRef = ref<FormInst | null>(null)
const submitting = ref(false)
const selectInput = ref(false)
const savedSelectValue = ref<string | null>(null)

const formModel = ref({
    restoreSavepoint: false,
    savepointPath: '' as string | null,
    allowNonRestoredState: false,
})

const savepointOptions = computed(() =>
    (props.payload?.historySavePoint ?? []).map((k) => ({
        label: k.path,
        value: k.path,
        createTime: k.createTime,
        type: k.type,
        latest: k.latest,
    })),
)

watch(
    () => [props.show, props.payload] as const,
    ([show, payload]) => {
        if (!show || !payload) return
        selectInput.value = false
        savedSelectValue.value = null
        formModel.value = {
            restoreSavepoint: payload.selected != null,
            savepointPath: payload.selected?.path ?? '',
            allowNonRestoredState: false,
        }
    },
)

function closeModal() {
    emit('update:show', false)
}

function toggleSavepointInput() {
    if (!selectInput.value && savepointOptions.value.length > 0) {
        savedSelectValue.value = formModel.value.savepointPath
        formModel.value.savepointPath = ''
        selectInput.value = true
    } else {
        formModel.value.savepointPath = savedSelectValue.value ?? ''
        selectInput.value = false
    }
}

async function handleSubmit() {
    if (!props.payload?.application) return
    submitting.value = true
    try {
        const checkResult = await fetchCheckStart({ id: props.payload.application.id })
        if (checkResult.isSuccess && Number(checkResult.data) === AppExistsEnum.IN_YARN) {
            await fetchAbort({ id: props.payload.application.id })
        }

        if (formModel.value.restoreSavepoint && !formModel.value.savepointPath) {
            window.$message?.warning(t('flink.app.view.savepointInput'))
            return
        }

        const result = await fetchStart({
            id: props.payload.application.id,
            restoreOrTriggerSavepoint: formModel.value.restoreSavepoint,
            savepointPath: formModel.value.restoreSavepoint ? formModel.value.savepointPath : null,
            allowNonRestored: formModel.value.allowNonRestoredState,
        })

        if (!result.isSuccess) throwApiFailure(result, t('sys.api.apiRequestFailed'))

        const { ok, message } = unwrapBooleanResult(result.data, result.message)
        if (ok) {
            window.$message?.success(t('flink.app.operation.starting'))
            emit('update-option', {
                type: 'starting',
                key: props.payload.application.id,
                value: Date.now(),
            })
            closeModal()
        } else {
            const msg = (message || 'startup failed').replaceAll(/\[StreamPark]/g, '')
            window.$message?.error(msg)
            closeModal()
            window.$dialog?.warning({
                title: 'Failed',
                content: msg,
                positiveText: t('common.detailText'),
                negativeText: t('common.closeText'),
                onPositiveClick: () => {
                    router.push({
                        path: '/flink/app/detail',
                        query: { appId: props.payload!.application.id },
                    })
                },
            })
        }
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
                <n-icon :component="ionIconComponent('PlayCircleOutline')" />
                <span>{{ t('flink.app.view.start') }}</span>
            </div>
        </template>
        <n-form ref="formRef" :model="formModel" label-placement="top">
            <n-form-item :label="t('flink.app.view.fromSavepoint')">
                <n-switch v-model:value="formModel.restoreSavepoint" />
                <n-text depth="3" class="mt-4px text-12px">
                    {{ t('flink.app.view.savepointTip') }}
                </n-text>
            </n-form-item>
            <n-form-item
                v-if="formModel.restoreSavepoint"
                :label="t('flink.app.detail.detailTab.detailTabName.savepoint')"
                required
            >
                <n-select
                    v-if="!selectInput && savepointOptions.length > 0"
                    v-model:value="formModel.savepointPath"
                    :options="savepointOptions"
                    filterable
                    @dblclick="toggleSavepointInput"
                />
                <n-input
                    v-else
                    v-model:value="formModel.savepointPath"
                    :placeholder="t('flink.app.view.savepointInput')"
                    @dblclick="toggleSavepointInput"
                />
                <n-text depth="3" class="mt-4px text-12px">
                    {{
                        savepointOptions.length > 0
                            ? t('flink.app.view.savepointSwitch')
                            : t('flink.app.view.savepointInput')
                    }}
                </n-text>
            </n-form-item>
            <n-form-item
                v-if="formModel.restoreSavepoint"
                :label="t('flink.app.view.ignoreRestored')"
            >
                <n-switch v-model:value="formModel.allowNonRestoredState" />
                <n-text depth="3" class="mt-4px text-12px">
                    {{ t('flink.app.view.ignoreRestoredTip') }}
                </n-text>
            </n-form-item>
        </n-form>
        <template #footer>
            <n-space justify="end">
                <n-button id="e2e-flinkapp-start-cancel" @click="closeModal">
                    {{ t('common.cancelText') }}
                </n-button>
                <n-button
                    id="e2e-flinkapp-start-submit"
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
