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
import type { FormInst, FormRules } from 'naive-ui'
import {
    fetchAppUpdate,
    fetchConfHistory,
    fetchFlinkEnvPage,
    fetchFlinkSqlList,
    fetchUpload,
} from '@/service'
import { resolveListData } from '@/views/system/shared/utils'
import { useEdit } from '@/views/flink/app/composables/useEdit'
import { mapEditDeployFields } from '@/views/flink/app/composables/mapEditDeployFields'
import { deployModes } from '@/views/flink/app/shared/data/index'
import { buildDependencyJson } from '@/views/flink/app/shared/pom'
import {
    getAppConfType,
    handleSubmitParams,
    handleTeamResource,
    isK8sDeployMode,
} from '@/views/flink/app/shared/utils'
import AppAdvancedFields from './components/AppAdvancedFields.vue'
import AppConf from './components/AppConf.vue'
import AppDeployFields from './components/AppDeployFields.vue'
import CompareConfPicker from './components/CompareConfPicker.vue'
import Dependency from './components/Dependency.vue'
import FlinkSqlEditor from './components/FlinkSqlEditor.vue'
import PodTemplateTab from './components/PodTemplateTab.vue'
import UploadJobJar from './components/UploadJobJar.vue'
import MergelyDrawer from '@/views/shared/editors/MergelyDrawer.vue'
import ProgramArgsEditor from '@/views/shared/editors/ProgramArgsEditor.vue'
import TeamResourceField from '@/views/shared/components/TeamResourceField.vue'
import { DeployMode, JobTypeEnum, ResourceFromEnum, UseStrategyEnum } from '@/enums/flinkEnum'
import { decodeByBase64, encryptByBase64 } from '@/utils/cipher'

function decodeSqlValue(value?: string | null) {
    if (!value) return ''
    try {
        return decodeByBase64(value)
    } catch {
        return value
    }
}

defineOptions({ name: 'EditStreamParkApp' })

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const formRef = ref<FormInst | null>(null)
const dependencyRef = ref<InstanceType<typeof Dependency> | null>(null)
const podTemplateRef = ref<InstanceType<typeof PodTemplateTab> | null>(null)
const flinkSqlEditorRef = ref<InstanceType<typeof FlinkSqlEditor> | null>(null)
const submitting = ref(false)
const uploadLoading = ref(false)
const flinkEnvs = ref<Array<{ id: string; flinkName: string; scalaVersion?: string }>>([])
const flinkSqlHistory = ref<Array<{ id: string; version: number; effective?: boolean }>>([])
const configVersions = ref<Array<{ id: string; version?: number; effective?: boolean }>>([])

const { handleGetApplication, app, handleResetApplication } = useEdit()

const k8sTemplate = reactive({
    podTemplate: '',
    jmPodTemplate: '',
    tmPodTemplate: '',
})

const formModel = ref({
    jobName: '',
    tags: '',
    description: '',
    jobType: JobTypeEnum.JAR,
    deployMode: 0,
    versionId: null as string | null,
    mainClass: '',
    jar: '',
    args: '',
    flinkSql: '',
    configOverride: '',
    isSetConfig: false,
    flinkSqlHistory: null as string | null,
    teamResource: [] as string[],
    dynamicProperties: '',
    restartSize: 0,
    parallelism: null as number | null,
    slot: null as number | null,
    jmOptions: [] as string[],
    tmOptions: [] as string[],
    jmOptionsItem: {} as Recordable,
    tmOptionsItem: {} as Recordable,
    totalOptions: [] as string[],
    totalItem: {} as Recordable,
    strategy: UseStrategyEnum.USE_EXIST,
    configId: null as string | null,
    config: null as string | null,
    compareConf: [] as string[],
    project: null as string | null,
    module: null as string | null,
    alertId: null as string | null,
    resolveOrder: 0,
    remoteClusterId: null as string | null,
    yarnSessionClusterId: null as string | null,
    k8sSessionClusterId: null as string | null,
    k8sNamespace: '',
    serviceAccount: '',
    flinkImage: '',
    k8sRestExposedType: 0,
    useSysHadoopConf: false,
    yarnQueue: '',
    hadoopUser: '',
    checkPointFailure: {
        cpMaxFailureInterval: null as number | null,
        cpFailureRateInterval: null as number | null,
        cpFailureAction: null as number | null,
    },
})

