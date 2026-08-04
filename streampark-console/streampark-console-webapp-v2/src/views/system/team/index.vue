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
import type { TeamListRecord } from '@/types/api/system/model/teamModel'
import { usePermission } from '@/hooks'
import { fetchTeamDelete, fetchTeamList } from '@/service'
import TeamModal from './components/TeamModal.vue'
import { resolveListData } from '../shared/utils'
defineOptions({ name: 'Team' })

const { t } = useI18n()
const { hasPermission } = usePermission()

const loading = ref(false)
const tableData = ref<TeamListRecord[]>([])
const searchTeamName = ref('')
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
const editingRecord = ref<TeamListRecord | null>(null)

const columns = computed<DataTableColumns<TeamListRecord>>(() => [
    { title: t('system.team.table.teamName'), key: 'teamName', sorter: true },
    {
        title: t('common.description'),
        key: 'description',
        ellipsis: { tooltip: true },
        width: 350,
    },
    { title: t('common.createTime'), key: 'createTime', sorter: true },
    { title: t('common.modifyTime'), key: 'modifyTime', sorter: true },
    {
        title: t('component.table.operation'),
        key: 'action',
        width: 200,
        render(row) {
            const actions: VNode[] = []
            if (hasPermission('team:update')) {
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
                                        class: 'e2e-team-edit-btn',
                                        onClick: () => openEdit(row),
                                    },
                                    { icon: () => ionIcon(I.edit) },
                                ),
                            default: () => t('system.team.modifyTeam'),
                        },
                    ),
                )
            }
            if (hasPermission('team:delete')) {
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
                                                    class: 'e2e-team-delete-btn',
                                                },
                                                { icon: () => ionIcon(I.delete) },
                                            ),
                                        default: () => t('system.team.deleteTeam'),
                                    },
                                ),
                            default: () => t('system.team.deletePopConfirm'),
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
        const result = await fetchTeamList({
            pageNum: pagination.page,
            pageSize: pagination.pageSize,
            teamName: searchTeamName.value || undefined,
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
    isUpdate.value = false
    editingRecord.value = null
    modalVisible.value = true
}

function openEdit(record: TeamListRecord) {
    isUpdate.value = true
    editingRecord.value = record
    modalVisible.value = true
}

async function handleDelete(record: TeamListRecord) {
    try {
        const result = await fetchTeamDelete({ id: record.id })
        if (!result.isSuccess || result.data?.status !== 'success')
            throwApiFailure(result, t('sys.api.apiRequestFailed'))
        window.$message?.success(`${t('system.team.deleteTeam')} ${t('system.team.success')}`)
        loadData()
    } catch {
        window.$message?.error(`${t('system.team.deleteTeam')} ${t('system.team.fail')}`)
    }
}

function handleModalSuccess(updated: boolean) {
    window.$message?.success(
        `${updated ? t('common.edit') : t('system.team.add')} ${t('system.team.success')}`,
    )
    loadData()
}

onMounted(loadData)
</script>

<template>
    <n-card :bordered="false" class="h-full">
        <div class="mb-16px flex flex-wrap items-center justify-between gap-12px">
            <n-input
                v-model:value="searchTeamName"
                clearable
                :placeholder="t('system.team.searchByTeam')"
                class="max-w-280px"
                @keyup.enter="handleSearch"
                @clear="handleSearch"
            />
            <n-space>
                <n-button type="primary" ghost @click="handleSearch">
                    {{ t('common.queryText') }}
                </n-button>
                <n-button
                    id="e2e-team-create-btn"
                    v-auth="'team:add'"
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
            :row-key="(row: TeamListRecord) => row.id"
            flex-height
            class="min-h-480px"
        />
    </n-card>
    <TeamModal
        v-model:show="modalVisible"
        :is-update="isUpdate"
        :record="editingRecord"
        @success="handleModalSuccess"
    />
</template>
