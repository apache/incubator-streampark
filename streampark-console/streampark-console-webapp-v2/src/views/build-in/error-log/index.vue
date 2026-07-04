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
import type { ErrorLogInfo } from '/#/store'
import type { DataTableColumns } from 'naive-ui'
import { fireErrorApi } from '@/service'
import { ErrorTypeEnum } from '@/enums/exceptionEnum'
import { useErrorLogStore } from '@/store/modules/errorLog'
import ErrorLogDetailModal from './components/ErrorLogDetailModal.vue'

defineOptions({ name: 'ErrorLog' })

const { t } = useI18n()
const errorLogStore = useErrorLogStore()

const tableData = ref<ErrorLogInfo[]>([])
const detailVisible = ref(false)
const detailRecord = ref<ErrorLogInfo | null>(null)

const typeColorMap: Record<string, 'success' | 'info' | 'warning' | 'error' | 'default'> = {
  [ErrorTypeEnum.VUE]: 'success',
  [ErrorTypeEnum.RESOURCE]: 'info',
  [ErrorTypeEnum.PROMISE]: 'warning',
  [ErrorTypeEnum.AJAX]: 'error',
}

const columns = computed<DataTableColumns<ErrorLogInfo>>(() => [
  {
    title: t('sys.errorLog.tableColumnType'),
    key: 'type',
    width: 100,
    render(row) {
      return h(NTag, { size: 'small', type: typeColorMap[row.type] ?? 'default' }, { default: () => row.type })
    },
  },
  { title: 'URL', key: 'url', ellipsis: { tooltip: true }, width: 180 },
  { title: t('sys.errorLog.tableColumnDate'), key: 'time', width: 170 },
  { title: t('sys.errorLog.tableColumnFile'), key: 'file', ellipsis: { tooltip: true }, width: 180 },
  { title: 'Name', key: 'name', ellipsis: { tooltip: true }, width: 160 },
  { title: t('sys.errorLog.tableColumnMsg'), key: 'message', ellipsis: { tooltip: true } },
  {
    title: t('component.table.operation'),
    key: 'action',
    width: 100,
    render(row) {
      return h(NButton, { text: true, type: 'primary', onClick: () => openDetail(row) }, {
        default: () => t('sys.errorLog.tableActionDesc'),
      })
    },
  },
])

watch(
  () => errorLogStore.getErrorLogInfoList,
  (list) => {
    tableData.value = list ? [...list] : []
  },
  { immediate: true, deep: true },
)

if (import.meta.env.DEV)
  window.$message?.info(t('sys.errorLog.enableMessage'))

function openDetail(row: ErrorLogInfo) {
  detailRecord.value = row
  detailVisible.value = true
}

function fireVueError() {
  throw new Error('fire vue error!')
}

function fireResourceError() {
  const img = new Image()
  img.src = `${Date.now()}.png`
}

async function fireAjaxError() {
  await fireErrorApi()
}
</script>

<template>
  <n-card :bordered="false" :title="t('sys.errorLog.tableTitle')">
    <template #header-extra>
      <n-space>
        <n-button type="primary" @click="fireVueError">
          {{ t('sys.errorLog.fireVueError') }}
        </n-button>
        <n-button type="primary" @click="fireResourceError">
          {{ t('sys.errorLog.fireResourceError') }}
        </n-button>
        <n-button type="primary" @click="fireAjaxError">
          {{ t('sys.errorLog.fireAjaxError') }}
        </n-button>
      </n-space>
    </template>
    <n-data-table
      :columns="columns"
      :data="tableData"
      :row-key="(row: ErrorLogInfo) => String(row.time)"
      flex-height
      class="min-h-480px"
    />
    <ErrorLogDetailModal v-model:show="detailVisible" :record="detailRecord" />
  </n-card>
</template>
