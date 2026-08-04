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
import type { FormInst, FormRules } from 'naive-ui'
import type { ClusterFormModel } from '../composables/useClusterForm'

defineProps<{
    model: ClusterFormModel
    rules: FormRules
    deployModeOptions: Array<{ label: string; value: number }>
    flinkEnvOptions: Array<{ label: string; value: string }>
    alertOptions: Array<{ label: string; value: string }>
    totalMemoryOptions: Array<{ label: string; value: string }>
    jmMemoryOptions: Array<{ label: string; value: string }>
    tmMemoryOptions: Array<{ label: string; value: string }>
    resolveOrder: Array<{ label: string; value: number }>
    k8sRestExposedType: Array<{ label: string; value: number }>
    historyRecord: {
        k8sNamespace: string[]
        k8sSessionClusterId: string[]
        serviceAccount: string[]
        k8sConf: string[]
        flinkImage: string[]
    }
    isSessionMode: boolean
    isStandalone: boolean
    isYarnSession: boolean
    isK8sSession: boolean
    showAlert: boolean
}>()

const formRef = ref<FormInst | null>(null)
const { t } = useI18n()

defineExpose({
    validate: () => formRef.value?.validate(),
    restoreValidation: () => formRef.value?.restoreValidation(),
})

function memoryLabel(key: string, options: Array<{ label: string; value: string }>) {
    return options.find((item) => item.value === key)?.label ?? key
}
</script>

