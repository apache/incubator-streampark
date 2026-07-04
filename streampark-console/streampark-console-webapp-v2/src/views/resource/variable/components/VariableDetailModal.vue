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
import { ionIconComponent } from '@/utils/ionIcon'
import type { VariableListRecord } from '@/types/api/resource/variable/model/variableModel'
import { usePermission } from '@/hooks'
import { fetchVariableInfo } from '@/service'
const props = defineProps<{
  show: boolean
  record: VariableListRecord | null
}>()

const emit = defineEmits<{ 'update:show': [value: boolean] }>()

const { t } = useI18n()
const { hasPermission } = usePermission()
const showOriginal = ref(false)
const realValue = ref('')
const displayValue = ref('')

watch(
  () => [props.show, props.record] as const,
  ([show, record]) => {
    if (!show || !record)
      return
    showOriginal.value = false
    realValue.value = ''
    displayValue.value = record.variableValue ?? ''
  },
)

async function toggleValue() {
  if (!props.record?.desensitization || !hasPermission('variable:show_original'))
    return
  if (!realValue.value) {
    const result = await fetchVariableInfo({ id: String(props.record.id) })
    if (result.isSuccess && result.data)
      realValue.value = result.data.variableValue
  }
  showOriginal.value = !showOriginal.value
  displayValue.value = showOriginal.value ? realValue.value : (props.record?.variableValue ?? '')
}
</script>

<template>
  <n-modal
    :show="show"
    preset="card"
    :title="t('flink.variable.variableInfoTitle')"
    :style="{ width: '600px' }"
    @update:show="emit('update:show', $event)"
  >
    <n-descriptions v-if="record" :column="1" bordered label-placement="left">
      <n-descriptions-item :label="t('flink.variable.table.variableCode')">
        {{ record.variableCode }}
      </n-descriptions-item>
      <n-descriptions-item :label="t('flink.variable.table.variableValue')">
        <n-space align="center">
          <span>{{ displayValue }}</span>
          <n-button
            v-if="record.desensitization && hasPermission('variable:show_original')"
            quaternary
            size="tiny"
            @click="toggleValue"
          >
            <template #icon>
              <n-icon><component :is="showOriginal ? ionIconComponent('EyeOffOutline') : ionIconComponent('EyeOutline')" /></n-icon>
            </template>
          </n-button>
        </n-space>
      </n-descriptions-item>
      <n-descriptions-item :label="t('flink.variable.table.createUser')">
        {{ record.creatorName || '-' }}
      </n-descriptions-item>
      <n-descriptions-item :label="t('flink.variable.table.createTime')">
        {{ record.createTime || '-' }}
      </n-descriptions-item>
      <n-descriptions-item :label="t('flink.variable.table.modifyTime')">
        {{ record.modifyTime || '-' }}
      </n-descriptions-item>
      <n-descriptions-item :label="t('flink.variable.table.description')">
        {{ record.description || '-' }}
      </n-descriptions-item>
    </n-descriptions>
  </n-modal>
</template>
