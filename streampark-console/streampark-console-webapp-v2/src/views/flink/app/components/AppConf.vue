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
import { fetchAppConf, fetchGetVer, fetchName, fetchProjectListConf } from '@/service'
import { getAppConfType } from '@/views/flink/app/shared/utils'
import MergelyDrawer from '@/views/shared/editors/MergelyDrawer.vue'
import { ConfigTypeEnum, UseStrategyEnum } from '@/enums/flinkEnum'
import { decodeByBase64 } from '@/utils/cipher'

const props = defineProps<{
    projectId?: string | null
    module?: string | null
    configVersions: Array<{ id: string; version?: number; effective?: boolean }>
}>()

const model = defineModel<Recordable>({ required: true })

const { t } = useI18n()

const confTree = ref<Recordable[]>([])
const mergelyVisible = ref(false)

const strategyOptions = [
    { label: t('flink.app.addAppTips.useExisting'), value: UseStrategyEnum.USE_EXIST },
    { label: t('flink.app.addAppTips.reselect'), value: UseStrategyEnum.RESELECT },
]

const versionOptions = computed(() =>
    props.configVersions.map((ver) => ({
        label: `v${ver.version ?? ver.id}${ver.effective ? ' (effective)' : ''}`,
        value: ver.id,
    })),
)

async function loadConfTree() {
    if (!props.projectId || !props.module) return
    const result = await fetchProjectListConf({ id: props.projectId, module: props.module })
    if (result.isSuccess) confTree.value = (result.data ?? []) as Recordable[]
}

async function handleVersionChange(versionId: string | null) {
    if (!versionId) return
    const result = await fetchGetVer({ id: versionId })
    if (!result.isSuccess) return
    const content = result.data?.content ?? result.data
    model.value.configId = versionId
    model.value.configOverride = decodeByBase64(String(content ?? ''))
    model.value.isSetConfig = true
}

async function handleConfigPathChange(configPath: string | null) {
    if (!configPath) {
        model.value.config = null
        return
    }
    const confType = getAppConfType(configPath)
    if (confType === ConfigTypeEnum.UNKNOWN) {
        window.$message?.warning(t('flink.app.addAppTips.configFileInvalid'))
        model.value.config = null
        return
    }
    model.value.config = configPath
    const [nameResult, confResult] = await Promise.all([
        fetchName({ config: configPath }),
        fetchAppConf({ config: configPath }),
    ])
    if (nameResult.isSuccess && nameResult.data) model.value.jobName = nameResult.data
    if (confResult.isSuccess && confResult.data)
        model.value.configOverride = decodeByBase64(String(confResult.data))
    model.value.isSetConfig = true
}

function handleMergelyOk(payload: { isSetConfig: boolean; configOverride: string | null }) {
    model.value.isSetConfig = payload.isSetConfig
    model.value.configOverride = payload.configOverride ?? ''
}

watch(
    () => [props.projectId, props.module] as const,
    () => loadConfTree(),
    { immediate: true },
)

onMounted(() => {
    if (!model.value.strategy) model.value.strategy = UseStrategyEnum.USE_EXIST
})
</script>

<template>
    <n-space align="center" class="w-full">
        <n-select v-model:value="model.strategy" style="width: 200px" :options="strategyOptions" />
        <n-select
            v-if="model.strategy === UseStrategyEnum.USE_EXIST"
            v-model:value="model.configId"
            style="min-width: 280px"
            filterable
            clearable
            :options="versionOptions"
            :placeholder="t('flink.app.addAppTips.configTagPlaceholder')"
            @update:value="handleVersionChange"
        />
        <n-tree-select
            v-else
            v-model:value="model.config"
            style="min-width: 320px"
            filterable
            clearable
            :options="confTree"
            key-field="value"
            label-field="title"
            children-field="children"
            :placeholder="t('flink.app.addAppTips.configSelectPlaceholder')"
            @update:value="handleConfigPathChange"
        />
        <n-button
            :disabled="model.strategy === UseStrategyEnum.RESELECT && !model.config"
            type="primary"
            @click="mergelyVisible = true"
        >
            {{ t('common.editText') }}
        </n-button>
    </n-space>
    <n-text v-if="model.config" depth="3" class="mt-8px block text-12px">
        {{ model.config }}
    </n-text>
    <MergelyDrawer
        v-model:show="mergelyVisible"
        template-source="flink"
        :original-value="model.configOverride"
        @ok="handleMergelyOk"
    />
</template>
