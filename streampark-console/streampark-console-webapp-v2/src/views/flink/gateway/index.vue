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
import { ionIcon } from '@/utils/ionIcon'
import { SP_ICONS as I } from '@/constants/streamparkIcons'
import type { DataTableColumns } from 'naive-ui'
import type { FlinkGatewayRecord } from '@/service/api/flink/gateway'
import { usePermission } from '@/hooks'
import { fetchGatewayDelete, fetchGatewayList } from '@/service'
import GatewayDrawer from './components/GatewayDrawer.vue'
defineOptions({ name: 'FlinkGateway' })

const { t } = useI18n()
const { hasPermission } = usePermission()

const loading = ref(false)
const tableData = ref<FlinkGatewayRecord[]>([])
const drawerVisible = ref(false)

const columns = computed<DataTableColumns<FlinkGatewayRecord>>(() => [
  { title: t('setting.flinkGateway.name'), key: 'gatewayName', sorter: true },
  { title: t('setting.flinkGateway.gatewayType'), key: 'gatewayType', sorter: true },
  { title: t('setting.flinkGateway.gatewayAddress'), key: 'address', sorter: true },
  { title: t('common.description'), key: 'description', ellipsis: { tooltip: true }, width: 350 },
  { title: t('common.createTime'), key: 'createTime', sorter: true },
  { title: t('common.modifyTime'), key: 'modifyTime', sorter: true },
  {
    title: t('component.table.operation'),
    key: 'action',
    width: 120,
    render(row) {
      if (!hasPermission('gateway:delete'))
        return null
      return h(NPopconfirm, { onPositiveClick: () => handleDelete(row) }, {
        trigger: () => h(NButton, { quaternary: true, size: 'small' }, {
          icon: () => ionIcon(I.delete),
        }),
        default: () => t('setting.flinkGateway.operation.deleteConfirm'),
      })
    },
  },
])

async function loadData() {
  loading.value = true
  try {
    const result = await fetchGatewayList()
    if (!result.isSuccess)
      throwApiFailure(result, t('sys.api.apiRequestFailed'))
    tableData.value = result.data ?? []
  }
  catch (e: any) {
    showCatchError(e, t('sys.api.apiRequestFailed'))
    tableData.value = []
  }
  finally {
    loading.value = false
  }
}

function openCreate() {
  drawerVisible.value = true
}

async function handleDelete(record: FlinkGatewayRecord) {
  const result = await fetchGatewayDelete({ id: record.id })
  if (result.isSuccess) {
    window.$message?.success(t('common.operationSuccess'))
    loadData()
  }
  else {
    showResultError(result, t('sys.api.apiRequestFailed'))
  }
}

function handleDrawerSuccess() {
  window.$message?.success(t('common.operationSuccess'))
  loadData()
}

onMounted(loadData)
</script>

<template>
  <n-card :bordered="false" class="h-full">
    <div class="mb-16px flex items-center justify-between gap-12px">
      <n-text strong>
        {{ t('setting.flinkGateway.tableTitle') }}
      </n-text>
      <n-button v-auth="'gateway:add'" type="primary" @click="openCreate">
        {{ t('common.add') }}
      </n-button>
    </div>
    <n-data-table
      :loading="loading"
      :columns="columns"
      :data="tableData"
      :row-key="(row: FlinkGatewayRecord) => row.id"
      flex-height
      class="min-h-480px"
    />
  </n-card>
  <GatewayDrawer v-model:show="drawerVisible" @success="handleDrawerSuccess" />
</template>
