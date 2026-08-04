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
import { toTagColor } from '@/utils/tagColor'
import { SP_ICONS as I } from '@/constants/streamparkIcons'
import type { DataTableColumns } from 'naive-ui'
import type { SparkApplication } from '@/types/api/spark/app.type'
import {
    fetchSparkAppGet,
    fetchSparkBackUps,
    fetchSparkConfList,
    fetchSparkConfRemove,
    fetchSparkDeleteOptLog,
    fetchSparkOptionLog,
    fetchSparkRemoveBackup,
    fetchSparkSqlList,
    fetchSparkSqlRemove,
    fetchSparkYarn,
} from '@/service'
import { resolveListData } from '@/views/system/shared/utils'
import ExecOptionModal from '@/views/shared/modals/ExecOptionModal.vue'
import SparkAppStateTag from './components/SparkAppStateTag.vue'
import SparkConfigCompareModal from './components/SparkConfigCompareModal.vue'
import SparkConfigViewModal from './components/SparkConfigViewModal.vue'
import SparkSqlCompareModal from './components/SparkSqlCompareModal.vue'
import SparkSqlViewModal from './components/SparkSqlViewModal.vue'
import { CandidateTypeEnum, ConfigTypeEnum } from '@/enums/flinkEnum'
import { AppStateEnum, DeployMode, JobTypeEnum, OperationEnum } from '@/enums/sparkEnum'
import { handleView } from './utils'
import { dateToDuration } from '@/utils/dateUtil'
import { baseUrl } from '@/utils/url'
import { useClipboard } from '@vueuse/core'

defineOptions({ name: 'SparkApplicationDetail' })

const { t } = useI18n()
const router = useRouter()
const route = useRoute()
const { copy } = useClipboard({ legacy: true })

const loading = ref(false)
const tabLoading = ref(false)
const yarnUrl = ref<string | null>(null)
const app = reactive<Partial<SparkApplication>>({})
const optionLogs = ref<Recordable[]>([])
const sparkSqlVersions = ref<Recordable[]>([])
const confVersions = ref<Recordable[]>([])
const backupRecords = ref<Recordable[]>([])
const activeTab = ref('overview')

const showConfTab = ref(false)
const showBackupTab = ref(false)

const isSqlApp = computed(() => app.jobType === JobTypeEnum.SQL)
const parsedOptions = computed(() => {
    if (!app.options) return null
    try {
        return JSON.parse(app.options) as Record<string, unknown>
    } catch {
        return null
    }
})

const sqlViewVisible = ref(false)
const sqlViewRecord = ref<Recordable | null>(null)
const sqlCompareVisible = ref(false)
const sqlCompareRecord = ref<Recordable | null>(null)

const confViewVisible = ref(false)
const confViewRecord = ref<Recordable | null>(null)
const confCompareVisible = ref(false)
const confCompareRecord = ref<Recordable | null>(null)

const execOptionVisible = ref(false)
const execOptionContent = ref('')

const sparkSqlVersionOptions = computed(() =>
    sparkSqlVersions.value.map((row) => ({
        label: `v${row.version}`,
        value: row.id,
        version: row.version,
    })),
)

const confVersionOptions = computed(() =>
    confVersions.value.map((row) => ({
        label: `v${row.version}`,
        value: row.id,
        version: row.version,
    })),
)

const appId = computed(() => route.query.appId as string | undefined)

const jobTypeLabel = computed(() => {
    const map: Record<number, string> = {
        [JobTypeEnum.JAR]: 'JAR',
        [JobTypeEnum.SQL]: 'SQL',
        [JobTypeEnum.PYSPARK]: 'PySpark',
    }
    return map[app.jobType ?? -1] ?? '-'
})

function goEdit() {
    if (app.id) router.push({ path: '/spark/app/edit', query: { appId: app.id } })
}

const appNotRunning = computed(() => app.state !== AppStateEnum.RUNNING || yarnUrl.value === null)

function openYarnProxy(clusterId: string) {
    window.open(`${baseUrl()}/proxy/yarn/${clusterId}/`)
}

function openHistoryProxy(logId: string) {
    window.open(`${baseUrl()}/proxy/history/${logId}/`)
}

function renderOperationTag(name?: string | number) {
    const labels: Record<number, string> = {
        [OperationEnum.RELEASE]: t('spark.app.detail.operationNames.release'),
        [OperationEnum.START]: t('spark.app.detail.operationNames.start'),
        [OperationEnum.STOP]: t('spark.app.detail.operationNames.stop'),
    }
    const key = Number(name)
    const label = labels[key] ?? String(name ?? '-')
    const colorMap: Record<number, string> = {
        [OperationEnum.RELEASE]: '#1890ff',
        [OperationEnum.START]: '#52c41a',
        [OperationEnum.STOP]: '#fa8c16',
    }
    return h(
        NTag,
        { size: 'small', color: toTagColor(colorMap[key] ?? '#666') },
        { default: () => label },
    )
}

