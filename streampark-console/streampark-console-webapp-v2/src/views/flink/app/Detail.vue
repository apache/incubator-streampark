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
import { ionIconComponent } from '@/utils/ionIcon'
import { toTagColor } from '@/utils/tagColor'
import type { AppListRecord } from '@/types/api/flink/app.type'
import type { ExternalLink } from '@/service/api/setting/externalLink'
import { fetchAppExternalLink, fetchAppGet } from '@/service'
import { useIntervalFn } from '@vueuse/core'
import RequestModal from '@/views/shared/modals/RequestModal.vue'
import AppStateTag from './components/AppStateTag.vue'
import AppDetailTabs from './components/AppDetailTabs.vue'
import { deployModes } from '@/views/flink/app/shared/data/index'
import { handleView } from '@/views/flink/app/shared/utils'
import { AppTypeEnum, JobTypeEnum } from '@/enums/flinkEnum'

defineOptions({ name: 'FlinkAppDetail' })

const { t } = useI18n()
const route = useRoute()
const router = useRouter()

const app = reactive<Partial<AppListRecord>>({})
const appNotRunning = ref(true)
const externalLinks = ref<ExternalLink[]>([])
const requestModalVisible = ref(false)
const requestApiName = ref('')

async function loadExternalLinks(appId: string) {
    const result = await fetchAppExternalLink({ appId })
    if (result.isSuccess && Array.isArray(result.data)) externalLinks.value = result.data
}

function openRequestModal(name: string) {
    requestApiName.value = name
    requestModalVisible.value = true
}

function openExternalLink(url: string) {
    window.open(url, '_blank')
}

function deployModeLabel(mode?: number) {
    return deployModes.find((d) => d.value === mode)?.label ?? String(mode ?? '-')
}

async function loadApp() {
    const appId = route.query.appId as string
    if (!appId) {
        router.back()
        return
    }
    const result = await fetchAppGet({ id: appId })
    if (!result.isSuccess) {
        showResultError(result, t('sys.api.apiRequestFailed'))
        return
    }
    Object.assign(app, result.data)
    appNotRunning.value = !app.appControl?.allowView
    await loadExternalLinks(appId)
}

function goEdit() {
    if (app.appType === AppTypeEnum.STREAMPARK_FLINK)
        router.push({ path: '/flink/app/edit_streampark', query: { appId: app.id } })
    else router.push({ path: '/flink/app/edit_flink', query: { appId: app.id } })
}

const { pause } = useIntervalFn(loadApp, 5000, { immediateCallback: true })

onUnmounted(() => pause())
</script>

<template>
    <n-card :bordered="false">
        <template #header>
            <div class="flex items-center justify-between">
                <span>{{ t('flink.app.detail.detailTitle') }} — {{ app.jobName }}</span>
                <n-space>
                    <n-tag
                        v-for="link in externalLinks"
                        :key="link.id ?? link.badgeName"
                        :color="toTagColor(link.badgeColor)"
                        class="cursor-pointer"
                        @click="openExternalLink(link.linkUrl)"
                    >
                        {{ link.badgeLabel || link.badgeName }}
                    </n-tag>
                    <n-button
                        type="primary"
                        :disabled="appNotRunning"
                        @click="handleView(app as AppListRecord)"
                    >
                        <template #icon>
                            <n-icon :component="ionIconComponent('CloudOutline')" />
                        </template>
                        {{ t('flink.app.detail.flinkWebUi') }}
                    </n-button>
                    <n-button size="small" @click="openRequestModal('flinkStart')">
                        {{ t('flink.app.detail.copyStartcURL') }}
                    </n-button>
                    <n-button size="small" @click="openRequestModal('flinkCancel')">
                        {{ t('flink.app.detail.copyCancelcURL') }}
                    </n-button>
                    <n-button v-auth="'app:update'" type="primary" ghost @click="goEdit">
                        {{ t('flink.app.operation.edit') }}
                    </n-button>
                    <n-button circle @click="router.back()">
                        <template #icon>
                            <n-icon :component="ionIconComponent('ArrowBackOutline')" />
                        </template>
                    </n-button>
                </n-space>
            </div>
        </template>

        <n-descriptions bordered :column="2" size="small" label-placement="left">
            <n-descriptions-item :label="t('flink.app.appName')">
                {{ app.jobName }}
            </n-descriptions-item>
            <n-descriptions-item :label="t('flink.app.jobType')">
                <n-tag size="small">
                    {{
                        app.jobType === JobTypeEnum.JAR
                            ? 'JAR'
                            : app.jobType === JobTypeEnum.SQL
                              ? 'SQL'
                              : 'PY'
                    }}
                </n-tag>
            </n-descriptions-item>
            <n-descriptions-item :label="t('flink.app.runStatus')">
                <AppStateTag option="state" :data="app" />
            </n-descriptions-item>
            <n-descriptions-item :label="t('flink.app.releaseBuild')">
                <AppStateTag option="release" :data="app" />
            </n-descriptions-item>
            <n-descriptions-item :label="t('flink.app.flinkVersion')">
                {{ app.flinkVersion }}
            </n-descriptions-item>
            <n-descriptions-item :label="t('flink.app.deployMode')">
                {{ deployModeLabel(app.deployMode) }}
            </n-descriptions-item>
            <n-descriptions-item :label="t('flink.app.owner')">
                {{ app.nickName || app.userName }}
            </n-descriptions-item>
            <n-descriptions-item :label="t('flink.app.tags')">
                {{ app.tags || '-' }}
            </n-descriptions-item>
            <n-descriptions-item :label="t('common.description')" :span="2">
                {{ app.description || '-' }}
            </n-descriptions-item>
            <n-descriptions-item v-if="app.mainClass" :label="t('flink.app.mainClass')">
                {{ app.mainClass }}
            </n-descriptions-item>
            <n-descriptions-item v-if="app.jar" :label="t('flink.app.uploadJobJar')">
                {{ app.jar }}
            </n-descriptions-item>
            <n-descriptions-item :label="t('flink.app.modifiedTime')">
                {{ app.modifyTime }}
            </n-descriptions-item>
            <n-descriptions-item :label="t('common.createTime')">
                {{ app.createTime }}
            </n-descriptions-item>
        </n-descriptions>

        <n-divider />

        <AppDetailTabs :app="app" />
        <RequestModal v-model:show="requestModalVisible" :app="app" :api-name="requestApiName" />
    </n-card>
</template>
