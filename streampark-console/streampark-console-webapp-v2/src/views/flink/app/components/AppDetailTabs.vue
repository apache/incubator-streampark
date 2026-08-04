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
import type { DataTableColumns } from 'naive-ui'
import type { AppListRecord } from '@/types/api/flink/app.type'
import {
    fetchBackUps,
    fetchDeleteOperationLog,
    fetchFlinkSqlList,
    fetchListVer,
    fetchOptionLog,
    fetchRemoveBackup,
    fetchRemoveConf,
    fetchRemoveFlinkSql,
    fetchRemoveSavePoint,
    fetchSavePointHistory,
} from '@/service'
import { resolveListData } from '@/views/system/shared/utils'
import ExecOptionModal from '@/views/shared/modals/ExecOptionModal.vue'
import ConfigCompareModal from './ConfigCompareModal.vue'
import ConfigViewModal from './ConfigViewModal.vue'
import FlinkSqlCompareModal from './FlinkSqlCompareModal.vue'
import FlinkSqlViewModal from './FlinkSqlViewModal.vue'
import { CandidateTypeEnum, JobTypeEnum, OperationEnum } from '@/enums/flinkEnum'
import { toTagColor } from '@/utils/tagColor'
import { useClipboard } from '@vueuse/core'

const props = defineProps<{
    app: Partial<AppListRecord>
}>()

const { t } = useI18n()
const { copy } = useClipboard({ legacy: true })
const activeTab = ref('optionLog')

const optionLogs = ref<Recordable[]>([])
const backups = ref<Recordable[]>([])
const savepoints = ref<Recordable[]>([])
const configs = ref<Recordable[]>([])
const flinkSqlVersions = ref<Recordable[]>([])
const loading = ref(false)

const isSqlApp = computed(() => props.app.jobType === JobTypeEnum.SQL)

const configViewVisible = ref(false)
const configViewVersion = ref<string | number | null>(null)
const compareVisible = ref(false)
const compareSourceVersion = ref<string | number | null>(null)
const sqlViewVisible = ref(false)
const sqlViewRecord = ref<Recordable | null>(null)
const sqlCompareVisible = ref(false)
const sqlCompareRecord = ref<Recordable | null>(null)
const execOptionVisible = ref(false)
const execOptionContent = ref('')

const parsedOptions = computed(() => {
    try {
        return JSON.parse(props.app.options || '{}') as Record<string, unknown>
    } catch {
        return {}
    }
})

const configVersionOptions = computed(() =>
    configs.value.map((row) => ({
        label: `v${row.version}`,
        value: row.version,
    })),
)

const flinkSqlVersionOptions = computed(() =>
    flinkSqlVersions.value.map((row) => ({
        label: `v${row.version}`,
        value: row.id,
        version: row.version,
        effective: row.effective,
        candidate: row.candidate,
    })),
)

function configActionBtn(
    icon: string,
    tip: string,
    onClick: () => void,
    cls?: string,
    disabled = false,
) {
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
                        disabled,
                        onClick: disabled ? undefined : onClick,
                    },
                    {
                        icon: () => ionIcon(icon),
                    },
                ),
            default: () => tip,
        },
    )
}

const configColumns = computed<DataTableColumns<Recordable>>(() => [
    { title: t('flink.app.detail.columns.version'), key: 'version', width: 80 },
    { title: t('flink.app.detail.columns.confFormat'), key: 'format', width: 100 },
    {
        title: t('flink.app.detail.columns.effective'),
        key: 'effective',
        width: 100,
        render(row) {
            return h(
                NTag,
                { size: 'small', type: row.effective ? 'success' : 'default' },
                {
                    default: () => String(row.effective ?? false),
                },
            )
        },
    },
    { title: t('flink.app.detail.columns.modifyTime'), key: 'createTime', width: 180 },
    {
        title: t('component.table.operation'),
        key: 'action',
        width: 140,
        render(row) {
            return h(
                NSpace,
                { size: 4 },
                {
                    default: () => [
                        configActionBtn(I.view, t('common.detail'), () =>
                            openConfigView(row.version),
                        ),
                        configActionBtn(I.compare, t('flink.app.flinkSql.compare'), () =>
                            openCompare(row.version),
                        ),
                        h(
                            NPopconfirm,
                            { onPositiveClick: () => handleDeleteConf(row.id) },
                            {
                                trigger: () =>
                                    configActionBtn(I.delete, t('common.delText'), () => {}),
                                default: () => t('common.confirmDelete'),
                            },
                        ),
                    ],
                },
            )
        },
    },
])

