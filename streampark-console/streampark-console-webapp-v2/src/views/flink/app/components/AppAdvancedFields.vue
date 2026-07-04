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
import { fetchAlertSetting } from '@/service'
import optionData from '@/views/flink/app/shared/data/option'
import { cpTriggerAction, resolveOrder } from '@/views/flink/app/shared/data/index'
import { isK8sDeployMode } from '@/views/flink/app/shared/utils'

const model = defineModel<Recordable>({ required: true })

const { t } = useI18n()

const alerts = ref<Array<{ id: string, alertName: string }>>([])

const jmMemoryOptions = optionData.filter(x => x.group === 'jobmanager-memory')
const tmMemoryOptions = optionData.filter(x => x.group === 'taskmanager-memory')
const totalMemoryOptions = optionData.filter(x => x.group === 'total-memory')

const showCheckpoint = computed(() => !isK8sDeployMode(model.value.deployMode))

function ensureCheckpointFailure() {
  if (!model.value.checkPointFailure) {
    model.value.checkPointFailure = {
      cpMaxFailureInterval: null,
      cpFailureRateInterval: null,
      cpFailureAction: null,
    }
  }
}

onMounted(async () => {
  ensureCheckpointFailure()
  const result = await fetchAlertSetting()
  if (result.isSuccess)
    alerts.value = (result.data ?? []) as Array<{ id: string, alertName: string }>
})
</script>

<template>
  <n-collapse v-if="model" class="mt-8px">
    <n-collapse-item :title="t('flink.app.dynamicOption')" name="advanced">
      <n-grid :cols="24" :x-gap="16">
        <n-form-item-gi :span="8" :label="t('flink.app.resolveOrder')">
          <n-select
            v-model:value="model.resolveOrder"
            :options="resolveOrder"
            placeholder="classloader.resolve-order"
          />
        </n-form-item-gi>
        <n-form-item-gi :span="8" :label="t('flink.app.faultAlertTemplate')">
          <n-select
            v-model:value="model.alertId"
            filterable
            clearable
            :options="alerts.map(a => ({ label: a.alertName, value: a.id }))"
            :placeholder="t('flink.app.addAppTips.alertTemplatePlaceholder')"
          />
        </n-form-item-gi>
        <n-form-item-gi :span="8" :label="t('flink.app.parallelism')">
          <n-input-number v-model:value="model.parallelism" :min="1" :style="{ width: '100%' }" />
        </n-form-item-gi>
        <n-form-item-gi :span="8" :label="t('setting.flinkCluster.form.taskSlots')">
          <n-input-number v-model:value="model.slot" :min="1" :style="{ width: '100%' }" />
        </n-form-item-gi>
        <n-form-item-gi v-if="!isK8sDeployMode(model.deployMode)" :span="8" :label="t('flink.app.restartSize')">
          <n-input-number v-model:value="model.restartSize" :min="0" :style="{ width: '100%' }" />
        </n-form-item-gi>
        <template v-if="showCheckpoint">
          <n-form-item-gi :span="8" :label="t('flink.app.checkPointFailureOptions')">
            <n-input-number
              v-model:value="model.checkPointFailure.cpMaxFailureInterval"
              :min="0"
              :style="{ width: '100%' }"
              :placeholder="t('flink.app.cpMaxFailureInterval')"
            />
          </n-form-item-gi>
          <n-form-item-gi :span="8" :label="t('flink.app.cpFailureRateInterval')">
            <n-input-number
              v-model:value="model.checkPointFailure.cpFailureRateInterval"
              :min="0"
              :style="{ width: '100%' }"
            />
          </n-form-item-gi>
          <n-form-item-gi :span="8" :label="t('flink.app.cpFailureAction')">
            <n-select
              v-model:value="model.checkPointFailure.cpFailureAction"
              clearable
              :options="cpTriggerAction"
            />
          </n-form-item-gi>
        </template>
        <n-form-item-gi :span="24" :label="t('flink.app.dynamicProperties')">
          <n-input
            v-model:value="model.dynamicProperties"
            type="textarea"
            :rows="3"
            :placeholder="t('flink.app.dynamicProperties')"
          />
        </n-form-item-gi>
        <n-form-item-gi :span="12" :label="t('flink.app.totalMemoryOptions')">
          <n-select
            v-model:value="model.totalOptions"
            multiple
            filterable
            clearable
            :options="totalMemoryOptions.map(o => ({ label: o.name, value: o.key }))"
            :placeholder="t('flink.app.addAppTips.totalMemoryOptionsPlaceholder')"
          />
        </n-form-item-gi>
        <template v-if="model.totalOptions?.length">
          <n-form-item-gi
            v-for="key in model.totalOptions"
            :key="`total-${key}`"
            :span="8"
            :label="key"
          >
            <n-input-number
              v-model:value="model.totalItem[key]"
              :style="{ width: '100%' }"
            />
          </n-form-item-gi>
        </template>
        <n-form-item-gi :span="12" :label="t('setting.flinkCluster.form.jmOptions')">
          <n-select
            v-model:value="model.jmOptions"
            multiple
            filterable
            clearable
            :options="jmMemoryOptions.map(o => ({ label: o.name, value: o.key }))"
            :placeholder="t('setting.flinkCluster.placeholder.jmOptions')"
          />
        </n-form-item-gi>
        <n-form-item-gi :span="12" :label="t('setting.flinkCluster.form.tmOptions')">
          <n-select
            v-model:value="model.tmOptions"
            multiple
            filterable
            clearable
            :options="tmMemoryOptions.map(o => ({ label: o.name, value: o.key }))"
            :placeholder="t('setting.flinkCluster.placeholder.tmOptions')"
          />
        </n-form-item-gi>
        <template v-if="model.jmOptions?.length">
          <n-form-item-gi
            v-for="key in model.jmOptions"
            :key="`jm-${key}`"
            :span="8"
            :label="key"
          >
            <n-input-number
              v-model:value="model.jmOptionsItem[key]"
              :style="{ width: '100%' }"
            />
          </n-form-item-gi>
        </template>
        <template v-if="model.tmOptions?.length">
          <n-form-item-gi
            v-for="key in model.tmOptions"
            :key="`tm-${key}`"
            :span="8"
            :label="key"
          >
            <n-input-number
              v-model:value="model.tmOptionsItem[key]"
              :style="{ width: '100%' }"
            />
          </n-form-item-gi>
        </template>
      </n-grid>
    </n-collapse-item>
  </n-collapse>
</template>
