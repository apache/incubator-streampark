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
import type { DataTableColumns, DropdownOption } from 'naive-ui'
import type { SparkApplication } from '@/types/api/spark/app.type'
import { usePermission } from '@/hooks'
import {
    fetchBuildSparkApp,
    fetchCheckSparkAppStart,
    fetchCheckSparkName,
    fetchAppOwners,
    fetchSparkAppCancel,
    fetchSparkAppCopy,
    fetchSparkAppForcedStop,
    fetchSparkAppRecord,
    fetchSparkAppRemove,
    fetchSparkAppStart,
    fetchSparkBuildDetail,
    fetchSparkMapping,
    fetchSparkYarn,
} from '@/service'
import { unwrapBooleanResult } from '@/utils/apiResult'
import { resolveListData } from '@/views/system/shared/utils'
import AppDashboard from './components/AppDashboard.vue'
import BuildDrawer from './components/BuildDrawer.vue'
import LogModal from './components/LogModal.vue'
import SparkAppStateTag from './components/SparkAppStateTag.vue'
import AppListNameCell from '@/views/shared/components/AppListNameCell.vue'
import AppTagsCell from '@/views/shared/components/AppTagsCell.vue'
import {
    createSparkOptionStateMap,
    createSparkReleaseStateMap,
    createSparkStateMap,
    sparkBuildStatusMap,
} from './shared/constants'
import { dateToDuration } from '@/utils/dateUtil'
import {
    canAbortSpark,
    canCancelSpark,
    canDeleteSpark,
    handleIsStart,
    handleView,
} from '@/views/spark/app/utils'
import { ReleaseStateEnum } from '@/enums/flinkEnum'
import {
    AppExistsStateEnum,
    AppStateEnum,
    DeployMode,
    JobTypeEnum,
    OptionStateEnum,
} from '@/enums/sparkEnum'
import { useAdaptivePolling } from '@/hooks/useAdaptivePolling'

defineOptions({ name: 'SparkApplication' })

const { t } = useI18n()
const { hasPermission } = usePermission()
const router = useRouter()

const dashboardRef = ref<InstanceType<typeof AppDashboard> | null>(null)
const loading = ref(false)
const tableData = ref<SparkApplication[]>([])
const yarnUrl = ref<string | null>(null)
const tagsOptions = ref<string[]>([])
const users = ref<Array<{ userId: string; username: string; nickName?: string }>>([])

const searchForm = reactive({
    appName: '',
    tags: null as string | null,
    jobType: null as number | null,
    userId: null as string | null,
    stateArray: [] as number[],
})

const buildDrawerVisible = ref(false)
const buildAppId = ref<string | null>(null)
const logModalVisible = ref(false)
const logApp = ref<SparkApplication | null>(null)

const copyModalVisible = ref(false)
const copyApp = ref<SparkApplication | null>(null)
const copyAppName = ref('')

const mappingModalVisible = ref(false)
const mappingApp = ref<SparkApplication | null>(null)
const mappingForm = reactive({ clusterId: '', jobId: '' })

const titleLenRef = reactive({
    maxState: '',
    maxRelease: '',
    maxBuild: '',
})

const appNameColumnWidth = ref(250)
const releaseColumnWidth = ref(190)

const optionApps = {
    starting: new Map<string, number>(),
    stopping: new Map<string, number>(),
}

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

const jobTypeOptions = [
    { label: 'JAR', value: JobTypeEnum.JAR },
    { label: 'SQL', value: JobTypeEnum.SQL },
    { label: 'PySpark', value: JobTypeEnum.PYSPARK },
]

const stateOptions = computed(() => {
    const stateMap = createSparkStateMap(t)
    return [
        AppStateEnum.ADDED,
        AppStateEnum.STARTING,
        AppStateEnum.RUNNING,
        AppStateEnum.FAILED,
        AppStateEnum.STOPPING,
        AppStateEnum.KILLED,
        AppStateEnum.FINISHED,
        AppStateEnum.LOST,
    ].map((value) => ({
        label: stateMap[value]?.title ?? String(value),
        value,
    }))
})

