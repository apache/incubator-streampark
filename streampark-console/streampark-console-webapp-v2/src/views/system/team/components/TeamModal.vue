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
import type { TeamListRecord } from '@/types/api/system/model/teamModel'
import { fetchTeamCreate, fetchTeamUpdate } from '@/service'

const props = defineProps<{
  show: boolean
  isUpdate: boolean
  record?: TeamListRecord | null
}>()

const emit = defineEmits<{
  'update:show': [value: boolean]
  success: [isUpdate: boolean]
}>()

const { t } = useI18n()
const formRef = ref<FormInst | null>(null)
const submitting = ref(false)
const teamId = ref<string>('')

const formModel = ref({
  teamName: '',
  description: '',
})

const title = computed(() =>
  props.isUpdate ? t('system.team.modifyTeam') : t('system.team.addTeam'),
)

const rules = computed<FormRules>(() => ({
  teamName: props.isUpdate
    ? []
    : [{ required: true, min: 4, message: t('system.team.table.teamMessage'), trigger: 'blur' }],
  description: [{ max: 100, message: t('system.team.table.descriptionMessage'), trigger: 'blur' }],
}))

watch(
  () => [props.show, props.isUpdate, props.record] as const,
  ([show, isUpdate, record]) => {
    if (!show)
      return
    teamId.value = isUpdate && record ? record.id : ''
    formModel.value = {
      teamName: record?.teamName ?? '',
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
    const result = props.isUpdate
      ? await fetchTeamUpdate({ id: teamId.value, ...formModel.value })
      : await fetchTeamCreate(formModel.value)
    if (!result.isSuccess)
      throwApiFailure(result, t('sys.api.apiRequestFailed'))
    closeModal()
    emit('success', props.isUpdate)
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
    :title="title"
    :style="{ width: '600px' }"
    @update:show="emit('update:show', $event)"
  >
    <n-form ref="formRef" :model="formModel" :rules="rules" label-placement="top">
      <n-form-item :label="t('system.team.table.teamName')" path="teamName">
        <n-input
          v-model:value="formModel.teamName"
          :disabled="isUpdate"
          :placeholder="t('system.team.table.teamNamePlaceholder')"
        />
      </n-form-item>
      <n-form-item :label="t('common.description')" path="description">
        <n-input v-model:value="formModel.description" type="textarea" :rows="4" />
      </n-form-item>
    </n-form>
    <template #footer>
      <n-space justify="end">
        <n-button class="e2e-team-cancel-btn" @click="closeModal">
          {{ t('common.cancelText') }}
        </n-button>
        <n-button
          class="e2e-team-submit-btn"
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