const rules: FormRules = {
    jobName: [{ required: true, trigger: 'blur' }],
    versionId: [{ required: true, trigger: 'change' }],
}

const isSqlJob = computed(() => formModel.value.jobType === JobTypeEnum.SQL)
const isPyflinkJob = computed(() => formModel.value.jobType === JobTypeEnum.PYFLINK)
const jobTypeOptions = computed(() => {
    const options = [
        { label: 'JAR', value: JobTypeEnum.JAR },
        { label: 'SQL', value: JobTypeEnum.SQL },
    ]
    if (formModel.value.jobType === JobTypeEnum.PYFLINK || app.jobType === JobTypeEnum.PYFLINK)
        options.push({ label: 'PYFLINK', value: JobTypeEnum.PYFLINK })
    return options
})
const isK8sApp = computed(() => formModel.value.deployMode === DeployMode.KUBERNETES_APPLICATION)
const showUploadJar = computed(
    () => !isSqlJob.value && app.resourceFrom === ResourceFromEnum.UPLOAD,
)
const showProjectConf = computed(
    () => !isSqlJob.value && app.resourceFrom === ResourceFromEnum.PROJECT,
)
const showSqlConfig = computed(() => isSqlJob.value && !isK8sDeployMode(formModel.value.deployMode))
const showCompareConf = computed(
    () =>
        showProjectConf.value &&
        formModel.value.strategy === UseStrategyEnum.USE_EXIST &&
        configVersions.value.length > 1,
)
const configDrawerVisible = ref(false)

function handleConfigOk(payload: { isSetConfig: boolean; configOverride: string | null }) {
    formModel.value.isSetConfig = payload.isSetConfig
    formModel.value.configOverride = payload.configOverride ?? ''
}

async function loadEnvs() {
    const result = await fetchFlinkEnvPage({ pageNum: 1, pageSize: 999 })
    if (result.isSuccess) flinkEnvs.value = resolveListData(result.data).records
}

async function loadFlinkSqlHistory() {
    if (!app.id || app.jobType !== JobTypeEnum.SQL) return
    const result = await fetchFlinkSqlList({ appId: app.id, pageNum: 1, pageSize: 200 })
    if (result.isSuccess)
        flinkSqlHistory.value = resolveListData<{
            id: string
            version: number
            effective?: boolean
        }>(
            result.data as {
                records?: Array<{ id: string; version: number; effective?: boolean }>
                total?: number | string
            },
        ).records
}

async function loadConfigVersions() {
    if (!app.id) return
    const result = await fetchConfHistory({ id: app.id })
    if (!result.isSuccess) return
    configVersions.value = (result.data ?? []) as Array<{
        id: string
        version?: number
        effective?: boolean
    }>
}

