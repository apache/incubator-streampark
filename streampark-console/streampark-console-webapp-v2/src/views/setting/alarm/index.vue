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
import { SP_ICONS as I } from '@/constants/streamparkIcons'
import type { AlertSetting } from '@/types/api/setting/types/alert.type'
import { fetchAlertDelete, fetchAlertSetting, fetchSendAlert } from '@/service'
import { AlertTypeEnum } from '@/enums/flinkEnum'
import AlertDetailModal from './components/AlertDetailModal.vue'
import AlertModal from './components/AlertModal.vue'
import type { AlertFormRecord } from './components/AlertModal.vue'
import AlertTypeInfo from './components/AlertTypeInfo.vue'
import { alertTypes, computeAlertType } from './components/constants'

defineOptions({ name: 'AlertSetting' })

const { t } = useI18n()

type AlertSettingItem = AlertSetting & { alertTypeTags: string[] }

const loading = ref(false)
const alerts = ref<AlertSettingItem[]>([])
const formModalVisible = ref(false)
const detailModalVisible = ref(false)
const editingRecord = ref<AlertFormRecord | null>(null)
const detailRecord = ref<AlertSettingItem | null>(null)

const typeMap = computed(() => alertTypes(t))

function getAlertTypeName(type: string) {
    return typeMap.value[type]?.name ?? type
}

async function loadAlerts() {
    loading.value = true
    try {
        const result = await fetchAlertSetting()
        if (!result.isSuccess) throwApiFailure(result, t('sys.api.apiRequestFailed'))
        alerts.value = (result.data ?? []).map((item) => ({
            ...item,
            alertTypeTags: computeAlertType(item.alertType),
        }))
    } catch (e: any) {
        showCatchError(e, t('sys.api.apiRequestFailed'))
        alerts.value = []
    } finally {
        loading.value = false
    }
}

function openCreate() {
    editingRecord.value = null
    formModalVisible.value = true
}

function buildEditRecord(item: AlertSettingItem): AlertFormRecord {
    let emailParams: Recordable = {}
    let dingTalkParams: Recordable = {}
    let weComParams: Recordable = {}
    let larkParams: Recordable = {}

    if (item.alertTypeTags.includes(String(AlertTypeEnum.MAIL)))
        emailParams = JSON.parse(item.emailParams || '{}')
    if (item.alertTypeTags.includes(String(AlertTypeEnum.DINGTALK)))
        dingTalkParams = JSON.parse(item.dingTalkParams || '{}')
    if (item.alertTypeTags.includes(String(AlertTypeEnum.WECOM)))
        weComParams = JSON.parse(item.weComParams || '{}')
    if (item.alertTypeTags.includes(String(AlertTypeEnum.LARK)))
        larkParams = JSON.parse(item.larkParams || '{}')

    return {
        id: item.id,
        alertName: item.alertName,
        alertType: [...item.alertTypeTags],
        alertEmail: emailParams.contacts,
        alertDingURL: dingTalkParams.alertDingURL,
        dingtalkToken: dingTalkParams.token,
        dingtalkSecretToken: dingTalkParams.secretToken,
        alertDingUser: dingTalkParams.contacts,
        dingtalkIsAtAll: dingTalkParams.isAtAll,
        dingtalkSecretEnable: dingTalkParams.secretEnable,
        weToken: weComParams.token,
        larkToken: larkParams.token,
        larkIsAtAll: larkParams.isAtAll,
        larkSecretEnable: larkParams.secretEnable,
        larkSecretToken: larkParams.secretToken,
    }
}

function openEdit(item: AlertSettingItem) {
    editingRecord.value = buildEditRecord(item)
    formModalVisible.value = true
}

function openDetail(item: AlertSettingItem) {
    detailRecord.value = item
    detailModalVisible.value = true
}

async function handleTestAlarm(item: AlertSettingItem) {
    const message = window.$message?.loading(t('common.loadingText'), { duration: 0 })
    try {
        const result = await fetchSendAlert({ id: item.id })
        if (!result.isSuccess) throwApiFailure(result, t('sys.api.apiRequestFailed'))
        window.$message?.success(t('setting.alarm.success.test'))
        loadAlerts()
    } catch (e: any) {
        showCatchError(e, t('sys.api.apiRequestFailed'))
    } finally {
        message?.destroy()
    }
}

function stripStreamParkMessage(message?: string) {
    return (message ?? '').replaceAll(/\[StreamPark]/g, '')
}

async function handleDeleteAlertConf(item: AlertSettingItem) {
    try {
        const result = await fetchAlertDelete({ id: item.id })
        if (!result.isSuccess) throwApiFailure(result, t('sys.api.apiRequestFailed'))

        const payload = result.data as { data?: boolean; message?: string } | boolean | undefined
        const ok = typeof payload === 'boolean' ? payload : payload?.data
        if (ok) {
            window.$message?.success(t('setting.alarm.success.delete'))
        } else {
            const msg =
                typeof payload === 'object' && payload?.message
                    ? stripStreamParkMessage(payload.message)
                    : t('setting.alarm.fail.delete')
            window.$dialog?.error({
                title: t('setting.alarm.fail.delete'),
                content: msg,
                positiveText: t('common.okText'),
            })
        }
        loadAlerts()
    } catch (e: any) {
        showCatchError(e, t('sys.api.apiRequestFailed'))
    }
}