function actionBtn(
    icon: string,
    tip: string,
    onClick?: () => void,
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
                    { icon: () => ionIcon(icon) },
                ),
            default: () => tip,
        },
    )
}

function updateTitleLens(records: SparkApplication[]) {
    const stateMap = createSparkStateMap(t)
    const optionStateMap = createSparkOptionStateMap(t)
    const releaseStateMap = createSparkReleaseStateMap(t)
    const lens = { maxState: '', maxRelease: '', maxBuild: '' }

    records.forEach((cur) => {
        const { state, optionState, release, buildStatus } = cur
        if (optionState === OptionStateEnum.NONE) {
            const stateStr = state != null ? stateMap[state]?.title : undefined
            if (stateStr && stateStr.length > lens.maxState.length) lens.maxState = stateStr
        } else {
            const stateStr = optionState != null ? optionStateMap[optionState]?.title : undefined
            if (stateStr && stateStr.length > lens.maxState.length) lens.maxState = stateStr
        }
        const releaseStr = release != null ? releaseStateMap[release]?.title : undefined
        if (releaseStr && releaseStr.length > lens.maxRelease.length) lens.maxRelease = releaseStr
        const buildStr = buildStatus != null ? sparkBuildStatusMap[buildStatus]?.title : undefined
        if (buildStr && buildStr.length > lens.maxBuild.length) lens.maxBuild = buildStr
    })

    Object.assign(titleLenRef, lens)
}

function cleanupOptionApps(records: SparkApplication[]) {
    const timestamp = Date.now()
    records.forEach((row) => {
        if (row.optionState !== OptionStateEnum.NONE || !row.id) return
        if (optionApps.starting.get(row.id) && timestamp - optionApps.starting.get(row.id)! > 4000)
            optionApps.starting.delete(row.id)
        if (optionApps.stopping.get(row.id) && timestamp - optionApps.stopping.get(row.id)! > 2000)
            optionApps.stopping.delete(row.id)
    })
}

function openCopyModal(app: SparkApplication) {
    copyApp.value = app
    copyAppName.value = `${app.appName}_copy`
    copyModalVisible.value = true
}

async function submitCopy() {
    if (!copyApp.value?.id || !copyAppName.value.trim()) {
        window.$message?.warning(t('spark.app.addAppTips.appNameNotValid'))
        return
    }
    const name = copyAppName.value.trim()
    const checkResult = await fetchCheckSparkName({ appName: name })
    if (!checkResult.isSuccess) {
        showResultError(checkResult, t('sys.api.apiRequestFailed'))
        return
    }
    const code = Number(checkResult.data)
    if (code !== 0) {
        const msgMap: Record<number, string> = {
            1: t('spark.app.addAppTips.appNameNotUniqueMessage'),
            2: t('spark.app.addAppTips.appNameExistsInYarnMessage'),
            3: t('spark.app.addAppTips.appNameExistsInK8sMessage'),
        }
        window.$message?.warning(msgMap[code] || t('spark.app.addAppTips.appNameNotValid'))
        return
    }
    const result = await fetchSparkAppCopy({ id: copyApp.value.id, appName: name })
    const { ok, message } = unwrapBooleanResult(result.data, result.message)
    if (result.isSuccess && ok) {
        window.$message?.success(t('spark.app.operation.copySuccess'))
        copyModalVisible.value = false
        handlePageDataReload(false)
    } else {
        const msg = (message || t('spark.app.operation.copyFail')).replaceAll(/\[StreamPark]/g, '')
        window.$message?.error(msg)
    }
}

function openMappingModal(app: SparkApplication) {
    mappingApp.value = app
    mappingForm.clusterId = ''
    mappingForm.jobId = ''
    mappingModalVisible.value = true
}

async function submitMapping() {
    if (!mappingApp.value?.id) {
        return
    }
    const clusterId = mappingForm.clusterId.trim() || mappingForm.jobId.trim()
    if (!clusterId) {
        window.$message?.warning(t('spark.app.operation.mappingJobIdRequired'))
        return
    }
    const result = await fetchSparkMapping({
        id: mappingApp.value.id,
        clusterId,
    })
    if (result.isSuccess) {
        window.$message?.success(t('spark.app.operation.mappingSuccess'))
        mappingModalVisible.value = false
        handlePageDataReload(true)
    } else {
        showResultError(result, t('sys.api.apiRequestFailed'))
    }
}

