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
import { fetchAppUpdate, fetchUpload } from '@/service'
import { useEdit } from '@/views/flink/app/composables/useEdit'
import { mapEditDeployFields } from '@/views/flink/app/composables/mapEditDeployFields'
import { deployModes } from '@/views/flink/app/shared/data/index'
import { handleSubmitParams } from '@/views/flink/app/shared/utils'
import ProgramArgsEditor from '@/views/shared/editors/ProgramArgsEditor.vue'
import AppAdvancedFields from './components/AppAdvancedFields.vue'
import AppDeployFields from './components/AppDeployFields.vue'
import PodTemplateTab from './components/PodTemplateTab.vue'
import UploadJobJar from './components/UploadJobJar.vue'
import { DeployMode, ResourceFromEnum } from '@/enums/flinkEnum'

defineOptions({ name: 'EditFlinkApp' })

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const formRef = ref<FormInst | null>(null)
const podTemplateRef = ref<InstanceType<typeof PodTemplateTab> | null>(null)
const submitting = ref(false)
const uploadLoading = ref(false)

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
    deployMode: 0,
    mainClass: '',
    jar: '',
    args: '',
    dynamicProperties: '',
    hadoopUser: '',
    yarnQueue: '',
    restartSize: 0,
    parallelism: null as number | null,
    slot: null as number | null,
    jmOptions: [] as string[],
    tmOptions: [] as string[],
    jmOptionsItem: {} as Recordable,
    tmOptionsItem: {} as Recordable,
    totalOptions: [] as string[],
    totalItem: {} as Recordable,
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
    checkPointFailure: {
        cpMaxFailureInterval: null as number | null,
        cpFailureRateInterval: null as number | null,
        cpFailureAction: null as number | null,
    },
})

const rules: FormRules = {
    jobName: [{ required: true, trigger: 'blur' }],
    jar: [{ required: true, trigger: 'blur' }],
    mainClass: [{ required: true, trigger: 'blur' }],
}

const isK8sApp = computed(() => formModel.value.deployMode === DeployMode.KUBERNETES_APPLICATION)
const showUploadJar = computed(() => app.resourceFrom === ResourceFromEnum.UPLOAD)

async function initForm() {
    if (!route.query.appId) {
        window.$message?.warning(t('flink.app.editStreamPark.appidCheck'))
        router.push('/flink/app')
        return
    }
    await handleGetApplication()
    const resetParams = handleResetApplication()
    formModel.value = {
        jobName: app.jobName ?? '',
        tags: app.tags ?? '',
        description: app.description ?? '',
        deployMode: app.deployMode ?? 0,
        mainClass: app.mainClass ?? '',
        jar: app.jar ?? '',
        args: app.args ?? '',
        dynamicProperties: app.dynamicProperties ?? '',
        hadoopUser: app.hadoopUser ?? '',
        yarnQueue: app.yarnQueue ?? '',
        restartSize: app.restartSize ?? 0,
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
        if (app.k8sPodTemplate)
            podTemplateRef.value?.handleChoicePodTemplate('ptVisual', app.k8sPodTemplate)
        if (app.k8sJmPodTemplate)
            podTemplateRef.value?.handleChoicePodTemplate('jmPtVisual', app.k8sJmPodTemplate)
        if (app.k8sTmPodTemplate)
            podTemplateRef.value?.handleChoicePodTemplate('tmPtVisual', app.k8sTmPodTemplate)
    }, 500)
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

async function handleSubmit() {
    await formRef.value?.validate()
    submitting.value = true
    try {
        const params: Recordable = {
            id: app.id,
            jar: formModel.value.jar,
            mainClass: formModel.value.mainClass,
        }
        handleSubmitParams(
            params,
            {
                ...formModel.value,
                versionId: app.versionId,
            },
            k8sTemplate,
        )

        const result = await fetchAppUpdate(params)
        if (!result.isSuccess) throwApiFailure(result, t('sys.api.apiRequestFailed'))
        if (!result.data) {
            showResultError(result, t('sys.api.apiRequestFailed'))
            return
        }
        window.$message?.success(t('flink.app.editStreamPark.success'))
        router.push('/flink/app')
    } catch (e: any) {
        showCatchError(e, t('sys.api.apiRequestFailed'))
    } finally {
        submitting.value = false
    }
}

onMounted(() => initForm())
</script>

<template>
    <n-card :bordered="false" :title="t('flink.app.operation.edit')">
        <template #header-extra>
            <n-button quaternary @click="router.push('/flink/app')">
                {{ t('common.cancelText') }}
            </n-button>
        </template>
        <n-alert v-if="app.resourceFrom === ResourceFromEnum.UPLOAD" type="info" class="mb-16px">
            Apache Flink JAR application
        </n-alert>
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
                <n-form-item-gi :span="12" :label="t('flink.app.deployMode')">
                    <n-select
                        v-model:value="formModel.deployMode"
                        :options="deployModes.map((d) => ({ label: d.label, value: d.value }))"
                    />
                </n-form-item-gi>
                <n-form-item-gi :span="24">
                    <AppDeployFields v-model="formModel" />
                </n-form-item-gi>
                <n-form-item-gi :span="12" :label="t('flink.app.mainClass')" path="mainClass">
                    <n-input v-model:value="formModel.mainClass" />
                </n-form-item-gi>
                <n-form-item-gi
                    v-if="showUploadJar"
                    :span="24"
                    :label="t('flink.app.uploadJobJar')"
                >
                    <UploadJobJar :custom-request="handleCustomJobRequest" :loading="uploadLoading">
                        <template #uploadInfo>
                            <n-alert v-if="formModel.jar" type="info" class="mt-8px">
                                {{ formModel.jar }}
                            </n-alert>
                        </template>
                    </UploadJobJar>
                </n-form-item-gi>
                <n-form-item-gi v-else :span="12" label="JAR" path="jar">
                    <n-input v-model:value="formModel.jar" />
                </n-form-item-gi>
                <n-form-item-gi :span="24" :label="t('flink.app.programArgs')">
                    <ProgramArgsEditor v-model="formModel.args" />
                </n-form-item-gi>
                <n-form-item-gi v-if="isK8sApp" :span="24" :label="t('flink.app.podTemplate')">
                    <PodTemplateTab
                        ref="podTemplateRef"
                        v-model:pod-template="k8sTemplate.podTemplate"
                        v-model:jm-pod-template="k8sTemplate.jmPodTemplate"
                        v-model:tm-pod-template="k8sTemplate.tmPodTemplate"
                    />
                </n-form-item-gi>
                <n-form-item-gi :span="24">
                    <AppAdvancedFields v-model="formModel" />
                </n-form-item-gi>
            </n-grid>
        </n-form>
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
