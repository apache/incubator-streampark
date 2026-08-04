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
import type { FlinkEnv } from '@/types/api/flink/flinkEnv.type'
import { fetchDefaultSet, fetchFlinkEnvPage, fetchFlinkEnvRemove, fetchValidity } from '@/service'
import { resolveListData } from '@/views/system/shared/utils'
import { SvgIcon } from '@/components/Icon'
import FlinkEnvDrawer from './components/FlinkEnvDrawer.vue'
import FlinkEnvModal from './components/FlinkEnvModal.vue'

defineOptions({ name: 'FlinkHome' })

const { t } = useI18n()

const loading = ref(false)
const tableData = ref<FlinkEnv[]>([])
const searchFlinkName = ref('')
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
const versionId = ref<string | null>(null)
const modalInitialData = ref<{
    flinkName?: string
    flinkHome?: string
    description?: string | null
} | null>(null)

const drawerVisible = ref(false)
const drawerEnvId = ref<string | null>(null)

const columns = computed<DataTableColumns<FlinkEnv>>(() => [
    {
        title: t('setting.flinkHome.flinkName'),
        key: 'flinkName',
        render(row) {
            return h('div', { class: 'flex items-center gap-8px' }, [
                h(SvgIcon, { name: 'flink', size: 20 }),
                h('span', null, row.flinkName),
            ])
        },
    },
    { title: t('setting.flinkHome.flinkHome'), key: 'flinkHome', ellipsis: { tooltip: true } },
    { title: t('setting.flinkHome.flinkVersion'), key: 'version' },
    {
        title: t('setting.flinkHome.defaultLabel'),
        key: 'isDefault',
        width: 100,
        render(row) {
            return h(
                NSwitch,
                {
                    value: row.isDefault,
                    disabled: row.isDefault,
                    onUpdateValue: (value: boolean) => handleSetDefault(row, value),
                },
                {
                    checked: () => ionIcon('CheckmarkOutline'),
                    unchecked: () => ionIcon('CloseOutline'),
                },
            )
        },
    },
    {
        title: t('setting.flinkHome.description'),
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
                                    class: 'e2e-flinkenv-edit-btn',
                                    onClick: () => handleEdit(row),
                                },
                                { icon: () => ionIcon(I.edit) },
                            ),
                        default: () => t('setting.flinkHome.edit'),
                    },
                ),
            )
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
                                    onClick: () => openConfigDrawer(row),
                                },
                                { icon: () => ionIcon(I.view) },
                            ),
                        default: () => t('setting.flinkHome.conf'),
                    },
                ),
            )
            const deleteDisabled = row.isDefault && tableData.value.length > 1
            actions.push(
                h(
                    NPopconfirm,
                    {
                        disabled: deleteDisabled,
                        onPositiveClick: () => handleDelete(row),
                    },
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
                                                class: 'e2e-flinkenv-delete-btn',
                                                disabled: deleteDisabled,
                                            },
                                            { icon: () => ionIcon(I.delete) },
                                        ),
                                    default: () => t('common.delText'),
                                },
                            ),
                        default: () => t('setting.flinkHome.delete'),
                    },
                ),
            )
            return h(NSpace, { size: 4 }, { default: () => actions })
        },
    },
])

async function loadData() {
    loading.value = true
    try {
        const result = await fetchFlinkEnvPage({
            pageNum: pagination.page,
            pageSize: pagination.pageSize,
            flinkName: searchFlinkName.value || undefined,
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
    versionId.value = null
    modalInitialData.value = null
    modalVisible.value = true
}

async function handleEdit(record: FlinkEnv) {
    try {
        const result = await fetchValidity(record.id)
        if (!result.isSuccess) throwApiFailure(result, t('sys.api.apiRequestFailed'))
        versionId.value = record.id
        modalInitialData.value = {
            flinkName: record.flinkName,
            flinkHome: record.flinkHome,
            description: record.description || null,
        }
        modalVisible.value = true
    } catch (e: any) {
        showCatchError(e, t('sys.api.apiRequestFailed'))
    }
}

function openConfigDrawer(record: FlinkEnv) {
    drawerEnvId.value = record.id
    drawerVisible.value = true
}

async function handleDelete(record: FlinkEnv) {
    const result = await fetchFlinkEnvRemove(record.id)
    if (result.isSuccess) {
        window.$message?.success(t('flink.app.home.removed'))
        loadData()
    } else {
        showResultError(result, t('sys.api.apiRequestFailed'))
    }
}

async function handleSetDefault(record: FlinkEnv, value: boolean) {
    if (!value || record.isDefault) return
    try {
        const result = await fetchDefaultSet(record.id)
        if (!result.isSuccess) throwApiFailure(result, t('sys.api.apiRequestFailed'))
        window.$message?.success(`${record.flinkName} set default successful!`)
        loadData()
    } catch (e: any) {
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
                v-model:value="searchFlinkName"
                clearable
                :placeholder="t('setting.flinkHome.searchByName')"
                class="max-w-280px"
                @keyup.enter="handleSearch"
                @clear="handleSearch"
            />
            <n-space>
                <n-button type="primary" ghost @click="handleSearch">
                    {{ t('common.queryText') }}
                </n-button>
                <n-button
                    id="e2e-env-add-btn"
                    v-auth="'project:create'"
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
            :row-key="(row: FlinkEnv) => row.id"
            flex-height
            class="min-h-480px"
        />
    </n-card>
    <FlinkEnvModal
        v-model:show="modalVisible"
        :version-id="versionId"
        :initial-data="modalInitialData"
        @success="handleModalSuccess"
    />
    <FlinkEnvDrawer v-model:show="drawerVisible" :env-id="drawerEnvId" />
</template>
