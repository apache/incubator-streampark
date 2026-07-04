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
import type { FormInst, FormItemRule, FormRules } from 'naive-ui'
import type { SparkEnv } from '@/types/api/spark/home.type'
import {
  fetchCheckSparkName,
  fetchSparkEnvList,
  fetchTeamResource,
  fetchVariableAll,
  fetchYarnQueueList,
} from '@/service'
import { resolveListData } from '@/views/system/shared/utils'
import { deployModes } from '@/views/spark/app/constants'
import MergelyDrawer from '@/views/shared/editors/MergelyDrawer.vue'
import ProgramArgsEditor from '@/views/shared/editors/ProgramArgsEditor.vue'
import VariableReviewDrawer from '@/views/shared/editors/VariableReviewDrawer.vue'
import SparkSqlEditor from './SparkSqlEditor.vue'
import { AppTypeEnum, ResourceFromEnum } from '@/enums/flinkEnum'
import { AppExistsStateEnum, DeployMode, JobTypeEnum } from '@/enums/sparkEnum'

const props = defineProps<{
  mode: 'create' | 'edit'
  appId?: string | null
  initialData?: Recordable | null
}>()

const emit = defineEmits<{
  submit: [values: Recordable]
}>()

const { t } = useI18n()
const formRef = ref<FormInst | null>(null)
const sqlEditorRef = ref<InstanceType<typeof SparkSqlEditor> | null>(null)
const submitting = ref(false)

const sparkEnvs = ref<SparkEnv[]>([])
const teamResources = ref<Array<{ id: string, resource: string, resourceName?: string }>>([])
const yarnQueues = ref<Array<{ queueLabel: string }>>([])
const variableSuggestions = ref<Array<{ text: string, description: string, value: string }>>([])
const configDrawerVisible = ref(false)
const argsPreviewVisible = ref(false)

const formModel = ref({
  jobType: JobTypeEnum.SQL,
  deployMode: DeployMode.YARN_CLUSTER,
  versionId: null as string | null,
  sparkSql: '',
  jar: null as string | null,
  mainClass: '',
  appName: '',
  args: '',
  appProperties: '',
  isSetConfig: false,
  configOverride: '',
  tags: '',
  hadoopUser: '',
  yarnQueue: null as string | null,
  description: '',
})

const isSqlJob = computed(() => formModel.value.jobType === JobTypeEnum.SQL)
const isJarJob = computed(() => formModel.value.jobType === JobTypeEnum.JAR)
const showYarnFields = computed(() =>
  [DeployMode.YARN_CLIENT, DeployMode.YARN_CLUSTER].includes(formModel.value.deployMode),
)

const jobTypeOptions = [
  { label: 'SQL', value: JobTypeEnum.SQL },
  { label: 'JAR', value: JobTypeEnum.JAR },
  { label: 'PySpark', value: JobTypeEnum.PYSPARK },
]

const jarOptions = computed(() =>
  teamResources.value.map(r => ({
    label: r.resourceName || r.resource,
    value: r.resource,
  })),
)

const yarnQueueOptions = computed(() =>
  yarnQueues.value.map(q => ({ label: q.queueLabel, value: q.queueLabel })),
)

async function validateAppName(_rule: FormItemRule, value: string) {
  if (!value?.trim())
    return Promise.reject(t('spark.app.addAppTips.appNameIsRequiredMessage'))
  const params: { appName: string, id?: string } = { appName: value }
  if (props.appId)
    params.id = props.appId
  const result = await fetchCheckSparkName(params)
  if (!result.isSuccess)
    return Promise.reject(t('spark.app.addAppTips.appNameValid'))
  switch (Number(result.data)) {
    case AppExistsStateEnum.NO:
      return Promise.resolve()
    case AppExistsStateEnum.IN_DB:
      return Promise.reject(t('spark.app.addAppTips.appNameNotUniqueMessage'))
    case AppExistsStateEnum.IN_YARN:
      return Promise.reject(t('spark.app.addAppTips.appNameExistsInYarnMessage'))
    case AppExistsStateEnum.IN_KUBERNETES:
      return Promise.reject(t('spark.app.addAppTips.appNameExistsInK8sMessage'))
    default:
      return Promise.reject(t('spark.app.addAppTips.appNameValid'))
  }
}

