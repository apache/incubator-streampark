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
import type { DashboardResponse } from '@/types/api/flink/app.type'
import { fetchDashboard } from '@/service'

defineOptions({ name: 'AppDashboard' })

const { t } = useI18n()
const loading = ref(false)

const stats = ref<Array<{
  title: string
  value: string | number
  footer: Array<{ title: string, value: string | number }>
}>>([])

async function loadDashboard(showLoading = true) {
  if (showLoading)
    loading.value = true
  try {
    const result = await fetchDashboard()
    if (!result.isSuccess)
      throwApiFailure(result, t('sys.api.apiRequestFailed'))
    const res = result.data as DashboardResponse
    stats.value = [
      {
        title: t('flink.app.dashboard.runningJobs'),
        value: res.runningJob,
        footer: [
          { title: t('flink.app.dashboard.totalTask'), value: res.task.total },
          { title: t('flink.app.dashboard.runningTask'), value: res.task.running },
        ],
      },
      {
        title: t('flink.app.dashboard.availableTaskSlots'),
        value: res.availableSlot,
        footer: [
          { title: t('flink.app.dashboard.taskSlots'), value: res.totalSlot },
          { title: t('flink.app.dashboard.taskManagers'), value: res.totalTM },
        ],
      },
      {
        title: t('flink.app.dashboard.jobManagerMemory'),
        value: `${res.jmMemory} MB`,
        footer: [
          { title: t('flink.app.dashboard.totalJobManagerMemory'), value: `${res.jmMemory} MB` },
        ],
      },
      {
        title: t('flink.app.dashboard.taskManagerMemory'),
        value: `${res.tmMemory} MB`,
        footer: [
          { title: t('flink.app.dashboard.totalTaskManagerMemory'), value: `${res.tmMemory} MB` },
        ],
      },
    ]
  }
  catch (e: any) {
    if (showLoading)
      showCatchError(e, t('sys.api.apiRequestFailed'))
  }
  finally {
    if (showLoading)
      loading.value = false
  }
}

onMounted(() => loadDashboard(true))

defineExpose({ loadDashboard })
</script>

<template>
  <n-grid :x-gap="16" :y-gap="16" class="mb-16px">
    <n-gi v-for="(item, index) in stats" :key="index" :span="6" :xs="24" :md="6">
      <n-card :bordered="false" size="small">
        <n-spin :show="loading">
          <n-statistic :label="item.title" :value="item.value" />
          <div class="mt-8px flex flex-wrap gap-12px text-12px text-gray-500">
            <span v-for="(footer, fi) in item.footer" :key="fi">
              {{ footer.title }}: {{ footer.value }}
            </span>
          </div>
        </n-spin>
      </n-card>
    </n-gi>
  </n-grid>
</template>
