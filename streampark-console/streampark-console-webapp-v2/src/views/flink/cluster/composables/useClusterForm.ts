/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import type { FormInst, FormRules } from 'naive-ui'
import type { AlertSetting } from '@/types/api/setting/types/alert.type'
import type { FlinkEnv } from '@/types/api/flink/flinkEnv.type'
import {
  fetchAlertSetting,
  fetchCheckHadoop,
  fetchFlinkBaseImages,
  fetchK8sNamespaces,
  fetchListFlinkEnv,
  fetchSessionClusterIds,
} from '@/service'
import { DeployMode } from '@/enums/flinkEnum'
import { k8sRestExposedType, resolveOrder } from '@/views/flink/app/shared/data'
import optionData from '@/views/flink/app/shared/data/option'
import { optionsValueMapping } from '@/views/flink/app/shared/data/option'
import { handleFormValue } from '@/views/flink/app/shared/utils'
import { isString } from '@/utils/is'

export interface DynamicPropertyItem {
  key: string
  value: string
}

export interface ClusterFormModel {
  clusterName: string
  deployMode: number | null
  versionId: string | null
  address: string
  yarnQueue: string
  alertId: string | null
  clusterId: string
  k8sNamespace: string
  serviceAccount: string
  k8sConf: string
  flinkImage: string
  k8sRestExposedType: number | null
  resolveOrder: number | null
  slot: number | null
  totalOptions: string[]
  totalItem: Record<string, number | null>
  jmOptions: string[]
  jmOptionsItem: Record<string, number | null>
  tmOptions: string[]
  tmOptionsItem: Record<string, number | null>
  dynamicProperties: DynamicPropertyItem[]
  description: string
}

export function createDefaultClusterForm(): ClusterFormModel {
  return {
    clusterName: '',
    deployMode: null,
    versionId: null,
    address: '',
    yarnQueue: 'default',
    alertId: null,
    clusterId: '',
    k8sNamespace: '',
    serviceAccount: '',
    k8sConf: '',
    flinkImage: '',
    k8sRestExposedType: null,
    resolveOrder: null,
    slot: null,
    totalOptions: [],
    totalItem: {},
    jmOptions: [],
    jmOptionsItem: {},
    tmOptions: [],
    tmOptionsItem: {},
    dynamicProperties: [],
    description: '',
  }
}

function parseDynamicProperties(raw?: string | null): DynamicPropertyItem[] {
  if (!raw?.trim())
    return []
  return raw
    .split('\n')
    .map(line => line.trim())
    .filter(Boolean)
    .map((line) => {
      const idx = line.indexOf('=')
      if (idx <= 0)
        return { key: line, value: '' }
      return { key: line.slice(0, idx).trim(), value: line.slice(idx + 1).trim() }
    })
}

function serializeDynamicProperties(items: DynamicPropertyItem[]): string {
  return items
    .filter(item => item.key?.trim())
    .map(item => `${item.key.trim()}=${item.value ?? ''}`)
    .join('\n')
}

function parseOptionsForEdit(optionsJson?: string | null) {
  const reset = {
    slot: null as number | null,
    totalOptions: [] as string[],
    totalItem: {} as Record<string, number | null>,
    jmOptions: [] as string[],
    jmOptionsItem: {} as Record<string, number | null>,
    tmOptions: [] as string[],
    tmOptionsItem: {} as Record<string, number | null>,
  }
  if (!optionsJson)
    return reset

  let defaultOptions: Record<string, string | number> = {}
  try {
    defaultOptions = JSON.parse(optionsJson)
  }
  catch {
    return reset
  }

  for (const k in defaultOptions) {
    let v = defaultOptions[k]
    if (isString(v))
      v = v.replace(/[k|m|g]b$/g, '')
    const key = optionsValueMapping.get(k)
    if (key) {
      if (
        k === 'jobmanager.memory.flink.size'
        || k === 'taskmanager.memory.flink.size'
        || k === 'jobmanager.memory.process.size'
        || k === 'taskmanager.memory.process.size'
      ) {
        reset.totalOptions.push(key)
        reset.totalItem[key] = Number.parseInt(String(v), 10)
      }
      else if (k.startsWith('jobmanager.memory.')) {
        reset.jmOptions.push(key)
        reset.jmOptionsItem[key] = k.includes('fraction')
          ? Number.parseFloat(String(v))
          : Number.parseInt(String(v), 10)
      }
      else if (k.startsWith('taskmanager.memory.')) {
        reset.tmOptions.push(key)
        reset.tmOptionsItem[key] = k.includes('fraction')
          ? Number.parseFloat(String(v))
          : Number.parseInt(String(v), 10)
      }
    }
    else if (k === 'taskmanager.numberOfTaskSlots') {
      reset.slot = Number.parseInt(String(v), 10)
    }
  }
  return reset
}