function handleFormSuccess() {
    loadAlerts()
}

onMounted(loadAlerts)
</script>

<template>
    <n-card :bordered="false" class="h-full">
        <div class="mb-16px flex flex-col gap-10px">
            <n-text strong>
                {{ t('setting.alarm.alertSetting') }}
            </n-text>
            <n-button v-auth="'project:create'" dashed class="w-full" @click="openCreate">
                <template #icon>
                    <n-icon><IonIcon name="AddOutline" /></n-icon>
                </template>
                {{ t('common.add') }}
            </n-button>
        </div>

        <n-spin :show="loading">
            <n-grid cols="1 640:2 1024:4" :x-gap="24" :y-gap="16">
                <n-gi v-for="item in alerts" :key="item.id">
                    <n-card
                        class="alert-card shadow-md"
                        :bordered="false"
                        :content-style="{ height: '240px', padding: '15px', overflowY: 'auto' }"
                    >
                        <template #header>
                            <div>
                                {{ item.alertName }}
                                <div class="mt-4px flex flex-wrap gap-4px">
                                    <n-tag
                                        v-for="type in item.alertTypeTags"
                                        :key="type"
                                        type="info"
                                        size="small"
                                    >
                                        {{ getAlertTypeName(type) }}
                                    </n-tag>
                                </div>
                            </div>
                        </template>

                        <template #action>
                            <n-space :size="4">
                                <n-tooltip trigger="hover">
                                    <template #trigger>
                                        <n-button circle quaternary @click="handleTestAlarm(item)">
                                            <template #icon>
                                                <n-icon><IonIcon :name="I.build" /></n-icon>
                                            </template>
                                        </n-button>
                                    </template>
                                    {{ t('setting.alarm.tooltip.test') }}
                                </n-tooltip>
                                <n-tooltip trigger="hover">
                                    <template #trigger>
                                        <n-button circle quaternary @click="openDetail(item)">
                                            <template #icon>
                                                <n-icon><IonIcon :name="I.view" /></n-icon>
                                            </template>
                                        </n-button>
                                    </template>
                                    {{ t('setting.alarm.tooltip.detail') }}
                                </n-tooltip>
                                <n-tooltip trigger="hover">
                                    <template #trigger>
                                        <n-button circle quaternary @click="openEdit(item)">
                                            <template #icon>
                                                <n-icon><IonIcon :name="I.edit" /></n-icon>
                                            </template>
                                        </n-button>
                                    </template>
                                    {{ t('setting.alarm.tooltip.edit') }}
                                </n-tooltip>
                                <n-popconfirm
                                    :positive-text="t('common.yes')"
                                    :negative-text="t('common.no')"
                                    @positive-click="handleDeleteAlertConf(item)"
                                >
                                    <template #trigger>
                                        <n-button circle type="error" quaternary>
                                            <template #icon>
                                                <n-icon><IonIcon :name="I.delete" /></n-icon>
                                            </template>
                                        </n-button>
                                    </template>
                                    {{ t('setting.alarm.tooltip.delete') }}
                                </n-popconfirm>
                            </n-space>
                        </template>

                        <AlertTypeInfo
                            v-if="item.alertTypeTags.includes(String(AlertTypeEnum.MAIL))"
                            :alert-type="String(AlertTypeEnum.MAIL)"
                            :alert-source="item"
                        />
                        <AlertTypeInfo
                            v-if="item.alertTypeTags.includes(String(AlertTypeEnum.WECOM))"
                            :alert-type="String(AlertTypeEnum.WECOM)"
                            :alert-source="item"
                        />
                        <AlertTypeInfo
                            v-if="item.alertTypeTags.includes(String(AlertTypeEnum.DINGTALK))"
                            :alert-type="String(AlertTypeEnum.DINGTALK)"
                            :alert-source="item"
                        />
                        <AlertTypeInfo
                            v-if="item.alertTypeTags.includes(String(AlertTypeEnum.MESSAGE))"
                            :alert-type="String(AlertTypeEnum.MESSAGE)"
                            :alert-source="item"
                        />
                        <AlertTypeInfo
                            v-if="item.alertTypeTags.includes(String(AlertTypeEnum.LARK))"
                            :alert-type="String(AlertTypeEnum.LARK)"
                            :alert-source="item"
                        />
                    </n-card>
                </n-gi>
            </n-grid>
        </n-spin>
    </n-card>

    <AlertModal
        v-model:show="formModalVisible"
        :record="editingRecord"
        @success="handleFormSuccess"
    />
    <AlertDetailModal v-model:show="detailModalVisible" :record="detailRecord" />
</template>

<style scoped>
:deep(.alert-card .n-card-header__main) {
    padding: 8px 0;
}
</style>
