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
import { NButton, NDropdown, NSpace, NTooltip } from 'naive-ui'
import type { AppListRecord } from '@/types/api/flink/app.type'
import type { StartModalPayload } from './components/StartApplicationModal.vue'
import { usePermission } from '@/hooks'
import {
    fetchAbort,
    fetchAppOwners,
    fetchAppRecord,
    fetchAppRemove,
    fetchBuild,
    fetchBuildDetail,
    fetchCopy,
    fetchCheckName,
    fetchMapping,
    fetchSavePointHistory,
} from '@/service'
import { resolveListData } from '@/views/system/shared/utils'
import { dateToDuration } from '@/utils/dateUtil'
import { unwrapBooleanResult } from '@/utils/apiResult'
import {
    AppStateEnum,
    AppTypeEnum,
    DeployMode,
    JobTypeEnum,
    OptionStateEnum,
    ReleaseStateEnum,
} from '@/enums/flinkEnum'
import { handleIsStart } from '@/views/flink/app/shared/utils'
import { useDebounceFn } from '@vueuse/core'
import { useAdaptivePolling } from '@/hooks/useAdaptivePolling'
import AppDashboard from './components/AppDashboard.vue'
import AppStateTag from './components/AppStateTag.vue'
import AppListNameCell from '@/views/shared/components/AppListNameCell.vue'
import AppTagsCell from '@/views/shared/components/AppTagsCell.vue'
import BuildDrawer from './components/BuildDrawer.vue'
import LogModal from './components/LogModal.vue'
import SavepointModal from './components/SavepointModal.vue'
import StartApplicationModal from './components/StartApplicationModal.vue'
import StopApplicationModal from './components/StopApplicationModal.vue'
import {
    buildStatusMap,
    createOptionStateMap,
    createReleaseStateMap,
    createStateMap,
} from './shared/constants'

defineOptions({ name: 'FlinkAppList' })

const { t } = useI18n()
const { hasPermission } = usePermission()
const router = useRouter()

const dashboardRef = ref<InstanceType<typeof AppDashboard> | null>(null)
const loading = ref(false)
const tableData = ref<AppListRecord[]>([])
const tagsOptions = ref<string[]>([])
const users = ref<Array<{ userId: string; username: string; nickName?: string }>>([])

const searchForm = reactive({
    jobName: '',
    tags: null as string | null,
    jobType: null as number | null,
    userId: null as string | null,
})

const titleLenRef = reactive({
    maxState: '',
    maxRelease: '',
    maxBuild: '',
})

const jobNameColumnWidth = ref(250)
const releaseColumnWidth = ref(190)

const optionApps = {
    starting: new Map<string, number>(),
    stopping: new Map<string, number>(),
    release: new Map<string, number>(),
    savepointing: new Map<string, number>(),
}

const startModalVisible = ref(false)
const startModalPayload = ref<StartModalPayload | null>(null)
const stopModalVisible = ref(false)
const stopApp = ref<AppListRecord | null>(null)
const savepointModalVisible = ref(false)
const savepointApp = ref<AppListRecord | null>(null)
const logModalVisible = ref(false)
const logApp = ref<AppListRecord | null>(null)
const buildDrawerVisible = ref(false)
const buildAppId = ref<string | null>(null)

const copyModalVisible = ref(false)
const copyApp = ref<AppListRecord | null>(null)
const copyJobName = ref('')

const mappingModalVisible = ref(false)
const mappingApp = ref<AppListRecord | null>(null)
const mappingForm = reactive({ clusterId: '', jobId: '' })

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

const stateMap = computed(() => createStateMap(t))
const optionStateMap = computed(() => createOptionStateMap(t))
const releaseStateMap = computed(() => createReleaseStateMap(t))

function updateTitleLens(records: AppListRecord[]) {
    let maxState = ''
    let maxRelease = ''
    let maxBuild = ''
    for (const cur of records) {
        const { state, optionState, release, buildStatus } = cur
        if (optionState === OptionStateEnum.NONE) {
            const stateStr = stateMap.value[state]?.title
            if (stateStr && stateStr.length > maxState.length) maxState = stateStr
        } else {
            const stateStr = optionStateMap.value[optionState]?.title
            if (stateStr && stateStr.length > maxState.length) maxState = stateStr
        }
        const releaseStr = releaseStateMap.value[release]?.title
        if (releaseStr && releaseStr.length > maxRelease.length) maxRelease = releaseStr
        const buildStr = buildStatusMap[buildStatus]?.title
        if (buildStr && buildStr.length > maxBuild.length) maxBuild = buildStr
    }
    titleLenRef.maxState = maxState
    titleLenRef.maxRelease = maxRelease
    titleLenRef.maxBuild = maxBuild
}

