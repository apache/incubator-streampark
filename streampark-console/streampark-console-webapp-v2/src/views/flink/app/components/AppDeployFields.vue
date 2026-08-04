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
import type { FlinkCluster } from '@/types/api/flink/flinkCluster.type'
import {
    fetchFlinkBaseImages,
    fetchFlinkClusterList,
    fetchK8sNamespaces,
    fetchSessionClusterIds,
} from '@/service'
import { k8sRestExposedType } from '@/views/flink/app/shared/data/index'
import { isK8sDeployMode } from '@/views/flink/app/shared/utils'
import UseSysHadoopConf from './UseSysHadoopConf.vue'
import { ClusterStateEnum, DeployMode } from '@/enums/flinkEnum'

const model = defineModel<Recordable>({ required: true })

const { t } = useI18n()

const flinkClusters = ref<FlinkCluster[]>([])
const k8sNamespaces = ref<string[]>([])
const k8sSessionClusterIds = ref<string[]>([])
const flinkBaseImages = ref<string[]>([])

const isStandalone = computed(() => model.value.deployMode === DeployMode.STANDALONE)
const isYarnSession = computed(() => model.value.deployMode === DeployMode.YARN_SESSION)
const isK8sSession = computed(() => model.value.deployMode === DeployMode.KUBERNETES_SESSION)
const isK8sApp = computed(() => model.value.deployMode === DeployMode.KUBERNETES_APPLICATION)
const showYarnQueue = computed(
    () =>
        model.value.deployMode === DeployMode.YARN_APPLICATION ||
        model.value.deployMode === DeployMode.YARN_PER_JOB,
)

function clusterOptions(deployMode: number) {
    return flinkClusters.value
        .filter((c) => c.deployMode === deployMode)
        .map((c) => ({
            label: `${c.clusterName}${c.clusterState === ClusterStateEnum.RUNNING ? '' : ' (offline)'}`,
            value: c.id,
            disabled: c.clusterState !== ClusterStateEnum.RUNNING,
        }))
}

async function loadOptions() {
    const [clusterResult, nsResult, sessionResult, imageResult] = await Promise.all([
        fetchFlinkClusterList(),
        fetchK8sNamespaces(),
        fetchSessionClusterIds({ deployMode: DeployMode.KUBERNETES_SESSION }),
        fetchFlinkBaseImages(),
    ])
    if (clusterResult.isSuccess) flinkClusters.value = clusterResult.data ?? []
    if (nsResult.isSuccess) k8sNamespaces.value = nsResult.data ?? []
    if (sessionResult.isSuccess) k8sSessionClusterIds.value = sessionResult.data ?? []
    if (imageResult.isSuccess) flinkBaseImages.value = imageResult.data ?? []
}

onMounted(() => loadOptions())
</script>

<template>
    <n-grid :cols="24" :x-gap="16">
        <template v-if="isStandalone">
            <n-form-item-gi :span="12" :label="t('flink.app.flinkCluster')">
                <n-select
                    v-model:value="model.remoteClusterId"
                    filterable
                    :options="clusterOptions(DeployMode.STANDALONE)"
                    :placeholder="t('flink.app.addAppTips.flinkClusterIsRequiredMessage')"
                />
            </n-form-item-gi>
        </template>
        <template v-if="isYarnSession">
            <n-form-item-gi :span="12" :label="t('flink.app.flinkCluster')">
                <n-select
                    v-model:value="model.yarnSessionClusterId"
                    filterable
                    :options="clusterOptions(DeployMode.YARN_SESSION)"
                    :placeholder="t('flink.app.addAppTips.flinkClusterIsRequiredMessage')"
                />
            </n-form-item-gi>
        </template>
        <template v-if="isK8sSession">
            <n-form-item-gi :span="12" :label="t('flink.app.flinkCluster')">
                <n-select
                    v-model:value="model.k8sSessionClusterId"
                    filterable
                    :options="clusterOptions(DeployMode.KUBERNETES_SESSION)"
                    :placeholder="t('flink.app.addAppTips.flinkClusterIsRequiredMessage')"
                />
            </n-form-item-gi>
        </template>
        <template v-if="isK8sApp">
            <n-form-item-gi :span="12" :label="t('flink.app.kubernetesNamespace')">
                <n-auto-complete
                    v-model:value="model.k8sNamespace"
                    :options="k8sNamespaces.map((v) => ({ label: v, value: v }))"
                    :placeholder="t('flink.app.addAppTips.kubernetesNamespacePlaceholder')"
                />
            </n-form-item-gi>
            <n-form-item-gi :span="12" :label="t('setting.flinkCluster.form.serviceAccount')">
                <n-auto-complete
                    v-model:value="model.serviceAccount"
                    :options="k8sNamespaces.map((v) => ({ label: v, value: v }))"
                    :placeholder="t('flink.app.addAppTips.serviceAccountPlaceholder')"
                />
            </n-form-item-gi>
            <n-form-item-gi :span="12" :label="t('flink.app.flinkBaseDockerImage')">
                <n-auto-complete
                    v-model:value="model.flinkImage"
                    :options="flinkBaseImages.map((v) => ({ label: v, value: v }))"
                    :placeholder="t('flink.app.addAppTips.flinkImagePlaceholder')"
                />
            </n-form-item-gi>
            <n-form-item-gi :span="12" :label="t('flink.app.restServiceExposedType')">
                <n-select
                    v-model:value="model.k8sRestExposedType"
                    :options="k8sRestExposedType"
                    :placeholder="t('flink.app.addAppTips.k8sRestExposedTypePlaceholder')"
                />
            </n-form-item-gi>
            <n-form-item-gi :span="12" :label="t('flink.app.addAppTips.useSysHadoopConf')">
                <UseSysHadoopConf v-model="model.useSysHadoopConf" />
            </n-form-item-gi>
        </template>
        <n-form-item-gi v-if="showYarnQueue" :span="12" :label="t('flink.app.yarnQueue')">
            <n-input v-model:value="model.yarnQueue" :placeholder="t('flink.app.yarnQueue')" />
        </n-form-item-gi>
        <n-form-item-gi
            v-if="!isK8sDeployMode(model.deployMode)"
            :span="12"
            :label="t('flink.app.hadoopUser')"
        >
            <n-input v-model:value="model.hadoopUser" :placeholder="t('flink.app.hadoopUser')" />
        </n-form-item-gi>
    </n-grid>
</template>
