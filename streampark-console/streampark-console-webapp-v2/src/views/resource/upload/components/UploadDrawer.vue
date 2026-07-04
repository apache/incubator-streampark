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
import type { FormInst, FormRules, UploadCustomRequestOptions } from 'naive-ui'
import type { ResourceListRecord } from '@/types/api/resource/upload/model/resourceModel'
import {
  checkResource,
  fetchAddResource,
  fetchUpdateResource,
  fetchUpload,
} from '@/service'
import { EngineTypeEnum, ResourceTypeEnum } from '@/views/resource/upload/shared/constants'

const props = defineProps<{
  show: boolean
  isUpdate: boolean
  record: ResourceListRecord | null
  teamResource: ResourceListRecord[]
}>()

const emit = defineEmits<{
  'update:show': [value: boolean]
  success: [updated: boolean]
}>()

const { t } = useI18n()
const formRef = ref<FormInst | null>(null)
const submitting = ref(false)
const uploadJars = ref<string[]>([])
const resourceId = ref<string | null>(null)

const formModel = ref({
  engineType: EngineTypeEnum.FLINK,
  resourceType: ResourceTypeEnum.JAR_LIBRARY as string,
  resourceName: '',
  mainClass: '',
  description: '',
  resourceGroup: [] as string[],
})

const engineOptions = [
  { label: 'Apache Flink', value: EngineTypeEnum.FLINK },
  { label: 'Apache Spark', value: EngineTypeEnum.SPARK },
]

const resourceTypeOptions = computed(() => {
  const isFlink = formModel.value.engineType === EngineTypeEnum.FLINK
  return [
    { label: isFlink ? 'Flink App' : 'Spark App', value: ResourceTypeEnum.APP },
    { label: 'Connector', value: ResourceTypeEnum.CONNECTOR },
    { label: 'UDXF', value: ResourceTypeEnum.UDXF },
    { label: 'Jar Library', value: ResourceTypeEnum.JAR_LIBRARY },
    { label: 'Group', value: ResourceTypeEnum.GROUP },
  ]
})

const groupOptions = computed(() =>
  props.teamResource
    .filter(item => item.resourceType !== ResourceTypeEnum.GROUP)
    .map(item => ({ label: item.resourceName, value: item.id })),
)

const showResourceName = computed(() =>
  formModel.value.resourceType !== ResourceTypeEnum.CONNECTOR
  && formModel.value.resourceType !== ResourceTypeEnum.GROUP,
)

const showGroupName = computed(() => formModel.value.resourceType === ResourceTypeEnum.GROUP)

const showJarUpload = computed(() => formModel.value.resourceType !== ResourceTypeEnum.GROUP)

const showMainClass = computed(() => formModel.value.resourceType === ResourceTypeEnum.APP)

const rules = computed<FormRules>(() => {
  const base: FormRules = {
    engineType: [{ required: true, message: t('flink.resource.form.engineTypeIsRequiredMessage'), trigger: 'change' }],
    resourceType: [{ required: true, message: t('flink.resource.form.resourceTypeIsRequiredMessage'), trigger: 'change' }],
    description: [{ max: 100, message: t('flink.resource.form.descriptionMessage'), trigger: 'blur' }],
  }
  if (showResourceName.value) {
    base.resourceName = [{ required: true, message: t('flink.resource.form.resourceNameIsRequiredMessage'), trigger: 'blur' }]
  }
  if (showGroupName.value) {
    base.resourceName = [{ required: true, message: t('flink.resource.groupNameIsRequiredMessage'), trigger: 'blur' }]
  }
  return base
})

const drawerTitle = computed(() =>
  props.isUpdate ? t('flink.resource.modifyResource') : t('flink.resource.addResource'),
)