function cleanupOptionApps(records: AppListRecord[]) {
    const timestamp = Date.now()
    for (const x of records) {
        if (x.optionState === OptionStateEnum.NONE) {
            if (optionApps.starting.get(x.id) && timestamp - optionApps.starting.get(x.id)! > 4000)
                optionApps.starting.delete(x.id)
            if (optionApps.stopping.get(x.id) && timestamp - optionApps.stopping.get(x.id)! > 2000)
                optionApps.stopping.delete(x.id)
            if (optionApps.release.get(x.id) && timestamp - optionApps.release.get(x.id)! > 2000)
                optionApps.release.delete(x.id)
            if (
                optionApps.savepointing.get(x.id) &&
                timestamp - optionApps.savepointing.get(x.id)! > 2000
            )
                optionApps.savepointing.delete(x.id)
        }
    }
}

async function loadData(silent = false) {
    if (!silent) loading.value = true
    try {
        const result = await fetchAppRecord({
            pageNum: pagination.page,
            pageSize: pagination.pageSize,
            jobName: searchForm.jobName || undefined,
            tags: searchForm.tags || undefined,
            jobType: searchForm.jobType ?? undefined,
            userId: searchForm.userId || undefined,
        })
        if (!result.isSuccess) throwApiFailure(result, t('sys.api.apiRequestFailed'))
        const { records, total } = resolveListData(result.data)
        cleanupOptionApps(records)
        tableData.value = records
        pagination.itemCount = total
        updateTitleLens(records)
        sessionStorage.setItem('appPageNo', String(pagination.page))
    } catch (e: any) {
        if (!silent) showCatchError(e, t('sys.api.apiRequestFailed'))
    } finally {
        if (!silent) loading.value = false
    }
}

function handlePageDataReload(silent = false) {
    dashboardRef.value?.loadDashboard(false)
    loadData(silent)
}

const handleSearch = useDebounceFn(() => {
    pagination.page = 1
    loadData()
}, 500)

function handleOptionApp(data: {
    type: 'starting' | 'stopping' | 'release' | 'savepointing'
    key: string
    value: number
}) {
    optionApps[data.type].set(data.key, data.value)
}

function goDetail(app: AppListRecord) {
    router.push({ path: '/flink/app/detail', query: { appId: app.id } })
}

function goEdit(app: AppListRecord) {
    sessionStorage.setItem('appPageNo', String(pagination.page))
    if (app.appType === AppTypeEnum.STREAMPARK_FLINK)
        router.push({ path: '/flink/app/edit_streampark', query: { appId: app.id } })
    else router.push({ path: '/flink/app/edit_flink', query: { appId: app.id } })
}

async function handleRelease(app: AppListRecord, force = false) {
    if (!force && app.appControl?.allowBuild === false) {
        window.$dialog?.warning({
            title: t('common.tip'),
            content: `${t('flink.app.release.releaseTitle')}\n${t('flink.app.release.releaseDesc')}`,
            positiveText: t('common.okText'),
            negativeText: t('common.cancelText'),
            onPositiveClick: () => handleRelease(app, true),
        })
        return
    }
    const msg = window.$message?.loading(t('flink.app.release.releasing'), { duration: 0 })
    try {
        const result = await fetchBuild({ appId: app.id, forceBuild: force })
        if (!result.isSuccess) throwApiFailure(result, t('sys.api.apiRequestFailed'))
        const { ok, message } = unwrapBooleanResult(result.data, result.message)
        if (ok) {
            window.$message?.success(t('flink.app.release.releasing'))
            handleOptionApp({ type: 'release', key: app.id, value: Date.now() })
            handlePageDataReload(true)
        } else {
            window.$message?.error(
                (message || t('flink.app.release.releaseFail')).replaceAll(/\[StreamPark]/g, ''),
            )
        }
    } catch (e: any) {
        showCatchError(e, t('sys.api.apiRequestFailed'))
    } finally {
        msg?.destroy()
    }
}