function canMapping(row: SparkApplication) {
    return (
        row.state != null &&
        [
            AppStateEnum.ADDED,
            AppStateEnum.FAILED,
            AppStateEnum.STOPPING,
            AppStateEnum.KILLED,
            AppStateEnum.SUCCEEDED,
            AppStateEnum.FINISHED,
            AppStateEnum.LOST,
        ].includes(row.state)
    )
}

function buildRowActions(row: SparkApplication) {
    const actions: ReturnType<typeof h>[] = []

    if (hasPermission('app:update')) {
        actions.push(
            actionBtn(
                I.edit,
                t('spark.app.operation.edit'),
                () => {
                    sessionStorage.setItem('sparkAppPageNo', String(pagination.page))
                    router.push({ path: '/spark/app/edit', query: { appId: row.id } })
                },
                'e2e-sparkapp-edit-btn',
            ),
        )
    }
    if (
        hasPermission('app:release') &&
        row.release != null &&
        [
            ReleaseStateEnum.FAILED,
            ReleaseStateEnum.NEED_RELEASE,
            ReleaseStateEnum.NEED_ROLLBACK,
        ].includes(row.release) &&
        row.optionState === OptionStateEnum.NONE
    ) {
        actions.push(
            actionBtn(
                I.release,
                t('spark.app.operation.release'),
                () => handleRelease(row),
                'e2e-sparkapp-release-btn',
            ),
        )
    }
    if (
        hasPermission('app:release') &&
        ((row.release != null &&
            [ReleaseStateEnum.FAILED, ReleaseStateEnum.RELEASING].includes(row.release)) ||
            row.optionState === OptionStateEnum.RELEASING)
    ) {
        actions.push(
            actionBtn(
                I.releaseDetail,
                t('spark.app.operation.releaseDetail'),
                () => {
                    buildAppId.value = row.id ?? null
                    buildDrawerVisible.value = true
                },
                'e2e-sparkapp-build-detail-btn',
            ),
        )
    }
    if (hasPermission('app:start') && handleIsStart(row, optionApps)) {
        actions.push(
            actionBtn(
                I.start,
                t('spark.app.operation.start'),
                () => handleStartCheck(row),
                'e2e-sparkapp-startup-btn',
            ),
        )
    }
    if (hasPermission('app:cancel') && canCancelSpark(row)) {
        actions.push(
            actionBtn(
                I.pause,
                t('spark.app.operation.cancel'),
                () => confirmCancel(row),
                'e2e-sparkapp-cancel-btn',
            ),
        )
    }
    if (hasPermission('app:detail')) {
        actions.push(
            actionBtn(
                I.detail,
                t('spark.app.operation.detail'),
                () => {
                    router.push(`/spark/app/detail?appId=${row.id}`)
                },
                'e2e-sparkapp-detail-btn',
            ),
        )
    }

    const dropdownOptions: DropdownOption[] = []
    if (hasPermission('app:detail')) {
        dropdownOptions.push({
            label: t('spark.app.operation.startLog'),
            key: 'log',
            icon: () => ionIcon(I.code),
        })
    }
    if (hasPermission('app:cancel') && canAbortSpark(row)) {
        dropdownOptions.push({
            label: t('flink.app.operation.abort'),
            key: 'abort',
            icon: () => ionIcon(I.pause),
        })
    }
    if (hasPermission('app:copy')) {
        dropdownOptions.push({
            label: t('spark.app.operation.copy'),
            key: 'copy',
            icon: () => ionIcon(I.copy),
        })
    }
    if (hasPermission('app:mapping') && canMapping(row)) {
        dropdownOptions.push({
            label: t('spark.app.operation.remapping'),
            key: 'mapping',
            icon: () => ionIcon(I.mapping),
        })
    }
    if (hasPermission('app:delete') && canDeleteSpark(row)) {
        dropdownOptions.push({
            label: t('common.delText'),
            key: 'delete',
            icon: () => ionIcon(I.delete),
        })
    }

    if (dropdownOptions.length) {
        actions.push(
            h(
                NDropdown,
                {
                    trigger: 'click',
                    options: dropdownOptions,
                    onSelect: (key: string) => {
                        if (key === 'log') {
                            logApp.value = row
                            logModalVisible.value = true
                        } else if (key === 'abort') {
                            handleAbort(row)
                        } else if (key === 'copy') {
                            openCopyModal(row)
                        } else if (key === 'mapping') {
                            openMappingModal(row)
                        } else if (key === 'delete') {
                            window.$dialog?.warning({
                                title: t('common.tip'),
                                content: t('spark.app.operation.deleteTip'),
                                positiveText: t('common.okText'),
                                negativeText: t('common.cancelText'),
                                onPositiveClick: () => handleDelete(row),
                            })
                        }
                    },
                },
                {
                    default: () =>
                        h(
                            NButton,
                            { quaternary: true, size: 'small', class: 'e2e-sparkapp-more-btn' },
                            { default: () => '...' },
                        ),
                },
            ),
        )
    }

    return h(NSpace, { size: 4 }, { default: () => actions })
}

