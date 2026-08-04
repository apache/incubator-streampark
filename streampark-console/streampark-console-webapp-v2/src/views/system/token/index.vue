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
import type { TokenListRecord } from '@/types/api/system/model/tokenModel'
import { usePermission } from '@/hooks'
import { fetchTokenDelete, fetchTokenList, fetchTokenStatusToggle } from '@/service'
import TokenModal from './components/TokenModal.vue'
import { TOKEN_STATUS_OFF, TOKEN_STATUS_ON } from '../shared/constants'
import { resolveListData } from '../shared/utils'
defineOptions({ name: 'UserToken' })

const { t } = useI18n()
const { hasPermission } = usePermission()

const loading = ref(false)
const tableData = ref<TokenListRecord[]>([])
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

const modalVisible = ref(false)
const statusLoadingMap = reactive<Record<string, boolean>>({})

const columns = computed<DataTableColumns<TokenListRecord>>(() => [
    { title: t('system.token.table.userName'), key: 'username', width: 150, sorter: true },
    {
        title: t('system.token.table.token'),
        key: 'token',
        ellipsis: { tooltip: true },
        width: 250,
    },
    { title: t('common.description'), key: 'description' },
    { title: t('common.createTime'), key: 'createTime' },
    {
        title: t('system.token.table.status'),
        key: 'status',
        width: 100,
        render(row) {
            return h(NSwitch, {
                value: String(row.status) === TOKEN_STATUS_ON,
                loading: statusLoadingMap[row.id],
                disabled: !hasPermission('token:update'),
                onUpdateValue: (checked: boolean) => toggleStatus(row, checked),
            })
        },
    },
    {
        title: t('component.table.operation'),
        key: 'action',
        width: 200,
        render(row) {
            const actions: VNode[] = []
            if (hasPermission('token:view')) {
                actions.push(
                    h(
                        NTooltip,
                        { trigger: 'hover' },
                        {
                            trigger: () =>
                                h(
                                    NButton,
                                    {
                                        quaternary: true,
                                        size: 'small',
                                        class: 'e2e-token-copy-btn',
                                        onClick: () => handleCopy(row),
                                    },
                                    { icon: () => ionIcon(I.copy) },
                                ),
                            default: () => t('system.token.copyToken'),
                        },
                    ),
                )
            }
            if (hasPermission('token:delete')) {
                actions.push(
                    h(
                        NPopconfirm,
                        { onPositiveClick: () => handleDelete(row) },
                        {
                            trigger: () =>
                                h(
                                    NTooltip,
                                    { trigger: 'hover' },
                                    {
                                        trigger: () =>
                                            h(
                                                NButton,
                                                {
                                                    quaternary: true,
                                                    size: 'small',
                                                    class: 'e2e-token-delete-btn',
                                                },
                                                { icon: () => ionIcon(I.delete) },
                                            ),
                                        default: () => t('system.token.deleteToken'),
                                    },
                                ),
                            default: () => t('system.token.operation.deleteTokenConfirm'),
                        },
                    ),
                )
            }
            return h(NSpace, { size: 4 }, { default: () => actions })
        },
    },
])

async function loadData() {
    loading.value = true
    try {
        const result = await fetchTokenList({
            pageNum: pagination.page,
            pageSize: pagination.pageSize,
            username: searchUsername.value || undefined,
        } as any)
        if (!result.isSuccess) throwApiFailure(result, t('sys.api.apiRequestFailed'))
        const { records, total } = resolveListData(result.data)
        tableData.value = records
        pagination.itemCount = total
    } catch (e: any) {
        showCatchError(e, t('sys.api.apiRequestFailed'))
        tableData.value = []
        pagination.itemCount = 0
    } finally {
        loading.value = false
    }
}

function handleSearch() {
    pagination.page = 1
    loadData()
}

function openCreate() {
    modalVisible.value = true
}

async function handleCopy(record: TokenListRecord) {
    try {
        await navigator.clipboard.writeText(record.token)
        window.$message?.success(t('system.token.operation.copySuccess'))
    } catch {
        window.$message?.error(t('components.copyText.unsupportedError'))
    }
}

async function handleDelete(record: TokenListRecord) {
    const result = await fetchTokenDelete({ tokenId: record.id })
    if (result.isSuccess) {
        window.$message?.success(t('system.token.operation.deleteSuccess'))
        loadData()
    } else {
        window.$message?.error(t('system.token.operation.deleteFailed'))
    }
}

async function toggleStatus(record: TokenListRecord, checked: boolean) {
    statusLoadingMap[record.id] = true
    try {
        const result = await fetchTokenStatusToggle({ tokenId: record.id })
        if (!result.isSuccess) throwApiFailure(result, t('sys.api.apiRequestFailed'))
        record.status = checked ? Number(TOKEN_STATUS_ON) : Number(TOKEN_STATUS_OFF)
        window.$message?.success(t('common.operationSuccess'))
    } catch (e: any) {
        showCatchError(e, t('sys.api.apiRequestFailed'))
    } finally {
        statusLoadingMap[record.id] = false
    }
}

function handleModalSuccess() {
    window.$message?.success(t('system.token.operation.createSuccess'))
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
                    id="e2e-token-create-btn"
                    v-auth="'token:add'"
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
            :row-key="(row: TokenListRecord) => row.id"
            flex-height
            class="min-h-480px"
        />
    </n-card>
    <TokenModal v-model:show="modalVisible" @success="handleModalSuccess" />
</template>