async function handleStartCheck(app: AppListRecord) {
    if (app.flinkVersion == null) {
        window.$message?.error(t('flink.app.addAppTips.flinkVersionIsRequiredMessage'))
        return
    }
    if (optionApps.starting.get(app.id) && app.optionState !== OptionStateEnum.NONE) return

    if (app.appControl?.allowStart === false) {
        const detailResult = await fetchBuildDetail({ appId: app.id })
        const pipeStatus = detailResult.data?.pipeline?.pipeStatus
        window.$dialog?.warning({
            title: t('common.tip'),
            content:
                pipeStatus == null
                    ? t('flink.app.operation.forceStartNoBuild')
                    : t('flink.app.operation.forceStartBuildState'),
            positiveText: t('common.okText'),
            negativeText: t('common.cancelText'),
            onPositiveClick: () => openStartModal(app),
        })
        return
    }
    openStartModal(app)
}

async function openStartModal(app: AppListRecord) {
    type SavepointHistoryItem = { path: string; latest?: boolean }
    const result = await fetchSavePointHistory({ appId: app.id, pageNum: 1, pageSize: 9999 })
    const records = result.isSuccess
        ? resolveListData<SavepointHistoryItem>(
              result.data as { records?: SavepointHistoryItem[]; total?: number | string },
          ).records
        : []
    const history = records.filter((x) => x.path)
    const latest = history.find((x) => x.latest) ?? null
    startModalPayload.value = { application: app, historySavePoint: history, selected: latest }
    startModalVisible.value = true
}

function openStopModal(app: AppListRecord) {
    if (optionApps.stopping.get(app.id) && app.optionState !== OptionStateEnum.NONE) return
    stopApp.value = app
    stopModalVisible.value = true
}

function openSavepointModal(app: AppListRecord) {
    if (optionApps.savepointing.get(app.id) && app.optionState !== OptionStateEnum.NONE) return
    savepointApp.value = app
    savepointModalVisible.value = true
}

async function handleDelete(app: AppListRecord) {
    const msg = window.$message?.loading(t('flink.app.operation.deleting'), { duration: 0 })
    try {
        const result = await fetchAppRemove(app.id)
        if (!result.isSuccess) throwApiFailure(result, t('sys.api.apiRequestFailed'))
        window.$message?.success(t('flink.app.operation.deleteSuccess'))
        handlePageDataReload(false)
    } catch (e: any) {
        showCatchError(e, t('sys.api.apiRequestFailed'))
    } finally {
        msg?.destroy()
    }
}

async function handleAbort(app: AppListRecord) {
    window.$dialog?.warning({
        title: t('common.tip'),
        content: t('flink.app.operation.abortConfirm'),
        positiveText: t('common.okText'),
        negativeText: t('common.cancelText'),
        onPositiveClick: async () => {
            const result = await fetchAbort({ id: app.id })
            if (result.isSuccess) window.$message?.success(t('flink.app.action.abortJobStarting'))
            handlePageDataReload(true)
        },
    })
}

function openCopyModal(app: AppListRecord) {
    copyApp.value = app
    copyJobName.value = `${app.jobName}_copy`
    copyModalVisible.value = true
}

async function validateCopyJobName(jobName: string) {
    if (!jobName) {
        window.$message?.warning(t('flink.app.addAppTips.appNameIsRequiredMessage'))
        return false
    }
    const result = await fetchCheckName({ jobName })
    if (!result.isSuccess) {
        showResultError(result, t('sys.api.apiRequestFailed'))
        return false
    }
    const code = Number(result.data)
    switch (code) {
        case 0:
            return true
        case 1:
            window.$message?.warning(t('flink.app.addAppTips.appNameNotUniqueMessage'))
            return false
        case 2:
            window.$message?.warning(t('flink.app.addAppTips.appNameExistsInYarnMessage'))
            return false
        case 3:
            window.$message?.warning(t('flink.app.addAppTips.appNameExistsInK8sMessage'))
            return false
        default:
            window.$message?.warning(t('flink.app.addAppTips.appNameNotValid'))
            return false
    }
}