async function initForm() {
    if (!route.query.appId) {
        window.$message?.warning(t('flink.app.editStreamPark.appidCheck'))
        router.push('/flink/app')
        return
    }
    await handleGetApplication()
    await loadConfigVersions()
    let effectiveConfigId: string | null = null
    configVersions.value.forEach((conf) => {
        if (conf.effective) effectiveConfigId = conf.id
    })
    const resetParams = handleResetApplication()
    formModel.value = {
        jobName: app.jobName ?? '',
        tags: app.tags ?? '',
        description: app.description ?? '',
        jobType: app.jobType ?? JobTypeEnum.JAR,
        deployMode: app.deployMode ?? 0,
        versionId: app.versionId ?? null,
        mainClass: app.mainClass ?? '',
        jar: app.jar ?? '',
        args: app.args ?? '',
        flinkSql: decodeSqlValue(app.flinkSql),
        configOverride: (() => {
            if (!app.config) return ''
            try {
                return decodeByBase64(app.config)
            } catch {
                return app.config
            }
        })(),
        isSetConfig: Boolean(app.config),
        flinkSqlHistory: app.sqlId ?? null,
        teamResource: handleTeamResource(app.teamResource as string),
        dynamicProperties: app.dynamicProperties ?? '',
        restartSize: app.restartSize ?? 0,
        strategy: UseStrategyEnum.USE_EXIST,
        configId: effectiveConfigId ?? (app.configId as string) ?? null,
        config: (app as Recordable).configPath ?? null,
        compareConf: [],
        project: app.projectId ?? null,
        module: app.module ?? null,
        yarnQueue: app.yarnQueue ?? '',
        hadoopUser: app.hadoopUser ?? '',
        jmOptionsItem: {},
        tmOptionsItem: {},
        totalItem: {},
        ...resetParams,
        ...mapEditDeployFields(app),
    } as typeof formModel.value

    k8sTemplate.podTemplate = app.k8sPodTemplate ?? ''
    k8sTemplate.jmPodTemplate = app.k8sJmPodTemplate ?? ''
    k8sTemplate.tmPodTemplate = app.k8sTmPodTemplate ?? ''

    await nextTick()
    setTimeout(() => {
        if (app.dependency) {
            try {
                dependencyRef.value?.setDefaultValue(JSON.parse(app.dependency))
            } catch {
                /* ignore invalid dependency json */
            }
        }
        if (app.k8sPodTemplate)
            podTemplateRef.value?.handleChoicePodTemplate('ptVisual', app.k8sPodTemplate)
        if (app.k8sJmPodTemplate)
            podTemplateRef.value?.handleChoicePodTemplate('jmPtVisual', app.k8sJmPodTemplate)
        if (app.k8sTmPodTemplate)
            podTemplateRef.value?.handleChoicePodTemplate('tmPtVisual', app.k8sTmPodTemplate)
    }, 500)
    await loadFlinkSqlHistory()
}

async function handleCustomJobRequest(data: { file: File }) {
    try {
        uploadLoading.value = true
        const formData = new FormData()
        formData.append('file', data.file)
        const result = await fetchUpload(formData)
        if (!result.isSuccess) throwApiFailure(result, t('sys.api.apiRequestFailed'))
        formModel.value.jar = data.file.name
        if (result.data?.mainClass) formModel.value.mainClass = result.data.mainClass
    } catch (e: any) {
        showCatchError(e, t('sys.api.apiRequestFailed'))
    } finally {
        uploadLoading.value = false
    }
}

async function buildDependencyParam() {
    await dependencyRef.value?.handleApplyPom()
    return buildDependencyJson(
        dependencyRef.value?.dependencyRecords ?? [],
        dependencyRef.value?.uploadJars ?? [],
    )
}

async function handleSubmit() {
    await formRef.value?.validate()
    if (isSqlJob.value && !formModel.value.flinkSql?.trim()) {
        window.$message?.warning(t('flink.app.editStreamPark.flinkSqlRequired'))
        return
    }
    submitting.value = true
    try {
        if (isSqlJob.value) {
            const verified = await flinkSqlEditorRef.value?.handleVerifySql(false)
            if (!verified) {
                window.$message?.warning(t('flink.app.editStreamPark.sqlCheck'))
                return
            }
            await submitSqlJob()
        } else {
            await submitCustomJob()
        }
    } catch (e: any) {
        showCatchError(e, t('sys.api.apiRequestFailed'))
    } finally {
        submitting.value = false
    }
}

async function submitSqlJob() {
    let config: string | null = formModel.value.isSetConfig
        ? formModel.value.configOverride?.trim() || null
        : null
    if (config) config = encryptByBase64(config)

    const params: Recordable = {
        id: app.id,
        flinkSql: formModel.value.flinkSql,
        dependency: await buildDependencyParam(),
        sqlId: formModel.value.flinkSqlHistory || app.sqlId || null,
        config,
        format:
            formModel.value.isSetConfig && config
                ? getAppConfType(formModel.value.configOverride)
                : null,
        teamResource: JSON.stringify(formModel.value.teamResource ?? []),
    }
    handleSubmitParams(params, formModel.value, k8sTemplate)
    await submitUpdate(params)
}

