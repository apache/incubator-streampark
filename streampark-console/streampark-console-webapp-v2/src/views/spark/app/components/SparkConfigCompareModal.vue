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
import { fetchGetSparkConf } from '@/service'
import { getMonacoOptions } from '@/views/flink/app/shared/data/index'
import { decodeByBase64 } from '@/utils/cipher'
import { useMonaco } from '@/hooks/web/useMonaco'

const props = defineProps<{
  show: boolean
  sourceId?: number | string | null
  sourceVersion?: number | string | null
  versions: Array<{ label: string, value: string | number }>
}>()

const emit = defineEmits<{
  'update:show': [value: boolean]
}>()

const { t } = useI18n()
const targetId = ref<string | number | null>(null)
const loading = ref(false)
const sourceRef = ref<HTMLElement | null>(null)
const targetRef = ref<HTMLElement | null>(null)

const { setContent: setSource } = useMonaco(sourceRef, {
  language: 'yaml',
  code: '',
  options: { ...(getMonacoOptions(true) as Recordable), readOnly: true },
})

const { setContent: setTarget } = useMonaco(targetRef, {
  language: 'yaml',
  code: '',
  options: { ...(getMonacoOptions(true) as Recordable), readOnly: true },
})

const targetOptions = computed(() =>
  props.versions.filter(v => v.value !== props.sourceId),
)

watch(
  () => props.show,
  (show) => {
    if (show) {
      targetId.value = null
      setSource('')
      setTarget('')
    }
  },
)

async function loadConfig(id: string | number, setter: (c: string) => Promise<void>) {
  const result = await fetchGetSparkConf({ id })
  if (!result.isSuccess)
    throwApiFailure(result, t('sys.api.apiRequestFailed'))
  const content = result.data?.content ? decodeByBase64(result.data.content) : ''
  await setter(content)
}

watch(
  () => [props.show, props.sourceId] as const,
  async ([show, sourceId]) => {
    if (!show || sourceId == null)
      return
    loading.value = true
    try {
      await loadConfig(sourceId, setSource)
    }
    catch (e: any) {
      showCatchError(e, t('sys.api.apiRequestFailed'))
    }
    finally {
      loading.value = false
    }
  },
)

watch(targetId, async (id) => {
  if (id == null)
    return
  loading.value = true
  try {
    await loadConfig(id, setTarget)
  }
  catch (e: any) {
    showCatchError(e, t('sys.api.apiRequestFailed'))
  }
  finally {
    loading.value = false
  }
})
</script>

<template>
  <n-modal
    :show="show"
    preset="card"
    :style="{ width: '960px' }"
    :title="t('spark.app.detail.compareConfig')"
    @update:show="emit('update:show', $event)"
  >
    <n-spin :show="loading">
      <n-form label-placement="left" label-width="120" class="mb-12px">
        <n-form-item :label="t('flink.app.detail.columns.version')">
          <n-tag type="info">
            v{{ sourceVersion }}
          </n-tag>
        </n-form-item>
        <n-form-item :label="t('flink.app.detail.compareTarget')">
          <n-select
            v-model:value="targetId"
            :options="targetOptions"
            :placeholder="t('common.chooseText')"
            class="max-w-240px"
          />
        </n-form-item>
      </n-form>
      <div class="grid grid-cols-2 gap-12px">
        <div ref="sourceRef" class="compare-editor" />
        <div ref="targetRef" class="compare-editor" />
      </div>
    </n-spin>
  </n-modal>
</template>

<style scoped>
.compare-editor {
  height: 420px;
  width: 100%;
  border: 1px solid var(--border-color);
}
</style>
