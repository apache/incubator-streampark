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
import {
    fetchMain,
    fetchName,
    fetchProjectJars,
    fetchProjectListConf,
    fetchProjectModules,
    fetchProjectSelect,
    fetchTeamResource,
    fetchUpload,
} from '@/service'
import ProgramArgsEditor from '@/views/shared/editors/ProgramArgsEditor.vue'
import TeamResourceField from '@/views/shared/components/TeamResourceField.vue'
import UploadJobJar from './UploadJobJar.vue'
import { ResourceTypeEnum } from '@/views/resource/upload/shared/constants'
import { getAppConfType } from '@/views/flink/app/shared/utils'
import { AppTypeEnum, ConfigTypeEnum, JobTypeEnum, ResourceFromEnum } from '@/enums/flinkEnum'

const model = defineModel<Recordable>({ required: true })

const { t } = useI18n()

const projectList = ref<Array<{ id: string; name: string }>>([])
const moduleList = ref<Array<{ label: string; value: string }>>([])
const jarList = ref<string[]>([])
const confTree = ref<Recordable[]>([])
const teamResources = ref<Recordable[]>([])
const uploadLoading = ref(false)

const isSqlJob = computed(() => model.value.jobType === JobTypeEnum.SQL)
const isProject = computed(() => model.value.resourceFrom === ResourceFromEnum.PROJECT)
const isUpload = computed(() => model.value.resourceFrom === ResourceFromEnum.UPLOAD)
const isApacheFlink = computed(() => model.value.appType === AppTypeEnum.APACHE_FLINK)
const isStreamParkFlink = computed(() => model.value.appType === AppTypeEnum.STREAMPARK_FLINK)

const resourceFromOptions = [
    { label: t('flink.app.resourceFromProject'), value: ResourceFromEnum.PROJECT },
    { label: t('flink.app.resourceFromUpload'), value: ResourceFromEnum.UPLOAD },
]

const appTypeOptions = [
    { label: 'StreamPark Flink', value: AppTypeEnum.STREAMPARK_FLINK },
    { label: 'Apache Flink', value: AppTypeEnum.APACHE_FLINK },
]

const uploadJarOptions = computed(() =>
    teamResources.value
        .filter((item) => item.resourceType === ResourceTypeEnum.APP)
        .map((resource) => ({
            value: resource.resourceName,
            label: resource.resourceName,
            resource,
        })),
)

async function loadProjects() {
    const result = await fetchProjectSelect({})
    if (result.isSuccess)
        projectList.value = (result.data ?? []) as Array<{ id: string; name: string }>
}

async function loadTeamResources() {
    const result = await fetchTeamResource({})
    if (result.isSuccess) teamResources.value = result.data ?? []
}

async function loadModules(projectId: string) {
    const result = await fetchProjectModules({ id: projectId })
    if (result.isSuccess)
        moduleList.value = (result.data ?? []).map((name: string) => ({ label: name, value: name }))
}

async function loadJars() {
    if (!model.value.project || !model.value.module) return
    const result = await fetchProjectJars({ id: model.value.project, module: model.value.module })
    if (result.isSuccess) jarList.value = result.data ?? []
}

async function loadConfTree() {
    if (!model.value.project || !model.value.module) return
    const result = await fetchProjectListConf({
        id: model.value.project,
        module: model.value.module,
    })
    if (result.isSuccess) confTree.value = (result.data ?? []) as Recordable[]
}

function resetProjectCascade() {
    model.value.module = null
    model.value.appType = AppTypeEnum.STREAMPARK_FLINK
    model.value.jar = ''
    model.value.mainClass = ''
    model.value.config = null
    moduleList.value = []
    jarList.value = []
    confTree.value = []
}

function resetModuleCascade() {
    model.value.appType = AppTypeEnum.STREAMPARK_FLINK
    model.value.jar = ''
    model.value.mainClass = ''
    model.value.config = null
    jarList.value = []
    confTree.value = []
}

async function handleProjectChange(projectId: string | null) {
    resetProjectCascade()
    if (projectId) await loadModules(projectId)
}

async function handleModuleChange(_module: string | null) {
    resetModuleCascade()
    if (model.value.project && model.value.module) {
        await loadConfTree()
        if (model.value.appType === AppTypeEnum.APACHE_FLINK) await loadJars()
    }
}

async function handleAppTypeChange(appType: number) {
    model.value.jar = ''
    model.value.mainClass = ''
    model.value.config = null
    if (appType === AppTypeEnum.APACHE_FLINK && model.value.project && model.value.module)
        await loadJars()
}

async function handleJarChange(jar: string | null) {
    if (!jar || !model.value.project || !model.value.module) return
    const result = await fetchMain({
        projectId: model.value.project,
        module: model.value.module,
        jar,
    })
    if (result.isSuccess && result.data) model.value.mainClass = result.data
}

async function handleConfigChange(configPath: string | null) {
    if (!configPath) {
        model.value.config = null
        return
    }
    const confType = getAppConfType(configPath)
    if (confType === ConfigTypeEnum.UNKNOWN) {
        window.$message?.warning(t('flink.app.addAppTips.configFileInvalid'))
        model.value.config = null
        return
    }
    model.value.config = configPath
    const nameResult = await fetchName({ config: configPath })
    if (nameResult.isSuccess && nameResult.data) model.value.jobName = nameResult.data
}

