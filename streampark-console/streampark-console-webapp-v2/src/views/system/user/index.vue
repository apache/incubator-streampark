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
import { ionIcon } from '@/utils/ionIcon'
import { SP_ICONS as I } from '@/constants/streamparkIcons'
import type { DataTableColumns } from 'naive-ui'
import type { UserListRecord } from '@/types/api/system/model/userModel'
import { usePermission } from '@/hooks'
import {
  fetchDeleteUser,
  fetchResetUserPassword,
  fetchUserList,
} from '@/service'
import { useUserStoreWithOut } from '@/store/modules/user'
import UserDetailModal from './components/UserDetailModal.vue'
import UserDrawer from './components/UserDrawer.vue'
import { FormTypeEnum, LOGIN_TYPE_PASSWORD, StatusEnum } from '../shared/constants'
defineOptions({ name: 'User' })

const { t } = useI18n()
const { hasPermission } = usePermission()
const userStore = useUserStoreWithOut()

const currentUsername = computed(() => userStore.getUserInfo?.username)

const loading = ref(false)
const tableData = ref<UserListRecord[]>([])
const searchUsername = ref('')
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
const editingRecord = ref<UserListRecord | null>(null)

const detailVisible = ref(false)
const detailRecord = ref<UserListRecord | null>(null)

const columns = computed<DataTableColumns<UserListRecord>>(() => [
  {
    title: t('system.user.form.userName'),
    key: 'username',
    sorter: true,
  },
  {
    title: t('system.user.form.nickName'),
    key: 'nickName',
  },
  {
    title: t('system.user.form.userType'),
    key: 'userType',
  },
  {
    title: t('system.user.form.loginType'),
    key: 'loginType',
  },
  {
    title: t('system.user.form.status'),
    key: 'status',
    filterOptions: [
      { label: t('system.user.effective'), value: StatusEnum.Effective },
      { label: t('system.user.locked'), value: StatusEnum.Locked },
    ],
    filter(value, row) {
      return row.status === value
    },
    render(row) {
      const effective = row.status === StatusEnum.Effective
      return h(
        NTag,
        { type: effective ? 'success' : 'error', size: 'small' },
        { default: () => effective ? t('system.user.effective') : t('system.user.locked') },
      )
    },
  },
  {
    title: t('common.createTime'),
    key: 'createTime',
    sorter: true,
  },
  {
    title: t('component.table.operation'),
    key: 'action',
    width: 200,
    render(row) {
      const actions: Array<{ show: boolean, node: ReturnType<typeof h> }> = []

      if (hasPermission('user:update') && (row.username !== 'admin' || currentUsername.value === 'admin')) {
        actions.push({
          show: true,
          node: h(
            NTooltip,
            { trigger: 'hover' },
            {
              trigger: () => h(
                NButton,
                {
                  quaternary: true,
                  size: 'small',
                  class: 'e2e-user-edit-btn',
                  onClick: () => openEdit(row),
                },
                { icon: () => ionIcon(I.edit) },
              ),
              default: () => t('system.user.table.modify'),
            },
          ),
        })
      }

      actions.push({
        show: true,
        node: h(
          NTooltip,
          { trigger: 'hover' },
          {
            trigger: () => h(
              NButton,
              {
                quaternary: true,
                size: 'small',
                onClick: () => openDetail(row),
              },
              { icon: () => ionIcon(I.detail) },
            ),
            default: () => t('common.detail'),
          },
        ),
      })

      if (
        hasPermission('user:reset')
        && (row.username !== 'admin' || currentUsername.value === 'admin')
        && row.loginType === LOGIN_TYPE_PASSWORD
      ) {
        actions.push({
          show: true,
          node: h(
            NPopconfirm,
            {
              onPositiveClick: () => handleReset(row),
            },
            {
              trigger: () => h(
                NTooltip,
                { trigger: 'hover' },
                {
                  trigger: () => h(
                    NButton,
                    { quaternary: true, size: 'small' },
                    { icon: () => ionIcon(I.resetPassword) },
                  ),
                  default: () => t('system.user.table.reset'),
                },
              ),
              default: () => t('system.user.table.resetTip'),
            },
          ),
        })
      }

      if (hasPermission('user:delete') && row.username !== 'admin') {
        actions.push({
          show: true,
          node: h(
            NPopconfirm,
            {
              onPositiveClick: () => handleDelete(row),
            },
            {
              trigger: () => h(
                NTooltip,
                { trigger: 'hover' },
                {
                  trigger: () => h(
                    NButton,
                    { quaternary: true, size: 'small', type: 'error' },
                    { icon: () => ionIcon(I.delete) },
                  ),
                  default: () => t('system.user.table.delete'),
                },
              ),
              default: () => t('system.user.table.deleteTip'),
            },
          ),
        })
      }

      return h(NSpace, { size: 4 }, {
        default: () => actions.filter(a => a.show).map(a => a.node),
      })
    },
  },
])