function handleColumnWidthChange(
    width: number,
    _limitedWidth: number,
    column: { key?: string | number },
) {
    if (column.key === 'appName') appNameColumnWidth.value = width
    if (column.key === 'release') releaseColumnWidth.value = width
}

const columns = computed<DataTableColumns<SparkApplication>>(() => [
    {
        title: t('spark.app.appName'),
        key: 'appName',
        fixed: 'left',
        width: appNameColumnWidth.value,
        minWidth: 200,
        resizable: true,
        render(row) {
            return h(AppListNameCell, {
                name: row.appName ?? '',
                jobType: row.jobType ?? 0,
                description: row.description,
                release: row.release,
                engine: 'spark',
                clickable: row.state === AppStateEnum.RUNNING,
                nameLabel: t('spark.app.appName'),
                jobTypeLabel: t('spark.app.jobType'),
                onClick: () => handleJobView(row),
            })
        },
    },
    {
        title: t('spark.app.tags'),
        key: 'tags',
        width: 120,
        render: (row) => h(AppTagsCell, { tags: row.tags }),
    },
    {
        title: t('spark.app.sparkVersion'),
        key: 'sparkVersion',
        width: 110,
        ellipsis: { tooltip: true },
    },
    {
        title: t('spark.app.status'),
        key: 'state',
        width: 130,
        render: (row) =>
            h(SparkAppStateTag, { option: 'state', data: row, maxTitle: titleLenRef.maxState }),
    },
    {
        title: t('spark.app.releaseBuild'),
        key: 'release',
        width: releaseColumnWidth.value,
        minWidth: 130,
        resizable: true,
        render: (row) =>
            h(SparkAppStateTag, { option: 'release', data: row, maxTitle: titleLenRef.maxRelease }),
    },
    {
        title: 'Build',
        key: 'buildStatus',
        width: 110,
        render: (row) =>
            h(SparkAppStateTag, { option: 'build', data: row, maxTitle: titleLenRef.maxBuild }),
    },
    {
        title: t('spark.app.duration'),
        key: 'duration',
        width: 100,
        render: (row) => (row.duration != null ? dateToDuration(row.duration) : '-'),
    },
    {
        title: t('spark.app.owner'),
        key: 'nickName',
        width: 100,
        ellipsis: { tooltip: true },
        render: (row) => row.nickName || row.username || '-',
    },
    { title: t('spark.app.modifyTime'), key: 'modifyTime', width: 170 },
    {
        title: t('component.table.operation'),
        key: 'action',
        width: 220,
        render: (row) => buildRowActions(row),
    },
])