function actionBtn(icon: string, tip: string, onClick?: () => void, disabled = false) {
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
                        disabled,
                        onClick: disabled ? undefined : onClick,
                    },
                    { icon: () => ionIcon(icon) },
                ),
            default: () => tip,
        },
    )
}

const optionLogColumns = computed<DataTableColumns<Recordable>>(() => [
    {
        title: t('flink.app.detail.columns.operationName'),
        key: 'optionName',
        width: 120,
        render: (row) => renderOperationTag(row.optionName),
    },
    {
        title: t('flink.app.detail.columns.clusterId'),
        key: 'clusterId',
        ellipsis: { tooltip: true },
        render(row) {
            if (!row.clusterId) return '-'
            return h(
                'a',
                {
                    class: 'cursor-pointer text-primary',
                    onClick: () => openYarnProxy(row.clusterId),
                },
                row.clusterId,
            )
        },
    },
    {
        title: t('flink.app.detail.columns.trackingUrl'),
        key: 'trackingUrl',
        ellipsis: { tooltip: true },
        render(row) {
            if (!row.trackingUrl) return '-'
            return h(
                'a',
                {
                    class: 'cursor-pointer text-primary',
                    onClick: () => openHistoryProxy(row.id),
                },
                row.trackingUrl,
            )
        },
    },
    {
        title: t('flink.app.detail.columns.startStatus'),
        key: 'success',
        width: 100,
        render(row) {
            return h(
                NTag,
                { size: 'small', type: row.success ? 'success' : 'error' },
                {
                    default: () => (row.success ? 'SUCCESS' : 'FAILED'),
                },
            )
        },
    },
    { title: t('flink.app.detail.columns.optionTime'), key: 'optionTime', width: 180 },
    {
        title: t('component.table.operation'),
        key: 'action',
        width: 120,
        render(row) {
            const actions: ReturnType<typeof h>[] = []
            if (!row.success) {
                actions.push(
                    actionBtn(I.view, t('spark.app.detail.detailTab.exception'), () => {
                        execOptionContent.value = row.exception ?? ''
                        execOptionVisible.value = true
                    }),
                )
            }
            actions.push(
                h(
                    NPopconfirm,
                    { onPositiveClick: () => handleDeleteLog(row.id) },
                    {
                        trigger: () => actionBtn(I.delete, t('common.delText'), () => {}),
                        default: () => t('spark.app.detail.detailTab.operationLogDeleteTitle'),
                    },
                ),
            )
            return h(NSpace, { size: 4 }, { default: () => actions })
        },
    },
])

function renderCandidate(candidate?: number) {
    const map: Record<number, { label: string; type: 'default' | 'success' | 'info' }> = {
        [CandidateTypeEnum.NONE]: { label: 'None', type: 'default' },
        [CandidateTypeEnum.NEW]: { label: 'New', type: 'success' },
        [CandidateTypeEnum.HISTORY]: { label: 'History', type: 'info' },
    }
    const meta = candidate != null ? map[candidate] : undefined
    if (!meta) return '-'
    return h(NTag, { size: 'small', type: meta.type }, { default: () => meta.label })
}

const sparkSqlColumns = computed<DataTableColumns<Recordable>>(() => [
    { title: t('flink.app.detail.columns.version'), key: 'version', width: 80 },
    {
        title: 'Candidate',
        key: 'candidate',
        width: 100,
        render: (row) => renderCandidate(row.candidate),
    },
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
                        actionBtn(I.view, t('spark.app.detail.detailTab.sqlDetail'), () =>
                            openSqlView(row),
                        ),
                        actionBtn(
                            I.compare,
                            t('spark.app.detail.compareSparkSql'),
                            () => openSqlCompare(row),
                            sparkSqlVersions.value.length <= 1,
                        ),
                        h(
                            NPopconfirm,
                            { onPositiveClick: () => handleDeleteSparkSql(row) },
                            {
                                trigger: () =>
                                    actionBtn(
                                        I.delete,
                                        t('common.delText'),
                                        () => {},
                                        !!row.effective,
                                    ),
                                default: () => t('spark.app.detail.detailTab.sqlDeleteTitle'),
                            },
                        ),
                    ],
                },
            )
        },
    },
])

