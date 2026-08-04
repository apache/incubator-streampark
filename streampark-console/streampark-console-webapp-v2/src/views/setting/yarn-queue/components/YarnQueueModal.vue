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
import type { YarnQueueRecord } from '@/service/api/setting/yarnQueue'
import { fetchCheckYarnQueue, fetchYarnQueueCreate, fetchYarnQueueUpdate } from '@/service'

const props = defineProps<{
    show: boolean
    isUpdate: boolean
    record?: YarnQueueRecord | null
}>()

const emit = defineEmits<{
    'update:show': [value: boolean]
    success: [isUpdate: boolean]
}>()

const { t } = useI18n()
const formRef = ref<FormInst | null>(null)
const submitting = ref(false)

const formModel = ref({
    id: '',
    queueLabel: '',
    description: '',
})

const title = computed(() =>
    props.isUpdate ? t('setting.yarnQueue.modifyYarnQueue') : t('setting.yarnQueue.createQueue'),
)

const rules = computed<FormRules>(() => ({
    queueLabel: [
        {
            required: true,
            trigger: 'blur',
            asyncValidator: async (_rule, value: string) => {
                if (!value) throw new Error(t('setting.yarnQueue.checkResult.emptyHint'))
                const payload: Recordable = { queueLabel: value }
                if (props.isUpdate && formModel.value.id) payload.id = formModel.value.id
                const result = await fetchCheckYarnQueue(payload)
                if (!result.isSuccess) throwApiFailure(result, t('sys.api.apiRequestFailed'))
                const status = result.data?.status ?? 0
                if (status === 0) return
                if (status === 1) throw new Error(t('setting.yarnQueue.checkResult.existedHint'))
                if (status === 2)
                    throw new Error(t('setting.yarnQueue.checkResult.invalidFormatHint'))
                throw new Error(t('setting.yarnQueue.checkResult.emptyHint'))
            },
        },
    ],
    description: [
        { max: 512, message: t('setting.yarnQueue.descriptionMessage'), trigger: 'blur' },
    ],
}))

watch(
    () => [props.show, props.isUpdate, props.record] as const,
    ([show, isUpdate, record]) => {
        if (!show) return
        formModel.value = {
            id: record?.id ?? '',
            queueLabel: record?.queueLabel ?? '',
            description: record?.description ?? '',
        }
        if (!isUpdate) formModel.value.id = ''
        nextTick(() => formRef.value?.restoreValidation())
    },
    { immediate: true },
)

function closeModal() {
    emit('update:show', false)
}

async function handleSubmit() {
    await formRef.value?.validate()
    submitting.value = true
    try {
        const payload = { ...formModel.value }
        const result = props.isUpdate
            ? await fetchYarnQueueUpdate(payload)
            : await fetchYarnQueueCreate(payload)
        if (!result.isSuccess) throwApiFailure(result, t('sys.api.apiRequestFailed'))
        closeModal()
        emit('success', props.isUpdate)
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
        :title="title"
        :style="{ width: '600px' }"
        @update:show="emit('update:show', $event)"
    >
        <n-form ref="formRef" :model="formModel" :rules="rules" label-placement="top">
            <n-form-item :label="t('setting.yarnQueue.yarnQueueLabelExpression')" path="queueLabel">
                <n-input
                    v-model:value="formModel.queueLabel"
                    :placeholder="t('setting.yarnQueue.placeholder.yarnQueueLabelExpression')"
                />
            </n-form-item>
            <n-form-item :label="t('common.description')" path="description">
                <n-input
                    v-model:value="formModel.description"
                    type="textarea"
                    :rows="4"
                    :placeholder="t('setting.yarnQueue.placeholder.description')"
                />
            </n-form-item>
        </n-form>
        <template #footer>
            <n-space justify="end">
                <n-button class="e2e-yarnqueue-cancel-btn" @click="closeModal">
                    {{ t('common.cancelText') }}
                </n-button>
                <n-button
                    class="e2e-yarnqueue-submit-btn"
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