function resetForm() {
  formModel.value = {
    engineType: EngineTypeEnum.FLINK,
    resourceType: ResourceTypeEnum.JAR_LIBRARY,
    resourceName: '',
    mainClass: '',
    description: '',
    resourceGroup: [],
  }
  uploadJars.value = []
  resourceId.value = null
}

watch(
  () => props.show,
  (show) => {
    if (!show)
      return
    resetForm()
    if (props.isUpdate && props.record) {
      resourceId.value = props.record.id
      formModel.value = {
        engineType: props.record.engineType as EngineTypeEnum,
        resourceType: props.record.resourceType,
        resourceName: props.record.resourceName,
        mainClass: props.record.mainClass ?? '',
        description: props.record.description ?? '',
        resourceGroup: [],
      }
      if (props.record.resourceType === ResourceTypeEnum.GROUP) {
        try {
          formModel.value.resourceGroup = JSON.parse(props.record.resource || '[]')
        }
        catch {
          formModel.value.resourceGroup = []
        }
      }
      else {
        try {
          const parsed = JSON.parse(props.record.resource || '{}')
          uploadJars.value = parsed.jar ?? []
        }
        catch {
          uploadJars.value = []
        }
      }
    }
    nextTick(() => formRef.value?.restoreValidation())
  },
)

function closeDrawer() {
  emit('update:show', false)
}

async function handleJarUpload(options: UploadCustomRequestOptions) {
  const file = options.file.file as File
  if (!file)
    return
  if (file.type !== 'application/java-archive' && !/\.(jar|JAR|py)$/i.test(file.name)) {
    window.$message?.error(t('flink.resource.jarFileErrorTip'))
    options.onError()
    return
  }
  try {
    const formData = new FormData()
    formData.append('file', file)
    const result = await fetchUpload(formData)
    if (!result.isSuccess)
      throwApiFailure(result, t('sys.api.apiRequestFailed'))
    const path = result.data?.path
    if (!path)
      throw new Error(t('flink.resource.jarFileErrorTip'))
    uploadJars.value = [`${file.name}:${path}`]
    if (formModel.value.resourceType === ResourceTypeEnum.APP && result.data?.mainClass)
      formModel.value.mainClass = result.data.mainClass
    options.onFinish()
  }
  catch (e: any) {
    window.$message?.error(e?.message || t('flink.resource.jarFileErrorTip'))
    options.onError()
  }
}

function handleCheckState(state: number, resp: Recordable, resourceJson: string) {
  switch (state) {
    case 1:
      window.$message?.error(resp.exception || t('flink.resource.jarFileErrorTip'))
      break
    case 2:
      if (formModel.value.resourceType === ResourceTypeEnum.APP)
        window.$message?.error(t('flink.resource.mainNullTip'))
      if (formModel.value.resourceType === ResourceTypeEnum.CONNECTOR)
        window.$message?.error(t('flink.resource.connectorInvalidTip'))
      break
    case 3:
      window.$message?.error(`${t('flink.resource.connectorInfoErrorTip')}: ${resp.name ?? ''}`)
      break
    case 4:
      window.$message?.error(t('flink.resource.connectorExistsTip'))
      break
    case 5:
      window.$message?.error(t('flink.resource.connectorModifyTip'))
      break
    case 0:
      return submitResource(resourceJson, resp.connector ?? null)
    default:
      break
  }
  return false
}

async function submitResource(resourceJson: string, connector: string | null) {
  submitting.value = true
  try {
    const payload = {
      ...formModel.value,
      resource: resourceJson,
      connector,
      id: resourceId.value ?? undefined,
    }
    const result = props.isUpdate
      ? await fetchUpdateResource(payload as any)
      : await fetchAddResource(payload as any)
    if (!result.isSuccess)
      throwApiFailure(result, t('sys.api.apiRequestFailed'))
    closeDrawer()
    emit('success', props.isUpdate)
    return true
  }
  catch (e: any) {
    showCatchError(e, t('sys.api.apiRequestFailed'))
    return false
  }
  finally {
    submitting.value = false
  }
}