async function submitCopy() {
    if (!copyApp.value) return
    const jobName = copyJobName.value.trim()
    if (!(await validateCopyJobName(jobName))) return
    try {
        const result = await fetchCopy({ id: copyApp.value.id, jobName })
        if (!result.isSuccess) throwApiFailure(result, t('sys.api.apiRequestFailed'))
        const payload = result.data as Recordable
        const status = payload?.status ?? (payload?.data === true ? 'success' : 'error')
        if (status === 'success') {
            window.$message?.success(t('flink.app.operation.copySuccess'))
            copyModalVisible.value = false
            handlePageDataReload(false)
        } else {
            window.$message?.error(
                String(payload?.message || t('flink.app.operation.copyFail')).replaceAll(
                    /\[StreamPark]/g,
                    '',
                ),
            )
        }
    } catch (e: any) {
        showCatchError(e, t('flink.app.operation.copyFail'))
    }
}

function openMappingModal(app: AppListRecord) {
    mappingApp.value = app
    mappingForm.clusterId = ''
    mappingForm.jobId = ''
    mappingModalVisible.value = true
}

async function submitMapping() {
    if (!mappingApp.value) return
    if (!mappingForm.jobId.trim()) {
        window.$message?.warning(t('flink.app.operation.mappingJobIdRequired'))
        return
    }
    try {
        const result = await fetchMapping({
            id: mappingApp.value.id,
            clusterId: mappingForm.clusterId || undefined,
            jobId: mappingForm.jobId.trim(),
        })
        if (!result.isSuccess) throwApiFailure(result, t('sys.api.apiRequestFailed'))
        window.$message?.success(t('flink.app.operation.mappingSuccess'))
        mappingModalVisible.value = false
        handlePageDataReload(true)
    } catch (e: any) {
        showCatchError(e, t('sys.api.apiRequestFailed'))
    }
}

function canDelete(app: AppListRecord) {
    return [
        AppStateEnum.ADDED,
        AppStateEnum.FAILED,
        AppStateEnum.CANCELED,
        AppStateEnum.FINISHED,
        AppStateEnum.LOST,
        AppStateEnum.TERMINATED,
        AppStateEnum.POS_TERMINATED,
        AppStateEnum.SUCCEEDED,
        AppStateEnum.KILLED,
    ].includes(app.state)
}

function canAbort(app: AppListRecord) {
    const optionTime = new Date(app.optionTime || 0).getTime()
    if (Date.now() - optionTime < 60 * 1000) return false
    if (app.optionState === OptionStateEnum.NONE) {
        return [
            AppStateEnum.INITIALIZING,
            AppStateEnum.STARTING,
            AppStateEnum.RESTARTING,
            AppStateEnum.CANCELLING,
            AppStateEnum.RECONCILING,
            AppStateEnum.MAPPING,
        ].includes(app.state)
    }
    return true
}

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
                        onClick,
                    },
                    { icon: () => ionIcon(icon) },
                ),
            default: () => tip,
        },
    )
}

