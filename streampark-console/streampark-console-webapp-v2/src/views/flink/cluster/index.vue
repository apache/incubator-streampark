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
import type { VNode } from 'vue'
import type { DataTableColumns } from 'naive-ui'
import type { FlinkCluster } from '@/types/api/flink/flinkCluster.type'
import { usePermission } from '@/hooks'
import {
  fetchClusterRemove,
  fetchClusterShutdown,
  fetchClusterStart,
  fetchFlinkClusterPage,
} from '@/service'
import { resolveListData } from '@/views/system/shared/utils'
import { toTagColor } from '@/utils/tagColor'
import { ClusterStateEnum, DeployMode } from '@/enums/flinkEnum'
import { useTimeoutFn } from '@vueuse/core'
defineOptions({ name: 'FlinkCluster' })

const { t } = useI18n()
const { hasPermission } = usePermission()
const router = useRouter()

const loading = ref(false)
const tableData = ref<FlinkCluster[]>([])
const searchClusterName = ref('')
const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 50, 80, 100],
  onChange: (page: number) => {
    pagination.page = page
    loadData()
  },
  onUpdatePageSize: (pageSize: number) => {
    pagination.pageSize = pageSize
    pagination.page = 1
    loadData()
  },
})

const deployModeMap: Record<number, { color: string, text: string }> = {
  [DeployMode.STANDALONE]: { color: '#2db7f5', text: 'standalone' },
  [DeployMode.YARN_SESSION]: { color: '#87d068', text: 'yarn session' },
  [DeployMode.KUBERNETES_SESSION]: { color: '#108ee9', text: 'k8s session' },
}

const clusterStateMap: Record<number, { color: string, title: string }> = {
  [ClusterStateEnum.CREATED]: { color: '#2f54eb', title: 'CREATED' },
  [ClusterStateEnum.STARTING]: { color: '#1AB58E', title: 'STARTING' },
  [ClusterStateEnum.RUNNING]: { color: '#52c41a', title: 'RUNNING' },
  [ClusterStateEnum.FAILED]: { color: '#f5222d', title: 'FAILED' },
  [ClusterStateEnum.CANCELLING]: { color: '#faad14', title: 'CANCELLING' },
  [ClusterStateEnum.CANCELED]: { color: '#fa8c16', title: 'CANCELED' },
  [ClusterStateEnum.KILLED]: { color: '#fa8c16', title: 'KILLED' },
  [ClusterStateEnum.LOST]: { color: '#99A3A4', title: 'LOST' },
  [ClusterStateEnum.UNKNOWN]: { color: '#000000', title: 'UNKNOWN' },
}

function isRunning(row: FlinkCluster) {
  return row.clusterState === ClusterStateEnum.RUNNING
}

function renderClusterState(state: number) {
  const meta = clusterStateMap[state] ?? { color: '#d9d9d9', title: 'UNKNOWN' }
  return h(NTag, { color: toTagColor(meta.color), size: 'small' }, { default: () => meta.title })
}

function renderDeployMode(mode: number) {
  const meta = deployModeMap[mode]
  if (!meta)
    return String(mode)
  return h(NTag, { color: toTagColor(meta.color), size: 'small' }, { default: () => meta.text })
}

function renderAddress(row: FlinkCluster) {
  if (row.deployMode === DeployMode.STANDALONE || row.deployMode === DeployMode.YARN_SESSION) {
    return h('a', {
      href: `/proxy/flink_cluster/${row.id}/`,
      target: '_blank',
    }, row.address)
  }
  return '-'
}

const columns = computed<DataTableColumns<FlinkCluster>>(() => [
  { title: t('setting.flinkCluster.form.clusterName'), key: 'clusterName' },
  {
    title: t('setting.flinkCluster.form.deployMode'),
    key: 'deployMode',
    render: row => renderDeployMode(row.deployMode),
  },
  {
    title: t('setting.flinkCluster.form.address'),
    key: 'address',
    render: row => renderAddress(row),
  },
  {
    title: t('setting.flinkCluster.form.runState'),
    key: 'clusterState',
    render: row => renderClusterState(row.clusterState),
  },
  {
    title: t('setting.flinkHome.description'),
    key: 'description',
    ellipsis: { tooltip: true },
  },
  {
    title: t('component.table.operation'),
    key: 'action',
    width: 200,
    render(row) {
      const actions: VNode[] = []
      if (hasPermission('cluster:update')) {
        actions.push(actionBtn(
          I.edit,
          t('setting.flinkCluster.edit'),
          () => router.push(`/flink/edit_cluster?clusterId=${row.id}`),
          'e2e-flinkcluster-edit-btn',
          isRunning(row),
        ))
      }
      if (hasPermission('cluster:create')) {
        if (isRunning(row)) {
          actions.push(actionBtn(
          I.pause,
            t('setting.flinkCluster.stop'),
            () => handleShutdown(row),
            'e2e-flinkcluster-shutdown-btn',
            row.deployMode === DeployMode.STANDALONE,
          ))
        }
        else {
          actions.push(actionBtn(
          I.start,
            t('setting.flinkCluster.start'),
            () => handleStart(row),
            'e2e-flinkcluster-start-btn',
            row.deployMode === DeployMode.STANDALONE,
          ))
        }
      }
      if (hasPermission('app:detail')) {
        actions.push(actionBtn(
          I.view,
          t('setting.flinkCluster.detail'),
          () => window.open(`/proxy/flink_cluster/${row.id}/`, '_blank'),
          undefined,
          !isRunning(row),
        ))
      }
      actions.push(h(NPopconfirm, { onPositiveClick: () => handleDelete(row) }, {
        trigger: () => actionBtn(I.delete, t('common.delText'), undefined, 'e2e-flinkcluster-delete-btn'),
        default: () => t('setting.flinkCluster.delete'),
      }))
      return h(NSpace, { size: 4 }, { default: () => actions })
    },
  },
])

