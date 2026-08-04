<!--
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
-->
<script setup lang="ts">
import type { FormInst, FormRules } from 'naive-ui'
import { fetchCheckEnv, fetchFlinkCreate, fetchFlinkUpdate } from '@/service'
import { FlinkEnvCheckEnum } from '@/enums/flinkEnum'
import { SvgIcon } from '@/components/Icon'

export interface FlinkEnvFormData {
    flinkName: string
    flinkHome: string
    description: string
}

const props = defineProps<{
    show: boolean
    versionId: string | null
    initialData?: {
        flinkName?: string
        flinkHome?: string
        description?: string | null
    } | null
}>()

const emit = defineEmits<{
    'update:show': [value: boolean]
    success: [message: string]
}>()

const { t } = useI18n()
const formRef = ref<FormInst | null>(null)
const submitting = ref(false)

const formModel = ref<FlinkEnvFormData>({
    flinkName: '',
    flinkHome: '',
    description: '',
})

const title = computed(() => (props.versionId ? t('common.edit') : t('common.add')))

const rules = computed<FormRules>(() => ({
    flinkName: [
        {
            required: true,
            message: t('setting.flinkHome.operateMessage.flinkNameIsRequired'),
            trigger: 'blur',
        },
    ],
    flinkHome: [
        {
            required: true,
            message: t('setting.flinkHome.operateMessage.flinkHomeIsRequired'),
            trigger: 'blur',
        },
    ],
}))

watch(
    () => [props.show, props.versionId, props.initialData] as const,
    ([show, , initialData]) => {
        if (!show) return
        formModel.value = {
            flinkName: initialData?.flinkName ?? '',
            flinkHome: initialData?.flinkHome ?? '',
            description: initialData?.description ?? '',
        }
        nextTick(() => formRef.value?.restoreValidation())
    },
    { immediate: true },
)

function closeModal() {
    emit('update:show', false)
}

function resolveCheckError(checkResp: number) {
    switch (checkResp) {
        case FlinkEnvCheckEnum.INVALID_PATH:
            return t('setting.flinkHome.operateMessage.flinkHomePathIsInvalid')
        case FlinkEnvCheckEnum.NAME_REPEATED:
            return t('setting.flinkHome.operateMessage.flinkNameIsRepeated')
        case FlinkEnvCheckEnum.FLINK_DIST_NOT_FOUND:
            return t('setting.flinkHome.operateMessage.flinkDistNotFound')
        case FlinkEnvCheckEnum.FLINK_DIST_REPEATED:
            return t('setting.flinkHome.operateMessage.flinkDistIsRepeated')
        default:
            return t('sys.api.apiRequestFailed')
    }
}

async function handleSubmit() {
    await formRef.value?.validate()
    submitting.value = true
    try {
        const checkResult = await fetchCheckEnv({
            id: props.versionId,
            flinkName: formModel.value.flinkName,
            flinkHome: formModel.value.flinkHome,
        })
        if (!checkResult.isSuccess) throwApiFailure(checkResult, t('sys.api.apiRequestFailed'))

        const checkResp = Number(checkResult.data)
        if (checkResp !== FlinkEnvCheckEnum.OK) {
            window.$message?.error(resolveCheckError(checkResp))
            return
        }

        const payload = { ...formModel.value }
        const result =
            props.versionId == null
                ? await fetchFlinkCreate(payload)
                : await fetchFlinkUpdate({ id: props.versionId, ...payload })

        if (!result.isSuccess) throwApiFailure(result, t('sys.api.apiRequestFailed'))

        if (!result.data) {
            const message = (result.message || t('sys.api.apiRequestFailed')).replaceAll(
                /\[StreamPark]/g,
                '',
            )
            window.$message?.error(message)
            return
        }

        const successMessage =
            props.versionId == null
                ? formModel.value.flinkName.concat(
                      t('setting.flinkHome.operateMessage.createFlinkHomeSuccessful'),
                  )
                : formModel.value.flinkName.concat(
                      t('setting.flinkHome.operateMessage.updateFlinkHomeSuccessful'),
                  )

        closeModal()
        emit('success', successMessage)
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
        :style="{ width: '600px' }"
        @update:show="emit('update:show', $event)"
    >
        <template #header>
            <div class="flex items-center gap-8px">
                <SvgIcon name="flink" />
                <span>{{ title }}</span>
            </div>
        </template>
        <n-form ref="formRef" :model="formModel" :rules="rules" label-placement="top">
            <n-form-item :label="t('setting.flinkHome.flinkName')" path="flinkName">
                <n-input
                    v-model:value="formModel.flinkName"
                    :placeholder="t('setting.flinkHome.flinkNamePlaceholder')"
                    clearable
                />
                <n-text depth="3" class="mt-4px text-12px">
                    {{ t('setting.flinkHome.operateMessage.flinkNameTips') }}
                </n-text>
            </n-form-item>
            <n-form-item :label="t('setting.flinkHome.flinkHome')" path="flinkHome">
                <n-input
                    v-model:value="formModel.flinkHome"
                    :placeholder="t('setting.flinkHome.flinkHomePlaceholder')"
                    clearable
                />
                <n-text depth="3" class="mt-4px text-12px">
                    {{ t('setting.flinkHome.operateMessage.flinkHomeTips') }}
                </n-text>
            </n-form-item>
            <n-form-item :label="t('setting.flinkHome.description')" path="description">
                <n-input
                    v-model:value="formModel.description"
                    type="textarea"
                    :rows="3"
                    :placeholder="t('setting.flinkHome.descriptionPlaceholder')"
                    clearable
                />
            </n-form-item>
        </n-form>
        <template #footer>
            <n-space justify="end">
                <n-button @click="closeModal">
                    {{ t('common.cancelText') }}
                </n-button>
                <n-button
                    id="e2e-flinkenv-submit-btn"
                    type="primary"
                    :loading="submitting"
                    @click="handleSubmit"
                >
                    {{ t('common.submitText') }}
                </n-button>
            </n-space>
        </template>
    </n-modal>
</template>
