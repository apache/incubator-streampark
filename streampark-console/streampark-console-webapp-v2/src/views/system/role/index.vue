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
import type { RoleListRecord } from '@/types/api/system/model/roleModel'
import { usePermission } from '@/hooks'
import { fetchRoleDelete, fetchRoleListByPage } from '@/service'
import { useUserStoreWithOut } from '@/store/modules/user'
import RoleDetailDrawer from './components/RoleDetailDrawer.vue'
import RoleDrawer from './components/RoleDrawer.vue'
import { FormTypeEnum } from '../shared/constants'
import { resolveListData } from '../shared/utils'
defineOptions({ name: 'RoleManagement' })

const { t } = useI18n()
const { hasPermission } = usePermission()
const userStore = useUserStoreWithOut()
const currentUsername = computed(() => userStore.getUserInfo?.username)

const loading = ref(false)
const tableData = ref<RoleListRecord[]>([])
const searchRoleName = ref('')
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

const drawerVisible = ref(false)
const drawerFormType = ref(FormTypeEnum.Create)
const editingRecord = ref<RoleListRecord | null>(null)
const detailVisible = ref(false)
const detailRecord = ref<RoleListRecord | null>(null)

const columns = computed<DataTableColumns<RoleListRecord>>(() => [
  { title: t('system.role.form.roleName'), key: 'roleName' },
  { title: t('common.createTime'), key: 'createTime', sorter: true },
  { title: t('common.modifyTime'), key: 'modifyTime', sorter: true },
  {
    title: t('common.description'),
    key: 'description',
    ellipsis: { tooltip: true },
  },
  {
    title: t('component.table.operation'),
    key: 'action',
    width: 200,
    render(row) {
      const actions: VNode[] = []
      if (hasPermission('role:update') && (row.roleName !== 'admin' || currentUsername.value === 'admin')) {
        actions.push(h(
          NTooltip,
          { trigger: 'hover' },
          {
            trigger: () => h(NButton, {
              quaternary: true,
              size: 'small',
              class: 'e2e-role-edit-btn',
              onClick: () => openEdit(row),
            }, { icon: () => ionIcon(I.edit) }),
            default: () => t('system.role.form.edit'),
          },
        ))
      }
      actions.push(h(
        NTooltip,
        { trigger: 'hover' },
        {
          trigger: () => h(NButton, {
            quaternary: true,
            size: 'small',
            onClick: () => openDetail(row),
          }, { icon: () => ionIcon(I.detail) }),
          default: () => t('common.detail'),
        },
      ))
      if (hasPermission('role:delete') && row.roleName !== 'admin') {
        actions.push(h(
          NPopconfirm,
          { onPositiveClick: () => handleDelete(row) },
          {
            trigger: () => h(
              NTooltip,
              { trigger: 'hover' },
              {
                trigger: () => h(NButton, {
                  quaternary: true,
                  size: 'small',
                  class: 'e2e-role-delete-btn',
                }, { icon: () => ionIcon(I.delete) }),
                default: () => t('system.role.form.delete'),
              },
            ),
            default: () => t('system.role.deleteTip'),
          },
        ))
      }
      return h(NSpace, { size: 4 }, { default: () => actions })
    },
  },
])

async function loadData() {
  loading.value = true
  try {
    const result = await fetchRoleListByPage({
      pageNum: pagination.page,
      pageSize: pagination.pageSize,
      roleName: searchRoleName.value || undefined,
    } as any)
    if (!result.isSuccess)
      throwApiFailure(result, t('sys.api.apiRequestFailed'))
    const { records, total } = resolveListData(result.data as any)
    tableData.value = records as RoleListRecord[]
    pagination.itemCount = total
  }
  catch (e: any) {
    showCatchError(e, t('sys.api.apiRequestFailed'))
    tableData.value = []
    pagination.itemCount = 0
  }
  finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  loadData()
}

function openCreate() {
  drawerFormType.value = FormTypeEnum.Create
  editingRecord.value = null
  drawerVisible.value = true
}

function openEdit(record: RoleListRecord) {
  drawerFormType.value = FormTypeEnum.Edit
  editingRecord.value = record
  drawerVisible.value = true
}

function openDetail(record: RoleListRecord) {
  detailRecord.value = record
  detailVisible.value = true
}

async function handleDelete(record: RoleListRecord) {
  try {
    const result = await fetchRoleDelete({ roleId: record.roleId })
    if (!result.isSuccess)
      throwApiFailure(result, t('sys.api.apiRequestFailed'))
    window.$message?.success(t('common.operationSuccess'))
    loadData()
  }
  catch (e: any) {
    showCatchError(e, t('sys.api.apiRequestFailed'))
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
    <div class="mb-16px flex flex-wrap items-center justify-between gap-12px">
      <n-input
        v-model:value="searchRoleName"
        clearable
        :placeholder="t('system.role.searchByRole')"
        class="max-w-280px"
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      />
      <n-space>
        <n-button type="primary" ghost @click="handleSearch">
          {{ t('common.queryText') }}
        </n-button>
        <n-button
          id="e2e-role-create-btn"
          v-auth="'role:add'"
          type="primary"
          @click="openCreate"
        >
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
      :row-key="(row: RoleListRecord) => row.roleId"
      flex-height
      class="min-h-480px"
    />
  </n-card>
  <RoleDrawer
    v-model:show="drawerVisible"
    :form-type="drawerFormType"
    :record="editingRecord"
    @success="handleDrawerSuccess"
  />
  <RoleDetailDrawer
    v-model:show="detailVisible"
    :record="detailRecord"
  />
</template>
