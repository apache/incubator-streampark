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
import type { MemberListRecord } from '@/types/api/system/model/memberModel'
import type { RoleListItem } from '@/types/api/base/model/systemModel'
import { usePermission } from '@/hooks'
import { fetchMemberDelete, fetchMemberList, fetchRoleListByPage } from '@/service'
import { useUserStoreWithOut } from '@/store/modules/user'
import MemberModal from './components/MemberModal.vue'
import { resolveListData } from '../shared/utils'
defineOptions({ name: 'Member' })

const { t } = useI18n()
const { hasPermission } = usePermission()
const router = useRouter()
const userStore = useUserStoreWithOut()

const loading = ref(false)
const tableData = ref<MemberListRecord[]>([])
const searchUserName = ref<string | null>(null)
const searchRoleName = ref<string | null>(null)
const roleListOptions = ref<Array<Partial<RoleListItem>>>([])

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

const modalVisible = ref(false)
const isUpdate = ref(false)
const editingRecord = ref<MemberListRecord | null>(null)

const roleFilterOptions = computed(() =>
  roleListOptions.value.map(item => ({
    label: item.roleName ?? '',
    value: item.roleName ?? '',
  })),
)

const columns = computed<DataTableColumns<MemberListRecord>>(() => [
  { title: t('system.member.table.userName'), key: 'userName', sorter: true },
  { title: t('system.member.table.roleName'), key: 'roleName', sorter: true },
  { title: t('common.createTime'), key: 'createTime', sorter: true },
  { title: t('common.modifyTime'), key: 'modifyTime', sorter: true },
  {
    title: t('component.table.operation'),
    key: 'action',
    width: 200,
    render(row) {
      const actions: VNode[] = []
      if (hasPermission('member:update')) {
        actions.push(h(
          NTooltip,
          { trigger: 'hover' },
          {
            trigger: () => h(NButton, {
              quaternary: true,
              size: 'small',
              class: 'e2e-member-edit-btn',
              onClick: () => openEdit(row),
            }, { icon: () => ionIcon(I.edit) }),
            default: () => t('system.member.modifyMember'),
          },
        ))
      }
      if (hasPermission('member:delete')) {
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
                  class: 'e2e-member-delete-btn',
                }, { icon: () => ionIcon(I.delete) }),
                default: () => t('system.member.deleteMember'),
              },
            ),
            default: () => t('system.member.deletePopConfirm'),
          },
        ))
      }
      return h(NSpace, { size: 4 }, { default: () => actions })
    },
  },
])

async function loadRoles() {
  const result = await fetchRoleListByPage({ pageNum: 1, pageSize: 9999 } as any)
  if (result.isSuccess) {
    const { records } = resolveListData(result.data as any)
    roleListOptions.value = records as RoleListItem[]
  }
}

async function loadData() {
  loading.value = true
  try {
    const result = await fetchMemberList({
      pageNum: pagination.page,
      pageSize: pagination.pageSize,
      userName: searchUserName.value || undefined,
      roleName: searchRoleName.value || undefined,
    } as any)
    if (!result.isSuccess)
      throwApiFailure(result, t('sys.api.apiRequestFailed'))
    const { records, total } = resolveListData(result.data)
    tableData.value = records
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
  isUpdate.value = false
  editingRecord.value = null
  modalVisible.value = true
}

function openEdit(record: MemberListRecord) {
  isUpdate.value = true
  editingRecord.value = record
  modalVisible.value = true
}

async function handleDelete(record: MemberListRecord) {
  try {
    const result = await fetchMemberDelete({ id: record.id })
    if (!result.isSuccess || result.data?.status !== 'success')
      throwApiFailure(result, t('sys.api.apiRequestFailed'))
    window.$message?.success(t('system.member.deleteMember') + t('system.member.success'))
    loadData()
  }
  catch {
    window.$message?.error(t('system.member.deleteMember') + t('system.member.fail'))
  }
}

function handleModalSuccess(updated: boolean) {
  window.$message?.success(
    `${updated ? t('common.edit') : t('system.member.addMember')} ${t('system.member.success')}`,
  )
  loadData()
}

onMounted(async () => {
  if (!userStore.getTeamId) {
    window.$message?.warning(t('system.member.selectTeamFirst'))
    router.push('/system/team')
    return
  }
  await loadRoles()
  await loadData()
})
</script>

<template>
  <n-card :bordered="false" class="h-full">
    <div class="mb-16px flex flex-wrap items-center gap-12px">
      <n-input
        v-model:value="searchUserName"
        clearable
        :placeholder="t('system.member.searchByUser')"
        class="max-w-220px"
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      />
      <n-select
        v-model:value="searchRoleName"
        clearable
        :options="roleFilterOptions"
        :placeholder="t('system.member.searchByRole')"
        class="max-w-220px"
        @update:value="handleSearch"
      />
      <n-button type="primary" ghost @click="handleSearch">
        {{ t('common.queryText') }}
      </n-button>
      <n-button
        id="e2e-member-create-btn"
        v-auth="'member:add'"
        type="primary"
        class="ml-auto"
        @click="openCreate"
      >
        {{ t('common.add') }}
      </n-button>
    </div>
    <n-data-table
      remote
      :loading="loading"
      :columns="columns"
      :data="tableData"
      :pagination="pagination"
      :row-key="(row: MemberListRecord) => row.id"
      flex-height
      class="min-h-480px"
    />
  </n-card>
  <MemberModal
    v-model:show="modalVisible"
    :is-update="isUpdate"
    :record="editingRecord"
    :role-options="roleListOptions"
    @success="handleModalSuccess"
  />
</template>
