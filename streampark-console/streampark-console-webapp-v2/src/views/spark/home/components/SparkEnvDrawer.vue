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
import type { SparkEnv } from '@/types/api/spark/home.type'
import { fetchSparkEnv, fetchSparkSync } from '@/service'
import { useMonaco } from '@/hooks/web/useMonaco'
const props = defineProps<{
  show: boolean
  envId: string | null
}>()

const emit = defineEmits<{
  'update:show': [value: boolean]
}>()

const { t } = useI18n()

const loading = ref(false)
const syncLoading = ref(false)
const sparkInfo = ref<SparkEnv | null>(null)
const editorRef = ref<HTMLElement | null>(null)

const { setContent } = useMonaco(editorRef, {
  language: 'yaml',
  options: {
    selectOnLineNumbers: false,
    folding: true,
    foldingStrategy: 'indentation',
    overviewRulerBorder: false,
    tabSize: 2,
    readOnly: true,
    scrollBeyondLastLine: false,
    lineNumbersMinChars: 5,
    lineHeight: 24,
    automaticLayout: true,
    cursorStyle: 'line',
    cursorWidth: 3,
    renderFinalNewline: 'on',
    renderLineHighlight: 'all',
    quickSuggestionsDelay: 100,
    minimap: { enabled: true },
    scrollbar: {
      useShadows: false,
      vertical: 'visible',
      horizontal: 'visible',
      horizontalSliderSize: 5,
      verticalSliderSize: 5,
      horizontalScrollbarSize: 15,
      verticalScrollbarSize: 15,
    },
  },
})

async function loadEnvDetail(id: string) {
  loading.value = true
  try {
    const result = await fetchSparkEnv(id)
    if (!result.isSuccess)
      throwApiFailure(result, t('sys.api.apiRequestFailed'))
    sparkInfo.value = result.data ?? null
    await setContent(sparkInfo.value?.sparkConf ?? '')
  }
  catch (e: any) {
    showCatchError(e, t('sys.api.apiRequestFailed'))
    sparkInfo.value = null
    await setContent('')
  }
  finally {
    loading.value = false
  }
}

watch(
  () => [props.show, props.envId] as const,
  ([show, envId]) => {
    if (!show || !envId)
      return
    loadEnvDetail(envId)
  },
)

function closeDrawer() {
  emit('update:show', false)
}

async function handleSync() {
  if (!props.envId)
    return
  syncLoading.value = true
  try {
    const syncResult = await fetchSparkSync(props.envId)
    if (!syncResult.isSuccess)
      throwApiFailure(syncResult, t('sys.api.apiRequestFailed'))
    await loadEnvDetail(props.envId)
    const name = sparkInfo.value?.sparkName ?? ''
    window.$message?.success(t('spark.home.syncSuccess', [name]))
  }
  catch (e: any) {
    showCatchError(e, t('sys.api.apiRequestFailed'))
  }
  finally {
    syncLoading.value = false
  }
}
</script>

<template>
  <n-drawer
    :show="show"
    :width="'60%'"
    placement="right"
    @update:show="emit('update:show', $event)"
  >
    <n-drawer-content :title="t('spark.home.conf')" closable>
      <n-spin :show="loading">
        <div class="py-15px pl-10px">
          {{ t('spark.home.title') }}:&nbsp;&nbsp;{{ sparkInfo?.sparkHome }}
        </div>
        <div>
          <div class="pl-10px">
            {{ t('spark.home.sync') }}:
          </div>
          <div class="py-15px">
            <div ref="editorRef" class="min-h-480px" />
            <div class="mt-10px flex justify-end">
              <n-button type="primary" :loading="syncLoading" @click="handleSync">
                <template #icon>
                  <n-icon>
                    <IonIcon name="SyncOutline" />
                  </n-icon>
                </template>
                {{ t('spark.home.sync') }}
              </n-button>
            </div>
          </div>
        </div>
      </n-spin>
      <template #footer>
        <n-space justify="end">
          <n-button @click="closeDrawer">
            {{ t('common.cancelText') }}
          </n-button>
        </n-space>
      </template>
    </n-drawer-content>
  </n-drawer>
</template>