const confColumns = computed<DataTableColumns<Recordable>>(() => [
    {
        title: t('flink.app.detail.columns.version'),
        key: 'version',
        width: 90,
        render: (row) =>
            h(
                NTag,
                { size: 'small', type: 'primary', round: true },
                { default: () => String(row.version) },
            ),
    },
    {
        title: 'Format',
        key: 'format',
        width: 110,
        render(row) {
            if (row.format === ConfigTypeEnum.YAML)
                return h(
                    NTag,
                    { size: 'small', color: toTagColor('#2db7f5') },
                    { default: () => 'yaml' },
                )
            if (row.format === ConfigTypeEnum.PROPERTIES)
                return h(
                    NTag,
                    { size: 'small', color: toTagColor('#108ee9') },
                    { default: () => 'properties' },
                )
            return '-'
        },
    },
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
                        actionBtn(I.view, t('spark.app.detail.detailTab.configDetail'), () =>
                            openConfView(row),
                        ),
                        actionBtn(
                            I.compare,
                            t('spark.app.detail.compareConfig'),
                            () => openConfCompare(row),
                            confVersions.value.length <= 1,
                        ),
                        h(
                            NPopconfirm,
                            { onPositiveClick: () => handleDeleteConf(row) },
                            {
                                trigger: () =>
                                    actionBtn(
                                        I.delete,
                                        t('common.delText'),
                                        () => {},
                                        !!row.effective,
                                    ),
                                default: () => t('spark.app.detail.detailTab.confDeleteTitle'),
                            },
                        ),
                    ],
                },
            )
        },
    },
])

const backupColumns = computed<DataTableColumns<Recordable>>(() => [
    { title: t('flink.app.detail.columns.path'), key: 'path', ellipsis: { tooltip: true } },
    { title: t('common.description'), key: 'description', ellipsis: { tooltip: true } },
    { title: t('flink.app.detail.columns.version'), key: 'version', width: 80 },
    { title: t('flink.app.detail.columns.modifyTime'), key: 'createTime', width: 180 },
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
                        actionBtn(I.copy, t('spark.app.detail.detailTab.copyPath'), () =>
                            handleCopyPath(row.path),
                        ),
                        h(
                            NPopconfirm,
                            { onPositiveClick: () => handleDeleteBackup(row.id) },
                            {
                                trigger: () => actionBtn(I.delete, t('common.delText'), () => {}),
                                default: () => t('spark.app.detail.detailTab.confBackupTitle'),
                            },
                        ),
                    ],
                },
            )
        },
    },
])

async function loadYarnIfNeeded(data: SparkApplication) {
    if (
        data.deployMode != null &&
        [DeployMode.YARN_CLIENT, DeployMode.YARN_CLUSTER].includes(data.deployMode)
    ) {
        const result = await fetchSparkYarn()
        if (result.isSuccess) yarnUrl.value = result.data ?? null
    }
}

async function checkTabVisibility() {
    if (!appId.value) return
    const [confResult, backupResult] = await Promise.all([
        fetchSparkConfList({ appId: appId.value, pageNum: 1, pageSize: 1 }),
        fetchSparkBackUps({ id: appId.value } as SparkApplication),
    ])
    showConfTab.value = confResult.isSuccess && resolveListData(confResult.data as any).total > 0
    showBackupTab.value =
        backupResult.isSuccess && Array.isArray(backupResult.data) && backupResult.data.length > 0
}

async function loadSparkSqlVersions() {
    if (!appId.value) return
    tabLoading.value = true
    try {
        const result = await fetchSparkSqlList({ appId: appId.value, pageNum: 1, pageSize: 200 })
        if (result.isSuccess) sparkSqlVersions.value = resolveListData(result.data).records
    } catch {
        window.$message?.error(t('sys.api.apiRequestFailed'))
    } finally {
        tabLoading.value = false
    }
}

async function loadConfVersions() {
    if (!appId.value) return
    tabLoading.value = true
    try {
        const result = await fetchSparkConfList({ appId: appId.value, pageNum: 1, pageSize: 200 })
        if (result.isSuccess) confVersions.value = resolveListData<Recordable>(result.data).records
    } catch {
        window.$message?.error(t('sys.api.apiRequestFailed'))
    } finally {
        tabLoading.value = false
    }
}

async function loadBackups() {
    if (!appId.value) return
    tabLoading.value = true
    try {
        const result = await fetchSparkBackUps({ id: appId.value } as SparkApplication)
        if (result.isSuccess) backupRecords.value = (result.data as Recordable[]) ?? []
    } catch {
        window.$message?.error(t('sys.api.apiRequestFailed'))
    } finally {
        tabLoading.value = false
    }
}