const rules: FormRules = {
  deployMode: [{ required: true, type: 'number', trigger: 'change' }],
  versionId: [{ required: true, message: t('spark.app.addAppTips.sparkVersionIsRequiredMessage'), trigger: 'change' }],
  appName: [{ required: true, validator: validateAppName, trigger: 'blur' }],
  jar: [{ required: true, message: t('spark.app.addAppTips.sparkAppRequire'), trigger: 'change' }],
  mainClass: [{ required: true, message: t('spark.app.addAppTips.mainClassIsRequiredMessage'), trigger: 'blur' }],
}

async function loadOptions() {
  const [envResult, resourceResult, queueResult] = await Promise.all([
    fetchSparkEnvList(),
    fetchTeamResource({}),
    fetchYarnQueueList({ pageNum: 1, pageSize: 9999 }),
  ])
  if (envResult.isSuccess && envResult.data)
    sparkEnvs.value = envResult.data
  if (resourceResult.isSuccess && resourceResult.data)
    teamResources.value = resourceResult.data as typeof teamResources.value
  if (queueResult.isSuccess)
    yarnQueues.value = resolveListData(queueResult.data).records

  const varResult = await fetchVariableAll()
  if (varResult.isSuccess && Array.isArray(varResult.data)) {
    variableSuggestions.value = varResult.data.map(v => ({
      text: v.variableCode,
      description: v.description ?? v.variableCode,
      value: v.variableValue ?? '',
    }))
  }

  if (props.mode === 'create') {
    const defaultEnv = sparkEnvs.value.find(v => v.isDefault)
    if (defaultEnv)
      formModel.value.versionId = defaultEnv.id
  }
}

function applyInitialData(data: Recordable) {
  formModel.value = {
    jobType: data.jobType ?? JobTypeEnum.SQL,
    deployMode: data.deployMode ?? DeployMode.YARN_CLUSTER,
    versionId: data.versionId ?? null,
    sparkSql: data.sparkSql ?? '',
    jar: data.jar ?? null,
    mainClass: data.mainClass ?? '',
    appName: data.appName ?? '',
    args: data.appArgs ?? data.args ?? '',
    appProperties: data.appProperties ?? '',
    isSetConfig: Boolean(data.isSetConfig),
    configOverride: data.configOverride ?? '',
    tags: data.tags ?? '',
    hadoopUser: data.hadoopUser ?? '',
    yarnQueue: data.yarnQueue ?? null,
    description: data.description ?? '',
  }
}

async function handleSubmit() {
  await formRef.value?.validate()
  if (isSqlJob.value) {
    if (!formModel.value.sparkSql?.trim()) {
      window.$message?.warning(t('spark.app.addAppTips.sparkSqlIsRequiredMessage'))
      return
    }
    const verified = await sqlEditorRef.value?.handleVerifySql()
    if (!verified) {
      window.$message?.warning(t('spark.app.addAppTips.sqlCheck'))
      return
    }
  }
  submitting.value = true
  try {
    const values = { ...formModel.value }
    let config: string | null = null
    if (values.isSetConfig && values.configOverride?.trim())
      config = values.configOverride
    else
      config = null

    const base = {
      deployMode: values.deployMode,
      appType: AppTypeEnum.APACHE_SPARK,
      versionId: values.versionId,
      appName: values.appName,
      tags: values.tags,
      yarnQueue: values.yarnQueue,
      config,
      appProperties: values.appProperties || null,
      hadoopUser: values.hadoopUser || null,
      description: values.description,
    }

    if (isSqlJob.value) {
      emit('submit', {
        ...base,
        jobType: JobTypeEnum.SQL,
        sparkSql: values.sparkSql,
        resourceFrom: ResourceFromEnum.UPLOAD,
        appArgs: values.args || null,
        jar: null,
        mainClass: null,
      })
    }
    else {
      emit('submit', {
        ...base,
        jobType: values.jobType,
        sparkSql: null,
        resourceFrom: ResourceFromEnum.UPLOAD,
        jar: values.jar,
        mainClass: values.mainClass,
        appArgs: values.args || null,
      })
    }
  }
  finally {
    submitting.value = false
  }
}

watch(
  () => props.initialData,
  (data) => {
    if (data)
      applyInitialData(data)
  },
  { immediate: true },
)

onMounted(() => loadOptions())

defineExpose({ handleSubmit, submitting })

function handleConfigOk(payload: { isSetConfig: boolean, configOverride: string | null }) {
  formModel.value.isSetConfig = payload.isSetConfig
  formModel.value.configOverride = payload.configOverride ?? ''
}
</script>