const flinkSqlColumns = computed<DataTableColumns<Recordable>>(() => [
    { title: t('flink.app.detail.columns.version'), key: 'version', width: 80 },
    {
        title: t('flink.app.detail.columns.effective'),
        key: 'effective',
        width: 100,
        render(row) {
            return h(
                NTag,
                { size: 'small', type: row.effective ? 'success' : 'default' },
                {
                    default: () => String(row.effective ?? false),
                },
            )
        },
    },
    {
        title: t('flink.app.detail.columns.candidate'),
        key: 'candidate',
        width: 120,
        render(row) {
            if ([CandidateTypeEnum.NEW, CandidateTypeEnum.HISTORY].includes(row.candidate))
                return h(
                    NTag,
                    { size: 'small', type: 'info' },
                    { default: () => t('flink.app.detail.candidate') },
                )
            return '-'
        },
    },
    { title: t('flink.app.detail.columns.modifyTime'), key: 'createTime', width: 180 },
    {
        title: t('component.table.operation'),
        key: 'action',
        width: 140,
        render(row) {
            return h(
                NSpace,
                { size: 4 },
                {
                    default: () => [
                        configActionBtn(I.view, t('flink.app.detail.detailTab.sqlDetail'), () =>
                            openSqlView(row),
                        ),
                        configActionBtn(
                            I.compare,
                            t('flink.app.detail.compareFlinkSql'),
                            () => openSqlCompare(row),
                            undefined,
                            flinkSqlVersions.value.length <= 1,
                        ),
                        h(
                            NPopconfirm,
                            { onPositiveClick: () => handleDeleteFlinkSql(row) },
                            {
                                trigger: () =>
                                    configActionBtn(
                                        I.delete,
                                        t('common.delText'),
                                        () => {},
                                        undefined,
                                        !!row.effective,
                                    ),
                                default: () => t('flink.app.detail.detailTab.sqlDeleteTitle'),
                            },
                        ),
                    ],
                },
            )
        },
    },
])

const optionLogColumns = computed<DataTableColumns<Recordable>>(() => [
    { title: t('flink.app.detail.optionTime'), key: 'optionTime', width: 180 },
    {
        title: t('flink.app.detail.optionName'),
        key: 'optionName',
        render(row) {
            const tags: Record<number, { color: string; label: string }> = {
                [OperationEnum.RELEASE]: {
                    color: '#2080f0',
                    label: t('flink.app.detail.operationNames.release'),
                },
                [OperationEnum.START]: {
                    color: '#18a058',
                    label: t('flink.app.detail.operationNames.start'),
                },
                [OperationEnum.SAVEPOINT]: {
                    color: '#36cfc9',
                    label: t('flink.app.detail.operationNames.savepoint'),
                },
                [OperationEnum.CANCEL]: {
                    color: '#f0a020',
                    label: t('flink.app.detail.operationNames.cancel'),
                },
            }
            const meta = tags[row.optionName as number]
            return meta
                ? h(
                      NTag,
                      { size: 'small', color: toTagColor(meta.color) },
                      { default: () => meta.label },
                  )
                : String(row.optionName ?? '-')
        },
    },
    { title: t('flink.app.detail.optionUser'), key: 'optionUser' },
    {
        title: t('flink.app.detail.columns.startStatus'),
        key: 'success',
        width: 100,
        render(row) {
            return h(
                NTag,
                {
                    size: 'small',
                    type: row.success ? 'success' : 'error',
                },
                {
                    default: () =>
                        row.success
                            ? t('flink.app.detail.startStatusText.success')
                            : t('flink.app.detail.startStatusText.failed'),
                },
            )
        },
    },
    {
        title: t('component.table.operation'),
        key: 'action',
        width: 120,
        render(row) {
            return h(
                NSpace,
                { size: 4 },
                {
                    default: () =>
                        [
                            !row.success
                                ? configActionBtn(
                                      I.view,
                                      t('flink.app.detail.detailTab.exception'),
                                      () => openExecOption(row),
                                  )
                                : null,
                            h(
                                NPopconfirm,
                                {
                                    onPositiveClick: () => handleDeleteLog(row.id),
                                },
                                {
                                    trigger: () =>
                                        h(
                                            NButton,
                                            { quaternary: true, size: 'small' },
                                            { default: () => t('common.delText') },
                                        ),
                                    default: () => t('common.confirmDelete'),
                                },
                            ),
                        ].filter(Boolean),
                },
            )
        },
    },
])