async function loadData() {
  loading.value = true
  try {
    const result = await fetchUserList({
      pageNum: pagination.page,
      pageSize: pagination.pageSize,
      username: searchUsername.value || undefined,
    } as any)
    if (!result.isSuccess)
      throwApiFailure(result, t('sys.api.apiRequestFailed'))

    tableData.value = result.data?.records ?? []
    pagination.itemCount = Number(result.data?.total ?? tableData.value.length)
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

function openEdit(record: UserListRecord) {
  drawerFormType.value = FormTypeEnum.Edit
  editingRecord.value = record
  drawerVisible.value = true
}

function openDetail(record: UserListRecord) {
  detailRecord.value = record
  detailVisible.value = true
}

async function handleDelete(record: UserListRecord) {
  const message = window.$message?.loading(t('system.user.table.deleting'), { duration: 0 })
  try {
    const result = await fetchDeleteUser({ userId: record.userId })
    if (!result.isSuccess)
      throwApiFailure(result, t('sys.api.apiRequestFailed'))
    window.$message?.success(t('system.user.table.deleteSuccess'))
    loadData()
  }
  catch (e: any) {
    showCatchError(e, t('sys.api.apiRequestFailed'))
  }
  finally {
    message?.destroy()
  }
}

async function handleReset(record: UserListRecord) {
  const message = window.$message?.loading(t('system.user.table.resetLoading'), { duration: 0 })
  try {
    const result = await fetchResetUserPassword({ username: record.username })
    if (!result.isSuccess)
      throwApiFailure(result, t('sys.api.apiRequestFailed'))
    window.$dialog?.success({
      title: t('system.user.resetSucceeded'),
      content: `${t('system.user.newPasswordTip')}${result.data ?? ''}`,
      positiveText: t('common.okText'),
    })
  }
  catch (e: any) {
    showCatchError(e, t('sys.api.apiRequestFailed'))
  }
  finally {
    message?.destroy()
  }
}

function handleDrawerSuccess() {
  window.$message?.success(t('system.user.table.operationSuccess'))
  loadData()
}

onMounted(loadData)
</script>

<template>
  <n-card :bordered="false" class="h-full">
    <div class="mb-16px flex flex-wrap items-center justify-between gap-12px">
      <n-input
        v-model:value="searchUsername"
        clearable
        :placeholder="t('system.user.searchByName')"
        class="max-w-280px"
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      />
      <n-space>
        <n-button type="primary" ghost @click="handleSearch">
          {{ t('common.queryText') }}
        </n-button>
        <n-button
          id="e2e-user-create-btn"
          v-auth="'user:add'"
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
      :row-key="(row: UserListRecord) => row.userId"
      flex-height
      class="min-h-480px"
    />
  </n-card>

  <UserDrawer
    v-model:show="drawerVisible"
    :form-type="drawerFormType"
    :record="editingRecord"
    @success="handleDrawerSuccess"
  />

  <UserDetailModal
    v-model:show="detailVisible"
    :record="detailRecord"
  />
</template>
