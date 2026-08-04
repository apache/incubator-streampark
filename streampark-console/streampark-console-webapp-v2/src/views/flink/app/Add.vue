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
import type { CreateParams } from '@/types/api/flink/app.type'
import { fetchAppConf, fetchAppCreate, fetchCheckName, fetchFlinkEnvPage } from '@/service'
import { resolveListData } from '@/views/system/shared/utils'
import { deployModes } from '@/views/flink/app/shared/data/index'
import { buildDependencyJson } from '@/views/flink/app/shared/pom'
import { getAppConfType, handleSubmitParams, isK8sDeployMode } from '@/views/flink/app/shared/utils'
import MergelyDrawer from '@/views/shared/editors/MergelyDrawer.vue'
import AppAdvancedFields from './components/AppAdvancedFields.vue'
import AppDeployFields from './components/AppDeployFields.vue'
import AppResourceFields from './components/AppResourceFields.vue'
import Dependency from './components/Dependency.vue'
import FlinkSqlEditor from './components/FlinkSqlEditor.vue'
import PodTemplateTab from './components/PodTemplateTab.vue'
import { AppTypeEnum, DeployMode, JobTypeEnum, ResourceFromEnum } from '@/enums/flinkEnum'
import { encryptByBase64 } from '@/utils/cipher'
import { buildUUID } from '@/utils/uuid'
import { createLocalStorage } from '@/utils/cache'

defineOptions({ name: 'FlinkAppAdd' })

const { t } = useI18n()
const router = useRouter()
const ls = createLocalStorage()
const formRef = ref<FormInst | null>(null)
const dependencyRef = ref<InstanceType<typeof Dependency> | null>(null)
const flinkSqlEditorRef = ref<InstanceType<typeof FlinkSqlEditor> | null>(null)
const submitting = ref(false)
const flinkEnvs = ref<
    Array<{ id: string; flinkName: string; isDefault?: boolean; scalaVersion?: string }>
>([])

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
    deployMode: DeployMode.YARN_APPLICATION,
    versionId: null as string | null,
    resourceFrom: ResourceFromEnum.PROJECT,
    project: null as string | null,
    module: null as string | null,
    appType: AppTypeEnum.STREAMPARK_FLINK,
    config: null as string | null,
    uploadJobJar: null as string | null,
    mainClass: '',
    jar: '',
    args: '',
    flinkSql: '',
    teamResource: [] as string[],
    parallelism: 1,
    slot: 1,
    restartSize: 0,
    dynamicProperties: '',
    jmOptions: [] as string[],
    tmOptions: [] as string[],
    jmOptionsItem: {} as Recordable,
    tmOptionsItem: {} as Recordable,
    totalOptions: [] as string[],
    totalItem: {} as Recordable,
    isSetConfig: false,
    configOverride: '',
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

const configDrawerVisible = ref(false)

function handleConfigOk(payload: { isSetConfig: boolean; configOverride: string | null }) {
    formModel.value.isSetConfig = payload.isSetConfig
    formModel.value.configOverride = payload.configOverride ?? ''
}

const rules: FormRules = {
    jobName: [
        { required: true, message: t('flink.app.addAppTips.appNameNotValid'), trigger: 'blur' },
    ],
    versionId: [
        {
            required: true,
            message: t('flink.app.addAppTips.flinkVersionIsRequiredMessage'),
            trigger: 'change',
        },
    ],
}

const isSqlJob = computed(() => formModel.value.jobType === JobTypeEnum.SQL)
const isK8sApp = computed(() => formModel.value.deployMode === DeployMode.KUBERNETES_APPLICATION)
const showSqlConfig = computed(() => isSqlJob.value && !isK8sDeployMode(formModel.value.deployMode))

async function loadEnvs() {
    const result = await fetchFlinkEnvPage({ pageNum: 1, pageSize: 999 })
    if (!result.isSuccess) return
    flinkEnvs.value = resolveListData(result.data).records
    const defaultEnv = flinkEnvs.value.find((v) => v.isDefault)
    if (defaultEnv) formModel.value.versionId = defaultEnv.id
}

