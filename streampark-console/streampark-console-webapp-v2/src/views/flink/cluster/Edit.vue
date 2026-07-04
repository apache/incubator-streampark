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
import { fetchCheckCluster, fetchGetCluster, fetchUpdateCluster } from '@/service'
import ClusterFormFields from './components/ClusterFormFields.vue'
import { useClusterForm } from './composables/useClusterForm'

defineOptions({ name: 'EditCluster' })

const { t } = useI18n()
const router = useRouter()
const route = useRoute()

const clusterId = computed(() => route.query.clusterId as string | undefined)

const {
  submitting,
  formModel,
  historyRecord,
  deployModeOptions,
  resolveOrder,
  k8sRestExposedType,
  isSessionMode,
  isStandalone,
  isYarnSession,
  isK8sSession,
  showAlert,
  flinkEnvOptions,
  alertOptions,
  totalMemoryOptions,
  jmMemoryOptions,
  tmMemoryOptions,
  rules,
  handleSubmitParams,
  applyClusterRecord,
  loadReferenceData,
} = useClusterForm()

const formFieldsRef = ref<InstanceType<typeof ClusterFormFields> | null>(null)
const pageLoading = ref(false)

async function loadCluster() {
  if (!clusterId.value) {
    router.push('/flink/cluster')
    return
  }
  pageLoading.value = true
  try {
    const result = await fetchGetCluster({ id: clusterId.value })
    if (!result.isSuccess || !result.data)
      throwApiFailure(result, t('sys.api.apiRequestFailed'))
    applyClusterRecord(result.data)
    await nextTick()
    formFieldsRef.value?.restoreValidation()
  }
  catch (e: any) {
    showCatchError(e, t('sys.api.apiRequestFailed'))
    router.push('/flink/cluster')
  }
  finally {
    pageLoading.value = false
  }
}

async function handleSubmit() {
  await formFieldsRef.value?.validate()
  submitting.value = true
  try {
    const params = handleSubmitParams(formModel.value)
    if (!Object.keys(params).length)
      return

    Object.assign(params, { id: clusterId.value })

    const checkResult = await fetchCheckCluster(params)
    if (!checkResult.isSuccess)
      throwApiFailure(checkResult, t('sys.api.apiRequestFailed'))

    const checkData = checkResult.data as { status?: number, msg?: string } | undefined
    const status = Number(checkData?.status ?? -1)
    if (status !== 0) {
      window.$message?.error(checkData?.msg || t('sys.api.apiRequestFailed'))
      return
    }

    const updateResult = await fetchUpdateCluster(params)
    if (!updateResult.isSuccess || !updateResult.data)
      throwApiFailure(updateResult, t('sys.api.apiRequestFailed'))

    window.$message?.success(
      formModel.value.clusterName.concat(t('setting.flinkCluster.operateMessage.updateFlinkClusterSuccessful')),
    )
    router.push('/flink/cluster')
  }
  catch (e: any) {
    showCatchError(e, t('sys.api.apiRequestFailed'))
  }
  finally {
    submitting.value = false
  }
}

function handleCancel() {
  router.push('/flink/cluster')
}

onMounted(async () => {
  await loadReferenceData()
  await loadCluster()
})
</script>

<template>
  <n-card :bordered="false" class="h-full">
    <n-spin :show="pageLoading">
      <ClusterFormFields
        ref="formFieldsRef"
        :model="formModel"
        :rules="rules"
        :deploy-mode-options="deployModeOptions"
        :flink-env-options="flinkEnvOptions"
        :alert-options="alertOptions"
        :total-memory-options="totalMemoryOptions"
        :jm-memory-options="jmMemoryOptions"
        :tm-memory-options="tmMemoryOptions"
        :resolve-order="resolveOrder"
        :k8s-rest-exposed-type="k8sRestExposedType"
        :history-record="historyRecord"
        :is-session-mode="isSessionMode"
        :is-standalone="isStandalone"
        :is-yarn-session="isYarnSession"
        :is-k8s-session="isK8sSession"
        :show-alert="showAlert"
      />
      <div class="mt-24px flex justify-center gap-12px">
        <n-button @click="handleCancel">
          {{ t('common.cancelText') }}
        </n-button>
        <n-button
          id="e2e-flinkcluster-submit-btn"
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