async function handleSubmit() {
  await formRef.value?.validate()
  const values = { ...formModel.value }

  if (values.resourceType === ResourceTypeEnum.GROUP) {
    const resourceJson = JSON.stringify(values.resourceGroup)
    await submitResource(resourceJson, null)
    return
  }

  if (!uploadJars.value.length) {
    window.$message?.error(t('flink.resource.addResourceTip'))
    return
  }

  const resourceJson = JSON.stringify({ jar: uploadJars.value })
  const checkResult = await checkResource({
    id: resourceId.value ?? undefined,
    resource: resourceJson,
    ...values,
  } as any)

  if (!checkResult.isSuccess) {
    showResultError(checkResult, t('sys.api.apiRequestFailed'))
    return
  }

  const resp = (checkResult.data ?? checkResult) as Recordable
  const state = Number(resp.state ?? resp.status)
  await handleCheckState(state, resp, resourceJson)
}
</script>

<template>
  <n-drawer :show="show" :width="700" @update:show="emit('update:show', $event)">
    <n-drawer-content :title="drawerTitle" closable>
      <n-form ref="formRef" :model="formModel" :rules="rules" label-placement="top">
        <n-form-item :label="t('flink.resource.engineType')" path="engineType">
          <n-select v-model:value="formModel.engineType" :options="engineOptions" />
        </n-form-item>
        <n-form-item :label="t('flink.resource.resourceType')" path="resourceType">
          <n-select v-model:value="formModel.resourceType" :options="resourceTypeOptions" />
        </n-form-item>
        <n-form-item
          v-if="showResourceName"
          :label="t('flink.resource.resourceName')"
          path="resourceName"
        >
          <n-input v-model:value="formModel.resourceName" :placeholder="t('flink.resource.resourceNamePlaceholder')" />
        </n-form-item>
        <n-form-item
          v-if="showGroupName"
          :label="t('flink.resource.groupName')"
          path="resourceName"
        >
          <n-input v-model:value="formModel.resourceName" :placeholder="t('flink.resource.groupNamePlaceholder')" />
        </n-form-item>
        <n-form-item
          v-if="showGroupName"
          :label="t('flink.resource.resourceGroup')"
          path="resourceGroup"
        >
          <n-select
            v-model:value="formModel.resourceGroup"
            multiple
            :options="groupOptions"
            :placeholder="t('flink.resource.addResource')"
          />
        </n-form-item>
        <n-form-item v-if="showJarUpload" :label="t('flink.resource.addResource')">
          <n-upload
            :max="1"
            accept=".jar,.JAR,.py"
            :custom-request="handleJarUpload"
            :default-file-list="[]"
          >
            <n-upload-dragger>
              <div class="py-16px text-center">
                {{ t('flink.resource.addResourceTip') }}
              </div>
            </n-upload-dragger>
          </n-upload>
          <n-space v-if="uploadJars.length" class="mt-8px">
            <n-tag v-for="jar in uploadJars" :key="jar" type="info">
              {{ jar.split(':')[0] }}
            </n-tag>
          </n-space>
        </n-form-item>
        <n-form-item v-if="showMainClass" :label="t('flink.app.mainClass')" path="mainClass">
          <n-input v-model:value="formModel.mainClass" :placeholder="t('flink.app.addAppTips.mainClassPlaceholder')" />
        </n-form-item>
        <n-form-item :label="t('common.description')" path="description">
          <n-input v-model:value="formModel.description" type="textarea" :rows="4" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button class="e2e-upload-cancel-btn" @click="closeDrawer">
            {{ t('common.cancelText') }}
          </n-button>
          <n-button class="e2e-upload-submit-btn" type="primary" :loading="submitting" @click="handleSubmit">
            {{ t('common.submitText') }}
          </n-button>
        </n-space>
      </template>
    </n-drawer-content>
  </n-drawer>
</template>
