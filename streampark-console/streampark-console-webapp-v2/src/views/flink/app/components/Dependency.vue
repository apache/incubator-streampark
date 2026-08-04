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
import { fetchListFlinkEnv, fetchUpload, fetchUploadJars } from '@/service'
import { getMonacoOptions } from '@/views/flink/app/shared/data/index'
import { getPomId, toPomString, type PomDependency } from '@/views/flink/app/shared/pom'
import UploadJobJar from './UploadJobJar.vue'
import { useMonaco } from '@/hooks/web/useMonaco'
import { toTagColor } from '@/utils/tagColor'

const props = defineProps<{
    formModel: Recordable
    flinkEnvs?: Array<{ id: string; scalaVersion?: string }>
}>()

const { t } = useI18n()

const activeTab = ref('pom')
const pomBox = ref<HTMLElement | null>(null)
const dependency = reactive<{ pom: Record<string, PomDependency>; jar: Record<string, string> }>({
    pom: {},
    jar: {},
})
const selectedHistoryUploadJars = ref<string[]>([])
const dependencyRecords = ref<PomDependency[]>([])
const uploadJars = ref<string[]>([])
const historyUploadJars = ref<string[]>([])
const loading = ref(false)
const cachedFlinkEnvs = ref<Array<{ id: string; scalaVersion?: string }>>([])

const defaultPomValue = ''

const { setContent, getContent, getInstance } = useMonaco(pomBox, {
    language: 'xml',
    code: defaultPomValue,
    options: {
        minimap: { enabled: true },
        ...(getMonacoOptions(false) as Recordable),
    },
})

async function resolveFlinkEnvs() {
    if (props.flinkEnvs?.length) return props.flinkEnvs
    if (cachedFlinkEnvs.value.length) return cachedFlinkEnvs.value
    const result = await fetchListFlinkEnv()
    if (result.isSuccess && result.data)
        cachedFlinkEnvs.value = result.data as Array<{ id: string; scalaVersion?: string }>
    return cachedFlinkEnvs.value
}

async function handleApplyPom() {
    const versionId = props.formModel?.versionId
    if (versionId == null) {
        window.$message?.error(t('flink.app.dependencyError'))
        return
    }

    const flinkEnv = await resolveFlinkEnvs()
    const scalaVersion = flinkEnv.find((v) => v.id === versionId)?.scalaVersion
    const propsValue = await getContent()
    if (!propsValue?.trim()) return

    const groupExp = /<groupId>([\s\S]*?)<\/groupId>/
    const artifactExp = /<artifactId>([\s\S]*?)<\/artifactId>/
    const versionExp = /<version>([\s\S]*?)<\/version>/
    const classifierExp = /<classifier>([\s\S]*?)<\/classifier>/
    const exclusionsExp = /<exclusions>([\s\S]*?)<\/exclusions>/
    const invalidArtifact: string[] = []

    propsValue
        .split('</dependency>')
        .filter((x) => x.replace(/\s+/, '') !== '')
        .forEach((dep) => {
            const groupId = dep.match(groupExp)?.[1]?.trim() ?? null
            const artifactId = dep.match(artifactExp)?.[1]?.trim() ?? null
            const version = dep.match(versionExp)?.[1]?.trim() ?? null
            const classifier = dep.match(classifierExp)?.[1]?.trim() ?? null
            const exclusion = dep.match(exclusionsExp)?.[1]?.trim() ?? null
            if (groupId != null && artifactId != null && version != null) {
                if (/flink-(.*)_(.*)/.test(artifactId)) {
                    const depScalaVersion = artifactId.substring(artifactId.lastIndexOf('_') + 1)
                    if (scalaVersion !== depScalaVersion) invalidArtifact.push(artifactId)
                }
                if (invalidArtifact.length === 0) {
                    const mvnPom: PomDependency = { groupId, artifactId, version, exclusions: [] }
                    if (classifier != null) mvnPom.classifier = classifier
                    if (exclusion != null) {
                        exclusion.split('<exclusion>').forEach((e) => {
                            if (e?.length > 0) {
                                const eGroup = e.match(groupExp)?.[1]?.trim()
                                const eArtifact = e.match(artifactExp)?.[1]?.trim()
                                if (eGroup && eArtifact) {
                                    mvnPom.exclusions!.push({
                                        groupId: eGroup,
                                        artifactId: eArtifact,
                                    })
                                }
                            }
                        })
                    }
                    dependency.pom[getPomId(mvnPom)] = mvnPom
                }
            }
        })

    if (invalidArtifact.length > 0) {
        window.$dialog?.error({
            title: t('flink.app.dependencyValidate.invalidTitle'),
            content: `${t('flink.app.dependencyValidate.invalidContent', { scalaVersion })}\n${invalidArtifact.join('\n')}`,
        })
        return
    }
    handleUpdateDependency()
    setContent(defaultPomValue)
}