function buildRowActions(row: AppListRecord) {
    const actions: ReturnType<typeof h>[] = []

    if (hasPermission('app:update')) {
        actions.push(
            actionBtn(
                I.edit,
                t('flink.app.operation.edit'),
                () => goEdit(row),
                'e2e-flinkapp-edit-btn',
            ),
        )
    }
    if (
        hasPermission('app:release') &&
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
                t('flink.app.operation.release'),
                () => handleRelease(row),
                'e2e-flinkapp-release-btn',
            ),
        )
    }
    if (
        hasPermission('app:release') &&
        ([ReleaseStateEnum.FAILED, ReleaseStateEnum.RELEASING].includes(row.release) ||
            row.optionState === OptionStateEnum.RELEASING)
    ) {
        actions.push(
            actionBtn(
                I.releaseDetail,
                t('flink.app.operation.releaseDetail'),
                () => {
                    buildAppId.value = row.id
                    buildDrawerVisible.value = true
                },
                'e2e-flinkapp-build-detail-btn',
            ),
        )
    }
    if (hasPermission('app:start') && handleIsStart(row, optionApps)) {
        actions.push(
            actionBtn(
                I.start,
                t('flink.app.operation.start'),
                () => handleStartCheck(row),
                'e2e-flinkapp-startup-btn',
            ),
        )
    }
    if (
        hasPermission('app:cancel') &&
        row.state === AppStateEnum.RUNNING &&
        row.optionState === OptionStateEnum.NONE
    ) {
        actions.push(
            actionBtn(
                I.pause,
                t('flink.app.operation.cancel'),
                () => openStopModal(row),
                'e2e-flinkapp-cancel-btn',
            ),
        )
    }
    if (
        hasPermission('savepoint:trigger') &&
        row.state === AppStateEnum.RUNNING &&
        row.optionState === OptionStateEnum.NONE
    ) {
        actions.push(
            actionBtn(
                I.savepoint,
                t('flink.app.operation.savepoint'),
                () => openSavepointModal(row),
                'e2e-flinkapp-savepoint-btn',
            ),
        )
    }
    if (hasPermission('app:detail')) {
        actions.push(
            actionBtn(
                I.detail,
                t('flink.app.operation.detail'),
                () => goDetail(row),
                'e2e-flinkapp-detail-btn',
            ),
        )
    }

    const dropdownOptions: DropdownOption[] = []
    if (
        hasPermission('app:detail') &&
        [DeployMode.KUBERNETES_SESSION, DeployMode.KUBERNETES_APPLICATION].includes(row.deployMode)
    ) {
        dropdownOptions.push({
            label: t('flink.app.operation.startLog'),
            key: 'log',
            icon: () => ionIcon(I.code),
        })
    }
    if (hasPermission('app:cancel') && canAbort(row)) {
        dropdownOptions.push({
            label: t('flink.app.operation.abort'),
            key: 'abort',
            icon: () => ionIcon(I.pause),
        })
    }
    if (hasPermission('app:copy')) {
        dropdownOptions.push({
            label: t('flink.app.operation.copy'),
            key: 'copy',
            icon: () => ionIcon(I.copy),
        })
    }
    if (
        hasPermission('app:mapping') &&
        [
            AppStateEnum.ADDED,
            AppStateEnum.FAILED,
            AppStateEnum.CANCELED,
            AppStateEnum.KILLED,
            AppStateEnum.SUCCEEDED,
            AppStateEnum.TERMINATED,
            AppStateEnum.POS_TERMINATED,
            AppStateEnum.FINISHED,
            AppStateEnum.SUSPENDED,
            AppStateEnum.LOST,
        ].includes(row.state)
    ) {
        dropdownOptions.push({
            label: t('flink.app.operation.remapping'),
            key: 'mapping',
            icon: () => ionIcon(I.mapping),
        })
    }
    if (hasPermission('app:delete') && canDelete(row)) {
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
                    to: 'body',
                    scrollable: true,
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
                                content: t('flink.app.operation.deleteTip'),
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
                            { quaternary: true, size: 'small', class: 'e2e-flinkapp-more-btn' },
                            {
                                default: () => '...',
                            },
                        ),
                },
            ),
        )
    }

    return h(NSpace, { size: 4, wrap: false }, { default: () => actions })
}

function handleColumnWidthChange(
    width: number,
    _limitedWidth: number,
    column: { key?: string | number },
) {
    if (column.key === 'jobName') jobNameColumnWidth.value = width
    if (column.key === 'release') releaseColumnWidth.value = width
}

