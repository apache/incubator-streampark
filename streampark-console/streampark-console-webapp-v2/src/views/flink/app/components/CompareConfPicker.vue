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
import ConfigCompareModal from './ConfigCompareModal.vue'

const props = defineProps<{
  appId: string
  configVersions: Array<{ id: string, version?: number, effective?: boolean }>
}>()

const selected = defineModel<string[]>('value', { default: () => [] })

const { t } = useI18n()

const compareVisible = ref(false)
const compareSource = ref<string | number | null>(null)

const versionOptions = computed(() =>
  props.configVersions.map(ver => ({
    label: `v${ver.version ?? ver.id}${ver.effective ? ' (effective)' : ''}`,
    value: ver.id,
  })),
)

function handleCompare() {
  if (selected.value.length !== 2) {
    window.$message?.warning(t('flink.app.addAppTips.compareConfPlaceholder'))
    return
  }
  compareSource.value = selected.value[0]
  compareVisible.value = true
}
</script>

<template>
  <n-space align="center" class="w-full">
    <n-select
      v-model:value="selected"
      style="flex: 1"
      multiple
      :max-tag-count="2"
      :options="versionOptions"
      :placeholder="t('flink.app.addAppTips.compareConfPlaceholder')"
    />
    <n-button
      type="primary"
      :disabled="selected.length !== 2"
      @click="handleCompare"
    >
      {{ t('common.compareText') }}
    </n-button>
  </n-space>
  <ConfigCompareModal
    v-model:show="compareVisible"
    :app-id="appId"
    :source-version="compareSource"
    :initial-target="selected[1]"
    :versions="versionOptions"
  />
</template>