async function loadData(polling = false) {
    if (!polling) loading.value = true
    try {
        const params: Recordable = {
            pageNum: pagination.page,
            pageSize: pagination.pageSize,
            appName: searchForm.appName || undefined,
            jobType: searchForm.jobType ?? undefined,
            tags: searchForm.tags || undefined,
            userId: searchForm.userId || undefined,
        }
        if (searchForm.stateArray.length > 0) params.stateArray = [...searchForm.stateArray]

        const result = await fetchSparkAppRecord(params)
        if (!result.isSuccess) throwApiFailure(result, t('sys.api.apiRequestFailed'))
        const { records, total } = resolveListData<SparkApplication>(result.data)
        cleanupOptionApps(records)
        updateTitleLens(records)
        tableData.value = records
        pagination.itemCount = total
    } catch (e: any) {
        if (!polling) showCatchError(e, t('sys.api.apiRequestFailed'))
        if (!polling) {
            tableData.value = []
            pagination.itemCount = 0
        }
    } finally {
        if (!polling) loading.value = false
    }
}

function handlePageDataReload(polling = false) {
    dashboardRef.value?.loadDashboard(false)
    loadData(polling)
}

async function ensureYarnUrl() {
    if (yarnUrl.value != null) return
    const result = await fetchSparkYarn()
    if (result.isSuccess) yarnUrl.value = result.data ?? null
}

async function handleJobView(app: SparkApplication) {
    if (app.state !== AppStateEnum.RUNNING) return
    await ensureYarnUrl()
    await handleView(app, yarnUrl.value)
}

function handleSearch() {
    pagination.page = 1
    handlePageDataReload(false)
}

async function handleDelete(row: SparkApplication) {
    if (!row.id) return
    const result = await fetchSparkAppRemove(row.id)
    if (result.isSuccess && result.data) {
        window.$message?.success(t('spark.home.tips.remove'))
        handlePageDataReload(false)
    } else {
        showResultError(result, t('sys.api.apiRequestFailed'))
    }
}

async function handleRelease(app: SparkApplication) {
    const force = app.appControl?.allowBuild !== true
    if (force) {
        window.$dialog?.warning({
            title: t('common.tip'),
            content: `${t('spark.app.release.releaseTitle')}\n${t('spark.app.release.releaseDesc')}`,
            positiveText: t('common.okText'),
            negativeText: t('common.cancelText'),
            onPositiveClick: () => doRelease(app, true),
        })
        return
    }
    await doRelease(app, false)
}

async function doRelease(app: SparkApplication, forceBuild: boolean) {
    if (!app.id) return
    const result = await fetchBuildSparkApp({ appId: app.id, forceBuild })
    const { ok, message } = unwrapBooleanResult(result.data, result.message)
    if (!result.isSuccess || !ok) {
        const msg = (message || t('spark.app.release.releaseFail')).replaceAll(/\[StreamPark]/g, '')
        window.$message?.error(msg)
        return
    }
    window.$message?.success(t('spark.app.release.releasing'))
    handlePageDataReload(true)
}

async function handleStartCheck(app: SparkApplication) {
    if (!app.sparkVersion) {
        window.$message?.error('please set spark version first.')
        return
    }
    if (app.appControl?.allowStart === false) {
        const detail = app.id ? await fetchSparkBuildDetail({ appId: app.id }) : null
        const pipeStatus = detail?.data?.pipeline?.pipeStatus
        window.$dialog?.warning({
            title: t('common.tip'),
            content:
                pipeStatus == null
                    ? 'No build record exists for the current application. Are you sure to force the application to run?'
                    : `The current build state is ${pipeStatus}. Are you sure to force the application to run?`,
            positiveText: t('common.okText'),
            negativeText: t('common.cancelText'),
            onPositiveClick: () => handleStart(app),
        })
        return
    }
    await handleStart(app)
}

async function handleStart(app: SparkApplication) {
    if (!app.id) return
    if (!optionApps.starting.get(app.id) || app.optionState === OptionStateEnum.NONE) {
        const checkResult = await fetchCheckSparkAppStart({ id: app.id })
        if (checkResult.isSuccess && checkResult.data === AppExistsStateEnum.IN_YARN) {
            await fetchSparkAppForcedStop({ id: app.id })
        }
        const result = await fetchSparkAppStart({ id: app.id })
        const { ok, message } = unwrapBooleanResult(result.data, result.message)
        if (result.isSuccess && ok) {
            window.$message?.success(t('spark.app.operation.starting'))
            optionApps.starting.set(app.id, Date.now())
            handlePageDataReload(true)
        } else {
            const msg = (message || t('sys.api.apiRequestFailed')).replaceAll(/\[StreamPark]/g, '')
            window.$message?.error(msg)
        }
    }
}