const columns = computed<DataTableColumns<AppListRecord>>(() => [
    {
        title: t('flink.app.appName'),
        key: 'jobName',
        fixed: 'left',
        width: jobNameColumnWidth.value,
        minWidth: 200,
        resizable: true,
        render(row) {
            return h(AppListNameCell, {
                name: row.jobName,
                jobType: row.jobType,
                description: row.description,
                release: row.release,
                engine: 'flink',
                onClick: () => goDetail(row),
            })
        },
    },
    {
        title: t('flink.app.jobType'),
        key: 'jobType',
        width: 90,
        render: (row) =>
            row.jobType === JobTypeEnum.JAR
                ? 'JAR'
                : row.jobType === JobTypeEnum.SQL
                  ? 'SQL'
                  : 'PY',
    },
    {
        title: t('flink.app.flinkVersion'),
        key: 'flinkVersion',
        width: 110,
        ellipsis: { tooltip: true },
        render: (row) => row.flinkVersion || row.versionId || '-',
    },
    {
        title: t('flink.app.tags'),
        key: 'tags',
        width: 150,
        render: (row) => h(AppTagsCell, { tags: row.tags }),
    },
    {
        title: t('flink.app.runStatus'),
        key: 'state',
        width: 130,
        render: (row) =>
            h(AppStateTag, { option: 'state', data: row, maxTitle: titleLenRef.maxState }),
    },
    {
        title: t('flink.app.releaseBuild'),
        key: 'release',
        width: releaseColumnWidth.value,
        minWidth: 130,
        resizable: true,
        render: (row) =>
            h(AppStateTag, { option: 'release', data: row, maxTitle: titleLenRef.maxRelease }),
    },
    {
        title: t('flink.app.buildColumn'),
        key: 'buildStatus',
        width: 110,
        render: (row) =>
            h(AppStateTag, { option: 'build', data: row, maxTitle: titleLenRef.maxBuild }),
    },
    {
        title: t('flink.app.taskColumn'),
        key: 'task',
        width: 120,
        render: (row) => h(AppStateTag, { option: 'task', data: row }),
    },
    {
        title: t('flink.app.duration'),
        key: 'duration',
        width: 130,
        sorter: (a, b) => Number(a.duration ?? 0) - Number(b.duration ?? 0),
        render: (row) => (row.duration != null ? dateToDuration(Number(row.duration)) : '-'),
    },
    {
        title: t('flink.app.owner'),
        key: 'nickName',
        width: 100,
        ellipsis: { tooltip: true },
        render: (row) => row.nickName || row.userName || '-',
    },
    {
        title: t('flink.app.modifiedTime'),
        key: 'modifyTime',
        width: 170,
        ellipsis: { tooltip: true },
        sorter: (a, b) => String(a.modifyTime ?? '').localeCompare(String(b.modifyTime ?? '')),
    },
    {
        title: t('component.table.operation'),
        key: 'action',
        width: 220,
        fixed: 'right',
        render: (row) => buildRowActions(row),
    },
])

async function initTagsOptions() {
    const result = await fetchAppRecord({ pageNum: 1, pageSize: 999999999 })
    if (!result.isSuccess) return
    const { records } = resolveListData(result.data)
    const tags = new Set<string>()
    for (const record of records) {
        if (record.tags) {
            String(record.tags)
                .split(',')
                .filter(Boolean)
                .forEach((tag) => tags.add(tag))
        }
    }
    tagsOptions.value = [...tags]
}

async function initUsers() {
    const result = await fetchAppOwners({})
    if (result.isSuccess && Array.isArray(result.data)) users.value = result.data
}

function hasActiveOperations() {
    return (
        optionApps.starting.size > 0 ||
        optionApps.stopping.size > 0 ||
        optionApps.release.size > 0 ||
        optionApps.savepointing.size > 0
    )
}

const { start: startPolling, stop: stopPolling } = useAdaptivePolling(
    () => {
        if (!loading.value) handlePageDataReload(true)
    },
    { isBusy: hasActiveOperations },
)

