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
import { fetchGetVer } from '@/service'
import { getMonacoOptions } from '@/views/flink/app/shared/data/index'
import { decodeByBase64 } from '@/utils/cipher'
import { useMonaco } from '@/hooks/web/useMonaco'

const props = defineProps<{
  show: boolean
  appId?: string | null
  sourceVersion?: number | string | null
  initialTarget?: number | string | null
  versions: Array<{ label: string, value: string | number }>
}>()

const emit = defineEmits<{
  'update:show': [value: boolean]
}>()

const { t } = useI18n()
const targetVersion = ref<string | number | null>(null)
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

watch(
  () => props.show,
  (show) => {
    if (show) {
      targetVersion.value = props.initialTarget ?? null
      if (!targetVersion.value) {
        setSource('')
        setTarget('')
      }
    }
    else {
      targetVersion.value = null
      setSource('')
      setTarget('')
    }
  },
)

async function loadConfig(version: string | number, setter: (c: string) => Promise<void>) {
  const result = await fetchGetVer({ id: String(version) })
  if (!result.isSuccess)
    throwApiFailure(result, t('sys.api.apiRequestFailed'))
  const content = result.data?.content ? decodeByBase64(result.data.content) : ''
  await setter(content)
}

watch(
  () => [props.show, props.sourceVersion] as const,
  async ([show, source]) => {
    if (!show || source == null)
      return
    loading.value = true
    try {
      await loadConfig(source, setSource)
    }
    catch (e: any) {
      showCatchError(e, t('sys.api.apiRequestFailed'))
    }
    finally {
      loading.value = false
    }
  },
)

watch(targetVersion, async (target) => {
  if (target == null)
    return
  loading.value = true
  try {
    await loadConfig(target, setTarget)
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
    :title="t('flink.app.detail.compareConf')"
    @update:show="emit('update:show', $event)"
  >
    <n-spin :show="loading">
      <div class="mb-12px">
        <n-form-item :label="t('flink.app.detail.targetVersion')" label-placement="left" :show-feedback="false">
          <n-select
            v-model:value="targetVersion"
            :options="versions.filter(v => String(v.value) !== String(sourceVersion))"
            clearable
            class="max-w-320px"
          />
        </n-form-item>
      </div>
      <n-grid :cols="2" :x-gap="12">
        <n-gi>
          <n-text depth="3" class="mb-4px block text-12px">
            source v{{ sourceVersion }}
          </n-text>
          <div ref="sourceRef" class="compare-editor" />
        </n-gi>
        <n-gi>
          <n-text depth="3" class="mb-4px block text-12px">
            target v{{ targetVersion ?? '-' }}
          </n-text>
          <div ref="targetRef" class="compare-editor" />
        </n-gi>
      </n-grid>
    </n-spin>
  </n-modal>
</template>

<style scoped>
.compare-editor {
  height: 420px;
  width: 100%;
  border: 1px solid var(--border-color);
  border-radius: 4px;
}
</style>
