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
import { fetchGatewayCreate } from '@/service'

const props = defineProps<{ show: boolean }>()

const emit = defineEmits<{
  'update:show': [value: boolean]
  success: []
}>()

const { t } = useI18n()
const formRef = ref<FormInst | null>(null)
const submitting = ref(false)

const formModel = ref({
  gatewayName: '',
  address: '',
  description: '',
})

const rules: FormRules = {
  gatewayName: [{ required: true, message: t('setting.flinkGateway.checkResult.emptyHint'), trigger: 'blur' }],
  address: [{ required: true, message: t('setting.flinkGateway.checkResult.emptyAddress'), trigger: 'blur' }],
}

watch(
  () => props.show,
  (show) => {
    if (!show)
      return
    formModel.value = { gatewayName: '', address: '', description: '' }
    nextTick(() => formRef.value?.restoreValidation())
  },
)

function closeDrawer() {
  emit('update:show', false)
}

async function handleSubmit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    const result = await fetchGatewayCreate(formModel.value)
    if (!result.isSuccess)
      throwApiFailure(result, t('sys.api.apiRequestFailed'))
    closeDrawer()
    emit('success')
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
  <n-drawer :show="show" :width="480" @update:show="emit('update:show', $event)">
    <n-drawer-content :title="t('setting.flinkGateway.createGateway')" closable>
      <n-form ref="formRef" :model="formModel" :rules="rules" label-placement="top">
        <n-form-item :label="t('setting.flinkGateway.name')" path="gatewayName">
          <n-input v-model:value="formModel.gatewayName" />
        </n-form-item>
        <n-form-item :label="t('setting.flinkGateway.gatewayAddress')" path="address">
          <n-input v-model:value="formModel.address" />
        </n-form-item>
        <n-form-item :label="t('common.description')" path="description">
          <n-input v-model:value="formModel.description" type="textarea" :rows="3" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="closeDrawer">
            {{ t('common.cancelText') }}
          </n-button>
          <n-button type="primary" :loading="submitting" @click="handleSubmit">
            {{ t('common.submitText') }}
          </n-button>
        </n-space>
      </template>
    </n-drawer-content>
  </n-drawer>
</template>