const backupColumns = computed<DataTableColumns<Recordable>>(() => [
    { title: t('flink.app.detail.version'), key: 'version', width: 80 },
    { title: t('flink.app.detail.description'), key: 'description', ellipsis: { tooltip: true } },
    { title: t('common.createTime'), key: 'createTime', width: 180 },
    {
        title: t('component.table.operation'),
        key: 'action',
        width: 80,
        render(row) {
            return h(
                NPopconfirm,
                {
                    onPositiveClick: () => handleDeleteBackup(row.id),
                },
                {
                    trigger: () =>
                        h(
                            NButton,
                            { quaternary: true, size: 'small' },
                            { default: () => t('common.delText') },
                        ),
                    default: () => t('common.confirmDelete'),
                },
            )
        },
    },
])

const savepointColumns = computed<DataTableColumns<Recordable>>(() => [
    { title: t('flink.app.detail.path'), key: 'path', ellipsis: { tooltip: true } },
    { title: t('flink.app.detail.triggerTime'), key: 'triggerTime', width: 180 },
    { title: t('flink.app.detail.checkPointType'), key: 'checkPointType', width: 120 },
    {
        title: t('component.table.operation'),
        key: 'action',
        width: 100,
        render(row) {
            return h(
                NSpace,
                { size: 4 },
                {
                    default: () => [
                        configActionBtn(I.copy, t('flink.app.detail.detailTab.copyPath'), () =>
                            handleCopyPath(row.path),
                        ),
                        h(
                            NPopconfirm,
                            {
                                onPositiveClick: () => handleDeleteSavepoint(row),
                            },
                            {
                                trigger: () =>
                                    configActionBtn(I.delete, t('common.delText'), () => {}),
                                default: () => t('flink.app.detail.detailTab.pointDeleteTitle'),
                            },
                        ),
                    ],
                },
            )
        },
    },
])

function openConfigView(version: string | number) {
    configViewVersion.value = version
    configViewVisible.value = true
}

function openCompare(version: string | number) {
    compareSourceVersion.value = version
    compareVisible.value = true
}

function openSqlView(record: Recordable) {
    sqlViewRecord.value = record
    sqlViewVisible.value = true
}

function openSqlCompare(record: Recordable) {
    sqlCompareRecord.value = record
    sqlCompareVisible.value = true
}

function openExecOption(record: Recordable) {
    execOptionContent.value = record.exception || ''
    execOptionVisible.value = true
}

async function handleCopyPath(path?: string) {
    if (!path) {
        window.$message?.error(t('flink.app.detail.detailTab.copyFail'))
        return
    }
    try {
        await copy(path)
        window.$message?.success(t('flink.app.detail.detailTab.copySuccess'))
    } catch {
        window.$message?.error(t('flink.app.detail.detailTab.copyFail'))
    }
}

async function loadTabData() {
    if (!props.app.id) return
    loading.value = true
    try {
        const appId = props.app.id
        if (activeTab.value === 'optionLog') {
            const result = await fetchOptionLog({ id: appId, pageNum: 1, pageSize: 100 })
            if (result.isSuccess) optionLogs.value = resolveListData(result.data).records
        } else if (activeTab.value === 'backup') {
            const result = await fetchBackUps({ id: appId, pageNum: 1, pageSize: 100 })
            if (result.isSuccess) backups.value = resolveListData(result.data).records
        } else if (activeTab.value === 'savepoint') {
            const result = await fetchSavePointHistory({ appId, pageNum: 1, pageSize: 100 })
            if (result.isSuccess) savepoints.value = resolveListData(result.data).records
        } else if (activeTab.value === 'config') {
            const result = await fetchListVer({ appId, pageNum: 1, pageSize: 200 })
            if (result.isSuccess) configs.value = resolveListData(result.data).records
        } else if (activeTab.value === 'flinkSql') {
            const result = await fetchFlinkSqlList({ appId, pageNum: 1, pageSize: 200 })
            if (result.isSuccess) flinkSqlVersions.value = resolveListData(result.data).records
        }
    } catch {
        window.$message?.error(t('sys.api.apiRequestFailed'))
    } finally {
        loading.value = false
    }
}