onMounted(() => {
    const savedPage = sessionStorage.getItem('appPageNo')
    if (savedPage) {
        pagination.page = Number(savedPage) || 1
        sessionStorage.removeItem('appPageNo')
    }
    initTagsOptions()
    initUsers()
    loadData()
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
                                v-model:value="searchForm.jobName"
                                clearable
                                :placeholder="t('flink.app.searchName')"
                                @keyup.enter="handleSearch"
                                @clear="handleSearch"
                            />
                        </n-form-item-gi>
                        <n-form-item-gi :span="5" :xs="24" :sm="12" :md="5">
                            <n-select
                                v-model:value="searchForm.tags"
                                clearable
                                filterable
                                :placeholder="t('flink.app.tags')"
                                :options="tagsOptions.map((tag) => ({ label: tag, value: tag }))"
                                @update:value="handleSearch"
                            />
                        </n-form-item-gi>
                        <n-form-item-gi :span="5" :xs="24" :sm="12" :md="5">
                            <n-select
                                v-model:value="searchForm.jobType"
                                clearable
                                filterable
                                :placeholder="t('flink.app.jobType')"
                                :options="[
                                    { label: 'JAR', value: JobTypeEnum.JAR },
                                    { label: 'SQL', value: JobTypeEnum.SQL },
                                    { label: 'PYFLINK', value: JobTypeEnum.PYFLINK },
                                ]"
                                @update:value="handleSearch"
                            />
                        </n-form-item-gi>
                        <n-form-item-gi :span="5" :xs="24" :sm="12" :md="5">
                            <n-select
                                v-model:value="searchForm.userId"
                                clearable
                                filterable
                                :placeholder="t('flink.app.owner')"
                                :options="
                                    users.map((u) => ({
                                        label: u.nickName || u.username,
                                        value: u.userId,
                                    }))
                                "
                                @update:value="handleSearch"
                            />
                        </n-form-item-gi>
                    </n-grid>
                </n-form>
                <n-button
                    id="e2e-flinkapp-create-btn"
                    v-auth="'app:create'"
                    type="primary"
                    @click="router.push('/flink/app/add')"
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
                :scroll-x="1500"
                :row-key="(row: AppListRecord) => row.id"
                flex-height
                class="min-h-480px"
                @unstable-column-resize="handleColumnWidthChange"
            />
        </n-card>

        <StartApplicationModal
            v-model:show="startModalVisible"
            :payload="startModalPayload"
            @update-option="handleOptionApp"
        />
        <StopApplicationModal
            v-model:show="stopModalVisible"
            :application="stopApp"
            @update-option="handleOptionApp"
        />
        <SavepointModal
            v-model:show="savepointModalVisible"
            :application="savepointApp"
            @update-option="handleOptionApp"
        />
        <LogModal v-model:show="logModalVisible" :app="logApp" />
        <BuildDrawer v-model:show="buildDrawerVisible" :app-id="buildAppId" />

        <n-modal
            v-model:show="copyModalVisible"
            preset="card"
            :style="{ width: '480px' }"
            :title="t('flink.app.operation.copyModalTitle')"
        >
            <n-form label-placement="top">
                <n-form-item :label="t('flink.app.operation.copyJobNameLabel')" required>
                    <n-input
                        v-model:value="copyJobName"
                        :placeholder="t('flink.app.operation.copyJobNamePlaceholder')"
                    />
                </n-form-item>
            </n-form>
            <template #footer>
                <n-space justify="end">
                    <n-button @click="copyModalVisible = false">
                        {{ t('common.cancelText') }}
                    </n-button>
                    <n-button type="primary" class="e2e-flinkapp-copy-btn" @click="submitCopy">
                        {{ t('common.apply') }}
                    </n-button>
                </n-space>
            </template>
        </n-modal>

        <n-modal
            v-model:show="mappingModalVisible"
            preset="card"
            :style="{ width: '520px' }"
            :title="t('flink.app.operation.mappingModalTitle')"
        >
            <n-form label-placement="top">
                <n-form-item :label="t('flink.app.operation.mappingJobNameLabel')">
                    <n-alert type="info">
                        {{ mappingApp?.jobName }}
                    </n-alert>
                </n-form-item>
                <n-form-item
                    v-if="
                        mappingApp &&
                        [
                            DeployMode.YARN_PER_JOB,
                            DeployMode.YARN_SESSION,
                            DeployMode.YARN_APPLICATION,
                        ].includes(mappingApp.deployMode)
                    "
                    :label="t('flink.app.operation.mappingYarnAppIdLabel')"
                >
                    <n-input
                        v-model:value="mappingForm.clusterId"
                        :placeholder="t('flink.app.operation.mappingYarnAppIdPlaceholder')"
                    />
                </n-form-item>
                <n-form-item :label="t('flink.app.operation.mappingJobIdLabel')" required>
                    <n-input
                        v-model:value="mappingForm.jobId"
                        :placeholder="t('flink.app.operation.mappingJobIdPlaceholder')"
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
                        class="e2e-flinkapp-remapping-btn"
                        @click="submitMapping"
                    >
                        {{ t('common.apply') }}
                    </n-button>
                </n-space>
            </template>
        </n-modal>
    </div>
</template>

<style scoped>
.text-primary {
    color: var(--primary-color);
}
</style>
