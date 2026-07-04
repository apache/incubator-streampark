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
import { fetchSparkAppCreate } from '@/service'
import SparkAppForm from './components/SparkAppForm.vue'
import { JobTypeEnum } from '@/enums/sparkEnum'
import { encryptByBase64 } from '@/utils/cipher'
import { buildUUID } from '@/utils/uuid'
import { createLocalStorage } from '@/utils/cache'

defineOptions({ name: 'SparkAppAdd' })

const { t } = useI18n()
const router = useRouter()
const submitting = ref(false)
const ls = createLocalStorage()

async function handleSubmit(values: Recordable) {
  submitting.value = true
  try {
    const params: Recordable = {}
    for (const k in values) {
      const v = values[k]
      if (v != null)
        params[k] = v
    }
    if (params.config)
      params.config = encryptByBase64(params.config)
    else
      params.config = null

    if (params.jobType === JobTypeEnum.SQL)
      params.sparkSql = values.sparkSql

    params.socketId = buildUUID()
    ls.set('DOWN_SOCKET_ID', params.socketId)

    const result = await fetchSparkAppCreate(params as SparkApplication)
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
</script>

<template>
  <n-card :bordered="false" :title="t('common.add')">
    <template #header-extra>
      <n-button quaternary @click="router.push('/spark/app')">
        {{ t('common.cancelText') }}
      </n-button>
    </template>
    <SparkAppForm mode="create" @submit="handleSubmit">
      <template #footer="{ submit, submitting: formSubmitting }">
        <n-button @click="router.push('/spark/app')">
          {{ t('common.cancelText') }}
        </n-button>
        <n-button type="primary" :loading="submitting || formSubmitting" @click="submit">
          {{ t('common.submitText') }}
        </n-button>
      </template>
    </SparkAppForm>
  </n-card>
</template>