function actionBtn(
  icon: string,
  tip: string,
  onClick?: () => void,
  cls?: string,
  disabled = false,
) {
  return h(NTooltip, { trigger: 'hover' }, {
    trigger: () => h(NButton, {
      quaternary: true,
      size: 'small',
      class: cls,
      disabled,
      onClick,
    }, { icon: () => ionIcon(icon) }),
    default: () => tip,
  })
}

async function loadData(silent = false) {
  if (!silent)
    loading.value = true
  try {
    const result = await fetchFlinkClusterPage({
      pageNum: pagination.page,
      pageSize: pagination.pageSize,
      clusterName: searchClusterName.value || undefined,
    } as any)
    if (!result.isSuccess)
      throwApiFailure(result, t('sys.api.apiRequestFailed'))
    const { records, total } = resolveListData(result.data)
    tableData.value = records
    pagination.itemCount = total
  }
  catch (e: any) {
    if (!silent)
      showCatchError(e, t('sys.api.apiRequestFailed'))
    if (!silent) {
      tableData.value = []
      pagination.itemCount = 0
    }
  }
  finally {
    if (!silent)
      loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  loadData()
}

async function handleStart(row: FlinkCluster) {
  const msg = window.$message?.loading(t('setting.flinkCluster.operateMessage.flinkClusterIsStarting'), { duration: 0 })
  try {
    const result = await fetchClusterStart(row.id)
    if (!result.isSuccess)
      throwApiFailure(result, t('sys.api.apiRequestFailed'))
    window.$message?.success(t('setting.flinkCluster.operateMessage.flinkClusterHasStartedSuccessful'))
    loadData(true)
  }
  catch (e: any) {
    showCatchError(e, t('sys.api.apiRequestFailed'))
  }
  finally {
    msg?.destroy()
  }
}

async function handleShutdown(row: FlinkCluster) {
  const msg = window.$message?.loading(t('setting.flinkCluster.operateMessage.flinkClusterIsCanceling'), { duration: 0 })
  try {
    const result = await fetchClusterShutdown(row.id)
    if (!result.isSuccess)
      throwApiFailure(result, t('sys.api.apiRequestFailed'))
    window.$message?.success(t('setting.flinkCluster.operateMessage.flinkClusterIsShutdown'))
    loadData(true)
  }
  catch (e: any) {
    showCatchError(e, t('sys.api.apiRequestFailed'))
  }
  finally {
    msg?.destroy()
  }
}

async function handleDelete(row: FlinkCluster) {
  try {
    const result = await fetchClusterRemove(row.id)
    if (!result.isSuccess)
      throwApiFailure(result, t('sys.api.apiRequestFailed'))
    window.$message?.success(t('setting.flinkCluster.operateMessage.flinkClusterIsRemoved'))
    loadData()
  }
  catch (e: any) {
    showCatchError(e, t('sys.api.apiRequestFailed'))
  }
}

const { start: startPolling, stop: stopPolling } = useTimeoutFn(() => {
  if (!loading.value)
    loadData(true)
  startPolling()
}, 5000)

onMounted(() => {
  loadData()
  startPolling()
})

onUnmounted(() => {
  stopPolling()
})
</script>

<template>
  <n-card :bordered="false" class="h-full">
    <div class="mb-16px flex flex-wrap items-center justify-between gap-12px">
      <n-input
        v-model:value="searchClusterName"
        clearable
        :placeholder="t('setting.flinkCluster.searchByName')"
        class="max-w-280px"
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      />
      <n-space>
        <n-button type="primary" ghost @click="handleSearch">
          {{ t('common.queryText') }}
        </n-button>
        <n-button id="e2e-flinkcluster-create-btn" v-auth="'cluster:create'" type="primary" @click="router.push('/flink/add_cluster')">
          {{ t('common.add') }}
        </n-button>
      </n-space>
    </div>
    <n-data-table
      remote
      :loading="loading"
      :columns="columns"
      :data="tableData"
      :pagination="pagination"
      :row-key="(row: FlinkCluster) => row.id"
      flex-height
      class="min-h-480px"
    />
  </n-card>
</template>