async function handleDeleteLog(id: string) {
    const result = await fetchDeleteOperationLog(id)
    if (result.isSuccess) {
        window.$message?.success(t('common.operationSuccess'))
        loadTabData()
    }
}

async function handleDeleteBackup(id: string) {
    const result = await fetchRemoveBackup(id)
    if (result.isSuccess) {
        window.$message?.success(t('common.operationSuccess'))
        loadTabData()
    }
}

async function handleDeleteConf(id: string) {
    const result = await fetchRemoveConf({ id })
    if (result.isSuccess) {
        window.$message?.success(t('common.operationSuccess'))
        loadTabData()
    }
}

async function handleDeleteFlinkSql(record: Recordable) {
    if (!props.app.id) return
    const result = await fetchRemoveFlinkSql({ id: record.id, appId: props.app.id })
    if (result.isSuccess) {
        window.$message?.success(t('common.operationSuccess'))
        loadTabData()
    }
}

async function handleDeleteSavepoint(record: Recordable) {
    if (!props.app.id) return
    const result = await fetchRemoveSavePoint({ id: record.id, appId: props.app.id })
    if (result.isSuccess) {
        window.$message?.success(t('common.operationSuccess'))
        loadTabData()
    }
}

watch(
    () => [props.app.id, activeTab.value] as const,
    () => loadTabData(),
    { immediate: true },
)
</script>

<template>
    <n-tabs v-model:value="activeTab" type="line" animated>
        <n-tab-pane name="config" :tab="t('flink.app.detail.conf')">
            <n-data-table
                :loading="loading"
                :columns="configColumns"
                :data="configs"
                :row-key="(row: Recordable) => String(row.id ?? row.version)"
                size="small"
            />
        </n-tab-pane>
        <n-tab-pane v-if="isSqlApp" name="flinkSql" :tab="t('flink.app.flinkSqlLabel')">
            <n-data-table
                :loading="loading"
                :columns="flinkSqlColumns"
                :data="flinkSqlVersions"
                :row-key="(row: Recordable) => String(row.id ?? row.version)"
                size="small"
            />
        </n-tab-pane>
        <n-tab-pane name="optionLog" :tab="t('flink.app.detail.optionLog')">
            <n-data-table
                :loading="loading"
                :columns="optionLogColumns"
                :data="optionLogs"
                :row-key="(row: Recordable) => String(row.id)"
                size="small"
            />
        </n-tab-pane>
        <n-tab-pane name="options" :tab="t('flink.app.detail.detailTab.detailTabName.option')">
            <n-descriptions bordered size="small" label-placement="top" :column="2">
                <n-descriptions-item
                    v-for="(value, key) in parsedOptions"
                    :key="String(key)"
                    :label="String(key)"
                >
                    {{ value }}
                </n-descriptions-item>
            </n-descriptions>
        </n-tab-pane>
        <n-tab-pane name="backup" :tab="t('flink.app.detail.backup')">
            <n-data-table
                :loading="loading"
                :columns="backupColumns"
                :data="backups"
                :row-key="(row: Recordable) => String(row.id)"
                size="small"
            />
        </n-tab-pane>
        <n-tab-pane name="savepoint" :tab="t('flink.app.detail.savePoint')">
            <n-data-table
                :loading="loading"
                :columns="savepointColumns"
                :data="savepoints"
                :row-key="(row: Recordable) => String(row.id ?? row.path ?? '')"
                size="small"
            />
        </n-tab-pane>
    </n-tabs>

    <ConfigViewModal
        v-model:show="configViewVisible"
        :version="configViewVersion"
        :app-id="app.id"
    />
    <ConfigCompareModal
        v-model:show="compareVisible"
        :app-id="app.id"
        :source-version="compareSourceVersion"
        :versions="configVersionOptions"
    />
    <FlinkSqlViewModal
        v-model:show="sqlViewVisible"
        :record-id="sqlViewRecord?.id"
        :app-id="app.id"
        :version="sqlViewRecord?.version"
    />
    <FlinkSqlCompareModal
        v-model:show="sqlCompareVisible"
        :app-id="app.id"
        :source-record="sqlCompareRecord"
        :versions="flinkSqlVersionOptions"
    />
    <ExecOptionModal v-model:show="execOptionVisible" :content="execOptionContent" />
</template>