async function submitCustomJob() {
    const values = formModel.value
    const format =
        values.strategy === UseStrategyEnum.USE_EXIST
            ? app.format
            : getAppConfType(values.config || '')
    let config: string | null = values.configOverride?.trim() || null
    if (config) config = encryptByBase64(config)
    else config = app.config ?? null

    const params: Recordable = {
        id: app.id,
        jar: values.jar,
        mainClass: values.mainClass,
        format,
        configId:
            values.strategy === UseStrategyEnum.USE_EXIST
                ? (values.configId ?? app.configId ?? null)
                : null,
        config,
    }
    handleSubmitParams(params, values, k8sTemplate)
    await submitUpdate(params)
}

async function submitUpdate(params: Recordable) {
    const result = await fetchAppUpdate(params)
    if (!result.isSuccess) throwApiFailure(result, t('sys.api.apiRequestFailed'))
    if (!result.data) {
        showResultError(result, t('sys.api.apiRequestFailed'))
        return
    }
    window.$message?.success(t('flink.app.editStreamPark.success'))
    router.push('/flink/app')
}

onMounted(async () => {
    await loadEnvs()
    await initForm()
})
</script>

<template>
    <n-card :bordered="false" :title="t('flink.app.operation.edit')">
        <template #header-extra>
            <n-button quaternary @click="router.push('/flink/app')">
                {{ t('common.cancelText') }}
            </n-button>
        </template>
        <n-form
            ref="formRef"
            :model="formModel"
            :rules="rules"
            label-placement="left"
            label-width="140"
        >
            <n-grid :cols="24" :x-gap="16">
                <n-form-item-gi :span="12" :label="t('flink.app.appName')" path="jobName">
                    <n-input v-model:value="formModel.jobName" />
                </n-form-item-gi>
                <n-form-item-gi :span="12" :label="t('flink.app.tags')">
                    <n-input v-model:value="formModel.tags" />
                </n-form-item-gi>
                <n-form-item-gi :span="24" :label="t('common.description')">
                    <n-input v-model:value="formModel.description" type="textarea" :rows="2" />
                </n-form-item-gi>
                <n-form-item-gi :span="12" :label="t('flink.app.jobType')">
                    <n-select
                        v-model:value="formModel.jobType"
                        :options="jobTypeOptions"
                        disabled
                    />
                </n-form-item-gi>
                <n-form-item-gi :span="12" :label="t('flink.app.deployMode')">
                    <n-select
                        v-model:value="formModel.deployMode"
                        :options="deployModes.map((d) => ({ label: d.label, value: d.value }))"
                    />
                </n-form-item-gi>
                <n-form-item-gi :span="12" :label="t('flink.app.flinkVersion')" path="versionId">
                    <n-select
                        v-model:value="formModel.versionId"
                        :options="flinkEnvs.map((e) => ({ label: e.flinkName, value: e.id }))"
                        filterable
                    />
                </n-form-item-gi>
                <n-form-item-gi :span="24">
                    <AppDeployFields v-model="formModel" />
                </n-form-item-gi>
                <template v-if="!isSqlJob">
                    <n-form-item-gi :span="12" :label="t('flink.app.mainClass')">
                        <n-input
                            v-model:value="formModel.mainClass"
                            :placeholder="
                                isPyflinkJob
                                    ? t('flink.app.addAppTips.mainClassPlaceholder')
                                    : undefined
                            "
                        />
                    </n-form-item-gi>
                    <n-form-item-gi
                        v-if="showUploadJar"
                        :span="24"
                        :label="t('flink.app.uploadJobJar')"
                    >
                        <UploadJobJar
                            :custom-request="handleCustomJobRequest"
                            :loading="uploadLoading"
                        >
                            <template #uploadInfo>
                                <n-alert v-if="formModel.jar" type="info" class="mt-8px">
                                    {{ formModel.jar }}
                                </n-alert>
                            </template>
                        </UploadJobJar>
                    </n-form-item-gi>
                    <n-form-item-gi v-else :span="12" :label="t('common.jobType.jar')">
                        <n-input v-model:value="formModel.jar" />
                    </n-form-item-gi>
                    <n-form-item-gi :span="24" :label="t('flink.app.programArgs')">
                        <ProgramArgsEditor v-model="formModel.args" />
                    </n-form-item-gi>
                </template>
                <n-form-item-gi v-else :span="24" :label="t('flink.app.flinkSqlLabel')">
                    <FlinkSqlEditor
                        ref="flinkSqlEditorRef"
                        v-model="formModel.flinkSql"
                        :version-id="formModel.versionId"
                        :app-id="app.id"
                    />
                </n-form-item-gi>
                <n-form-item-gi
                    v-if="isSqlJob && flinkSqlHistory.length"
                    :span="24"
                    :label="t('flink.app.editStreamPark.sqlHistory')"
                >
                    <n-select
                        v-model:value="formModel.flinkSqlHistory"
                        clearable
                        :options="
                            flinkSqlHistory.map((item) => ({
                                label: `v${item.version}${item.effective ? ' (effective)' : ''}`,
                                value: item.id,
                            }))
                        "
                    />
                </n-form-item-gi>
                <n-form-item-gi v-if="isSqlJob" :span="24" :label="t('flink.app.dependency')">
                    <Dependency
                        ref="dependencyRef"
                        :form-model="formModel"
                        :flink-envs="flinkEnvs"
                    />
                </n-form-item-gi>
                <n-form-item-gi v-if="isSqlJob" :span="24" :label="t('flink.app.resource')">
                    <TeamResourceField v-model="formModel.teamResource" />
                </n-form-item-gi>
                <n-form-item-gi v-if="isK8sApp" :span="24" :label="t('flink.app.podTemplate')">
                    <PodTemplateTab
                        ref="podTemplateRef"
                        v-model:pod-template="k8sTemplate.podTemplate"
                        v-model:jm-pod-template="k8sTemplate.jmPodTemplate"
                        v-model:tm-pod-template="k8sTemplate.tmPodTemplate"
                    />
                </n-form-item-gi>
                <n-form-item-gi v-if="showProjectConf" :span="24" :label="t('flink.app.appConf')">
                    <AppConf
                        v-model="formModel"
                        :project-id="formModel.project"
                        :module="formModel.module"
                        :config-versions="configVersions"
                    />
                </n-form-item-gi>
                <n-form-item-gi
                    v-if="showCompareConf"
                    :span="24"
                    :label="t('flink.app.detail.compareConfig')"
                >
                    <CompareConfPicker
                        v-model:value="formModel.compareConf"
                        :app-id="app.id ?? ''"
                        :config-versions="configVersions"
                    />
                </n-form-item-gi>
                <n-form-item-gi v-if="showSqlConfig" :span="24" :label="t('flink.app.appConf')">
                    <n-space>
                        <n-switch v-model:value="formModel.isSetConfig">
                            <template #checked>ON</template>
                            <template #unchecked>OFF</template>
                        </n-switch>
                        <n-button
                            v-if="formModel.isSetConfig"
                            size="small"
                            @click="configDrawerVisible = true"
                        >
                            {{ t('common.editText') }}
                        </n-button>
                    </n-space>
                </n-form-item-gi>
                <n-form-item-gi
                    v-else-if="!isSqlJob && showUploadJar"
                    :span="24"
                    :label="t('flink.app.appConf')"
                >
                    <n-space>
                        <n-switch v-model:value="formModel.isSetConfig">
                            <template #checked>ON</template>
                            <template #unchecked>OFF</template>
                        </n-switch>
                        <n-button
                            v-if="formModel.isSetConfig"
                            size="small"
                            @click="configDrawerVisible = true"
                        >
                            {{ t('common.editText') }}
                        </n-button>
                    </n-space>
                </n-form-item-gi>
                <n-form-item-gi :span="24">
                    <AppAdvancedFields v-model="formModel" />
                </n-form-item-gi>
            </n-grid>
        </n-form>
        <MergelyDrawer
            v-model:show="configDrawerVisible"
            template-source="flink"
            :original-value="formModel.configOverride"
            @ok="handleConfigOk"
        />
        <div class="mt-24px flex justify-center gap-12px">
            <n-button @click="router.push('/flink/app')">
                {{ t('common.cancelText') }}
            </n-button>
            <n-button type="primary" :loading="submitting" @click="handleSubmit">
                {{ t('common.submitText') }}
            </n-button>
        </div>
    </n-card>
</template>