<template>
    <n-form ref="formRef" :model="model" :rules="rules" label-placement="top">
        <n-form-item :label="t('setting.flinkCluster.form.clusterName')" path="clusterName">
            <n-input
                v-model:value="model.clusterName"
                :placeholder="t('setting.flinkCluster.placeholder.clusterName')"
                clearable
            />
        </n-form-item>

        <n-form-item :label="t('setting.flinkCluster.form.deployMode')" path="deployMode">
            <n-select
                v-model:value="model.deployMode"
                :options="deployModeOptions"
                :placeholder="t('setting.flinkCluster.placeholder.deployMode')"
                clearable
            />
        </n-form-item>

        <n-form-item :label="t('setting.flinkCluster.form.versionId')" path="versionId">
            <n-select
                v-model:value="model.versionId"
                :options="flinkEnvOptions"
                :placeholder="t('setting.flinkCluster.placeholder.versionId')"
                filterable
                clearable
            />
        </n-form-item>

        <n-form-item
            v-if="isStandalone"
            :label="t('setting.flinkCluster.form.address')"
            path="address"
        >
            <n-input
                v-model:value="model.address"
                :placeholder="t('setting.flinkCluster.placeholder.addressRemoteMode')"
                clearable
            />
        </n-form-item>

        <n-form-item v-if="isYarnSession" :label="t('setting.flinkCluster.form.yarnQueue')">
            <n-input v-model:value="model.yarnQueue" placeholder="default" clearable />
        </n-form-item>

        <n-form-item v-if="showAlert" :label="t('flink.app.faultAlertTemplate')">
            <n-select
                v-model:value="model.alertId"
                :options="alertOptions"
                :placeholder="t('flink.app.addAppTips.alertTemplatePlaceholder')"
                clearable
            />
        </n-form-item>

        <template v-if="isK8sSession">
            <n-form-item :label="t('setting.flinkCluster.form.k8sClusterId')">
                <n-auto-complete
                    v-model:value="model.clusterId"
                    :options="
                        historyRecord.k8sSessionClusterId.map((v) => ({ label: v, value: v }))
                    "
                    placeholder="default"
                    clearable
                />
            </n-form-item>
            <n-form-item :label="t('setting.flinkCluster.form.k8sNamespace')">
                <n-auto-complete
                    v-model:value="model.k8sNamespace"
                    :options="historyRecord.k8sNamespace.map((v) => ({ label: v, value: v }))"
                    placeholder="default"
                    clearable
                />
            </n-form-item>
            <n-form-item :label="t('setting.flinkCluster.form.serviceAccount')">
                <n-auto-complete
                    v-model:value="model.serviceAccount"
                    :options="historyRecord.serviceAccount.map((v) => ({ label: v, value: v }))"
                    placeholder="default"
                    clearable
                />
            </n-form-item>
            <n-form-item :label="t('setting.flinkCluster.form.k8sConf')">
                <n-auto-complete
                    v-model:value="model.k8sConf"
                    :options="historyRecord.k8sConf.map((v) => ({ label: v, value: v }))"
                    :placeholder="t('setting.flinkCluster.placeholder.k8sConf')"
                    clearable
                />
            </n-form-item>
            <n-form-item :label="t('setting.flinkCluster.form.flinkImage')" path="flinkImage">
                <n-auto-complete
                    v-model:value="model.flinkImage"
                    :options="historyRecord.flinkImage.map((v) => ({ label: v, value: v }))"
                    :placeholder="t('setting.flinkCluster.placeholder.flinkImage')"
                    clearable
                />
            </n-form-item>
            <n-form-item :label="t('setting.flinkCluster.form.k8sRestExposedType')">
                <n-select
                    v-model:value="model.k8sRestExposedType"
                    :options="k8sRestExposedType"
                    :placeholder="t('setting.flinkCluster.placeholder.k8sRestExposedType')"
                    clearable
                />
            </n-form-item>
        </template>

        <template v-if="isSessionMode">
            <n-form-item :label="t('setting.flinkCluster.form.resolveOrder')">
                <n-select
                    v-model:value="model.resolveOrder"
                    :options="resolveOrder"
                    :placeholder="t('setting.flinkCluster.placeholder.resolveOrder')"
                    clearable
                />
            </n-form-item>
            <n-form-item :label="t('setting.flinkCluster.form.taskSlots')">
                <n-input-number
                    v-model:value="model.slot"
                    :min="1"
                    :step="1"
                    class="w-full"
                    :placeholder="t('setting.flinkCluster.placeholder.taskSlots')"
                />
            </n-form-item>
            <n-form-item :label="t('setting.flinkCluster.form.totalOptions')">
                <n-select
                    v-model:value="model.totalOptions"
                    :options="totalMemoryOptions"
                    multiple
                    filterable
                    clearable
                    :placeholder="t('setting.flinkCluster.placeholder.jmOptions')"
                />
            </n-form-item>
            <n-form-item
                v-for="key in model.totalOptions"
                :key="`total-${key}`"
                :label="memoryLabel(key, totalMemoryOptions)"
            >
                <n-input-number v-model:value="model.totalItem[key]" class="w-full" :min="0" />
            </n-form-item>
            <n-form-item :label="t('setting.flinkCluster.form.jmOptions')">
                <n-select
                    v-model:value="model.jmOptions"
                    :options="jmMemoryOptions"
                    multiple
                    filterable
                    clearable
                    :placeholder="t('setting.flinkCluster.placeholder.jmOptions')"
                />
            </n-form-item>
            <n-form-item
                v-for="key in model.jmOptions"
                :key="`jm-${key}`"
                :label="memoryLabel(key, jmMemoryOptions)"
            >
                <n-input-number v-model:value="model.jmOptionsItem[key]" class="w-full" :min="0" />
            </n-form-item>
            <n-form-item :label="t('setting.flinkCluster.form.tmOptions')">
                <n-select
                    v-model:value="model.tmOptions"
                    :options="tmMemoryOptions"
                    multiple
                    filterable
                    clearable
                    :placeholder="t('setting.flinkCluster.placeholder.tmOptions')"
                />
            </n-form-item>
            <n-form-item
                v-for="key in model.tmOptions"
                :key="`tm-${key}`"
                :label="memoryLabel(key, tmMemoryOptions)"
            >
                <n-input-number v-model:value="model.tmOptionsItem[key]" class="w-full" :min="0" />
            </n-form-item>
            <n-form-item :label="t('setting.flinkCluster.form.dynamicProperties')">
                <n-dynamic-input
                    v-model:value="model.dynamicProperties"
                    :on-create="() => ({ key: '', value: '' })"
                >
                    <template #default="{ value }">
                        <div class="flex w-full gap-8px">
                            <n-input v-model:value="value.key" placeholder="key" class="flex-1" />
                            <n-input
                                v-model:value="value.value"
                                placeholder="value"
                                class="flex-1"
                            />
                        </div>
                    </template>
                </n-dynamic-input>
            </n-form-item>
        </template>

        <n-form-item :label="t('setting.flinkCluster.form.clusterDescription')">
            <n-input
                v-model:value="model.description"
                type="textarea"
                :rows="4"
                :placeholder="t('setting.flinkCluster.placeholder.clusterDescription')"
                clearable
            />
        </n-form-item>
    </n-form>
</template>
