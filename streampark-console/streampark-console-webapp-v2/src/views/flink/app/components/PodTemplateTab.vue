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
import {
  fetchCompleteHostAliasToPodTemplate,
  fetchExtractHostAliasFromPodTemplate,
  fetchFlinkJmPodTemplates,
  fetchFlinkPodTemplates,
  fetchFlinkTmPodTemplates,
  fetchInitPodTemplate,
  fetchPreviewHostAlias,
  fetchSysHosts,
} from '@/service'
import { getMonacoOptions } from '@/views/flink/app/shared/data/index'
import Icon from '@/components/Icon/src/Icon.vue'
import { useMonaco } from '@/hooks/web/useMonaco'
import { toTagColor } from '@/utils/tagColor'

type VisualType = 'ptVisual' | 'jmPtVisual' | 'tmPtVisual'

const props = defineProps<{
  podTemplate?: string
  jmPodTemplate?: string
  tmPodTemplate?: string
}>()

const emit = defineEmits<{
  'update:podTemplate': [value: string]
  'update:jmPodTemplate': [value: string]
  'update:tmPodTemplate': [value: string]
}>()

const { t } = useI18n()

const podTemplateTab = ref('pod-template')
const podTemplateRef = ref<HTMLElement | null>(null)
const jmPodTemplateRef = ref<HTMLElement | null>(null)
const tmPodTemplateRef = ref<HTMLElement | null>(null)

const historyRecord = reactive<Record<VisualType, string[]>>({
  ptVisual: [],
  jmPtVisual: [],
  tmPtVisual: [],
})

const historyDrawerVisible = ref(false)
const historyVisualType = ref<VisualType>('ptVisual')
const historyDataSource = ref<string[]>([])

const hostAliasDrawerVisible = ref(false)
const hostAliasVisualType = ref<VisualType>('ptVisual')
const hostAliasSelectValue = ref<string[]>([])
const hostAliasPreview = ref('')
const hostAliasPodTemplate = ref('')
const sysHostsAlias = ref<string[]>([])

const drawerTitleMap = computed<Record<VisualType, string>>(() => ({
  ptVisual: t('flink.app.pod.historyTitle'),
  jmPtVisual: t('flink.app.pod.jmHistoryTitle'),
  tmPtVisual: t('flink.app.pod.tmHistoryTitle'),
}))

const cardTitleMap: Record<VisualType, string> = {
  ptVisual: 'pod-template.yaml',
  jmPtVisual: 'jm-pod-template.yaml',
  tmPtVisual: 'tm-pod-template.yaml',
}

const hostAliasTitleMap = computed<Record<VisualType, string>>(() => ({
  ptVisual: t('flink.app.pod.hostAliasTitle'),
  jmPtVisual: t('flink.app.pod.jmHostAliasTitle'),
  tmPtVisual: t('flink.app.pod.tmHostAliasTitle'),
}))

const { setContent: setPodContent, onUpdateValue: podContentChange } = useMonaco(
  podTemplateRef,
  { language: 'yaml', ...(getMonacoOptions(false) as Recordable) },
)
podContentChange(value => emit('update:podTemplate', value))

const { setContent: setJmPodContent, onUpdateValue: jmPodContentChange } = useMonaco(
  jmPodTemplateRef,
  { language: 'yaml', ...(getMonacoOptions(false) as Recordable) },
)
jmPodContentChange(value => emit('update:jmPodTemplate', value))

const { setContent: setTmPodContent, onUpdateValue: tmPodContentChange } = useMonaco(
  tmPodTemplateRef,
  { language: 'yaml', ...(getMonacoOptions(false) as Recordable) },
)
tmPodContentChange(value => emit('update:tmPodTemplate', value))

const hostAliasOptions = computed(() =>
  sysHostsAlias.value.map(item => ({ label: item, value: item })),
)