async function handleCancel(app: SparkApplication) {
    if (!app.id) return
    const result = await fetchSparkAppCancel({ id: app.id })
    if (result.isSuccess) {
        window.$message?.success(t('flink.app.operation.canceling'))
        optionApps.stopping.set(app.id, Date.now())
        handlePageDataReload(true)
    }
}

function confirmCancel(app: SparkApplication) {
    window.$dialog?.warning({
        title: t('common.tip'),
        content: t('spark.app.operation.cancelTip'),
        positiveText: t('common.okText'),
        negativeText: t('common.cancelText'),
        onPositiveClick: () => handleCancel(app),
    })
}

async function initTagsOptions() {
    const result = await fetchSparkAppRecord({ pageNum: 1, pageSize: 999999 })
    if (!result.isSuccess) return
    const { records } = resolveListData<SparkApplication>(result.data)
    const tags = new Set<string>()
    records.forEach((r: SparkApplication) => {
        if (r.tags) r.tags.split(',').forEach((tag) => tags.add(tag.trim()))
    })
    tagsOptions.value = [...tags]
}

async function initUsers() {
    const result = await fetchAppOwners({})
    if (result.isSuccess && Array.isArray(result.data)) users.value = result.data
}

async function handleAbort(app: SparkApplication) {
    if (!app.id) return
    window.$dialog?.warning({
        title: t('common.tips'),
        content: t('flink.app.action.abortJobStarting'),
        positiveText: t('common.okText'),
        negativeText: t('common.cancelText'),
        onPositiveClick: async () => {
            const result = await fetchSparkAppForcedStop({ id: app.id })
            if (result.isSuccess && result.data) {
                window.$message?.success(t('flink.app.action.abortJobStarting'))
                handlePageDataReload(true)
            }
        },
    })
}

function hasActiveOperations() {
    return optionApps.starting.size > 0 || optionApps.stopping.size > 0
}

const { start: startPolling, stop: stopPolling } = useAdaptivePolling(
    () => {
        if (!loading.value) handlePageDataReload(true)
    },
    { isBusy: hasActiveOperations },
)

onMounted(() => {
    const savedPage = sessionStorage.getItem('sparkAppPageNo')
    if (savedPage) {
        pagination.page = Number(savedPage) || 1
        sessionStorage.removeItem('sparkAppPageNo')
    }
    initTagsOptions()
    initUsers()
    handlePageDataReload(false)
    startPolling()
})

onUnmounted(() => {
    stopPolling()
})
</script>