<template>
  <n-form ref="formRef" :model="formModel" :rules="rules" label-placement="left" label-width="140">
    <n-grid :cols="24" :x-gap="16">
      <n-form-item-gi v-if="mode === 'create'" :span="12" :label="t('spark.app.jobType')">
        <n-select
          v-model:value="formModel.jobType"
          :options="jobTypeOptions"
        />
      </n-form-item-gi>
      <n-form-item-gi v-else :span="12" :label="t('spark.app.jobType')">
        <n-tag type="info">
          {{ jobTypeOptions.find(o => o.value === formModel.jobType)?.label }}
        </n-tag>
      </n-form-item-gi>
      <n-form-item-gi :span="12" :label="t('spark.app.deployMode')" path="deployMode">
        <n-select
          v-model:value="formModel.deployMode"
          :options="deployModes"
        />
      </n-form-item-gi>
      <n-form-item-gi :span="12" :label="t('spark.app.sparkVersion')" path="versionId">
        <n-select
          v-model:value="formModel.versionId"
          :options="sparkEnvs.map(e => ({ label: e.sparkName, value: e.id }))"
          filterable
        />
      </n-form-item-gi>
      <n-form-item-gi :span="12" :label="t('spark.app.appName')" path="appName">
        <n-input v-model:value="formModel.appName" :placeholder="t('spark.app.addAppTips.appNamePlaceholder')" />
      </n-form-item-gi>
      <n-form-item-gi v-if="isSqlJob" :span="24" :label="t('spark.app.detail.detailTab.detailTabName.sparkSql')">
        <SparkSqlEditor
          ref="sqlEditorRef"
          v-model="formModel.sparkSql"
          :version-id="formModel.versionId"
          :app-id="appId"
        />
      </n-form-item-gi>
      <template v-if="isJarJob">
        <n-form-item-gi :span="12" :label="t('spark.app.resource')" path="jar">
          <n-select
            v-model:value="formModel.jar"
            :options="jarOptions"
            filterable
          />
        </n-form-item-gi>
        <n-form-item-gi :span="12" :label="t('spark.app.mainClass')" path="mainClass">
          <n-input v-model:value="formModel.mainClass" />
        </n-form-item-gi>
      </template>
      <n-form-item-gi
        v-if="!isSqlJob"
        :span="24"
        :label="t('spark.app.programArgs')"
      >
        <ProgramArgsEditor
          v-model="formModel.args"
          :suggestions="variableSuggestions"
          @preview="argsPreviewVisible = true"
        />
      </n-form-item-gi>
      <n-form-item-gi :span="24" :label="t('spark.app.dynamicProperties')">
        <n-input
          v-model:value="formModel.appProperties"
          type="textarea"
          :rows="3"
          placeholder="--conf, -c PROP=VALUE"
        />
      </n-form-item-gi>
      <n-form-item-gi :span="24" :label="t('spark.app.appConf')">
        <n-space vertical class="w-full">
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
        </n-space>
      </n-form-item-gi>
      <n-form-item-gi :span="12" :label="t('spark.app.tags')">
        <n-input v-model:value="formModel.tags" />
      </n-form-item-gi>
      <template v-if="showYarnFields">
        <n-form-item-gi :span="12" :label="t('spark.app.hadoopUser')">
          <n-input v-model:value="formModel.hadoopUser" />
        </n-form-item-gi>
        <n-form-item-gi :span="12" :label="t('spark.app.yarnQueue')">
          <n-select
            v-model:value="formModel.yarnQueue"
            :options="yarnQueueOptions"
            filterable
            clearable
          />
        </n-form-item-gi>
      </template>
      <n-form-item-gi :span="24" :label="t('common.description')">
        <n-input v-model:value="formModel.description" type="textarea" :rows="3" />
      </n-form-item-gi>
    </n-grid>
  </n-form>
  <div class="mt-24px flex justify-center gap-12px">
    <slot name="footer" :submitting="submitting" :submit="handleSubmit" />
  </div>
  <MergelyDrawer
    v-model:show="configDrawerVisible"
    :original-value="formModel.configOverride"
    @ok="handleConfigOk"
  />
  <VariableReviewDrawer
    v-model:show="argsPreviewVisible"
    :value="formModel.args"
    :suggestions="variableSuggestions"
    :title="t('spark.app.programArgs')"
  />
</template>
