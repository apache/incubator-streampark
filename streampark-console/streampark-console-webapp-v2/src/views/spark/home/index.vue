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
import type { SparkEnv } from '@/types/api/spark/home.type'
import {
  fetchSparkEnvList,
  fetchSparkEnvRemove,
  fetchSparkSetDefault,
} from '@/service'
import { usePermission } from '@/hooks'
import { SvgIcon } from '@/components/Icon'
import SparkEnvDrawer from './components/SparkEnvDrawer.vue'
import SparkEnvModal from './components/SparkEnvModal.vue'

defineOptions({ name: 'SparkHome' })

const { t } = useI18n()
const { hasPermission } = usePermission()

const loading = ref(false)
const allData = ref<SparkEnv[]>([])
const searchSparkName = ref('')

const modalVisible = ref(false)
const versionId = ref<string | null>(null)
const modalInitialData = ref<{
  sparkName?: string
  sparkHome?: string
  description?: string | null
} | null>(null)

const drawerVisible = ref(false)
const drawerEnvId = ref<string | null>(null)

const filteredData = computed(() => {
  const keyword = searchSparkName.value.trim().toLowerCase()
  if (!keyword)
    return allData.value
  return allData.value.filter(item => item.sparkName.toLowerCase().includes(keyword))
})

const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 50, 80, 100],
  onChange: (page: number) => {
    pagination.page = page
  },
  onUpdatePageSize: (pageSize: number) => {
    pagination.pageSize = pageSize
    pagination.page = 1
  },
})

watch(filteredData, (rows) => {
  pagination.itemCount = rows.length
})

const columns = computed<DataTableColumns<SparkEnv>>(() => [
  {
    title: t('spark.home.form.sparkName'),
    key: 'sparkName',
    render(row) {
      return h('div', { class: 'flex items-center gap-8px' }, [
        h(SvgIcon, { name: 'spark', size: 20 }),
        h('span', null, row.sparkName),
      ])
    },
  },
  { title: t('spark.home.form.sparkHome'), key: 'sparkHome', ellipsis: { tooltip: true } },
  { title: t('spark.home.sparkVersion'), key: 'version' },
  {
    title: t('spark.home.defaultLabel'),
    key: 'isDefault',
    width: 100,
    render(row) {
      return h(NSwitch, {
        value: row.isDefault,
        disabled: row.isDefault,
        onUpdateValue: (value: boolean) => handleSetDefault(row, value),
      }, {
        checked: () => ionIcon('CheckmarkOutline'),
        unchecked: () => ionIcon('CloseOutline'),
      })
    },
  },
  {
    title: t('spark.home.form.description'),
    key: 'description',
    ellipsis: { tooltip: true },
    width: 280,
  },
  {
    title: t('component.table.operation'),
    key: 'action',
    width: 200,
    render(row) {
      const actions: VNode[] = []
      if (hasPermission('project:build')) {
        actions.push(h(NTooltip, { trigger: 'hover' }, {
          trigger: () => h(NButton, {
            quaternary: true,
            size: 'small',
            onClick: () => handleEdit(row),
          }, { icon: () => ionIcon(I.edit) }),
          default: () => t('spark.home.edit'),
        }))
      }
      actions.push(h(NTooltip, { trigger: 'hover' }, {
        trigger: () => h(NButton, {
          quaternary: true,
          size: 'small',
          onClick: () => openConfigDrawer(row),
        }, { icon: () => ionIcon(I.view) }),
        default: () => t('spark.home.conf'),
      }))
      const deleteDisabled = row.isDefault && allData.value.length > 1
      actions.push(h(NPopconfirm, {
        disabled: deleteDisabled,
        onPositiveClick: () => handleDelete(row),
      }, {
        trigger: () => h(NTooltip, { trigger: 'hover' }, {
          trigger: () => h(NButton, {
            quaternary: true,
            size: 'small',
            disabled: deleteDisabled,
          }, { icon: () => ionIcon(I.delete) }),
          default: () => t('common.delText'),
        }),
        default: () => t('spark.home.delete'),
      }))
      return h(NSpace, { size: 4 }, { default: () => actions })
    },
  },
])

async function loadData() {
  loading.value = true
  try {
    const result = await fetchSparkEnvList()
    if (!result.isSuccess)
      throwApiFailure(result, t('sys.api.apiRequestFailed'))
    allData.value = result.data ?? []
    pagination.itemCount = filteredData.value.length
  }
  catch (e: any) {
    showCatchError(e, t('sys.api.apiRequestFailed'))
    allData.value = []
    pagination.itemCount = 0
  }
  finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  pagination.itemCount = filteredData.value.length
}

function openCreate() {
  versionId.value = null
  modalInitialData.value = null
  modalVisible.value = true
}

function handleEdit(record: SparkEnv) {
  versionId.value = record.id
  modalInitialData.value = {
    sparkName: record.sparkName,
    sparkHome: record.sparkHome,
    description: record.description || null,
  }
  modalVisible.value = true
}

function openConfigDrawer(record: SparkEnv) {
  drawerEnvId.value = record.id
  drawerVisible.value = true
}

async function handleDelete(record: SparkEnv) {
  const result = await fetchSparkEnvRemove(record.id)
  if (result.isSuccess && result.data) {
    window.$message?.success(t('spark.home.tips.remove'))
    loadData()
  }
  else {
    showResultError(result, t('sys.api.apiRequestFailed'))
  }
}

async function handleSetDefault(record: SparkEnv, value: boolean) {
  if (!value || record.isDefault)
    return
  try {
    const result = await fetchSparkSetDefault(record.id)
    if (!result.isSuccess)
      throwApiFailure(result, t('sys.api.apiRequestFailed'))
    window.$message?.success(record.sparkName.concat(t('spark.home.tips.setDefault')))
    loadData()
  }
  catch (e: any) {
    showCatchError(e, t('sys.api.apiRequestFailed'))
    loadData()
  }
}

function handleModalSuccess(message: string) {
  window.$message?.success(message)
  loadData()
}

onMounted(loadData)
</script>

<template>
  <n-card :bordered="false" class="h-full">
    <div class="mb-16px flex flex-wrap items-center justify-between gap-12px">
      <n-input
        v-model:value="searchSparkName"
        clearable
        :placeholder="t('spark.home.searchByName')"
        class="max-w-280px"
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      />
      <n-space>
        <n-button type="primary" ghost @click="handleSearch">
          {{ t('common.queryText') }}
        </n-button>
        <n-button v-auth="'project:create'" type="primary" @click="openCreate">
          {{ t('common.add') }}
        </n-button>
      </n-space>
    </div>
    <n-data-table
      :loading="loading"
      :columns="columns"
      :data="filteredData"
      :pagination="pagination"
      :row-key="(row: SparkEnv) => row.id"
      flex-height
      class="min-h-480px"
    />
  </n-card>
  <SparkEnvModal
    v-model:show="modalVisible"
    :version-id="versionId"
    :initial-data="modalInitialData"
    @success="handleModalSuccess"
  />
  <SparkEnvDrawer
    v-model:show="drawerVisible"
    :env-id="drawerEnvId"
  />
</template>
