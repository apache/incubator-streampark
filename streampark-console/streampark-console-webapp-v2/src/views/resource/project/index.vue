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
import type { ProjectRecord } from '@/types/api/resource/project/model/projectModel'
import { usePermission } from '@/hooks'
import {
    fetchProjectBuild,
    fetchProjectBuildLog,
    fetchProjectDelete,
    fetchProjectList,
} from '@/service'
import { resolveListData } from '@/views/system/shared/utils'
import { toTagColor } from '@/utils/tagColor'
import { BuildStateEnum } from '@/enums/flinkEnum'
import { ProjectTypeEnum } from '@/enums/projectEnum'
import { useProjectConstants } from '@/views/resource/project/shared/constants'
import LogModal from './components/LogModal.vue'
import { useTimeoutFn } from '@vueuse/core'
import { buildUUID } from '@/utils/uuid'
import flinkSvg from '@/assets/icons/flink.svg'
import sparkSvg from '@/assets/icons/spark.svg'

defineOptions({ name: 'Project' })

const { t } = useI18n()
const { hasPermission } = usePermission()
const router = useRouter()
const { statusList, buildStateMap } = useProjectConstants()

const loading = ref(false)
const tableData = ref<ProjectRecord[]>([])
const buildStateFilter = ref('')
const searchName = ref('')
const logModalVisible = ref(false)
const logProject = ref<ProjectRecord | null>(null)

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

async function loadProjectBuildLog(data: Recordable) {
    return fetchProjectBuildLog(data)
}

const projectIconMap: Record<number, { src: string; label: string }> = {
    [ProjectTypeEnum.FLINK]: { src: flinkSvg, label: 'FLINK' },
    [ProjectTypeEnum.SPARK]: { src: sparkSvg, label: 'SPARK' },
}

function renderBranch(row: ProjectRecord) {
    const refs = (row as Recordable).refs ?? row.branches ?? ''
    if (String(refs).startsWith('refs/tags/')) {
        return h(
            NTag,
            { type: 'success', size: 'small' },
            {
                default: () => String(refs).replace('refs/tags/', ''),
            },
        )
    }
    return h(
        NTag,
        { type: 'info', size: 'small' },
        {
            default: () => String(refs).replace('refs/heads/', ''),
        },
    )
}

function renderProjectType(row: ProjectRecord) {
    const meta = projectIconMap[row.type] ?? projectIconMap[ProjectTypeEnum.FLINK]
    const iconEl = h('img', {
        src: meta.src,
        class: 'h-20px w-20px',
        alt: meta.label,
    })
    if (row.buildState === BuildStateEnum.NEED_REBUILD) {
        return h(
            NBadge,
            { value: 'NEW', type: 'warning' },
            {
                default: () =>
                    h('span', { class: 'inline-flex items-center gap-6px' }, [iconEl, meta.label]),
            },
        )
    }
    return h('span', { class: 'inline-flex items-center gap-6px' }, [iconEl, meta.label])
}

function renderBuildState(row: ProjectRecord) {
    const key = String(row.buildState)
    const meta = buildStateMap.value[key] ?? {
        color: '#f5222d',
        label: t('flink.project.projectStatus.failed'),
    }
    const children = [
        h(NTag, { color: toTagColor(meta.color), size: 'small' }, { default: () => meta.label }),
    ]
    if (row.buildState === BuildStateEnum.BUILDING) {
        return h('span', { class: 'inline-flex items-center gap-8px' }, [
            h(NBadge, { dot: true, type: 'success' }),
            ...children,
        ])
    }
    return children[0]
}