async function handleCustomDepsRequest(data: { file: File }) {
    try {
        loading.value = true
        const formData = new FormData()
        formData.append('file', data.file)
        await fetchUpload(formData)
        dependency.jar[data.file.name] = data.file.name
        handleUpdateDependency()
    } catch (e) {
        console.error(e)
    } finally {
        loading.value = false
    }
}

function handleUpdateDependency() {
    dependencyRecords.value = Object.keys(dependency.pom).map((k) => dependency.pom[k])
    uploadJars.value = Object.keys(dependency.jar)
}

async function handleReloadHistoryUploads() {
    selectedHistoryUploadJars.value = []
    const result = await fetchUploadJars()
    if (result.isSuccess && result.data) historyUploadJars.value = result.data
}

const filteredHistoryUploadJarsOptions = computed(() =>
    historyUploadJars.value.filter((o) => !Reflect.has(dependency.jar, o)),
)

const historyUploadOptions = computed(() =>
    filteredHistoryUploadJarsOptions.value.map((item) => ({ label: item, value: item })),
)

function handleRemoveJar(jar: string) {
    delete dependency.jar[jar]
    handleUpdateDependency()
}

function handleRemovePom(pom: PomDependency) {
    delete dependency.pom[getPomId(pom)]
    handleUpdateDependency()
}

function handleEditPom(pom: PomDependency) {
    activeTab.value = 'pom'
    setContent(toPomString(pom))
}

function setDefaultValue(dataSource: { pom?: PomDependency[]; jar?: string[] }) {
    dependencyRecords.value = dataSource.pom || []
    uploadJars.value = dataSource.jar || []
    dependency.pom = {}
    dependency.jar = {}
    dataSource.pom?.forEach((pomRecord) => {
        dependency.pom[getPomId(pomRecord)] = pomRecord
    })
    dataSource.jar?.forEach((fileName) => {
        dependency.jar[fileName] = fileName
    })
}

function handleHistoryUploadJarsChange(values: string[]) {
    values.forEach((item) => {
        if (!Reflect.has(dependency.jar, item)) dependency.jar[item] = item
    })
    selectedHistoryUploadJars.value = []
    handleUpdateDependency()
}

onMounted(() => {
    handleReloadHistoryUploads()
    relayoutPomEditor()
})

watch(activeTab, (tab) => {
    if (tab === 'pom') relayoutPomEditor()
})

async function relayoutPomEditor() {
    await nextTick()
    requestAnimationFrame(async () => {
        const editor = await getInstance()
        editor?.layout()
    })
}

defineExpose({
    setDefaultValue,
    handleApplyPom,
    dependencyRecords,
    uploadJars,
})
</script>

<template>
    <n-tabs v-model:value="activeTab" type="card" class="w-full min-w-0">
        <n-tab-pane name="pom" :tab="t('flink.app.mavenPom')">
            <div class="relative w-full min-w-0">
                <div ref="pomBox" class="syntax-true w-full min-w-0" style="height: 330px" />
                <n-button type="primary" class="absolute right-8px top-8px" @click="handleApplyPom">
                    {{ t('common.apply') }}
                </n-button>
            </div>
        </n-tab-pane>
        <n-tab-pane name="jar" :tab="t('flink.app.uploadJar')">
            <n-select
                v-if="filteredHistoryUploadJarsOptions.length > 0"
                v-model:value="selectedHistoryUploadJars"
                multiple
                filterable
                class="mb-12px"
                :placeholder="t('flink.app.addAppTips.searchHistoryUploads')"
                :options="historyUploadOptions"
                @update:value="handleHistoryUploadJarsChange"
            />
            <UploadJobJar :custom-request="handleCustomDepsRequest" :loading="loading" />
        </n-tab-pane>
    </n-tabs>
    <div
        v-if="dependencyRecords.length > 0 || uploadJars.length > 0"
        class="mt-12px flex flex-col gap-8px"
    >
        <n-alert
            v-for="(dept, index) in dependencyRecords"
            :key="`dependency_${index}`"
            type="info"
            class="cursor-pointer"
            @click="handleEditPom(dept)"
        >
            <template #header>
                <n-space align="center">
                    <n-tag :color="toTagColor('#2db7f5')"> POM </n-tag>
                    <span v-if="dept.classifier != null">
                        {{ dept.artifactId }}-{{ dept.version }}-{{ dept.classifier }}.jar
                    </span>
                    <span v-else>{{ dept.artifactId }}-{{ dept.version }}.jar</span>
                    <n-button text type="error" @click.stop="handleRemovePom(dept)"> × </n-button>
                </n-space>
            </template>
        </n-alert>
        <n-alert v-for="jar in uploadJars" :key="`upload_jars_${jar}`" type="info">
            <template #header>
                <n-space align="center">
                    <n-tag :color="toTagColor('#108ee9')"> JAR </n-tag>
                    {{ jar }}
                    <n-button text type="error" @click="handleRemoveJar(jar)"> × </n-button>
                </n-space>
            </template>
        </n-alert>
    </div>
</template>
