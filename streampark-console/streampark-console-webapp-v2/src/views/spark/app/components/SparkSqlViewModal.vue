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
import { fetchSparkSql } from '@/service'
import SparkSqlEditor from '@/views/spark/app/components/SparkSqlEditor.vue'
import { decodeByBase64 } from '@/utils/cipher'

const props = defineProps<{
  show: boolean
  recordId?: string | null
  appId?: string | null
  version?: number | string | null
}>()

const emit = defineEmits<{
  'update:show': [value: boolean]
}>()

const { t } = useI18n()
const loading = ref(false)
const sqlContent = ref('')

watch(
  () => [props.show, props.recordId, props.appId] as const,
  async ([show, recordId, appId]) => {
    if (!show || !recordId || !appId) {
      sqlContent.value = ''
      return
    }
    loading.value = true
    try {
      const result = await fetchSparkSql({ id: recordId, appId })
      if (!result.isSuccess)
        throwApiFailure(result, t('sys.api.apiRequestFailed'))
      const raw = result.data?.sql ?? ''
      try {
        sqlContent.value = decodeByBase64(raw)
      }
      catch {
        sqlContent.value = raw
      }
    }
    catch (e: any) {
      showCatchError(e, t('sys.api.apiRequestFailed'))
      sqlContent.value = ''
    }
    finally {
      loading.value = false
    }
  },
)
</script>

<template>
  <n-modal
    :show="show"
    preset="card"
    :style="{ width: '900px' }"
    :title="`${t('spark.app.detail.detailTab.detailTabName.sparkSql')} v${version ?? ''}`"
    @update:show="emit('update:show', $event)"
  >
    <n-spin :show="loading">
      <SparkSqlEditor v-model="sqlContent" readonly height="480px" />
    </n-spin>
  </n-modal>
</template>
