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
import type { SparkApplication } from '@/types/api/spark/app.type'
import { fetchSparkAppGet, fetchSparkAppUpdate } from '@/service'
import SparkAppForm from './components/SparkAppForm.vue'
import { JobTypeEnum } from '@/enums/sparkEnum'
import { decodeByBase64, encryptByBase64 } from '@/utils/cipher'
import { buildUUID } from '@/utils/uuid'
import { createLocalStorage } from '@/utils/cache'

defineOptions({ name: 'SparkAppEdit' })

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const submitting = ref(false)
const initialData = ref<Recordable | null>(null)
const sqlId = ref<number | null>(null)
const ls = createLocalStorage()

function decodeSqlValue(value?: string | null) {
  if (!value)
    return ''
  try {
    return decodeByBase64(value)
  }
  catch {
    return value
  }
}

async function loadApp() {
  const appId = route.query.appId as string
  if (!appId) {
    window.$message?.warning(t('spark.app.appidCheck'))
    router.push('/spark/app')
    return
  }
  const result = await fetchSparkAppGet({ id: appId })
  if (!result.isSuccess || !result.data)
    throwApiFailure(result, t('sys.api.apiRequestFailed'))
  const app = result.data
  sqlId.value = app.sqlId ?? null
  let configOverride = ''
  let isSetConfig = false
  if (app.config?.trim()) {
    configOverride = decodeByBase64(app.config)
    isSetConfig = true
  }
  initialData.value = {
    ...app,
    sparkSql: decodeSqlValue(app.sparkSql),
    isSetConfig,
    configOverride,
  }
}

async function handleSubmit(values: Recordable) {
  submitting.value = true
  try {
    const params: Recordable = { id: route.query.appId }
    for (const k in values) {
      const v = values[k]
      if (v != null)
        params[k] = v
    }
    if (params.config)
      params.config = encryptByBase64(params.config)
    else
      params.config = null

    if (values.jobType === JobTypeEnum.SQL)
      params.sqlId = sqlId.value

    params.socketId = buildUUID()
    ls.set('DOWN_SOCKET_ID', params.socketId)

    const result = await fetchSparkAppUpdate(params as SparkApplication)
    if (!result.isSuccess || !result.data)
      throwApiFailure(result, t('sys.api.apiRequestFailed'))
    window.$message?.success(t('spark.app.success'))
    router.push('/spark/app')
  }
  catch (e: any) {
    showCatchError(e, t('sys.api.apiRequestFailed'))
  }
  finally {
    submitting.value = false
  }
}

onMounted(() => loadApp())
</script>

<template>
  <n-card :bordered="false" :title="t('spark.app.operation.edit')">
    <template #header-extra>
      <n-button quaternary @click="router.push('/spark/app')">
        {{ t('common.cancelText') }}
      </n-button>
    </template>
    <n-spin :show="!initialData">
      <SparkAppForm
        v-if="initialData"
        mode="edit"
        :app-id="String(route.query.appId ?? '')"
        :initial-data="initialData"
        @submit="handleSubmit"
      >
        <template #footer="{ submit, submitting: formSubmitting }">
          <n-button @click="router.push('/spark/app')">
            {{ t('common.cancelText') }}
          </n-button>
          <n-button type="primary" :loading="submitting || formSubmitting" @click="submit">
            {{ t('common.submitText') }}
          </n-button>
        </template>
      </SparkAppForm>
    </n-spin>
  </n-card>
</template>