<template>
    <div class="h-full flex flex-col">
        <AppDashboard ref="dashboardRef" />
        <n-card :bordered="false" class="flex-1">
            <div class="mb-16px flex flex-wrap items-end justify-between gap-12px">
                <n-form inline :model="searchForm" class="flex-1">
                    <n-grid :x-gap="12" :cols="24" responsive="screen" item-responsive>
                        <n-form-item-gi :span="6" :xs="24" :sm="12" :md="6">
                            <n-input
                                v-model:value="searchForm.appName"
                                clearable
                                :placeholder="t('spark.app.searchName')"
                                @keyup.enter="handleSearch"
                                @clear="handleSearch"
                            />
                        </n-form-item-gi>
                        <n-form-item-gi :span="5" :xs="24" :sm="12" :md="5">
                            <n-select
                                v-model:value="searchForm.tags"
                                clearable
                                filterable
                                :placeholder="t('spark.app.tags')"
                                :options="tagsOptions.map((tag) => ({ label: tag, value: tag }))"
                                @update:value="handleSearch"
                            />
                        </n-form-item-gi>
                        <n-form-item-gi :span="5" :xs="24" :sm="12" :md="5">
                            <n-select
                                v-model:value="searchForm.userId"
                                clearable
                                filterable
                                :placeholder="t('spark.app.owner')"
                                :options="
                                    users.map((u) => ({
                                        label: u.nickName || u.username,
                                        value: u.userId,
                                    }))
                                "
                                @update:value="handleSearch"
                            />
                        </n-form-item-gi>
                        <n-form-item-gi :span="4" :xs="24" :sm="12" :md="4">
                            <n-select
                                v-model:value="searchForm.jobType"
                                clearable
                                filterable
                                :placeholder="t('spark.app.jobType')"
                                :options="jobTypeOptions"
                                @update:value="handleSearch"
                            />
                        </n-form-item-gi>
                        <n-form-item-gi :span="4" :xs="24" :sm="12" :md="4">
                            <n-select
                                v-model:value="searchForm.stateArray"
                                clearable
                                multiple
                                filterable
                                :placeholder="t('spark.app.status')"
                                :options="stateOptions"
                                @update:value="handleSearch"
                            />
                        </n-form-item-gi>
                    </n-grid>
                </n-form>
                <n-space>
                    <n-button type="primary" ghost @click="handleSearch">
                        {{ t('common.queryText') }}
                    </n-button>
                    <n-button
                        id="e2e-sparkapp-create-btn"
                        v-auth="'app:create'"
                        type="primary"
                        @click="router.push('/spark/app/add')"
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
                :scroll-x="1500"
                :row-key="(row: SparkApplication) => row.id || ''"
                flex-height
                class="min-h-480px"
                @unstable-column-resize="handleColumnWidthChange"
            />

            <BuildDrawer v-model:show="buildDrawerVisible" :app-id="buildAppId" />
            <LogModal v-model:show="logModalVisible" :app="logApp" />

            <n-modal
                v-model:show="copyModalVisible"
                preset="card"
                :style="{ width: '480px' }"
                :title="t('spark.app.operation.copyModalTitle')"
            >
                <n-form label-placement="top">
                    <n-form-item :label="t('spark.app.operation.copyJobNameLabel')" required>
                        <n-input
                            v-model:value="copyAppName"
                            :placeholder="t('spark.app.operation.copyJobNamePlaceholder')"
                        />
                    </n-form-item>
                </n-form>
                <template #footer>
                    <n-space justify="end">
                        <n-button @click="copyModalVisible = false">
                            {{ t('common.cancelText') }}
                        </n-button>
                        <n-button type="primary" class="e2e-sparkapp-copy-btn" @click="submitCopy">
                            {{ t('common.apply') }}
                        </n-button>
                    </n-space>
                </template>
            </n-modal>

            <n-modal
                v-model:show="mappingModalVisible"
                preset="card"
                :style="{ width: '520px' }"
                :title="t('spark.app.operation.mappingModalTitle')"
            >
                <n-form label-placement="top">
                    <n-form-item :label="t('spark.app.operation.mappingJobNameLabel')">
                        <n-alert type="info">
                            {{ mappingApp?.appName }}
                        </n-alert>
                    </n-form-item>
                    <n-form-item
                        v-if="
                            mappingApp &&
                            [DeployMode.YARN_CLIENT, DeployMode.YARN_CLUSTER].includes(
                                mappingApp.deployMode!,
                            )
                        "
                        :label="t('spark.app.operation.mappingYarnAppIdLabel')"
                        required
                    >
                        <n-input
                            v-model:value="mappingForm.clusterId"
                            :placeholder="t('spark.app.operation.mappingYarnAppIdPlaceholder')"
                        />
                    </n-form-item>
                    <n-form-item :label="t('spark.app.operation.mappingJobIdLabel')" required>
                        <n-input
                            v-model:value="mappingForm.jobId"
                            :placeholder="t('spark.app.operation.mappingJobIdPlaceholder')"
                        />
                    </n-form-item>
                </n-form>
                <template #footer>
                    <n-space justify="end">
                        <n-button @click="mappingModalVisible = false">
                            {{ t('common.cancelText') }}
                        </n-button>
                        <n-button
                            type="primary"
                            class="e2e-sparkapp-remapping-btn"
                            @click="submitMapping"
                        >
                            {{ t('common.apply') }}
                        </n-button>
                    </n-space>
                </template>
            </n-modal>
        </n-card>
    </div>
</template>

<style scoped>
.text-primary {
    color: var(--primary-color);
}
</style>