function openSqlView(record: Recordable) {
    sqlViewRecord.value = record
    sqlViewVisible.value = true
}

function openSqlCompare(record: Recordable) {
    sqlCompareRecord.value = record
    sqlCompareVisible.value = true
}

function openConfView(record: Recordable) {
    confViewRecord.value = record
    confViewVisible.value = true
}

function openConfCompare(record: Recordable) {
    confCompareRecord.value = record
    confCompareVisible.value = true
}

async function handleDeleteSparkSql(record: Recordable) {
    if (!appId.value) return
    const result = await fetchSparkSqlRemove({ id: record.id, appId: appId.value })
    if (result.isSuccess) {
        window.$message?.success(t('common.operationSuccess'))
        loadSparkSqlVersions()
    }
}

async function handleDeleteConf(record: Recordable) {
    const result = await fetchSparkConfRemove({ id: record.id })
    if (result.isSuccess) {
        window.$message?.success(t('common.operationSuccess'))
        loadConfVersions()
        checkTabVisibility()
    }
}

async function handleDeleteBackup(id: string) {
    const result = await fetchSparkRemoveBackup(id)
    if (result.isSuccess) {
        window.$message?.success(t('common.operationSuccess'))
        loadBackups()
        checkTabVisibility()
    }
}

function handleCopyPath(path: string) {
    try {
        copy(path)
        window.$message?.success(t('spark.app.detail.detailTab.copySuccess'))
    } catch {
        window.$message?.error(t('spark.app.detail.detailTab.copyFail'))
    }
}

async function loadOptionLogs() {
    if (!appId.value) return
    tabLoading.value = true
    try {
        const result = await fetchSparkOptionLog({ id: appId.value } as SparkApplication)
        if (result.isSuccess) optionLogs.value = (result.data as Recordable[]) ?? []
    } catch {
        window.$message?.error(t('sys.api.apiRequestFailed'))
    } finally {
        tabLoading.value = false
    }
}

async function loadApp() {
    if (!appId.value) {
        router.back()
        return
    }
    loading.value = true
    try {
        const result = await fetchSparkAppGet({ id: appId.value })
        if (!result.isSuccess || !result.data)
            throwApiFailure(result, t('sys.api.apiRequestFailed'))
        Object.assign(app, result.data)
        if (Object.keys(app).length > 0) await loadYarnIfNeeded(result.data)
        await checkTabVisibility()
    } catch (e: any) {
        showCatchError(e, t('sys.api.apiRequestFailed'))
        router.back()
    } finally {
        loading.value = false
    }
}

async function handleDeleteLog(id: string) {
    const result = await fetchSparkDeleteOptLog(id)
    if (result.isSuccess) {
        window.$message?.success(t('common.operationSuccess'))
        loadOptionLogs()
    }
}

function handleWebUi() {
    handleView(app as SparkApplication, yarnUrl.value)
}

watch(activeTab, (tab) => {
    if (tab === 'optionLog') loadOptionLogs()
    else if (tab === 'sparkSql') loadSparkSqlVersions()
    else if (tab === 'configuration') loadConfVersions()
    else if (tab === 'backup') loadBackups()
})

let pollTimer: ReturnType<typeof setInterval> | null = null

onMounted(() => {
    loadApp()
    pollTimer = setInterval(loadApp, 5000)
})

onUnmounted(() => {
    if (pollTimer) clearInterval(pollTimer)
})
</script>