async function checkJobName(): Promise<boolean> {
    const jobName = formModel.value.jobName?.trim()
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

function validateResourceFields(): boolean {
    const values = formModel.value
    if (isSqlJob.value) return true

    if (values.resourceFrom === ResourceFromEnum.PROJECT) {
        if (!values.project) {
            window.$message?.warning(t('flink.app.addAppTips.projectIsRequiredMessage'))
            return false
        }
        if (!values.module) {
            window.$message?.warning(t('flink.app.addAppTips.projectIsRequiredMessage'))
            return false
        }
        if (values.appType === AppTypeEnum.STREAMPARK_FLINK && !values.config) {
            window.$message?.warning(t('flink.app.addAppTips.configRequired'))
            return false
        }
        if (values.appType === AppTypeEnum.APACHE_FLINK) {
            if (!values.jar) {
                window.$message?.warning(t('flink.app.addAppTips.programJarIsRequiredMessage'))
                return false
            }
            if (values.jobType !== JobTypeEnum.PYFLINK && !values.mainClass?.trim()) {
                window.$message?.warning(t('flink.app.addAppTips.mainClassIsRequiredMessage'))
                return false
            }
        }
        return true
    }

    const jarName = values.uploadJobJar || values.jar
    if (!jarName) {
        window.$message?.warning(t('flink.app.addAppTips.programJarIsRequiredMessage'))
        return false
    }
    if (values.jobType !== JobTypeEnum.PYFLINK && !values.mainClass?.trim()) {
        window.$message?.warning(t('flink.app.addAppTips.mainClassIsRequiredMessage'))
        return false
    }
    return true
}

function validateDeployFields(): boolean {
    const values = formModel.value
    if (values.deployMode === DeployMode.STANDALONE && !values.remoteClusterId) {
        window.$message?.warning(t('flink.app.addAppTips.flinkClusterIsRequiredMessage'))
        return false
    }
    if (values.deployMode === DeployMode.YARN_SESSION && !values.yarnSessionClusterId) {
        window.$message?.warning(t('flink.app.addAppTips.flinkClusterIsRequiredMessage'))
        return false
    }
    if (values.deployMode === DeployMode.KUBERNETES_SESSION && !values.k8sSessionClusterId) {
        window.$message?.warning(t('flink.app.addAppTips.flinkClusterIsRequiredMessage'))
        return false
    }
    if (values.deployMode === DeployMode.KUBERNETES_APPLICATION && !values.flinkImage?.trim()) {
        window.$message?.warning(t('flink.app.addAppTips.flinkImageIsRequiredMessage'))
        return false
    }
    return true
}

async function buildDependencyParam() {
    await dependencyRef.value?.handleApplyPom()
    return buildDependencyJson(
        dependencyRef.value?.dependencyRecords ?? [],
        dependencyRef.value?.uploadJars ?? [],
    )
}

function stripNullParams(params: Recordable) {
    const result: Recordable = {}
    for (const key in params) {
        const value = params[key]
        if (value != null) result[key] = value
    }
    return result
}

async function buildSqlParams(values: Recordable) {
    let config: string | null = values.configOverride?.trim() || null
    if (config) config = encryptByBase64(config)

    const params: Recordable = {
        jobType: JobTypeEnum.SQL,
        flinkSql: values.flinkSql,
        appType: AppTypeEnum.STREAMPARK_FLINK,
        config,
        format: values.isSetConfig && config ? getAppConfType(values.configOverride) : null,
        teamResource: JSON.stringify(values.teamResource ?? []),
        dependency: await buildDependencyParam(),
    }
    handleSubmitParams(params, values, k8sTemplate)
    return stripNullParams(params)
}

async function buildCustomJobParams(values: Recordable) {
    const params: Recordable = {
        jobType: values.jobType,
        projectId: values.project || null,
        module: values.module || null,
        dependency: await buildDependencyParam(),
        appType: values.appType,
    }
    handleSubmitParams(params, values, k8sTemplate)

    if (values.resourceFrom === ResourceFromEnum.PROJECT) {
        params.resourceFrom = ResourceFromEnum.PROJECT
        if (values.appType === AppTypeEnum.STREAMPARK_FLINK) {
            params.format = getAppConfType(values.config)
            if (values.configOverride?.trim()) {
                params.config = encryptByBase64(values.configOverride.trim())
            } else {
                const confResult = await fetchAppConf({ config: values.config })
                if (!confResult.isSuccess)
                    throwApiFailure(confResult, t('sys.api.apiRequestFailed'))
                params.config = confResult.data
            }
        } else {
            params.jar = values.jar || null
            params.mainClass = values.mainClass || null
        }
    } else {
        Object.assign(params, {
            resourceFrom: ResourceFromEnum.UPLOAD,
            appType: AppTypeEnum.APACHE_FLINK,
            jar: values.uploadJobJar || values.jar,
            mainClass: values.mainClass,
        })
    }
    return stripNullParams(params)
}

async function handleSubmit() {
    await formRef.value?.validate()
    if (!(await checkJobName())) return
    if (!validateResourceFields()) return
    if (!validateDeployFields()) return

    if (isSqlJob.value && !formModel.value.flinkSql?.trim()) {
        window.$message?.warning(t('flink.app.editStreamPark.flinkSqlRequired'))
        return
    }
    if (isSqlJob.value) {
        const verified = await flinkSqlEditorRef.value?.handleVerifySql(false)
        if (!verified) {
            window.$message?.warning(t('flink.app.editStreamPark.sqlCheck'))
            return
        }
    }

    submitting.value = true
    try {
        const values = { ...formModel.value }
        const params = isSqlJob.value
            ? await buildSqlParams(values)
            : await buildCustomJobParams(values)

        const socketId = buildUUID()
        ls.set('DOWN_SOCKET_ID', socketId)
        params.socketId = socketId

        const result = await fetchAppCreate(params as CreateParams)
        if (!result.isSuccess) throwApiFailure(result, t('sys.api.apiRequestFailed'))
        const payload = result.data as boolean | { data?: boolean; message?: string }
        const ok = typeof payload === 'boolean' ? payload : payload?.data
        if (!ok) {
            const msg =
                (typeof payload === 'object' ? payload?.message : result.message) ||
                t('sys.api.apiRequestFailed')
            window.$message?.error(String(msg).replaceAll(/\[StreamPark]/g, ''))
            return
        }
        router.push('/flink/app')
    } catch (e: any) {
        showCatchError(e, t('sys.api.apiRequestFailed'))
    } finally {
        submitting.value = false
    }
}

onMounted(() => loadEnvs())
</script>

<template>
    <n-card :bordered="false" :title="t('common.add')">
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
                    <n-input
                        v-model:value="formModel.jobName"
                        :placeholder="t('flink.app.searchName')"
                        @blur="checkJobName"
                    />
                </n-form-item-gi>
                <n-form-item-gi :span="12" :label="t('flink.app.tags')">
                    <n-input v-model:value="formModel.tags" :placeholder="t('flink.app.tags')" />
                </n-form-item-gi>
                <n-form-item-gi :span="24" :label="t('common.description')">
                    <n-input v-model:value="formModel.description" type="textarea" :rows="2" />
                </n-form-item-gi>
                <n-form-item-gi :span="12" :label="t('flink.app.jobType')">
                    <n-select
                        v-model:value="formModel.jobType"
                        :options="[
                            { label: 'JAR', value: JobTypeEnum.JAR },
                            { label: 'SQL', value: JobTypeEnum.SQL },
                            { label: 'PYFLINK', value: JobTypeEnum.PYFLINK },
                        ]"
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
                        :placeholder="t('flink.app.addAppTips.flinkVersionIsRequiredMessage')"
                        filterable
                    />
                </n-form-item-gi>
                <n-form-item-gi :span="24" :show-label="false" :show-feedback="false">
                    <AppDeployFields v-model="formModel" />
                </n-form-item-gi>
                <n-form-item-gi :span="24" :show-label="false" :show-feedback="false">
                    <AppResourceFields v-model="formModel" />
                </n-form-item-gi>
                <n-form-item-gi v-if="isSqlJob" :span="24" :label="t('flink.app.flinkSqlLabel')">
                    <FlinkSqlEditor
                        ref="flinkSqlEditorRef"
                        v-model="formModel.flinkSql"
                        :version-id="formModel.versionId"
                    />
                </n-form-item-gi>
                <n-form-item-gi v-if="isSqlJob" :span="24" :label="t('flink.app.dependency')">
                    <Dependency
                        ref="dependencyRef"
                        :form-model="formModel"
                        :flink-envs="flinkEnvs"
                    />
                </n-form-item-gi>
                <n-form-item-gi v-if="isK8sApp" :span="24" :label="t('flink.app.podTemplate')">
                    <PodTemplateTab
                        v-model:pod-template="k8sTemplate.podTemplate"
                        v-model:jm-pod-template="k8sTemplate.jmPodTemplate"
                        v-model:tm-pod-template="k8sTemplate.tmPodTemplate"
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
                <n-form-item-gi :span="24" :show-label="false" :show-feedback="false">
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