async function loadHistoryTemplates(visualType: VisualType) {
  if (historyRecord[visualType].length > 0)
    return historyRecord[visualType]
  let result
  if (visualType === 'ptVisual')
    result = await fetchFlinkPodTemplates({})
  else if (visualType === 'jmPtVisual')
    result = await fetchFlinkJmPodTemplates({})
  else
    result = await fetchFlinkTmPodTemplates({})
  if (result.isSuccess && result.data)
    historyRecord[visualType] = result.data
  return historyRecord[visualType]
}

async function showPodTemplateDrawer(visualType: VisualType) {
  historyVisualType.value = visualType
  historyDataSource.value = await loadHistoryTemplates(visualType)
  historyDrawerVisible.value = true
}

async function handleGetInitPodTemplate(visualType: VisualType) {
  const result = await fetchInitPodTemplate({})
  if (result.isSuccess && result.data)
    handleChoicePodTemplate(visualType, result.data)
}

async function showTemplateHostAliasDrawer(visualType: VisualType) {
  hostAliasVisualType.value = visualType
  let tmplContent = ''
  if (visualType === 'ptVisual')
    tmplContent = props.podTemplate ?? ''
  else if (visualType === 'jmPtVisual')
    tmplContent = props.jmPodTemplate ?? ''
  else
    tmplContent = props.tmPodTemplate ?? ''

  hostAliasPodTemplate.value = tmplContent
  hostAliasSelectValue.value = []
  hostAliasPreview.value = ''

  const sysResult = await fetchSysHosts({})
  if (sysResult.isSuccess && sysResult.data)
    sysHostsAlias.value = sysResult.data

  if (tmplContent) {
    const extractResult = await fetchExtractHostAliasFromPodTemplate({ podTemplate: tmplContent })
    if (extractResult.isSuccess && extractResult.data) {
      hostAliasSelectValue.value = extractResult.data
      await handleSelectedTemplateHostAlias()
    }
  }
  hostAliasDrawerVisible.value = true
}

async function handleSelectedTemplateHostAlias() {
  const result = await fetchPreviewHostAlias({ hosts: hostAliasSelectValue.value.join(',') })
  if (result.isSuccess)
    hostAliasPreview.value = result.data ?? ''
}

async function handleSubmitHostAliasToPodTemplate() {
  const result = await fetchCompleteHostAliasToPodTemplate({
    hosts: hostAliasSelectValue.value.join(','),
    podTemplate: hostAliasPodTemplate.value,
  })
  if (result.isSuccess && result.data)
    handleChoicePodTemplate(hostAliasVisualType.value, result.data)
  hostAliasDrawerVisible.value = false
}

function handleChoicePodTemplate(visualType: VisualType, content: string) {
  switch (visualType) {
    case 'ptVisual':
      emit('update:podTemplate', content)
      setPodContent(content)
      break
    case 'jmPtVisual':
      emit('update:jmPodTemplate', content)
      setJmPodContent(content)
      break
    case 'tmPtVisual':
      emit('update:tmPodTemplate', content)
      setTmPodContent(content)
      break
  }
  historyDrawerVisible.value = false
}

defineExpose({
  handleChoicePodTemplate,
})
</script>