<template>
    <n-card :bordered="false" class="h-full">
        <n-spin :show="loading">
            <div class="mb-16px flex flex-wrap items-center justify-between gap-12px">
                <span class="text-18px font-medium"
                    >{{ t('spark.app.detail.detailTitle') }} — {{ app.appName }}</span
                >
                <n-space>
                    <n-button type="primary" :disabled="appNotRunning" @click="handleWebUi">
                        <template #icon>
                            <n-icon><IonIcon :name="I.cloud" /></n-icon>
                        </template>
                        {{ t('spark.app.detail.webUI') }}
                    </n-button>
                    <n-button v-auth="'app:update'" type="primary" ghost @click="goEdit">
                        <template #icon>
                            <n-icon><IonIcon :name="I.edit" /></n-icon>
                        </template>
                        {{ t('spark.app.operation.edit') }}
                    </n-button>
                    <n-button circle @click="router.back()">
                        <template #icon>
                            <n-icon><IonIcon :name="I.back" /></n-icon>
                        </template>
                    </n-button>
                </n-space>
            </div>

            <n-tabs v-model:value="activeTab" type="line" animated>
                <n-tab-pane name="overview" :tab="t('common.detailText')">
                    <n-descriptions bordered :column="2" label-placement="left">
                        <n-descriptions-item :label="t('spark.app.id')">
                            <n-text copyable>{{ app.id }}</n-text>
                        </n-descriptions-item>
                        <n-descriptions-item :label="t('spark.app.appName')">
                            {{ app.appName }}
                        </n-descriptions-item>
                        <n-descriptions-item :label="t('spark.app.jobType')">
                            {{ jobTypeLabel }}
                        </n-descriptions-item>
                        <n-descriptions-item :label="t('spark.app.status')">
                            <SparkAppStateTag option="state" :data="app" />
                        </n-descriptions-item>
                        <n-descriptions-item
                            v-if="app.jobType !== JobTypeEnum.SQL"
                            :label="t('spark.app.resource')"
                        >
                            {{ app.jar || '-' }}
                        </n-descriptions-item>
                        <n-descriptions-item :label="t('spark.app.startTime')">
                            {{ app.startTime || '-' }}
                        </n-descriptions-item>
                        <n-descriptions-item :label="t('spark.app.endTime')">
                            {{ app.endTime || '-' }}
                        </n-descriptions-item>
                        <n-descriptions-item :label="t('spark.app.duration')">
                            {{ app.duration != null ? dateToDuration(app.duration) : '-' }}
                        </n-descriptions-item>
                        <n-descriptions-item :label="t('common.description')" :span="2">
                            {{ app.description || '-' }}
                        </n-descriptions-item>
                    </n-descriptions>
                </n-tab-pane>
                <n-tab-pane
                    name="optionLog"
                    :tab="t('spark.app.detail.detailTab.detailTabName.operationLog')"
                >
                    <n-data-table
                        :loading="tabLoading"
                        :columns="optionLogColumns"
                        :data="optionLogs"
                        :row-key="(row: Recordable) => String(row.id)"
                        size="small"
                    />
                </n-tab-pane>
                <n-tab-pane
                    v-if="parsedOptions"
                    name="options"
                    :tab="t('spark.app.detail.detailTab.detailTabName.option')"
                >
                    <n-descriptions bordered :column="2" label-placement="top">
                        <n-descriptions-item
                            v-for="(value, key) in parsedOptions"
                            :key="String(key)"
                            :label="String(key)"
                        >
                            {{ value }}
                        </n-descriptions-item>
                    </n-descriptions>
                </n-tab-pane>
                <n-tab-pane
                    v-if="showConfTab"
                    name="configuration"
                    :tab="t('spark.app.detail.detailTab.detailTabName.configuration')"
                >
                    <n-data-table
                        :loading="tabLoading"
                        :columns="confColumns"
                        :data="confVersions"
                        :row-key="(row: Recordable) => String(row.id)"
                        size="small"
                    />
                </n-tab-pane>
                <n-tab-pane
                    v-if="isSqlApp"
                    name="sparkSql"
                    :tab="t('spark.app.detail.detailTab.detailTabName.sparkSql')"
                >
                    <n-data-table
                        :loading="tabLoading"
                        :columns="sparkSqlColumns"
                        :data="sparkSqlVersions"
                        :row-key="(row: Recordable) => String(row.id ?? row.version)"
                        size="small"
                    />
                </n-tab-pane>
                <n-tab-pane
                    v-if="showBackupTab"
                    name="backup"
                    :tab="t('spark.app.detail.detailTab.detailTabName.backup')"
                >
                    <n-data-table
                        :loading="tabLoading"
                        :columns="backupColumns"
                        :data="backupRecords"
                        :row-key="(row: Recordable) => String(row.id)"
                        size="small"
                    />
                </n-tab-pane>
            </n-tabs>
        </n-spin>

        <SparkSqlViewModal
            v-model:show="sqlViewVisible"
            :record-id="sqlViewRecord?.id"
            :app-id="appId"
            :version="sqlViewRecord?.version"
        />
        <SparkSqlCompareModal
            v-model:show="sqlCompareVisible"
            :app-id="appId"
            :source-record="sqlCompareRecord"
            :versions="sparkSqlVersionOptions"
        />
        <SparkConfigViewModal
            v-model:show="confViewVisible"
            :config-id="confViewRecord?.id"
            :version="confViewRecord?.version"
        />
        <SparkConfigCompareModal
            v-model:show="confCompareVisible"
            :source-id="confCompareRecord?.id"
            :source-version="confCompareRecord?.version"
            :versions="confVersionOptions"
        />
        <ExecOptionModal v-model:show="execOptionVisible" :content="execOptionContent" />
    </n-card>
</template>

<style scoped>
.text-primary {
    color: var(--primary-color);
    text-decoration: none;
}
</style>
