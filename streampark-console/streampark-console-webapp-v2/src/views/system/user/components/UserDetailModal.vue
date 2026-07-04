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
import type { UserListRecord } from '@/types/api/system/model/userModel'
import { GenderEnum, StatusEnum } from '../../shared/constants'

defineProps<{
  show: boolean
  record: UserListRecord | null
}>()

const emit = defineEmits<{
  'update:show': [value: boolean]
}>()

const { t } = useI18n()

function genderLabel(value?: string) {
  const map: Record<string, string> = {
    [GenderEnum.Male]: t('system.user.male'),
    [GenderEnum.Female]: t('system.user.female'),
    [GenderEnum.Other]: t('system.user.secret'),
  }
  return value ? map[value] ?? value : '-'
}

function statusTagType(status?: string) {
  return status === StatusEnum.Effective ? 'success' : 'error'
}

function statusLabel(status?: string) {
  return status === StatusEnum.Effective ? t('system.user.effective') : t('system.user.locked')
}
</script>

<template>
  <n-modal
    :show="show"
    preset="card"
    :title="t('system.user.userInfo')"
    :style="{ width: '600px' }"
    @update:show="emit('update:show', $event)"
  >
    <n-descriptions v-if="record" :column="1" label-placement="left" bordered>
      <n-descriptions-item :label="t('system.user.form.userName')">
        {{ record.username }}
      </n-descriptions-item>
      <n-descriptions-item :label="t('system.user.form.userType')">
        {{ record.userType }}
      </n-descriptions-item>
      <n-descriptions-item :label="t('system.user.form.gender')">
        {{ genderLabel(record.sex) }}
      </n-descriptions-item>
      <n-descriptions-item label="E-Mail">
        {{ record.email || '-' }}
      </n-descriptions-item>
      <n-descriptions-item :label="t('system.user.form.status')">
        <n-tag :type="statusTagType(record.status)" size="small">
          {{ statusLabel(record.status) }}
        </n-tag>
      </n-descriptions-item>
      <n-descriptions-item :label="t('common.createTime')">
        {{ record.createTime || '-' }}
      </n-descriptions-item>
      <n-descriptions-item :label="t('system.user.form.lastLoginTime')">
        {{ record.lastLoginTime || '-' }}
      </n-descriptions-item>
      <n-descriptions-item :label="t('common.description')">
        {{ record.description || '-' }}
      </n-descriptions-item>
    </n-descriptions>
  </n-modal>
</template>