const columns = computed<DataTableColumns<ProjectRecord>>(() => [
    { title: t('flink.project.form.projectName'), key: 'name' },
    {
        title: t('flink.project.form.projectType'),
        key: 'type',
        render: (row) => renderProjectType(row),
    },
    {
        title: t('flink.project.form.branches'),
        key: 'branches',
        render: (row) => renderBranch(row),
    },
    { title: t('flink.project.form.lastBuild'), key: 'lastBuild' },
    {
        title: t('flink.project.form.buildState'),
        key: 'buildState',
        render: (row) => renderBuildState(row),
    },
    {
        title: t('component.table.operation'),
        key: 'action',
        width: 200,
        render(row) {
            const isBuilding = row.buildState === BuildStateEnum.BUILDING
            const actions: VNode[] = []
            if (hasPermission('project:build')) {
                actions.push(
                    actionBtn(I.code, t('flink.project.operationTips.seeBuildLog'), () =>
                        openLog(row),
                    ),
                )
                if (!isBuilding) {
                    actions.push(
                        h(
                            NPopconfirm,
                            {
                                onPositiveClick: () => handleBuild(row),
                            },
                            {
                                trigger: () =>
                                    actionBtn(
                                        I.build,
                                        t('flink.project.operationTips.buildProject'),
                                        undefined,
                                        'e2e-project-build-btn',
                                    ),
                                default: () => t('flink.project.operationTips.buildProjectMessage'),
                            },
                        ),
                    )
                }
            }
            if (!isBuilding && hasPermission('project:update')) {
                actions.push(
                    actionBtn(
                        I.edit,
                        t('common.edit'),
                        () => router.push(`/project/edit?id=${row.id}`),
                        'e2e-project-edit-btn',
                    ),
                )
            }
            if (!isBuilding && hasPermission('project:delete')) {
                actions.push(
                    h(
                        NPopconfirm,
                        { onPositiveClick: () => handleDelete(row) },
                        {
                            trigger: () =>
                                actionBtn(
                                    I.delete,
                                    t('common.delText'),
                                    undefined,
                                    'e2e-project-delete-btn',
                                ),
                            default: () => t('flink.project.operationTips.deleteProjectMessage'),
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

async function loadData(silent = false) {
    if (!silent) loading.value = true
    try {
        const result = await fetchProjectList({
            pageNum: pagination.page,
            pageSize: pagination.pageSize,
            buildState: buildStateFilter.value || undefined,
            name: searchName.value || undefined,
        })
        if (!result.isSuccess) throwApiFailure(result, t('sys.api.apiRequestFailed'))
        const { records, total } = resolveListData(
            result.data as ProjectRecord[] | { records?: ProjectRecord[]; total?: number },
        )
        tableData.value = records
        pagination.itemCount = total
    } catch (e: any) {
        if (!silent) showCatchError(e, t('sys.api.apiRequestFailed'))
        if (!silent) {
            tableData.value = []
            pagination.itemCount = 0
        }
    } finally {
        if (!silent) loading.value = false
    }
}

function handleFilterChange(value: string) {
    buildStateFilter.value = value
    pagination.page = 1
    loadData()
}

function handleSearch() {
    pagination.page = 1
    loadData()
}

function openLog(record: ProjectRecord) {
    logProject.value = record
    logModalVisible.value = true
}

async function handleBuild(record: ProjectRecord) {
    try {
        const result = await fetchProjectBuild({
            id: record.id,
            socketId: buildUUID(),
        })
        if (!result.isSuccess) throwApiFailure(result, t('sys.api.apiRequestFailed'))
        window.$message?.success(t('flink.project.operationTips.projectIsbuildingMessage'))
        loadData(true)
    } catch {
        window.$message?.error(t('flink.project.operationTips.projectIsbuildFailedMessage'))
    }
}

async function handleDelete(record: ProjectRecord) {
    try {
        const result = await fetchProjectDelete({ id: record.id })
        if (!result.isSuccess || !result.data)
            throwApiFailure(
                result,
                t('flink.project.operationTips.deleteProjectFailedDetailMessage'),
            )
        window.$message?.success(t('flink.project.operationTips.deleteProjectSuccessMessage'))
        loadData()
    } catch (e: any) {
        showCatchError(e, t('flink.project.operationTips.deleteProjectFailedMessage'))
    }
}

const { start: startPolling, stop: stopPolling } = useTimeoutFn(() => {
    if (!loading.value) loadData(true)
    startPolling()
}, 2000)

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
            <n-space align="center" wrap>
                <n-radio-group :value="buildStateFilter" @update:value="handleFilterChange">
                    <n-radio-button
                        v-for="item in statusList"
                        :key="item.key"
                        :value="item.key ?? ''"
                    >
                        {{ item.label }}
                    </n-radio-button>
                </n-radio-group>
                <n-input
                    v-model:value="searchName"
                    clearable
                    :placeholder="t('flink.project.searchPlaceholder')"
                    class="max-w-280px"
                    @keyup.enter="handleSearch"
                    @clear="handleSearch"
                />
                <n-button type="primary" ghost @click="handleSearch">
                    {{ t('common.queryText') }}
                </n-button>
            </n-space>
            <n-button
                id="e2e-project-create-btn"
                v-auth="'project:create'"
                type="primary"
                @click="router.push('/project/add')"
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
            :row-key="(row: ProjectRecord) => row.id"
            flex-height
            class="min-h-480px"
        />
    </n-card>
    <LogModal
        v-model:show="logModalVisible"
        :project="logProject"
        :fetch-log="loadProjectBuildLog"
    />
</template>

<style scoped>
:deep(.h-20px) {
    height: 20px;
}
:deep(.w-20px) {
    width: 20px;
}
</style>
