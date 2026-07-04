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
import { fetchCheckSavepointPath, fetchSavepointTrigger } from '@/service'
import { unwrapBooleanResult } from '@/utils/apiResult'
const props = defineProps<{
  show: boolean
  application: AppListRecord | null
}>()

const emit = defineEmits<{
  'update:show': [value: boolean]
  'update-option': [data: { type: 'savepointing', key: string, value: number }]
}>()

const { t } = useI18n()
const submitting = ref(false)

const formModel = ref({
  customSavepoint: '',
  nativeFormat: false,
})

watch(
  () => [props.show, props.application] as const,
  ([show]) => {
    if (!show)
      return
    formModel.value = {
      customSavepoint: '',
      nativeFormat: false,
    }
  },
)

function closeModal() {
  emit('update:show', false)
}

async function handleSavepointAction() {
  if (!props.application)
    return
  const result = await fetchSavepointTrigger({
    appId: props.application.id,
    savepointPath: formModel.value.customSavepoint || null,
    nativeFormat: formModel.value.nativeFormat,
  })
  if (!result.isSuccess)
    throwApiFailure(result, t('sys.api.apiRequestFailed'))
  window.$message?.success(t('flink.app.savepoint.requestSent'))
  emit('update-option', {
    type: 'savepointing',
    key: props.application.id,
    value: Date.now(),
  })
  closeModal()
}

async function handleSubmit() {
  if (!props.application)
    return
  submitting.value = true
  try {
    if (formModel.value.customSavepoint) {
      const checkResult = await fetchCheckSavepointPath({
        savepointPath: formModel.value.customSavepoint,
      })
      if (!checkResult.isSuccess)
        throwApiFailure(checkResult, t('sys.api.apiRequestFailed'))
      const { ok, message } = unwrapBooleanResult(checkResult.data, checkResult.message)
      if (!ok) {
        window.$message?.error(`${t('flink.app.operation.invalidSavePoint')}${message || ''}`)
        return
      }
    }
    else {
      const checkResult = await fetchCheckSavepointPath({ id: props.application.id })
      if (!checkResult.isSuccess)
        throwApiFailure(checkResult, t('sys.api.apiRequestFailed'))
      const { ok, message } = unwrapBooleanResult(checkResult.data, checkResult.message)
      if (!ok) {
        window.$message?.error(message || t('sys.api.apiRequestFailed'))
        return
      }
    }
    await handleSavepointAction()
  }
  catch (e: any) {
    showCatchError(e, t('sys.api.apiRequestFailed'))
  }
  finally {
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
        <n-icon :component="ionIconComponent('CameraOutline')" color="#3c7eff" />
        <span>{{ t('flink.app.view.savepoint') }}</span>
      </div>
    </template>
    <n-form :model="formModel" label-placement="top">
      <n-form-item :label="t('flink.app.savepoint.customSavepointLabel')">
        <n-input
          v-model:value="formModel.customSavepoint"
          :placeholder="t('flink.app.savepoint.customPathPlaceholder')"
          clearable
        />
      </n-form-item>
      <n-form-item :label="t('flink.app.savepoint.nativeFormatLabel')">
        <n-switch v-model:value="formModel.nativeFormat" />
      </n-form-item>
    </n-form>
    <template #footer>
      <n-space justify="end">
        <n-button @click="closeModal">
          {{ t('common.cancelText') }}
        </n-button>
        <n-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ t('common.apply') }}
        </n-button>
      </n-space>
    </template>
  </n-modal>
</template>
