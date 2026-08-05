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
import { fetchSparkEnvCheck, fetchSparkEnvCreate, fetchSparkEnvUpdate } from '@/service'
import { SparkEnvCheckEnum } from '@/enums/sparkEnum'
import { SvgIcon } from '@/components/Icon'

export interface SparkEnvFormData {
    sparkName: string
    sparkHome: string
    description: string
}

const props = defineProps<{
    show: boolean
    versionId: string | null
    initialData?: {
        sparkName?: string
        sparkHome?: string
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

const formModel = ref<SparkEnvFormData>({
    sparkName: '',
    sparkHome: '',
    description: '',
})

const title = computed(() => (props.versionId ? t('common.edit') : t('common.add')))

const rules = computed<FormRules>(() => ({
    sparkName: [
        {
            required: true,
            message: t('spark.home.tips.sparkNameIsRequired'),
            trigger: 'blur',
        },
    ],
    sparkHome: [
        {
            required: true,
            message: t('spark.home.tips.sparkHomeIsRequired'),
            trigger: 'blur',
        },
    ],
}))

watch(
    () => [props.show, props.versionId, props.initialData] as const,
    ([show, , initialData]) => {
        if (!show) return
        formModel.value = {
            sparkName: initialData?.sparkName ?? '',
            sparkHome: initialData?.sparkHome ?? '',
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
        case SparkEnvCheckEnum.INVALID_PATH:
            return t('spark.home.tips.sparkHomePathIsInvalid')
        case SparkEnvCheckEnum.NAME_REPEATED:
            return t('spark.home.tips.sparkNameIsRepeated')
        case SparkEnvCheckEnum.SPARK_DIST_NOT_FOUND:
            return t('spark.home.tips.sparkDistNotFound')
        case SparkEnvCheckEnum.SPARK_DIST_REPEATED:
            return t('spark.home.tips.sparkDistIsRepeated')
        default:
            return t('sys.api.apiRequestFailed')
    }
}

async function handleSubmit() {
    await formRef.value?.validate()
    submitting.value = true
    try {
        const checkResult = await fetchSparkEnvCheck({
            id: props.versionId,
            sparkName: formModel.value.sparkName,
            sparkHome: formModel.value.sparkHome,
        })
        if (!checkResult.isSuccess) throwApiFailure(checkResult, t('sys.api.apiRequestFailed'))

        const checkResp = Number(checkResult.data)
        if (checkResp !== SparkEnvCheckEnum.OK) {
            window.$message?.error(resolveCheckError(checkResp))
            return
        }

        const payload = { ...formModel.value }
        const result =
            props.versionId == null
                ? await fetchSparkEnvCreate(payload as any)
                : await fetchSparkEnvUpdate({ id: props.versionId, ...payload } as any)

        if (!result.isSuccess) throwApiFailure(result, t('sys.api.apiRequestFailed'))

        const inner = result.data as
            { data?: boolean; msg?: string; message?: string } | boolean | undefined
        const success = typeof inner === 'boolean' ? inner : Boolean(inner?.data)
        if (!success) {
            const message =
                (typeof inner === 'object' ? inner?.msg || inner?.message : result.message) ||
                t('sys.api.apiRequestFailed')
            window.$message?.error(String(message).replaceAll(/\[StreamPark]/g, ''))
            return
        }

        const successMessage =
            props.versionId == null
                ? formModel.value.sparkName.concat(t('spark.home.tips.createSparkHomeSuccessful'))
                : formModel.value.sparkName.concat(t('spark.home.tips.updateSparkHomeSuccessful'))

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
                <SvgIcon name="spark" />
                <span>{{ title }}</span>
            </div>
        </template>
        <n-form ref="formRef" :model="formModel" :rules="rules" label-placement="top">
            <n-form-item :label="t('spark.home.form.sparkName')" path="sparkName">
                <n-input
                    v-model:value="formModel.sparkName"
                    :placeholder="t('spark.home.placeholder.sparkName')"
                    clearable
                />
                <span class="tip-info">
                    {{ t('spark.home.tips.sparkName') }}
                </span>
            </n-form-item>
            <n-form-item :label="t('spark.home.form.sparkHome')" path="sparkHome">
                <n-input
                    v-model:value="formModel.sparkHome"
                    :placeholder="t('spark.home.placeholder.sparkHome')"
                    clearable
                />
                <span class="tip-info">
                    {{ t('spark.home.tips.sparkHome') }}
                </span>
            </n-form-item>
            <n-form-item :label="t('spark.home.form.description')" path="description">
                <n-input
                    v-model:value="formModel.description"
                    type="textarea"
                    :rows="3"
                    :placeholder="t('spark.home.placeholder.description')"
                    clearable
                />
            </n-form-item>
        </n-form>
        <template #footer>
            <n-space justify="end">
                <n-button @click="closeModal">
                    {{ t('common.cancelText') }}
                </n-button>
                <n-button type="primary" :loading="submitting" @click="handleSubmit">
                    {{ t('common.submitText') }}
                </n-button>
            </n-space>
        </template>
    </n-modal>
</template>
