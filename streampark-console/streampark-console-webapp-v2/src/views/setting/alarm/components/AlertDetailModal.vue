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
import type { AlertSetting } from '@/types/api/setting/types/alert.type'
import { SvgIcon } from '@/components/Icon'
import { computeAlertType } from './constants'

const props = defineProps<{
    show: boolean
    record?: AlertSetting | null
}>()

const emit = defineEmits<{
    'update:show': [value: boolean]
}>()

const { t } = useI18n()

interface DingTalkParams {
    token?: string
    contacts?: string
    isAtAll?: boolean
    alertDingURL?: string
    secretEnable?: boolean
    secretToken?: string
}

interface LarkParams {
    token?: string
    isAtAll?: boolean
    secretEnable?: boolean
    secretToken?: string
}

interface EmailParams {
    contacts?: string
}

interface WeComParams {
    token?: string
}

const alertTypeTags = computed(() => {
    if (!props.record) return []
    return props.record.alertTypeTags ?? computeAlertType(props.record.alertType)
})

const emailInfo = computed<EmailParams>(() => {
    try {
        return JSON.parse(props.record?.emailParams || '{}')
    } catch {
        return {}
    }
})

const dingTalk = computed<DingTalkParams>(() => {
    try {
        return JSON.parse(props.record?.dingTalkParams || '{}')
    } catch {
        return {}
    }
})

const weChat = computed<WeComParams>(() => {
    try {
        return JSON.parse(props.record?.weComParams || '{}')
    } catch {
        return {}
    }
})

const lark = computed<LarkParams>(() => {
    try {
        return JSON.parse(props.record?.larkParams || '{}')
    } catch {
        return {}
    }
})

function closeModal() {
    emit('update:show', false)
}

async function copyText(value?: string) {
    if (!value) return
    try {
        await navigator.clipboard.writeText(value)
        window.$message?.success(t('common.operationSuccess'))
    } catch {
        window.$message?.error(t('sys.api.apiRequestFailed'))
    }
}
</script>