export function useClusterForm() {
  const { t } = useI18n()

  const formRef = ref<FormInst | null>(null)
  const submitting = ref(false)
  const formModel = ref<ClusterFormModel>(createDefaultClusterForm())

  const flinkEnvs = ref<FlinkEnv[]>([])
  const alerts = ref<AlertSetting[]>([])
  const historyRecord = reactive({
    k8sNamespace: [] as string[],
    k8sSessionClusterId: [] as string[],
    serviceAccount: [] as string[],
    k8sConf: [] as string[],
    flinkImage: [] as string[],
  })

  const deployModeOptions = [
    { label: 'standalone', value: DeployMode.STANDALONE },
    { label: 'yarn session', value: DeployMode.YARN_SESSION },
    { label: 'kubernetes session', value: DeployMode.KUBERNETES_SESSION },
  ]

  const isSessionMode = computed(() =>
    formModel.value.deployMode === DeployMode.YARN_SESSION
    || formModel.value.deployMode === DeployMode.KUBERNETES_SESSION,
  )

  const isStandalone = computed(() => formModel.value.deployMode === DeployMode.STANDALONE)
  const isYarnSession = computed(() => formModel.value.deployMode === DeployMode.YARN_SESSION)
  const isK8sSession = computed(() => formModel.value.deployMode === DeployMode.KUBERNETES_SESSION)

  const showAlert = computed(() => isStandalone.value || isYarnSession.value)

  const flinkEnvOptions = computed(() =>
    flinkEnvs.value.map(item => ({ label: item.flinkName, value: item.id })),
  )

  const alertOptions = computed(() =>
    alerts.value.map(item => ({ label: item.alertName, value: item.id })),
  )

  const totalMemoryOptions = computed(() =>
    optionData
      .filter(item => item.group === 'total-memory')
      .map(item => ({ label: item.description || item.name, value: item.key })),
  )

  const jmMemoryOptions = computed(() =>
    optionData
      .filter(item => item.group === 'jobmanager-memory')
      .map(item => ({ label: item.description || item.name, value: item.key })),
  )

  const tmMemoryOptions = computed(() =>
    optionData
      .filter(item => item.group === 'taskmanager-memory')
      .map(item => ({ label: item.description || item.name, value: item.key })),
  )

  const rules = computed<FormRules>(() => ({
    clusterName: [{
      required: true,
      message: t('setting.flinkCluster.placeholder.clusterName'),
      trigger: 'blur',
    }],
    deployMode: [{
      required: true,
      validator: async (_rule, value) => {
        if (value === null || value === undefined || value === '')
          return Promise.reject(t('setting.flinkCluster.required.deployMode'))
        if (value === DeployMode.YARN_SESSION) {
          const result = await fetchCheckHadoop()
          if (!result.isSuccess || !result.data)
            return Promise.reject(t('setting.flinkCluster.operateMessage.hadoopEnvInitializationFailed'))
        }
        return Promise.resolve()
      },
      trigger: 'change',
    }],
    versionId: [{
      required: true,
      message: t('setting.flinkCluster.required.versionId'),
      trigger: 'change',
    }],
    address: [{
      required: isStandalone.value,
      message: t('setting.flinkCluster.required.address'),
      trigger: 'blur',
    }],
    flinkImage: [{
      required: isK8sSession.value,
      message: t('setting.flinkCluster.required.flinkImage'),
      trigger: 'blur',
    }],
  }))

  function handleSubmitParams(values: ClusterFormModel) {
    const dynamicProperties = serializeDynamicProperties(values.dynamicProperties)
    const submitValues = {
      ...values,
      dynamicProperties,
    }
    const options = handleFormValue(submitValues)
    const params: Recordable = {
      clusterName: values.clusterName,
      deployMode: values.deployMode,
      versionId: values.versionId,
      description: values.description,
      alertId: values.alertId,
    }

    switch (values.deployMode) {
      case DeployMode.STANDALONE:
        return { ...params, address: values.address }
      case DeployMode.YARN_SESSION:
        return {
          ...params,
          options: JSON.stringify(options),
          yarnQueue: values.yarnQueue || 'default',
          dynamicProperties,
          resolveOrder: values.resolveOrder,
        }
      case DeployMode.KUBERNETES_SESSION:
        return {
          ...params,
          clusterId: values.clusterId,
          options: JSON.stringify(options),
          dynamicProperties,
          resolveOrder: values.resolveOrder,
          k8sRestExposedType: values.k8sRestExposedType,
          k8sNamespace: values.k8sNamespace || null,
          serviceAccount: values.serviceAccount,
          k8sConf: values.k8sConf,
          flinkImage: values.flinkImage || null,
          address: values.address,
        }
      default:
        window.$message?.error(t('flink.app.action.deployModeError'))
        return {}
    }
  }

  function applyClusterRecord(cluster: Recordable) {
    const optionFields = parseOptionsForEdit(cluster.options)
    formModel.value = {
      ...createDefaultClusterForm(),
      clusterName: cluster.clusterName ?? '',
      deployMode: cluster.deployMode ?? null,
      versionId: cluster.versionId ?? null,
      address: cluster.address ?? '',
      yarnQueue: cluster.yarnQueue ?? 'default',
      alertId: cluster.alertId ?? null,
      clusterId: cluster.clusterId ?? '',
      k8sNamespace: cluster.k8sNamespace ?? '',
      serviceAccount: cluster.serviceAccount ?? '',
      k8sConf: cluster.k8sConf ?? '',
      flinkImage: cluster.flinkImage ?? '',
      k8sRestExposedType: cluster.k8sRestExposedType ?? null,
      resolveOrder: cluster.resolveOrder ?? null,
      description: cluster.description ?? '',
      dynamicProperties: parseDynamicProperties(cluster.dynamicProperties),
      ...optionFields,
    }
  }

  async function loadReferenceData() {
    const [envResult, alertResult, nsResult, clusterIdsResult, imagesResult] = await Promise.all([
      fetchListFlinkEnv(),
      fetchAlertSetting(),
      fetchK8sNamespaces(),
      fetchSessionClusterIds({ deployMode: DeployMode.KUBERNETES_SESSION }),
      fetchFlinkBaseImages(),
    ])

    if (envResult.isSuccess)
      flinkEnvs.value = envResult.data ?? []
    if (alertResult.isSuccess)
      alerts.value = alertResult.data ?? []
    if (nsResult.isSuccess)
      historyRecord.k8sNamespace = nsResult.data ?? []
    if (clusterIdsResult.isSuccess)
      historyRecord.k8sSessionClusterId = clusterIdsResult.data ?? []
    if (imagesResult.isSuccess)
      historyRecord.flinkImage = imagesResult.data ?? []
  }

  function resetForm() {
    formModel.value = createDefaultClusterForm()
    nextTick(() => formRef.value?.restoreValidation())
  }

  return {
    formRef,
    submitting,
    formModel,
    flinkEnvs,
    alerts,
    historyRecord,
    deployModeOptions,
    resolveOrder,
    k8sRestExposedType,
    isSessionMode,
    isStandalone,
    isYarnSession,
    isK8sSession,
    showAlert,
    flinkEnvOptions,
    alertOptions,
    totalMemoryOptions,
    jmMemoryOptions,
    tmMemoryOptions,
    rules,
    handleSubmitParams,
    applyClusterRecord,
    loadReferenceData,
    resetForm,
  }
}