<template>
  <n-tabs v-model:value="podTemplateTab" type="card">
    <n-tab-pane name="pod-template" :tab="t('flink.app.pod.tab')" display-directive="show">
      <div ref="podTemplateRef" class="syntax-true mb-8px" style="height: 280px" />
      <n-button-group size="small">
        <n-button type="primary" @click="showPodTemplateDrawer('ptVisual')">
          <Icon icon="ion:time-outline" class="mr-4px" />
          {{ t('common.history') }}
        </n-button>
        <n-button @click="handleGetInitPodTemplate('ptVisual')">
          <Icon icon="ion:copy-outline" class="mr-4px" />
          {{ t('flink.app.pod.init') }}
        </n-button>
        <n-button @click="showTemplateHostAliasDrawer('ptVisual')">
          <Icon icon="ion:share-social-outline" class="mr-4px" />
          {{ t('flink.app.pod.host') }}
        </n-button>
      </n-button-group>
    </n-tab-pane>
    <n-tab-pane name="jm-pod-template" :tab="t('flink.app.pod.jmTab')" display-directive="show">
      <div ref="jmPodTemplateRef" class="syntax-true mb-8px" style="height: 280px" />
      <n-button-group size="small">
        <n-button type="primary" @click="showPodTemplateDrawer('jmPtVisual')">
          <Icon icon="ion:time-outline" class="mr-4px" />
          {{ t('common.history') }}
        </n-button>
        <n-button @click="handleGetInitPodTemplate('jmPtVisual')">
          <Icon icon="ion:copy-outline" class="mr-4px" />
          {{ t('flink.app.pod.init') }}
        </n-button>
        <n-button @click="showTemplateHostAliasDrawer('jmPtVisual')">
          <Icon icon="ion:share-social-outline" class="mr-4px" />
          {{ t('flink.app.pod.host') }}
        </n-button>
      </n-button-group>
    </n-tab-pane>
    <n-tab-pane name="tm-pod-template" :tab="t('flink.app.pod.tmTab')" display-directive="show">
      <div ref="tmPodTemplateRef" class="syntax-true mb-8px" style="height: 280px" />
      <n-button-group size="small">
        <n-button type="primary" @click="showPodTemplateDrawer('tmPtVisual')">
          <Icon icon="ion:time-outline" class="mr-4px" />
          {{ t('common.history') }}
        </n-button>
        <n-button @click="handleGetInitPodTemplate('tmPtVisual')">
          <Icon icon="ion:copy-outline" class="mr-4px" />
          {{ t('flink.app.pod.init') }}
        </n-button>
        <n-button @click="showTemplateHostAliasDrawer('tmPtVisual')">
          <Icon icon="ion:share-social-outline" class="mr-4px" />
          {{ t('flink.app.pod.host') }}
        </n-button>
      </n-button-group>
    </n-tab-pane>
  </n-tabs>

  <n-drawer v-model:show="historyDrawerVisible" :width="700">
    <n-drawer-content :title="drawerTitleMap[historyVisualType]">
      <n-empty v-if="historyDataSource.length === 0" />
      <n-card
        v-for="(item, index) in historyDataSource"
        :key="index"
        :title="cardTitleMap[historyVisualType]"
        size="small"
        class="mb-8px"
        hoverable
      >
        <template #header-extra>
          <n-button text type="primary" @click="handleChoicePodTemplate(historyVisualType, item)">
            {{ t('flink.app.pod.choice') }}
          </n-button>
        </template>
        <pre class="text-12px">{{ item }}</pre>
      </n-card>
    </n-drawer-content>
  </n-drawer>

  <n-drawer v-model:show="hostAliasDrawerVisible" :width="500">
    <n-drawer-content :title="hostAliasTitleMap[hostAliasVisualType]">
      <n-tag :color="toTagColor('#2db7f5')" class="mb-12px">
        {{ t('flink.app.pod.hostAliasNote') }}
      </n-tag>
      <p class="mb-16px text-14px">
        {{ t('flink.app.pod.hostAliasHint') }}
      </p>
      <n-select
        v-model:value="hostAliasSelectValue"
        multiple
        filterable
        :placeholder="t('flink.app.addAppTips.searchSystemHosts')"
        :options="hostAliasOptions"
        @update:value="handleSelectedTemplateHostAlias"
      />
      <n-card :title="t('flink.app.pod.preview')" size="small" class="mt-24px" hoverable>
        <pre class="text-12px">{{ hostAliasPreview }}</pre>
      </n-card>
      <template #footer>
        <n-space justify="end">
          <n-button @click="hostAliasDrawerVisible = false">
            {{ t('common.cancelText') }}
          </n-button>
          <n-button type="primary" @click="handleSubmitHostAliasToPodTemplate">
            {{ t('common.submitText') }}
          </n-button>
        </n-space>
      </template>
    </n-drawer-content>
  </n-drawer>
</template>
