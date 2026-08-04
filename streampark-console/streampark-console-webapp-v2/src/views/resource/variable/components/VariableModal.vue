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
import type { VariableListRecord } from '@/types/api/resource/variable/model/variableModel'
import { fetchAddVariable, fetchCheckVariableCode, fetchUpdateVariable } from '@/service'

const props = defineProps<{
    show: boolean
    isUpdate: boolean
    record?: VariableListRecord | null
}>()

const emit = defineEmits<{
    'update:show': [value: boolean]
    success: [isUpdate: boolean]
}>()

const { t } = useI18n()
const formRef = ref<FormInst | null>(null)
const submitting = ref(false)
const variableId = ref('')

const formModel = ref({
    variableCode: '',
    variableValue: '',
    description: '',
})

const title = computed(() =>
    props.isUpdate ? t('flink.variable.modifyVariable') : t('flink.variable.add'),
)

const codePattern = /^([A-Za-z])+([A-Za-z0-9._-])+$/

const rules = computed<FormRules>(() => ({
    variableCode: props.isUpdate
        ? []
        : [
              {
                  required: true,
                  trigger: 'blur',
                  asyncValidator: async (_rule, value: string) => {
                      if (!value) throw new Error(t('flink.variable.form.empty'))
                      if (value.length < 3 || value.length > 50)
                          throw new Error(t('flink.variable.form.len'))
                      if (!codePattern.test(value)) throw new Error(t('flink.variable.form.regExp'))
                      const result = await fetchCheckVariableCode({ variableCode: value })
                      if (!result.isSuccess) throwApiFailure(result, t('sys.api.apiRequestFailed'))
                      if (result.data?.status !== 'success')
                          throw new Error(result.data?.message || t('flink.variable.form.exists'))
                      if (!result.data?.data) throw new Error(t('flink.variable.form.exists'))
                  },
              },
          ],
    variableValue: [{ required: true, message: t('flink.variable.form.empty'), trigger: 'blur' }],
}))

watch(
    () => [props.show, props.isUpdate, props.record] as const,
    ([show, isUpdate, record]) => {
        if (!show) return
        variableId.value = isUpdate && record ? String(record.id) : ''
        formModel.value = {
            variableCode: record?.variableCode ?? '',
            variableValue: record?.variableValue ?? '',
            description: record?.description ?? '',
        }
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
        const payload = { ...formModel.value, id: variableId.value || undefined }
        const result = props.isUpdate
            ? await fetchUpdateVariable(payload as any)
            : await fetchAddVariable(payload as any)
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
            <n-form-item :label="t('flink.variable.table.variableCode')" path="variableCode">
                <n-input v-model:value="formModel.variableCode" :disabled="isUpdate" />
            </n-form-item>
            <n-form-item :label="t('flink.variable.table.variableValue')" path="variableValue">
                <n-input v-model:value="formModel.variableValue" type="textarea" :rows="2" />
            </n-form-item>
            <n-form-item :label="t('common.description')" path="description">
                <n-input v-model:value="formModel.description" type="textarea" :rows="3" />
            </n-form-item>
        </n-form>
        <template #footer>
            <n-space justify="end">
                <n-button class="e2e-var-cancel-btn" @click="closeModal">
                    {{ t('common.cancelText') }}
                </n-button>
                <n-button
                    class="e2e-var-submit-btn"
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