<template>
    <n-modal
        :show="show"
        preset="card"
        :style="{ width: '680px' }"
        :mask-closable="false"
        @update:show="emit('update:show', $event)"
    >
        <template #header>
            <n-space align="center" :size="8">
                <SvgIcon name="alarm" :size="25" />
                <span>{{ t('setting.alarm.alertDetail') }}</span>
            </n-space>
        </template>

        <template v-if="record">
            <template v-if="alertTypeTags.includes('1')">
                <n-divider>
                    <n-space align="center" :size="8">
                        <SvgIcon name="mail" :size="20" />
                        <span>{{ t('setting.alarm.email') }}</span>
                    </n-space>
                </n-divider>
                <n-descriptions :column="1" label-placement="left" class="alert-detail-desc">
                    <n-descriptions-item :label="t('setting.alarm.alertEmail')">
                        {{ emailInfo.contacts || '-' }}
                    </n-descriptions-item>
                </n-descriptions>
            </template>

            <template v-if="alertTypeTags.includes('2')">
                <n-divider>
                    <n-space align="center" :size="8">
                        <SvgIcon name="dingtalk" :size="20" />
                        <span>{{ t('setting.alarm.dingTalk') }}</span>
                    </n-space>
                </n-divider>
                <n-descriptions :column="2" label-placement="left" class="alert-detail-desc">
                    <n-descriptions-item :label="t('setting.alarm.dingTalkUrl')" :span="2">
                        {{ dingTalk.alertDingURL || '-' }}
                    </n-descriptions-item>
                    <n-descriptions-item :label="t('setting.alarm.dingtalkAccessToken')" :span="2">
                        <n-space align="center" :size="8">
                            <span class="break-all">{{ dingTalk.token || '-' }}</span>
                            <n-button
                                v-if="dingTalk.token"
                                quaternary
                                size="tiny"
                                @click="copyText(dingTalk.token)"
                            >
                                <template #icon>
                                    <n-icon><IonIcon name="CopyOutline" /></n-icon>
                                </template>
                            </n-button>
                        </n-space>
                    </n-descriptions-item>
                    <n-descriptions-item :label="t('setting.alarm.secretToken')" :span="2">
                        <n-space align="center" :size="8">
                            <span class="break-all">{{ dingTalk.secretToken || '-' }}</span>
                            <n-button
                                v-if="dingTalk.secretToken"
                                quaternary
                                size="tiny"
                                @click="copyText(dingTalk.secretToken)"
                            >
                                <template #icon>
                                    <n-icon><IonIcon name="CopyOutline" /></n-icon>
                                </template>
                            </n-button>
                        </n-space>
                    </n-descriptions-item>
                    <n-descriptions-item :label="t('setting.alarm.dingTalkUser')">
                        {{ dingTalk.contacts || '-' }}
                    </n-descriptions-item>
                    <n-descriptions-item :label="t('setting.alarm.dingtalkIsAtAll')">
                        <n-tag :type="dingTalk.isAtAll ? 'success' : 'error'" class="!leading-20px">
                            {{ String(dingTalk.isAtAll ?? false) }}
                        </n-tag>
                    </n-descriptions-item>
                </n-descriptions>
            </template>

            <template v-if="alertTypeTags.includes('4')">
                <n-divider>
                    <n-space align="center" :size="8">
                        <SvgIcon name="wecom" :size="20" />
                        <span>{{ t('setting.alarm.weChat') }}</span>
                    </n-space>
                </n-divider>
                <n-descriptions :column="1" label-placement="left" class="alert-detail-desc">
                    <n-descriptions-item :label="t('setting.alarm.weChattoken')">
                        <n-space align="center" :size="8">
                            <span class="break-all">{{ weChat.token || '-' }}</span>
                            <n-button
                                v-if="weChat.token"
                                quaternary
                                size="tiny"
                                @click="copyText(weChat.token)"
                            >
                                <template #icon>
                                    <n-icon><IonIcon name="CopyOutline" /></n-icon>
                                </template>
                            </n-button>
                        </n-space>
                    </n-descriptions-item>
                </n-descriptions>
            </template>

            <template v-if="alertTypeTags.includes('16')">
                <n-divider>
                    <n-space align="center" :size="8">
                        <SvgIcon name="lark" :size="20" />
                        <span>{{ t('setting.alarm.lark') }}</span>
                    </n-space>
                </n-divider>
                <n-descriptions :column="2" label-placement="left" class="alert-detail-desc">
                    <n-descriptions-item :label="t('setting.alarm.larkToken')" :span="2">
                        <n-space align="center" :size="8">
                            <span class="break-all">{{ lark.token || '-' }}</span>
                            <n-button
                                v-if="lark.token"
                                quaternary
                                size="tiny"
                                @click="copyText(lark.token)"
                            >
                                <template #icon>
                                    <n-icon><IonIcon name="CopyOutline" /></n-icon>
                                </template>
                            </n-button>
                        </n-space>
                    </n-descriptions-item>
                    <n-descriptions-item :label="t('setting.alarm.larkSecretToken')" :span="2">
                        <n-space align="center" :size="8">
                            <span class="break-all">{{ lark.secretToken || '-' }}</span>
                            <n-button
                                v-if="lark.secretToken"
                                quaternary
                                size="tiny"
                                @click="copyText(lark.secretToken)"
                            >
                                <template #icon>
                                    <n-icon><IonIcon name="CopyOutline" /></n-icon>
                                </template>
                            </n-button>
                        </n-space>
                    </n-descriptions-item>
                    <n-descriptions-item :label="t('setting.alarm.larkIsAtAll')">
                        <n-tag :type="lark.isAtAll ? 'success' : 'error'" class="!leading-20px">
                            {{ String(lark.isAtAll ?? false) }}
                        </n-tag>
                    </n-descriptions-item>
                </n-descriptions>
            </template>
        </template>

        <template #footer>
            <n-space justify="end">
                <n-button @click="closeModal">
                    {{ t('common.closeText') }}
                </n-button>
            </n-space>
        </template>
    </n-modal>
</template>

<style scoped>
:deep(.alert-detail-desc .n-descriptions-table-content__label) {
    width: 150px;
}
</style>
