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
import type { FormInst, FormRules, SelectOption } from 'naive-ui'
import { fetchNoTokenUserList, fetchTokenCreate } from '@/service'

const props = defineProps<{
    show: boolean
}>()

const emit = defineEmits<{
    'update:show': [value: boolean]
    success: []
}>()

const { t } = useI18n()
const formRef = ref<FormInst | null>(null)
const submitting = ref(false)
const userOptions = ref<SelectOption[]>([])

const formModel = ref({
    userId: null as string | null,
    description: '',
})

const rules: FormRules = {
    userId: [
        { required: true, message: t('system.token.selectUserAlertMessage'), trigger: 'change' },
    ],
}

async function loadUsers() {
    const result = await fetchNoTokenUserList({})
    if (result.isSuccess && result.data?.records) {
        userOptions.value = result.data.records.map((item) => ({
            label: item.username,
            value: item.userId,
        }))
    }
}

watch(
    () => props.show,
    async (show) => {
        if (!show) return
        formModel.value = { userId: null, description: '' }
        await loadUsers()
        nextTick(() => formRef.value?.restoreValidation())
    },
)

function closeModal() {
    emit('update:show', false)
}

async function handleSubmit() {
    await formRef.value?.validate()
    submitting.value = true
    try {
        const result = await fetchTokenCreate({
            userId: Number(formModel.value.userId),
            description: formModel.value.description,
            teamId: '',
        } as any)
        if (!result.isSuccess) throwApiFailure(result, t('sys.api.apiRequestFailed'))
        closeModal()
        emit('success')
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
        :title="t('system.token.addToken')"
        :style="{ width: '600px' }"
        @update:show="emit('update:show', $event)"
    >
        <n-form ref="formRef" :model="formModel" :rules="rules" label-placement="top">
            <n-form-item :label="t('system.token.table.userName')" path="userId">
                <n-select v-model:value="formModel.userId" filterable :options="userOptions" />
            </n-form-item>
            <n-form-item :label="t('common.description')" path="description">
                <n-input v-model:value="formModel.description" type="textarea" :rows="3" />
            </n-form-item>
        </n-form>
        <template #footer>
            <n-space justify="end">
                <n-button class="e2e-token-cancel-btn" @click="closeModal">
                    {{ t('common.cancelText') }}
                </n-button>
                <n-button
                    class="e2e-token-submit-btn"
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
