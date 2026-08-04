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
import type { FormInst, FormRules, SelectOption } from 'naive-ui'
import type { ProjectRecord } from '@/types/api/resource/project/model/projectModel'
import {
    fetchProjectBranches,
    fetchProjectDetail,
    fetchProjectExists,
    fetchProjectGitCheck,
    fetchProjectUpdate,
} from '@/service'
import { BuildStateEnum } from '@/enums/flinkEnum'
import { CVSTypeEnum, ProjectTypeEnum } from '@/enums/projectEnum'

defineOptions({ name: 'EditProject' })

const { t } = useI18n()
const router = useRouter()
const route = useRoute()

const projectId = computed(() => route.query.id as string | undefined)
const projectResource = reactive<Partial<ProjectRecord>>({})

const formRef = ref<FormInst | null>(null)
const submitting = ref(false)
const pageLoading = ref(false)

const formModel = ref({
    name: '',
    type: ProjectTypeEnum.FLINK,
    repository: CVSTypeEnum.GIT,
    url: '',
    refs: null as string | null,
    userName: '',
    password: '',
    prvkeyPath: '',
    pom: '',
    buildArgs: '',
    description: '',
})

const branchOptions = ref<SelectOption[]>([])

const isGitSsh = computed(() => /^git@(.*)/.test(formModel.value.url || ''))
const isGitHttps = computed(() => /^http(s)?:\/\//.test(formModel.value.url || ''))

const projectTypeOptions = [
    { label: 'Apache Flink', value: ProjectTypeEnum.FLINK },
    { label: 'Apache Spark', value: ProjectTypeEnum.SPARK, disabled: true },
]

const repositoryOptions = [{ label: 'GitHub/GitLab', value: CVSTypeEnum.GIT }]

const rules = computed<FormRules>(() => ({
    name: [
        {
            required: true,
            validator: async (_rule, value) => {
                if (!value)
                    return Promise.reject(
                        t('flink.project.operationTips.projectNameIsRequiredMessage'),
                    )
                if (!projectResource.name || value !== projectResource.name) {
                    const result = await fetchProjectExists({ name: value })
                    if (result.isSuccess && result.data)
                        return Promise.reject(
                            t('flink.project.operationTips.projectNameIsUniqueMessage'),
                        )
                }
                return Promise.resolve()
            },
            trigger: 'blur',
        },
    ],
    type: [
        {
            required: true,
            type: 'number',
            message: t('flink.project.operationTips.projectTypeIsRequiredMessage'),
            trigger: 'change',
        },
    ],
    repository: [
        {
            required: true,
            type: 'number',
            message: t('flink.project.operationTips.cvsIsRequiredMessage'),
            trigger: 'change',
        },
    ],
    url: [
        {
            required: true,
            validator: (_rule, value) => {
                if (!value) return Promise.reject(t('flink.project.form.repositoryURLRequired'))
                if (/^git@(.*)/.test(value) || /^http(s)?:\/\//.test(value))
                    return Promise.resolve()
                return Promise.reject(t('flink.project.form.credentialError'))
            },
            trigger: 'blur',
        },
    ],
    refs: [
        {
            required: true,
            message: t('flink.project.form.branchesPlaceholder'),
            trigger: 'change',
        },
    ],
}))

async function loadBranches() {
    const { url, userName, password, prvkeyPath } = formModel.value
    if (!url) return

    const userNull = !userName
    const passNull = !password
    if (!((userNull && passNull) || (!userNull && !passNull))) return

    const loadingMsg = window.$message?.loading(t('flink.project.gettingBranch'), { duration: 0 })
    try {
        const result = await fetchProjectBranches({
            url,
            userName: userName || null,
            password: password || null,
            prvkeyPath: prvkeyPath || null,
        })
        if (!result.isSuccess) throwApiFailure(result, t('sys.api.apiRequestFailed'))

        const resp = result.data as { branches?: string[]; tags?: string[] } | string[] | undefined
        const branches = Array.isArray(resp) ? resp : (resp?.branches ?? [])
        const tags = Array.isArray(resp) ? [] : (resp?.tags ?? [])

        branchOptions.value = [
            {
                type: 'group',
                label: t('flink.project.form.branches'),
                key: 'branches',
                children: branches.map((c) => ({ label: c, value: `refs/heads/${c}` })),
            },
            {
                type: 'group',
                label: t('flink.project.form.tags'),
                key: 'tags',
                children: tags.map((c) => ({ label: c, value: `refs/tags/${c}` })),
            },
        ]
    } catch (e: any) {
        showCatchError(e, t('sys.api.apiRequestFailed'))
    } finally {
        loadingMsg?.destroy()
    }
}

async function loadProject() {
    if (!projectId.value) {
        router.push('/resource/project')
        return
    }
    pageLoading.value = true
    try {
        const result = await fetchProjectDetail({ id: projectId.value })
        if (!result.isSuccess || !result.data)
            throwApiFailure(result, t('sys.api.apiRequestFailed'))

        Object.assign(projectResource, result.data)
        const res = result.data as ProjectRecord
        formModel.value = {
            name: res.name,
            type: res.type,
            repository: res.repository,
            url: res.url,
            userName: res.userName ?? '',
            password: res.password ?? '',
            prvkeyPath: res.prvkeyPath ?? '',
            refs: res.refs ?? '',
            pom: res.pom ?? '',
            buildArgs: res.buildArgs ?? '',
            description: res.description ?? '',
        }
    } catch (e: any) {
        showCatchError(e, t('sys.api.apiRequestFailed'))
        router.push('/resource/project')
    } finally {
        pageLoading.value = false
    }
}

function resolveBuildState(values: typeof formModel.value) {
    if (
        projectResource.url !== values.url ||
        projectResource.refs !== values.refs ||
        projectResource.pom !== values.pom
    ) {
        return BuildStateEnum.NEED_REBUILD
    }
    return projectResource.buildState
}

function buildPayload(buildState?: number | string | null) {
    const values = formModel.value
    return {
        id: projectId.value,
        name: values.name,
        url: values.url,
        repository: values.repository,
        type: values.type,
        refs: values.refs,
        userName: values.userName || null,
        password: values.password || null,
        prvkeyPath: values.prvkeyPath || null,
        pom: values.pom,
        buildArgs: values.buildArgs,
        description: values.description,
        buildState,
    }
}

async function handleSubmit() {
    await formRef.value?.validate()
    submitting.value = true
    try {
        const values = formModel.value
        const checkResult = await fetchProjectGitCheck({
            url: values.url,
            userName: values.userName || null,
            password: values.password || null,
            prvkeyPath: values.prvkeyPath || null,
        })
        if (!checkResult.isSuccess) throwApiFailure(checkResult, t('sys.api.apiRequestFailed'))

        const checkCode = Number(checkResult.data ?? -1)
        if (checkCode !== 0) {
            window.$message?.error(
                checkCode === 1
                    ? t('flink.project.operationTips.notAuthorizedMessage')
                    : t('flink.project.operationTips.authenticationErrorMessage'),
            )
            return
        }

        if (!branchOptions.value.length) await loadBranches()

        const buildState = resolveBuildState(values)
        const result = await fetchProjectUpdate(buildPayload(buildState))
        if (!result.isSuccess || !result.data)
            throwApiFailure(result, t('flink.project.saveFailed'))

        window.$message?.success(t('flink.project.updateSuccess'))
        router.push('/resource/project')
    } catch (e: any) {
        showCatchError(e, t('flink.project.saveFailed'))
    } finally {
        submitting.value = false
    }
}

function handleCancel() {
    router.push('/resource/project')
}

onMounted(loadProject)
</script>

<template>
    <n-card :bordered="false" class="h-full">
        <n-spin :show="pageLoading">
            <n-form ref="formRef" :model="formModel" :rules="rules" label-placement="top">
                <n-form-item :label="t('flink.project.form.projectName')" path="name">
                    <n-input
                        v-model:value="formModel.name"
                        :placeholder="t('flink.project.form.projectNamePlaceholder')"
                        clearable
                    />
                </n-form-item>
                <n-form-item :label="t('flink.project.form.projectType')" path="type">
                    <n-select
                        v-model:value="formModel.type"
                        :options="projectTypeOptions"
                        :placeholder="t('flink.project.form.projectTypePlaceholder')"
                    />
                </n-form-item>
                <n-form-item :label="t('flink.project.form.cvs')" path="repository">
                    <n-select
                        v-model:value="formModel.repository"
                        :options="repositoryOptions"
                        :placeholder="t('flink.project.form.cvsPlaceholder')"
                    />
                </n-form-item>
                <n-form-item :label="t('flink.project.form.repositoryURL')" path="url">
                    <n-input
                        v-model:value="formModel.url"
                        :placeholder="t('flink.project.form.repositoryURLPlaceholder')"
                        clearable
                    />
                    <n-text v-if="isGitHttps" depth="3" class="mt-4px text-12px">
                        {{ t('flink.project.operationTips.httpsCredential') }}
                    </n-text>
                    <n-text v-if="isGitSsh" depth="3" class="mt-4px text-12px">
                        {{ t('flink.project.operationTips.sshCredential') }}
                    </n-text>
                </n-form-item>
                <n-form-item :label="t('flink.project.form.branches')" path="refs">
                    <n-select
                        v-model:value="formModel.refs"
                        :options="branchOptions"
                        filterable
                        :placeholder="t('flink.project.form.branchesPlaceholder')"
                        @focus="loadBranches"
                    />
                </n-form-item>
                <n-form-item v-if="isGitSsh" :label="t('flink.project.form.prvkeyPath')">
                    <n-input
                        v-model:value="formModel.prvkeyPath"
                        :placeholder="t('flink.project.form.prvkeyPathPlaceholder')"
                        clearable
                    />
                </n-form-item>
                <n-form-item v-if="isGitHttps" :label="t('flink.project.form.userName')">
                    <n-input
                        v-model:value="formModel.userName"
                        :placeholder="t('flink.project.form.userNamePlaceholder')"
                        autocomplete="new-password"
                        clearable
                    />
                </n-form-item>
                <n-form-item :label="t('flink.project.form.password')">
                    <n-input
                        v-model:value="formModel.password"
                        type="password"
                        show-password-on="click"
                        :placeholder="t('flink.project.form.passwordPlaceholder')"
                        autocomplete="new-password"
                        clearable
                    />
                </n-form-item>
                <n-form-item :label="t('flink.project.form.pom')">
                    <n-input
                        v-model:value="formModel.pom"
                        :placeholder="t('flink.project.form.pomPlaceholder')"
                        clearable
                    />
                </n-form-item>
                <n-form-item :label="t('flink.project.form.buildArgs')">
                    <n-input
                        v-model:value="formModel.buildArgs"
                        type="textarea"
                        :rows="2"
                        :placeholder="t('flink.project.form.buildArgsPlaceholder')"
                        clearable
                    />
                </n-form-item>
                <n-form-item :label="t('flink.project.form.description')">
                    <n-input
                        v-model:value="formModel.description"
                        type="textarea"
                        :rows="4"
                        :placeholder="t('flink.project.form.descriptionPlaceholder')"
                        clearable
                    />
                </n-form-item>
            </n-form>
            <div class="mt-24px flex justify-center gap-12px">
                <n-button @click="handleCancel">
                    {{ t('common.cancelText') }}
                </n-button>
                <n-button
                    id="e2e-project-submit-btn"
                    type="primary"
                    :loading="submitting"
                    @click="handleSubmit"
                >
                    {{ t('common.submitText') }}
                </n-button>
            </div>
        </n-spin>
    </n-card>
</template>
