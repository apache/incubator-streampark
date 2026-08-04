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
import type { VariableListRecord } from '@/types/api/resource/variable/model/variableModel'
import { usePermission } from '@/hooks'
import { fetchVariableDelete, fetchVariableList } from '@/service'
import { resolveListData } from '@/views/system/shared/utils'
import VariableDetailModal from './components/VariableDetailModal.vue'
import VariableModal from './components/VariableModal.vue'
defineOptions({ name: 'Variable' })

const { t } = useI18n()
const { hasPermission } = usePermission()
const router = useRouter()

const loading = ref(false)
const tableData = ref<VariableListRecord[]>([])
const searchCode = ref('')
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
const detailVisible = ref(false)
const isUpdate = ref(false)
const editingRecord = ref<VariableListRecord | null>(null)
const detailRecord = ref<VariableListRecord | null>(null)

const columns = computed<DataTableColumns<VariableListRecord>>(() => [
    { title: t('flink.variable.table.variableCode'), key: 'variableCode' },
    {
        title: t('flink.variable.table.variableValue'),
        key: 'variableValue',
        ellipsis: { tooltip: true },
    },
    { title: t('common.description'), key: 'description', ellipsis: { tooltip: true } },
    { title: t('common.createTime'), key: 'createTime', sorter: true },
    { title: t('common.modifyTime'), key: 'modifyTime', sorter: true },
    {
        title: t('component.table.operation'),
        key: 'action',
        width: 220,
        render(row) {
            const actions: VNode[] = []
            if (hasPermission('variable:update')) {
                actions.push(
                    actionBtn(
                        I.edit,
                        t('flink.variable.modifyVariable'),
                        () => openEdit(row),
                        'e2e-var-edit-btn',
                    ),
                )
            }
            actions.push(actionBtn(I.detail, t('common.detail'), () => openDetail(row)))
            if (hasPermission('variable:depend_apps')) {
                actions.push(
                    actionBtn(I.depend, t('flink.variable.table.depend'), () =>
                        router.push(`/resource/variable/depend_apps?id=${row.variableCode}`),
                    ),
                )
            }
            if (hasPermission('variable:delete')) {
                actions.push(
                    h(
                        NPopconfirm,
                        { onPositiveClick: () => handleDelete(row) },
                        {
                            trigger: () =>
                                actionBtn(
                                    I.delete,
                                    t('flink.variable.deleteVariable'),
                                    undefined,
                                    'e2e-var-delete-btn',
                                ),
                            default: () => t('flink.variable.deletePopConfirm'),
                        },
                    ),
                )
            }
            return h(NSpace, { size: 4 }, { default: () => actions })
        },
    },
])

function actionBtn(icon: string, tip: string, onClick?: () => void, cls?: string) {
    return h(
        NTooltip,
        { trigger: 'hover' },
        {
            trigger: () =>
                h(
                    NButton,
                    {
                        quaternary: true,
                        size: 'small',
                        class: cls,
                        onClick,
                    },
                    { icon: () => ionIcon(icon) },
                ),
            default: () => tip,
        },
    )
}

async function loadData() {
    loading.value = true
    try {
        const result = await fetchVariableList({
            pageNum: pagination.page,
            pageSize: pagination.pageSize,
            variableCode: searchCode.value || undefined,
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

function openEdit(record: VariableListRecord) {
    isUpdate.value = true
    editingRecord.value = record
    modalVisible.value = true
}

function openDetail(record: VariableListRecord) {
    detailRecord.value = record
    detailVisible.value = true
}

async function handleDelete(record: VariableListRecord) {
    try {
        const result = await fetchVariableDelete({
            id: record.id,
            teamId: record.teamId,
            variableCode: record.variableCode,
            variableValue: record.variableValue,
        } as any)
        if (!result.isSuccess || result.data?.status !== 'success')
            throwApiFailure(result, t('sys.api.apiRequestFailed'))
        window.$message?.success(t('flink.variable.deleteVariable') + t('flink.variable.success'))
        loadData()
    } catch {
        window.$message?.error(t('flink.variable.deleteVariable') + t('flink.variable.fail'))
    }
}

function handleModalSuccess(updated: boolean) {
    window.$message?.success(
        `${updated ? t('common.edit') : t('flink.variable.add')}${t('flink.variable.success')}`,
    )
    loadData()
}

onMounted(loadData)
</script>

<template>
    <n-card :bordered="false" class="h-full">
        <div class="mb-16px flex flex-wrap items-center justify-between gap-12px">
            <n-input
                v-model:value="searchCode"
                clearable
                :placeholder="t('flink.variable.searchByCode')"
                class="max-w-280px"
                @keyup.enter="handleSearch"
                @clear="handleSearch"
            />
            <n-space>
                <n-button type="primary" ghost @click="handleSearch">
                    {{ t('common.queryText') }}
                </n-button>
                <n-button
                    id="e2e-var-create-btn"
                    v-auth="'variable:add'"
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
            :row-key="(row: VariableListRecord) => String(row.id)"
            flex-height
            class="min-h-480px"
        />
    </n-card>
    <VariableModal
        v-model:show="modalVisible"
        :is-update="isUpdate"
        :record="editingRecord"
        @success="handleModalSuccess"
    />
    <VariableDetailModal v-model:show="detailVisible" :record="detailRecord" />
</template>