function handleUploadJarSelect(resourceName: string | null) {
    model.value.uploadJobJar = resourceName
    if (!resourceName) {
        model.value.mainClass = ''
        return
    }
    const resource = teamResources.value.find((item) => item.resourceName === resourceName)
    if (resource?.mainClass) model.value.mainClass = resource.mainClass
}

async function handleCustomJobRequest(data: { file: File }) {
    try {
        uploadLoading.value = true
        const formData = new FormData()
        formData.append('file', data.file)
        const result = await fetchUpload(formData)
        if (!result.isSuccess) throwApiFailure(result, t('sys.api.apiRequestFailed'))
        model.value.uploadJobJar = data.file.name
        model.value.jar = result.data?.path ?? data.file.name
        if (result.data?.mainClass) model.value.mainClass = result.data.mainClass
    } catch (e: any) {
        showCatchError(e, t('sys.api.apiRequestFailed'))
    } finally {
        uploadLoading.value = false
    }
}

watch(
    () => model.value.jobType,
    (jobType) => {
        if (jobType !== JobTypeEnum.SQL) model.value.resourceFrom = ResourceFromEnum.PROJECT
    },
)

onMounted(async () => {
    await Promise.all([loadProjects(), loadTeamResources()])
})
</script>

<template>
    <n-grid :cols="24" :x-gap="16">
        <template v-if="isSqlJob">
            <n-form-item-gi :span="24" :label="t('flink.app.resource')">
                <TeamResourceField v-model="model.teamResource" />
            </n-form-item-gi>
        </template>
        <template v-else>
            <n-form-item-gi :span="12" :label="t('flink.app.resourceFrom')">
                <n-select
                    v-model:value="model.resourceFrom"
                    :options="resourceFromOptions"
                    :placeholder="t('flink.app.resourceFromPlaceholder')"
                />
            </n-form-item-gi>
            <template v-if="isProject">
                <n-form-item-gi :span="12" :label="t('flink.app.project')">
                    <n-select
                        v-model:value="model.project"
                        filterable
                        :options="projectList.map((p) => ({ label: p.name, value: p.id }))"
                        :placeholder="t('flink.app.addAppTips.projectPlaceholder')"
                        @update:value="handleProjectChange"
                    />
                </n-form-item-gi>
                <n-form-item-gi :span="12" :label="t('flink.app.module')">
                    <n-select
                        v-model:value="model.module"
                        filterable
                        :options="moduleList"
                        :placeholder="t('flink.app.addAppTips.projectModulePlaceholder')"
                        @update:value="handleModuleChange"
                    />
                </n-form-item-gi>
                <n-form-item-gi :span="12" :label="t('flink.app.appType')">
                    <n-select
                        v-model:value="model.appType"
                        :options="appTypeOptions"
                        :placeholder="t('flink.app.addAppTips.appTypePlaceholder')"
                        @update:value="handleAppTypeChange"
                    />
                </n-form-item-gi>
                <n-form-item-gi v-if="isApacheFlink" :span="12" :label="t('flink.app.programJar')">
                    <n-select
                        v-model:value="model.jar"
                        filterable
                        :options="jarList.map((j) => ({ label: j, value: j }))"
                        :placeholder="t('flink.app.addAppTips.programJarIsRequiredMessage')"
                        @update:value="handleJarChange"
                    />
                </n-form-item-gi>
                <n-form-item-gi v-if="isApacheFlink" :span="12" :label="t('flink.app.mainClass')">
                    <n-input
                        v-model:value="model.mainClass"
                        :placeholder="t('flink.app.addAppTips.mainClassPlaceholder')"
                    />
                </n-form-item-gi>
                <n-form-item-gi v-if="isStreamParkFlink" :span="24" :label="t('flink.app.appConf')">
                    <n-tree-select
                        v-model:value="model.config"
                        filterable
                        clearable
                        :options="confTree"
                        key-field="value"
                        label-field="title"
                        children-field="children"
                        :placeholder="t('flink.app.addAppTips.configSelectPlaceholder')"
                        @update:value="handleConfigChange"
                    />
                </n-form-item-gi>
            </template>
            <template v-else-if="isUpload">
                <n-form-item-gi :span="12" :label="t('flink.app.selectJobJar')">
                    <n-select
                        v-model:value="model.uploadJobJar"
                        filterable
                        clearable
                        :options="uploadJarOptions"
                        :placeholder="t('flink.app.selectAppPlaceHolder')"
                        @update:value="handleUploadJarSelect"
                    />
                </n-form-item-gi>
                <n-form-item-gi :span="12" :label="t('flink.app.mainClass')">
                    <n-input
                        v-model:value="model.mainClass"
                        :placeholder="t('flink.app.addAppTips.mainClassPlaceholder')"
                    />
                </n-form-item-gi>
                <n-form-item-gi :span="24" :label="t('flink.app.uploadJobJar')">
                    <UploadJobJar :custom-request="handleCustomJobRequest" :loading="uploadLoading">
                        <template #uploadInfo>
                            <n-alert
                                v-if="model.jar || model.uploadJobJar"
                                type="info"
                                class="mt-8px"
                            >
                                {{ model.uploadJobJar || model.jar }}
                            </n-alert>
                        </template>
                    </UploadJobJar>
                </n-form-item-gi>
            </template>
            <n-form-item-gi :span="24" :label="t('flink.app.programArgs')">
                <ProgramArgsEditor v-model="model.args" />
            </n-form-item-gi>
        </template>
    </n-grid>
</template>
