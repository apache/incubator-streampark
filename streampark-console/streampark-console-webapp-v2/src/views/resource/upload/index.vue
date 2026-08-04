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
import type { ResourceListRecord } from '@/types/api/resource/upload/model/resourceModel'
import { usePermission } from '@/hooks'
import { fetchResourceDelete, fetchResourceList, fetchTeamResource } from '@/service'
import { resolveListData } from '@/views/system/shared/utils'
import { EngineTypeEnum, ResourceTypeEnum } from '@/views/resource/upload/shared/constants'
import UploadDrawer from './components/UploadDrawer.vue'
import flinkAppSvg from '@/assets/icons/flink2.svg'
import sparkAppSvg from '@/assets/icons/spark.svg'
import connectorSvg from '@/assets/icons/connector.svg'
import udxfSvg from '@/assets/icons/fx.svg'
import jarSvg from '@/assets/icons/jar.svg'
import groupSvg from '@/assets/icons/group.svg'

defineOptions({ name: 'ResourceUpload' })

const { t } = useI18n()
const { hasPermission } = usePermission()

const loading = ref(false)
const tableData = ref<ResourceListRecord[]>([])
const searchResourceName = ref('')
const teamResource = ref<ResourceListRecord[]>([])
const drawerVisible = ref(false)
const isUpdate = ref(false)
const editingRecord = ref<ResourceListRecord | null>(null)

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

function renderEngineType(row: ResourceListRecord) {
    if (row.engineType === EngineTypeEnum.FLINK) {
        return h('span', { class: 'inline-flex items-center gap-6px' }, [
            h('img', { src: flinkAppSvg, class: 'h-14px w-14px', alt: 'Flink' }),
            'Apache Flink',
        ])
    }
    if (row.engineType === EngineTypeEnum.SPARK) {
        return h('span', { class: 'inline-flex items-center gap-6px' }, [
            h('img', { src: sparkAppSvg, class: 'h-14px w-14px', alt: 'Spark' }),
            'Apache Spark',
        ])
    }
    return row.engineType
}

function renderResourceType(row: ResourceListRecord) {
    const iconMap: Record<string, { src: string; label: string }> = {
        [ResourceTypeEnum.APP]: {
            src: row.engineType === EngineTypeEnum.FLINK ? flinkAppSvg : sparkAppSvg,
            label: row.engineType === EngineTypeEnum.FLINK ? 'Flink App' : 'Spark App',
        },
        [ResourceTypeEnum.CONNECTOR]: { src: connectorSvg, label: 'Connector' },
        [ResourceTypeEnum.UDXF]: { src: udxfSvg, label: 'UDXF' },
        [ResourceTypeEnum.JAR_LIBRARY]: { src: jarSvg, label: 'Jar Library' },
        [ResourceTypeEnum.GROUP]: { src: groupSvg, label: 'GROUP' },
    }
    const meta = iconMap[row.resourceType]
    if (!meta) return row.resourceType
    return h(
        NTag,
        { type: 'info', size: 'small' },
        {
            default: () =>
                h('span', { class: 'inline-flex items-center gap-4px' }, [
                    h('img', { src: meta.src, class: 'h-14px w-14px', alt: meta.label }),
                    meta.label,
                ]),
        },
    )
}

const columns = computed<DataTableColumns<ResourceListRecord>>(() => [
    { title: t('flink.resource.table.resourceName'), key: 'resourceName', sorter: true },
    { title: t('common.description'), key: 'description', ellipsis: { tooltip: true } },
    {
        title: t('flink.resource.resourceType'),
        key: 'resourceType',
        render: (row) => renderResourceType(row),
    },
    {
        title: t('flink.resource.engineType'),
        key: 'engineType',
        render: (row) => renderEngineType(row),
    },
    { title: t('common.createTime'), key: 'createTime', sorter: true },
    { title: t('common.modifyTime'), key: 'modifyTime', sorter: true },
    {
        title: t('component.table.operation'),
        key: 'action',
        width: 120,
        render(row) {
            const actions: VNode[] = []
            if (hasPermission('resource:update')) {
                actions.push(
                    actionBtn(
                        I.edit,
                        t('flink.resource.modifyResource'),
                        () => openEdit(row),
                        'e2e-upload-edit-btn',
                    ),
                )
            }
            if (hasPermission('resource:delete')) {
                actions.push(
                    h(
                        NPopconfirm,
                        { onPositiveClick: () => handleDelete(row) },
                        {
                            trigger: () =>
                                actionBtn(
                                    I.delete,
                                    t('flink.resource.deleteResource'),
                                    undefined,
                                    'e2e-upload-delete-btn',
                                ),
                            default: () => t('flink.resource.deletePopConfirm'),
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
        const result = await fetchResourceList({
            pageNum: pagination.page,
            pageSize: pagination.pageSize,
            resourceName: searchResourceName.value || undefined,
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

async function loadTeamResource() {
    const result = await fetchTeamResource({})
    if (result.isSuccess) teamResource.value = result.data ?? []
}

function handleSearch() {
    pagination.page = 1
    loadData()
}

function openCreate() {
    isUpdate.value = false
    editingRecord.value = null
    drawerVisible.value = true
}

function openEdit(record: ResourceListRecord) {
    isUpdate.value = true
    editingRecord.value = record
    drawerVisible.value = true
}

async function handleDelete(record: ResourceListRecord) {
    try {
        const result = await fetchResourceDelete({
            id: record.id,
            teamId: record.teamId,
            resourceName: record.resourceName,
        })
        if (!result.isSuccess || result.data?.status !== 'success')
            throwApiFailure(result, t('sys.api.apiRequestFailed'))
        window.$message?.success(t('flink.resource.deleteResource') + t('flink.resource.success'))
        loadData()
        loadTeamResource()
    } catch {
        window.$message?.error(t('flink.resource.deleteResource') + t('flink.resource.fail'))
    }
}

function handleDrawerSuccess(updated: boolean) {
    window.$message?.success(
        `${updated ? t('common.edit') : t('flink.resource.add')}${t('flink.resource.success')}`,
    )
    loadData()
    loadTeamResource()
}

onMounted(() => {
    loadData()
    loadTeamResource()
})
</script>

<template>
    <n-card :bordered="false" class="h-full">
        <div class="mb-16px flex flex-wrap items-center justify-between gap-12px">
            <n-input
                v-model:value="searchResourceName"
                clearable
                :placeholder="t('flink.resource.table.searchByResourceName')"
                class="max-w-280px"
                @keyup.enter="handleSearch"
                @clear="handleSearch"
            />
            <n-space>
                <n-button type="primary" ghost @click="handleSearch">
                    {{ t('common.queryText') }}
                </n-button>
                <n-button
                    id="e2e-upload-create-btn"
                    v-auth="'resource:add'"
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
            :row-key="(row: ResourceListRecord) => String(row.id)"
            flex-height
            class="min-h-480px"
        />
    </n-card>
    <UploadDrawer
        v-model:show="drawerVisible"
        :is-update="isUpdate"
        :record="editingRecord"
        :team-resource="teamResource"
        @success="handleDrawerSuccess"
    />
</template>

<style scoped>
:deep(.h-14px) {
    height: 14px;
}
:deep(.w-14px) {
    width: 14px;
}
</style>
